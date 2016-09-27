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

import org.jdom2.Element;
import org.jdom2.Namespace;

import com.movielabs.mddflib.AvailsSheet;

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
public class RowToXmlHelper {

	private static final String MISSING = "--FUBAR (missing)";
	protected int row;
	protected String shortDesc = ""; // default
	protected XmlBuilder xb;
	private AvailsSheet sheet;
	private String workType = "";

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

	private Pedigree getData(String columnID, int row) {
		String value = sheet.getColumnData(columnID, row);
		Pedigree ped = new Pedigree(row, sheet.getColumnIdx(columnID), sheet.getName(), value);
		return ped;
	}

	protected Element makeAvail(XmlBuilder xb) throws Exception {
		this.xb = xb;
		Element avail = new Element("Avail", xb.getAvailsNSpace());
		Element e;

		// ALID
		String value = sheet.getColumnData("Avail/AvailID", row);
		if (xb.isRequired("ALID", "avails") || isSpecified(value)) {
			avail.addContent(mALID(value));
		}

		// Disposition
		value = sheet.getColumnData("Disposition/EntryType", row);
		if (xb.isRequired("Disposition", "avails") || isSpecified(value)) {
			avail.addContent(mDisposition(value));
		}

		// Licensor
		value = sheet.getColumnData("Avail/DisplayName", row);
		if (xb.isRequired("Licensor", "avails") || isSpecified(value)) {
			avail.addContent(mPublisher("Licensor", value));
		}

		// Service Provider (OPTIONAL)
		value = sheet.getColumnData("Avail/ServiceProvider", row);
		if (xb.isRequired("ServiceProvider", "avails") || isSpecified(value)) {
			avail.addContent(mPublisher("ServiceProvider", value));
		}

		/*
		 * Need to save the current workType for use in Transaction/Terms
		 */
		this.workType = sheet.getColumnData("AvailAsset/WorkType", row);
		String availType;
		switch (workType) {
		case "Movie":
		case "Short":
			availType = "single";
			break;
		case "Season":
			availType = "season";
			break;
		case "Episode":
			availType = "episode";
			break;
		default:
			availType = "";
		}

		// AvailType (e.g., 'single' for a Movie)
		avail.addContent(mGenericElement("AvailType", availType, xb.getAvailsNSpace()));

		// ShortDescription
		if (xb.isRequired("ShortDescription", "avails") || isSpecified(shortDesc)) {
			e = mGenericElement("ShortDescription", shortDesc, xb.getAvailsNSpace());
			avail.addContent(e);
		}
		// Asset
		if (xb.isRequired("WorkType", "avails") || isSpecified(workType)) {
			e = mAssetHeader(workType);
			avail.addContent(e);
		}

		// Transaction
		if ((e = mTransactionHeader()) != null)
			avail.addContent(e);

		// Exception Flag
		process(avail, "ExceptionFlag", xb.getAvailsNSpace(), "Avail/ExceptionFlag", row);

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
		Element pubEl = new Element(name, xb.getAvailsNSpace());
		Element e = new Element("DisplayName", xb.getMdNSpace());
		e.setText(displayName);
		pubEl.addContent(e);
		// XXX ContactInfo mandatory but can't get this info from the
		// spreadsheet
		if (xb.isRequired("ContactInfo", "mdmec")) {
			e = new Element("ContactInfo", xb.getMdMecNSpace());
			Element e2 = new Element("Name", xb.getMdNSpace());
			e.addContent(e2);
			e2 = new Element("PrimaryEmail", xb.getMdNSpace());
			e.addContent(e2);
			pubEl.addContent(e);
		}
		return pubEl;
	}

	protected Element mAssetHeader(String workType) {
		Element asset = new Element("Asset", xb.getAvailsNSpace());
		Element wt = new Element("WorkType", xb.getAvailsNSpace());
		wt.setText(workType);
		asset.addContent(wt);
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
			asset.setAttribute("contentID", "contentID");
		}

		Element metadata = new Element("Metadata", xb.getAvailsNSpace());

