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
package com.movielabs.mddflib.status.offer;

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
public class RowIngester implements RowDataSrc {

	protected LogMgmt logger;
	protected StreamingOStatusBuilder builder;
	protected Pedigree workTypePedigree;
	protected String workType;
	protected String[] row;
	protected Integer rowNum;

	RowIngester(String[] row, int rowNum, StreamingOStatusBuilder builder, LogMgmt logger) {
		super();
		this.row = row;
		this.rowNum = rowNum;
		this.logger = logger;
		this.builder = builder;
		makeOStatus();
	}

	private void makeOStatus() {
		/*
		 * Need to save the current workType for use in identifying whether a
		 * FeatureStatus or BonusStatus is to be generated.
		 */
		workTypePedigree = getPedigreedData("AvailAsset/WorkType");
		this.workType = workTypePedigree.getRawValue();

		Element avail = builder.getOStatusElement(this);

		Element e = createTransaction();
		// Transaction
		if (e != null) {
			builder.addTransaction(avail, e);
		}

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

//	abstract protected Element mDisposition();

	/**
	 * Create an <tt>mdmec:Publisher-type</tt> XML element with a md:DisplayName
	 * element child, and populate the latter with the DisplayName
	 * 
	 * @param elName the parent element to be created (i.e., Licensor or
	 *               ServiceProvider)
	 * @param colKey the name to be held in the DisplayName child node
	 * @return the created element
	 */
	protected Element mPublisher(String elName, String colKey) {
		Element pubEl = new Element(elName, builder.getAvailsNSpace());
		process(pubEl, "DisplayName", builder.getMdNSpace(), colKey);
		return pubEl;
	}

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
//	abstract protected Element buildAsset();

	/**
	 * Create a Transaction Element. While the Avail XSD allows multiple
	 * Transactions per Avail, the XLSX mechanism only allows a single Transaction
	 * element to be defined <i>per-row</i>.
	 * 
	 * @return
	 */
	protected Element createTransaction() {
		Element transactionEl = new Element("Transaction", builder.getAvailsNSpace());
		processTransactionBody(transactionEl);
		return transactionEl;
	}

	/**
	 * populate a Transaction element
	 * 
	 * @param transactionEl
	 */
	protected void processTransactionBody(Element transactionEl) {
		/*
		 * TransactionID is OPTIONAL. For mystical reasons lost in the mists of time, it
		 * come from the 'AvailID' column.
		 */

		process(transactionEl, "TransactionID", builder.getAvailsNSpace(), "Avail/AvailID");

		String prefix = "AvailTrans/";
		addRegion(transactionEl, "Territory", builder.getAvailsNSpace(), prefix + "Territory");
		addRegion(transactionEl, "TerritoryExcluded", builder.getAvailsNSpace(), prefix + "TerritoryExclusion");
		process(transactionEl, "FormatProfile", builder.getAvailsNSpace(), prefix + "FormatProfile");

		Element statusEl = builder.buildStatusEl("FeatureStatus", getData("Status/StatusProgressCode"), this);
		transactionEl.addContent(statusEl);

		/*
		 * Add a BonusStatus IFF a StatusBonusProgressCode was specified.
		 * 
		 */
		String pCode = getData("Status/StatusBonusProgressCode");
		if ((pCode != null) && (pCode.length() > 0)) {
			Element bStatusEl = builder.buildStatusEl("BonusStatus", pCode, this);
			transactionEl.addContent(bStatusEl);
		}
		process(transactionEl, "PlatformLRD", builder.getAvailsNSpace(), "Status/StatusLRD");
		process(transactionEl, "OfferURL", builder.getAvailsNSpace(), "Status/StatusOfferURL");

		addAllTerms(transactionEl);

		process(transactionEl, "Comments", builder.getAvailsNSpace(), "Status/StatusComments");
		process(transactionEl, "ExceptionsFlag", builder.getAvailsNSpace(), "Avail/ExceptionFlag");

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
