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
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Map;

import com.movielabs.mddflib.avails.xml.AvailsWrkBook;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.avails.xml.streaming.StreamingXmlBuilder;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class XlsxImportTest {

	private static String rsrcPath = "./test/resources/avails/xlsx/";
	private static String tempDirPath = "./test/tmp/";
	private File tempDir;
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
		iLog.setPrintToConsole(false);
		tempDir = new File("tempDirPath");
		if (tempDir.exists()) {
			deleteDirectory(tempDir);
		} 
		tempDir.mkdirs();
	}

	@Test
	public void testIngestV1_7() {
		String testFileName = "Movies_v1.7.xlsx";
		String srcFilePath = rsrcPath + testFileName;
		Version version = Version.V1_7;
		File srcFile = new File(srcFilePath);

		iLog.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

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

		iLog.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

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
	public void testIngestMovie_V1_7_3() {
		Version version = Version.V1_7_3;
		String testFileName = "Movies_v1.7.3.xlsx";
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);

		iLog.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

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
	public void testIngestTV_V1_7_3() {
		Version version = Version.V1_7_3; 
		String testFileName = "TV_v1.7.3.xlsx";
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);

		iLog.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

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
	public void testIngest_MovieV1_8() {
		Version version = Version.V1_8;
		String testFileName = "Movies_v1.8.xlsx";
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);

		iLog.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

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
	public void testIngest_Tv_V1_8() {
		Version version = Version.V1_8;
		String testFileName = "TV_v1.8.xlsx";
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);

		iLog.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

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
	public void testIngest_MovieV1_9() {
		Version version = Version.V1_9;
		String testFileName = "Movies_v1.9.xlsx";
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);

		iLog.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

//		Map<String, Object> results = AvailsWrkBook.convertSpreadsheet(srcFile, version, null, iLog);
		MddfTarget target = new MddfTarget(srcFile, iLog); 
		StreamingXmlBuilder bldr1 = new StreamingXmlBuilder(iLog, version);
		Map<String, Object>  results = bldr1.convert(target, null, 0, "JUnit test");
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
	public void testIngest_Bonus_V1_9() {
		Version version = Version.V1_9;
		String testFileName = "Bonus_v1.9.xlsx";
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);
		iLog.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");
//		Map<String, Object> results = AvailsWrkBook.convertSpreadsheet(srcFile, version, null, iLog);

		MddfTarget target = new MddfTarget(srcFile, iLog); 
		StreamingXmlBuilder bldr1 = new StreamingXmlBuilder(iLog, version);
		Map<String, Object>  results = bldr1.convert(target, null, 0, "JUnit test");
		
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
	public void testIngest_TV_V1_9() {
		Version version = Version.V1_9;
		String testFileName = "TV_v1.9.xlsx";
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);

		iLog.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

//		Map<String, Object> results = AvailsWrkBook.convertSpreadsheet(srcFile, version, null, iLog);
		MddfTarget target = new MddfTarget(srcFile, iLog); 
		StreamingXmlBuilder bldr1 = new StreamingXmlBuilder(iLog, version);
		Map<String, Object>  results = bldr1.convert(target, null, 0, "JUnit test");
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
