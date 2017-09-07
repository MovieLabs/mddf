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

	/**
	 * @param fields
	 */
	AbstractRowHelper(AvailsSheet sheet, Row row) {
		super();
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
	 * @param displayName
	 *            the name to be held in the DisplayName child node
	 * @return the created element
	 */
	abstract protected Element mPublisher(String elName, String colKey);

	/**
	 * Invoked by XmlBuilder.createAsset() when a pre-existing Asset element
	 * does not exist.
	 * 
	 * @param workTypePedigree
	 * @return
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
	abstract protected Element[] process(Element parentEl, String childName, Namespace ns, String cellKey,
			String separator);

	abstract protected void addRegion(Element parentEl, String regionType, Namespace ns, String cellKey);

	abstract protected String getData(String colKey);

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

	int getRowNumber() {
		return row.getRowNum();
	}

}
