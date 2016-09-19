/*
 * Copyright (c) 2015 MovieLabs
 * 
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
 *
 * Author: Paul Jensen <pgj@movielabs.com>
 */

package com.movielabs.mddflib;

import org.w3c.dom.*;

/**
 * A subclass representing an Avails movie
 */
public class Movie extends SheetRow {

	/**
	 * An enum used to represent offsets into an array of spreadsheet cells. The
	 * name() method returns the Column name (based on Avails spreadsheet
	 * representation; the toString() method returns a corresponding or related
	 * XML element; and ordinal() returns the offset
	 */
	private enum COL {
		DisplayName("DisplayName"), // 0
		StoreLanguage("StoreLanguage"), // 1
		Territory("md:country"), // 2
		WorkType("WorkType"), // 3
		EntryType("EntryType"), // 4
		TitleInternalAlias("TitleInternalAlias"), // 5
		TitleDisplayUnlimited("TitleDisplayUnlimited"), // 6
		LocalizationType("LocalizationOffering"), // 7
		LicenseType("LicenseType"), // 8
		LicenseRightsDescription("LicenseRightsDescription"), // 9
		FormatProfile("FormatProfile"), // 10
		Start("StartCondition"), // 11
		End("EndCondition"), // 12
		PriceType("PriceType"), // 13
		PriceValue("PriceValue"), // 14
		SRP("SRP"), // 15
		Description("Description"), // 16
		OtherTerms("OtherTerms"), // 17
		OtherInstructions("OtherInstructions"), // 18
		ContentID("contentID"), // 19
		ProductID("EditEIDR-S"), // 20
		EncodeID("EncodeID"), // 21
		AvailID("AvailID"), // 22
		Metadata("Metadata"), // 23
		AltID("md:Identifier"), // 24
		SuppressionLiftDate("AnnounceDate"), // 25
		SpecialPreOrderFulfillDate("SpecialPreOrderFulfillDate"), // 26
		ReleaseYear("ReleaseDate"), // 27
		ReleaseHistoryOriginal("md:Date"), // 28
		ReleaseHistoryPhysicalHV("md:Date"), // 29
		ExceptionFlag("ExceptionFlag"), // 30
		RatingSystem("md:System"), // 31
		RatingValue("md:Value"), // 32
		RatingReason("md:Reason"), // 33
		RentalDuration("RentalDuration"), // 34
		WatchDuration("WatchDuration"), // 35
		CaptionIncluded("USACaptionsExemptionReason"), // 36
		CaptionExemption("USACaptionsExemptionReason"), // 37
		Any("Any"), // 38
		ContractID("ContractID"), // 39
		ServiceProvider("ServiceProvider"), // 40
		TotalRunTime("RunLength"), // 41
		HoldbackLanguage("HoldbackLanguage"), // 42
		HoldbackExclusionLanguage("HoldbackExclusionLanguage"); // 43

		private final String name;

		private COL(String s) {
			name = s;
		}

		public boolean equalsName(String otherName) {
			return (otherName == null) ? false : name.equals(otherName);
		}

		public String toString() {
			return this.name;
		}
	} /* COL */

