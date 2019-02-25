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
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.poi.POIXMLException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdom2.Document;

import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.Translator;

/**
 * Wrapper for an Excel spreadsheet file comprising multiple individual sheets,
 * each of which are represented by an AvailsSheet object. The
 * <tt>AvailsWrkBook</tt> class should be used to wrap pre-existing Avails XLSX
 * files. This is in contrast to the <tt>TemplateWorkBook</tt> class that is
 * used when programmatically constructing an Avails (e.g. when converting an
 * XML-formatted Avails to the XLSX format).
 * 
 * 
 */
public class AvailsWrkBook {
	protected static String logMsgSrcId = "AvailsWrkBook";
	private File file;
	private ArrayList<AvailsSheet> sheets;
	private LogMgmt logger;
	private boolean exitOnError;
	private boolean cleanupData;
	private XSSFWorkbook wrkBook;

	/**
	 * Convert an AVAIL file in spreadsheet (i.e., xlsx) format to an XML file. If
	 * <tt>inStream</tt> is <tt>null</tt> the <tt>xlsxFile</tt> parameter is used to
	 * read a file accessible via the local system. Otherwise the contents of the
	 * <tt>inStream</tt> is used.
	 * <p>
	 * The result <tt>Map</tt> that is returned will contain:
	 * </p>
	 * <ul>
	 * <li><tt>xlsx</tt>: the xlsx File that was passed as the input argument</li>
	 * <li><tt>xml</tt>: the JDOM2 Document that was created from the xlsx</li>
	 * <li><tt>pedigree</tt>: the <tt>Pedigree</tt> map that was created by the
	 * <tt>XmlBuilder</tt> during the conversion process.</li>
	 * <li><tt>srcFmt</tt>: the <tt>MddfContect.FILE_FMT</tt> of the ingested file.
	 * </ul>
	 * 
	 * @param xslxFile
	 * @param targetVersion
	 * @param inStream
	 * @param logMgr
	 * @return a Map&lt;String, Object&gt;
	 */
	public static Map<String, Object> convertSpreadsheet(File xslxFile, Version targetVersion, InputStream inStream,
			LogMgmt logMgr) {
		boolean autoCorrect = false;
		boolean exitOnError = false;
		AvailsWrkBook ss;
		try {
			if (inStream == null) {
				inStream = new FileInputStream(xslxFile);
			}
			ss = new AvailsWrkBook(inStream, xslxFile, logMgr, exitOnError, autoCorrect);
		} catch (FileNotFoundException e1) {
			logMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, "File not found", xslxFile, logMsgSrcId);
			return null;
		} catch (POIXMLException e1) {
			logMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL,
					"Unable to parse XLSX. Check for comments or embedded objects.", xslxFile, logMsgSrcId);
			return null;
		} catch (IOException e1) {
			logMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, "IO Exception when accessing file", xslxFile, logMsgSrcId);
			return null;
		}
		int sheetNum = 0; // KLUDGE for now
		AvailsSheet as;
		try {
			as = ss.ingestSheet(sheetNum);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		Version unknown = AvailsSheet.Version.valueOf("UNK");
		if (targetVersion == null) {
			targetVersion = unknown;
		}
		// does 'targetVersion' match the inferred version?
		Version inferredVer = as.getVersion();
		if (!inferredVer.equals(unknown)) {
			if (targetVersion.equals(unknown)) {
				// use inferred
				targetVersion = inferredVer;
			} else if (!targetVersion.equals(inferredVer)) {
				// ERROR
				String msg = "XLSX was identified as using " + targetVersion.name() + " but appears to be "
						+ inferredVer.name();
				logMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, msg, xslxFile, logMsgSrcId);
				return null;
			}
		}
		// if still UNKNOWN we can't proceed
		if (targetVersion.equals(unknown)) { 
			String msg = "Avails schema version MUST be provided for this file";
			logMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, msg, xslxFile, logMsgSrcId);
			return null;
		}

		FILE_FMT srcMddfFmt = null;
		FILE_FMT targetMddfFmt = null;
		XmlBuilder xBuilder = new XmlBuilder(logMgr, targetVersion);
		switch (targetVersion) {
		case V1_7_3:
			srcMddfFmt = FILE_FMT.AVAILS_1_7_3;
			targetMddfFmt = FILE_FMT.AVAILS_2_3;
			xBuilder.setVersion("2.3");
			break;
		case V1_7_2:
			srcMddfFmt = FILE_FMT.AVAILS_1_7_2;
			targetMddfFmt = FILE_FMT.AVAILS_2_2_2;
			xBuilder.setVersion("2.2.2");
			break;
		case V1_7:
			srcMddfFmt = FILE_FMT.AVAILS_1_7;
			targetMddfFmt = FILE_FMT.AVAILS_2_2;
			xBuilder.setVersion("2.2");
			break;
		case V1_6:
			logMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL,
					"Version " + targetVersion + " has been deprecated and is no longer supported", xslxFile,
					logMsgSrcId);
			return null;
		case UNK:
			logMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, "Unable to identify XLSX format ", xslxFile, logMsgSrcId);
			break;
		default:
			logMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, "Unsupported template version " + targetVersion, xslxFile,
					logMsgSrcId);
			return null;
		}
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, "Ingesting XLSX in " + targetVersion + " format", xslxFile,
				logMsgSrcId);
		String inFileName = xslxFile.getName();
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
		String shortDesc = String.format("generated XML from %s:Sheet_%s on %s", inFileName, sheetNum, timeStamp);
		try {
			Document xmlJDomDoc = xBuilder.makeXmlAsJDom(as, shortDesc, xslxFile);
			if (xmlJDomDoc == null) {
				// Ingest failed
				return null;
			}
			Translator.addXlateHistory(xmlJDomDoc, srcMddfFmt, targetMddfFmt);
			Map<Object, Pedigree> pedigreeMap = xBuilder.getPedigreeMap();
			Map<String, Object> results = new HashMap<String, Object>();
			results.put("xlsx", xslxFile);
			results.put("xml", xmlJDomDoc);
			results.put("pedigree", pedigreeMap);
			results.put("srcFmt", srcMddfFmt);

			return results;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Compress an Avails XLSX file by hiding empty columns, then save the resulting
	 * file in the designated location. Compression mechanism does not require the
	 * file to be validated first.
	 * <p>
	 * <b>NOTE:</b> This method is only usable when the Avails file is readable
	 * from, and writable to, the local file system.
	 * </p>
	 * <p>
	 * <b>NOTE:</b> This method is intended for use with an MDDF Avails file and
	 * therefore assumes the workbook contains only a single sheet.
	 * </p>
	 * 
	 * @param srcFile
	 * @param outputDir
	 * @param outFileName
	 * @return
	 */
	public static boolean compress(File srcFile, String outputDir, String outFileName) {
		try {
			XSSFWorkbook srcWrkBook = compress(new FileInputStream(srcFile));
			if (srcWrkBook == null) {
				return false;
			}
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
	 * Read an XLSX file from the designated input stream, then return a compressed
	 * version hiding empty columns.
	 * <p>
	 * <b>NOTE:</b> This method is intended for use with an MDDF Avails file and
	 * therefore assumes the workbook contains only a single sheet.
	 * </p>
	 * 
	 * @param inStream
	 * @return
	 * @throws IOException
	 */
	public static XSSFWorkbook compress(InputStream inStream) throws IOException {
		XSSFWorkbook srcWrkBook;
		srcWrkBook = new XSSFWorkbook(inStream);
		XSSFSheet excelSheet = srcWrkBook.getSheetAt(0);
		AvailsSheet.compress(excelSheet);
		return srcWrkBook;
	}

	/**
	 * Create a Spreadsheet object from an <tt>InputStream</tt>. The <tt>File</tt>
	 * parameter is used strictly for logging (i.e., as a source of identifying
	 * metadata).
	 * 
	 * @param inStream
	 * @param file        name of the Excel Spreadsheet file
	 * @param logger      a log4j logger object
	 * @param exitOnError true if validation errors should cause immediate failure
	 * @param cleanupData true if minor validation errors should be auto-corrected
	 * @throws IOException
	 */
	public AvailsWrkBook(InputStream inStream, File file, LogMgmt logger, boolean exitOnError, boolean cleanupData)
			throws IOException {
		this.file = file;
		this.logger = logger;
		this.exitOnError = exitOnError;
		this.cleanupData = cleanupData;
		sheets = new ArrayList<AvailsSheet>();
		wrkBook = new XSSFWorkbook(inStream);
	}

	/**
	 * Create a Spreadsheet object from a File accessible via the local file system.
	 * 
	 * @param file        name of the Excel Spreadsheet file
	 * @param logger      a log4j logger object
	 * @param exitOnError true if validation errors should cause immediate failure
	 * @param cleanupData true if minor validation errors should be auto-corrected
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws InvalidFormatException
	 */
	public AvailsWrkBook(File file, LogMgmt logger, boolean exitOnError, boolean cleanupData)
			throws FileNotFoundException, IOException, POIXMLException, InvalidFormatException {
		this(new FileInputStream(file), file, logger, exitOnError, cleanupData);
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
	 * <i>wrap</i> the Excel sheet in an instance of the <tt>AvailsSheet</tt> class
	 * and include it in the scope of the AvailsWrkBook object.
	 * 
	 * @param sheetNumber zero-based index of sheet to add
	 * @return created AvailsSheet object
	 * @throws IllegalArgumentException if the sheet does not exist in the Excel
	 *                                  spreadsheet
	 * @throws Exception                other error conditions may also throw
	 *                                  exceptions
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
	LogMgmt getLogger() {
		return logger;
	}

	/**
	 * @return the file
	 */
	File getFile() {
		return file;
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
	 * @param sheetName name of the sheet to dump
	 * @throws Exception if any error is encountered (e.g. non-existant or corrupt
	 *                   file)
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
	 * @param file name of the Excel .xlsx spreadsheet
	 * @throws Exception if any error is encountered (e.g. non-existant or corrupt
	 *                   file)
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
