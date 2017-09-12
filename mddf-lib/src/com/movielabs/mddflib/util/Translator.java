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
package com.movielabs.mddflib.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.avails.xlsx.XlsxBuilder;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * Performs translation of Avails from one format or version to another and then
 * exports (i.e., saves) the translated file(s).
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class Translator {
	
	private static String moduleId = "Translator";

	/**
	 * @param xmlDoc
	 * @param xportFmts
	 * @param exportDir
	 * @param filePrefix
	 * @param logMgr
	 * @return
	 * @throws UnsupportedOperationException
	 */
	public static int translateAvails(Document xmlDoc, EnumSet<FILE_FMT> xportFmts, File exportDir, String filePrefix,
			boolean appendVersion, LogMgmt logMgr) throws UnsupportedOperationException {
		String dirPath = exportDir.getAbsolutePath();
		return translateAvails(xmlDoc, xportFmts, dirPath, filePrefix, appendVersion, logMgr);
	}

	/**
	 * @param xmlDoc
	 * @param selections
	 * @param dirPath
	 * @param outFileName
	 * @param appendVersion
	 * @param logMgr
	 * @return
	 * @throws UnsupportedOperationException
	 */
	public static int translateAvails(Document xmlDoc, EnumSet<FILE_FMT> selections, String dirPath, String outFileName,
			boolean appendVersion, LogMgmt logMgr) throws UnsupportedOperationException {
		Iterator<FILE_FMT> selIt = selections.iterator();
		int outputCnt = 0;
		while (selIt.hasNext()) {
			FILE_FMT targetFmt = selIt.next();
			if (targetFmt.getEncoding().equalsIgnoreCase("xlsx")) {
				if (convertToExcel(xmlDoc, targetFmt, dirPath, outFileName, appendVersion, logMgr)) {
					outputCnt++;
				}
			} else {
				Document targetDoc = convertToXml(xmlDoc, targetFmt, logMgr);
				if (targetDoc != null) {
					// did it work? if so, write to file system.
					if (targetDoc != null) {
						String fileName = outFileName;
						if (appendVersion) {
							fileName = fileName + "_v" + targetFmt.getVersion() + ".xml";
						} else {
							fileName = fileName + ".xml";
						}
						File exported = new File(dirPath, fileName);
						// Save as XML
						if (XmlIngester.writeXml(exported, targetDoc)) {
							logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE, "Saved translated file as "+exported.getPath(), null, moduleId);
							outputCnt++;
						}
					}
				}
			}
		}
		return outputCnt;
	}

	/**
	 * Convert an XML Avails doc from one version to another.
	 * 
	 * @param srcDoc
	 * @param targetFmt
	 * @param dirPath
	 * @param filePrefix
	 * @param logMgr
	 * @return
	 */
	private static Document convertToXml(Document srcDoc, FILE_FMT targetFmt, LogMgmt logMgr)
			throws UnsupportedOperationException {
		Document targetDoc = null;
		/*
		 * what's the schema used for the existing XML representation? It may
		 * already be a match for the desired format.
		 */
		String curVersion = XmlIngester.identifyXsdVersion(srcDoc.getRootElement());
		String targetVersion = targetFmt.getVersion();
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE, "Translating to XML v"+targetFmt.getVersion()+" from XML v"+curVersion, null, moduleId);
		if (curVersion.equals(targetVersion)) {
			targetDoc = srcDoc;
		} else {
			switch (targetFmt) {
			case AVAILS_2_1:
				// not supported as a target format
				break;
			case AVAILS_2_2:
				switch (curVersion) {
				case "2.1":
					targetDoc = avail2_1_to_2_2(srcDoc);
					return targetDoc;
				case "2.2.1":
					targetDoc = avail2_2_1_to_2_2(srcDoc);
					return targetDoc;
				case "2.2.2":
				default:
					// Unsupported request
					break;
				}
			case AVAILS_2_2_1:
				switch (curVersion) {
				case "2.1":
					targetDoc = avail2_1_to_2_2(srcDoc);
					targetDoc = avail2_2_to_2_2_1(targetDoc);
					return targetDoc;
				case "2.2":
					targetDoc = avail2_2_to_2_2_1(srcDoc);
					return targetDoc;
				case "2.2.2":
					targetDoc = avail2_2_2_to_2_2_1(srcDoc);
					return targetDoc;
				default:
					// Unsupported request
					break;
				}
			case AVAILS_2_2_2:
				switch (curVersion) {
				case "2.1":
					targetDoc = avail2_1_to_2_2(srcDoc);
					targetDoc = avail2_2_to_2_2_2(targetDoc);
					return targetDoc;
				case "2.2":
					targetDoc = avail2_2_to_2_2_2(srcDoc);
					return targetDoc;
				default:
					// Unsupported request
					break;
				}
			}
		}
		throw new UnsupportedOperationException("Conversion to Avails " + targetFmt.getEncoding() + " "
				+ targetFmt.name() + " from v" + curVersion + " not supported");
	}

	/**
	 * Handle conversion of an XML formatted Avails to an Excel formated Avails.
	 * 
	 * @param xmlSrcDoc
	 * @param targetFormat
	 * @param dirPath
	 * @param outFileName
	 * @param appendVersion 
	 * @param logMgr
	 * @return
	 * @throws UnsupportedOperationException
	 */
	private static boolean convertToExcel(Document xmlSrcDoc, FILE_FMT targetFormat, String dirPath, String outFileName,
			boolean appendVersion, LogMgmt logMgr) throws UnsupportedOperationException {
		Document xmlDoc = null;
		String curVersion = XmlIngester.identifyXsdVersion(xmlSrcDoc.getRootElement());
		Version excelVer = null;
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE, "Translating to Excel v"+targetFormat.getVersion()+" from XML v"+curVersion, null, moduleId);
		switch (targetFormat) {
		case AVAILS_1_6:
			// not yet implemented. May never be.
			break;
		case AVAILS_1_7:
			excelVer = Version.V1_7;
			/*
			 * A v1.7 spreadsheet should be generated from v2.2 XML.
			 */
			switch (curVersion) {
			case "2.1":
				xmlDoc = avail2_1_to_2_2(xmlSrcDoc);
				break;
			case "2.2":
				xmlDoc = xmlSrcDoc;
				break;
			case "2.2.1":
				xmlDoc = avail2_2_1_to_2_2(xmlSrcDoc);
				break;
			default:
				// Unsupported request
				break;
			}
			break;
		case AVAILS_1_7_2:
			excelVer = Version.V1_7_2;
			/*
			 * A v1.7.2 spreadsheet should be generated from v2.2.2 XML.
			 */
			switch (curVersion) {
			case "2.1":
				xmlDoc = avail2_1_to_2_2(xmlSrcDoc);
				xmlDoc = avail2_2_to_2_2_2(xmlDoc);
				break;
			case "2.2":
				xmlDoc = avail2_2_to_2_2_2(xmlSrcDoc);
				break;
			case "2.2.1":
				xmlDoc = avail2_2_1_to_2_2(xmlSrcDoc);
				xmlDoc = avail2_2_to_2_2_2(xmlDoc);
				break;
			default:
				// Unsupported request
				break;
			}
			break;
		}
		if (xmlDoc != null) {
			XlsxBuilder converter = new XlsxBuilder(xmlDoc.getRootElement(), excelVer, logMgr);
			String fileName = outFileName;
			if (appendVersion) {
				fileName = fileName + "_v" + targetFormat.getVersion() + ".xlsx";
			} else {
				fileName = fileName + ".xlsx";
			} 
			File exported = new File(dirPath, fileName);
			try {
				converter.export(exported.getPath()); 
				logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE, "Saved translated file as "+exported.getPath(), null, moduleId);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			throw new UnsupportedOperationException(
					"Conversion to Avails xlsx " + targetFormat.name() + " from XML v" + curVersion + " not supported");
		}
		return false;
	}

	/**
	 * 
	 * @param xmlDocIn
	 * @return
	 */
	private static Document avail2_1_to_2_2(Document xmlDocIn) {
		// XmlIngester.writeXml(new File("./tmp/Conversion-TEST.xml"),
		// xmlDocIn);
		/*
		 * STAGE ONE: some changes are easiest to do by converting the Doc to a
		 * string and then doing string replacements prior to converting back to
		 * Doc form.
		 */
		XMLOutputter outputter = new XMLOutputter();
		String inDoc = outputter.outputString(xmlDocIn);
		// Change Namespace declaration
		String t1 = inDoc.replaceFirst("/avails/v2.1/avails", "/avails/v2.2/avails");
		String outDoc = t1.replaceAll(":StoreLanguage>", ":AssetLanguage>");
		// Now gen a new doc
		SAXBuilder builder = new SAXBuilder();
		builder.setJDOMFactory(new LocatedJDOMFactory());
		Document xmlDocOut = regenXml(outDoc);
		/*
		 * Stage TWO is to do anything that is easier to handle via manipulation
		 * of the XML.
		 */
		// ...........
		XPathFactory xpfac = XPathFactory.instance();
		Namespace availsNSpace = Namespace.getNamespace("avails",
				MddfContext.NSPACE_AVAILS_PREFIX + "2.2" + MddfContext.NSPACE_AVAILS_SUFFIX);
		/*
		 * Find all Transaction/Term[[@termName='HoldbackExclusionLanguage'].
		 * These need to be removed from the XML and then replaced with a
		 * Transaction/AllowedLanguage element. Luckily, the location of the new
		 * AllowedLanguage element is easy to pinpoint.... it follows
		 * immediately after a REQUIRED element (i.e., either an <End> or
		 * <EndCondition>).
		 */
		String helTermPath = "//avails:Transaction/avails:Term[./@termName='HoldbackExclusionLanguage']";
		XPathExpression<Element> helTermPathExp = xpfac.compile(helTermPath, Filters.element(), null, availsNSpace);
		List<Element> helElList = helTermPathExp.evaluate(xmlDocOut.getRootElement());
		for (Element termEl : helElList) {
			// find insert point
			Element transEl = termEl.getParentElement();
			Element target = transEl.getChild("End", availsNSpace);
			if (target == null) {
				target = transEl.getChild("EndCondition", availsNSpace);
			}
			int insertPoint = transEl.indexOf(target) + 1;
			termEl.detach();
			String lang = termEl.getChildText("Language", availsNSpace);
			Element allowedEl = new Element("AllowedLanguage", availsNSpace);
			allowedEl.setText(lang);
			transEl.addContent(insertPoint, allowedEl);
		}
		return xmlDocOut;
	}

	/**
	 * @param xmlDocIn
	 * @return
	 */
	private static Document avail2_2_to_2_2_1(Document xmlDocIn) {
		/*
		 * STAGE ONE: some changes are easiest to do by converting the Doc to a
		 * string and then doing string replacements prior to converting back to
		 * Doc form.
		 */
		XMLOutputter outputter = new XMLOutputter();
		String inDoc = outputter.outputString(xmlDocIn);
		// Change Namespace declaration
		String t1 = inDoc.replaceFirst("/avails/v2.2/avails", "/avails/v2.2.1/avails");
		String outDoc = t1.replaceFirst("/schema/md/v2.4/md", "/schema/md/v2.5/md");
		// Now gen a new doc
		return regenXml(outDoc);
	}

	/**
	 * 
	 * 
	 * @param xmlDocIn
	 * @return
	 */
	private static Document avail2_2_1_to_2_2(Document xmlDocIn) {
		/*
		 * STAGE ONE: some changes are easiest to do by converting the Doc to a
		 * string and then doing string replacements prior to converting back to
		 * Doc form.
		 */
		XMLOutputter outputter = new XMLOutputter();
		String inDoc = outputter.outputString(xmlDocIn);
		// Change Namespace declaration
		String t1 = inDoc.replaceFirst("/avails/v2.2.1/avails", "/avails/v2.2/avails");
		String t2 = t1.replaceFirst("/schema/md/v2.5/md", "/schema/md/v2.4/md");
		// Now gen a new doc
		Document xmlDocOut = regenXml(t2);
		/*
		 * remove any elements or attributes present in v2.2.1 but not in v2.2
		 */
		Element rootEl = xmlDocOut.getRootElement();
		String targetPath = "/avails:AvailList/avails:Avail/avails:Transaction/avails:AssetLanguage[@asset]";
		removeAttribute(targetPath, "asset", rootEl);
		targetPath = "/avails:AvailList/avails:Avail/avails:Transaction/avails:AssetLanguage[@descriptive]";
		removeAttribute(targetPath, "descriptive", rootEl);
		targetPath = "/avails:AvailList/avails:Avail/avails:Transaction/avails:AllowedLanguage[@asset]";
		removeAttribute(targetPath, "asset", rootEl);

		return xmlDocOut;
	}

	/**
	 * @param xmlInputDoc
	 * @return
	 */
	private static Document avail2_2_to_2_2_2(Document xmlDocIn) {

		XmlIngester.writeXml(new File("./tmp/Conversion-TEST.xml"), xmlDocIn);

		XMLOutputter outputter = new XMLOutputter();
		String inDoc = outputter.outputString(xmlDocIn);
		// Change Namespace declaration
		String t1 = inDoc.replaceFirst("/avails/v2.2/avails", "/avails/v2.2.2/avails");
		String outDoc = t1.replaceFirst("/schema/md/v2.4/md", "/schema/md/v2.5/md");
		/*
		 * v2.2.2 adds several new elements and attributes. However, since all
		 * are optional no further changes are required.
		 */
		return regenXml(outDoc);
	}

	/**
	 * @param srcDoc
	 * @return
	 */
	private static Document avail2_2_2_to_2_2_1(Document xmlDocIn) {
		XMLOutputter outputter = new XMLOutputter();
		String inDoc = outputter.outputString(xmlDocIn);
		// Change Namespace declaration
		String t1 = inDoc.replaceFirst("/avails/v2.2.2/avails", "/avails/v2.2.1/avails");
		/*
		 * v2.2.2 adds several elements and attributes missing in v2.2.1. These
		 * need to be removed
		 */
		Document xmlDocOut = regenXml(t1);
		Element rootEl = xmlDocOut.getRootElement();
		Namespace availsNSpace = rootEl.getNamespace("avails");
		XPathFactory xpfac = XPathFactory.instance();
		String targetPath = "//avails:People";
		XPathExpression<Element> pathExp = xpfac.compile(targetPath, Filters.element(), null, availsNSpace);
		List<Element> removalList = pathExp.evaluate(rootEl);
		for (Element targetEl : removalList) {
			targetEl.detach();
		}
		targetPath = "//avails:GroupingEntity";
		pathExp = xpfac.compile(targetPath, Filters.element(), null, availsNSpace);
		removalList = pathExp.evaluate(rootEl);
		for (Element targetEl : removalList) {
			targetEl.detach();
		}
		targetPath = "/avails:AvailList/avails:Avail/avails:Transaction/*[@lag]";
		removeAttribute(targetPath, "lag", rootEl);
		targetPath = "/avails:AvailList/avails:Avail/avails:Transaction/avails:AssetLanguage[@assetProvided]";
		removeAttribute(targetPath, "assetProvided", rootEl);
		targetPath = "/avails:AvailList/avails:Avail/avails:Transaction/avails:AssetLanguage[@metadataProvided]";
		removeAttribute(targetPath, "metadataProvided", rootEl);
		/*
		 * v2.2.2 allows multiple instances of TitleInternalAlias for different
		 * regions as well as multiple instances of TitleDisplayUnlimited for
		 * different languages (also for Season and Series). If multiple
		 * instances are present, all but 1st is removed and a WARNING is
		 * issues.
		 */
		targetPath = "/avails:AvailList/avails:Avail/avails:Asset//*[count(avails:TitleInternalAlias) > 1 ]";
		pathExp = xpfac.compile(targetPath, Filters.element(), null, availsNSpace);
		removalList = pathExp.evaluate(rootEl);
		for (Element targetEl : removalList) {
			List<Element> extrasList = targetEl.getChildren("TitleInternalAlias", availsNSpace);
			// keep the 1st and detach the rest
			for (int i = 1; i < extrasList.size(); i++) {
				Element extra = extrasList.get(i);
				extra.detach();
			}
		}
		targetPath = "/avails:AvailList/avails:Avail/avails:Asset//*[count(avails:TitleDisplayUnlimited) > 1 ]";
		pathExp = xpfac.compile(targetPath, Filters.element(), null, availsNSpace);
		removalList = pathExp.evaluate(rootEl);
		for (Element targetEl : removalList) {
			List<Element> extrasList = targetEl.getChildren("TitleDisplayUnlimited", availsNSpace);
			// keep the 1st and detach the rest
			for (int i = 1; i < extrasList.size(); i++) {
				Element extra = extrasList.get(i);
				extra.detach();
			}

		}
		return xmlDocOut;
	}

	/**
	 * Identify all elements matching the specified xpath and then detach the
	 * identified attribute if it exists. If <tt>attName == '*'</tt> all
	 * attributes are removed.
	 * 
	 * @param targetPath
	 * @param attName
	 * @param rootEl
	 */
	private static void removeAttribute(String targetPath, String attName, Element rootEl) {
		XPathFactory xpfac = XPathFactory.instance();
		Namespace availsNSpace = rootEl.getNamespace("avails");
		XPathExpression<Element> pathExp = xpfac.compile(targetPath, Filters.element(), null, availsNSpace);
		List<Element> removalList = pathExp.evaluate(rootEl);
		for (Element targetEl : removalList) {
			if (attName.equals("*")) {
				List<Attribute> attList = targetEl.getAttributes();
				for (Attribute att : attList) {
					att.detach();
				}
			} else {
				targetEl.removeAttribute(attName);
			}
		}

	}

	/**
	 * Converts a string containing an entire XML document to a JDOM
	 * <tt>Document</tt>
	 * 
	 * @param xmlAsString
	 * @return
	 */
	private static Document regenXml(String xmlAsString) {
		StringReader sReader = new StringReader(xmlAsString);
		SAXBuilder builder = new SAXBuilder();
		builder.setJDOMFactory(new LocatedJDOMFactory());
		Document xmlDocOut = null;
		try {
			xmlDocOut = builder.build(sReader);
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return xmlDocOut;
	}

	/**
	 * Returns text suitable for describing capabilities and usage of
	 * translation functions. Text is formated for incorporation in <tt>man</tt>
	 * and Help pages.
	 * 
	 * @return
	 */
	public static String getHelp() {
		String helpTxt = "The formats available are dependant on the type of MDDF file.\n"
				+ "For AVAILS the following formats may be specified:\n" + "-- AVAILS_1_7: Excel using v1.7 template.\n"
				+ "-- AVAILS_1_7_2: Excel using v1.7.2 template.\n" + "-- AVAILS_2_2: XML using v2.2 schema.";

		return helpTxt;
	}
}
