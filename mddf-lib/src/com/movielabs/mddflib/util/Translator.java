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

	/**
	 * @param xmlDoc
	 * @param xportFmts
	 * @param exportDir
	 * @param logMgr
	 */
	public static int translateAvails(Document xmlDoc, EnumSet<FILE_FMT> xportFmts, File exportDir, String filePrefix,
			LogMgmt logMgr) {
		String dirPath = exportDir.getAbsolutePath();
		return translateAvails(xmlDoc, xportFmts, dirPath, filePrefix, logMgr);
	}

	/**
	 * @param xmlDoc
	 * @param selections
	 * @param dirPath
	 * @param filePrefix
	 * @param logMgr
	 * @return
	 */
	public static int translateAvails(Document xmlDoc, EnumSet<FILE_FMT> selections, String dirPath, String filePrefix,
			LogMgmt logMgr) {
		Iterator<FILE_FMT> selIt = selections.iterator();
		int outputCnt = 0;
		while (selIt.hasNext()) {
			FILE_FMT targetFmt = selIt.next();
			switch (targetFmt) {
			case AVAILS_1_6:
				// not yet implemented. May never be.
				break;
			case AVAILS_1_7:
				/*
				 * A v1.7 spreadsheet should be generated from v2.2 XML. We need
				 * to 'up-convert' any XML that is using the v2.1 syntax. This
				 * will be the case if either (a) the source Avails was a v2.1
				 * XML file OR (b) the source Avails was a v1.6 Excel file.
				 */
				String nSpace = XmlIngester.identifyXsdVersion(xmlDoc.getRootElement());
				Document srcXmlDoc = null;
				switch (nSpace) {
				case "2.1":
					srcXmlDoc = avail2_1_to_2_2(xmlDoc);
					break;
				case "2.2":
					srcXmlDoc = xmlDoc;
					break;
				default:
					// Unsupported request
					break;
				}
				if (srcXmlDoc != null) {
					XlsxBuilder converter = new XlsxBuilder(srcXmlDoc.getRootElement(), Version.V1_7, logMgr);
					String fileName = filePrefix + "_v1.7.xlsx";
					File exported = new File(dirPath, fileName);
					try {
						converter.export(exported.getPath());
						outputCnt++;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				break;
			case AVAILS_2_2:
				/*
				 * what's the schema used for the existing XML representation?
				 * It may already be a match for the desired format.
				 */
				nSpace = XmlIngester.identifyXsdVersion(xmlDoc.getRootElement());
				srcXmlDoc = null;
				switch (nSpace) {
				case "2.1":
					srcXmlDoc = avail2_1_to_2_2(xmlDoc);
					break;
				case "2.2":
					srcXmlDoc = xmlDoc;
					break;
				default:
					// Unsupported request
					break;
				}
				if (srcXmlDoc != null) {
					String fileName = filePrefix + "_v2.2.xml";
					File exported = new File(dirPath, fileName);
					// Save as XML
					if (XmlIngester.writeXml(exported, srcXmlDoc)) {
						outputCnt++;
					}
				}
			}

		}
		return outputCnt;

	}

	/**
	 * @param xmlDoc
	 * @return
	 */
	private static Document avail2_1_to_2_2(Document xmlDocIn) {
		XmlIngester.writeXml(new File("./tmp/Conversion-TEST.xml"), xmlDocIn);
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
		StringReader sReader = new StringReader(outDoc);
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
	 * Returns text suitable for describing capabilities and usage of
	 * translation functions. Text is formated for incorporation in <tt>man</tt>
	 * and Help pages.
	 * 
	 * @return
	 */
	public static String getHelp() {
		String helpTxt = "The formats available are dependant on the type of MDDF file.\n"
				+ "For AVAILS the following formats may be specified:\n" + "-- AVAILS_1_7: Excel using v1.7 template.\n"
				+ "-- AVAILS_2_2: XML using v2.2 schema.";

		return helpTxt;
	}
}
