/**
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

package com.movielabs.mddflib.avails.xml;

import java.util.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.movielabs.mddflib.logging.LogMgmt;

/**
 * Wrapper class for an individual sheet of an Excel spreadsheet that has been
 * fully populated with Avails data.
 */
public class AvailsSheet {
	protected static String logMsgSrcId = "AvailsSheet";
	public static enum Version {
		V1_7_2, V1_7, V1_6, UNK
	};

	private ArrayList<Row> rows;
	private AvailsWrkBook parent;
	private String name;
	private ArrayList<String> headerList;
	private HashMap<String, Integer> headerMap;
	private LogMgmt logger;
	private Sheet excelSheet;
	private Version version = Version.UNK;
	private boolean noPrefix = true;
	private boolean isForTV;

	/**
	 * Create an object representing a single sheet of an Avails spreadsheet.
	 * The spread sheet:
	 * <ul>
	 * <li>MUST be formatted with two rows of column headers.</li>
	 * <li>MUST match one of the supported template versions.</li>
	 * <li>MUST be named either 'TV' or 'Movies'.</li>
	 * </ul>
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
		 * The spread sheet MUST be formatted with two rows of column headers.
		 */
		Row headerRow1 = excelSheet.getRow(0);
		Row headerRow2 = excelSheet.getRow(1);
		if (headerRow2.getPhysicalNumberOfCells() < 1) {
			return;
		}
		// ................
		headerList = new ArrayList<String>();
		headerMap = new HashMap<String, Integer>();
		for (Cell headerCell : headerRow2) {
			int idx = headerCell.getColumnIndex();
			String value = dataF.formatCellValue(headerCell);
			if ((value != null) && !value.isEmpty()) {
				String prefix;
				if (noPrefix) {
					prefix = "";
				} else {
					prefix = dataF.formatCellValue(headerRow1.getCell(idx)) + "/";
				}
				String key = prefix + value;
				headerList.add(key);
				headerMap.put(key, new Integer(idx));
			}
		}
		logger.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_AVAIL, "Found " + headerList.size() + " defined columns", parent.getFile(), logMsgSrcId); 

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
			logger.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, "Unrecognized sheet name: Must be 'TV' or 'Movies'", parent.getFile(), logMsgSrcId); 
			return;
		}

		// VERSION check and support..
		identifyVersion();
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

	private void identifyVersion() {
		/*
		 * There is no explicit identification in a spreadsheet of the template
		 * version being used. Instead we need to infer based on what the column
		 * headers are.
		 * 
		 */
		boolean hasAltID = (this.getColumnIdx("AvailAsset/AltID") >= 0)
				|| (this.getColumnIdx("AvailMetadata/EpisodeAltID") >= 0);
		boolean hasALID = (this.getColumnIdx("Avail/ALID") >= 0);
		if (hasALID && hasAltID) {
			version = Version.V1_7_2;
		} else if (hasALID && !hasAltID) {
			version = Version.V1_7;
		} else if (!hasALID && hasAltID) {
			version = Version.V1_6;
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

	public static void compress(Sheet ssheet) {
		Row headerRow1 = ssheet.getRow(0);
		Row headerRow2 = ssheet.getRow(1);
		int colCount = headerRow1.getPhysicalNumberOfCells();
		boolean[] isEmptyCol = new boolean[colCount];
		for (int i = 0; i < colCount; i++) {
			isEmptyCol[i] = true;
		}
		int emptyColCount = colCount;
		DataFormatter dataF = new DataFormatter();
		/*
		 * Skip over the header rows and process all data rows...
		 */
		checkLoop: for (int idxR = 2; idxR <= ssheet.getLastRowNum(); idxR++) {
			Row nextRow = ssheet.getRow(idxR);
			for (int idxC = 0; idxC < colCount; idxC++) {
				// only check a column if it still shows as empty
				if (isEmptyCol[idxC]) {
					Cell nextCell = nextRow.getCell(idxC);
					String value = dataF.formatCellValue(nextCell);
					isEmptyCol[idxC] = value.isEmpty();
					if (!isEmptyCol[idxC]) {
						// col that was empty is now flagged as non-empty
						emptyColCount--;
						if (emptyColCount == 0) {
							// no need to continue. All columns have data.
							break checkLoop;
						}
					}
				}
			}
		}
		// now hide the empty ones.
		for (int j = 0; j < isEmptyCol.length; j++) {
			if (isEmptyCol[j]) {
				ssheet.setColumnHidden(j, true);
			}
		}
		// // autosize the columns
		// for (int j = 0; j < colCount; j++) {
		// ssheet.autoSizeColumn(j);
		// }
	}

	/**
	 * Determine if either one or two rows of column headers is being used and
	 * return the identified <tt>Row</tt>. Returns <tt>null</tt> if the first
	 * row is empty.
	 * 
	 * @param ssheet
	 * @return
	 */
	public static Row getHeaderRow(Sheet ssheet) {
		Row headerRow1 = ssheet.getRow(0);
		Row headerRow2 = ssheet.getRow(1);
		if (headerRow1.getPhysicalNumberOfCells() < 1) {
			return null;
		}
		Iterator<Cell> cellIt = headerRow1.cellIterator();
		boolean use2 = false;
		DataFormatter dataF = new DataFormatter();
		while (cellIt.hasNext() && !use2) {
			Cell next = cellIt.next();
			String value = dataF.formatCellValue(next);
			if (value.equalsIgnoreCase("AvailTrans")) {
				use2 = true;
			}
		}
		Row headerRow;
		if (use2) {
			headerRow = headerRow2;
		} else {
			headerRow = headerRow1;
		}
		return headerRow;
	}

}
