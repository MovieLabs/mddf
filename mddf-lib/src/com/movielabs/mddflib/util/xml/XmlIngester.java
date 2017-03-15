/**
 * Created Jul 27, 2015
 * Copyright Motion Picture Laboratories, Inc. (2015)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.movielabs.mddflib.util.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.SAXParseException;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddflib.logging.IssueLogger;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public abstract class XmlIngester implements IssueLogger {
	public static String MD_VER = "2.3";
	public static String MDMEC_VER = "2.4";
	public static String MAN_VER = "1.5";
	public static String AVAIL_VER = "2.1";
	public static Namespace xsiNSpace = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

	public static Namespace mdNSpace = Namespace.getNamespace("md",
			MddfContext.NSPACE_CMD_PREFIX + MD_VER + MddfContext.NSPACE_CMD_SUFFIX);

	public static Namespace mdmecNSpace = Namespace.getNamespace("mdmec",
			MddfContext.NSPACE_MDMEC_PREFIX + MDMEC_VER + MddfContext.NSPACE_MDMEC_SUFFIX);

	public static Namespace manifestNSpace = Namespace.getNamespace("manifest",
			MddfContext.NSPACE_MANIFEST_PREFIX + MAN_VER + MddfContext.NSPACE_MANIFEST_SUFFIX);
	public static Namespace availsNSpace = Namespace.getNamespace("avails",
			MddfContext.NSPACE_AVAILS_PREFIX + AVAIL_VER + MddfContext.NSPACE_AVAILS_SUFFIX);

	protected static XPathFactory xpfac = XPathFactory.instance();

	private static Map<String, JSONObject> rsrcCache = new HashMap<String, JSONObject>();

	protected static File srcFile;
	protected static File sourceFolder;

	protected File curFile;
	protected String curFileName;

	/*
	 * should be changed by extending classes
	 */
	protected String logMsgSrcId = "Ingester";
	protected int logMsgDefaultTag = LogMgmt.TAG_N_A;
	protected LogMgmt loggingMgr;

	public XmlIngester(LogMgmt loggingMgr) {
		this.loggingMgr = loggingMgr;
	}

	protected static JSONObject getMddfResource(String rsrcId) {
		String rsrcPath = MddfContext.RSRC_PATH + rsrcId + ".json";
		JSONObject rsrc = rsrcCache.get(rsrcPath);
		if (rsrc == null) {
			try {
				rsrc = loadJSON(rsrcPath);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			rsrcCache.put(rsrcPath, rsrc);
		}
		JSONObject jsonRsrc = rsrc.getJSONObject(rsrcId);
		return jsonRsrc;
	}

	/**
	 * Return resource defining a version-specific controlled vocabulary set.
	 * 
	 * @param rsrcId
	 * @param rsrcVersion
	 * @return a JSONObject or <tt>null</tt. if the resource is not accessible
	 *         or is not valid JSON
	 */
	protected static Object getMddfResource(String rsrcId, String rsrcVersion) {
		String rsrcPath = MddfContext.RSRC_PATH + "vocab_" + rsrcId + "_v" + rsrcVersion + ".json";
		JSONObject rsrc = (JSONObject) rsrcCache.get(rsrcPath);
		if (rsrc == null) {
			try {
				rsrc = loadJSON(rsrcPath);
			} catch (Exception e) {
				System.out.println("Missing MDDF Resc " + rsrcPath);
				e.printStackTrace();
				return null;
			}
			rsrcCache.put(rsrcPath, rsrc);
		}
		Object jsonRsrc = rsrc.get(rsrcId);
		return jsonRsrc;
	}

	protected static JSONObject loadJSON(String rsrcPath) throws JDOMException, IOException {
		InputStream inp = XmlIngester.class.getResourceAsStream(rsrcPath);
		InputStreamReader isr = new InputStreamReader(inp, "UTF-8");
		BufferedReader reader = new BufferedReader(isr);
		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}
		String input = builder.toString();
		return JSONObject.fromObject(input);
	}

	protected static Properties loadProperties(String rsrcPath) {
		Properties props = new Properties();
		InputStream inStream = XmlIngester.class.getResourceAsStream(rsrcPath);
		try {
			props.load(inStream);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return props;
	}

	/**
	 * Identify all ALID and determine which experience is used when the ALID is
	 * accessed by a consumer.
	 * 
	 * @param root
	 * @return
	 */
	protected List<String> extractAlidMap(Element root) {
		List<String> primaryExpSet = new ArrayList<String>();
		Element mapsEl = root.getChild("ALIDExperienceMaps", manifestNSpace);
		if (mapsEl == null) {
			return null;
		}
		List<Element> mapEList = mapsEl.getChildren("ALIDExperienceMap", manifestNSpace);
		Object[] targets = mapEList.toArray();
		for (int i = 0; i < targets.length; i++) {
			Element target = (Element) targets[i];
			/* get the values for ALID and ExperienceID */
			// Element alidEl = target.getChild("ALID", manifestNSpace);
			// String alid = alidEl.getTextNormalize();
			Element expEl = target.getChild("ExperienceID", manifestNSpace);
			String expId = expEl.getTextNormalize();
			if (!primaryExpSet.contains(expId)) {
				primaryExpSet.add(expId);
			}
		}
		return primaryExpSet;
	}

	protected static String readFile(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");
		while ((line = reader.readLine()) != null) {
			/* remove any tabs (\t) */
			line = line.replaceAll("\\t", "");
			/* replace linefeeds with single space */
			line = line.replaceAll("\\n", " ");
			stringBuilder.append(line);
			stringBuilder.append(ls);
		}
		reader.close();
		return stringBuilder.toString();
	}

	/**
	 * Return an ordered list of child Elements that have the specified
	 * relationship with the parent or null if unable to sort the children. A
	 * return value of <tt>null</tt> is, therefore, not the same as returning an
	 * empty List as the later indicates no matching child Elements were found
	 * rather than a problem while sorting.
	 * <p>
	 * Ordering is determined in one of two ways. If
	 * <tt>&lt;SequenceInfo&gt;</tt> elements are present they will be used to
	 * determine the order of the list entries returned. Otherwise ordering is
	 * indeterminate.
	 * </p>
	 * <p>
	 * Additional restrictions on the use of <tt>&lt;SequenceInfo&gt;</tt>:
	 * <ul>
	 * <li><tt>&lt;SequenceInfo&gt;</tt> elements shall be used for either all
	 * of the children or none of the children. Mixed usage is not allowed.
	 * <li>SequenceInfo/Number starts with one and increases monotonically for
	 * each child</li>
	 * </ul>
	 * </p>
	 * 
	 * @param parentEl
	 * @param childName
	 * @param relationship
	 * @param seqInfoRequired
	 * @return <tt>List</tt> of child Elements or null if unable to sort the
	 *         children.
	 */
	protected List<Element> getSortedChildren(Element parentEl, String childName, String relationship,
			boolean seqInfoRequired) {
		boolean hasErrors = false;
		// ..........
		List<Element> allChildList = parentEl.getChildren(childName, manifestNSpace);
		Element[] firstPass = new Element[allChildList.size()];
		int lastIndex = -1;
		for (int i = 0; i < allChildList.size(); i++) {
			Element nextEl = allChildList.get(i);
			String relType = nextEl.getChildTextNormalize("Relationship", manifestNSpace);
			if (relType == null) {
				String summaryMsg = "Missing Relationship element in " + childName + " element";
				loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_WARN, nextEl, summaryMsg, null, null, logMsgSrcId);
			} else if (relType.equalsIgnoreCase(relationship)) {
				Element seqEl = nextEl.getChild("SequenceInfo", manifestNSpace);
				String seqNum = null;
				if (seqEl != null) {
					seqNum = seqEl.getChildTextNormalize("Number", mdNSpace);
					// check for empty string..
					if (seqNum.isEmpty()) {
						seqNum = null;
					}
				}
				if (seqInfoRequired && (seqNum == null)) {
					// Flag as ERROR
					loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, seqEl, "Missing required SequenceInfo", null,
							null, logMsgSrcId);
					hasErrors = true;
				} else if (seqNum != null) {
					// seqNum should be monotonically increasing starting with 1
					int index = -1;
					try {
						index = Integer.parseInt(seqNum) - 1;
					} catch (NumberFormatException e) {
					}
					if (index < 0) {
						loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, seqEl,
								"Invalid Number for SequenceInfo (must be a positive integer).", null, null,
								logMsgSrcId);
						hasErrors = true;
					} else if (index >= firstPass.length) {
						loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, seqEl,
								"Invalid Number for SequenceInfo (must increase monotonically).", null, null,
								logMsgSrcId);
						hasErrors = true;

					} else {
						if (firstPass[index] != null) {
							loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, seqEl,
									"Duplicate Number in SequenceInfo", null, null, logMsgSrcId);
							hasErrors = true;
						} else {
							firstPass[index] = nextEl;
							lastIndex = Math.max(lastIndex, index);
						}
					}
				} else {
					/*
					 * Explicit SeqInfo is neither provided nor required so use
					 * as-is order
					 */
					firstPass[i] = nextEl;
					lastIndex = i;
				}
			}
		}
		if (hasErrors) {
			return null;
		}

		List<Element> childElList = new ArrayList<Element>();
		/*
		 * Transfer the Elements from the 'firstPass' array to the List. While
		 * transferring, check to make sure the SequenceInfo/Number starts with
		 * one and increases monotonically for each child.
		 * 
		 */
		for (int i = 0; i <= lastIndex; i++)

		{
			if (firstPass[i] != null) {
				childElList.add(firstPass[i]);
			} else {
				int gap = i + 1;
				loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, parentEl,
						"Incomplete SequenceInfo... missing Number=" + gap, null, null, logMsgSrcId);
				hasErrors = true;
			}
		}
		if (hasErrors) {
			return null;
		}
		return childElList;

	}

	public void logIssue(int tag, int level, Object target, String msg, String explanation, LogReference srcRef,
			String moduleId) {
		loggingMgr.logIssue(tag, level, target, msg, explanation, srcRef, moduleId);
	}

	/**
	 * Reads an XML-formatted file, converting it to a JDOM document that is
	 * returned to the caller.
	 * 
	 * @param inputFile
	 * @return
	 * @throws SAXParseException
	 *             if the XML is improperly formatted
	 * @throws IOException
	 *             it the specified file can not be found or read.
	 */
	public static Document getAsXml(File inputFile) throws SAXParseException, IOException {
		InputStreamReader isr;
		isr = new InputStreamReader(new FileInputStream(inputFile), "UTF-8");
		SAXBuilder builder = new SAXBuilder();
		builder.setJDOMFactory(new LocatedJDOMFactory());
		Document xmlDoc;
		try {
			xmlDoc = builder.build(isr);
		} catch (JDOMException e) {
			SAXParseException cause = (SAXParseException) e.getCause();
			throw cause;
		}
		return xmlDoc;
	}

	public static boolean writeXml(File outputLoc, Document xmlDoc) {
		Format myFormat = Format.getPrettyFormat();
		XMLOutputter outputter = new XMLOutputter(myFormat);
		try {
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(outputLoc), "UTF-8");
			outputter.output(xmlDoc, osw);
			osw.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return (outputLoc.exists());
	}

	public static JSONObject getAsJson(File inputFile) throws IOException {
		String inString = readFile(inputFile.getAbsolutePath());
		JSON jsonObj = JSONSerializer.toJSON(inString);
		return (JSONObject) jsonObj;
	}

	/**
	 * @return the sourceFolder
	 */
	public static File getSourceFolder() {
		return sourceFolder;
	}

	/**
	 * @param srcPath
	 *            the srcPath to set
	 */
	public static void setSourceDirPath(String srcPath) {
		srcFile = new File(srcPath);
		if (srcFile.isFile()) {
			XmlIngester.sourceFolder = srcFile.getParentFile();
		} else {
			XmlIngester.sourceFolder = srcFile;
		}

	}

	/**
	 * Generate a relative <tt>Path</tt> from the current <tt>sourceFolder</tt>
	 * to the <tt>target</tt>.
	 * 
	 * @param target
	 * @return
	 */
	protected static Path getRelativePath(File target) {
		Path srcPath = sourceFolder.toPath();
		Path relative = srcPath.relativize(target.toPath());
		return relative;
	}

	/**
	 * Identify the XSD version for the document's <i>primary</i> MDDF namespace
	 * (i.e., Manifest, Avails, MDMec, etc.)
	 * 
	 * @param docRootEl
	 * @return
	 */
	public static String identifyXsdVersion(Element docRootEl) {
		String nSpaceUri = docRootEl.getNamespaceURI();
		String schemaType = null;
		if (nSpaceUri.contains("manifest")) {
			schemaType = "manifest";
		} else if (nSpaceUri.contains("avails")) {
			schemaType = "avails";
		} else if (nSpaceUri.contains("mdmec")) {
			schemaType = "mdmec";
		} else {
			return null;
		}
		String schemaPrefix = MddfContext.SCHEMA_PREFIX + schemaType + "/v";
		String schemaVer = nSpaceUri.replace(schemaPrefix, "");
		schemaVer = schemaVer.replace("/" + schemaType, "");
		return schemaVer;
	}

	/**
	 * Configure all XML-related functions to work with the specified version of
	 * the Manifest XSD. This includes setting the correct version of the Common
	 * Metadata XSD that is used with the specified Manifest version. If the
	 * <tt>manifestSchemaVer</tt> is not supported by the current version of
	 * <tt>mddf-lib</tt> an <tt>IllegalArgumentException</tt> will be thrown.
	 * 
	 * @param manifestSchemaVer
	 * @throws IllegalArgumentException
	 */
	public static void setManifestVersion(String manifestSchemaVer) throws IllegalArgumentException {
		switch (manifestSchemaVer) {
		case "1.6":
			MAN_VER = "1.6";
			MD_VER = "2.5";
			break;
		case "1.5":
			MAN_VER = "1.5";
			MD_VER = "2.4";
			break;
		case "1.4":
			MAN_VER = "1.4";
			MD_VER = "2.3";
			break;
		default:
			throw new IllegalArgumentException("Unsupported Manifest Schema version " + manifestSchemaVer);
		}
		/* Since MDMEC isn't used for Manifest, set to NULL */
		MDMEC_VER = null;
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + MD_VER + "/md");
		manifestNSpace = Namespace.getNamespace("manifest",
				MddfContext.NSPACE_MANIFEST_PREFIX + MAN_VER + MddfContext.NSPACE_MANIFEST_SUFFIX);
	}

	/**
	 * @param mecSchemaVer
	 * @throws IllegalArgumentException
	 */
	public static void setMdMecVersion(String mecSchemaVer) throws IllegalArgumentException {
		switch (mecSchemaVer) {
		case "2.4":
		case "2.3":
			MD_VER = "2.4";
			break;
		case "2.5":
			MD_VER = "2.5";
			break;
		default:
			throw new IllegalArgumentException("Unsupported MEC Schema version " + mecSchemaVer);
		}
		MDMEC_VER = mecSchemaVer;
		mdmecNSpace = Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v" + MDMEC_VER + "/mdmec");
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + MD_VER + "/md");
	}

	/**
	 * Configure all XML-related functions to work with the specified version of
	 * the Avails XSD. This includes setting the correct version of the Common
	 * Metadata and MDMEC XSD that are used with the specified Avails version.
	 * If the <tt>availSchemaVer</tt> is not supported by the current version of
	 * <tt>mddf-lib</tt> an <tt>IllegalArgumentException</tt> will be thrown.
	 * 
	 * @param availSchemaVer
	 * @throws IllegalArgumentException
	 */
	public static void setAvailVersion(String availSchemaVer) throws IllegalArgumentException {
		switch (availSchemaVer) {
		case "2.2.1":
			MD_VER = "2.5";
			MDMEC_VER = "2.5";
			break;
		case "2.2":
		case "2.1":
			MD_VER = "2.4";
			MDMEC_VER = "2.4";
			break;
		case "1.7":
			MD_VER = "2.3";
			MDMEC_VER = "2.3";
			break;
		default:
			throw new IllegalArgumentException("Unsupported Avails Schema version " + availSchemaVer);
		}
		AVAIL_VER = availSchemaVer;
		mdmecNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/mdmec/v" + MDMEC_VER + "/mdmec");
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + MD_VER + "/md");
		availsNSpace = Namespace.getNamespace("avails",
				"http://www.movielabs.com/schema/avails/v" + AVAIL_VER + "/avails");
	}

}
