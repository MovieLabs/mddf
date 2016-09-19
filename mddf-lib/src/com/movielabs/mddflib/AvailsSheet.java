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
import java.text.ParseException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		System.out.println(headerList);
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
	 * Add a row of spreadsheet data
	 * 
	 * @param fields
	 *            an array containing the raw values of a spreadsheet row
	 * @param rowNum
	 *            the row number from the source spreadsheet
	 * @throws Exception
	 *             if an invalid workType is encountered (currently, only
	 *             'movie', 'episode', or 'season' are accepted)
	 */
	private void addRow(String[] fields, int rowNum) {
		int cIdx = getColumnIdx("AvailAsset/WorkType");
		String workType = fields[cIdx];

		System.out.println("workType=" + workType);

		if (!(workType.equals("Movie") || workType.equals("Episode") || workType.equals("Season"))) {
			if (parent.getCleanupData()) {
				Pattern pat = Pattern.compile("^\\s*(movie|episode|season)\\s*$", Pattern.CASE_INSENSITIVE);
				Matcher m = pat.matcher(workType);
				if (m.matches()) {
					log("corrected from '" + workType + "'", rowNum);
					workType = m.group(1).substring(0, 1).toUpperCase() + m.group(1).substring(1).toLowerCase();
				} else {
					log("invalid workType: '" + workType + "'", rowNum);
					return;
				}
			} else {
				log("invalid workType: '" + workType + "'", rowNum);
				return;
			}
		}
		SheetRow sr;
		switch (workType) {
		case "Movie":
			sr = new Movie(this, "Movie", rowNum, fields);
			break;
		case "Episode":
			sr = new Episode(this, "Episode", rowNum, fields);
			break;
		case "Season":
			sr = new Season(this, "Season", rowNum, fields);
			break;
		default:
			log("invalid workType: " + workType, rowNum);
			return;
		}
		// rows.add(sr);
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
	 * helper routine to create a log entry
	 * 
	 * @param msg
	 *            the data to be logged
	 * @param bail
	 *            if true, throw a ParseException after logging the message
	 * @throws ParseException
	 *             if bail is true
	 */
	private void log(String msg, int rowNum) {
		String logEntry = String.format("Sheet %s Row %5d: %s", name, rowNum, msg);
		parent.getLogger().warn(logEntry);
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
	 * @param key
	 * @param row
	 *            zero-based row number
	 * @return
	 */
	public String getColumnData(String key, int row) {
		if (row >= rows.size()) {
			return null;
		}
		int idx = getColumnIdx(key);
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
