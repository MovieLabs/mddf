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
package com.movielabs.mddflib.tests.junit.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.MissingResourceException;
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
import com.movielabs.mddflib.manifest.validation.MecValidator;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.CMValidator;
import com.movielabs.mddflib.util.xml.MddfTarget;

/**
 * Test cases specific to the use of CM v2.7.1 in the context of either Manifest
 * or MEC files. This is a special situation in that there are several ways that
 * may be used to define the XML header and namespaces so that they indicate
 * that CM 2.7.1 is being used instead of CM 2.7.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class CM_Migration_Test {

	private static String rsrcPath = "./test/resources/common/CM_2.7.1/";
	private InstrumentedLogger iLog = new InstrumentedLogger();

	public CM_Migration_Test() {
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
		iLog.setMinLevel(iLog.LEV_DEBUG);
	}

	@AfterEach
	public void tearDown() {
	}

	/**
	 * @param string
	 */
	protected MddfTarget initialize(String testFileName) {
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);
		if (!srcFile.exists()) {
			throw new MissingResourceException("Missing test artifact " + srcFilePath, "File", srcFilePath);
		}
		MddfTarget target = new MddfTarget(srcFile, iLog);
		return target;

	}

	/**
	 * Test case:
	 * <ul>
	 * <li>Manifest v1.8 using CM v2.7</li>
	 * <li>schema location is specified and point to v1.8 XSD</li>
	 * </ul>
	 * Header:
	 * 
	 * <pre>
	 * 	xmlns:manifest="http://www.movielabs.com/schema/manifest/v1.8/manifest"
	xmlns:md="http://www.movielabs.com/schema/md/v2.7/md" 
	xsi:schemaLocation="http://www.movielabs.com/schema/manifest/v1.8/manifest manifest-v1.8.xsd"
	 * </pre>
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	@Test
	public void testFull1_v1_8_A() throws IOException, JDOMException {
		// v1.8 with schemaLocation
		MddfTarget target = initialize("Manifest_v1.8_A.xml");
		CMValidator validator = new ManifestValidator(true, iLog);
		execute(target, validator);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	/**
	 * Test case:
	 * <ul>
	 * <li>Manifest v1.8 using CM v2.7</li>
	 * <li>schema location is NOT specified</li>
	 * </ul>
	 * Header:
	 * 
	 * <pre>
	 * 	xmlns:manifest="http://www.movielabs.com/schema/manifest/v1.8/manifest"
	xmlns:md="http://www.movielabs.com/schema/md/v2.7/md"
	 * </pre>
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	@Test
	public void testFull1_v1_8_B() throws IOException, JDOMException {
		// v1.8 with out schemaLocation
		MddfTarget target = initialize("Manifest_v1.8_B.xml");
		CMValidator validator = new ManifestValidator(true, iLog);
		execute(target, validator);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	/**
	 * Test case:
	 * <ul>
	 * <li>Manifest v1.8.1 using CM v2.7.1</li>
	 * <li>'manifest' URI identifies v1.8
	 * <li>'md' URI identifies CM v2.7
	 * <li>schema location is specified and points to v1.8.1 XSD</li>
	 * </ul>
	 * Header:
	 * 
	 * <pre>
	 * 	xmlns:manifest="http://www.movielabs.com/schema/manifest/v1.8/manifest"
	xmlns:md="http://www.movielabs.com/schema/md/v2.7/md" 
	xsi:schemaLocation="http://www.movielabs.com/schema/manifest/v1.8/manifest manifest-v1.8.1.xsd"
	 * </pre>
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	@Test
	public void testFull1_v1_8_1_A() throws IOException, JDOMException {
		MddfTarget target = initialize("Manifest_v1.8.1_A.xml");
		CMValidator validator = new ManifestValidator(true, iLog);
		execute(target, validator);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	/**
	 * Test with INVALID file that will result in a FATAL validation error:
	 * 
	 * Test case:
	 * <ul>
	 * <li>Manifest v1.8.1 using CM v2.7.1</li>
	 * <li>'manifest' URI identifies v1.8.1
	 * <li>'md' URI identifies CM v2.7.1
	 * <li>schema location is specified and points to v1.8.1 XSD</li>
	 * </ul>
	 * Header:
	 * 
	 * <pre>
	 * 	xmlns:manifest="http://www.movielabs.com/schema/manifest/v1.8/manifest"
	xmlns:md="http://www.movielabs.com/schema/md/v2.7/md" 
	xsi:schemaLocation="http://www.movielabs.com/schema/manifest/v1.8/manifest manifest-v1.8.1.xsd"
	 * </pre>
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	@Test
	public void testFull1_v1_8_1_B() throws IOException, JDOMException {
		MddfTarget target = initialize("Manifest_v1.8.1_B.xml");
		CMValidator validator = new ManifestValidator(true, iLog);
		execute(target, validator);
		try {
			assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	@Test
	public void testMEC_v2_7() throws IOException, JDOMException {
		MddfTarget target = initialize("MEC_v2.7_F.xml");
		CMValidator validator = new MecValidator(true, iLog);
		execute(target, validator);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	/**
	 * Test case:
	 * <ul>
	 * <li>MEC using CM v2.7.1</li>
	 * <li>'mdmec' URI identifies v2.7
	 * <li>'md' URI identifies CM v2.7
	 * <li>schema location is specified and points to v2.7.1 XSD</li>
	 * </ul>
	 * Header:
	 * 
	 * <pre>
	<mdmec:CoreMetadata xmlns:mdmec="http://www.movielabs.com/schema/mdmec/v2.7"
	        xmlns:md="http://www.movielabs.com/schema/md/v2.7/md"
		    xmlns:xs="http://www.w3.org/2001/XMLSchema"
		    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		    xsi:schemaLocation=
	"http://www.movielabs.com/schema/mdmec/v2.7 mdmec-v2.7.1.xsd">
	 * </pre>
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	@Test
	public void testMEC_v2_7_1() throws IOException, JDOMException {
		MddfTarget target = initialize("MEC_v2.7.1_D.xml");
		CMValidator validator = new MecValidator(true, iLog);
		execute(target, validator);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_FATAL));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	protected void execute(MddfTarget target, CMValidator validator) throws IOException, JDOMException {
		Element rootEl = target.getXmlDoc().getRootElement();
		List<Namespace> nsList = rootEl.getNamespacesInScope();
		iLog.setPrintToConsole(false);
		iLog.setMinLevel(iLog.LEV_DEBUG);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + target.getSrcFile().getCanonicalPath(), null,
				"JUnit");
		validator.process(target);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Test completed", null, "JUnit");
		iLog.setPrintToConsole(false);
	}

	private void dumpLog() {
		System.out.println("\n\n === FAILED TEST... dumping log ===");
		iLog.printLog();
		System.out.println(" === End log dump for FAILED TEST ===");
	}
}
