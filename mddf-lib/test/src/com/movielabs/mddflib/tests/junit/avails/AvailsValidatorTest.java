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
package com.movielabs.mddflib.tests.junit.avails;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.MissingResourceException;

import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

import com.movielabs.mddflib.avails.validation.AvailValidator;
import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.xml.MddfTarget;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class AvailsValidatorTest extends AvailValidator {

	private static String rsrcPath = "./test/resources/avails/";
	private InstrumentedLogger iLog;

	public AvailsValidatorTest() {
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
		File srcFile = new File(srcFilePath);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");

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
	public void testNoErrors_2_2_2() throws IOException, JDOMException {
		MddfTarget target = initialize("Avails_noErrors_v2.2.2.xml");
		execute(target);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	/**
	 * @throws JDOMException
	 * @throws IOException
	 * 
	 */
	@Test
	public void testNoErrors_2_3() throws IOException, JDOMException {
		MddfTarget target = initialize("Avails_noErrors_v2.3.xml");
		execute(target);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(12, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	/**
	 * @throws JDOMException
	 * @throws IOException
	 * 
	 */
	@Test
	public void testNoErrors_2_4() throws IOException, JDOMException {
		MddfTarget target = initialize("Avails_noErrors_v2.4.xml");
		execute(target);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	/**
	 * @throws JDOMException
	 * @throws IOException
	 * 
	 */
	@Test
	public void testVolumeErrors() throws IOException, JDOMException {
		MddfTarget target = initialize("Avails_Volume_error_v2.4.xml");
		execute(target);
		try {
			assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	/**
	 * @throws JDOMException
	 * @throws IOException
	 * 
	 */
	@Test
	public void testWithErrors() throws IOException, JDOMException {
		MddfTarget target = initialize("Avails_withErrors.xml");
		execute(target);
		try {
			assertEquals(6, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(1, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	protected void execute(MddfTarget target) throws IOException, JDOMException {
		iLog.setPrintToConsole(false);
		iLog.setMinLevel(LogMgmt.LEV_DEBUG);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + target.getSrcFile().getCanonicalPath(), null,
				"JUnit");
		Map<Object, Pedigree> pedigreeMap = null;
		try {
			super.process(target, pedigreeMap);
		} catch (Exception e) {
			throw new AssertionFailedError();
		}
	}

	private void dumpLog() {
		System.out.println("\n === FAILED TEST... dumping log ===");
		iLog.printLog();
		System.out.println(" === End log dump for FAILED TEST ===");
	}
}
