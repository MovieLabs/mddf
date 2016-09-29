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
package com.movielabs.mddflib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.Located;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.movielabs.mddflib.avails.validation.AvailValidator;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public abstract class ManifestIngester {
	public static final String SCHEMA_PREFIX = "http://www.movielabs.com/schema/";
	public static final String NSPACE_MANIFEST_PREFIX = "http://www.movielabs.com/schema/manifest/v";
	public static final String NSPACE_MANIFEST_SUFFIX = "/manifest";
	public static String MD_VER = "2.3";
	public static String MDMEC_VER = "2.3";
	public static String MAN_VER = "1.4";
	public static String AVAIL_VER = "1.7";
	public static Namespace xsiNSpace = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	public static Namespace mdNSpace = Namespace.getNamespace("md",
			"http://www.movielabs.com/schema/md/v" + MD_VER + "/md");
	public static Namespace mdmecNSpace = Namespace.getNamespace("mdmec",
			"http://www.movielabs.com/schema/mdmec/v" + MDMEC_VER);
	public static Namespace manifestNSpace = Namespace.getNamespace("manifest",
			NSPACE_MANIFEST_PREFIX + MAN_VER + NSPACE_MANIFEST_SUFFIX);
	public static Namespace availsNSpace = Namespace.getNamespace("avails",
			"http://www.movielabs.com/schema/avails/v" + AVAIL_VER + "/avails");

	protected static XPathFactory xpfac = XPathFactory.instance();

	protected static String cmVrcPath = "/com/movielabs/mddf/resources/cm_vocab.json";
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

	public ManifestIngester(LogMgmt loggingMgr) {
		this.loggingMgr = loggingMgr;
	}


	protected static JSONObject loadVocab(String rsrcPath, String vocabSet) throws JDOMException, IOException {
		StringBuilder builder = new StringBuilder();
		InputStream inp = AvailValidator.class.getResourceAsStream(rsrcPath);
		InputStreamReader isr = new InputStreamReader(inp, "UTF-8");
		BufferedReader reader = new BufferedReader(isr);
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}
		String input = builder.toString();
		JSONObject vocabResource = JSONObject.fromObject(input);
		JSONObject vocabulary = vocabResource.getJSONObject(vocabSet);
		return vocabulary;
	}
	
	protected static StreamSource[] generateStreamSources(String[] xsdFilesPaths) {
		Stream<String> xsdStreams = Arrays.stream(xsdFilesPaths);
		Stream<Object> streamSrcs = xsdStreams.map(StreamSource::new);
		List<Object> collector = streamSrcs.collect(Collectors.toList());
		StreamSource[] result = collector.toArray(new StreamSource[xsdFilesPaths.length]);
		return result;
	}

	/**
	 * Validate everything that is fully specified via the identified XSD.
	 * 
	 * @param xmlFile
	 * @param xsdLocation
	 * @param moduleId
	 *            identifier used in log messages
	 * @return
	 */
	protected boolean validateXml(File xmlFile, String xsdLocation, String moduleId) {
		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Validating XML", xmlFile, -1, moduleId, null, null);
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		List<String> xsdPathList = new ArrayList<String>();
		xsdPathList.add(xsdLocation);
		// Remember to include Common Metadata schema......
		String mdXsdFile = "./resources/md-v" + MD_VER + ".xsd";
		xsdPathList.add(mdXsdFile);
		if (MDMEC_VER != null) {
			String mdmecXsdFile = "./resources/mdmec-v" + MDMEC_VER + ".xsd";
			xsdPathList.add(mdmecXsdFile);
		}
		String[] xsdPaths = new String[xsdPathList.size()];
		xsdPaths = xsdPathList.toArray(xsdPaths);
		StreamSource[] xsdSources = generateStreamSources(xsdPaths);
		String genericTooltip = "XML does not conform to schema as defined in " + xsdLocation;
		// now do actual validation
		try {
			Schema schema = schemaFactory.newSchema(xsdSources);
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(xmlFile));
		} catch (SAXParseException e) {
			String msg = e.getMessage();
			// get rid of some boilerplate and unhelpfull lead-in text
			int i = msg.indexOf(":");
			msg = msg.substring(i + 2);
			loggingMgr.log(LogMgmt.LEV_ERR, logMsgDefaultTag, msg, xmlFile, e.getLineNumber(), moduleId, genericTooltip,
					null);
			return (false);
		} catch (IOException e) {
			String msg = "Validation error -::" + e.getMessage();
			loggingMgr.log(LogMgmt.LEV_ERR, logMsgDefaultTag, msg, xmlFile, -1, moduleId, genericTooltip, null);
			return (false);
		} catch (SAXException e) {
			String msg = "Validation error -::" + e.getMessage();
			loggingMgr.log(LogMgmt.LEV_ERR, logMsgDefaultTag, msg, xmlFile, -1, moduleId, genericTooltip, null);
			return (false);
		}
		loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, "XML is valid", xmlFile, -1, moduleId, null, null);
		return (true);
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
					loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, seqEl, "Missing required SequenceInfo",
							null, null, logMsgSrcId);
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

	/**
	 * returns true if string defines a non-negative integer value
	 * 
	 * @param indexValue
	 * @return
	 */
	protected boolean isValidIndex(String indexValue) {
		try {
			int iv = Integer.parseInt(indexValue);
			return (iv >= 0);
		} catch (NumberFormatException e) {
			return false;
		}
	}

	// protected JSONObject extractLocalInfo(JSONObject mData_external) {
	// JSONObject mData_local = JsonUtilities.findByAttribute(mData_external,
	// "LocalizedInfo", "@language", "en");
	// if (mData_local == null) {
	// mData_local = JsonUtilities.findByAttribute(mData_external,
	// "LocalizedInfo", "@language", "en-US");
	// }
	// return mData_local;
	// }

	public static Element getAsXml(File inputFile) throws JDOMException, IOException {
		InputStreamReader isr = new InputStreamReader(new FileInputStream(inputFile), "UTF-8");
		SAXBuilder builder = new SAXBuilder();
		builder.setJDOMFactory(new LocatedJDOMFactory());
		Document validatedDoc = builder.build(isr);
		Element rootEl = validatedDoc.getRootElement();
		return rootEl;
	}

	public static JSONObject getAsJson(File inputFile) throws IOException {
		String inString = readFile(inputFile.getAbsolutePath());
		JSON jsonObj = JSONSerializer.toJSON(inString);
		return (JSONObject) jsonObj;
	}

	/**
	 * Locates a <tt>&lt;BasicMetadata&gt; Element</tt> using the specified
	 * <tt>cid</tt>. The contents of it's <tt>&lt;LocalizedInfo&gt;</tt>is
	 * converted to a JSON format using the <tt>extractMetadata()</tt> function
	 * and then returned to the caller. If a match for the specified
	 * <tt>cid</tt> value can not be found, a <tt>null</tt> value is returned.
	 * Note that the returned JSON will contain only the <u>required</u>
	 * properties of the <u>default</u> <tt>&lt;LocalizedInfo&gt; Element</tt>
	 * 
	 * @param cid
	 * @param mdList
	 * @return
	 */
	// protected static JSONObject extractMetadata(String cid, List<Element>
	// mdList) {
	// Element targetEl = getBasicMetaData(cid, mdList);
	// Object[] targets = mdList.toArray();
	// for (int i = 0; i < targets.length; i++) {
	// Element target = (Element) targets[i];
	// /* get the value for @ContentID */
	// String targetCid = target.getAttributeValue("ContentID");
	// if (targetCid.equals(cid)) {
	// // Process the match
	// Element basic = target.getChild("BasicMetadata", manifestNSpace);
	// return extractMetadata(basic);
	// }
	// }
	// return null;
	// }

	/**
	 * Locate and return a <tt>&lt;BasicMetadata&gt; Element</tt> using the
	 * specified <tt>cid</tt> . If a match for the specified <tt>cid</tt> value
	 * can not be found, a <tt>null</tt> value is returned.
	 * 
	 * @param cid
	 * @param mdList
	 * @return
	 */
	protected static Element getBasicMetaData(String cid, List<Element> mdList) {
		Object[] targets = mdList.toArray();
		for (int i = 0; i < targets.length; i++) {
			Element target = (Element) targets[i];
			/* get the value for @ContentID */
			String targetCid = target.getAttributeValue("ContentID");
			if (targetCid.equals(cid)) {
				// Process the match
				Element basicMDEl = target.getChild("BasicMetadata", manifestNSpace);
				return basicMDEl;
			}
		}
		return null;
	}

	/**
	 * Return asJSON the <u>required</u> properties of the <u>default</u>
	 * <tt>&lt;LocalizedInfo&gt; Element</tt>
	 * 
	 * @param target
	 * @return
	 */
	// protected static JSONObject extractMetadata(Element target) {
	// JSONObject json = new JSONObject();
	// List<Element> localList = target.getChildren("LocalizedInfo", mdNSpace);
	// Object[] locals = localList.toArray();
	// for (int j = 0; j < locals.length; j++) {
	// Element localMData = (Element) locals[j];
	// Map<String, String> localKVP = new HashMap<String, String>();
	// String language = localMData.getAttributeValue("language");
	// // NOTE: We are limited to elements that the Schema flags as
	// // REQUIRED
	// String title = localMData.getChildTextNormalize("TitleSort", mdNSpace);
	// String summary = localMData.getChildTextNormalize("Summary190",
	// mdNSpace);
	// localKVP.put("title", title);
	// localKVP.put("summary", summary);
	// /*
	// * Remaining fields are optional in XSD but may be required by a
	// * profile
	// */
	// String titleFull =
	// localMData.getChildTextNormalize("TitleDisplayUnlimited", mdNSpace);
	// json.put(language, localKVP);
	// }
	// return json;
	// }

	//
	// /**
	// * @param object
	// * @param string
	// */
	// protected static void logDebug(String moduleID, int tag, String msg) {
	// loggingMgr.log(LogMgmt.LEV_DEBUG, tag, msg, null, -1, moduleID, null,
	// null);
	// }
	//
	// /**
	// * @param object
	// * @param string
	// */
	// protected static void logInfo(String moduleID, int tag, String msg) {
	// loggingMgr.log(LogMgmt.LEV_INFO, tag, msg, null, -1, moduleID, null,
	// null);
	// }
	//
	// /**
	// * @param name
	// * @param alid
	// * @param msg
	// *
	// */
	// protected static void logError(String moduleID, int tag, File xmlFile,
	// String msg, String supplemental,
	// LogReference docRef) {
	// loggingMgr.log(LogMgmt.LEV_ERR, tag, msg, xmlFile, -1, moduleID,
	// supplemental, docRef);
	//
	// }

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
			ManifestIngester.sourceFolder = srcFile.getParentFile();
		} else {
			ManifestIngester.sourceFolder = srcFile;
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
	 * @param manifestSchemaVer
	 * @throws IllegalArgumentException
	 */
	public static void setManifestVersion(String manifestSchemaVer) throws IllegalArgumentException {
		switch (manifestSchemaVer) {
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
		/* Since MDMEC isnt used for Manifest, set to NULL */
		MDMEC_VER = null;
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + MD_VER + "/md");
		manifestNSpace = Namespace.getNamespace("manifest", NSPACE_MANIFEST_PREFIX + MAN_VER + NSPACE_MANIFEST_SUFFIX);
	}

	/**
	 * @param availSchemaVer
	 */
	public static void setAvailVersion(String availSchemaVer) throws IllegalArgumentException {
		switch (availSchemaVer) {
		case "2.1":
			AVAIL_VER = "2.1";
			MD_VER = "2.4";
			MDMEC_VER = "2.4";
			break;
		case "1.7":
			AVAIL_VER = "1.7";
			MD_VER = "2.3";
			MDMEC_VER = "2.3";
			break;
		default:
			throw new IllegalArgumentException("Unsupported Avails Schema version " + availSchemaVer);
		}
		mdmecNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/mdmec/v" + MDMEC_VER + "/mdmec");
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + MD_VER + "/md");
		availsNSpace = Namespace.getNamespace("avails",
				"http://www.movielabs.com/schema/avails/v" + AVAIL_VER + "/avails");
	}

}
