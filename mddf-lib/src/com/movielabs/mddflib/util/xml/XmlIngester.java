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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathFactory;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.xml.XmlIngester.Input;
import com.movielabs.mddflib.util.xml.XmlIngester.ResourceResolver;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public abstract class XmlIngester {
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

	protected static String vocabRsrcPath = "/com/movielabs/mddf/resources/cm_vocab.json";
	protected static File srcFile;
	protected static File sourceFolder;
	private static JSONObject vocabResource = null;

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

	protected static JSONObject loadVocab(String rsrcPath, String vocabSet) throws JDOMException, IOException {
		if (vocabResource == null) {
			vocabResource = loadJSON(rsrcPath);
		}
		JSONObject vocabulary = vocabResource.getJSONObject(vocabSet);
		return vocabulary;
	}

	protected static JSONObject loadJSON(String rsrcPath) throws JDOMException, IOException {
		StringBuilder builder = new StringBuilder();
		InputStream inp = XmlIngester.class.getResourceAsStream(rsrcPath);
		InputStreamReader isr = new InputStreamReader(inp, "UTF-8");
		BufferedReader reader = new BufferedReader(isr);
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

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public static String defaultRsrcLoc;
	private static boolean streamModeA;

	static {
		/*
		 * Mode A works but required a kludge (i.e., duplicate XSD files); Mode
		 * B has a bug.
		 */

		streamModeA = true;

		if (streamModeA) {
			defaultRsrcLoc = "./resources/";
		} else {
			defaultRsrcLoc = SchemaWrapper.RSRC_PACKAGE;
		}
	}

	public static StreamSource[] generateStreamSources(String[] xsdFilesPaths) {

		if (streamModeA) {
			return generateStreamSources_A(xsdFilesPaths);
		} else {
			return generateStreamSources_B(xsdFilesPaths);
		}
	}

	private static StreamSource[] generateStreamSources_A(String[] xsdFilesPaths) {
		Stream<String> xsdStreams = Arrays.stream(xsdFilesPaths);
		Stream<Object> streamSrcs = xsdStreams.map(StreamSource::new);
		List<Object> collector = streamSrcs.collect(Collectors.toList());
		StreamSource[] result = collector.toArray(new StreamSource[xsdFilesPaths.length]);
		return result;
	}

	private static StreamSource[] generateStreamSources_B(String[] xsdFilesPaths) {
		StreamSource[] result = new StreamSource[xsdFilesPaths.length];
		for (int i = 0; i < result.length; i++) {
			InputStream inStream = XmlIngester.class.getResourceAsStream(xsdFilesPaths[i]);
			if (inStream != null) {
				result[i] = new StreamSource(inStream);
				System.out.println("Found schema rsrc: " + xsdFilesPaths[i]);
			} else {
				System.out.println("NULL schema rsrc: " + xsdFilesPaths[i]);
			}
		}
		return result;
	}
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++

	/**
	 * Validate everything that is fully specified via the identified XSD.
	 * 
	 * @param srcFile
	 *            source from which XML was obtained (used for logging only; not
	 *            required to be an XML file)
	 * @param xsdLocation
	 * @param moduleId
	 *            identifier used in log messages
	 * @return
	 */
	protected boolean validateXml(File srcFile, Element docRootEl, String xsdLocation, String moduleId) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		if (!streamModeA) {
			System.out.println("Loading XSD resource usiing Mode 'B'");
			schemaFactory.setResourceResolver(new ResourceResolver());
		}

		List<String> xsdPathList = new ArrayList<String>();
		xsdPathList.add(xsdLocation);
		String[] xsdPaths = new String[xsdPathList.size()];
		xsdPaths = xsdPathList.toArray(xsdPaths);
		StreamSource[] xsdSources = generateStreamSources(xsdPaths);
		String genericTooltip = "XML does not conform to schema as defined in " + xsdLocation;
		Schema schema;
		try {
			schema = schemaFactory.newSchema(xsdSources);
		} catch (SAXException e1) {
			e1.printStackTrace();
			return false;
		}
		/*
		 * Use a custom ErrorHandler for all SAXParseExceptions
		 */
		XsdErrorHandler errHandler = new XsdErrorHandler(srcFile);
		// now do actual validation
		try {
			Validator validator = schema.newValidator();
			validator.setErrorHandler(errHandler);
			/*
			 * This block of code handles a problem associated with supporting
			 * the validation of XML generated from an Excel spreadsheet.
			 * Regardless of whether the XML comes from an XML file or is
			 * internally generated via the transformation of an xlsx file, we
			 * still have (at this point in processing) a JDOM document. In
			 * theory the obvious approach is to use a JDOMSource in all cases.
			 * In practice, however, using a JDOMSource will result in
			 * SaxParseException messages that lack line numbers. This greatly
			 * reduces the value of the error messages. The work-around solution
			 * is to use a StreamSource and process the XML on the file system
			 * even though we already have the same XML in the form of the JDOM
			 * document.
			 * 
			 */
			Source src;
			if (srcFile.getName().endsWith(".xml")) {
				src = new StreamSource(srcFile);
			} else {
				src = new JDOMSource(docRootEl);
			}
			validator.validate(src);
		} catch (IOException e) {
			String msg = "Validation error -::" + getExceptionCause(e);
			loggingMgr.log(LogMgmt.LEV_ERR, logMsgDefaultTag, msg, srcFile, -1, moduleId, genericTooltip, null);
			return (false);
		} catch (SAXException e) {
			String msg = "Validation error -::" + getExceptionCause(e);
			loggingMgr.log(LogMgmt.LEV_ERR, logMsgDefaultTag, msg, srcFile, -1, moduleId, genericTooltip, null);
			return (false);
		}
		if (errHandler.errCount == 0) {
			loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, "XML is valid", srcFile, -1, moduleId, null, null);
			return (true);
		} else {
			loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, "Invalid XML, " + errHandler.errCount + " errors found",
					srcFile, -1, moduleId, null, null);
			return (false);
		}
	}

	protected String getExceptionCause(Exception e) {
		String description = e.getMessage();
		Throwable cause = e.getCause();
		while (cause != null) {
			description = cause.getMessage();
			cause = cause.getCause();
		}
		return description;
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

	protected void logIssue(int tag, int level, Element xmlElement, String msg, String explanation, LogReference srcRef,
			String moduleId) {
		loggingMgr.logIssue(tag, level, xmlElement, msg, explanation, srcRef, moduleId);
	}

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

	public static void setMdMecVersion(String mecSchemaVer) throws IllegalArgumentException {
		switch (mecSchemaVer) {
		case "2.4":
		case "2.3":
			MD_VER = "2.4";
			break;
		default:
			throw new IllegalArgumentException("Unsupported MEC Schema version " + mecSchemaVer);
		}
		MDMEC_VER = mecSchemaVer;
		mdmecNSpace = Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v" + MDMEC_VER + "/mdmec");
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + MD_VER + "/md");
	}

	/**
	 * @param availSchemaVer
	 */
	public static void setAvailVersion(String availSchemaVer) throws IllegalArgumentException {
		switch (availSchemaVer) {
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

	// ###################################################################

	// ###################################################################
	/**
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	public class ResourceResolver implements LSResourceResolver {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.w3c.dom.ls.LSResourceResolver#resolveResource(java.lang.String,
		 * java.lang.String, java.lang.String, java.lang.String,
		 * java.lang.String)
		 */
		@Override
		public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId,
				String baseURI) {
			InputStream inStream = XmlIngester.class.getResourceAsStream(SchemaWrapper.RSRC_PACKAGE + systemId);
			System.out.println("resolveResource:: namespaceURI="+namespaceURI+", systemId="+systemId);
			return new Input(publicId, systemId, inStream);
		}

	}

	// ###################################################################
	/**
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	public class Input implements LSInput {

		private String publicId;
		private String systemId;
		private BufferedInputStream bufferedInStream;
		private InputStreamReader inStreamReader;

		/**
		 * @param publicId
		 * @param systemId
		 * @param inStream
		 */
		public Input(String publicId, String systemId, InputStream inStream) {
			this.publicId = publicId;
			this.systemId = systemId;
			this.bufferedInStream = new BufferedInputStream(inStream);
			this.inStreamReader = new InputStreamReader(inStream);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#getCharacterStream()
		 */
		@Override
		public Reader getCharacterStream() {
			// TODO Auto-generated method stub
			 System.out.println("##### getCharacterStream "+systemId);
			return inStreamReader;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#setCharacterStream(java.io.Reader)
		 */
		@Override
		public void setCharacterStream(Reader characterStream) {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#getByteStream()
		 */
		@Override
		public InputStream getByteStream() {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#setByteStream(java.io.InputStream)
		 */
		@Override
		public void setByteStream(InputStream byteStream) {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#getStringData()
		 */
		@Override
		public String getStringData() {
			synchronized (bufferedInStream) {
				try {
					byte[] input = new byte[bufferedInStream.available()];
					bufferedInStream.read(input);
					String contents = new String(input);
					 System.out.println("##### getStringData "+systemId);
					return contents;
				} catch (IOException e) {
					// e.printStackTrace();
					return null;
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#setStringData(java.lang.String)
		 */
		@Override
		public void setStringData(String stringData) {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#getSystemId()
		 */
		@Override
		public String getSystemId() {
			return systemId;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#setSystemId(java.lang.String)
		 */
		@Override
		public void setSystemId(String systemId) {
			this.systemId = systemId;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#getPublicId()
		 */
		@Override
		public String getPublicId() {
			return publicId;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#setPublicId(java.lang.String)
		 */
		@Override
		public void setPublicId(String publicId) {
			this.publicId = publicId;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#getBaseURI()
		 */
		@Override
		public String getBaseURI() {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#setBaseURI(java.lang.String)
		 */
		@Override
		public void setBaseURI(String baseURI) {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#getEncoding()
		 */
		@Override
		public String getEncoding() {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#setEncoding(java.lang.String)
		 */
		@Override
		public void setEncoding(String encoding) {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#getCertifiedText()
		 */
		@Override
		public boolean getCertifiedText() {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.w3c.dom.ls.LSInput#setCertifiedText(boolean)
		 */
		@Override
		public void setCertifiedText(boolean certifiedText) {
			// TODO Auto-generated method stub

		}

	}

	// ###################################################################
	/**
	 * Custom error handler used while validating xml against xsd. This class
	 * serves two purposes:
	 * <ol>
	 * <li>it allows validation to continue even after an error or warning
	 * condition, thereby allowing the entire XML file to be checked in one
	 * pass, and</li>
	 * <li>it provides condensed and easy to read versions of the error message.
	 * </li>
	 * </ol>
	 */
	public class XsdErrorHandler implements ErrorHandler {
		int errCount = 0;
		private File srcFile;

		/**
		 * @param srcFile
		 */
		public XsdErrorHandler(File srcFile) {
			this.srcFile = srcFile;
		}

		@Override
		public void warning(SAXParseException exception) throws SAXException {
			handleMessage(LogMgmt.LEV_WARN, exception);
		}

		@Override
		public void error(SAXParseException exception) throws SAXException {
			handleMessage(LogMgmt.LEV_ERR, exception);
			errCount++;
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			handleMessage(LogMgmt.LEV_FATAL, exception);
		}

		private void handleMessage(int level, SAXParseException exception) throws SAXException {
			int lineNumber = exception.getLineNumber();
			String message = parseSaxMessage(exception);
			String explanation = "XML at line: " + lineNumber + " does not comply with schema :: " + message;
			loggingMgr.log(level, LogMgmt.TAG_XSD, message, srcFile, lineNumber, "XmlIngester", explanation, null);
			// logIssue(LogMgmt.TAG_XSD, level, xmlEl, message, explanation,
			// null, "XmlIngester");

			if (level == LogMgmt.LEV_FATAL) {
				throw new SAXException(explanation);
			}
		}

		/**
		 * Return a shortened and simplified message
		 * 
		 * @param exception
		 * @return
		 */
		private String parseSaxMessage(SAXParseException exception) {
			String msgType1 = "Invalid content was found starting with element";
			String msgType2 = "is a simple type, so it cannot have attributes";
			String msgType3 = "The content of element '[\\w]+:[\\w]+' is not complete.";
			String message = exception.getMessage();
			String primary = getPrimary(message);
			String secondary = getSecondary(message);
			if (message.contains(msgType1)) {
				return "Invalid content begining with " + primary + "; expected " + secondary;
			}
			if (message.contains(msgType2)) {
				return "A " + primary + " may not have attributes";
			}
			Pattern p = Pattern.compile(msgType3);
			Matcher m = p.matcher(message);
			if (m.find()) {
				return "Incomplete " + primary + "; Missing child " + secondary;
			}
			// final default handling...
			// System.out.println("\n" + message);
			String msg = message.replaceFirst("cvc-[a-zA-Z0-9.\\-]+: ", "");
			return msg;
		}

		private String getPrimary(String text) {
			Pattern p = Pattern.compile("[a-z]+:[a-zA-Z\\-]+");
			Matcher m = p.matcher(text);
			if (m.find()) {
				return m.group();
			}
			Pattern p2 = Pattern.compile("'[a-zA-Z\\-]+'");
			m = p2.matcher(text);
			if (m.find()) {
				return m.group();
			}
			return "-UNKNOWN-";

		}

		private String getSecondary(String text) {
			Pattern p = Pattern.compile("[a-z]+\":[a-zA-Z\\-]+");
			Matcher m = p.matcher(text);
			String result = "";
			boolean hasAnother = m.find();
			String sep = "";
			while (hasAnother) {
				String next = m.group();
				hasAnother = m.find();
				if (hasAnother) {
					result = result + sep + next;
				} else if (result.isEmpty()) {
					result = next;
				} else {
					result = result + ", or " + next;
				}
				sep = ", ";
			}

			return result.replaceAll("\"", "");
		}
	}
}