	/**
	 * Create a Metadata element and append it to the parent; called from
	 * superclass
	 * 
	 * @param asset
	 *            parent node
	 * @return created EpisodeMetadata element
	 */
	protected Element mAssetBody(Element asset) throws Exception {
		Element e;
		String contentID = fields[COL.ContentID.ordinal()];
		if (contentID.equals(""))
			contentID = MISSING;
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
			 * SPECIAL CASE KLUDGE!!! Column name is no longer same as Element name
			 */
			String elementName = COL.ProductID.toString();
			if(!elementName.endsWith("-URN")){
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
	} /* mAssetBody() */

	/**
	 * populate a Transaction element; called from superclass
	 * 
	 * @param transaction
	 *            parent node
	 * @return transaction parent node
	 */
	protected Element mTransactionBody(Element transaction) throws Exception {
		Element e;

		// LicenseType
		transaction.appendChild(mLicenseType(fields[COL.LicenseType.ordinal()]));

		// Description
		transaction.appendChild(mDescription(fields[COL.Description.ordinal()]));

		// Territory
		transaction.appendChild(mTerritory(fields[COL.Territory.ordinal()]));

		// Start or StartCondition
		transaction.appendChild(mStart(fields[COL.Start.ordinal()]));

		// End or EndCondition
		transaction.appendChild(mEnd(fields[COL.End.ordinal()]));

		// StoreLanguage
		if ((e = mStoreLanguage(fields[COL.StoreLanguage.ordinal()])) != null)
			transaction.appendChild(e);

		// LicenseRightsDescription
		if ((e = mLicenseRightsDescription(fields[COL.LicenseRightsDescription.ordinal()])) != null)
			transaction.appendChild(e);

		// FormatProfile
		transaction.appendChild(mFormatProfile(fields[COL.FormatProfile.ordinal()]));

		// ContractID
		if ((e = mGenericElement(COL.ContractID.toString(), fields[COL.ContractID.ordinal()], false)) != null)
			transaction.appendChild(e);

		// ------------------ Term(s)
		// PriceType term
		transaction.appendChild(mPriceType(fields[COL.PriceType.ordinal()], fields[COL.PriceValue.ordinal()]));

		// XXX currency not specified
		String val = fields[COL.SRP.ordinal()];
		if (!val.equals(""))
			transaction.appendChild(makeMoneyTerm("SRP", fields[COL.SRP.ordinal()], null));

		// SuppressionLiftDate term
		// XXX validate; required for pre-orders
		val = fields[COL.SuppressionLiftDate.ordinal()];
		if (!val.equals("")) {
			transaction.appendChild(makeEventTerm(COL.SuppressionLiftDate.toString(), normalizeDate(val)));
		}

		// Any Term
		val = fields[COL.Any.ordinal()];
		if (!val.equals("")) {
			transaction.appendChild(makeTextTerm(COL.Any.toString(), val));
		}

		// RentalDuration Term
		val = fields[COL.RentalDuration.ordinal()];
		if (!val.equals("")) {
			if ((e = makeDurationTerm(COL.RentalDuration.toString(), val)) != null)
				transaction.appendChild(e);
		}

		// WatchDuration Term
		val = fields[COL.WatchDuration.ordinal()];
		if (!val.equals("")) {
			if ((e = makeDurationTerm(COL.WatchDuration.toString(), val)) != null)
				if (e != null)
					transaction.appendChild(e);
		}

		// HoldbackLanguage Term
		val = fields[COL.HoldbackLanguage.ordinal()].trim();
		if (!val.equals("")) {
			transaction.appendChild(makeLanguageTerm(COL.HoldbackLanguage.toString(), val));
		}

		// HoldbackExclusionLanguage Term
		val = fields[COL.HoldbackExclusionLanguage.ordinal()].trim();
		if (!val.equals("")) {
			transaction.appendChild(makeLanguageTerm(COL.HoldbackExclusionLanguage.toString(), val));
		}

		// OtherInstructions
		if ((e = mGenericElement(COL.OtherInstructions.toString(), fields[COL.OtherInstructions.ordinal()],
				false)) != null)
			transaction.appendChild(e);

		return transaction;
	} /* mTransaction() */

	/**
	 * Make an XML avail from a row of spreadsheet avails data
	 * 
	 * @param dom
	 *            the JAXP object where the created avail will be located
	 * @return a JAXP Element representing this avail
	 */
	public Element makeAvail(Document dom) throws Exception {
		this.dom = dom;
		Element avail = dom.createElement("Avail");
		Element e;

		// ALID
		avail.appendChild(mALID(fields[COL.AvailID.ordinal()]));

		// Disposition
		avail.appendChild(mDisposition(fields[COL.EntryType.ordinal()]));

		// Licensor
		avail.appendChild(mPublisher("Licensor", fields[COL.DisplayName.ordinal()], true));

		// Service Provider
		if ((e = mPublisher("ServiceProvider", fields[COL.ServiceProvider.ordinal()], false)) != null)
			avail.appendChild(e);

		// AvailType ('single' for a Movie)
		avail.appendChild(mGenericElement("AvailType", "single", true));

		// ShortDescription
		// XXX Doc says optional, schema says mandatory
		if ((e = mGenericElement("ShortDescription", shortDesc, true)) != null)
			avail.appendChild(e);

		// Asset
		if ((e = mAssetHeader()) != null)
			avail.appendChild(e);

		// Transaction
		if ((e = mTransactionHeader()) != null)
			avail.appendChild(e);

		// Exception Flag
		if ((e = mExceptionFlag(fields[COL.ExceptionFlag.ordinal()])) != null)
			avail.appendChild(e);

		return avail;
	}

	/**
	 * Create an object spreadsheet row representing a Movie avail
	 * 
	 * @param parent
	 *            the parent sheet object
	 * @param workType
	 *            must be either "Movie", "Episode", or "Season"
	 * @param lineNo
	 *            the row number corresponding to this row's position in the
	 *            sheet (1-based)
	 * @param fields
	 *            an array containing each cell value of the row (as a string)
	 */
	public Movie(AvailsSheet parent, String workType, int lineNo, String[] fields) {
		super(parent, workType, lineNo, fields);
	}
}
