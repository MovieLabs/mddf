/**
 * Copyright (c) 2017 MovieLabs

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
import org.jdom2.Element;
import org.jdom2.Namespace;

import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;

/**
 * Create XML document from an Excel spreadsheet. The XML generated will be
 * based on the matching version of the Avails XSD and reflects a "best effort"
 * in that there is no guarantee that it is valid.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public abstract class AbstractRowHelper {

	static final String MISSING = "--FUBAR (missing)";
	protected Row row;
	protected XmlBuilder xb;
	protected AvailsSheet sheet;
	protected String workType = "";
	protected DataFormatter dataF = new DataFormatter();
	protected Pedigree workTypePedigree;
	protected LogMgmt logger;

	public static AbstractRowHelper createHelper(AvailsSheet aSheet, Row row, LogMgmt logger) {
		Version ver = aSheet.getVersion();
		switch (ver) {
		case V1_7_3:
			return new RowToXmlHelperV1_7_3(aSheet, row, logger);
		case V1_7_2:
		case V1_7:
			return new RowToXmlHelperV1_7(aSheet, row, logger);
		default:
			return null;
		}
	}

	/**
	 * @param logger
	 * @param fields
	 */
	AbstractRowHelper(AvailsSheet sheet, Row row, LogMgmt logger) {
		super();
		this.logger = logger;
		this.sheet = sheet;
		this.row = row;
		/*
		 * Need to save the current workType for use in Transaction/Terms
		 */
		workTypePedigree = getPedigreedData("AvailAsset/WorkType");
		this.workType = workTypePedigree.getRawValue();
	}

	abstract protected Element mDisposition();

	/**
	 * Create an <tt>mdmec:Publisher-type</tt> XML element with a md:DisplayName
	 * element child, and populate the latter with the DisplayName
	 * 
	 * @param elName
	 *            the parent element to be created (i.e., Licensor or
	 *            ServiceProvider)
	 * @param colKey
	 *            the name to be held in the DisplayName child node
	 * @return the created element
	 */
	abstract protected Element mPublisher(String elName, String colKey);

	/**
	 * Invoked by XmlBuilder.createAsset() when a pre-existing Asset element
	 * does not exist.
	 *  
	 * @return the created <tt>&lt;avails:Asset&gt;</tt> element
	 */
	abstract protected Element buildAsset();

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

	abstract protected void addRegion(Element parentEl, String regionType, Namespace ns, String cellKey);

	abstract protected String getData(String colKey);

	/**
	 * @param colKey
	 * @return
	 */
	public Pedigree getPedigreedData(String colKey) {
		int cellIdx = sheet.getColumnIdx(colKey);
		if (cellIdx < 0) {
			return null;
		}
		Cell sourceCell = row.getCell(cellIdx);
		String value = dataF.formatCellValue(sourceCell);
		if (value == null) {
			value = "";
		}
		usesFormula(sourceCell);
		Pedigree ped = new Pedigree(sourceCell, value);

		return ped;
	}

	/**
	 * Return <tt>true</tt> if the cell value is the result of an Excel formula.
	 * Use of formulas may prevent the use of automated workflows for ingesting
	 * and processing the Avails.
	 * 
	 * @param sourceCell
	 * @return
	 */
	protected boolean usesFormula(Cell sourceCell) {
		if (sourceCell != null && (sourceCell.getCellType() == Cell.CELL_TYPE_FORMULA)) {
			String errMsg = "Use of Excel Formulas not supported";
			String details = "Use of formulas may prevent the use of automated workflows for ingesting and processing the Avails.";
			logger.logIssue(LogMgmt.TAG_XLSX, LogMgmt.LEV_ERR, sourceCell, errMsg, details, null, XmlBuilder.moduleId);
			return true;
		} else {
			return false;
		}
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
	public static boolean isSpecified(Object valueSrc) throws IllegalArgumentException {
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

	int getRowNumber() {
		return row.getRowNum();
	}

	/**
	 * @param xmlBuilder
	 */
	abstract protected void makeAvail(XmlBuilder xmlBuilder);

}