		/*
		 * TitleDisplayUnlimited is OPTIONAL in SS but REQUIRED in XML;
		 * workaround by assigning it internal alias value
		 */
		String titleDU = sheet.getColumnData("AvailMetadata/TitleDisplayUnlimited", row);
		String titleAlias = sheet.getColumnData("AvailMetadata/TitleInternalAlias", row);
		if (!isSpecified(titleDU)) {
			titleDU = titleAlias;
		}
		// if (isSpecified(titleDU)) {
		e = mGenericElement("TitleDisplayUnlimited", titleDU, xb.getAvailsNSpace());
		metadata.addContent(e);
		// }

		// TitleInternalAlias
		if (xb.isRequired("TitleInternalAlias", "avails") || isSpecified(titleAlias)) {
			metadata.addContent(mGenericElement("TitleInternalAlias", titleAlias, xb.getAvailsNSpace()));
		}

		// ProductID --> EditEIDR-URN ( optional field)
		process(metadata, "EditEIDR-URN", xb.getAvailsNSpace(), "AvailAsset/ProductID", row);

		// ContentID --> TitleEIDR-URN ( optional field)
		process(metadata, "TitleEIDR-URN", xb.getAvailsNSpace(), "AvailMetadata/ContentID", row);

		// AltID --> AltIdentifier
		String value = sheet.getColumnData("AvailMetadata/AltID", row);
		if (xb.isRequired("AltIdentifier", "avails") || isSpecified(value)) {
			Element altIdEl = new Element("AltIdentifier", xb.getAvailsNSpace());
			Element cid = new Element("Namespace", xb.getMdNSpace());
			cid.setText(MISSING);
			altIdEl.addContent(cid);
			altIdEl.addContent(mGenericElement("Identifier", value, xb.getMdNSpace()));
			Element loc = new Element("Location", xb.getMdNSpace());
			loc.setText(MISSING);
			altIdEl.addContent(loc);
			metadata.addContent(altIdEl);
		}

		process(metadata, "ReleaseDate", xb.getAvailsNSpace(), "AvailMetadata/ReleaseYear", row);
		process(metadata, "RunLength", xb.getAvailsNSpace(), "AvailMetadata/TotalRunTime", row);

		mReleaseHistory(metadata, "original", "AvailMetadata/ReleaseHistoryOriginal", row);
		mReleaseHistory(metadata, "DVD", "AvailMetadata/ReleaseHistoryPhysicalHV", row);

		process(metadata, "USACaptionsExemptionReason", xb.getAvailsNSpace(), "AvailMetadata/CaptionExemption", row);

		mRatings(metadata);

		process(metadata, "EncodeID", xb.getAvailsNSpace(), "AvailAsset/EncodeID", row);
		process(metadata, "LocalizationOffering", xb.getAvailsNSpace(), "AvailMetadata/LocalizationType", row);

