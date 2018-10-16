/**
 * Copyright (c) 2018 MovieLabs

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
package com.movielabs.mddflib.tests.common;

import java.io.File;
import java.util.MissingResourceException;

import org.jdom2.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.CMValidator;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public abstract class AbstractCmmTester extends CMValidator {

	protected static String rsrcPath = "./test/resources/";
	protected InstrumentedLogger iLog;

	/**
	 * @param validateC
	 * @param loggingMgr
	 */
	public AbstractCmmTester() {
		super(true, new InstrumentedLogger());
		iLog = (InstrumentedLogger) loggingMgr;
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
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
	}

	/**
	 * @param string
	 */
	protected void initialize(String testFileName) {
		Document xmlDoc = loadTestArtifact(testFileName);
		curRootEl = xmlDoc.getRootElement();
		FILE_FMT srcMddfFmt = MddfContext.identifyMddfFormat(curRootEl);		
		setMddfVersions(srcMddfFmt);
		rootNS = manifestNSpace;
	}

	private Document loadTestArtifact(String fileName) {
		String srcFilePath = rsrcPath + fileName;
		srcFile = new File(srcFilePath);
		if (!srcFile.canRead()) {
			throw new MissingResourceException("Missing test artifact "+srcFilePath, "File", srcFilePath);

		}
		Document xmlDoc;
		try {
			xmlDoc = XmlIngester.getAsXml(srcFile);
		} catch (Exception e) {
			return null;
		}
		return xmlDoc;
	}
}
