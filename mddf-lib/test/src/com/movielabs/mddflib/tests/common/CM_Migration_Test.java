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
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.manifest.validation.ManifestValidator;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.xml.MddfTarget;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class CM_Migration_Test extends ManifestValidator {

	private static String rsrcPath = "./test/resources/manifest/";
	private InstrumentedLogger iLog;

	public CM_Migration_Test() {
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

	@AfterEach
	public void tearDown() { 
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

	@Test
	public void testFull1_v1_8() throws IOException, JDOMException {
		// v1.8 with schemaLocation
		MddfTarget target = initialize("Manifest_v1.8_A.xml");
		execute(target);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	@Test
	public void testFull1_v1_8_B() throws IOException, JDOMException {
		// v1.8 with out schemaLocation
		MddfTarget target = initialize("Manifest_v1.8_B.xml");
		execute(target);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	@Test
	public void testFull1_v1_8_1_A() throws IOException, JDOMException {
		MddfTarget target = initialize("Manifest_v1.8.1_A.xml");
		execute(target);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}
	@Test
	public void testFull1_v1_8_1_B() throws IOException, JDOMException {
		MddfTarget target = initialize("Manifest_v1.8.1_B.xml");
		execute(target);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}
	protected void execute(MddfTarget target) throws IOException, JDOMException {
		Element rootEl = target.getXmlDoc().getRootElement();
		List<Namespace> nsList = rootEl.getNamespacesInScope();
		iLog.setPrintToConsole(true);
		iLog.setMinLevel(iLog.LEV_DEBUG);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + target.getSrcFile().getCanonicalPath(), null,
				"JUnit");
		super.process(target);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Test completed", null, "JUnit");
		iLog.setPrintToConsole(false);
	}

	private void dumpLog() {
		System.out.println("\n === FAILED TEST... dumping log ===");
		iLog.printLog();
		System.out.println(" === End log dump for FAILED TEST ===");
	}
}
