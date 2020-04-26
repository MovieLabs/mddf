/**
 * Copyright (c) 2019 MovieLabs

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
package com.movielabs.mddflib.tests.junit.avails;

import org.jdom2.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.EnumSet;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.avails.xml.AvailsWrkBook;
import com.movielabs.mddflib.avails.xml.AvailsSheet;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.Translator;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * test basic functionality required to translate XML to XLSX.
 * <p>
 * This is not an exhaustive test in that the correct handling of all fields is
 * not verified. Rather, the test verifies that:
 * <ol>
 * <li>an XLSX is generated, and</li>
 * <li>the XLSX has the correct number of rows.
 * </p>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class XlsxExportTest {

	private static String rsrcPath = "./test/resources/avails/";
	private static File tmpDirPath = new File("./test/tmp/");
	private File tempDir;
	private InstrumentedLogger iLog;

	public XlsxExportTest() {
		iLog = new InstrumentedLogger();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		iLog.clearLog();
		iLog.setPrintToConsole(false);
		tempDir = new File("tempDirPath");
		if (tempDir.exists()) {
			deleteDirectory(tempDir);
		} 
		tempDir.mkdirs();
	}

	@Test
	public void testExport1_7_3() throws Exception {
		String testFileName = "Avails_noErrors_v2.3.xml";
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);
		FILE_FMT excelFmt = FILE_FMT.AVAILS_1_7_3;
		Version excelVer = Version.V1_7_3;
		String tmpFileName = testFileName + "_v" + excelFmt.getVersion() + ".xlsx";
		File tmpFile = new File(tempDir, tmpFileName);

		EnumSet<FILE_FMT> selections = EnumSet.noneOf(FILE_FMT.class);
		selections.add(excelFmt);

		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");
		MddfTarget target = new MddfTarget(srcFile, iLog);
		int cnt = Translator.translateAvails(target, selections, tempDir, testFileName, true, iLog);
		try {
			assertEquals(1, cnt);
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			// Now re-ingest the XLSX to verify it is correct
			AvailsWrkBook awb = new AvailsWrkBook(tmpFile, iLog, false, false);
			AvailsSheet sheet = awb.ingestSheet(0);
			int rowCnt = sheet.getRowCount();
			assertEquals(16, rowCnt);
		} catch (Exception e) {
			dumpLog();
			throw e;
		}
	}

	/**
	 * @param string
	 * @param object
	 */
	private void dumpXml(Document xmlDoc, String fileName) {
		File dumpFile = new File(tempDir , fileName + ".xml");
		XmlIngester.writeXml(dumpFile, xmlDoc);
	}

	private void dumpLog() {
		System.out.println("\n === FAILED TEST... dumping log ===");
		iLog.printLog();
		System.out.println(" === End log dump for FAILED TEST ===");

	}
	
	boolean deleteDirectory(File directoryToBeDeleted) {
	    File[] allContents = directoryToBeDeleted.listFiles();
	    if (allContents != null) {
	        for (File file : allContents) {
	            deleteDirectory(file);
	        }
	    }
	    return directoryToBeDeleted.delete();
	}
}
