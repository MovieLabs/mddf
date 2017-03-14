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

import static org.junit.Assert.*;
import java.io.File;

import org.jdom2.Document;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.manifest.validation.ManifestValidator;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class ManifestValidatorTest extends ManifestValidator {


	private static String rsrcPath = "./test/resources/manifest/";
	private InstrumentedLogger iLog;

	public ManifestValidatorTest() {
		super(true, new InstrumentedLogger());
		iLog = (InstrumentedLogger)loggingMgr;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
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
	protected void initialize(String testFileName) {
		curRootEl = null;
		Document xmlDoc = loadTestArtifact(testFileName);
		if (xmlDoc == null) {
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
	public void testValidataMetadata(){
		/*
		 * First run with error-free XML
		 */
		initialize("MMM_base_v1.6.xml");
		super.validateMetadata();
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		
		/*
		 * Repeat with error-generating XML:
		 */
		iLog.clearLog();
		initialize("MMM_wErrors_v1.6.xml");
		super.validateMetadata();
		assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
	}
 
}
