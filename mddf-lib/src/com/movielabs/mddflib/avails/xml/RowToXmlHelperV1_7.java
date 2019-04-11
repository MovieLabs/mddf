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
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;

import com.movielabs.mddflib.logging.LogMgmt;

/**
 * Create XML document from a v1.7 or v1.7.2 Excel spreadsheet. The XML
 * generated will be based on v2.2 of the Avails XSD and reflects a "best
 * effort" in that there is no guarantee that it is valid.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class RowToXmlHelperV1_7 extends AbstractRowHelper {

	static final String MISSING = "--FUBAR (missing)";

	/**
	 * @param logger
	 * @param fields
	 */
	RowToXmlHelperV1_7(AvailsSheet sheet, Row row, LogMgmt logger) {
		super(sheet, row, logger);
	}

	protected void makeAvail(XmlBuilder xb) {
		this.xb = xb;
		Element avail = xb.getAvailElement(this);

		/*
		 * Assets can be defined redundantly on multiple lines so the XmlBuilder is used
		 * to coordinate and filter out duplicates.
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.avails.xml.AbstractRowHelper#mDisposition()
	 */
	protected Element mDisposition() {
		Element disp = new Element("Disposition", xb.getAvailsNSpace());
		process(disp, "EntryType", xb.getAvailsNSpace(), "Disposition/EntryType");
		return disp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.avails.xml.AbstractRowHelper#mPublisher(java.lang.
	 * String, java.lang.String)
	 */
	protected Element mPublisher(String elName, String colKey) {
		Element pubEl = new Element(elName, xb.getAvailsNSpace());

		process(pubEl, "DisplayName", xb.getMdNSpace(), colKey);

		/*
		 * if ContactInfo is mandatory we can't get this info from the spreadsheet
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.avails.xml.AbstractRowHelper#buildAsset()
	 */
	protected Element buildAsset() {
		Namespace availNS = xb.getAvailsNSpace();
		Element assetEl = new Element("Asset", availNS);
		Element wtEl = new Element("WorkType", availNS);
		wtEl.setText(workType);
		xb.addToPedigree(wtEl, workTypePedigree);
		assetEl.addContent(wtEl);
		/*
		 * Source key for 'contentID' depends (unfortunately) on the WorkType of the
		 * Asset.
		 */
		String cidPrefix = "";
		switch (workType) {
		case "Series":
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
				xb.addToPedigree(bAssetEl, pg);
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
	 * Transactions per Avail, the XLSX mechanism only allows a single Transaction
	 * element to be defined <i>per-row</i>.
	 * 
	 * @return
	 */
	protected Element createTransaction() {
		Element transactionEl = new Element("Transaction", xb.getAvailsNSpace());
		/*
		 * TransactionID is OPTIONAL. For mystical reasons lost in the mists of time, it
		 * come from the 'AvailID' column.
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
		process(transactionEl, "ReportingID", xb.getAvailsNSpace(), prefix + "ReportingID");

		addAllTerms(transactionEl);

		process(transactionEl, "OtherInstructions", xb.getAvailsNSpace(), prefix + "OtherInstructions");

	}

	/**
	 * Add 1 or more Term elements to a Transaction.
	 * <p>
	 * As of Excel v 1.6. Terms are a mess. There are two modes for defining:
	 * </p>
	 * <ol>
	 * <li>Use 'PriceType' cell to define a <tt>termName</tt> and then get value
	 * from 'PriceValue' cell, or</li>
	 * <li>the use of columns implicitly linked to specific <tt>termName</tt></li>
	 * </ol>
	 * An example of the 2nd approach is the 'WatchDuration' column. What makes the
	 * handling even more complex is that a term that was handled via 'PriceType' in
	 * one version of the Excel may be handled via a dedicated column in another
	 * version.
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
			/*
			 * Any term may be prefixed with 'TPR-' to indicate temp price reduction
			 */
			String baseTName = tName.replaceFirst("TPR-", "");
			switch (baseTName) {
			case "Tier":
			case "Category":
			case "LicenseFee":
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
				Element moneyEl = process(termEl, "Money", xb.getAvailsNSpace(), prefix + "PriceValue");
				Pedigree curPGee = getPedigreedData(prefix + "PriceCurrency");
				if (moneyEl != null && isSpecified(curPGee)) {
					Attribute curAt = new Attribute("currency", curPGee.getRawValue());
					moneyEl.setAttribute(curAt);
					xb.addToPedigree(curAt, curPGee);
				}
				break;
			case "Season Only":
				break;
			default:
				String errMsg = "Unrecognized PriceType '" + tName + "'";
				Cell target = (Cell) pg.getSource();
				logger.logIssue(LogMgmt.TAG_XLSX, LogMgmt.LEV_ERR, target, errMsg, null, null, XmlBuilder.moduleId);
				return;

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
	 * Flag any deprecated PriceType terms. This essentially means terms that are
	 * now handled via a unique column.
	 * 
	 * @param pg
	 * @return filtered input data
	 */
	protected Pedigree filterDeprecated(Pedigree pg) {
		String value = pg.getRawValue();
		switch (value) {
		case "SRP":
			String errMsg = "Invalid PriceType  '" + value + "' for v1.7 Excel";
			Cell target = (Cell) pg.getSource();
			logger.logIssue(LogMgmt.TAG_XLSX, LogMgmt.LEV_ERR, target, errMsg, null, null, XmlBuilder.moduleId);
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

	protected void createSharedEntitlements() {
		/*
		 * SharedEntitlement is OPTIONAL. There are two 'ecosystems' supported by the
		 * Excel format UV and DMA.
		 */
		addEcosystem("UVVU", "Avail/UV_ID");
		addEcosystem("DMA", "Avail/DMA_ID");
	}

	protected void addEcosystem(String ecosysId, String colKey) {
		Pedigree pg = getPedigreedData(colKey);
		if (this.isSpecified(pg)) {
			Element eidEl = new Element("EcosystemID", xb.getAvailsNSpace());
			eidEl.setText(pg.getRawValue());
			Element avail = xb.getAvailElement(this);
			xb.addEntitlement(avail, ecosysId, eidEl);
		}
	}

	protected void addRegion(Element parentEl, String regionType, Namespace ns, String cellKey) {
		addRegion(parentEl, regionType, ns, cellKey, null);
	}

	protected void addRegion(Element parentEl, String regionType, Namespace ns, String cellKey, String separator) {
		Pedigree pg = getPedigreedData(cellKey);
		String rawValue = pg.getRawValue();
		Element countryEl = null;
		if (isSpecified(rawValue)) {
			String[] valueSet;
			if (separator == null) {
				valueSet = new String[1];
				valueSet[0] = rawValue;
			} else {
				valueSet = rawValue.split(separator);
			}
			for (int i = 0; i < valueSet.length; i++) {
				Element regionEl = new Element(regionType, ns);
				String code = valueSet[i].trim();
				if (code.length() > 2) {
					countryEl = mGenericElement("countryRegion", code, xb.getMdNSpace());
					// process(regionEl, "countryRegion", xb.getMdNSpace(),
					// cellKey);
				} else {
					countryEl = mGenericElement("country", code, xb.getMdNSpace());
					// countryEl = process(regionEl, "country",
					// xb.getMdNSpace(), cellKey);
				}
				if (countryEl != null) {
					regionEl.addContent(countryEl);
					parentEl.addContent(regionEl);
					xb.addToPedigree(countryEl, pg);
				}
			}
		}
	}

	/**
	 * Process start or end conditions for a Transaction. The <tt>cellKey</tt> is
	 * used to identify a cell whose expected value is
	 * <ul>
	 * <li>YYYY-MM-DD for a date.</li>
	 * <li>ISO 8601 Date+Time (i.e., <tt>YYYY-MM-DDTHH:MM:SS</tt>) when time is
	 * included, or</li>
	 * <li>a key word such as "Immediate"</li>
	 * </ul>
	 * If the cell contains a date (or date/time) value, a 'Start' or 'End' element
	 * is created. If, however, the cell contains the name of a condition (e.g.,
	 * "Immediate"), a 'StartCondition' or 'EndCondition' element is created. If the
	 * cell contains an invalid value, no element is created an a <tt>null</tt>
	 * value will be returned.
	 * 
	 * @param parentEl
	 * @param childName either 'Start' or 'End'
	 * @param ns
	 * @param cellKey
	 * @return the created element or null
	 */
	protected Element processCondition(Element parentEl, String childName, Namespace ns, String cellKey) {
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
			return condEl;
		} else {
			return null;
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
			usesFormula(cell);
			String value = dataF.formatCellValue(cell);
			if (value == null) {
				value = "";
			}
			return value;
		}
	}

}
