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
import com.movielabs.mddflib.logging.LogMgmt;

/**
 * Create XML document from a v1.7 Excel spreadsheet. The XML generated will be
 * based on v2.2 of the Avails XSD and reflects a "best effort" in that there is
 * no guarantee that it is valid.
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

	static final String MISSING = "--FUBAR (missing)";
	protected Row row;
	protected XmlBuilder xb;
	protected AvailsSheet sheet;
	protected String workType = "";
	protected DataFormatter dataF = new DataFormatter();
	private Pedigree workTypePedigree;

	/**
	 * @param fields
	 */
	RowToXmlHelper(AvailsSheet sheet, Row row) {
		super();
		this.sheet = sheet;
		this.row = row;
		/*
		 * Need to save the current workType for use in Transaction/Terms
		 */
		workTypePedigree = getPedigreedData("AvailAsset/WorkType");
		this.workType = workTypePedigree.getRawValue();
	}

	protected void makeAvail(XmlBuilder xb) {
		this.xb = xb;
		Element avail = xb.getAvailElement(this);

		/*
		 * Assets can be defined redundantly on multiple lines so the XmlBuilder
		 * is used to coordinate and filter out duplicates.
		 */
		xb.createAsset(this);

		Element e = createTransaction();
		// Transaction
		if (e != null) {
			xb.addTransaction(avail, e);
		}

		createSharedEntitlements();
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

		/*
		 * if ContactInfo is mandatory we can't get this info from the
		 * spreadsheet
		 */
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

	/**
	 * Invoked by XmlBuilder.createAsset() when a pre-existing Asset element
	 * does not exist.
	 * 
	 * @param workTypePedigree
	 * @return
	 */
	protected Element buildAsset() {
		Namespace availNS = xb.getAvailsNSpace();
		Element assetEl = new Element("Asset", availNS);
		Element wtEl = new Element("WorkType", availNS);
		wtEl.setText(workType);
		xb.addToPedigree(wtEl, workTypePedigree);
		assetEl.addContent(wtEl);
		/*
		 * Source key for 'contentID' depends (unfortunately) on the WorkType of
		 * the Asset.
		 */
		String cidPrefix = "";
		switch (workType) {
		case "Season":
		case "Episode":
			cidPrefix = workType;
			break;
		default:
		}
		String colKey = "AvailAsset/" + cidPrefix + "ContentID";
		Pedigree pg = getPedigreedData(colKey);
		String contentID = pg.getRawValue();
		Attribute attEl = new Attribute("contentID", contentID);
		assetEl.setAttribute(attEl);
		xb.addToPedigree(attEl, pg);
		xb.addToPedigree(assetEl, pg);

		xb.createAssetMetadata(assetEl, workType, this);

		pg = getPedigreedData("Avail/BundledALIDs");
		if (isSpecified(pg)) {
			String[] alidList = pg.getRawValue().split(";");
			for (int i = 0; i < alidList.length; i++) {
				Element bAssetEl = new Element("BundledAsset", availNS);
				Element bAlidEl = new Element("BundledALID", availNS);
				bAlidEl.setText(alidList[i]);
				xb.addToPedigree(bAlidEl, pg);
				bAssetEl.addContent(bAlidEl);
				assetEl.addContent(bAssetEl);
			}
		}
		return assetEl;
	}

	/**
	 * Create a Transaction Element. While the Avail XSD allows multiple
	 * Transactions per Avail, the XLSX mechanism only allows a single
	 * Transaction element to be defined <i>per-row</i>.
	 * 
	 * @return
	 */
	protected Element createTransaction() {
		Element transactionEl = new Element("Transaction", xb.getAvailsNSpace());
		/*
		 * TransactionID is OPTIONAL. For mystical reasons lost in the mists of
		 * time, it come from the 'AvailID' column.
		 */
		Pedigree pg = getPedigreedData("Avail/AvailID");
		if (this.isSpecified(pg)) {
			transactionEl.setAttribute("TransactionID", pg.getRawValue());
		}
		processTransactionBody(transactionEl);
		return transactionEl;
	}

	/**
	 * populate a Transaction element
	 * 
	 * @param transactionEl
	 */
	protected void processTransactionBody(Element transactionEl) {
		String prefix = "AvailTrans/";
		process(transactionEl, "LicenseType", xb.getAvailsNSpace(), prefix + "LicenseType");
		process(transactionEl, "Description", xb.getAvailsNSpace(), prefix + "Description");
		addRegion(transactionEl, "Territory", xb.getAvailsNSpace(), prefix + "Territory");

		// Start or StartCondition
		processCondition(transactionEl, "Start", xb.getAvailsNSpace(), prefix + "Start");
		// End or EndCondition
		processCondition(transactionEl, "End", xb.getAvailsNSpace(), prefix + "End");

		process(transactionEl, "AllowedLanguage", xb.getAvailsNSpace(), prefix + "AllowedLanguages", ",");
		process(transactionEl, "AssetLanguage", xb.getAvailsNSpace(), prefix + "AssetLanguage");
		process(transactionEl, "HoldbackLanguage", xb.getAvailsNSpace(), prefix + "HoldbackLanguage", ",");
		process(transactionEl, "LicenseRightsDescription", xb.getAvailsNSpace(), prefix + "LicenseRightsDescription");
		process(transactionEl, "FormatProfile", xb.getAvailsNSpace(), prefix + "FormatProfile");
		process(transactionEl, "ContractID", xb.getAvailsNSpace(), prefix + "ContractID");

		addAllTerms(transactionEl);

		process(transactionEl, "OtherInstructions", xb.getAvailsNSpace(), prefix + "OtherInstructions");

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
	 * An example of the 2nd approach is the 'WatchDuration' column. What makes
	 * the handling even more complex is that a term that was handled via
	 * 'PriceType' in one version of the Excel may be handled via a dedicated
	 * column in another version.
	 * </p>
	 * 
	 * @param transactionEl
	 */
	protected void addAllTerms(Element transactionEl) {
		String prefix = "AvailTrans/";
		/*
		 * May be multiple 'terms'. Start with one specified via the PriceType
		 */
		Pedigree pg = getPedigreedData(prefix + "PriceType");

		pg = filterDeprecated(pg);
		if (isSpecified(pg)) {
			String tName = pg.getRawValue();
			Element termEl = new Element("Term", xb.getAvailsNSpace());
			switch (tName) {
			case "Tier":
			case "Category":
			case "LicenseFee":
			case "N.A.":
			case "NA":
				process(termEl, "Text", xb.getAvailsNSpace(), prefix + "PriceValue");
				break;
			case "WSP":
				if (workType.equals("Episode")) {
					tName = "EpisodeWSP";
				} else if (workType.equals("Season")) {
					tName = "SeasonWSP";
				}
			case "EpisodeWSP":
			case "SeasonWSP":
			case "SRP":
			case "DMRP":
			case "SMRP":
			case "TPR-SRP":
			case "TPR-WSP":
			case "TPR-EpisodeWSP":
			case "TPR-SeasonWSP":
			case "TPR-DMRP":
			case "TPR-SMRP":
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
			transactionEl.addContent(termEl);
		}

		/*
		 * Now look for Terms specified via other columns....
		 */
		Element termEl = addTerm(transactionEl, prefix + "SuppressionLiftDate", "SuppressionLiftDate", "Event");
		termEl = addTerm(transactionEl, prefix + "AnnounceDate", "AnnounceDate", "Event");
		termEl = addTerm(transactionEl, prefix + "SpecialPreOrderFulfillDate", "PreOrderFulfillDate", "Event");
		termEl = addTerm(transactionEl, prefix + "SRP", "SRP", "Money");
		termEl = addTerm(transactionEl, prefix + "RentalDuration", "RentalDuration", "Duration");
		termEl = addTerm(transactionEl, prefix + "WatchDuration", "WatchDuration", "Duration");
		termEl = addTerm(transactionEl, prefix + "FixedEndDate", "FixedEndDate", "Event");
	}

	/**
	 * Filter out deprecated PriceType terms. This essentially means terms that
	 * are now handled via a unique column.
	 * 
	 * @param pg
	 * @return
	 */
	protected Pedigree filterDeprecated(Pedigree pg) {
		String value = pg.getRawValue();
		switch (value) {
		case "SRP":
			String errMsg = "The value '" + value + "' is not a valid PriceType for v1.7 Excel";
			xb.appendToLog(errMsg, LogMgmt.LEV_ERR, (Cell) pg.getSource());
			return null;
		}
		return pg;
	}

	protected Element addTerm(Element parent, String src, String termName, String subElName) {
		Pedigree pg = getPedigreedData(src);
		if ((pg != null) && (isSpecified(pg.getRawValue()))) {
			Element termEl = new Element("Term", xb.getAvailsNSpace());
			termEl.setAttribute("termName", termName);
			Element childEl = mGenericElement(subElName, pg.getRawValue(), xb.getAvailsNSpace());
			termEl.addContent(childEl);
			xb.addToPedigree(childEl, pg);
			xb.addToPedigree(termEl, pg);
			parent.addContent(termEl);
			return termEl;
		} else {
			return null;
		}
	}

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	/**
	 * @return
	 */
	protected void createSharedEntitlements() {
		/*
		 * SharedEntitlement is OPTIONAL. There are two 'ecosystems' supported
		 * by the Excel format UV and DMA.
		 */
		addEcosystem("UVVU", "Avail/UV_ID");
		addEcosystem("DMA", "Avail/DMA_ID");
	}

	protected void addEcosystem(String ecosysId, String colKey) { 
		Pedigree pg = getPedigreedData(colKey);
		if (this.isSpecified(pg)) {
			Element eidEl = new Element("EcosystemID", xb.getAvailsNSpace());
			eidEl.setText( pg.getRawValue());
			Element avail = xb.getAvailElement(this);
			xb.addEntitlement(avail, ecosysId, eidEl);
		}
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

	/**
	 * Same as invoking
	 * <tt>process(Element parentEl, String childName, Namespace ns, String cellKey, String separator) </tt>
	 * with a <tt>null</tt> separator. A single child element will therefore be
	 * created.
	 * 
	 * @param parentEl
	 * @param childName
	 * @param ns
	 * @param cellKey
	 * @return
	 */
	protected Element process(Element parentEl, String childName, Namespace ns, String cellKey) {
		Element[] elementList = process(parentEl, childName, ns, cellKey, null);
		if (elementList != null) {
			return elementList[0];
		} else {
			return null;
		}
	}

	/**
	 * Add zero or more child elements with the specified name and namespace.
	 * The number of child elements created will be determined by the contents
	 * of the indicated cell. If <tt>separator</tt> is not <tt>null</tt>, then
	 * it will be used to split the string value in the cell with each resulting
	 * sub-string being used to create a distinct child element.
	 * 
	 * @param parentEl
	 * @param childName
	 * @param ns
	 * @param cellKey
	 * @param separator
	 * @return an array of child <tt>Element</tt> instances
	 */
	protected Element[] process(Element parentEl, String childName, Namespace ns, String cellKey, String separator) {
		Pedigree pg = getPedigreedData(cellKey);
		if (pg == null) {
			return null;
		}
		String value = pg.getRawValue();
		if (isSpecified(value) || xb.isRequired(childName, ns.getPrefix())) {
			String[] valueSet;
			if (separator == null) {
				valueSet = new String[1];
				valueSet[0] = value;
			} else {
				valueSet = value.split(separator);
			}
			Element[] elementList = new Element[valueSet.length];
			for (int i = 0; i < valueSet.length; i++) {
				Element childEl = mGenericElement(childName, valueSet[i], ns);
				parentEl.addContent(childEl);
				xb.addToPedigree(childEl, pg);
				elementList[i] = childEl;
			}
			return elementList;
		} else {
			return null;
		}
	}

	void addRegion(Element parentEl, String regionType, Namespace ns, String cellKey) {
		Element regionEl = new Element(regionType, ns);
		Pedigree pg = getPedigreedData(cellKey);
		String value = pg.getRawValue();
		Element countryEl = null;
		if (isSpecified(value)) {
			if (value.length() > 2) {
				countryEl = process(regionEl, "countryRegion", xb.getMdNSpace(), cellKey);
			} else {
				countryEl = process(regionEl, "country", xb.getMdNSpace(), cellKey);
			}
		}
		if (countryEl != null) {
			parentEl.addContent(regionEl);
		}
	}

	/**
	 * Process start or end conditions for a Transaction. The expected value is
	 * <ul>
	 * <li>YYYY-MM-DD for a date.</li>
	 * <li>ISO 8601 Date+Time (i.e., <tt>YYYY-MM-DDTHH:MM:SS</tt>) when time is
	 * included, or</li>
	 * <li>a key word such as "Immediate"</li>
	 * </ul>
	 * 
	 * @param parentEl
	 * @param childName
	 * @param ns
	 * @param cellKey
	 * @param row
	 * @return
	 */
	protected boolean processCondition(Element parentEl, String childName, Namespace ns, String cellKey) {
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
	protected String getData(String colKey) {
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
	protected Pedigree getPedigreedData(String colKey) {
		int cellIdx = sheet.getColumnIdx(colKey);
		if (cellIdx < 0) {
			return null;
		}
		Cell sourceCell = row.getCell(cellIdx);
		String value = dataF.formatCellValue(sourceCell);
		if (value == null) {
			value = "";
		}
		if (sourceCell != null && (sourceCell.getCellType() == Cell.CELL_TYPE_FORMULA)) {
			xb.appendToLog("Use of Excel Formulas not supported", LogMgmt.LEV_ERR, sourceCell);
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
