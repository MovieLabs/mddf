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
 */

package com.movielabs.mddflib.avails.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.apache.logging.log4j.*;
import org.apache.poi.POIXMLException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Wrapper for an Excel spreadsheet comprising multiple individual sheets, each
 * of which are represented by an AvailsSheet object.
 */
public class AvailsWrkBook {
	private File file;
	private ArrayList<AvailsSheet> sheets;
	private Logger logger;
	private boolean exitOnError;
	private boolean cleanupData;
	private XSSFWorkbook wrkBook;

	/**
	 * Compress an Avails XLSX file by hiding empty columns. Compression
	 * mechanism does not require the file to be validated first.
	 * 
	 * @param srcFile
	 * @param outputDir
	 * @param outFileName
	 * @return
	 */
	public static boolean compress(File srcFile, String outputDir, String outFileName) {
		try {
			XSSFWorkbook srcWrkBook = new XSSFWorkbook(new FileInputStream(srcFile));
			XSSFSheet excelSheet = srcWrkBook.getSheetAt(0);
			AvailsSheet.compress(excelSheet);
			File outFile = new File(outputDir, outFileName);
			FileOutputStream outputStream = new FileOutputStream(outFile);
			srcWrkBook.write(outputStream);
			srcWrkBook.close();
			outputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Create a Spreadsheet object
	 * 
	 * @param file
	 *            name of the Excel Spreadsheet file
	 * @param logger
	 *            a log4j logger object
	 * @param exitOnError
	 *            true if validation errors should cause immediate failure
	 * @param cleanupData
	 *            true if minor validation errors should be auto-corrected
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws InvalidFormatException
	 */
	public AvailsWrkBook(File file, Logger logger, boolean exitOnError, boolean cleanupData)
			throws FileNotFoundException, IOException, POIXMLException, InvalidFormatException {
		this.file = file;
		this.logger = logger;
		this.exitOnError = exitOnError;
		this.cleanupData = cleanupData;
		sheets = new ArrayList<AvailsSheet>();
		wrkBook = new XSSFWorkbook(file);
	}

	public AvailsSheet ingestSheet(String sheetName) throws Exception {
		Sheet excelSheet = wrkBook.getSheet(sheetName);
		if (excelSheet == null) {
			wrkBook.close();
			throw new IllegalArgumentException(file + ":" + sheetName + " not found");
		}
		AvailsSheet as = new AvailsSheet(this, excelSheet);
		wrkBook.close();
		return as;
	}

	/**
	 * Ingest a sheet from the Excel spreadsheet. The ingest process will
	 * <i>wrap</i> the Excel sheet in an instance of the <tt>AvailsSheet</tt>
	 * class and include it in the scope of the AvailsWrkBook object.
	 * 
	 * @param sheetNumber
	 *            zero-based index of sheet to add
	 * @return created AvailsSheet object
	 * @throws IllegalArgumentException
	 *             if the sheet does not exist in the Excel spreadsheet
	 * @throws Exception
	 *             other error conditions may also throw exceptions
	 */
	public AvailsSheet ingestSheet(int sheetNumber) throws Exception {
		Sheet excelSheet;
		try {
			excelSheet = wrkBook.getSheetAt(sheetNumber);
		} catch (IllegalArgumentException e) {
			wrkBook.close();
			throw new IllegalArgumentException(file + ": sheet number " + sheetNumber + " not found");
		}
		AvailsSheet as = new AvailsSheet(this, excelSheet);
		wrkBook.close();
		return as;
	}

	/**
	 * Get the logging object
	 * 
	 * @return Logger for this instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Get the error handling option
	 * 
	 * @return true if exiting on encountering an invalid cell
	 */
	public boolean getExitOnError() {
		return exitOnError;
	}

	/**
	 * Get the data cleaning option
	 * 
	 * @return true minor validation errors will be fixed up
	 */
	public boolean getCleanupData() {
		return cleanupData;
	}

	/**
	 * Dump raw contents of specified sheet
	 * 
	 * @param sheetName
	 *            name of the sheet to dump
	 * @throws Exception
	 *             if any error is encountered (e.g. non-existant or corrupt
	 *             file)
	 */
	public void dumpSheet(String sheetName) throws Exception {
		boolean foundSheet = false;
		for (AvailsSheet s : sheets) {
			if (s.getName().equals(sheetName)) {
				int i = 0;
				foundSheet = true;
				s.dump();
			}
		}
		if (!foundSheet)
			throw new IllegalArgumentException(file + ":" + sheetName + " not found");
	}

	/**
	 * Dump the contents (sheet-by-sheet) of an Excel spreadsheet
	 * 
	 * @param file
	 *            name of the Excel .xlsx spreadsheet
	 * @throws Exception
	 *             if any error is encountered (e.g. non-existant or corrupt
	 *             file)
	 */
	public static void dumpFile(String file) throws Exception {
		Workbook wb = new XSSFWorkbook(new FileInputStream(file));
		for (int i = 0; i < wb.getNumberOfSheets(); i++) {
			Sheet sheet = wb.getSheetAt(i);
			System.out.println("Sheet <" + wb.getSheetName(i) + ">");
			for (Row row : sheet) {
				System.out.println("rownum: " + row.getRowNum());
				for (Cell cell : row) {
					System.out.println("   | " + cell.toString());
				}
			}
		}
		wb.close();
	}
}
