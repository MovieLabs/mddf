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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.XmlIngester;

import net.sf.json.JSONObject;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class DigitalAssetTest extends AbstractCmmTester {

	@Test
	public void testWithNoErrors() {
		initialize("manifest/Manifest_v1.8_A.xml");
		JSONObject structDefs = XmlIngester.getMddfResource("vocab_digAsset", "2.7");
		String pre = manifestNSpace.getPrefix();
		JSONObject rqmtSet = structDefs.getJSONObject("Audio");
		validateDigitalAsset(".//" + pre + ":Audio", curRootEl, rqmtSet);
		rqmtSet = structDefs.getJSONObject("Video");
		validateDigitalAsset(".//" + pre + ":Video", curRootEl, rqmtSet);
		rqmtSet = structDefs.getJSONObject("Subtitle");
		validateDigitalAsset(".//" + pre + ":Subtitle", curRootEl, rqmtSet);
		try {
			assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		} catch (AssertionFailedError e) {
			System.out.println("\n === FAILED TEST... dumping log ===");
			iLog.printLog();
			System.out.println(" === End log dump for FAILED TEST ===");
			throw e;
		}
		System.out.println("\n === DEBUGGING ... dumping log ===");
		iLog.printLog();
		System.out.println(" === End log dump ===");
	
	}

}