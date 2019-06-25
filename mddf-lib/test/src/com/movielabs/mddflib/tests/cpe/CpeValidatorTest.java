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
package com.movielabs.mddflib.tests.cpe;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.tree.DefaultTreeModel;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.manifest.validation.CpeValidator;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class CpeValidatorTest extends CpeValidator {

	private static String rsrcPath = "./test/resources/cpe/";
	private InstrumentedLogger iLog;

	public CpeValidatorTest() {
		super(new InstrumentedLogger());
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
		iLog.setPrintToConsole(true);
		iLog.setMinLevel(iLog.LEV_DEBUG);
	}

	/**
	 * @param string
	 */
	protected void initialize(String testFileName) {
		try {
			setUp();
		} catch (Exception e) {
			assertNotNull(curRootEl);
			return;
		}
		Document xmlDoc = loadTestArtifact(testFileName);
		if (xmlDoc == null) {
			// forces a FAILED result
			assertNotNull(curRootEl);
			return;
		}
		curRootEl = xmlDoc.getRootElement();
		String schemaVer = identifyXsdVersion(curRootEl);
		setManifestVersion(schemaVer);
		rootNS = manifestNSpace;
	}

	private Document loadTestArtifact(String fileName) {
		String srcFilePath = rsrcPath + fileName;
		srcFile = new File(srcFilePath);
		if (!srcFile.canRead()) {
			return null;
		}
		Document xmlDoc;
		try {
			xmlDoc = XmlIngester.getAsXml(srcFile);
		} catch (Exception e) {
			return null;
		}
		return xmlDoc;
	}

	/**
	 * 
	 */
	@Test
	public void testValidataMetadata() {
		/*
		 * First run with error-free XML
		 */
		initialize("CPE_base_v1.0.xml");
		super.validateMetadata();
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}

		/*
		 * Repeat with error-generating XML:
		 */
		iLog.clearLog();
		initialize("CPE_errors_v1.0.xml");
		super.validateMetadata();
		try {
			assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	@Test
	public void testAlidExtraction() {
		initialize("CPE_base_v1.0.xml");
		List<Element> primaryExpSet = extractAlidMap(curRootEl);
		try {
			assertNotNull(primaryExpSet);
			assertEquals(1, primaryExpSet.size());
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	@Test
	public void testBuildInfoModel() {
		initialize("CPE_base_v1.0.xml");
		try {
			DefaultTreeModel infoModel = buildInfoModel();
			assertNotNull(infoModel);
			ExperienceNode root = (ExperienceNode) infoModel.getRoot();
			assertNotNull(root);
			assertEquals(5, root.getDescendents().size());
		} catch (AssertionFailedError e) {
			dumpLog();
			throw e;
		}
	}

	protected void execute(MddfTarget target, String profileId) throws IOException, JDOMException {
		iLog.setPrintToConsole(true);
		iLog.setMinLevel(LogMgmt.LEV_DEBUG);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + target.getSrcFile().getCanonicalPath(), null,
				"JUnit");
		List<String> useCases = null;
		try {
			super.process(target, profileId, useCases);
		} catch (Exception e) {
			throw new AssertionFailedError();
		}
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Test completed", null, "JUnit");
		iLog.setPrintToConsole(false);

	}

	private void dumpLog() {
		System.out.println("\n\n === FAILED TEST... dumping log ===");
		iLog.printLog();
		System.out.println(" === End log dump for FAILED TEST ===");
	}
}
