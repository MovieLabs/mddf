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
import com.movielabs.mddflib.Movie.COL;

/**
 * * Code and functionality formerly in SheetRow
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public abstract class RowToXmlHelper {

	protected int row;
	protected String shortDesc = ""; // default
	protected Document dom;
	protected String availType;
	private AvailsSheet sheet;

	/**
	 * @param fields
	 */
	RowToXmlHelper(AvailsSheet sheet, int row) {
		super();
		this.sheet = sheet;
		this.row = row;

	}

	protected Element makeAvail(Document dom) throws Exception {
		this.dom = dom;
		Element avail = dom.createElement("Avail");
		Element e;

		// ALID
		String value = sheet.getColumnData("Avail/AvailID", row);
		avail.appendChild(mALID(value));

		// Disposition
		value = sheet.getColumnData("Disposition/EntryType", row);
		avail.appendChild(mDisposition(value));

		// Licensor
		value = sheet.getColumnData("Avail/DisplayName", row);
		avail.appendChild(mPublisher("Licensor", value, true));

		// Service Provider (OPTIONAL)
		value = sheet.getColumnData("Avail/ServiceProvider", row);
		e = mPublisher("ServiceProvider", value, false);
		if (e != null) {
			avail.appendChild(e);
		}

		// AvailType (e.g., 'single' for a Movie)
		avail.appendChild(mGenericElement("AvailType", availType, true));

		// ShortDescription
		// XXX Doc says optional, schema says mandatory
		if ((e = mGenericElement("ShortDescription", shortDesc, true)) != null)
			avail.appendChild(e);

		// Asset
		value = sheet.getColumnData("AvailAsset/WorkType", row);
		if ((e = mAssetHeader(value)) != null)
			avail.appendChild(e);

		// Transaction
		if ((e = mTransactionHeader()) != null)
			avail.appendChild(e);

		// Exception Flag
		value = sheet.getColumnData("Avail/ExceptionFlag", row);
		e = mExceptionFlag(value);
		if (e != null)
			avail.appendChild(e);

		return avail;
	}

	/**
	 * Create an Avails Licensor XML element with a md:DisplayName element
	 * child, and populate thei latter with the DisplayName
	 * 
	 * @param name
	 *            the Licensor element to be created
	 * @param displayName
	 *            the name to be held in the DisplayName child node of Licensor
	 * @param mandatory
	 *            if true, an exception will thrown if the displayName is empty
	 * @return the created Licensor element
	 * @throws ParseException
	 *             if there is an error and abort-on-error policy is in effect
	 * @throws Exception
	 *             other error conditions may also throw exceptions
	 */
	protected Element mPublisher(String name, String displayName, boolean mandatory) {
		if (displayName.equals("") && !mandatory) {
			return null;
		}
		Element licensor = dom.createElement(name);
		Element e = dom.createElement("md:DisplayName");
		Text tmp = dom.createTextNode(displayName);
		e.appendChild(tmp);
		licensor.appendChild(e);
		// XXX ContactInfo mandatory but can't get this info from the
		// spreadsheet
		e = dom.createElement("mdmec:ContactInfo");
		Element e2 = dom.createElement("md:Name");
		e.appendChild(e2);
		e2 = dom.createElement("md:PrimaryEmail");
		e.appendChild(e2);
		licensor.appendChild(e);
		return licensor;
	}

	protected Element mAssetHeader(String workType) throws Exception {
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
		String contentID =   sheet.getColumnData("AvailAsset/ContentID", row);
		Attr attr = dom.createAttribute(COL.ContentID.toString());
		attr.setValue(contentID);
		asset.setAttributeNode(attr);
		Element metadata = dom.createElement("Metadata");
		String territory = fields[COL.Territory.ordinal()];

		// TitleDisplayUnlimited
		// XXX Optional in SS, Required in XML; workaround by assigning it
		// internal alias value
		if (fields[COL.TitleDisplayUnlimited.ordinal()].equals(""))
			fields[COL.TitleDisplayUnlimited.ordinal()] = fields[COL.TitleInternalAlias.ordinal()];
		if ((e = mGenericElement(COL.TitleDisplayUnlimited.toString(), fields[COL.TitleDisplayUnlimited.ordinal()],
				false)) != null)
			metadata.appendChild(e);
		// TitleInternalAlias
		metadata.appendChild(
				mGenericElement(COL.TitleInternalAlias.toString(), fields[COL.TitleInternalAlias.ordinal()], true));

		// ProductID --> EditEIDR-S
		if (!fields[COL.ProductID.ordinal()].equals("")) { // optional field
			String productID = normalizeEIDR(fields[COL.ProductID.ordinal()]);
			if (productID == null) {
				reportError("Invalid ProductID: " + fields[COL.ProductID.ordinal()]);
			} else {
				fields[COL.ProductID.ordinal()] = productID;
			}
			// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
			/*
			 * SPECIAL CASE KLUDGE!!! Column name is no longer same as Element
			 * name
			 */
			String elementName = COL.ProductID.toString();
			if (!elementName.endsWith("-URN")) {
				elementName = "EditEIDR-URN";
			}
			metadata.appendChild(mGenericElement(elementName, fields[COL.ProductID.ordinal()], false));
		}

		// AltID --> AltIdentifier
		if (!fields[COL.AltID.ordinal()].equals("")) { // optional
			Element altID = dom.createElement("AltIdentifier");
			Element cid = dom.createElement("md:Namespace");
			Text tmp = dom.createTextNode(MISSING);
			cid.appendChild(tmp);
			altID.appendChild(cid);
			altID.appendChild(mGenericElement(COL.AltID.toString(), fields[COL.AltID.ordinal()], false));
			Element loc = dom.createElement("md:Location");
			tmp = dom.createTextNode(MISSING);
			loc.appendChild(tmp);
			altID.appendChild(loc);
			metadata.appendChild(altID);
		}

		// ReleaseYear ---> ReleaseDate
		if (!fields[COL.ReleaseYear.ordinal()].equals("")) { // optional
			String year = normalizeYear(fields[COL.ReleaseYear.ordinal()]);
			if (year == null) {
				reportError("Invalid ReleaseYear: " + fields[COL.ReleaseYear.ordinal()]);
			} else {
				fields[COL.ReleaseYear.ordinal()] = year;
			}
			metadata.appendChild(mGenericElement(COL.ReleaseYear.toString(), fields[COL.ReleaseYear.ordinal()], false));
		}

		// TotalRunTime --> RunLength
		// XXX TotalRunTime is optional, RunLength is required
		if ((e = mRunLength(fields[COL.TotalRunTime.ordinal()])) != null) {
			metadata.appendChild(e);
		}

		// ReleaseHistoryOriginal ---> ReleaseHistory/Date
		String date = fields[COL.ReleaseHistoryOriginal.ordinal()];
		if (!date.equals("")) {
			if ((e = mReleaseHistory(COL.ReleaseHistoryOriginal.toString(), normalizeDate(date), "original")) != null)
				metadata.appendChild(e);
		}

		// ReleaseHistoryPhysicalHV ---> ReleaseHistory/Date
		date = fields[COL.ReleaseHistoryPhysicalHV.ordinal()];
		if (!date.equals("")) {
			if ((e = mReleaseHistory(COL.ReleaseHistoryPhysicalHV.toString(), normalizeDate(date), "DVD")) != null)
				metadata.appendChild(e);
		}

		// CaptionIncluded/CaptionException
		mCaption(metadata, fields[COL.CaptionIncluded.ordinal()], fields[COL.CaptionExemption.ordinal()], territory);

		// RatingSystem ---> Ratings
		mRatings(metadata, fields[COL.RatingSystem.ordinal()], fields[COL.RatingValue.ordinal()],
				fields[COL.RatingReason.ordinal()], territory);

		// EncodeID --> EditEIDR-S
		if (!fields[COL.EncodeID.ordinal()].equals("")) { // optional field
			String encodeID = normalizeEIDR(fields[COL.EncodeID.ordinal()]);
			if (encodeID == null) {
				reportError("Invalid EncodeID: " + fields[COL.EncodeID.ordinal()]);
			} else {
				fields[COL.EncodeID.ordinal()] = encodeID;
			}
			metadata.appendChild(mGenericElement(COL.EncodeID.toString(), fields[COL.EncodeID.ordinal()], false));
		}

		// LocalizationType --> LocalizationOffering
		if ((e = mLocalizationType(metadata, fields[COL.LocalizationType.ordinal()])) != null) {
			metadata.appendChild(e);
		}

		// Attach generated Metadata node
		asset.appendChild(metadata);
		return asset;
	}

	protected Element mTransactionHeader() throws Exception {
		Element transaction = dom.createElement("Transaction");
		return mTransactionBody(transaction);
	}

	protected abstract Element mTransactionBody(Element transaction) throws Exception;

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
	 * Create an XML element
	 * 
	 * @param name
	 *            the name of the element
	 * @param val
	 *            the value of the element
	 * @param mandatory
	 *            if true, indicates this is a required field,
	 * @return the created element, or null
	 */
	protected Element mGenericElement(String name, String val, boolean mandatory) throws Exception {
		if (val.equals("") && !mandatory) {
			return null;
		}
		Element tia = dom.createElement(name);
		Text tmp = dom.createTextNode(val);
		tia.appendChild(tmp);
		return tia;
	}

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	/*
	 * ************************************** Node-generating methods
	 ****************************************/

	protected Element mReleaseHistory(String name, String val, String rType) throws Exception {
		Element rh = null;
		if (!val.equals("")) { // optional
			String date = normalizeDate(val);
			if (date == null)
				reportError("Invalid " + name + ": " + val);
			rh = dom.createElement("ReleaseHistory");
			Element rt = dom.createElement("md:ReleaseType");
			Text tmp = dom.createTextNode(rType);
			rt.appendChild(tmp);
			rh.appendChild(rt);
			rh.appendChild(mGenericElement(name, val, false));
		}
		return rh;
	}

	protected void mCaption(Element m, String capIncluded, String capExemption, String territory) throws Exception {
		Element e;
		if ((e = mCaptionsExemptionReason(capIncluded, capExemption, territory)) != null) {
			if (!territory.equals("US")) {
				Comment comment = dom.createComment("Exemption reason specified for non-US territory");
				m.appendChild(comment);
			}
			m.appendChild(e);
		}
	}

	protected void mRatings(Element m, String ratingSystem, String ratingValue, String ratingReason, String territory)
			throws Exception {
		// RatingSystem ---> Ratings
		if (ratingSystem.equals("")) { // optional
			if (!(ratingValue.equals("") && ratingReason.equals("")))
				reportError("RatingSystem not specified");
			else
				return;
		}
		if (!isValidISO3166_2(territory)) // validate legit ISO 3166-1 alpha-2
			reportError("invalid Country Code: " + territory);
		Element ratings = dom.createElement("Ratings");
		Element rat = dom.createElement("md:Rating");
		ratings.appendChild(rat);
		Comment comment = dom.createComment("Ratings Region derived from Spreadsheet Territory value");
		rat.appendChild(comment);
		Element region = dom.createElement("md:Region");
		Element country = dom.createElement("md:country");
		region.appendChild(country);
		Text tmp = dom.createTextNode(territory);
		country.appendChild(tmp);
		rat.appendChild(region);
		rat.appendChild(mGenericElement("md:System", ratingSystem, true));
		rat.appendChild(mGenericElement("md:Value", ratingValue, true));
		if (!ratingReason.equals("")) {
			String[] reasons = ratingReason.split(",");
			for (String s : reasons) {
				Element reason = mGenericElement("md:Reason", s, true);
				rat.appendChild(reason);
			}
		}
		m.appendChild(ratings);
	}

	protected Element mCount(String name, String val) throws Exception {
		if (val.equals(""))
			reportError("missing required count value on element: " + name);
		Element tia = dom.createElement(name);
		Element e = dom.createElement("md:Number");
		int n = normalizeInt(val);
		Text tmp = dom.createTextNode(String.valueOf(n));
		e.appendChild(tmp);
		tia.appendChild(e);

		return tia;
	}

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

	// ------------------------ Transaction-related methods

	protected Element mLicenseType(String val) throws Exception {
		if (!val.matches("^\\s*EST|VOD|SVOD|POEST\\s*$"))
			reportError("invalid LicenseType: " + val);
		Element e = dom.createElement("LicenseType");
		Text tmp = dom.createTextNode(val);
		e.appendChild(tmp);
		return e;
	}

	protected Element mTerritory(String val) throws Exception {
		if (!isValidISO3166_2(val)) // validate legit ISO 3166-1 alpha-2
			reportError("invalid Country Code: " + val);
		Element e = dom.createElement("Territory");
		Element e2 = mGenericElement("md:country", val, true);
		e.appendChild(e2);
		return e;
	}

	protected Element mDescription(String val) throws Exception {
		// XXX this can be optional in SS, but not in XML
		if (val.equals(""))
			val = MISSING;
		return mGenericElement("Description", val, true);
	}

	// XXX XML allows empty value
	// XXX cleanupData code not added
	/**
	 * Generate Start or StartCondition
	 * 
	 * @param val
	 *            start time
	 * @return a Start element
	 * @throws Exception
	 *             if an error condition is encountered
	 */
	protected Element mStart(String val) throws Exception {
		Element e = null;
		if (val.equals("")) {
			reportError("Missing Start date: ");
			// e = dom.createElement("StartCondition");
			// tmp = dom.createTextNode("Immediate");
		} else {
			String date = normalizeDate(val);
			if (date == null)
				reportError("Invalid Start date: " + val);
			e = dom.createElement("Start"); // [sic] yes name
			Text tmp = dom.createTextNode(date + "T00:00:00");
			e.appendChild(tmp);
		}
		return e;
	}

	// XXX cleanupData code not added
	protected Element mEnd(String val) throws Exception {
		Element e = null;
		Text tmp;
		if (val.equals(""))
			reportError("End date may not be null");

		String date = normalizeDate(val);
		if (date != null) {
			e = dom.createElement("End");
			tmp = dom.createTextNode(date + "T00:00:00");
			e.appendChild(tmp);
		} else if (val.matches("^\\s*Open|ESTStart|Immediate\\s*$")) {
			e = dom.createElement("EndCondition");
			tmp = dom.createTextNode(val);
			e.appendChild(tmp);
		} else {
			reportError("Invalid End Condition " + val);
		}
		return e;
	}

	protected Element mStoreLanguage(String val) throws Exception {
		if (!isValidLanguageTag(val)) // RFC 5646/BCP 47 validation
			reportError("invalid language tag: '" + val + "'");
		return mGenericElement("StoreLanguage", val, false);
	}

	// XXX cleanupData code not added
	protected Element mLicenseRightsDescription(String val) throws Exception {
		if (Arrays.binarySearch(LRD, val) == -1)
			reportError("invalid LicenseRightsDescription " + val);
		Element e = dom.createElement("LicenseRightsDescription");
		Text tmp = dom.createTextNode(val);
		e.appendChild(tmp);
		return e;
	}

	// XXX cleanupData code not added
	protected Element mFormatProfile(String val) throws Exception {
		if (!val.matches("^\\s*SD|HD|3D\\s*$"))
			reportError("invalid FormatProfile: " + val);
		Element e = dom.createElement("FormatProfile");
		Text tmp = dom.createTextNode(val);
		e.appendChild(tmp);
		return e;
	}

	// XXX cleanupData code not added
	protected Element mPriceType(String priceType, String priceVal) throws Exception {
		Element e = null;
		priceType = priceType.toLowerCase();
		Pattern pat = Pattern.compile("^\\s*(tier|category|wsp|srp)\\s*$", Pattern.CASE_INSENSITIVE);
		Matcher m = pat.matcher(priceType);
		if (!m.matches())
			reportError("Invalid PriceType: " + priceType);
		switch (priceType) {
		case "tier":
		case "category":
			e = makeTextTerm(priceType, priceVal);
			break;
		case "wsp":
		case "srp":
			// XXX set currency properly
			e = makeMoneyTerm(priceType, priceVal, "USD");
		}
		return e;
	}

	protected Element makeTextTerm(String name, String value) {
		Element e = dom.createElement("Term");
		Attr attr = dom.createAttribute("termName");
		attr.setValue(name);
		e.setAttributeNode(attr);

		Element e2 = dom.createElement("Text");
		Text tmp = dom.createTextNode(value);
		e2.appendChild(tmp);
		e.appendChild(e2);
		return e;
	}

	protected Element makeDurationTerm(String name, String value) throws Exception {
		int hours;
		try {
			hours = normalizeInt(value);
		} catch (NumberFormatException e) {
			reportError(" invalid duration: " + value);
			return null;
		}
		value = String.format("PT%dH", hours);

		Element e = dom.createElement("Term");
		Attr attr = dom.createAttribute("termName");
		attr.setValue(name);
		e.setAttributeNode(attr);

		// XXX validate
		Element e2 = dom.createElement("Duration");
		Text tmp = dom.createTextNode(value);
		e2.appendChild(tmp);
		e.appendChild(e2);
		return e;
	}

	protected Element makeLanguageTerm(String name, String value) throws Exception {
		// XXX validate
		if (!isValidLanguageTag(value))
			reportError("Suspicious Language Tag: " + value);
		Element e = dom.createElement("Term");
		Attr attr = dom.createAttribute("termName");
		attr.setValue(name);
		e.setAttributeNode(attr);

		// XXX validate
		Element e2 = dom.createElement("Language");
		Text tmp = dom.createTextNode(value);
		e2.appendChild(tmp);
		e.appendChild(e2);
		return e;
	}

	protected Element makeEventTerm(String name, String dateTime) {
		Element e = dom.createElement("Term");
		Attr attr = dom.createAttribute("termName");
		attr.setValue(name);
		e.setAttributeNode(attr);

		Element e2 = dom.createElement("Event");
		// XXX validate dateTime
		Text tmp = dom.createTextNode(dateTime);
		e2.appendChild(tmp);
		e.appendChild(e2);
		return e;
	}

	protected Element makeMoneyTerm(String name, String value, String currency) {
		Element e = dom.createElement("Term");
		Attr attr = dom.createAttribute("termName");
		attr.setValue(name);
		e.setAttributeNode(attr);

		Element e2 = dom.createElement("Money");
		if (currency != null) {
			attr = dom.createAttribute("currency");
			attr.setValue(currency);
			e2.setAttributeNode(attr);
		}
		Text tmp = dom.createTextNode(value);
		e2.appendChild(tmp);
		e.appendChild(e2);
		return e;
	}

	// ------------------

	protected Element mRunLength(String val) throws Exception {
		Element ret = dom.createElement("RunLength");
		Text tmp = dom.createTextNode(val);
		ret.appendChild(tmp);
		System.out.println("RunLength='" + val + "'");
		return ret;
	}

	protected Element mLocalizationType(Element parent, String loc) throws Exception {
		if (loc.equals(""))
			return null;
		if (!(loc.equals("sub") || loc.equals("dub") || loc.equals("subdub") || loc.equals("any"))) {
			if (cleanupData) {
				Pattern pat = Pattern.compile("^\\s*(sub|dub|subdub|any)\\s*$", Pattern.CASE_INSENSITIVE);
				Matcher m = pat.matcher(loc);
				if (m.matches()) {
					Comment comment = dom.createComment("corrected from '" + loc + "'");
					loc = m.group(1).toLowerCase();
					parent.appendChild(comment);
				}
			} else {
				reportError("invalid LocalizationOffering value: " + loc);
			}
		}
		return mGenericElement("LocalizationOffering", loc, false);
	}

	protected Element mCaptionsExemptionReason(String captionIncluded, String captionExemption, String territory)
			throws Exception {
		int exemption = 0;
		switch (yesorno(captionIncluded)) {
		case 1: // yes, caption is included
			if (!captionExemption.equals(""))
				reportError("CaptionExemption specified without CaptionIncluded");
			break;
		case 0: // no, captions not included
			if (captionExemption.equals("")) {
				reportError("Captions not included and CaptionExemption not specified");
				return null;
			} else {
				try {
					exemption = normalizeInt(captionExemption);
				} catch (NumberFormatException s) {
					exemption = -1;
				}
				if (exemption < 1 || exemption > 6)
					reportError("Invalid CaptionExamption Value: " + exemption);
				Element capex = dom.createElement("USACaptionsExemptionReason");
				Text tmp = dom.createTextNode(Integer.toString(exemption));
				capex.appendChild(tmp);
				return capex;
			}
			// break
		default:
			reportError("CaptionExemption specified without CaptionIncluded");
		}
		// } else {
		// if (captionIncluded.equals("") && captionExemption.equals(""))
		// return null;
		// else
		// reportError("CaptionIncluded/Exemption should only be specified in
		// US");
		// }
		return null;
	}

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
	protected boolean isValidLanguageTag(String val) throws Exception {
		Pattern pat = Pattern.compile("^([a-zA-Z]{2,3})(?:-[a-zA-Z0-9]+)*$");
		Matcher m = pat.matcher(val);
		boolean ret = false;
		if (m.matches()) {
			String lang = m.group(1).toLowerCase();
			ret = ((Arrays.binarySearch(ISO639, lang) >= 0) || isValidISO639_2(lang));
		}
		if (!ret)
			reportError("Suspicious Language Tag: " + val);
		return ret;
	}

	/**
	 * Determine if a string is a valid ISO 3066-2 country code
	 * 
	 * @param val
	 *            the string to be tested
	 * @return true iff val is a valid code
	 */
	protected boolean isValidISO3166_2(String val) {
		return Arrays.binarySearch(ISO3166, val) != -1;
	}

	/**
	 * logs an error and potentially throws an exception. The error is decorated
	 * with the current Sheet name and the row being processed
	 * 
	 * @param s
	 *            the error message
	 * @throws ParseException
	 *             if exit-on-error policy is in effect
	 */
	protected void reportError(String s) throws Exception {
		s = String.format("Row %5d: %s", rowNum, s);
		log.error(s);
		if (exitOnError)
			throw new ParseException(s, 0);
	}

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
	 * Verify that a string represents a valid EIDR, and return compact
	 * representation if so. Leading and trailing whitespace is tolerated, as is
	 * the presence/absence of the EIDR "10.5240/" prefix
	 * 
	 * @param s
	 *            the input string to be tested
	 * @return a short EIDR corresponding to the asset if valid; null if not a
	 *         proper EIDR exception
	 */
	protected String normalizeEIDR(String s) {
		Pattern eidr = Pattern.compile("^\\s*(?:10\\.5240/)?((?:(?:\\p{XDigit}){4}-){5}\\p{XDigit})\\s*$");
		Matcher m = eidr.matcher(s);
		if (m.matches()) {
			return m.group(1).toUpperCase();
		} else {
			return null;
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
