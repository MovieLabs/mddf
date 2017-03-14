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
package com.movielabs.mddflib.tests.common;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import org.jdom2.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.CMValidator;
import com.movielabs.mddflib.util.xml.SchemaWrapper;
import com.movielabs.mddflib.util.xml.XmlIngester;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * JUnit test of the basic functionality of the <tt>CMValidator</tt> class.
 * Testing uses two Manifest files: one is error-free and the other contains
 * errors. Test pass/fail criteria is the detection, and reporting, of the
 * correct number, type, and location of errors.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class CMValidatorTest extends CMValidator {

	private static String rsrcPath = "./test/resources/common/";
	private InstrumentedLogger iLog;

	public CMValidatorTest() {
		super(true, new InstrumentedLogger());
		iLog = (InstrumentedLogger) loggingMgr;
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
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
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
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.CMValidator#validateIndexing(java.lang.String, java.lang.String, java.lang.String)}
	 * .
	 */
	public void testValidateIndexing() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.CMValidator#validateNotEmpty(java.lang.String)}
	 * .
	 */
	@Test
	public void testValidateNotEmpty() {
		initialize("CM_withErrors.xml");
		SchemaWrapper targetSchema = SchemaWrapper.factory("md-v" + XmlIngester.MD_VER);
		validateNotEmpty(targetSchema);
		assertEquals(1, iLog.getCountForLevel(LogMgmt.LEV_ERR));
	}

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.CMValidator#validateId(java.lang.String, java.lang.String, boolean, boolean)}
	 * .
	 */
	@Test
	public void testValidateId() {
		id2typeMap = new HashMap<String, String>();
		id2typeMap.put("AudioTrackID", "audtrackid");
		id2typeMap.put("VideoTrackID", "vidtrackid");
		id2typeMap.put("ContentID", "cid");

		/*
		 * First run with error-free XML
		 */
		initialize("CM_base.xml");
		super.validateConstraints();

		validateId("Audio", "AudioTrackID", true, true);
		validateId("Video", "VideoTrackID", true, true);
		validateId("Metadata", "ContentID", true, true);
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));

		/* Reset and repeat with error-generating XML */
		iLog.clearLog();
		initialize("CM_ID-errors.xml");
		super.validateConstraints();
		validateId("Audio", "AudioTrackID", true, true);
		validateId("Video", "VideoTrackID", true, true);
		validateId("Metadata", "ContentID", true, true);
		assertEquals(3, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertNotNull(iLog.getMsg(LogMgmt.LEV_ERR, LogMgmt.TAG_MD, 25));
		assertNotNull(iLog.getMsg(LogMgmt.LEV_ERR, LogMgmt.TAG_MD, 33));
		assertEquals(1, iLog.getCountForLevel(LogMgmt.LEV_WARN));
		assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
	}

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.CMValidator#validateXRef(java.lang.String, java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public void testValidateXRef() {
		id2typeMap = new HashMap<String, String>();
		id2typeMap.put("AudioTrackID", "audtrackid");
		id2typeMap.put("VideoTrackID", "vidtrackid");
		id2typeMap.put("ContentID", "cid");

		/*
		 * First run with error-free XML
		 */
		initialize("CM_base.xml");
		super.validateConstraints();
		/*
		 * IDs must be processed before XREFs can be validated..
		 */
		validateId("Audio", "AudioTrackID", true, true);
		validateId("Video", "VideoTrackID", true, true);
		validateId("Metadata", "ContentID", true, true);

		validateXRef("Gallery", "ContentID", "Metadata");
		validateXRef("Audiovisual", "ContentID", "Metadata");
		validateXRef("VideoTrackReference", "VideoTrackID", "Video");
		validateXRef("AudioTrackReference", "AudioTrackID", "Audio");
		validateXRef("Experience", "ContentID", "Metadata");

		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		/*
		 * Reset and run file with errors
		 */
		iLog.clearLog();
		initialize("CM_IdXref-errors.xml");
		super.validateConstraints();
		/*
		 * IDs must be processed before XREFs can be validated..
		 */
		validateId("Audio", "AudioTrackID", true, true);
		validateId("Video", "VideoTrackID", true, true);
		validateId("Metadata", "ContentID", true, true);

		validateXRef("Gallery", "ContentID", "Metadata");
		validateXRef("Audiovisual", "ContentID", "Metadata");
		validateXRef("VideoTrackReference", "VideoTrackID", "Video");
		validateXRef("AudioTrackReference", "AudioTrackID", "Audio");
		validateXRef("Experience", "ContentID", "Metadata");

		assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		assertNotNull(iLog.getMsg(LogMgmt.LEV_ERR, LogMgmt.TAG_MD, 143));
		assertNotNull(iLog.getMsg(LogMgmt.LEV_ERR, LogMgmt.TAG_MD, 156));
	}

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.CMValidator#validateRatings()}.
	 */
	@Test
	public void testValidateRatings() {
		initialize("CM_withErrors.xml");
		boolean ok = validateRatings();
		assertFalse(ok);
		assertEquals(6, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(3, iLog.getCountForLevel(LogMgmt.LEV_WARN));
	}

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.CMValidator#validateLanguage(org.jdom2.Namespace, java.lang.String, org.jdom2.Namespace, java.lang.String)}
	 * .
	 */
	@Test
	public void testValidateLanguage() {
		initialize("CM_withErrors.xml");
		boolean ok = validateLanguage(mdNSpace, "PrimarySpokenLanguage", null, null);
		assertFalse(ok);
		assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_ERR));
	}

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.CMValidator#validateRegion(org.jdom2.Namespace, java.lang.String, org.jdom2.Namespace, java.lang.String)}
	 * .
	 */
	@Test
	public void testValidateRegion() {
		initialize("CM_withErrors.xml");
		boolean ok = validateRegion(mdNSpace, "DistrTerritory", mdNSpace, "country");
		assertFalse(ok);
		assertEquals(1, iLog.getCountForLevel(LogMgmt.LEV_ERR));
	}

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.CMValidator#validateVocab(org.jdom2.Namespace, java.lang.String, org.jdom2.Namespace, java.lang.String, net.sf.json.JSONArray, com.movielabs.mddflib.logging.LogReference, boolean)}
	 * .
	 */
	@Test
	public void testValidateVocab() {
		initialize("CM_withErrors.xml");
		JSONObject cmVocab = (JSONObject) getMddfResource("cm", MD_VER);

		JSONArray allowed = cmVocab.optJSONArray("WorkType");
		boolean ok = validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "WorkType", allowed, null, true);
		assertFalse(ok);
		assertEquals(1, iLog.getCountForLevel(LogMgmt.LEV_ERR));

		iLog.clearLog();
		allowed = cmVocab.optJSONArray("ReleaseType");
		ok = validateVocab(mdNSpace, "ReleaseHistory", mdNSpace, "ReleaseType", allowed, null, true);
		assertTrue(ok);
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
	}

	/**
	 * @param string
	 */
	protected void initialize(String testFileName) {
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

}
