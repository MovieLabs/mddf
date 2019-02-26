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
package com.movielabs.mddflib.tests.avails;

import org.jdom2.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Map;

import com.movielabs.mddflib.avails.xml.AvailsWrkBook;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class XlsxImportTest {

	private static String rsrcPath = "./test/resources/avails/xlsx/";
	private static String tempDir = "./test/tmp/";
	private InstrumentedLogger iLog;

	public XlsxImportTest() {
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
		iLog.setPrintToConsole(true);
	}

	@Test
	public void testIngestV1_7() {
		String testFileName = "Movies_v1.7.xlsx";
		String srcFilePath = rsrcPath + testFileName;
		Version version = Version.V1_7;
		File srcFile = new File(srcFilePath);

		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

		Map<String, Object> results = AvailsWrkBook.convertSpreadsheet(srcFile, version, null, iLog);
		try {
			assertNotNull(results);
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			dumpXml((Document) results.get("xml"), testFileName);
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	@Test
	public void testIngestV1_7_2() {
		String testFileName = "Movies_v1.7.2.xlsx";
		String srcFilePath = rsrcPath + testFileName;
		Version version = Version.V1_7_2;
		File srcFile = new File(srcFilePath);

		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

		Map<String, Object> results = AvailsWrkBook.convertSpreadsheet(srcFile, version, null, iLog);
		try {
			assertNotNull(results);
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			dumpXml((Document) results.get("xml"), testFileName);
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	@Test
	public void testIngestV1_7_3() {
		Version version = Version.V1_7_3;
		String testFileName = "Movies_v1.7.3.xlsx";
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);

		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

		Map<String, Object> results = AvailsWrkBook.convertSpreadsheet(srcFile, version, null, iLog);
		try {
			assertNotNull(results);
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			dumpXml((Document) results.get("xml"), testFileName);
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
		testFileName = "TV_v1.7.3.xlsx";
		srcFilePath = rsrcPath + testFileName;
		srcFile = new File(srcFilePath);

		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

		results = AvailsWrkBook.convertSpreadsheet(srcFile, version, null, iLog);
		try {
			assertNotNull(results);
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			dumpXml((Document) results.get("xml"), testFileName);
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	@Test
	public void testIngestV1_8() {
		Version version = Version.V1_8;
		String testFileName = "Movies_v1.8.xlsx";
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);

		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

		Map<String, Object> results = AvailsWrkBook.convertSpreadsheet(srcFile, version, null, iLog);
		try {
			assertNotNull(results);
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			dumpXml((Document) results.get("xml"), testFileName);
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
		testFileName = "TV_v1.8.xlsx";
		srcFilePath = rsrcPath + testFileName;
		srcFile = new File(srcFilePath);

		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

		results = AvailsWrkBook.convertSpreadsheet(srcFile, version, null, iLog);
		try {
			assertNotNull(results);
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			dumpXml((Document) results.get("xml"), testFileName);
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	/**
	 * @param string
	 * @param object
	 */
	private void dumpXml(Document xmlDoc, String fileName) {
		File dumpFile = new File(tempDir, fileName + ".xml");
		XmlIngester.writeXml(dumpFile, xmlDoc);
	}

	private void dumpLog() {
		System.out.println("\n === FAILED TEST... dumping log ===");
		iLog.printLog();
		System.out.println(" === End log dump for FAILED TEST ===");

	}
}
