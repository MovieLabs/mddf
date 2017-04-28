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
import java.util.EnumSet;
import java.util.Iterator;

import org.jdom2.Document;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.avails.xlsx.XlsxBuilder;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.XmlIngester;
import com.movielabs.mddflib.avails.xlsx.AvailsSheet.Version;

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
				// not yet implemented
				break;
			case AVAILS_1_7:
				XlsxBuilder converter = new XlsxBuilder(xmlDoc.getRootElement(), Version.V1_7, logMgr);
				String fileName = filePrefix + "_v1.7.xlsx";
				File exported = new File(dirPath, fileName);
				try {
					converter.export(exported.getPath());
					outputCnt++;
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case AVAILS_2_2:
				fileName = filePrefix + "_v2.2.xml";
				exported = new File(dirPath, fileName);
				// Save as XML
				if (XmlIngester.writeXml(exported, xmlDoc)) {
					outputCnt++;
				}
				break;
			}

		}
		return outputCnt;

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
				+ "For AVAILS the following formats may be specified:\n"
				+ "-- AVAILS_1_7: Excel using v1.7 template.\n"
				+ "-- AVAILS_2_2: XML using v2.2 schema.";

		return helpTxt;
	}
}
