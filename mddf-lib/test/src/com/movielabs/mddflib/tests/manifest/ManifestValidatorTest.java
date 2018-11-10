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
package com.movielabs.mddflib.tests.manifest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.MissingResourceException;

import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.manifest.validation.ManifestValidator;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.xml.MddfTarget;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class ManifestValidatorTest extends ManifestValidator {

	private static String rsrcPath = "./test/resources/manifest/";
	private InstrumentedLogger iLog;

	public ManifestValidatorTest() {
		super(true, new InstrumentedLogger());
		iLog = (InstrumentedLogger) loggingMgr;
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
		curFile = null;
		curFileName = null;
		curFileIsValid = true;
		curRootEl = null;
		rootNS = null;
		iLog.clearLog();
	}

	/**
	 * @param string
	 */
	protected MddfTarget initialize(String testFileName) {
		String srcFilePath = rsrcPath + testFileName;
		srcFile = new File(srcFilePath);
		try {
			MddfTarget target = new MddfTarget(srcFile, iLog);
			return target;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new MissingResourceException("Missing test artifact " + srcFilePath, "File", srcFilePath);
		}

	}

	/**
	 * @throws JDOMException
	 * @throws IOException
	 * 
	 */
	@Test
	public void testV16noErrors() throws IOException, JDOMException {
		MddfTarget target = initialize("MMM_v1.6_base.xml");
		execute(target, false);
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
	}

	/**
	 * @throws JDOMException
	 * @throws IOException
	 * 
	 */
	@Test
	public void testV16withErrors() throws IOException, JDOMException {
		MddfTarget target = initialize("MMM_v1.6_errors.xml");
		execute(target, false);
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
		assertEquals(5, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_WARN));
		assertEquals(1, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
	}

	/**
	 * @throws JDOMException
	 * @throws IOException
	 * 
	 */
	@Test
	public void testV17noErrors() throws IOException, JDOMException {
		MddfTarget target = initialize("MMM_v1.7_base.xml");
		execute(target, false);
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
	}

	/**
	 * @throws JDOMException
	 * @throws IOException
	 * 
	 */
	@Test
	public void testV17withErrors() throws IOException, JDOMException {
		MddfTarget target = initialize("MMM_v1.7_errors.xml");
		execute(target, false);
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
		assertEquals(9, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(7, iLog.getCountForLevel(LogMgmt.LEV_WARN));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
	}

	@Test
	public void testFull1_v1_6() throws IOException, JDOMException {
		MddfTarget target = initialize("Manifest_v1.6_A.xml");
		execute(target, false);
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		/*
		 * WIP:
		 */
//		assertEquals(3, iLog.getCountForLevel(LogMgmt.LEV_WARN));
//		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
	}

	@Test
	public void testFull1_v1_7() throws IOException, JDOMException {
		MddfTarget target = initialize("Manifest_v1.7_A.xml");
		execute(target, false);
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		/*
		 * WIP:
		 */
//		assertEquals(3, iLog.getCountForLevel(LogMgmt.LEV_WARN));
//		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
	}

	@Test
	public void testFull1_v1_8() throws IOException, JDOMException {
		MddfTarget target = initialize("Manifest_v1.8_A.xml");
		execute(target, false);
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		/*
		 * WIP:
		 */
//		assertEquals(3, iLog.getCountForLevel(LogMgmt.LEV_WARN));
//		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
	}

	// ======================================================
	/*
	 * These tests are disabled. The logic for validation process to 'follow'
	 * Container references to MEC files, currently resides in the
	 * ValidationController rather than the ManifestValidator.
	 */
//	/**
//	 * @throws JDOMException
//	 * @throws IOException
//	 * 
//	 */
//	@Test
//	public void testMEC_noErr() throws IOException, JDOMException {
//		MddfTarget target = initialize("containers/Manifest_w_good_MEC_v1.6.xml"); 
//		super.process(target); 
//		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
//		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
//		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
//	}
//
//	/**
//	 * @throws JDOMException
//	 * @throws IOException
//	 * 
//	 */
//	@Test
//	public void testMEC_withErrors() throws IOException, JDOMException {
//		MddfTarget target = initialize("containers/Manifest_w_bad_MEC_v1.6.xml");
//		iLog.setPrintToConsole(true);
//		super.process(target);
//		iLog.setPrintToConsole(false);
//		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
//		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
//		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
//	}

	protected void execute(MddfTarget target, boolean logToConsole) throws IOException, JDOMException {
		iLog.setPrintToConsole(logToConsole);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + target.getSrcFile().getCanonicalPath(), null,
				"JUnit");
		super.process(target);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Test completed", null,
				"JUnit");
		iLog.setPrintToConsole(false);
	}
}
