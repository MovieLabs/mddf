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
package com.movielabs.mddflib.avails.xml.streaming;

import org.apache.poi.ss.usermodel.Cell;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;

import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.avails.xml.RowDataSrc;
import com.movielabs.mddflib.avails.xml.DefaultXmlBuilder;
import com.movielabs.mddflib.logging.LogMgmt;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public abstract class StreamingRowIngester implements RowDataSrc {

	protected LogMgmt logger;
	protected StreamingXmlBuilder builder;
	protected Pedigree workTypePedigree;
	protected String workType;
	protected String[] row;
	protected Integer rowNum;

	StreamingRowIngester(String[] row, int rowNum, StreamingXmlBuilder builder, LogMgmt logger) {
		super();
		this.row = row;
		this.rowNum = rowNum;
		this.logger = logger;
		this.builder = builder;
		makeAvail();
	}

	private void makeAvail() {
		/*
		 * Need to save the current workType for use in Transaction/Terms
		 */
		workTypePedigree = getPedigreedData("AvailAsset/WorkType");
		this.workType = workTypePedigree.getRawValue();

		Element avail = builder.getAvailElement(this);

		/*
		 * Assets can be defined redundantly on multiple lines so the XmlBuilder is used
		 * to coordinate and filter out duplicates.
		 */
		builder.createAsset(this);

		Element e = createTransaction();
		// Transaction
		if (e != null) {
			builder.addTransaction(avail, e);
		}

		createSharedEntitlements();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddflib.avails.xml.streaming.RowDataSrc#getPedigreedData(java.
	 * lang.String)
	 */
	@Override
	public Pedigree getPedigreedData(String colKey) {
		int cellIdx = builder.getColumnIdx(colKey);
		if (cellIdx < 0) {
			return null;
		}
		String value = row[cellIdx];
		if (value == null) {
			value = "";
		}
		// TODO: trap use of formulas???
		Pedigree ped = new Pedigree(rowNum, value);

		return ped;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.avails.xml.streaming.RowDataSrc#getData(java.lang.
	 * String)
	 */
	@Override
	public String getData(String colKey) {
		int cellIdx = builder.getColumnIdx(colKey);
		if (cellIdx < 0) {
			return null;
		} else {
			String value = row[cellIdx];
			if (value == null) {
				value = "";
			}
			return value;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.avails.xml.streaming.RowDataSrc#process(org.jdom2.
	 * Element, java.lang.String, org.jdom2.Namespace, java.lang.String)
	 */
	@Override
	public Element process(Element parentEl, String childName, Namespace ns, String cellKey) {
		Element[] elementList = process(parentEl, childName, ns, cellKey, null);
		if (elementList != null) {
			return elementList[0];
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.avails.xml.streaming.RowDataSrc#process(org.jdom2.
	 * Element, java.lang.String, org.jdom2.Namespace, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public Element[] process(Element parentEl, String childName, Namespace ns, String cellKey, String separator) {
		Pedigree pg = getPedigreedData(cellKey);
		if (pg == null) {
			return null;
		}
		String value = pg.getRawValue();
		if (Pedigree.isSpecified(value) || builder.isRequired(childName, ns.getPrefix())) {
			String[] valueSet;
			if (separator == null) {
				valueSet = new String[1];
				valueSet[0] = value;
			} else {
				valueSet = value.split(separator);
			}
			Element[] elementList = new Element[valueSet.length];
			for (int i = 0; i < valueSet.length; i++) {
				Element childEl = builder.mGenericElement(childName, valueSet[i], ns);
				parentEl.addContent(childEl);
				builder.addToPedigree(childEl, pg);
				elementList[i] = childEl;
			}
			return elementList;
		} else {
			return null;
		}
	}

	abstract protected Element mDisposition();

	/**
	 * Create an <tt>mdmec:Publisher-type</tt> XML element with a md:DisplayName
	 * element child, and populate the latter with the DisplayName
	 * 
	 * @param elName the parent element to be created (i.e., Licensor or
	 *               ServiceProvider)
	 * @param colKey the name to be held in the DisplayName child node
	 * @return the created element
	 */
	abstract protected Element mPublisher(String elName, String colKey);

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.avails.xml.streaming.RowDataSrc#getRowNumber()
	 */
	@Override
	public int getRowNumber() {
		return rowNum.intValue();
	}

	/**
	 * @return
	 */
	abstract protected Element buildAsset();

	/**
	 * Create a Transaction Element. While the Avail XSD allows multiple
	 * Transactions per Avail, the XLSX mechanism only allows a single Transaction
	 * element to be defined <i>per-row</i>.
	 * 
	 * @return
	 */
	protected Element createTransaction() {
		Element transactionEl = new Element("Transaction", builder.getAvailsNSpace());
		/*
		 * TransactionID is OPTIONAL. For mystical reasons lost in the mists of time, it
		 * come from the 'AvailID' column.
		 */
		Pedigree pg = getPedigreedData("Avail/AvailID");
		if (Pedigree.isSpecified(pg)) {
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
		process(transactionEl, "LicenseType", builder.getAvailsNSpace(), prefix + "LicenseType");
		process(transactionEl, "Description", builder.getAvailsNSpace(), prefix + "Description");
		addRegion(transactionEl, "Territory", builder.getAvailsNSpace(), prefix + "Territory");

		// Start or StartCondition
		processCondition(transactionEl, "Start", builder.getAvailsNSpace(), prefix + "Start");
		// End or EndCondition
		processCondition(transactionEl, "End", builder.getAvailsNSpace(), prefix + "End");

		process(transactionEl, "AllowedLanguage", builder.getAvailsNSpace(), prefix + "AllowedLanguages", ",");
		process(transactionEl, "AssetLanguage", builder.getAvailsNSpace(), prefix + "AssetLanguage");
		process(transactionEl, "HoldbackLanguage", builder.getAvailsNSpace(), prefix + "HoldbackLanguage", ",");
		process(transactionEl, "LicenseRightsDescription", builder.getAvailsNSpace(),
				prefix + "LicenseRightsDescription");
		process(transactionEl, "FormatProfile", builder.getAvailsNSpace(), prefix + "FormatProfile");
		process(transactionEl, "ContractID", builder.getAvailsNSpace(), prefix + "ContractID");
		process(transactionEl, "ReportingID", builder.getAvailsNSpace(), prefix + "ReportingID");

		addAllTerms(transactionEl);

		process(transactionEl, "OtherInstructions", builder.getAvailsNSpace(), prefix + "OtherInstructions");

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
		if (Pedigree.isSpecified(pg)) {
			String tName = pg.getRawValue();
			Element termEl = new Element("Term", builder.getAvailsNSpace());
			/*
			 * Any term may be prefixed with 'TPR-' to indicate temp price reduction
			 */
			String baseTName = tName.replaceFirst("TPR-", "");
			switch (baseTName) {
			case "Tier":
			case "Category":
			case "LicenseFee":
			case "NA":
				process(termEl, "Text", builder.getAvailsNSpace(), prefix + "PriceValue");
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
				Element moneyEl = process(termEl, "Money", builder.getAvailsNSpace(), prefix + "PriceValue");
				Pedigree curPGee = getPedigreedData(prefix + "PriceCurrency");
				if (moneyEl != null && Pedigree.isSpecified(curPGee)) {
					Attribute curAt = new Attribute("currency", curPGee.getRawValue());
					moneyEl.setAttribute(curAt);
					builder.addToPedigree(curAt, curPGee);
				}
				break;
			case "Season Only":
				break;
			default:
				String errMsg = "Unrecognized PriceType '" + tName + "'";
				Cell target = (Cell) pg.getSource();
				logger.logIssue(LogMgmt.TAG_XLSX, LogMgmt.LEV_ERR, target, errMsg, null, null,
						DefaultXmlBuilder.moduleId);
				return;

			}
			termEl.setAttribute("termName", tName);
			builder.addToPedigree(termEl, pg);
			transactionEl.addContent(termEl);
		}

		/*
		 * Now look for Terms specified via other columns....
		 */
		/* add Terms that are for v1.7 */
		addTerm(transactionEl, prefix + "SuppressionLiftDate", "SuppressionLiftDate", "Event");
		addTerm(transactionEl, prefix + "AnnounceDate", "AnnounceDate", "Event");
		addTerm(transactionEl, prefix + "SpecialPreOrderFulfillDate", "PreOrderFulfillDate", "Event");
		addTerm(transactionEl, prefix + "SRP", "SRP", "Money");
		addTerm(transactionEl, prefix + "RentalDuration", "RentalDuration", "Duration");
		addTerm(transactionEl, prefix + "WatchDuration", "WatchDuration", "Duration");
		addTerm(transactionEl, prefix + "FixedEndDate", "FixedEndDate", "Event");
		/* Now add Terms that are new for v1.7.3 */
		addTerm(transactionEl, prefix + "Download", "Download", "Text");
		addTerm(transactionEl, prefix + "Exclusive", "Exclusive", "Boolean");
		addTerm(transactionEl, prefix + "ExclusiveAttributes", "ExclusiveAttributes", "Text");
		addTerm(transactionEl, prefix + "BrandingRights", "BrandingRights", "Boolean");
		addTerm(transactionEl, prefix + "BrandingRightsAttributes", "BrandingRightsAttributes", "Text");
		addTerm(transactionEl, prefix + "TitleStatus", "TitleStatus", "Text");
		/* Now add Terms that are new for v1.8 */
		addTerm(transactionEl, "AvailTrans/Bonus", "Bonus", "Text");
		addTerm(transactionEl, "AvailAsset/PackageLabel", "PackageLabel", "Text");
		/* Now add Terms that are new for v1.9 */
		addTerm(transactionEl, "AvailTrans/CampaignID", "CampaignID", "ID");
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
			logger.logIssue(LogMgmt.TAG_XLSX, LogMgmt.LEV_ERR, target, errMsg, null, null, DefaultXmlBuilder.moduleId);
			return null;
		}
		return pg;
	}

	protected Element addTerm(Element parent, String src, String termName, String subElName) {
		Pedigree pg = getPedigreedData(src);
		if ((pg != null) && (Pedigree.isSpecified(pg.getRawValue()))) {
			Element termEl = new Element("Term", builder.getAvailsNSpace());
			termEl.setAttribute("termName", termName);
			Element childEl = builder.mGenericElement(subElName, pg.getRawValue(), builder.getAvailsNSpace());
			termEl.addContent(childEl);
			builder.addToPedigree(childEl, pg);
			builder.addToPedigree(termEl, pg);
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
		if (Pedigree.isSpecified(pg)) {
			Element eidEl = new Element("EcosystemID", builder.getAvailsNSpace());
			eidEl.setText(pg.getRawValue());
			Element avail = builder.getAvailElement(this);
			builder.addEntitlement(avail, ecosysId, eidEl);
		}
	}

	public void addRegion(Element parentEl, String regionType, Namespace ns, String cellKey) {
		addRegion(parentEl, regionType, ns, cellKey, null);
	}

	protected void addRegion(Element parentEl, String regionType, Namespace ns, String cellKey, String separator) {
		Pedigree pg = getPedigreedData(cellKey);
		String rawValue = pg.getRawValue();
		Element countryEl = null;
		if (Pedigree.isSpecified(rawValue)) {
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
					countryEl = builder.mGenericElement("countryRegion", code, builder.getMdNSpace());
					// process(regionEl, "countryRegion", xb.getMdNSpace(),
					// cellKey);
				} else {
					countryEl = builder.mGenericElement("country", code, builder.getMdNSpace());
					// countryEl = process(regionEl, "country",
					// xb.getMdNSpace(), cellKey);
				}
				if (countryEl != null) {
					regionEl.addContent(countryEl);
					parentEl.addContent(regionEl);
					builder.addToPedigree(countryEl, pg);
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
		if (Pedigree.isSpecified(value)) {
			Element condEl = null;
			// does it start with 'yyyy' ?
			if (value.matches("^[\\d]{4}-.*")) {
				condEl = builder.mGenericElement(childName, value, ns);
			} else {
				condEl = builder.mGenericElement(childName + "Condition", value, ns);
			}
			parentEl.addContent(condEl);
			builder.addToPedigree(condEl, pg);
			return condEl;
		} else {
			return null;
		}
	}

}
