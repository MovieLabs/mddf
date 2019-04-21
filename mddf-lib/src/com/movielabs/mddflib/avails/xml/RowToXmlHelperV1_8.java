/**
 * Copyright (c) 2019 MovieLabs

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
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class RowToXmlHelperV1_8 extends RowToXmlHelperV1_7_3 {

	/**
	 * @param sheet
	 * @param row
	 * @param logger
	 */
	RowToXmlHelperV1_8(AvailsSheet sheet, Row row, LogMgmt logger) {
		super(sheet, row, logger);
	}

	protected Element buildAsset() {
		Element assetEl = super.buildAsset();
		// is this an Episode??
		Namespace availNS = xb.getAvailsNSpace();
		String wt = assetEl.getChildText("WorkType", availNS);
		if (!wt.equals("Episode")) {
			return assetEl;
		}
		Pedigree pg = getPedigreedData("AvailMetadata/VolumeNumber");
		if (isSpecified(pg)) {
			String volNum = pg.getRawValue();
			// add TEMPORARY attribute that will be removed during finalization
			assetEl.setAttribute("volNum", volNum);
		}
		return assetEl;
	}

	protected void addAllTerms(Element transactionEl) {
		super.addAllTerms(transactionEl);

		/* Now add Terms that are new for v1.8 */
		addTerm(transactionEl, "AvailTrans/Bonus", "Bonus", "Text");
		addTerm(transactionEl, "AvailAsset/PackageLabel", "PackageLabel", "Text");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddflib.avails.xml.AbstractRowHelper#locateContentID(java.lang.
	 * String)
	 */
	@Override
	protected String locateContentID(String workType) {
		String cidPrefix = "";
		switch (workType) {
		case "Series":
		case "Season":
		case "Episode":
		case "Volume":
			cidPrefix = workType;
			break;
		default:
		}
		String colKey = "AvailAsset/" + cidPrefix + "ContentID";
		return colKey;
	}
}
