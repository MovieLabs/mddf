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
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;

import com.movielabs.mddflib.avails.xlsx.AvailsSheet;

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
	protected Row row;
	protected XmlBuilder xb;
	AvailsSheet sheet;
	private String workType = "";
	private DataFormatter dataF = new DataFormatter();

	/**
	 * @param fields
	 */
	RowToXmlHelper(AvailsSheet sheet, Row row) {
		super();
		this.sheet = sheet;
		this.row = row;

	}

	protected void makeAvail(XmlBuilder xb) throws Exception {
		this.xb = xb;
		Element avail = xb.getAvailElement(this);
		/*
		 * Need to save the current workType for use in Transaction/Terms
		 */
		Pedigree pg = getPedigreedData("AvailAsset/WorkType");
		this.workType = pg.getRawValue();

		// Asset
		if (xb.isRequired("WorkType", "avails") || (pg != null && this.isSpecified(pg.getRawValue()))) {
			Element assetEl = createAsset(pg);
			xb.addAsset(avail, assetEl);
		}

		Element e = createTransaction();
		// Transaction
		if (e != null) {
			xb.addTransaction(avail, e);
		}

	}

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	Element mDisposition() {
		Element disp = new Element("Disposition", xb.getAvailsNSpace());
		process(disp, "EntryType", xb.getAvailsNSpace(), "Disposition/EntryType");
		return disp;
	}

	/**
	 * Create an <tt>mdmec:Publisher-type</tt> XML element with a md:DisplayName
	 * element child, and populate the latter with the DisplayName
	 * 
	 * @param elName
	 *            the parent element to be created (i.e., Licensor or
	 *            ServiceProvider)
	 * @param displayName
	 *            the name to be held in the DisplayName child node
	 * @return the created element
	 */
	Element mPublisher(String elName, String colKey) {
		Element pubEl = new Element(elName, xb.getAvailsNSpace());

		process(pubEl, "DisplayName", xb.getMdNSpace(), colKey);

		// XXX ContactInfo mandatory but can't get this info from the
		// spreadsheet
		if (xb.isRequired("ContactInfo", "mdmec")) {
			Element e = new Element("ContactInfo", xb.getMdMecNSpace());
			Element e2 = new Element("Name", xb.getMdNSpace());
			e.addContent(e2);
			e2 = new Element("PrimaryEmail", xb.getMdNSpace());
			e.addContent(e2);
			pubEl.addContent(e);
		}
		return pubEl;
	}

	protected Element createAsset(Pedigree workTypePedigree) {
		Element assetEl = new Element("Asset", xb.getAvailsNSpace());
		Element wt = new Element("WorkType", xb.getAvailsNSpace());
		wt.setText(workTypePedigree.getRawValue());
		xb.addToPedigree(wt, workTypePedigree);
		assetEl.addContent(wt);
		mAssetBody(assetEl);
		return assetEl;
	}

	/**
	 * Construct Element instantiating the <tt>AvailMetadata-type</tt>. This
	 * method should be extended or overridden by sub-classes specific to a
	 * given work type (i.e., Movie, Episode, Season, etc.)
	 * 
	 * @param assetEl
	 * @return
	 */
	protected void mAssetBody(Element assetEl) {
		Element e;
		Pedigree pg = getPedigreedData("AvailAsset/ContentID");
		String contentID = pg.getRawValue();
		Attribute attEl = new Attribute("contentID", contentID);
		assetEl.setAttribute(attEl);
		xb.addToPedigree(attEl, pg);
		xb.addToPedigree(assetEl, pg);

		Element metadata = new Element("Metadata", xb.getAvailsNSpace());

		/*
		 * TitleDisplayUnlimited is OPTIONAL in SS but REQUIRED in XML;
		 * workaround by assigning it internal alias value which is REQUIRED in
		 * SS.
		 */
		Pedigree titleDuPg = getPedigreedData("AvailMetadata/TitleDisplayUnlimited");
		Pedigree titleAliasPg = getPedigreedData("AvailMetadata/TitleInternalAlias");
		if (!isSpecified(titleDuPg)) {
			titleDuPg = titleAliasPg;
		}
		e = mGenericElement("TitleDisplayUnlimited", titleDuPg.getRawValue(), xb.getAvailsNSpace());
		xb.addToPedigree(e, titleDuPg);
		metadata.addContent(e);

		// TitleInternalAlias
		if (xb.isRequired("TitleInternalAlias", "avails") || isSpecified(titleAliasPg)) {
			e = mGenericElement("TitleInternalAlias", titleAliasPg.getRawValue(), xb.getAvailsNSpace());
			metadata.addContent(e);
			xb.addToPedigree(e, titleAliasPg);
		}

		// ProductID --> EditEIDR-URN ( optional field)
		process(metadata, "EditEIDR-URN", xb.getAvailsNSpace(), "AvailAsset/ProductID");

		// ContentID --> TitleEIDR-URN ( optional field)
		process(metadata, "TitleEIDR-URN", xb.getAvailsNSpace(), "AvailAsset/ContentID");

		// AltID --> AltIdentifier
		pg = getPedigreedData("AvailMetadata/AltID");
		if (xb.isRequired("AltIdentifier", "avails") || isSpecified(pg)) {
			String value = pg.getRawValue();
			Element altIdEl = new Element("AltIdentifier", xb.getAvailsNSpace());
			Element cid = new Element("Namespace", xb.getMdNSpace());
			cid.setText(MISSING);
			altIdEl.addContent(cid);
			Element idEl = mGenericElement("Identifier", value, xb.getMdNSpace());
			altIdEl.addContent(idEl);
			xb.addToPedigree(idEl, pg);
			Element loc = new Element("Location", xb.getMdNSpace());
			loc.setText(MISSING);
			altIdEl.addContent(loc);
			metadata.addContent(altIdEl);
		}

		process(metadata, "ReleaseDate", xb.getAvailsNSpace(), "AvailMetadata/ReleaseYear");
		process(metadata, "RunLength", xb.getAvailsNSpace(), "AvailMetadata/TotalRunTime");

		mReleaseHistory(metadata, "original", "AvailMetadata/ReleaseHistoryOriginal");
		mReleaseHistory(metadata, "DVD", "AvailMetadata/ReleaseHistoryPhysicalHV");

		process(metadata, "USACaptionsExemptionReason", xb.getAvailsNSpace(), "AvailMetadata/CaptionExemption");

		mRatings(metadata);

		process(metadata, "EncodeID", xb.getAvailsNSpace(), "AvailAsset/EncodeID");
		process(metadata, "LocalizationOffering", xb.getAvailsNSpace(), "AvailMetadata/LocalizationType");

		// Attach generated Metadata node
		assetEl.addContent(metadata);
	}

	protected Element createTransaction() throws Exception {
		Element transaction = new Element("Transaction", xb.getAvailsNSpace());
		/*
		 * TransactionID is OPTIONAL. For mystical reasons lost in the mists of
		 * time, it come from the 'AvailID' column.
		 */
		Pedigree pg = getPedigreedData("Avail/AvailID");
		if (this.isSpecified(pg)) {
			transaction.setAttribute("TransactionID", pg.getRawValue());
		}
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
		process(transaction, "LicenseType", xb.getAvailsNSpace(), prefix + "LicenseType");
		process(transaction, "Description", xb.getAvailsNSpace(), prefix + "Description");
		processRegion(transaction, "Territory", xb.getAvailsNSpace(), prefix + "Territory");

		// Start or StartCondition
		processCondition(transaction, "Start", xb.getAvailsNSpace(), prefix + "Start");
		// End or EndCondition
		processCondition(transaction, "End", xb.getAvailsNSpace(), prefix + "End");

		process(transaction, "AllowedLanguage", xb.getAvailsNSpace(), prefix + "AllowedLanguage");
		process(transaction, "AssetLanguage", xb.getAvailsNSpace(), prefix + "AssetLanguage");
		process(transaction, "HoldbackLanguage", xb.getAvailsNSpace(), prefix + "HoldbackLanguage");
		process(transaction, "LicenseRightsDescription", xb.getAvailsNSpace(), prefix + "LicenseRightsDescription");
		process(transaction, "FormatProfile", xb.getAvailsNSpace(), prefix + "FormatProfile");
		process(transaction, "ContractID", xb.getAvailsNSpace(), prefix + "ContractID");

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
		Pedigree pg = getPedigreedData(prefix + "PriceType");
		if (isSpecified(pg)) {
			String tName = pg.getRawValue();
			Element termEl = new Element("Term", xb.getAvailsNSpace());
			transaction.addContent(termEl);
			switch (tName) {
			case "Tier":
			case "Category":
				process(termEl, "Text", xb.getAvailsNSpace(), prefix + "PriceValue");
				break;
			case "WSP":
				if (workType.equals("Episode")) {
					tName = "EpisodeWSP";
				} else if (workType.equals("Season")) {
					tName = "SeasonWSP";
				}
			case "DMRP":
			case "SMRP":
				Element moneyEl = process(termEl, "Money", xb.getAvailsNSpace(), prefix + "PriceValue");
				Pedigree curPGee = getPedigreedData(prefix + "PriceCurrency");
				if (moneyEl != null && isSpecified(curPGee)) {
					Attribute curAt = new Attribute("currency", curPGee.getRawValue());
					moneyEl.setAttribute(curAt);
					xb.addToPedigree(curAt, curPGee);
				}
				break;
			case "Season Only":
			}
			termEl.setAttribute("termName", tName);
			xb.addToPedigree(termEl, pg);
		}
		/*
		 * Now look for Terms specified via other columns....
		 */

		Element termEl = processTerm(prefix + "SuppressionLiftDate", "SuppressionLiftDate", "Event");
		if (termEl != null) {
			transaction.addContent(termEl);
		}

		termEl = processTerm(prefix + "AnnounceDate", "AnnounceDate", "Event");
		if (termEl != null) {
			transaction.addContent(termEl);
		}

		termEl = processTerm(prefix + "SpecialPreOrderFulfillDate", "PreOrderFulfillDate", "Event");
		if (termEl != null) {
			transaction.addContent(termEl);
		}

		termEl = processTerm(prefix + "SRP", "SRP", "Money");
		if (termEl != null) {
			transaction.addContent(termEl);
		}

		termEl = processTerm(prefix + "RentalDuration", "RentalDuration", "Duration");
		if (termEl != null) {
			transaction.addContent(termEl);
		}

		termEl = processTerm(prefix + "WatchDuration", "WatchDuration", "Duration");
		if (termEl != null) {
			transaction.addContent(termEl);
		}

		termEl = processTerm(prefix + "FixedEndDate", "FixedEndDate", "Event");
		if (termEl != null) {
			transaction.addContent(termEl);
		}

		termEl = processTerm(prefix + "HoldbackLanguage", "HoldbackLanguage", "Language");
		if (termEl != null) {
			transaction.addContent(termEl);
		}

		termEl = processTerm(prefix + "AllowedLanguages", "HoldbackExclusionLanguage", "Duration");
		if (termEl != null) {
			transaction.addContent(termEl);
		}
	}

	private Element processTerm(String src, String termName, String subElName) {
		Pedigree pg = getPedigreedData(src);
		if ((pg != null) && (isSpecified(pg.getRawValue()))) {
			Element termEl = new Element("Term", xb.getAvailsNSpace());
			termEl.setAttribute("termName", termName);
			Element childEl = mGenericElement(subElName, pg.getRawValue(), xb.getAvailsNSpace());
			termEl.addContent(childEl);
			xb.addToPedigree(childEl, pg);
			xb.addToPedigree(termEl, pg);
			return termEl;
		} else {
			return null;
		}
	}

	/**
	 * @param parentEl
	 * @param type
	 * @param cellKey
	 * @param row
	 */
	private void mReleaseHistory(Element parentEl, String type, String cellKey) {
		Pedigree pg = getPedigreedData(cellKey);
		if (!isSpecified(pg)) {
			return;
		}
		String value = pg.getRawValue();
		Element rh = new Element("ReleaseHistory", xb.getAvailsNSpace());
		Element rt = new Element("ReleaseType", xb.getMdNSpace());
		rt.setText(type);
		rh.addContent(rt);
		Element dateEl = mGenericElement("Date", value, xb.getMdNSpace());
		rh.addContent(dateEl);
		xb.addToPedigree(dateEl, pg);
		parentEl.addContent(rh);
	}

	protected void mRatings(Element m) {
		String ratingSystem = getData("AvailMetadata/RatingSystem");
		String ratingValue = getData("AvailMetadata/RatingValue");
		String ratingReason = getData("AvailMetadata/RatingReason");
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
		Pedigree terrPg = getPedigreedData("AvailTrans/Territory");
		if (isSpecified(terrPg)) {
			Element country = new Element("country", xb.getMdNSpace());
			region.addContent(country);
			country.setText(terrPg.getRawValue()); 
			xb.addToPedigree(country, terrPg);
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
	Element mGenericElement(String name, String val, Namespace ns) {
		Element el = new Element(name, ns);
		String formatted = xb.formatForType(name, ns, val);
		el.setText(formatted);
		return el;
	}

	protected Element process(Element parentEl, String childName, Namespace ns, String cellKey) {
		Pedigree pg = getPedigreedData(cellKey);
		if (pg == null) {
			return null;
		}
		String value = pg.getRawValue();
		if (xb.isRequired(childName, ns.getPrefix()) || isSpecified(value)) {
			Element childEl = mGenericElement(childName, value, ns);
			parentEl.addContent(childEl);
			xb.addToPedigree(childEl, pg);
			return childEl;
		} else {
			return null;
		}
	}

	private void processRegion(Element parentEl, String regionType, Namespace ns, String cellKey) {
		Element regionEl = new Element(regionType, ns);
		Element countryEl = process(regionEl, "country", xb.getMdNSpace(), cellKey);
		if (countryEl != null) {
			parentEl.addContent(regionEl);
		}
	}

	/**
	 * Process start or end conditions for a Transaction.
	 * 
	 * @param parentEl
	 * @param childName
	 * @param ns
	 * @param cellKey
	 * @param row
	 * @return
	 */
	private boolean processCondition(Element parentEl, String childName, Namespace ns, String cellKey) {
		Pedigree pg = getPedigreedData(cellKey);
		String value = pg.getRawValue();
		if (isSpecified(value)) {
			Element condEl = null;
			// does it start with 'yyyy' ?
			if (value.matches("^[\\d]{4}-.*")) {
				condEl = mGenericElement(childName, value, ns);
			} else {
				condEl = mGenericElement(childName + "Condition", value, ns);
			}
			parentEl.addContent(condEl);
			xb.addToPedigree(condEl, pg);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param colKey
	 * @return
	 */
	String getData(String colKey) {
		int cellIdx = sheet.getColumnIdx(colKey);
		if (cellIdx < 0) {
			return null;
		} else {
			Cell cell = row.getCell(cellIdx);
			String value = dataF.formatCellValue(cell);
			if (value == null) {
				value = "";
			}
			return value;
		}
	}

	/**
	 * @param colKey
	 * @return
	 */
	Pedigree getPedigreedData(String colKey) {
		int cellIdx = sheet.getColumnIdx(colKey);
		if (cellIdx < 0) {
			return null;
		}
		Cell sourceCell = row.getCell(cellIdx);
		String value = dataF.formatCellValue(sourceCell);
		if (value == null) {
			value = "";
		}
		Pedigree ped = new Pedigree(sourceCell, value);

		return ped;
	}

	/**
	 * Returns <tt>true</tt> if the valueSrc is both non-null and not empty. The
	 * value source must be an instance of either the <tt>String</tt> or
	 * <tt>Pedigree</tt> class or an <tt>IllegalArgumentException</tt> is
	 * thrown.
	 * 
	 * @param valueSrc
	 * @throws IllegalArgumentException
	 */
	protected boolean isSpecified(Object valueSrc) throws IllegalArgumentException {
		if (valueSrc == null) {
			return false;
		}
		if (valueSrc instanceof String) {
			return (!((String) valueSrc).isEmpty());
		}

		if (valueSrc instanceof Pedigree) {
			return (!((Pedigree) valueSrc).isEmpty());
		}
		String msg = valueSrc.getClass().getCanonicalName() + " is not supported value source";
		throw new IllegalArgumentException(msg);
	}

	/**
	 * @return
	 */
	public int getRowNumber() {
		return row.getRowNum();
	}

}
