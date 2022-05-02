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

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import org.jdom2.JDOMException;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.MddfTarget;
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
public class CMValidatorTest extends AbstractCmmTester {

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.CMValidator#validateNotEmpty(java.lang.String)}
	 * .
	 */
	@Test
	public void testValidateNotEmpty() {
		initialize("common/CM_withErrors.xml");
		SchemaWrapper targetSchema = SchemaWrapper.factory("md-v" + CM_VER);
		validateNotEmpty(targetSchema);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		} catch (AssertionFailedError e) {
			System.out.println("\n === FAILED TEST... dumping log ===");
			iLog.printLog();
			System.out.println(" === End log dump for FAILED TEST ===");
			throw e;
		}
	}

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.CMValidator#validateId(java.lang.String, java.lang.String, boolean, boolean)}
	 * .
	 */
	@Test
	public void testValidateId_OK() {
		id2typeMap = new HashMap<String, String>();
		id2typeMap.put("AudioTrackID", "audtrackid");
		id2typeMap.put("VideoTrackID", "vidtrackid");
		id2typeMap.put("ContentID", "cid");

		/*
		 * run with error-free XML
		 */
		initialize("common/CM_base.xml");
		initializeIdChecks() ;
		super.validateConstraints();
		int count = 0;
		Set idSet = validateId("Audio", "AudioTrackID", true, true);
		count = count + idSet.size();
		idSet = validateId("Video", "VideoTrackID", true, true);
		count = count + idSet.size();
		idSet = validateId("Metadata", "ContentID", true, true);
		count = count + idSet.size();
		System.out.println(".....idCount = " + count);
		try {
			assertEquals(3, count);
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
	public void testValidateId_NOK() {
		id2typeMap = new HashMap<String, String>();
		id2typeMap.put("AudioTrackID", "audtrackid");
		id2typeMap.put("VideoTrackID", "vidtrackid");
		id2typeMap.put("ContentID", "cid");
		/* Run with error-generating XML */
		initialize("common/CM_ID-errors.xml");
		initializeIdChecks() ;
		super.validateConstraints();
		validateId("Audio", "AudioTrackID", true, true);
		validateId("Video", "VideoTrackID", true, true);
		validateId("Metadata", "ContentID", true, true);
		try {
			assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertNotNull(iLog.getMsg(LogMgmt.LEV_ERR, LogMgmt.TAG_MD, 34));
			assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertNotNull(iLog.getMsg(LogMgmt.LEV_WARN, LogMgmt.TAG_MD, 26));
			assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
		} catch (AssertionFailedError e) {
			System.out.println("\n === FAILED TEST... dumping log ===");
			iLog.printLog();
			System.out.println(" === End log dump for FAILED TEST ===");
			throw e;
		}
	}

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.CMValidator#validateXRef(java.lang.String, java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public void testValidateXRef_noErr() {
		id2typeMap = new HashMap<String, String>();
		id2typeMap.put("AudioTrackID", "audtrackid");
		id2typeMap.put("VideoTrackID", "vidtrackid");
		id2typeMap.put("ContentID", "cid");

		initialize("common/CM_base.xml");
		initializeIdChecks() ;
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
	public void testValidateXRef_errors() {

		id2typeMap = new HashMap<String, String>();
		id2typeMap.put("AudioTrackID", "audtrackid");
		id2typeMap.put("VideoTrackID", "vidtrackid");
		id2typeMap.put("ContentID", "cid");

		initialize("common/CM_IdXref-errors.xml");
		initializeIdChecks() ;
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
		try {
			assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_ERR));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_NOTICE));
			assertNotNull(iLog.getMsg(LogMgmt.LEV_ERR, LogMgmt.TAG_MD, 143));
			assertNotNull(iLog.getMsg(LogMgmt.LEV_ERR, LogMgmt.TAG_MD, 156));
		} catch (AssertionFailedError e) {
			System.out.println("\n === FAILED TEST... dumping log ===");
			iLog.printLog();
			System.out.println(" === End log dump for FAILED TEST ===");
			throw e;
		}
	}

	/**
	 * Test method for
	 * {@link com.movielabs.mddflib.util.CMValidator#validateVocab(org.jdom2.Namespace, java.lang.String, org.jdom2.Namespace, java.lang.String, net.sf.json.JSONArray, com.movielabs.mddflib.logging.LogReference, boolean)}
	 * .
	 */
	@Test
	public void testValidateVocab() {
		initialize("common/CM_withErrors.xml");
		JSONObject cmVocab = (JSONObject) getVocabResource("cm", CM_VER);

		JSONArray allowed = cmVocab.optJSONArray("WorkType");
		validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "WorkType", allowed, null, true);
		assertEquals(1, iLog.getCountForLevel(LogMgmt.LEV_ERR));

		iLog.clearLog();
		allowed = cmVocab.optJSONArray("ReleaseType");
		validateVocab(mdNSpace, "ReleaseHistory", mdNSpace, "ReleaseType", allowed, null, true);
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
	}


}
