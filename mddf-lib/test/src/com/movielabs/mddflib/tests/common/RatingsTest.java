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

import com.movielabs.mddflib.logging.LogMgmt;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class RatingsTest extends AbstractCmmTester {
	@Test
	public void testWithErrors() {
		initialize("common/CM_withErrors.xml");
		validateRatings();
		assertEquals(6, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(3, iLog.getCountForLevel(LogMgmt.LEV_WARN));
	}

	@Test
	public void availsNoErrors() {
		initialize("avails/Avails_noErrors.xml");
		validateRatings();
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
	}

	@Test
	public void availsWithErrors() {
		initialize("avails/Avails_withErrors.xml");
		validateRatings();
		assertEquals(2, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(1, iLog.getCountForLevel(LogMgmt.LEV_WARN));
	}

	@Test
	public void manifestNoErrors() {
		initialize("manifest/MMM_v1.7_base.xml");
		validateRatings();
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(0, iLog.getCountForLevel(LogMgmt.LEV_WARN));
	}

	@Test
	public void manifestWithErrors() {
		initialize("manifest/MMM_v1.7_errors.xml");
//		iLog.setPrintToConsole(true);
		validateRatings();
//		iLog.setPrintToConsole(false);
		assertEquals(3, iLog.getCountForLevel(LogMgmt.LEV_ERR));
		assertEquals(1, iLog.getCountForLevel(LogMgmt.LEV_WARN));
	}

}