		// Attach generated Metadata node
		asset.addContent(metadata);
		return asset;
	}

	protected Element mTransactionHeader() throws Exception {
		Element transaction = new Element("Transaction", xb.getAvailsNSpace());
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
		process(transaction, "LicenseType", xb.getAvailsNSpace(), prefix + "LicenseType", row);
		process(transaction, "Description", xb.getAvailsNSpace(), prefix + "Description", row);
		processRegion(transaction, "Territory", xb.getAvailsNSpace(), prefix + "Territory", row);

		// Start or StartCondition
		processCondition(transaction, "Start", xb.getAvailsNSpace(), prefix + "Start", row);
		// End or EndCondition
		processCondition(transaction, "End", xb.getAvailsNSpace(), prefix + "End", row);

		process(transaction, "StoreLanguage", xb.getAvailsNSpace(), prefix + "StoreLanguage", row);
		process(transaction, "LicenseRightsDescription", xb.getAvailsNSpace(), prefix + "LicenseRightsDescription",
				row);
		process(transaction, "FormatProfile", xb.getAvailsNSpace(), prefix + "FormatProfile", row);
		process(transaction, "ContractID", xb.getAvailsNSpace(), prefix + "ContractID", row);

		processTerm(transaction);

		// OtherInstructions
		// if ((e = mGenericElement(COL.OtherInstructions.toString(),
		// fields[COL.OtherInstructions.ordinal()],
		// false)) != null)
		// transaction.addContent(e);

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
		String prefix = "AvailTrans/";
		/*
		 * May be multiple 'terms'. Start with one specified via the PriceType
		 */
		String tName = sheet.getColumnData(prefix + "PriceType", row);
		if (isSpecified(tName)) {
			Element termEl = new Element("Term", xb.getAvailsNSpace());
			transaction.addContent(termEl);
			switch (tName) {
			case "Tier":
			case "Category":
				process(termEl, "Text", xb.getAvailsNSpace(), prefix + "PriceValue", row);
				break;
			case "WSP":
				if (workType.equals("Episode")) {
					tName = "EpisodeWSP";
				} else if (workType.equals("Season")) {
					tName = "SeasonWSP";
				}
			case "DMRP":
			case "SMRP":
				Element moneyEl = process(termEl, "Money", xb.getAvailsNSpace(), prefix + "PriceValue", row);
				String currency = sheet.getColumnData(prefix + "PriceCurrency", row);
				if (moneyEl != null && isSpecified(currency)) {
					moneyEl.setAttribute("currency", currency);
				}
				break;
			case "Season Only":
			}
			termEl.setAttribute("termName", tName);
		}
		/*
		 * Now look for Terms specified via other columns....
		 */

		// SRP Term
		String value = sheet.getColumnData(prefix + "SRP", row);
		if (isSpecified(value)) {
			Element termEl = new Element("Term", xb.getAvailsNSpace());
			transaction.addContent(termEl);
			termEl.setAttribute("termName", "SRP");
			Element childEl = new Element("Money", xb.getAvailsNSpace());
			termEl.addContent(childEl);
			termEl.setAttribute("currency", value);
		}

		value = sheet.getColumnData(prefix + "SuppressionLiftDate", row);
		if (isSpecified(value)) {
			Element termEl = new Element("Term", xb.getAvailsNSpace());
			transaction.addContent(termEl);
			termEl.setAttribute("termName", "AnnounceDate");
			Element childEl = mGenericElement("Event", value, xb.getAvailsNSpace());
			termEl.addContent(childEl);
		}

		value = sheet.getColumnData(prefix + "RentalDuration", row);
		if (isSpecified(value)) {
			Element termEl = new Element("Term", xb.getAvailsNSpace());
			transaction.addContent(termEl);
			termEl.setAttribute("termName", "RentalDuration");
			Element childEl = mGenericElement("Duration", value, xb.getAvailsNSpace());
			termEl.addContent(childEl);
		}

		value = sheet.getColumnData(prefix + "WatchDuration", row);
		if (isSpecified(value)) {
			Element termEl = new Element("Term", xb.getAvailsNSpace());
			transaction.addContent(termEl);
			termEl.setAttribute("termName", "WatchDuration");
			Element childEl = mGenericElement("Duration", value, xb.getAvailsNSpace());
			termEl.addContent(childEl);
		}

		value = sheet.getColumnData(prefix + "FixedEndDate", row);
		if (isSpecified(value)) {
			Element termEl = new Element("Term", xb.getAvailsNSpace());
			transaction.addContent(termEl);
			termEl.setAttribute("termName", "FixedEndDate");
			Element childEl = mGenericElement("Event", value, xb.getAvailsNSpace());
			termEl.addContent(childEl);
		}

		value = sheet.getColumnData(prefix + "HoldbackLanguage", row);
		if (isSpecified(value)) {
			Element termEl = new Element("Term", xb.getAvailsNSpace());
			transaction.addContent(termEl);
			termEl.setAttribute("termName", "HoldbackLanguage");
			Element childEl = mGenericElement("Language", value, xb.getAvailsNSpace());
			termEl.addContent(childEl);
		}

		value = sheet.getColumnData(prefix + "AllowedLanguages", row);
		if (isSpecified(value)) {
			Element termEl = new Element("Term", xb.getAvailsNSpace());
			transaction.addContent(termEl);
			termEl.setAttribute("termName", "HoldbackExclusionLanguage");
			Element childEl = mGenericElement("Language", value, xb.getAvailsNSpace());
			termEl.addContent(childEl);
		}
	}

	/**
	 * Create an Avails ExceptionFlag element
	 */
	protected Element mExceptionFlag(String exceptionFlag) {
		Element eFlag = new Element("ExceptionFlag", xb.getAvailsNSpace());
		eFlag.setText(exceptionFlag);
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
		Element rh = new Element("ReleaseHistory", xb.getAvailsNSpace());
		Element rt = new Element("ReleaseType", xb.getMdNSpace());
		rt.setText(type);
		rh.addContent(rt);
		rh.addContent(mGenericElement("Date", value, xb.getMdNSpace()));
		parentEl.addContent(rh);
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
		Element ratings = new Element("Ratings", xb.getAvailsNSpace());
		Element rat = new Element("Rating", xb.getMdNSpace());
		ratings.addContent(rat);
		Element region = new Element("Region", xb.getMdNSpace());
		String territory = sheet.getColumnData("AvailTrans/Territory", row);
		if (isSpecified(territory)) {
			Element country = new Element("country", xb.getMdNSpace());
			region.addContent(country);
			country.setText(territory);
		}
		rat.addContent(region);

		if (isSpecified(ratingSystem)) {
			rat.addContent(mGenericElement("System", ratingSystem, xb.getMdNSpace()));
		}

		if (isSpecified(ratingValue)) {
			rat.addContent(mGenericElement("Value", ratingValue, xb.getMdNSpace()));
		}
		if (isSpecified(ratingReason)) {
			String[] reasons = ratingReason.split(",");
			for (String s : reasons) {
				Element reason = mGenericElement("Reason", s, xb.getMdNSpace());
				rat.addContent(reason);
			}
		}
		m.addContent(ratings);
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
	protected Element mGenericElement(String name, String val, Namespace ns) {
		Element el = new Element(name, ns);
		String formatted = xb.formatForType(name, ns, val);
		el.setText(formatted);
		return el;
	}

	protected Element process(Element parentEl, String childName, Namespace ns, String cellKey, int row) {
		String value = sheet.getColumnData(cellKey, row);
		if (xb.isRequired(childName, ns.getPrefix()) || isSpecified(value)) {
			Element childEl = mGenericElement(childName, value, ns);
			parentEl.addContent(childEl);
			return childEl;
		} else {
			return null;
		}
	}

	private void processRegion(Element parentEl, String regionType, Namespace ns, String cellKey, int row) {
		String value = sheet.getColumnData(cellKey, row);
		if (xb.isRequired(regionType, ns.getPrefix()) || isSpecified(value)) {
			Element regionEl = new Element(regionType, ns);
			parentEl.addContent(regionEl);
			Element childEl = mGenericElement("country", value, xb.getMdNSpace());
			regionEl.addContent(childEl);
		}

	}

	protected boolean processCondition(Element parentEl, String childName, Namespace ns, String cellKey, int row) {
		String value = sheet.getColumnData(cellKey, row);
		if (isSpecified(value)) {
			// does it start with 'yyyy' ?
			if (value.matches("^\\d[.]*")) {
				parentEl.addContent(mGenericElement(childName, value, ns));
			} else {
				parentEl.addContent(mGenericElement(childName + "Condition", value, ns));
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
		Element ALID = new Element("ALID", xb.getAvailsNSpace());
		ALID.setText(availID);
		return ALID;
	}

	protected Element mDisposition(String entryType) {
		Element disp = new Element("Disposition", xb.getAvailsNSpace());
		Element entry = new Element("EntryType", xb.getAvailsNSpace());
		entry.setText(entryType);
		disp.addContent(entry);
		return disp;
	}

}
