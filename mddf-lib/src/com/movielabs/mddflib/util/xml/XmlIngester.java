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
import java.util.HashMap;
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
import com.movielabs.mddf.MddfContext.FILE_FMT;
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
	public static String CM_VER = "2.3";
	public static String MDMEC_VER = "2.4";
	public static String MAN_VER = "1.5";
	public static String AVAIL_VER = "2.1";
	public static Namespace xsiNSpace = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

	public static Namespace mdNSpace = Namespace.getNamespace("md",
			MddfContext.NSPACE_CMD_PREFIX + CM_VER + MddfContext.NSPACE_CMD_SUFFIX);

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

	public static JSONObject getMddfResource(String rsrcId, String version) {
		String rsrcKey = rsrcId + "_v" + version;
		JSONObject jsonRsrc = getMddfResource(rsrcKey);
		return jsonRsrc;
	}

	public static JSONObject getMddfResource(String rsrcId) {
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
		return rsrc;
	}

	/**
	 * Return resource defining a version-specific controlled vocabulary set.
	 * 
	 * @param rsrcId
	 * @param rsrcVersion
	 * @return a JSONObject or <tt>null</tt. if the resource is not accessible
	 *         or is not valid JSON
	 */
	protected static Object getVocabResource(String rsrcId, String rsrcVersion) {
		String key = rsrcId + "_v" + rsrcVersion;
		/*
		 * Check to see if a version is compatible with earlier version
		 */
		switch (key) {
		case "manifest_v1.6.1":
			key = "manifest_v1.6";
			break;
		case "cm_v2.6":
			key = "cm_v2.5";
			break;
		}
		String rsrcPath = MddfContext.RSRC_PATH + "vocab_" + key + ".json";
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
	 * (i.e., Manifest, Avails, MDMec, etc.). Version is returned as a string
	 * that <i>may</i> contain major and minor version identification (e.g.
	 * '2.1', '1.7.3_rc1')
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
		FILE_FMT manifestFmt = MddfContext.identifyMddfFormat("manifest", manifestSchemaVer);
		if(manifestFmt == null){
			throw new IllegalArgumentException("Unsupported Manifest Schema version " + manifestSchemaVer);
		}
		Map<String, String> uses = MddfContext.getReferencedXsdVersions(manifestFmt); 		 
		MAN_VER = manifestSchemaVer;
		CM_VER = uses.get("MD");
		/* Since MDMEC isn't used for Manifest, set to NULL */
		MDMEC_VER = null;
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + CM_VER + "/md");
		manifestNSpace = Namespace.getNamespace("manifest",
				MddfContext.NSPACE_MANIFEST_PREFIX + MAN_VER + MddfContext.NSPACE_MANIFEST_SUFFIX);
	}

	/**
	 * @param mecSchemaVer
	 * @throws IllegalArgumentException
	 */
	public static void setMdMecVersion(String mecSchemaVer) throws IllegalArgumentException {
		FILE_FMT mecFmt = MddfContext.identifyMddfFormat("mdmec", mecSchemaVer);
		if(mecFmt == null){
			throw new IllegalArgumentException("Unsupported MEC Schema version " + mecSchemaVer);
		}
		Map<String, String> uses = MddfContext.getReferencedXsdVersions(mecFmt); 		
		MDMEC_VER = mecSchemaVer;
		CM_VER = uses.get("MD");
		/* Since Manifest isn't used for MEC, set to NULL */
		MAN_VER = null;
		mdmecNSpace = Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v" + MDMEC_VER + "/mdmec");
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + CM_VER + "/md");
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
		FILE_FMT availsFmt = MddfContext.identifyMddfFormat("avails", availSchemaVer);
		if(availsFmt == null){
			throw new IllegalArgumentException("Unsupported Avails Schema version " + availSchemaVer);
		}
		Map<String, String> uses = MddfContext.getReferencedXsdVersions(availsFmt); 
		AVAIL_VER = availSchemaVer; 
		CM_VER = uses.get("MD");
		MDMEC_VER = uses.get("MDMEC");
		/* Since Manifest isn't used for Avails, set to NULL */
		MAN_VER = null;
		mdmecNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/mdmec/v" + MDMEC_VER + "/mdmec");
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + CM_VER + "/md");
		availsNSpace = Namespace.getNamespace("avails",
				"http://www.movielabs.com/schema/avails/v" + AVAIL_VER + "/avails");
	}

}
