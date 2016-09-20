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
package com.movielabs.mddflib.xml;

import java.text.ParseException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.movielabs.mddflib.AvailsSheet;
import com.movielabs.mddflib.ISO639_2_Code;

/**
 * Code and functionality formerly in SheetRow. The XML generated reflects a
 * "best effort" in that there is no guarantee that it is valid.
 * <p>
 * This class is intended to have a low footprint in terms of memory usage so as
 * to facilitate processing of sheets with large row counts. Note that Excel
 * 2010 supports up to 1,048,576 rows.
 * </p>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public  class RowToXmlHelper {

	private static final String MISSING = "--FUBAR (missing)";
	protected int row;
	protected String shortDesc = ""; // default
	protected Document dom;
	protected String availType;
	private AvailsSheet sheet;

	/**
	 * @param fields
	 */
	RowToXmlHelper(AvailsSheet sheet, int row, String desc) {
		super();
		this.sheet = sheet;
		this.row = row;
		if (desc != null) {
			this.shortDesc = desc;
		}

	}

	protected Element makeAvail(Document dom) throws Exception {
		this.dom = dom;
		Element avail = dom.createElement("Avail");
		Element e;

		// ALID
		String value = sheet.getColumnData("Avail/AvailID", row);
		if (isSpecified(value)) {
			avail.appendChild(mALID(value));
		}

		// Disposition
		value = sheet.getColumnData("Disposition/EntryType", row);
		if (isSpecified(value)) {
			avail.appendChild(mDisposition(value));
		}

		// Licensor
		value = sheet.getColumnData("Avail/DisplayName", row);
		if (isSpecified(value)) {
			avail.appendChild(mPublisher("Licensor", value));
		}

		// Service Provider (OPTIONAL)
		value = sheet.getColumnData("Avail/ServiceProvider", row);
		if (isSpecified(value)) {
			avail.appendChild(mPublisher("ServiceProvider", value));
		}

		// AvailType (e.g., 'single' for a Movie)
		avail.appendChild(mGenericElement("AvailType", availType));

		// ShortDescription
		// XXX Doc says optional, schema says mandatory
		if (isSpecified(shortDesc)) {
			e = mGenericElement("ShortDescription", shortDesc);
			avail.appendChild(e);
		}
		// Asset
		value = sheet.getColumnData("AvailAsset/WorkType", row);
		if (isSpecified(value)) {
			e = mAssetHeader(value);
			avail.appendChild(e);
		}

		// Transaction
		if ((e = mTransactionHeader()) != null)
			avail.appendChild(e);

		// Exception Flag
		value = sheet.getColumnData("Avail/ExceptionFlag", row);
		if (isSpecified(value)) {
			e = mExceptionFlag(value);
			avail.appendChild(e);
		}

		return avail;
	}

	/**
	 * Create an <tt>mdmec:Publisher-type</tt> XML element with a md:DisplayName
	 * element child, and populate the latter with the DisplayName
	 * 
	 * @param name
	 *            the parent element to be created (i.e., Licensor or
	 *            ServiceProvider)
	 * @param displayName
	 *            the name to be held in the DisplayName child node
	 * @return the created element
	 */
	protected Element mPublisher(String name, String displayName) {
		Element pubEl = dom.createElement(name);
		Element e = dom.createElement("md:DisplayName");
		Text tmp = dom.createTextNode(displayName);
		e.appendChild(tmp);
		pubEl.appendChild(e);
		// XXX ContactInfo mandatory but can't get this info from the
		// spreadsheet
		e = dom.createElement("mdmec:ContactInfo");
		Element e2 = dom.createElement("md:Name");
		e.appendChild(e2);
		e2 = dom.createElement("md:PrimaryEmail");
		e.appendChild(e2);
		pubEl.appendChild(e);
		return pubEl;
	}

	protected Element mAssetHeader(String workType) {
		Element asset = dom.createElement("Asset");
		Element wt = dom.createElement("WorkType");
		Text tmp = dom.createTextNode(workType);
		wt.appendChild(tmp);
		asset.appendChild(wt);
		return mAssetBody(asset);
	}

	/**
	 * Construct Element instantiating the <tt>AvailMetadata-type</tt>. This
	 * method should be extended or overridden by sub-classes specific to a
	 * given work type (i.e., Movie, Episode, Season, etc.)
	 * 
	 * @param asset
	 * @return
	 */
	protected Element mAssetBody(Element asset) {
		Element e;
		String contentID = sheet.getColumnData("AvailAsset/ContentID", row);
		if (isSpecified(contentID)) {
			Attr attr = dom.createAttribute("contentID");
			attr.setValue(contentID);
			asset.setAttributeNode(attr);
		}

		Element metadata = dom.createElement("Metadata");

		/*
		 * TitleDisplayUnlimited is OPTIONAL in SS but REQUIRED in XML;
		 * workaround by assigning it internal alias value
		 */
		String titleDU = sheet.getColumnData("AvailMetadata/TitleDisplayUnlimited", row);
		String titleAlias = sheet.getColumnData("AvailMetadata/TitleInternalAlias", row);
		if (!isSpecified(titleDU)) {
			titleDU = titleAlias;
		}
		if (isSpecified(titleDU)) {
			e = mGenericElement("TitleDisplayUnlimited", titleDU);
			metadata.appendChild(e);
		}
		// TitleInternalAlias
		if (isSpecified(titleAlias)) {
			metadata.appendChild(mGenericElement("TitleInternalAlias", titleAlias));
		}

		// ProductID --> EditEIDR-URN ( optional field)
		String value = sheet.getColumnData("AvailAsset/ProductID", row);
		if (isSpecified(value)) {
			metadata.appendChild(mGenericElement("EditEIDR-URN", value));
		}

		// ContentID --> TitleEIDR-URN ( optional field)
		process(metadata, "TitleEIDR-URN", "AvailMetadata/ContentID", row);

		// AltID --> AltIdentifier
		value = sheet.getColumnData("AvailMetadata/AltID", row);
		if (isSpecified(value)) {
			Element altIdEl = dom.createElement("AltIdentifier");
			Element cid = dom.createElement("md:Namespace");
			Text tmp = dom.createTextNode(MISSING);
			cid.appendChild(tmp);
			altIdEl.appendChild(cid);
			altIdEl.appendChild(mGenericElement("md:Identifier", value));
			Element loc = dom.createElement("md:Location");
			tmp = dom.createTextNode(MISSING);
			loc.appendChild(tmp);
			altIdEl.appendChild(loc);
			metadata.appendChild(altIdEl);
		}

		process(metadata, "ReleaseDate", "AvailMetadata/ReleaseYear", row);
		process(metadata, "RunLength", "AvailMetadata/TotalRunTime", row);

		mReleaseHistory(metadata, "original", "AvailMetadata/ReleaseHistoryOriginal", row);
		mReleaseHistory(metadata, "DVD", "AvailMetadata/ReleaseHistoryPhysicalHV", row);

		process(metadata, "USACaptionsExemptionReason", "AvailMetadata/CaptionExemption", row);

		mRatings(metadata);

		process(metadata, "EncodeID", "AvailAsset/EncodeID", row);
		process(metadata, "LocalizationOffering", "AvailMetadata/LocalizationType", row);

		// Attach generated Metadata node
		asset.appendChild(metadata);
		return asset;
	}

	protected Element mTransactionHeader() throws Exception {
		Element transaction = dom.createElement("Transaction");
		return mTransactionBody(transaction);
	}

	/**
	 * populate a Transaction element; called from superclass
	 * 
	 * @param transaction
	 *            parent node
	 * @return transaction parent node
	 */
	protected Element mTransactionBody(Element transaction) throws Exception {
		Element e;
		String prefix = "AvailTrans/";
		process(transaction, "LicenseType", prefix + "LicenseType", row);
		process(transaction, "Description", prefix + "Description", row);
		process(transaction, "Territory", prefix + "Territory", row);

		// Start or StartCondition
		processCondition(transaction, "Start", prefix + "Start", row);
		// End or EndCondition
		processCondition(transaction, "End", prefix + "End", row);

		process(transaction, "StoreLanguage", prefix + "StoreLanguage", row);
		process(transaction, "LicenseRightsDescription", prefix + "LicenseRightsDescription", row);
		process(transaction, "FormatProfile", prefix + "FormatProfile", row);
		process(transaction, "ContractID", prefix + "ContractID", row);

		processTerm(transaction);

		// OtherInstructions
		// if ((e = mGenericElement(COL.OtherInstructions.toString(),
		// fields[COL.OtherInstructions.ordinal()],
		// false)) != null)
		// transaction.appendChild(e);

		return transaction;
	}

	/**
	 * Add 1 or more Term elements to a Transaction.
	 * <p>
	 * As of Excel v 1.6. Terms are a mess. There are two modes for defining:
	 * <ol>
	 * <li>Use 'PriceType' cell to define a <tt>termName</tt> and then get value
	 * from 'PriceValue' cell, or</li>
	 * <li>the use of columns implicitly linked to specific <tt>termName</tt>
	 * </li>
	 * </ol>
	 * An example of the 2nd approach is the 'WatchDuration' column.
	 * </p>
	 * 
	 * @param transaction
	 */
	private void processTerm(Element transaction) {
		Element termEl = dom.createElement("Term");
		transaction.appendChild(termEl);
		String prefix = "AvailTrans/";
		String tName = sheet.getColumnData(prefix + "PriceType", row);
		if (isSpecified(tName)) {
			termEl.setAttribute("termName", tName);
			switch (tName) {
			case "Tier":
			case "Category":
				process(termEl, "Text", prefix + "PriceValue", row);
				break;
			case "SRP":
			case "WSP":
				Element moneyEl = process(termEl, "Money", prefix + "PriceValue", row);
				String currency = sheet.getColumnData(prefix + "PriceCurrency", row);
				if (moneyEl != null && isSpecified(currency)) {
					moneyEl.setAttribute("currency", currency);
				}
				break;
			}
		}

		// SRP Term
		// String val = fields[COL.SRP.ordinal()];
		// if (!val.equals(""))
		// transaction.appendChild(makeMoneyTerm("SRP",
		// fields[COL.SRP.ordinal()], null));

		String value = sheet.getColumnData(prefix + "SuppressionLiftDate", row);
		if (isSpecified(value)) {
			termEl = dom.createElement("AnnounceDate");
			transaction.appendChild(termEl);

		}

		// // Any Term
		// val = fields[COL.Any.ordinal()];
		// if (!val.equals("")) {
		// transaction.appendChild(makeTextTerm(COL.Any.toString(), val));
		// }
		//
		// // RentalDuration Term
		// val = fields[COL.RentalDuration.ordinal()];
		// if (!val.equals("")) {
		// if ((e = makeDurationTerm(COL.RentalDuration.toString(), val)) !=
		// null)
		// transaction.appendChild(e);
		// }
		//
		// // WatchDuration Term
		// val = fields[COL.WatchDuration.ordinal()];
		// if (!val.equals("")) {
		// if ((e = makeDurationTerm(COL.WatchDuration.toString(), val)) !=
		// null)
		// if (e != null)
		// transaction.appendChild(e);
		// }
		//
		// // FixedEndDate Term
		// val = fields[COL.FixedEndDate.ordinal()];
		// if (!val.equals("")) {
		// if ((e = makeEventTerm(COL.FixedEndDate.toString(),
		// normalizeDate(val))) != null)
		// if (e != null)
		// transaction.appendChild(e);
		// }
		//
		// // HoldbackLanguage Term
		// val = fields[COL.HoldbackLanguage.ordinal()].trim();
		// if (!val.equals("")) {
		// transaction.appendChild(makeLanguageTerm(COL.HoldbackLanguage.toString(),
		// val));
		// }
		//
		// // HoldbackExclusionLanguage Term
		// val = fields[COL.HoldbackExclusionLanguage.ordinal()].trim();
		// if (!val.equals("")) {
		// transaction.appendChild(makeLanguageTerm(COL.HoldbackExclusionLanguage.toString(),
		// val));
		// }

	}

	/**
	 * Create an Avails ExceptionFlag element
	 */
	protected Element mExceptionFlag(String exceptionFlag) {
		Element eFlag = dom.createElement("ExceptionFlag");
		Text tmp = dom.createTextNode(exceptionFlag);
		eFlag.appendChild(tmp);
		return eFlag;
	}

	/**
	 * @param parentEl
	 * @param type
	 * @param cellKey
	 * @param row
	 */
	private void mReleaseHistory(Element parentEl, String type, String cellKey, int row) {
		String value = sheet.getColumnData(cellKey, row);
		if (!isSpecified(value)) {
			return;
		}
		Element rh = dom.createElement("ReleaseHistory");
		Element rt = dom.createElement("md:ReleaseType");
		Text tmp = dom.createTextNode(type);
		rt.appendChild(tmp);
		rh.appendChild(rt);
		rh.appendChild(mGenericElement("md:Date", value));
		parentEl.appendChild(rh);
	}

	protected void mRatings(Element m) {

		String ratingSystem = sheet.getColumnData("AvailMetadata/RatingSystem", row);
		String ratingValue = sheet.getColumnData("AvailMetadata/RatingValue", row);
		String ratingReason = sheet.getColumnData("AvailMetadata/RatingReason", row);
		/*
		 * According to XML schema, all 3 values are REQUIRED for a Rating. If
		 * any has been specified than we add the Rating element and let XML
		 * validation worry about completeness,
		 */
		boolean add = isSpecified(ratingSystem) || isSpecified(ratingValue) || isSpecified(ratingReason);
		if (!add) {
			return;
		}
		Element ratings = dom.createElement("Ratings");
		Element rat = dom.createElement("md:Rating");
		ratings.appendChild(rat);
		Element region = dom.createElement("md:Region");
		String territory = sheet.getColumnData("AvailTrans/Territory", row);
		if (isSpecified(territory)) {
			Element country = dom.createElement("md:country");
			region.appendChild(country);
			Text tmp = dom.createTextNode(territory);
			country.appendChild(tmp);
		}
		rat.appendChild(region);

		if (isSpecified(ratingSystem)) {
			rat.appendChild(mGenericElement("md:System", ratingSystem));
		}

		if (isSpecified(ratingValue)) {
			rat.appendChild(mGenericElement("md:Value", ratingValue));
		}
		if (isSpecified(ratingReason)) {
			String[] reasons = ratingReason.split(",");
			for (String s : reasons) {
				Element reason = mGenericElement("md:Reason", s);
				rat.appendChild(reason);
			}
		}
		m.appendChild(ratings);
	}

	/**
	 * Create an XML element
	 * 
	 * @param name
	 *            the name of the element
	 * @param val
	 *            the value of the element
	 * @return the created element, or null
	 */
	protected Element mGenericElement(String name, String val) {
		// if (val.equals("") && !mandatory) {
		// return null;
		// }
		Element tia = dom.createElement(name);
		Text tmp = dom.createTextNode(val);
		tia.appendChild(tmp);
		return tia;
	}

	protected Element process(Element parentEl, String childName, String cellKey, int row) {
		String value = sheet.getColumnData(cellKey, row);
		if (isSpecified(value)) {
			Element childEl = mGenericElement(childName, value);
			parentEl.appendChild(childEl);
			return childEl;
		} else {
			return null;
		}
	}

	protected boolean processCondition(Element parentEl, String childName, String cellKey, int row) {
		String value = sheet.getColumnData(cellKey, row);
		if (isSpecified(value)) {
			// does it start with 'yyyy' ?
			if (value.matches("^\\d[.]*")) {
				parentEl.appendChild(mGenericElement(childName, value));
			} else {
				parentEl.appendChild(mGenericElement(childName + "Condition", value));
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns <tt>true</tt> if the value is both non-null and not empty.
	 * 
	 * @param value
	 * @return
	 */
	protected boolean isSpecified(String value) {
		return (value != null && (!value.isEmpty()));
	}
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	/**
	 * Creates an Avails/ALID XML node
	 * 
	 * @param availID
	 *            the value of the ALID
	 * @return the generated XML element
	 */
	protected Element mALID(String availID) {
		Text tmp = dom.createTextNode(availID);
		Element ALID = dom.createElement("ALID");
		ALID.appendChild(tmp);
		return ALID;
	}

	protected Element mDisposition(String entryType) {
		Element disp = dom.createElement("Disposition");
		Element entry = dom.createElement("EntryType");
		Text tmp = dom.createTextNode(entryType);
		entry.appendChild(tmp);
		disp.appendChild(entry);
		return disp;
	}
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	// ------------------------ Transaction-related methods
	//
	// protected Element mLicenseType(String val) throws Exception {
	// if (!val.matches("^\\s*EST|VOD|SVOD|POEST\\s*$"))
	// reportError("invalid LicenseType: " + val);
	// Element e = dom.createElement("LicenseType");
	// Text tmp = dom.createTextNode(val);
	// e.appendChild(tmp);
	// return e;
	// }
	//
	// // XXX cleanupData code not added
	// protected Element mLicenseRightsDescription(String val) throws Exception
	// {
	// if (Arrays.binarySearch(LRD, val) == -1)
	// reportError("invalid LicenseRightsDescription " + val);
	// Element e = dom.createElement("LicenseRightsDescription");
	// Text tmp = dom.createTextNode(val);
	// e.appendChild(tmp);
	// return e;
	// }
	//
	// // XXX cleanupData code not added
	// protected Element mFormatProfile(String val) throws Exception {
	// if (!val.matches("^\\s*SD|HD|3D\\s*$"))
	// reportError("invalid FormatProfile: " + val);
	// Element e = dom.createElement("FormatProfile");
	// Text tmp = dom.createTextNode(val);
	// e.appendChild(tmp);
	// return e;
	// }

	// XXX cleanupData code not added
	// protected Element mPriceType(String priceType, String priceVal) throws
	// Exception {
	// Element e = null;
	// priceType = priceType.toLowerCase();
	// Pattern pat = Pattern.compile("^\\s*(tier|category|wsp|srp)\\s*$",
	// Pattern.CASE_INSENSITIVE);
	// Matcher m = pat.matcher(priceType);
	// if (!m.matches())
	// reportError("Invalid PriceType: " + priceType);
	// switch (priceType) {
	// case "tier":
	// case "category":
	// e = makeTextTerm(priceType, priceVal);
	// break;
	// case "wsp":
	// case "srp":
	// // XXX set currency properly
	// e = makeMoneyTerm(priceType, priceVal, "USD");
	// }
	// return e;
	// }
	//
	// protected Element makeTextTerm(String name, String value) {
	// Element e = dom.createElement("Term");
	// Attr attr = dom.createAttribute("termName");
	// attr.setValue(name);
	// e.setAttributeNode(attr);
	//
	// Element e2 = dom.createElement("Text");
	// Text tmp = dom.createTextNode(value);
	// e2.appendChild(tmp);
	// e.appendChild(e2);
	// return e;
	// }
	//
	// protected Element makeDurationTerm(String name, String value) throws
	// Exception {
	// int hours;
	// try {
	// hours = normalizeInt(value);
	// } catch (NumberFormatException e) {
	// reportError(" invalid duration: " + value);
	// return null;
	// }
	// value = String.format("PT%dH", hours);
	//
	// Element e = dom.createElement("Term");
	// Attr attr = dom.createAttribute("termName");
	// attr.setValue(name);
	// e.setAttributeNode(attr);
	//
	// // XXX validate
	// Element e2 = dom.createElement("Duration");
	// Text tmp = dom.createTextNode(value);
	// e2.appendChild(tmp);
	// e.appendChild(e2);
	// return e;
	// }
	//
	// protected Element makeLanguageTerm(String name, String value) throws
	// Exception {
	// // XXX validate
	// if (!isValidLanguageTag(value))
	// reportError("Suspicious Language Tag: " + value);
	// Element e = dom.createElement("Term");
	// Attr attr = dom.createAttribute("termName");
	// attr.setValue(name);
	// e.setAttributeNode(attr);
	//
	// // XXX validate
	// Element e2 = dom.createElement("Language");
	// Text tmp = dom.createTextNode(value);
	// e2.appendChild(tmp);
	// e.appendChild(e2);
	// return e;
	// }
	//
	// protected Element makeEventTerm(String name, String dateTime) {
	// Element e = dom.createElement("Term");
	// Attr attr = dom.createAttribute("termName");
	// attr.setValue(name);
	// e.setAttributeNode(attr);
	//
	// Element e2 = dom.createElement("Event");
	// // XXX validate dateTime
	// Text tmp = dom.createTextNode(dateTime);
	// e2.appendChild(tmp);
	// e.appendChild(e2);
	// return e;
	// }
	//
	// protected Element makeMoneyTerm(String name, String value, String
	// currency) {
	// Element e = dom.createElement("Term");
	// Attr attr = dom.createAttribute("termName");
	// attr.setValue(name);
	// e.setAttributeNode(attr);
	//
	// Element e2 = dom.createElement("Money");
	// if (currency != null) {
	// attr = dom.createAttribute("currency");
	// attr.setValue(currency);
	// e2.setAttributeNode(attr);
	// }
	// Text tmp = dom.createTextNode(value);
	// e2.appendChild(tmp);
	// e.appendChild(e2);
	// return e;
	// }

	// ------------------

	// protected Element mRunLength(String val) throws Exception {
	// Element ret = dom.createElement("RunLength");
	// Text tmp = dom.createTextNode(val);
	// ret.appendChild(tmp);
	// System.out.println("RunLength='" + val + "'");
	// return ret;
	// }
	//
	// protected Element mLocalizationType(Element parent, String loc) throws
	// Exception {
	// if (loc.equals(""))
	// return null;
	// if (!(loc.equals("sub") || loc.equals("dub") || loc.equals("subdub") ||
	// loc.equals("any"))) {
	// if (cleanupData) {
	// Pattern pat = Pattern.compile("^\\s*(sub|dub|subdub|any)\\s*$",
	// Pattern.CASE_INSENSITIVE);
	// Matcher m = pat.matcher(loc);
	// if (m.matches()) {
	// Comment comment = dom.createComment("corrected from '" + loc + "'");
	// loc = m.group(1).toLowerCase();
	// parent.appendChild(comment);
	// }
	// } else {
	// reportError("invalid LocalizationOffering value: " + loc);
	// }
	// }
	// return mGenericElement("LocalizationOffering", loc, false);
	// }

	/*
	 * ************************************** Helper methods
	 ****************************************/

	/**
	 * Validate argument is an ISO 639-2 (3-character) language code
	 * 
	 * @param val
	 *            the character string to test
	 * @return true iff it is a valid code
	 */
	protected boolean isValidISO639_2(String val) {
		try {
			/* ISO639_2_Code code = */ ISO639_2_Code.fromValue(val);
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	/**
	 * Perform partial validation to determine if a string is a valid RFC 5646
	 * language tag
	 * 
	 * @param val
	 *            the string to be tested
	 * @return true iff val is a valid language tag
	 * @throws Exception
	 */
	// protected boolean isValidLanguageTag(String val) throws Exception {
	// Pattern pat = Pattern.compile("^([a-zA-Z]{2,3})(?:-[a-zA-Z0-9]+)*$");
	// Matcher m = pat.matcher(val);
	// boolean ret = false;
	// if (m.matches()) {
	// String lang = m.group(1).toLowerCase();
	// ret = ((Arrays.binarySearch(ISO639, lang) >= 0) ||
	// isValidISO639_2(lang));
	// }
	// if (!ret)
	// reportError("Suspicious Language Tag: " + val);
	// return ret;
	// }

	/**
	 * logs an error and potentially throws an exception. The error is decorated
	 * with the current Sheet name and the row being processed
	 * 
	 * @param s
	 *            the error message
	 * @throws ParseException
	 *             if exit-on-error policy is in effect
	 */
	// protected void reportError(String s) throws Exception {
	// s = String.format("Row %5d: %s", rowNum, s);
	// log.error(s);
	// if (exitOnError)
	// throw new ParseException(s, 0);
	// }

	/**
	 * Parse an input string to determine whether "Yes" or "No" is intended. A
	 * variety of case mismatches and leading/trailing whitespace are accepted.
	 * 
	 * @param s
	 *            the string to be tested
	 * @return 1 if "yes" was intended; 0 if "no" was intended; -1 for an
	 *         invalid pattern.
	 */
	protected int yesorno(String s) {
		if (s.equals(""))
			return 0;
		Pattern pat = Pattern.compile("^\\s*y(?:es)?\\s*$", Pattern.CASE_INSENSITIVE); // Y,
																						// Yes,
																						// yes,
																						// yEs,
																						// etc.
		Matcher m = pat.matcher(s);
		if (m.matches()) {
			return 1;
		} else {
			pat = Pattern.compile("^\\s*n(?:o)?\\s*$", Pattern.CASE_INSENSITIVE); // N,
																					// No,
																					// no,
																					// nO,
																					// etc.
			m = pat.matcher(s);
			if (m.matches())
				return 0;
			else
				return -1;
		}
	}

	/**
	 * Verify that a string represents a valid 4-digit year, and return a
	 * canonical representation if so. Leading and trailing whitespace is
	 * tolerated.
	 * 
	 * @param s
	 *            the input string to be tested
	 * @return a 4-digit year value represented as a string
	 */
	protected String normalizeYear(String s) {
		Pattern eidr = Pattern.compile("^\\s*(\\d{4})(?:\\.0)?\\s*$");
		Matcher m = eidr.matcher(s);
		if (m.matches()) {
			return m.group(1);
		} else {
			return null;
		}
	}

	protected int normalizeInt(String s) throws Exception {
		if (s == null)
			return 0;
		Pattern eidr = Pattern.compile("^\\s*(\\d+)(?:\\.0)?\\s*$");
		Matcher m = eidr.matcher(s);
		if (m.matches()) {
			return Integer.parseInt(m.group(1));
		} else {
			throw new NumberFormatException(s);
		}
	}

	protected String normalizeDate(String s) {
		final int[] dim = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
		int year = -1, month = -1, day = -1;
		Pattern date = Pattern.compile("^\\s*(\\d{4})-(\\d{1,2})-(\\d{1,2})\\s*$");
		Matcher m = date.matcher(s);
		if (m.matches()) { // try yyyy-mm-dd
			year = Integer.parseInt(m.group(1));
			month = Integer.parseInt(m.group(2));
			day = Integer.parseInt(m.group(3));
		} else { // try dd-mmm-yyyy
			date = Pattern.compile("^\\s*(\\d{1,2})-(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)-(\\d{4})\\s*$",
					Pattern.CASE_INSENSITIVE);
			m = date.matcher(s);
			if (m.matches()) {
				year = Integer.parseInt(m.group(3));
				switch (m.group(2).toLowerCase()) {
				case "jan":
					month = 1;
					break;
				case "feb":
					month = 2;
					break;
				case "mar":
					month = 3;
					break;
				case "apr":
					month = 4;
					break;
				case "may":
					month = 5;
					break;
				case "jun":
					month = 6;
					break;
				case "jul":
					month = 7;
					break;
				case "aug":
					month = 8;
					break;
				case "sep":
					month = 9;
					break;
				case "oct":
					month = 10;
					break;
				case "nov":
					month = 11;
					break;
				case "dec":
					month = 12;
					break;
				}
				day = Integer.parseInt(m.group(1));
			} else {
				return null;
			}
		}
		boolean badDate = year < 1850;
		badDate |= month < 1 || month > 12;
		badDate |= day < 1;
		if (month == 2) { // February
			if ((year % 4) == 0) { // leap year: fails in year 2400
				if ((year % 100) == 0) {
					badDate |= day > 28; // first-order exception to leap year
											// rule
				} else {
					badDate |= day > 29; // normal leap year
				}
			} else {
				badDate |= day > dim[1]; // non-leap year
			}
		} else {
			badDate |= day > dim[month - 1];
		}
		if (badDate)
			return null;
		else
			return String.format("%04d-%02d-%02d", year, month, day);
	}
}
