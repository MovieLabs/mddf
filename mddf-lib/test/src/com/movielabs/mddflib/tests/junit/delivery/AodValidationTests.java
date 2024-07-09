/**
 * Copyright (c) 2020 MovieLabs

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
package com.movielabs.mddflib.tests.junit.delivery;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.jdom2.JDOMException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.movielabs.mddflib.delivery.AodValidator;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.status.offer.OfferStatusValidator;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.xml.MddfTarget;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 * TODO Automate this test so it runs on build
 */
class AodValidationTests {

	private static final String MODULE_ID = "AodValidationTests";
	private static String rsrcPath = "./test/resources/delivery/";
	private InstrumentedLogger iLog;
	private AodValidator validator;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		iLog = new InstrumentedLogger();
		iLog.setPrintToConsole(true);
		iLog.setMinLevel(iLog.LEV_DEBUG);
		validator = new AodValidator(true, iLog);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void testV11() {
		validateFile("AssetAvailability_v1.1_no-errors.xml");
		int errCnt = iLog.getCountForLevel(LogMgmt.LEV_ERR);
		assertEquals(0, errCnt);
	}
	
	@Test
	void testV12() {
		validateFile("AssetAvailability_no-errors_v1.2.xml");
		int errCnt = iLog.getCountForLevel(LogMgmt.LEV_ERR);
		assertEquals(0, errCnt);
	}

	

	protected boolean validateFile(String filePath) {
		String srcFilePath = rsrcPath + filePath;
		iLog.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_ACTION, "validating file at " + srcFilePath, null, MODULE_ID);
		File srcFile = new File(srcFilePath);
		assertTrue(srcFile.canRead());
		MddfTarget target = new MddfTarget(srcFile, iLog);
		iLog.pushFileContext(target);
		AodValidator tool1 = new AodValidator(true, iLog);
		try {
			return tool1.process(target);
		} catch (Exception e) {
			iLog.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_ACTION, e.getLocalizedMessage(), target, MODULE_ID);
			e.printStackTrace();
			return false;
		}
	}

}
