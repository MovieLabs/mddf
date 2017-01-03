/**
 * Copyright (c) 2016 MovieLabs

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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.movielabs.mddflib.avails.xlsx.AvailsSheet;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class RowToXmlHelperV1_6 extends RowToXmlHelper {

	/**
	 * @param sheet
	 * @param row
	 */
	RowToXmlHelperV1_6(AvailsSheet sheet, Row row) {
		super(sheet, row);
		// TODO Auto-generated constructor stub
	}

	protected String getData(String colKey) {
		String mappedKey = mapKey(colKey);
		if (mappedKey != null) {
			return super.getData(mappedKey);
		}

		System.out.println("getData(V1.6):: Unmapped key [" + colKey + "]");
		return null;
	}

	/**
	 * @param colKey
	 * @return
	 */
	protected Pedigree getPedigreedData(String colKey) {
		String mappedKey = mapKey(colKey);
		if (mappedKey != null) {
			return super.getPedigreedData(mappedKey);
		}
		// handle special situations
		switch (colKey) {
		case "Avail/BundledALIDs":
		case "AvailTrans/PriceCurrency":
		case "AvailTrans/AnnounceDate":
			return null;
		}
		System.out.println("getPedigreedData(V1.6):: Unmapped key [" + colKey + "]");
		return null;
	}

	private String mapKey(String colKey) {
		switch (colKey) {
		case "AvailTrans/AssetLanguage":
			return "AvailTrans/StoreLanguage";
		case "Avail/ALID":
			return "AvailMetadata/AltID";
		case "AvailAsset/EditID":
			return "AvailAsset/ProductID";
		case "AvailAsset/TitleID":
			return "AvailAsset/ContentID";
		case "AvailAsset/ContentID":
			return "AvailMetadata/AltID";
		case "AvailTrans/AllowedLanguages":
			return "AvailTrans/HoldbackExclusionLanguage";
		case "Avail/BundledALIDs":
		case "AvailTrans/PriceCurrency":
		case "AvailTrans/AnnounceDate":
			return null;
		default:
			return colKey;
		}
	}

}