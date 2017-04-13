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

import org.apache.poi.ss.usermodel.Row;
import org.jdom2.Element;

import com.movielabs.mddflib.avails.xlsx.AvailsSheet;

/**
 * * Create XML document from a v1.6 Excel spreadsheet. The XML generated will
 * be based on v2.1 of the Avails XSD and reflects a "best effort" in that there
 * is no guarantee that it is valid.
 * 
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
	 * populate a Transaction element
	 * 
	 * @param transactionEl
	 */
	protected void processTransactionBody(Element transactionEl) {
		String prefix = "AvailTrans/";
		process(transactionEl, "LicenseType", xb.getAvailsNSpace(), prefix + "LicenseType");
		/*
		 * 'Description' is a SPECIAL CASE. The 2.1 XSD has it as REQUIRED but
		 * the 1.6 Excel has it as OPTIONAL.
		 * 
		 */ 
		Pedigree pg = getPedigreedData(prefix + "Description");
		String value = pg.getRawValue();
		if(value.isEmpty()){
			value = "not provided";
		}
		Element childEl = mGenericElement("Description", value,  xb.getAvailsNSpace());
		transactionEl.addContent(childEl);
		xb.addToPedigree(childEl, pg);
		
		addRegion(transactionEl, "Territory", xb.getAvailsNSpace(), prefix + "Territory");
		// Start or StartCondition
		processCondition(transactionEl, "Start", xb.getAvailsNSpace(), prefix + "Start");
		// End or EndCondition
		processCondition(transactionEl, "End", xb.getAvailsNSpace(), prefix + "End");

		process(transactionEl, "StoreLanguage", xb.getAvailsNSpace(), prefix + "StoreLanguage", ",");
		process(transactionEl, "LicenseRightsDescription", xb.getAvailsNSpace(), prefix + "LicenseRightsDescription");
		process(transactionEl, "FormatProfile", xb.getAvailsNSpace(), prefix + "FormatProfile");
		process(transactionEl, "ContractID", xb.getAvailsNSpace(), prefix + "ContractID");

		addAllTerms(transactionEl);

		process(transactionEl, "OtherInstructions", xb.getAvailsNSpace(), prefix + "OtherInstructions");

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
			if (sheet.isForTV()) {
				/*
				 * ALID replaces EpisodeAltID or SeasonAltID depending on
				 * whether it's an episode or season avail.
				 */

				if (workType.equals("Episode")) {
					return "AvailMetadata/EpisodeAltID";
				} else if (workType.equals("Season")) {
					return "AvailMetadata/SeasonAltID";
				} else {
					return null;
				}
			} else {
				return "AvailMetadata/AltID";
			}
		case "Avail/ExceptionFlag":
			if (sheet.isForTV()) {
				/*
				 * This handles a typo in the v1.6 template for TV Avails
				 */
				return "Avail/ExceptionsFlag";
			} else {
				return "Avail/ExceptionFlag";
			}
		case "AvailAsset/EditID":
			return "AvailAsset/ProductID";
		case "AvailAsset/TitleID":
			return "AvailAsset/ContentID";
		// ?????????????????????????????????
		case "AvailAsset/ContentID":
			return "AvailMetadata/AltID";
		// ..................................
		case "AvailTrans/AllowedLanguages":
			return "AvailTrans/HoldbackExclusionLanguage";
		case "AvailMetadata/EpisodeID":
			return "AvailMetadata/EpisodeAltID";
		case "AvailMetadata/SeasonID":
			return "AvailMetadata/SeasonAltID";
		case "AvailMetadata/SeriesID":
			return "AvailMetadata/SeriesAltID";
		case "Avail/BundledALIDs":
		case "AvailTrans/PriceCurrency":
		case "AvailTrans/AnnounceDate":
			return null;
		default:
			return colKey;
		}
	}

	/**
	 * Filter out deprecated PriceType terms. Since v1.6 is the 'base' version,
	 * there are no deprecated vocabulary and the implementation simply returns
	 * the input <tt>value</tt>.
	 * 
	 * @param colKey
	 * @param value
	 * @return
	 */
	protected Pedigree filterDeprecated(Pedigree pg) {
		return pg;
	}

}