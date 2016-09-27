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

import java.util.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Represents an individual sheet of an Excel spreadsheet
 */
public class AvailsSheet {
	private ArrayList<String[]> rows;
	private AvailSS parent;
	private String name;
	private ArrayList<String> headerList;
	private HashMap<String, Integer> headerMap;
	private org.apache.logging.log4j.Logger logger;

	/**
	 * Create an object representing a single sheet of a spreadsheet
	 * 
	 * @param parent
	 *            the parent Spreadsheet object
	 * @param name
	 *            the name of the spreadsheet
	 */
	public AvailsSheet(AvailSS parent, Sheet excelSheet) {
		this.parent = parent;
		logger = parent.getLogger();
		this.name = excelSheet.getSheetName();
		rows = new ArrayList<String[]>();
		ingest(excelSheet);
	}

	/**
	 * Convert a POI sheet from an Excel spreadsheet to an Avails spreadsheet
	 * object.
	 * 
	 * @param sheet
	 *            an Apache POI sheet object
	 * @return created sheet object
	 */
	private void ingest(Sheet sheet) {
		DataFormatter dataF = new DataFormatter();
		Row headerRow1 = sheet.getRow(0);
		Row headerRow2 = sheet.getRow(1);
		if (headerRow2.getPhysicalNumberOfCells() < 1) {
			return;
		}
		if (headerRow1.getPhysicalNumberOfCells() != headerRow2.getPhysicalNumberOfCells()) {
			String msg = "Sheet " + sheet.getSheetName() + ": : Invalid column headers";
			logger.error(msg);
			return;
		}
		headerList = new ArrayList<String>();
		headerMap = new HashMap<String, Integer>();
		for (Cell secondaryCell : headerRow2) {
			int idx = secondaryCell.getColumnIndex();
			Cell primaryCell = headerRow1.getCell(idx);
			String value1 = dataF.formatCellValue(primaryCell);
			String value2 = dataF.formatCellValue(secondaryCell);
			if ((value1 != null) && !value1.isEmpty() && (value2 != null) && !value2.isEmpty()) {
				String key = value1 + "/" + value2;
				headerList.add(key);
				headerMap.put(key, new Integer(idx));
			}
		} 
		// ...............................................
		/*
		 * Skip over the header rows and process all data rows...
		 */
		for (int idxR = 2; idxR < sheet.getLastRowNum(); idxR++) {
			Row nextRow = sheet.getRow(idxR);
			if (isAvail(nextRow)) {
				String[] fields = new String[headerList.size()];
				for (int i = 0; i < headerList.size(); i++) {
					Cell cell = nextRow.getCell(i);
					int idx = cell.getColumnIndex();
					String value = dataF.formatCellValue(cell);
					if (value == null) {
						fields[idx] = "";
					} else {
						fields[idx] = value;
					}
				}
				rows.add(fields);
			}
		}
	}

	/**
	 * Determine if a spreadsheet row contains an avail
	 * 
	 * @param nextRow
	 * @return true iff the row is an avail based on the contents of the
	 *         Territory column
	 */
	private boolean isAvail(Row nextRow) {
		/*
		 * Use 1st cell to determine if this is an Avails row. Other
		 * possibilities are and empty row or a comment row, both of which
		 * should be skipped.
		 */
		Cell firstCell = nextRow.getCell(0);
		DataFormatter dataF = new DataFormatter();
		String firstText = dataF.formatCellValue(firstCell);
		if (firstText == null || (firstText.isEmpty()) || (firstText.startsWith("//"))) {
			return false;
		} else {
			return true;
		}
	}

	public String getName() {
		return name;
	}

	/**
	 * Get a an array of objects representing each row of this sheet
	 * 
	 * @return an array containing all the SheetRow objects in this sheet
	 */
	public ArrayList<String[]> getRows() {
		return rows;
	}

	/**
	 * @return
	 */
	public int getRowCount() { 
		return rows.size();
	}

	/**
	 * Returns the (zero-based) column number that matches the key. The key is a
	 * composite of the two header columns (e.g., "AvailTrans/Territory")
	 * 
	 * @param key
	 * @return column number or -1 if key does not match a know column header.
	 */
	public int getColumnIdx(String key) {
		Integer colIdx = headerMap.get(key);
		if (colIdx == null) {
			return -1;
		}
		return colIdx.intValue();
	}

	/**
	 * Return value of cell identified by the columnKey and row. If either
	 * argument is invalid a <tt>null</tt> value is returned.
	 * 
	 * @param columnKey
	 * @param row
	 *            zero-based row number
	 * @return
	 */
	public String getColumnData(String columnKey, int row) {
		if (row >= rows.size()) {
			return null;
		}
		int idx = getColumnIdx(columnKey);
		if (idx < 0) {
			return null;
		} else {
			String[] fields = rows.get(row);
			return fields[idx];
		}
	}

	/**
	 * get the parent Spreadsheet object for this sheet
	 * 
	 * @return the parent of this sheet
	 */
	public AvailSS getAvailSS() {
		return parent;
	}

	/**
	 * 
	 */
	public void dump() {
		int i = 1;
		for (String[] sr : getRows()) {
			System.out.print("row " + i++ + "=[");
			for (String cell : sr) {
				System.out.print("|" + cell);
			}
			System.out.println("]");
		}

	}
}
