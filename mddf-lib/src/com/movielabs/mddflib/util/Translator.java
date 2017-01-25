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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.EnumSet;
import java.util.Iterator;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.avails.xlsx.XlsxBuilder;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.avails.xlsx.AvailsSheet.Version;

/**
 * Coordinates translation of Avails from one format or version to another.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class Translator {

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
				Format myFormat = Format.getPrettyFormat();
				XMLOutputter outputter = new XMLOutputter(myFormat);
				try {
					OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(exported), "UTF-8");
					outputter.output(xmlDoc, osw);
					osw.close();
					outputCnt++;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}

		}
		return outputCnt;

	}
}
