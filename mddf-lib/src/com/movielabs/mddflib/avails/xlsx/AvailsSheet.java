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

package com.movielabs.mddflib.avails.xlsx;

import java.util.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Represents an individual sheet of an Excel spreadsheet
 */
public class AvailsSheet {
	public static enum Version {
		V1_7, V1_6, UNK
	};

	private ArrayList<Row> rows;
	private AvailsWrkBook parent;
	private String name;
	private ArrayList<String> headerList;
	private HashMap<String, Integer> headerMap;
	private org.apache.logging.log4j.Logger logger;
	private Sheet excelSheet;
	private Version version = Version.UNK;
	private boolean noPrefix = true;
	private boolean isForTV;

	/**
	 * Create an object representing a single sheet of a spreadsheet
	 * 
	 * @param parent
	 *            the parent Spreadsheet object
	 * @param name
	 *            the name of the spreadsheet
	 */
	public AvailsSheet(AvailsWrkBook parent, Sheet excelSheet) {
		this.parent = parent;
		this.excelSheet = excelSheet;
		logger = parent.getLogger();
		this.name = excelSheet.getSheetName();
		rows = new ArrayList<Row>();
		ingest(excelSheet);
	}

	/**
	 * Convert a POI sheet from an Excel spreadsheet to an Avails spreadsheet
	 * object.
	 * 
	 * @param excelSheet
	 *            an Apache POI sheet object
	 * @return created sheet object
	 */
	private void ingest(Sheet excelSheet) {
		DataFormatter dataF = new DataFormatter();
		/*
		 * The spread sheet may be formatted with either one or two rows of
		 * column headers. First step is determine which is the case.
		 */
		Row headerRow1 = excelSheet.getRow(0);
		Row headerRow2 = excelSheet.getRow(1);
		if (headerRow2.getPhysicalNumberOfCells() < 1) {
			return;
		}
		Iterator<Cell> cellIt = headerRow1.cellIterator();
		boolean use2 = false;
		while (cellIt.hasNext() && !use2) {
			Cell next = cellIt.next();
			String value = dataF.formatCellValue(next);
			if (value.equalsIgnoreCase("AvailTrans")) {
				use2 = true;
			}
		}
		Row headerRow;
		if (use2) {
			logger.debug("XSLT Column headers in ROW 2");
			headerRow = headerRow2;
		} else {
			logger.debug("XSLT Column headers in ROW 1");
			headerRow = headerRow1;
		}
		// ................
		headerList = new ArrayList<String>();
		headerMap = new HashMap<String, Integer>();
		for (Cell headerCell : headerRow) {
			int idx = headerCell.getColumnIndex();
			String value = dataF.formatCellValue(headerCell);
			if ((value != null) && !value.isEmpty()) {
				String key = value;
				headerList.add(key);
				headerMap.put(key, new Integer(idx));
			}
		}
		logger.debug("Found " + headerList.size() + " defined columns");

		/*
		 * TYPE Check: Is this for movies or TV? The current rule is that this
		 * is defined by the name of the worksheet.
		 */
		switch (name) {
		case "TV":
			isForTV = true;
			break;
		case "Movies":
			isForTV = false;
			break;
		default:
			logger.fatal("Unrecognized sheet name: Must be 'TV' or 'Movies'");
			return;
		}

		// VERSION check and support..
		/*
		 * There is no explicit identification in a spreadsheet of the template
		 * version being used. Instead we need to infer based on what the column
		 * headers are.
		 * 
		 */
		if (this.getColumnIdx("Avail/ALID") >= 0) {
			version = Version.V1_7;
		} else if ((this.getColumnIdx("Avail/AltID") >= 0) || (this.getColumnIdx("Avail/EpisodeAltID") >= 0)) {
			version = Version.V1_6;
		}
		// ...............................................
		/*
		 * Skip over the header rows and process all data rows...
		 */
		for (int idxR = 2; idxR <= excelSheet.getLastRowNum(); idxR++) {
			Row nextRow = excelSheet.getRow(idxR);
			if (nextRow == null) {
				break;
			}
			if (isAvail(nextRow)) {
				rows.add(nextRow);
			}
		}
	}

	/**
	 * @return
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * @return the isForTV
	 */
	public boolean isForTV() {
		return isForTV;
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
	public ArrayList<Row> getRows() {
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
		if (noPrefix) {
			String[] parts = key.split("/");
			key = parts[1];
		}
		Integer colIdx = headerMap.get(key);
		if (colIdx == null) {
			return -1;
		}
		return colIdx.intValue();
	}

	/**
	 * @param columnID
	 * @param row
	 * @return
	 */
	public Cell getCell(String columnKey, int row) {
		if (row >= rows.size()) {
			return null;
		}
		int idx = getColumnIdx(columnKey);
		if (idx < 0) {
			return null;
		} else {
			return excelSheet.getRow(row).getCell(idx);
		}
	}

	/**
	 * get the parent Spreadsheet object for this sheet
	 * 
	 * @return the parent of this sheet
	 */
	public AvailsWrkBook getAvailSS() {
		return parent;
	}

	/**
	 * 
	 */
	public void dump() {
		DataFormatter dataF = new DataFormatter();
		for (Row nextRow : rows) {
			int rNum = nextRow.getRowNum() + 1;
			System.out.print("row " + rNum + "=[");
			for (int cNum = 0; cNum < headerMap.size(); cNum++) {
				Cell nextCell = nextRow.getCell(cNum);
				System.out.print("|" + dataF.formatCellValue(nextCell));
			}
			System.out.println("]");
		}

	}
}
