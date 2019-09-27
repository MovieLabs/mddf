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
package com.movielabs.mddflib.tests.junit.util.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.avails.validation.AvailValidator;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.manifest.validation.ManifestValidator;
import com.movielabs.mddflib.manifest.validation.MecValidator;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.CMValidator;
import com.movielabs.mddflib.util.xml.MddfTarget;
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
public class StructureValidationTest extends StructureValidation{

	private static InstrumentedLogger iLog = new InstrumentedLogger();
	private static String rsrcPath = "./test/resources/";
	private JSONObject structDefs;
	private Element rootEl;
	private CMValidator validator;
	private File srcFile;
	private MddfTarget mddfTarget; 
	
	public StructureValidationTest() {
		super( iLog, "JUnit"); 
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
	@BeforeEach
	public void setUp() throws Exception {
		iLog.clearLog();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
	}

	// =====================================================
	// =========== START OF TESTS ==================+=======

	/**
	 * Test simplest and most basic functionality.
	 */
	@Test
	public void testBasicFunctions() {
		initialize("mec/mec1.xml", "structure.json");
		List<Element> basicElList = rootEl.getChildren("Basic", validator.mdmecNSpace);
		JSONObject basicMD = structDefs.getJSONObject("BasicMetadata");
		JSONArray rqmtSet = basicMD.getJSONArray("requirement");
		for (int j = 0; j < basicElList.size(); j++) {
			Element basicEl = basicElList.get(j);
			for (int i = 0; i < rqmtSet.size(); i++) {
				JSONObject nextRqmt = rqmtSet.getJSONObject(i);
				boolean isValid = evaluateConstraint(basicEl, nextRqmt, basicEl,null, null, null);
				assertTrue(isValid);
			}
		}
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			System.out.println("\n === FAILED TEST... dumping log ===");
			iLog.printLog();
			System.out.println(" === End log dump for FAILED TEST ===");
			throw e;
		}
	}

	/**
	 * 
	 */
	@Test
	public void testAvailsStruct_2_3_noErr() {
		String targetFile = "avails/Avails_Structure_Tests_v2.3.xml";
		initialize(targetFile, null);
		JSONObject structDefs = XmlIngester.getMddfResource("structure_avail", "2.3");
		JSONObject rqmtSet = structDefs.getJSONObject("StrucRqmts");
		Iterator<String> keys = rqmtSet.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject rqmtSpec = rqmtSet.getJSONObject(key);
			// NOTE: This block of code requires a 'targetPath' be defined
			if (rqmtSpec.has("targetPath")) {
				boolean isValid = validateDocStructure(rootEl, rqmtSpec, mddfTarget, null);
			}
		}
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			System.out.println("\n === FAILED TEST... dumping log ===");
			iLog.printLog();
			System.out.println(" === End log dump for FAILED TEST ===");
			throw e;
		}
	}

	/**
	 * 
	 */
	@Test
	public void testAvailsStruct_2_3_Errors() {
		String targetFile = "avails/Avails_Structure_Tests_v2.3_errors.xml";
		initialize(targetFile, null);
		JSONObject structDefs = XmlIngester.getMddfResource("structure_avail", "2.3");
		JSONObject rqmtSet = structDefs.getJSONObject("StrucRqmts");
		Iterator<String> keys = rqmtSet.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject rqmtSpec = rqmtSet.getJSONObject(key);
			// NOTE: This block of code requires a 'targetPath' be defined
			if (rqmtSpec.has("targetPath")) {
				boolean isValid = validateDocStructure(rootEl, rqmtSpec, mddfTarget, null);
			}
		}
		try {
			assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			System.out.println("\n === FAILED TEST... dumping log ===");
			iLog.printLog();
			System.out.println(" === End log dump for FAILED TEST ===");
			throw e;
		}
	}

	@Test
	public void testDigitalAsset_no_Errors() {
		String targetFile = "common/CM_base.xml";
		FILE_FMT srcMddfFmt = initialize(targetFile, null);
		Map<String, String> verMap = MddfContext.getReferencedXsdVersions(srcMddfFmt);
		JSONObject structDefs = XmlIngester.getMddfResource("structure_cm", verMap.get("MD"));
		JSONObject rqmtSet = structDefs.getJSONObject("StrucRqmts");
		Iterator<String> keys = rqmtSet.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject rqmtSpec = rqmtSet.getJSONObject(key);
			// NOTE: This block of code requires a 'targetPath' be defined
			if (rqmtSpec.has("targetPath")) {
				boolean isValid = validateDocStructure(rootEl, rqmtSpec, mddfTarget, null);
			}
		}
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			System.out.println("\n === FAILED TEST... dumping log ===");
			iLog.printLog();
			System.out.println(" === End log dump for FAILED TEST ===");
			throw e;
		}
	}

	@Test
	public void testDigitalAsset_Errors() {
		String targetFile = "common/CM_withErrors.xml";
		FILE_FMT srcMddfFmt = initialize(targetFile, null);
		Map<String, String> verMap = MddfContext.getReferencedXsdVersions(srcMddfFmt);
		JSONObject structDefs = XmlIngester.getMddfResource("structure_cm", verMap.get("MD"));
		JSONObject rqmtSet = structDefs.getJSONObject("StrucRqmts");
		Iterator<String> keys = rqmtSet.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject rqmtSpec = rqmtSet.getJSONObject(key);
			// NOTE: This block of code requires a 'targetPath' be defined
			if (rqmtSpec.has("targetPath")) {
				boolean isValid = validateDocStructure(rootEl, rqmtSpec, mddfTarget, null);
			}
		}
		try {
			assertEquals(5, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			System.out.println("\n === FAILED TEST... dumping log ===");
			iLog.printLog();
			System.out.println(" === End log dump for FAILED TEST ===");
			throw e;
		}
	}

	/**
	 * 
	 */
	private FILE_FMT initialize(String testFile, String jsonFile) {
		Document xmlDoc = loadTestArtifact(testFile);
		rootEl = xmlDoc.getRootElement();
		FILE_FMT srcMddfFmt = MddfContext.identifyMddfFormat(rootEl);
		switch (srcMddfFmt.getStandard()) {
		case "Avails":
			validator = new AvailValidator(true, iLog);
			break;
		case "MEC":
			validator = new MecValidator(true, iLog);
			break;
		case "Manifest":
			validator = new ManifestValidator(true, iLog);
			break;
		}
		validator.setMddfVersions(srcMddfFmt);
		
		if (jsonFile != null) {
			structDefs = loadJSON(jsonFile);
		}
		iLog.setPrintToConsole(false);
		iLog.setMinLevel(iLog.LEV_DEBUG);
		iLog.setInfoIncluded(true);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + testFile, null, "JUnit");
		return srcMddfFmt;
	}

	private Document loadTestArtifact(String fileName) {
		Document xmlDoc = null;
		String srcFilePath = rsrcPath + fileName;
		srcFile = new File(srcFilePath);
		try {
			mddfTarget = new MddfTarget(srcFile, iLog);
		} catch (FileNotFoundException e1) { 
//			e1.printStackTrace();
		}
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
}
