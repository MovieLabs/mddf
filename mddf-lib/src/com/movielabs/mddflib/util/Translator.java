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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jdom2.Attribute;
import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedComment;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.avails.xlsx.TemplateWorkBook;
import com.movielabs.mddflib.avails.xlsx.XlsxBuilder;
import com.movielabs.mddflib.avails.xlsx.XlsxBuilderV1_8;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * Performs translation of an Avails file from one format or version to another
 * and then exports (i.e., saves) the translated file(s).
 * 
 * <h3>Peer-to-Peer Translations:</h3>
 * <p>
 * <i>Peer-to-peer translation</i> refers to the conversion between equivalent
 * release of the XML and XLSX Avails specifications. The supported pairings
 * are:
 * 
 * 
 * <table summary="">
 * <tr>
 * <td><b>XML</b></td>
 * <td>&lt;----&gt;</td>
 * <td><b>XLSX</b></td>
 * </tr>
 * <tr>
 * <td>v2.2</td>
 * <td></td>
 * <td>1.7</td>
 * </tr>
 * <tr >
 * <td>v2.2.2</td>
 * <td></td>
 * <td>v1.7.2</td>
 * </tr>
 * <tr>
 * <td>v2.3</td>
 * <td></td>
 * <td>v1.7.3</td>
 * </tr>
 * <tr>
 * <td>v2.4</td>
 * <td></td>
 * <td>v1.8</td>
 * </tr>
 * <tr>
 * <td>v2.5</td>
 * <td></td>
 * <td>v1.9</td>
 * </tr>
 * </table>
 * 
 * </p>
 * 
 * <h3>Version Translations:</h3>
 * 
 * <p>
 * <i>Version translation</i> refers to the conversion between different
 * releases of the XML and XLSX Avails specifications. Version translation is
 * only supported from an older version to a newer version (e.g., v2.3 to v2.4).
 * Translation is also supported to the peer-encoding of a future version (e.g.,
 * v2.3 to v1.8).
 * </p>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class Translator {

	private static String moduleId = "Translator";
	/**
	 * Identifies what a given Avail format may be translated <b>to</b>.
	 */
	private static Map<FILE_FMT, List<FILE_FMT>> supported = new HashMap<FILE_FMT, List<FILE_FMT>>();
	private static MddfTarget curTarget;

	static {
		/* identify what a given format may be translated TO */
		List<FILE_FMT> for_AVAILS_2_1 = new ArrayList<FILE_FMT>();
		/*
		 * Obsolete and deprecated version. Translation of this format to another is NOT
		 * supported.
		 */
		supported.put(FILE_FMT.AVAILS_2_1, for_AVAILS_2_1);

		List<FILE_FMT> for_AVAILS_2_2 = new ArrayList<FILE_FMT>();
		for_AVAILS_2_2.add(FILE_FMT.AVAILS_2_2_1);
		for_AVAILS_2_2.add(FILE_FMT.AVAILS_2_2_2);
		for_AVAILS_2_2.add(FILE_FMT.AVAILS_2_3);
		for_AVAILS_2_2.add(FILE_FMT.AVAILS_2_4);
		for_AVAILS_2_2.add(FILE_FMT.AVAILS_2_5);
		for_AVAILS_2_2.add(FILE_FMT.AVAILS_1_7);
		for_AVAILS_2_2.add(FILE_FMT.AVAILS_1_7_2);
		for_AVAILS_2_2.add(FILE_FMT.AVAILS_1_7_3);
		for_AVAILS_2_2.add(FILE_FMT.AVAILS_1_8);
		for_AVAILS_2_2.add(FILE_FMT.AVAILS_1_9);
		supported.put(FILE_FMT.AVAILS_2_2, for_AVAILS_2_2);

		List<FILE_FMT> for_AVAILS_1_7 = new ArrayList<FILE_FMT>();
		for_AVAILS_1_7.add(FILE_FMT.AVAILS_2_1);
		for_AVAILS_1_7.add(FILE_FMT.AVAILS_2_2);
		for_AVAILS_1_7.add(FILE_FMT.AVAILS_2_2_1);
		for_AVAILS_1_7.add(FILE_FMT.AVAILS_2_2_2);
		for_AVAILS_1_7.add(FILE_FMT.AVAILS_2_3);
		for_AVAILS_1_7.add(FILE_FMT.AVAILS_2_4);
		for_AVAILS_1_7.add(FILE_FMT.AVAILS_2_5);
		for_AVAILS_1_7.add(FILE_FMT.AVAILS_1_7_2);
		for_AVAILS_1_7.add(FILE_FMT.AVAILS_1_7_3);
		for_AVAILS_1_7.add(FILE_FMT.AVAILS_1_8);
		for_AVAILS_1_7.add(FILE_FMT.AVAILS_1_9);
		supported.put(FILE_FMT.AVAILS_1_7, for_AVAILS_1_7);

		// ........................................................

		List<FILE_FMT> for_AVAILS_2_2_1 = new ArrayList<FILE_FMT>();
		for_AVAILS_2_2_1.add(FILE_FMT.AVAILS_2_2_2);
		for_AVAILS_2_2_1.add(FILE_FMT.AVAILS_2_3);
		for_AVAILS_2_2_1.add(FILE_FMT.AVAILS_2_4);
		for_AVAILS_2_2_1.add(FILE_FMT.AVAILS_2_5);
		for_AVAILS_2_2_1.add(FILE_FMT.AVAILS_1_7_2);
		for_AVAILS_2_2_1.add(FILE_FMT.AVAILS_1_7_3);
		for_AVAILS_2_2_1.add(FILE_FMT.AVAILS_1_8);
		for_AVAILS_2_2_1.add(FILE_FMT.AVAILS_1_9);
		supported.put(FILE_FMT.AVAILS_2_2_1, for_AVAILS_2_2_1);

		// ........................................................

		List<FILE_FMT> for_AVAILS_2_2_2 = new ArrayList<FILE_FMT>();
		for_AVAILS_2_2_2.add(FILE_FMT.AVAILS_2_3);
		for_AVAILS_2_2_2.add(FILE_FMT.AVAILS_2_4);
		for_AVAILS_2_2_2.add(FILE_FMT.AVAILS_2_5);
		for_AVAILS_2_2_2.add(FILE_FMT.AVAILS_1_7_2);
		for_AVAILS_2_2_2.add(FILE_FMT.AVAILS_1_7_3);
		for_AVAILS_2_2_2.add(FILE_FMT.AVAILS_1_8);
		for_AVAILS_2_2_2.add(FILE_FMT.AVAILS_1_9);
		supported.put(FILE_FMT.AVAILS_2_2_2, for_AVAILS_2_2_2);

		List<FILE_FMT> for_AVAILS_1_7_2 = new ArrayList<FILE_FMT>();
		for_AVAILS_1_7_2.add(FILE_FMT.AVAILS_2_2_2);
		for_AVAILS_1_7_2.add(FILE_FMT.AVAILS_2_3);
		for_AVAILS_1_7_2.add(FILE_FMT.AVAILS_2_4);
		for_AVAILS_1_7_2.add(FILE_FMT.AVAILS_2_5);
		for_AVAILS_1_7_2.add(FILE_FMT.AVAILS_1_7_3);
		for_AVAILS_1_7_2.add(FILE_FMT.AVAILS_1_8);
		for_AVAILS_1_7_2.add(FILE_FMT.AVAILS_1_9);
		supported.put(FILE_FMT.AVAILS_1_7_2, for_AVAILS_1_7_2);

		// ........................................................

		List<FILE_FMT> for_AVAILS_2_3 = new ArrayList<FILE_FMT>();
		// formats that a v2.3 file may xlated TO...
		for_AVAILS_2_3.add(FILE_FMT.AVAILS_2_4);
		for_AVAILS_2_3.add(FILE_FMT.AVAILS_2_5);
		for_AVAILS_2_3.add(FILE_FMT.AVAILS_1_8);
		for_AVAILS_2_3.add(FILE_FMT.AVAILS_1_9);
		supported.put(FILE_FMT.AVAILS_2_3, for_AVAILS_2_3);

		List<FILE_FMT> for_AVAILS_1_7_3 = new ArrayList<FILE_FMT>(); 
		for_AVAILS_1_7_3.add(FILE_FMT.AVAILS_2_3);
		for_AVAILS_1_7_3.add(FILE_FMT.AVAILS_2_4);
		for_AVAILS_1_7_3.add(FILE_FMT.AVAILS_2_5); 
		for_AVAILS_1_7_3.add(FILE_FMT.AVAILS_1_8);
		for_AVAILS_1_7_3.add(FILE_FMT.AVAILS_1_9);
		supported.put(FILE_FMT.AVAILS_1_7_3, for_AVAILS_1_7_3);

		// ........................................................

		List<FILE_FMT> for_AVAILS_2_4 = new ArrayList<FILE_FMT>();
		// formats that a v2.4 file may xlated TO...
		for_AVAILS_2_4.add(FILE_FMT.AVAILS_2_5);
		for_AVAILS_2_4.add(FILE_FMT.AVAILS_1_8);
		for_AVAILS_2_4.add(FILE_FMT.AVAILS_1_9);
		supported.put(FILE_FMT.AVAILS_2_4, for_AVAILS_2_4);

		List<FILE_FMT> for_AVAILS_1_8 = new ArrayList<FILE_FMT>();
		for_AVAILS_1_8.add(FILE_FMT.AVAILS_2_4);
		for_AVAILS_1_8.add(FILE_FMT.AVAILS_2_5);
		for_AVAILS_1_8.add(FILE_FMT.AVAILS_1_9);
		supported.put(FILE_FMT.AVAILS_1_8, for_AVAILS_1_8);
		// ........................................................

		List<FILE_FMT> for_AVAILS_2_5 = new ArrayList<FILE_FMT>();
		// formats that a v2.5 file may xlated TO...
		for_AVAILS_2_5.add(FILE_FMT.AVAILS_1_9);
		supported.put(FILE_FMT.AVAILS_2_5, for_AVAILS_2_5);
	}

	/**
	 * Return a list of all formats that a file using the specified
	 * <tt>sourceFmt</tt> may be converted to.
	 * 
	 * @param sourceFmt
	 * @return
	 */
	public static List<FILE_FMT> supportedTranslations(FILE_FMT sourceFmt) {
		return supported.get(sourceFmt);
	}

	/**
	 * Convert an Avails file to other formats and save the translations to the
	 * local file system.
	 * 
	 * @param inputTarget
	 * @param xportFmts
	 * @param exportDir
	 * @param outFileName
	 * @param appendVersion
	 * @param logMgr
	 * @return
	 * @throws UnsupportedOperationException
	 */
	public static int translateAvails(MddfTarget inputTarget, EnumSet<FILE_FMT> xportFmts, File exportDir,
			String outFileName, boolean appendVersion, LogMgmt logMgr) throws UnsupportedOperationException {
		String dirPath = exportDir.getAbsolutePath();
		return translateAvails(inputTarget, xportFmts, dirPath, outFileName, appendVersion, logMgr);
	}

	/**
	 * Convert an Avails file to other formats and save the translations to the
	 * local file system.
	 * 
	 * @param inputTarget
	 * @param selections
	 * @param dirPath
	 * @param outFileName
	 * @param appendVersion
	 * @param logMgr
	 * @return
	 * @throws UnsupportedOperationException
	 */
	public static int translateAvails(MddfTarget inputTarget, EnumSet<FILE_FMT> selections, String dirPath,
			String outFileName, boolean appendVersion, LogMgmt logMgr) throws UnsupportedOperationException {
		curTarget = inputTarget;
		Iterator<FILE_FMT> selIt = selections.iterator();
		int outputCnt = 0;
		while (selIt.hasNext()) {
			FILE_FMT targetFmt = selIt.next();
			try {
				if (targetFmt.getEncoding().equalsIgnoreCase("xlsx")) {
					TemplateWorkBook wrkBook = convertToExcel(inputTarget, targetFmt, null, null, false, logMgr);
					if (wrkBook != null) {
						String fileName = outFileName;
						fileName = fileName.replaceFirst("(?i)\\.xlsx$", "");
						if (appendVersion) {
							fileName = fileName + "_v" + targetFmt.getVersion() + ".xlsx";
						} else {
							fileName = fileName + ".xlsx";
						}
						File exported = new File(dirPath, fileName);
						try {
							wrkBook.export(inputTarget, exported.getPath());
							logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE,
									"Saved translated file as " + exported.getPath(), inputTarget, moduleId);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					outputCnt++;
				} else {
					Document targetDoc = convertToXml(inputTarget, targetFmt, logMgr);
					if (targetDoc != null) {
						// did it work? if so, write to file system.
						if (targetDoc != null) {
							String fileName = outFileName;
							fileName = fileName.replaceFirst("(?i)\\.xml$", "");
							if (appendVersion) {
								fileName = fileName + "_v" + targetFmt.getVersion() + ".xml";
							} else {
								fileName = fileName + ".xml";
							}
							File exported = new File(dirPath, fileName);
							// Save as XML
							if (XmlIngester.writeXml(exported, targetDoc)) {
								logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE,
										"Saved translated file as " + exported.getPath(), inputTarget, moduleId);
								outputCnt++;
							}
						}
					}
				}
			} catch (Exception e) {
				if (e instanceof UnsupportedOperationException) {
					logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_XLATE, e.getMessage(), inputTarget, moduleId);
				} else {
					logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_XLATE, "Exception while translating: " + e.getMessage(),
							inputTarget, moduleId);
					e.printStackTrace();
				}
			}
		}
		curTarget = null;
		return outputCnt;
	}

	/**
	 * Convert an Avails file to other formats
	 * 
	 * @param inputTarget
	 * @param selections
	 * @param logMgr
	 * @return
	 */
	public static Map<FILE_FMT, Object> translateAvails(MddfTarget inputTarget, EnumSet<FILE_FMT> selections,
			LogMgmt logMgr) {
		curTarget = inputTarget;
		HashMap<FILE_FMT, Object> resultMap = new HashMap<FILE_FMT, Object>();
		Iterator<FILE_FMT> selIt = selections.iterator();
		while (selIt.hasNext()) {
			FILE_FMT targetFmt = selIt.next();
			try {
				if (targetFmt.getEncoding().equalsIgnoreCase("xlsx")) {
					// convert but don't save
					TemplateWorkBook wrkBook = convertToExcel(inputTarget, targetFmt, null, null, false, logMgr);
					resultMap.put(targetFmt, wrkBook);

				} else {
					Document targetDoc = convertToXml(inputTarget, targetFmt, logMgr);
					resultMap.put(targetFmt, targetDoc);
				}
			} catch (Exception e) {
				if (e instanceof UnsupportedOperationException) {
					logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_XLATE, e.getMessage(), inputTarget, moduleId);
				} else {
					logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_XLATE, "Exception while translating: " + e.getMessage(),
							inputTarget, moduleId);
					e.printStackTrace();
				}
			}
		}
		curTarget = null;
		return resultMap;
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
	private static Document convertToXml(MddfTarget inputTarget, FILE_FMT targetFmt, LogMgmt logMgr)
			throws UnsupportedOperationException {
		Document inputDoc = inputTarget.getXmlDoc();
		Document outputDoc = null;
		/*
		 * what's the schema used for the existing XML representation? It may already be
		 * a match for the desired format.
		 */
		String curVersion = XmlIngester.identifyXsdVersion(inputDoc.getRootElement());
		FILE_FMT curFmt = MddfContext.identifyMddfFormat("avails", curVersion);
		String targetVersion = targetFmt.getVersion();
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE,
				"Translating to XML v" + targetFmt.getVersion() + " from XML v" + curVersion, inputTarget, moduleId);
		if (curVersion.equals(targetVersion)) {
			return inputDoc;
		} else {
			switch (targetFmt) {
			case AVAILS_2_1:
				// not supported as a target format
				break;
			case AVAILS_2_2:
				switch (curVersion) {
				/*
				 * v2.2 is currently the earliest supported version. Since we do not support
				 * down-conversion, only v1.7 XLSX can be converted to v2.2. But any v1.7 file
				 * is already ingested as v2.2. Therefore there is nothing to do here.
				 */
//				case "2.1":
//					outputDoc = avail2_1_to_2_2(inputDoc, logMgr);
//					break;
//				case "2.2.1":
//					outputDoc = avail2_2_1_to_2_2(inputDoc, logMgr);
//					break;
//				case "2.3":
//					outputDoc = avail2_3_to_2_2_2(inputDoc, logMgr);
//					outputDoc = avail2_2_2_to_2_2_1(outputDoc, logMgr);
//					outputDoc = avail2_2_1_to_2_2(inputDoc, logMgr);
//					break;
//				case "2.2.2":
				default:
					// Unsupported request
					break;
				}
			case AVAILS_2_2_1:
				switch (curVersion) {
				case "2.1":
					outputDoc = avail2_1_to_2_2(inputDoc, logMgr);
					outputDoc = simpleConversion(outputDoc, curFmt, targetFmt);
					return outputDoc;
				case "2.2":
					outputDoc = simpleConversion(inputDoc, curFmt, targetFmt);
					return outputDoc; 
				default:
					// Unsupported request
					break;
				}
			case AVAILS_2_2_2:
				switch (curVersion) {
				case "2.1":
					outputDoc = avail2_1_to_2_2(inputDoc, logMgr);
					outputDoc = simpleConversion(outputDoc, curFmt, targetFmt);
					break;
				case "2.2":
					outputDoc = simpleConversion(inputDoc, curFmt, targetFmt);
					break;
				case "2.2.1":
					outputDoc = simpleConversion(inputDoc, curFmt, targetFmt);
					return outputDoc;
				default:
					// Unsupported request
					break;
				}
			case AVAILS_2_3:
				switch (curVersion) {
				case "2.1":
					outputDoc = avail2_1_to_2_2(inputDoc, logMgr);
					outputDoc = simpleConversion(outputDoc, curFmt, targetFmt);
					break;
				case "2.2":
				case "2.2.1":
				case "2.2.2":
					outputDoc = simpleConversion(inputDoc, curFmt, targetFmt);
					break;
				default:
					// Unsupported request
					break;
				}
			case AVAILS_2_4:
				switch (curVersion) {
				case "2.1":
					outputDoc = avail2_1_to_2_2(inputDoc, logMgr);
					outputDoc = simpleConversion(outputDoc, curFmt, targetFmt);
					break;
				case "2.2":
				case "2.2.1":
				case "2.2.2":
				case "2.2.3":
				case "2.3":
					outputDoc = simpleConversion(inputDoc, curFmt, targetFmt);
					break;
				default:
					// Unsupported request
					break;
				}
			case AVAILS_2_5:
				switch (curVersion) {
				case "2.1":
					outputDoc = avail2_1_to_2_2(inputDoc, logMgr);
					outputDoc = simpleConversion(outputDoc, curFmt, targetFmt);
					break;
				case "2.2":
				case "2.2.1":
				case "2.2.2":
				case "2.2.3":
				case "2.3":
				case "2.4":
					outputDoc = simpleConversion(inputDoc, curFmt, targetFmt);
					break;
				default:
					// Unsupported request
					break;
				}
			}
			if (outputDoc != null) {
				addXlateHistory(outputDoc, curFmt, targetFmt);
				return outputDoc;
			}
		}
		throw new UnsupportedOperationException("Conversion to Avails " + targetFmt.getEncoding() + " "
				+ targetFmt.name() + " from v" + curVersion + " not supported");
	}

	/**
	 * Add audit trail of translation stages. A <tt>&lt;Comment&gt; is added that
	 * indicates the
	 * <ol>
	 * <li>encoding of the source file (e.g., 'Avails v1.7.3')</li>
	 * <li>encoding of the translated file (e.g., 'Avails v2.3')</li>
	 * <li>version of mddf-lib that was used</li>
	 * <li></li>
	 * </ol>
	 * Note that each time a file is translated a new history Comment is added
	 * recording the translation event but any previous records will still remain.
	 * For example, the following indicates that a v1.7.3 Avails was first converted
	 * to v2.3 and then the v2.3 was converted to v2.2.2:
	 * 
	 * <pre>
	 *   &lt;!-- ===== Translation History::
	       from Avails v2.3 (xml) to Avails v2.2.2 (xml)
	       mddf-lib version=1.5.1.rc5-SNAPSHOT[4]
	--&gt;
	&lt;!-- ===== Translation History::
	       from Avails v1.7.3 (xlsx) to Avails v2.3 (xml)
	       mddf-lib version=1.5.1.rc5-SNAPSHOT[4]
	--&gt;
	 * </pre>
	 * 
	 * @param xmlDoc
	 * @param srcFmt
	 * @param targetFmt
	 */
	public static void addXlateHistory(Document xmlDoc, FILE_FMT srcFmt, FILE_FMT targetFmt) {
		Properties mddfLibProps = MddfContext.getProperties();
		String libVersion = mddfLibProps.getProperty("mddf.lib.version");
		String libBuild = mddfLibProps.getProperty("mddf.lib.build");
		String text = " ===== Translation History::\n";
		text = text + "           from " + srcFmt.toString() + " to " + targetFmt.toString() + "\n";
		text = text + "           mddf-lib version=" + libVersion + "[" + libBuild + "]\n";
		Comment pedigree = new LocatedComment(text);
		xmlDoc.getRootElement().addContent(0, pedigree);

	}

	/**
	 * Handle conversion of an XML formatted Avails to an Excel formated Avails.
	 * 
	 * @param inputTarget
	 * @param targetXlsxFormat
	 * @param dirPath
	 * @param outFileName
	 * @param appendVersion
	 * @param logMgr
	 * @return
	 * @throws UnsupportedOperationException
	 */
	private static TemplateWorkBook convertToExcel(MddfTarget inputTarget, FILE_FMT targetXlsxFormat, String dirPath,
			String outFileName, boolean appendVersion, LogMgmt logMgr) throws UnsupportedOperationException {

		Document xmlSrcDoc = inputTarget.getXmlDoc();
		String curVersion = XmlIngester.identifyXsdVersion(xmlSrcDoc.getRootElement());
//		FILE_FMT curFmt = MddfContext.identifyMddfFormat("avails", curVersion);
		Version excelVer = null;
		String msg = "Translating to Excel v" + targetXlsxFormat.getVersion() + " from XML v" + curVersion;
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE, msg, inputTarget, moduleId);
		FILE_FMT targetXmlFmt = null;
		switch (targetXlsxFormat) {
		case AVAILS_1_6:
			// not supported
			break;
		case AVAILS_1_7:
			/*
			 * Dead version that was never implemented or used by the community
			 */
			break;
		case AVAILS_1_7_2:
			excelVer = Version.V1_7_2;
			/*
			 * A v1.7.2 spreadsheet should be generated from v2.2.2 XML.
			 */
			targetXmlFmt = FILE_FMT.AVAILS_2_2_2;
			break;

		case AVAILS_1_7_3:
		case AVAILS_1_8:
			/*
			 * Both v1.7.3 and v1.8 spreadsheet should be generated from v2.3 XML.
			 */
			targetXmlFmt = FILE_FMT.AVAILS_2_3;
			if (targetXlsxFormat == FILE_FMT.AVAILS_1_8) {
				excelVer = Version.V1_8;
			} else {
				excelVer = Version.V1_7_3;
			}
			break;
		case AVAILS_1_9:
			excelVer = Version.V1_9;;
			targetXmlFmt = FILE_FMT.AVAILS_2_5;
			break;

		}
		if (targetXmlFmt == null) {
			throw new UnsupportedOperationException("Conversion to Avails xlsx " + targetXlsxFormat.name()
					+ " from XML v" + curVersion + " not supported");
		}
		Document xmlDoc = convertToXml(inputTarget, targetXmlFmt, logMgr);
		XlsxBuilder converter = null;
		if (excelVer == Version.V1_8) {
			converter = new XlsxBuilderV1_8(xmlDoc.getRootElement(), excelVer, logMgr);
		} else {
			converter = new XlsxBuilder(xmlDoc.getRootElement(), excelVer, logMgr);
		}
		TemplateWorkBook wrkBook = converter.getWorkbook();
		return wrkBook;
	}

	/**
	 * 
	 * @param xmlDocIn
	 * @param logMgr
	 * @return
	 */
	private static Document avail2_1_to_2_2(Document xmlDocIn, LogMgmt logMgr) {
		/*
		 * STAGE ONE: some changes are easiest to do by converting the Doc to a string
		 * and then doing string replacements prior to converting back to Doc form.
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
		 * Stage TWO is to do anything that is easier to handle via manipulation of the
		 * XML.
		 */
		// ...........
		XPathFactory xpfac = XPathFactory.instance();
		Namespace availsNSpace = Namespace.getNamespace("avails",
				MddfContext.NSPACE_AVAILS_PREFIX + "2.2" + MddfContext.NSPACE_AVAILS_SUFFIX);
		/*
		 * Find all Transaction/Term[[@termName='HoldbackExclusionLanguage']. These need
		 * to be removed from the XML and then replaced with a
		 * Transaction/AllowedLanguage element. Luckily, the location of the new
		 * AllowedLanguage element is easy to pinpoint.... it follows immediately after
		 * a REQUIRED element (i.e., either an <End> or <EndCondition>).
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
	 * 
	 * 
	 * @param xmlDocIn
	 * @param logMgr
	 * @return
	 */
	private static Document avail2_2_1_to_2_2(Document xmlDocIn, LogMgmt logMgr) {
		/*
		 * STAGE ONE: some changes are easiest to do by converting the Doc to a string
		 * and then doing string replacements prior to converting back to Doc form.
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
		removeAttribute(targetPath, "asset", rootEl, logMgr, "AssetLanguage");
		targetPath = "/avails:AvailList/avails:Avail/avails:Transaction/avails:AssetLanguage[@descriptive]";
		removeAttribute(targetPath, "descriptive", rootEl, logMgr, "AssetLanguage");
		targetPath = "/avails:AvailList/avails:Avail/avails:Transaction/avails:AllowedLanguage[@asset]";
		removeAttribute(targetPath, "asset", rootEl, logMgr, "AllowedLanguage");

		return xmlDocOut;
	}

	/**
	 * @param logMgr
	 * @param srcDoc
	 * @return
	 */
	private static Document avail2_2_2_to_2_2_1(Document xmlDocIn, LogMgmt logMgr) {
		XMLOutputter outputter = new XMLOutputter();
		String inDoc = outputter.outputString(xmlDocIn);
		// Change Namespace declaration
		String t1 = inDoc.replaceFirst("/avails/v2.2.2/avails", "/avails/v2.2.1/avails");
		/*
		 * v2.2.2 adds several elements and attributes missing in v2.2.1. These need to
		 * be removed
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
		if (!removalList.isEmpty()) {
			String msg = "Removing " + removalList.size() + " People elements";
			logMgr.log(LogMgmt.LEV_NOTICE, LogMgmt.TAG_XLATE, msg, curTarget, moduleId);
		}

		targetPath = "//avails:GroupingEntity";
		pathExp = xpfac.compile(targetPath, Filters.element(), null, availsNSpace);
		removalList = pathExp.evaluate(rootEl);
		for (Element targetEl : removalList) {
			targetEl.detach();
		}
		if (!removalList.isEmpty()) {
			String msg = "Removing " + removalList.size() + " GroupingEntity elements";
			logMgr.log(LogMgmt.LEV_NOTICE, LogMgmt.TAG_XLATE, msg, curTarget, moduleId);
		}

		targetPath = "//avails:Transaction/avails:Duration";
		pathExp = xpfac.compile(targetPath, Filters.element(), null, availsNSpace);
		removalList = pathExp.evaluate(rootEl);
		for (Element targetEl : removalList) {
			targetEl.detach();
		}
		if (!removalList.isEmpty()) {
			String msg = "Removing " + removalList.size() + " Transaction/Duration elements";
			logMgr.log(LogMgmt.LEV_NOTICE, LogMgmt.TAG_XLATE, msg, curTarget, moduleId);
		}

		targetPath = "/avails:AvailList/avails:Avail[@updateNum]";
		removeAttribute(targetPath, "updateNum", rootEl, logMgr, "Avail");
		targetPath = "/avails:AvailList/avails:Avail[@updateDeliveryFlow]";
		removeAttribute(targetPath, "updateDeliveryFlow", rootEl, logMgr, "Avail");
		targetPath = "/avails:AvailList/avails:Avail[@workflow]";
		removeAttribute(targetPath, "workflow", rootEl, logMgr, "Avail");

		targetPath = "/avails:AvailList/avails:Avail/avails:Transaction/*[@lag]";
		removeAttribute(targetPath, "lag", rootEl, logMgr, "StartCondition and EndCondition");

		targetPath = "/avails:AvailList/avails:Avail/avails:Transaction/avails:AssetLanguage[@assetProvided]";
		removeAttribute(targetPath, "assetProvided", rootEl, logMgr, "AssetLanguage");

		targetPath = "/avails:AvailList/avails:Avail/avails:Transaction/avails:AssetLanguage[@metadataProvided]";
		removeAttribute(targetPath, "metadataProvided", rootEl, logMgr, "AssetLanguage");
		/*
		 * v2.2.2 allows multiple instances of TitleInternalAlias for different regions
		 * as well as multiple instances of TitleDisplayUnlimited for different
		 * languages (also for Season and Series). If multiple instances are present,
		 * all but 1st is removed and a WARNING is issued. This is also true for Season
		 * and Series metadata
		 */
		scrubTitles(rootEl, "", logMgr);
		scrubTitles(rootEl, "Season", logMgr);
		scrubTitles(rootEl, "Series", logMgr);

		return xmlDocOut;
	}

	/**
	 * Remove multiple instances of <tt><i>prefix</i>TitleDisplayUnlimited</tt>
	 * leaving only the first instance.
	 * <p>
	 * v2.2.2 allows multiple instances of TitleInternalAlias for different regions
	 * as well as multiple instances of TitleDisplayUnlimited for different
	 * languages (also for Season and Series). If multiple instances are present,
	 * all but 1st is removed and a WARNING is issued. This is also true for Season
	 * and Series metadata
	 * </p>
	 * 
	 * @param rootEl
	 * @param prefix
	 * @param logMgr
	 */
	private static void scrubTitles(Element rootEl, String prefix, LogMgmt logMgr) {
		/*
		 */
		Namespace availsNSpace = rootEl.getNamespace("avails");
		XPathFactory xpfac = XPathFactory.instance();

		String thing1 = prefix + "TitleInternalAlias";
		String targetPath = "/avails:AvailList/avails:Avail/avails:Asset//*[count(avails:" + thing1 + ") > 1 ]";
		int removedCnt = removeExtras(rootEl, targetPath, thing1);
		if (removedCnt > 0) {
			String msg = "Removing " + removedCnt + " " + thing1 + " elements (max allowed exceeded)";
			String details = "Only 1 " + thing1 + " allowed per Asset";
			logMgr.log(LogMgmt.LEV_NOTICE, LogMgmt.TAG_XLATE, msg, curTarget, null, moduleId, details, null);
		}

		removedCnt = 0;
		String thing2 = prefix + "TitleDisplayUnlimited";
		targetPath = "/avails:AvailList/avails:Avail/avails:Asset//*[count(avails:" + thing2 + ") > 1 ]";
		removedCnt = removeExtras(rootEl, targetPath, thing2);
		if (removedCnt > 0) {
			String msg = "Removing " + removedCnt + " " + thing2 + " elements (max allowed exceeded)";
			String details = "Only 1 " + thing2 + " allowed per Asset";
			logMgr.log(LogMgmt.LEV_NOTICE, LogMgmt.TAG_XLATE, msg, curTarget, null, moduleId, details, null);
		}

		// now remove @language and @region from any remaining
		targetPath = "//avails:" + thing1 + "[@region]";
		removeAttribute(targetPath, "region", rootEl, logMgr, thing1);
		targetPath = "//avails:" + thing2 + "[@language]";
		removeAttribute(targetPath, "language", rootEl, logMgr, thing2);
	}

	/**
	 * @param logMgr
	 * @param inputDoc
	 * @return
	 */
	private static Document avail2_3_to_2_2_2(Document xmlDocIn, LogMgmt logMgr) {
		XMLOutputter outputter = new XMLOutputter();
		// start with namespace conversion
		Document xmlDocOut = simpleConversion(xmlDocIn, MddfContext.identifyMddfFormat("Avails", "2.3"),
				MddfContext.identifyMddfFormat("avails", "2.2.2"));

		Element rootEl = xmlDocOut.getRootElement();
		Namespace availsNSpace = rootEl.getNamespace("avails");
		XPathFactory xpfac = XPathFactory.instance();

		String targetPath = "//avails:Licensee";
		XPathExpression<Element> pathExp = xpfac.compile(targetPath, Filters.element(), null, availsNSpace);
		List<Element> removalList = pathExp.evaluate(rootEl);
		for (Element targetEl : removalList) {
			targetEl.detach();
		}
		if (!removalList.isEmpty()) {
			String msg = "Removing " + removalList.size() + " Licensee elements";
			logMgr.log(LogMgmt.LEV_NOTICE, LogMgmt.TAG_XLATE, msg, curTarget, null, moduleId, null, null);
		}

		/*
		 * 'Transaction/WindowDuration'in v2.3 is same as 'Transaction/Duration' in
		 * v2.2.2
		 * 
		 */
		targetPath = "//avails:Transaction/avails:WindowDuration";
		pathExp = xpfac.compile(targetPath, Filters.element(), null, availsNSpace);
		removalList = pathExp.evaluate(rootEl);
		for (Element targetEl : removalList) {
			targetEl.setName("Duration");
		}

		// remove all unsupported Terms
		targetPath = "//avails:Term[@termName[.='ContractStatus' or .='TitleStatus' or .='Download' or .='Exclusive' or .='ExclusiveAttributes' or .='BrandingRights' or .='BrandingRightsAttributes']]";
		pathExp = xpfac.compile(targetPath, Filters.element(), null, availsNSpace);
		removalList = pathExp.evaluate(rootEl);
		for (Element targetEl : removalList) {
			targetEl.detach();
		}
		if (!removalList.isEmpty()) {
			String msg = "Removing " + removalList.size() + " unsupported Terms";
			logMgr.log(LogMgmt.LEV_NOTICE, LogMgmt.TAG_XLATE, msg, curTarget, null, moduleId, null, null);
		}
		return xmlDocOut;
	}

	/**
	 * Handle conversion of XML document that only requires changes to the
	 * <tt>Namespace</tt> versions.
	 * 
	 * @param xmlDocIn
	 * @param srcFmt
	 * @param targetFmt
	 * @return
	 */
	private static Document simpleConversion(Document xmlDocIn, FILE_FMT srcFmt, FILE_FMT targetFmt) {
		XMLOutputter outputter = new XMLOutputter();
		String inDoc = outputter.outputString(xmlDocIn);
		Map<String, Namespace> srcNS = MddfContext.getRequiredNamespaces(srcFmt);
		Map<String, Namespace> targetNS = MddfContext.getRequiredNamespaces(targetFmt);
		Set<String> nsKeys = srcNS.keySet();
		for (String key : nsKeys) {
			// 1st the actual xmlns declarations...
			Namespace srcNSpace = srcNS.get(key);
			Namespace targetNSpace = targetNS.get(key);
			String t1 = inDoc.replaceFirst(srcNSpace.getURI(), targetNSpace.getURI());
			// now handle a possible schemaLocation:
			String prefix = srcNSpace.getPrefix();
			String verIn = srcFmt.getVersion();
			String verOut = targetFmt.getVersion();
			String target = prefix + "-v" + verIn + ".xsd";
			String replacement = prefix + "-v" + verOut + ".xsd";
			String outDoc = t1.replaceFirst(target, replacement);
			inDoc = outDoc;
		}
		return regenXml(inDoc);
	}

	/**
	 * Identify all elements matching the specified xpath and then detach the
	 * identified attribute if it exists. If <tt>attName == '*'</tt> all attributes
	 * are removed.
	 * 
	 * @param targetPath
	 * @param attName
	 * @param rootEl
	 * @param targetDesc used to describe targeted elements when constructing log
	 *                   message
	 */
	private static int removeAttribute(String targetPath, String attName, Element rootEl, LogMgmt logMgr,
			String targetDesc) {
		XPathFactory xpfac = XPathFactory.instance();
		Namespace availsNSpace = rootEl.getNamespace("avails");
		XPathExpression<Element> pathExp = xpfac.compile(targetPath, Filters.element(), null, availsNSpace);
		List<Element> removalList = pathExp.evaluate(rootEl);
		int hitCnt = 0;
		for (Element targetEl : removalList) {
			if (attName.equals("*")) {
				List<Attribute> attList = targetEl.getAttributes();
				for (Attribute att : attList) {
					att.detach();
					hitCnt++;
				}
			} else {
				if (targetEl.removeAttribute(attName)) {
					hitCnt++;
				}
			}
		}

		if (hitCnt > 0) {
			String msg = null;
			String attDesc = null;
			if (attName.equals("*")) {
				attDesc = "all attributes";
			} else {
				attDesc = "@" + attName + " attribute";
			}
			if (targetDesc == null) {
				msg = "Removing " + attDesc + " from " + hitCnt + " elements.";
			} else {
				msg = "Removing " + attDesc + " from " + hitCnt + " " + targetDesc + " elements.";
			}
			logMgr.log(LogMgmt.LEV_NOTICE, LogMgmt.TAG_XLATE, msg, curTarget, null, moduleId, null, null);
		}
		return hitCnt;
	}

	private static int removeExtras(Element rootEl, String targetPath, String targetNode) {
		Namespace availsNSpace = rootEl.getNamespace("avails");
		XPathFactory xpfac = XPathFactory.instance();
		XPathExpression<Element> pathExp = xpfac.compile(targetPath, Filters.element(), null, availsNSpace);
		List<Element> removalList = pathExp.evaluate(rootEl);
		int removedCnt = 0;
		for (Element targetEl : removalList) {
			List<Element> extrasList = targetEl.getChildren(targetNode, availsNSpace);
			// keep the 1st but detach the rest
			for (int i = 1; i < extrasList.size(); i++) {
				Element extra = extrasList.get(i);
				extra.detach();
				removedCnt++;
			}
		}
		return removedCnt;
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
	 * Returns text suitable for describing capabilities and usage of translation
	 * functions. Text is formated for incorporation in <tt>man</tt> and Help pages.
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
