/**
 * Copyright (c) 2017 MovieLabs

 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.movielabs.mddflib.util.xml;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddflib.logging.LogMgmt;

/**
 * A 'helper' class that supports the validation of an XML file against an XSD
 * specification.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class XsdValidation {
	public static String defaultRsrcLoc;
	private static int logMsgDefaultTag = LogMgmt.TAG_XSD;

	static {
		/*
		 * This will be used with ClassLoader.getResource() so the path is always
		 * absolute. Hence, no leading '/'
		 */
		defaultRsrcLoc = SchemaWrapper.RSRC_PACKAGE;
		if (defaultRsrcLoc.startsWith("/")) {
			defaultRsrcLoc = defaultRsrcLoc.replaceFirst("/", "");
		}
	}

	private LogMgmt loggingMgr;
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++
	private Validator validator;

	public XsdValidation(LogMgmt loggingMgr) {
		this.loggingMgr = loggingMgr;
	}

	/**
	 * Validate everything that is fully specified via the identified XSD.
	 * 
	 * @param target
	 * @param xsdLocation
	 * @param moduleId
	 * @return
	 */
	public boolean validateXml(MddfTarget target, String xsdLocation, String moduleId) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		URL xsdUrl = assignXSD(target);
		if (!validateHeader(target, xsdUrl)) {
			//return false;
		}
		String genericTooltip = "XML does not conform to schema as defined in " + xsdLocation;
		File srcFile = target.getSrcFile(); // used for logging
		Schema schema;
		try {
			schema = schemaFactory.newSchema(xsdUrl);
		} catch (SAXParseException e1) {
			String msg = "Unable to process: " + e1.getMessage();
			msg = msg.replace("schema_reference.4", "");
			loggingMgr.log(LogMgmt.LEV_FATAL, logMsgDefaultTag, msg, target, null, moduleId, genericTooltip, null);
			e1.printStackTrace();
			return false;
		} catch (SAXException e1) {
			e1.printStackTrace();
			return false;
		}
		/*
		 * Use a custom ErrorHandler for all SAXParseExceptions
		 */
		XsdErrorHandler errHandler = new XsdErrorHandler(target);
		// now do actual validation
		try {
			validator = schema.newValidator();
			validator.setErrorHandler(errHandler);
			/**
			 * <p>
			 * This block of code handles a problem associated with supporting the
			 * validation of XML generated from an Excel spreadsheet. Regardless of whether
			 * the XML comes from an XML file or is internally generated via the
			 * transformation of an xlsx file, we still have (at this point in processing) a
			 * JDOM document. In theory the obvious approach is to use a JDOMSource in all
			 * cases. In practice, however, using a JDOMSource will result in
			 * SaxParseException messages that lack line numbers. This greatly reduces the
			 * value of the error messages. On the other hand, if the MDDF file started out
			 * in a non-XML syntax and then was converted to XML, linking an error to a
			 * specific line in the XML has little, if any, value.
			 * </p>
			 * <p>
			 * The solution is to use a StreamSource when processing something that started
			 * as XML on the file system even though we already have the same XML in the
			 * form of the JDOM document. The JDOMSource is used only if the original MDDF
			 * file was not formatted as XML (i.e., it is an XLSX formatted Avails).
			 * </p>
			 */
			Source src;
			if (srcFile.getName().endsWith(".xml")) {
				src = new StreamSource(target.getXmlStreamSrc());
			} else {
				src = new JDOMSource(target.getXmlDoc().getRootElement());
			}
			validator.validate(src);
		} catch (IOException e) {
			String msg = "Validation error -::" + getExceptionCause(e);
			loggingMgr.log(LogMgmt.LEV_ERR, logMsgDefaultTag, msg, target, -1, moduleId, genericTooltip, null);
			return (false);
		} catch (SAXException e) {
			String msg = "Validation error -::" + getExceptionCause(e);
			loggingMgr.log(LogMgmt.LEV_ERR, logMsgDefaultTag, msg, target, -1, moduleId, genericTooltip, null);
			return (false);
		}
		if (errHandler.errCount == 0) {
			loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, "XML is valid", target, -1, moduleId, null, null);
			return (true);
		} else {
			loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, "Invalid XML, " + errHandler.errCount + " errors found",
					target, -1, moduleId, null, null);
			return (false);
		}
	}

	/**
	 * Handle a situation specific to Common Metadata 2.7.1 usage
	 * 
	 * @param target
	 * @return
	 */
	private URL assignXSD(MddfTarget target) {
		Element rootEl = target.getXmlDoc().getRootElement();
		List<Namespace> nsList = rootEl.getNamespacesInScope();
		Map<String, Namespace> nsMap = new HashMap();
		for (Namespace nextNs : nsList) {
			nsMap.put(nextNs.getPrefix(), nextNs);
		}
		// first ns in the list is the main one
		String mddfType = nsList.get(0).getPrefix();
		String xsdVer = XmlIngester.identifyXsdVersion(rootEl);
		// get the version used in the URI with the 'md' prefix:
//		String mdUri = nsMap.get("md").getURI();
//		String mdVer = extractVersionFromUri(mdUri, "md");
		switch (mddfType) {
		case "manifest":
			switch (xsdVer) {
			case "1.8":
				// is schema location used to 'alias v1.8.1?
				String schemaLoc = rootEl.getAttributeValue("schemaLocation", nsMap.get("xsi"));
				if ((schemaLoc != null) && (schemaLoc.contains("v1.8.1"))) {
					xsdVer = "1.8.1";
				} else {
					xsdVer = "1.8";
				}
			}
			break;
		case "mdmec":
			switch (xsdVer) {
			case "2.7":
				// is schema location used to 'alias v2.7.1?
				String schemaLoc = rootEl.getAttributeValue("schemaLocation", nsMap.get("xsi"));
				if ((schemaLoc != null) && (schemaLoc.contains("v2.7.1"))) {
					xsdVer = "2.7.1";
				} else {
					xsdVer = "2.7";
				}
			}
			break;
		case "avails":
			break;
		default:
			break;
		}
		String xsdFile = XsdValidation.defaultRsrcLoc + mddfType + "-v" + xsdVer + ".xsd";

		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Assigned XSD file " + xsdFile, target, -1,
				"XsdValidation", null, null);

		URL xsdUrl = getClass().getClassLoader().getResource(xsdFile);
		return xsdUrl;
	}

	/**
	 * Check that the namespace declarations in the MDDF XML file match those used
	 * in the XSD. This is explicitly verified so as to provide an unambiguous error
	 * message in situations where the namespaces do NOT match.
	 * 
	 * @param target
	 * @param xsdUrl
	 * @return
	 */
	private boolean validateHeader(MddfTarget target, URL xsdUrl) {
		boolean status = true;
		Element xmlRoot = target.getXmlDoc().getRootElement();
		String xsdPath = xsdUrl.getPath();
		String xsd = new File(xsdPath).getName();
		xsd = xsd.replace(".xsd", "");
		SchemaWrapper schema = SchemaWrapper.factory(xsd);
		List<Namespace> usedNSpaceList = xmlRoot.getNamespacesInScope();
		for (Namespace nextNSpace : usedNSpaceList) {
			String prefix = nextNSpace.getPrefix();
			switch (prefix) {
			case "":
			case "xml":
			case "xs":
			case "xsi":
				continue;

			}
			SchemaWrapper refSchema = schema.getReferencedSchema(prefix);
			if (refSchema == null) {
				// XML refers to an unknown schema
				loggingMgr.log(LogMgmt.LEV_FATAL, logMsgDefaultTag, "Use of unsupported Namespace: prefix=" + prefix,
						target, -1, "XsdValidation", null, null);
				status = false;
				continue;
			}
			// make sure the two URIs match
			String uriInXml = nextNSpace.getURI();
			String uriInXsd = refSchema.getTargetNamespace().getURI();
			if (!uriInXml.equals(uriInXsd)) {
				String errMsg = "Invalid URI for Namespace: URI=" + uriInXml;
				String details = "The URI must exactly match the URI used in the XSD";
				loggingMgr.log(LogMgmt.LEV_FATAL, logMsgDefaultTag, errMsg, target, -1, "XsdValidation",
						null, null);
				status = false;
			}
		}
		return status;
	}

	protected String extractVersionFromUri(String nSpaceUri, String mddfType) {
		String schemaPrefix = MddfContext.SCHEMA_PREFIX + mddfType + "/v";
		String schemaVer = nSpaceUri.replace(schemaPrefix, "");
		schemaVer = schemaVer.replace("/" + mddfType, "");
		return schemaVer;
	}

	protected static String getExceptionCause(Exception e) {
		String description = e.getMessage();
		Throwable cause = e.getCause();
		while (cause != null) {
			description = cause.getMessage();
			cause = cause.getCause();
		}
		return description;
	}

	// ###################################################################

	// ###################################################################

	/**
	 * Custom error handler used while validating xml against xsd. This class serves
	 * two purposes:
	 * <ol>
	 * <li>it allows validation to continue even after an error or warning
	 * condition, thereby allowing the entire XML file to be checked in one pass,
	 * and</li>
	 * <li>it provides condensed and easy to read versions of the error message.
	 * </li>
	 * </ol>
	 */
	public class XsdErrorHandler implements ErrorHandler {
		int errCount = 0;
		private MddfTarget target;

		/**
		 * @param srcFile
		 */
		public XsdErrorHandler(MddfTarget target) {
			this.target = target;
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
			loggingMgr.log(level, LogMgmt.TAG_XSD, message, target, lineNumber, "XsdValidation", explanation, null);
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
			String msgType3 = "'([a-z]+:)?+\\w+' is not complete";
			// "The content of element '[\\w]+:[\\w]+' is not complete.";
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
			Pattern p = Pattern.compile("([a-z]+:)?+\\w+'");
			Matcher m = p.matcher(text);
			if (m.find()) {
				return m.group();
			}
			return "-UNKNOWN-";

		}

		private String getSecondary(String text) {
			String uriPrefixRegex = "\"(http:/){1}(/[a-zA-Z\\d\\.]+)+\":?";
			String elementName = "[a-zA-Z\\d]+";
			Pattern p = Pattern.compile(uriPrefixRegex + elementName);
			Matcher m = p.matcher(text);
			String result = "";
			boolean hasAnother = m.find();
			String sep = "";
			while (hasAnother) {
				String next = m.group();
				next = next.replaceFirst(uriPrefixRegex, "");
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
