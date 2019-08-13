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
package com.movielabs.mddflib.avails.xml;

import org.apache.poi.ss.usermodel.Row;
import org.jdom2.Element;
import org.jdom2.Namespace;

import com.movielabs.mddflib.logging.LogMgmt;

/**
 * Create XML document from a v1.7.2 Excel spreadsheet. The XML generated will
 * be based on v2.2 of the Avails XSD and reflects a "best effort" in that there
 * is no guarantee that it is valid.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class RowToXmlHelperV1_7_2 extends RowToXmlHelperV1_7 {

	static final String MISSING = "--FUBAR (missing)";

	/**
	 * @param fields
	 */
	RowToXmlHelperV1_7_2(AvailsSheet sheet, Row row, LogMgmt logger) {
		super(sheet, row, logger);
	}

	public Element[] process(Element parentEl, String childName, Namespace ns, String cellKey, String separator) {
		switch (cellKey) {
		case "AvailTrans/FormatProfile":
			return processFmtProfile(parentEl, childName, ns, cellKey, separator);
		default:
			return super.process(parentEl, childName, ns, cellKey, separator);

		}

	}

	/**
	 * @param parentEl
	 * @param childName
	 * @param ns
	 * @param cellKey
	 * @param separator
	 * @return
	 */
	protected Element[] processFmtProfile(Element parentEl, String childName, Namespace ns, String cellKey,
			String separator) {
		// step 1: gen a single FormatProfile element the standard way
		Element[] fmtProList = super.process(parentEl, childName, ns, cellKey, separator);
		if (fmtProList == null || (fmtProList.length == 0)) {
			return null;
		}
		Element fmtProEl = fmtProList[0];
		// now add the optional arguments
		addArgument(fmtProEl, "HDR", "AvailTrans/HDR");
		addArgument(fmtProEl, "WCG", "AvailTrans/WCG");
		addArgument(fmtProEl, "HFR", "AvailTrans/HFR");
		addArgument(fmtProEl, "NGAudio", "AvailTrans/NGAudio");
		return fmtProList;
	}

	/**
	 * @param parentEl
	 * @param attribute
	 * @param cellKey
	 */
	protected void addArgument(Element parentEl, String attribute, String cellKey) {
		Pedigree pg = getPedigreedData(cellKey); 
		String value = pg.getRawValue();
		if (isSpecified(value)) {
			parentEl.setAttribute(attribute, value);
		}
	}

}
