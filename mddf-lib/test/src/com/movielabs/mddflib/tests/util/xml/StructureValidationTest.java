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
package com.movielabs.mddflib.tests.util.xml;
 

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test; 
import static org.junit.jupiter.api.Assertions.*; 

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.xml.StructureValidation;
import com.movielabs.mddflib.util.xml.XmlIngester;

import net.sf.json.JSONArray; 
import net.sf.json.JSONObject;

/**
 * JUnit test for the
 * <tt>com.movielabs.mddflib.util.xml.StructureValidation</tt> class. Testing
 * uses the MEC test resources (i.e., XML test files and structural
 * definitions).
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class StructureValidationTest {

	private static InstrumentedLogger iLog;
	private static String rsrcPath = "./test/resources/mec/";
	private JSONObject structDefs;
	private Element rootEl;
	private StructureValidation validator;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		iLog = new InstrumentedLogger();
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
	@BeforeEach
	public void setUp() throws Exception {
		iLog.clearLog();
		Document xmlDoc = loadTestArtifact("mec1.xml");
		rootEl = xmlDoc.getRootElement();
		String mecSchemaVer = XmlIngester.identifyXsdVersion(rootEl);
		XmlIngester.setMdMecVersion(mecSchemaVer);
		structDefs = loadJSON("structure.json");
		validator = new StructureValidation(iLog, "JUnit");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
	}

	private Document loadTestArtifact(String fileName) {
		Document xmlDoc = null;
		String srcFilePath = rsrcPath + fileName;
		File srcFile = new File(srcFilePath);
		if (srcFile.canRead()) {
			try {
				xmlDoc = XmlIngester.getAsXml(srcFile);
			} catch (Exception e) {
			}
		}
		assertNotNull(xmlDoc);
		return xmlDoc;
	}

	protected static JSONObject loadJSON(String fileName) {
		String input = null;
		String srcFilePath = fileName;
		InputStream inp = StructureValidationTest.class.getResourceAsStream(srcFilePath);
		StringBuilder builder;
		try {
			InputStreamReader isr = new InputStreamReader(inp, "UTF-8");
			BufferedReader reader = new BufferedReader(isr);
			builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			input = builder.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertNotNull(input);
		return JSONObject.fromObject(input);
	}

	// =====================================================
	// =========== START OF TESTS ==================+=======

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.xml.StructureValidation#validateStructure(org.jdom2.Element, net.sf.json.JSONObject)}
	 * .
	 */
	@Test
	public void testValidateStructure() {
		List<Element> basicElList = rootEl.getChildren("Basic", XmlIngester.mdmecNSpace);
		JSONObject basicMD = structDefs.getJSONObject("BasicMetadata");
		JSONArray rqmtSet = basicMD.getJSONArray("requirement");
		for (int j =0; j <basicElList.size(); j++){
			Element basicEl = basicElList.get(j);
			for (int i = 0; i < rqmtSet.size(); i++) {
				JSONObject nextRqmt = rqmtSet.getJSONObject(i);
				boolean isValid = validator.validateConstraint(basicEl, nextRqmt);
				assertTrue(isValid);
			}
		} 
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
	}
}
