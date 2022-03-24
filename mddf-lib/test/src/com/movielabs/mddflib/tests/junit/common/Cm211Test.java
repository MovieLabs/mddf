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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.movielabs.mddflib.logging.LogMgmt;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * JUnit test of the basic functionality of the <tt>CMValidator</tt> class. Test
 * pass/fail criteria is the detection, and reporting, of the correct number,
 * type, and location of errors.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class Cm211Test extends AbstractCmmTester {

	@Test
	public void testValidateCM211Errors() {
		initialize("common/CM_2.11/CM_v2.11_withErrors.xml");
		JSONObject cmVocab = (JSONObject) getVocabResource("cm", CM_VER);

		JSONArray allowed = cmVocab.optJSONArray("FilenameEmbedding@location");
		validateVocab(mdNSpace, "FilenameEmbedding", null, "@location", allowed, null, true, true);
		assertEquals(1, iLog.getCountForLevel(LogMgmt.LEV_ERR));
	}

	@Test
	public void testValidateCM211Base() {
		initialize("common/CM_2.11/CM_v2.11_base.xml");
		JSONObject cmVocab = (JSONObject) getVocabResource("cm", CM_VER);

		JSONArray allowed = cmVocab.optJSONArray("FilenameEmbedding@location");
		validateVocab(mdNSpace, "FilenameEmbedding", null, "@location", allowed, null, true, true);
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
	}
	
}
