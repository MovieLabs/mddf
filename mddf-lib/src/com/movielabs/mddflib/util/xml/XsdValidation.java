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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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
	private static boolean streamModeA;
	private static int logMsgDefaultTag = LogMgmt.TAG_XSD;

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

	private LogMgmt loggingMgr;
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++

	public XsdValidation(LogMgmt loggingMgr) {
		this.loggingMgr = loggingMgr;
	}

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
	public boolean validateXml(File srcFile, Element docRootEl, String xsdLocation, String moduleId) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		if (!streamModeA) {
			System.out.println("Loading XSD resource using Mode 'B'");
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
		} catch (SAXParseException e1) {
			String msg = "Unable to process" + e1.getMessage();
			msg = msg.replace("schema_reference.4", "");
			loggingMgr.log(LogMgmt.LEV_FATAL, logMsgDefaultTag, msg, srcFile, -1, moduleId, genericTooltip, null);
			return false;
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
			System.out.println("##### getCharacterStream " + systemId);
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
					System.out.println("##### getStringData " + systemId);
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
			InputStream inStream = ResourceResolver.class.getResourceAsStream(SchemaWrapper.RSRC_PACKAGE + systemId);
			System.out.println("resolveResource:: namespaceURI=" + namespaceURI + ", systemId=" + systemId);
			return new Input(publicId, systemId, inStream);
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
