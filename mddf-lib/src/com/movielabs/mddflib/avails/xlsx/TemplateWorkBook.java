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
package com.movielabs.mddflib.avails.xlsx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.movielabs.mddflib.avails.xml.AbstractRowHelper;
import com.movielabs.mddflib.avails.xml.AvailsSheet;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.avails.xml.AvailsWrkBook;
import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.logging.LogMgmt;

import net.sf.json.JSONObject;

/**
 * Wrapper for an Excel-formatted Avails that is initially empty. The
 * <tt>TemplateWorkBook</tt> class is used when programmatically constructing an
 * Avails (e.g. when converting an XML-formatted Avails to the XLSX format).
 * This is in contrast to the <tt>AvailsWrkBook</tt> class that is used to wrap
 * pre-existing Avails XLSX files.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class TemplateWorkBook {
	/**
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	protected class SheetProperties {

		XSSFSheet sheet;
		List<String> colIdList;
		boolean[] isEmptyCol;

		/**
		 * @param sheet
		 * @param colIdList
		 */
		public SheetProperties(XSSFSheet sheet, List<String> colIdList) {
			this.sheet = sheet;
			this.colIdList = colIdList;
			/*
			 * init data structure used to identify empty columns
			 */
			isEmptyCol = new boolean[colIdList.size()];
			for (int i = 0; i < colIdList.size(); i++) {
				isEmptyCol[i] = true;
			}
		}

	}

	private LogMgmt logger;
	private int logMsgDefaultTag = LogMgmt.TAG_XLSX;
	protected static String logMsgSrcId = "TemplateWorkBook";
	private XSSFWorkbook workbook;
	private Map<String, XSSFCellStyle> headerColors = new HashMap<String, XSSFCellStyle>();
	private XSSFCellStyle defaultStyle;
	private XSSFCellStyle headerStyleFill;
	private Map<XSSFSheet, SheetProperties> sheetData = new HashMap<XSSFSheet, SheetProperties>();

	/**
	 * Create a clean copy of an Avails workbook. The copy will have the same
	 * data but
	 * <ol>
	 * <li>columns will be re-ordered and grouped</li>
	 * <li>styles will match the template styles (e/g/ colors of column titles)
	 * </li>
	 * <li>empty columns will be hidden</li>
	 * </ol>
	 * 
	 * @param srcFile
	 * @param outputDir
	 * @param outFileName
	 * @param log
	 * @return
	 */
	public static boolean clone(File srcFile, String outputDir, String outFileName, LogMgmt log) {
		try {
			AvailsWrkBook srcWrkBook = new AvailsWrkBook(srcFile, log, true, false);
			AvailsSheet srcSheet = srcWrkBook.ingestSheet(0);

			TemplateWorkBook clone = new TemplateWorkBook(log);
			/*
			 * column headers are derived from the keys used in the XML-to-XLSX
			 * mappingDefs. The defs are version and category dependent.
			 * 
			 */
			Version ver = srcSheet.getVersion();
			JSONObject mappingsByVersion = XlsxBuilder.mappings.getJSONObject(ver.name());
			String category = srcSheet.getName();
			JSONObject mappingDefs = mappingsByVersion.getJSONObject(category);
			ArrayList<String> colIdList = new ArrayList<String>();
			colIdList.addAll(mappingDefs.keySet());
			XSSFSheet clonedSheet = clone.addSheet(category, colIdList);
			// now we can copy the rows.

			rowLoop: for (Row row : srcSheet.getRows()) {
				AbstractRowHelper rowHelper = AbstractRowHelper.createHelper(srcSheet, row);
				if (rowHelper != null) {
					clone.addDataRow(rowHelper, clonedSheet);
				} else {
					log.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, "Unsupported XLSX version", srcFile, logMsgSrcId);
					break rowLoop;
				}
			}
			File outFile = new File(outputDir, outFileName);
			clone.export(outFile.getCanonicalPath());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * @param logger
	 */
	public TemplateWorkBook(LogMgmt logger) {
		super();
		this.logger = logger;
		initializeWorkbook();
	}

	/**
	 * Initialize workbook styles to match as closely as possible the 'template'
	 * spreadsheets.
	 */
	private void initializeWorkbook() {
		workbook = new XSSFWorkbook();
		/* Initialize any styles used to make output more readable */
		XSSFFont font = workbook.createFont();
		font.setBold(true);
		font.setFontHeightInPoints((short) 8);
		XSSFColor hdrFontColor = new XSSFColor();
		hdrFontColor.setARGBHex("FFFFFF");
		font.setColor(hdrFontColor);
		XSSFCellStyle headerStyle1 = workbook.createCellStyle();
		headerStyle1.setFont(font);
		XSSFColor c1 = new XSSFColor();
		c1.setARGBHex("3776DB");
		headerStyle1.setFillForegroundColor(c1);
		headerStyle1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle1.setAlignment(HorizontalAlignment.CENTER);
		headerColors.put("Avail", headerStyle1);
		defaultStyle = headerStyle1;

		XSSFCellStyle headerStyle2 = workbook.createCellStyle();
		headerStyle2.setFont(font);
		XSSFColor c2 = new XSSFColor();
		c2.setARGBHex("B54E9B");
		headerStyle2.setFillForegroundColor(c2);
		headerStyle2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle2.setAlignment(HorizontalAlignment.CENTER);
		headerColors.put("AvailAsset", headerStyle2);

		XSSFCellStyle headerStyle3 = workbook.createCellStyle();
		headerStyle3.setFont(font);
		XSSFColor c3 = new XSSFColor();
		c3.setARGBHex("38761d");
		headerStyle3.setFillForegroundColor(c3);
		headerStyle3.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle3.setAlignment(HorizontalAlignment.CENTER);
		headerColors.put("AvailMetadata", headerStyle3);

		XSSFCellStyle headerStyle4 = workbook.createCellStyle();
		headerStyle4.setFont(font);
		XSSFColor c4 = new XSSFColor();
		c4.setARGBHex("85200c");
		headerStyle4.setFillForegroundColor(c4);
		headerStyle4.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle4.setAlignment(HorizontalAlignment.CENTER);
		headerColors.put("AvailTrans", headerStyle4);

		headerStyleFill = workbook.createCellStyle();
		headerStyleFill.setFont(font);
		XSSFColor c5 = new XSSFColor();
		c5.setARGBHex("0c0c0c");
		headerStyleFill.setFillForegroundColor(c5);
		headerStyleFill.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyleFill.setAlignment(HorizontalAlignment.CENTER);
	}

	public XSSFSheet addSheet(String name, List<String> colIdList) {
		XSSFSheet sheet = workbook.createSheet(name);
		SheetProperties sheetProps = new SheetProperties(sheet, colIdList);
		sheetData.put(sheet, sheetProps);
		addHeaderRows(sheet, colIdList);
		return sheet;
	}

	/**
	 * Add header row(s) that conform to the specified version of the Avails
	 * XLSX template.
	 * 
	 * @param sheet
	 */
	private void addHeaderRows(XSSFSheet sheet, List<String> colIdList) {
		Row row1 = sheet.createRow(0);
		Row row2 = sheet.createRow(1);
		// need to add an empty row cause spec sez Avails start on Row 4 :(
		Row row3 = sheet.createRow(2);
		for (int i = 0; i < colIdList.size(); i++) {
			String colTag = colIdList.get(i);
			String[] part = colTag.split(":");
			Cell cell1 = row1.createCell(i);
			cell1.setCellValue(part[0]);
			Cell cell2 = row2.createCell(i);
			cell2.setCellValue(part[1]);
			/* add styling to make it more readable */
			XSSFCellStyle headerStyle = headerColors.get(part[0]);
			if (headerStyle == null) {
				headerStyle = defaultStyle;
			}
			cell1.setCellStyle(headerStyle);
			cell2.setCellStyle(headerStyle);
			// empty header cell..
			Cell cell3 = row3.createCell(i);
			cell3.setCellStyle(headerStyleFill);
		}
	}

	/**
	 * @param cellData
	 * @param sheet
	 */
	public void addDataRow(Map<String, String> cellData, XSSFSheet sheet) {
		SheetProperties sProps = sheetData.get(sheet);
		boolean[] isEmptyCol = sProps.isEmptyCol;
		List<String> colIdList = sProps.colIdList;
		int rowCount = sheet.getLastRowNum();
		Row row = sheet.createRow(rowCount + 1);
		for (int i = 0; i < colIdList.size(); i++) {
			String colTag = colIdList.get(i);
			String cellValue = cellData.get(colTag);
			if ((cellValue != null) && !cellValue.isEmpty()) {
				Cell cell = row.createCell(i);
				cell.setCellValue(cellValue);
				isEmptyCol[i] = false;
			}
		}
	}

	/**
	 * Copy a row from a source spreadsheet into the template's sheet.
	 * 
	 * @param srcRow
	 * @param destSheet
	 */
	public void addDataRow(AbstractRowHelper srcRow, XSSFSheet destSheet) {
		SheetProperties sProps = sheetData.get(destSheet);
		boolean[] isEmptyCol = sProps.isEmptyCol;
		List<String> colIdList = sProps.colIdList;
		int rowCount = destSheet.getLastRowNum();
		Row row = destSheet.createRow(rowCount + 1);
		for (int i = 0; i < colIdList.size(); i++) {
			String colTag = colIdList.get(i);
			/*
			 * The colIdList use ":" as a separator (e.g. "AvailTrans:Start")
			 * while the 'getPedigreedData()' expects the keys to use "/" (e.g.
			 * "AvailTrans:/Start")
			 */
			colTag = colTag.replaceFirst(":", "/");
			Pedigree pg = srcRow.getPedigreedData(colTag);
			if (AbstractRowHelper.isSpecified(pg)) {
				Cell cell = row.createCell(i);
				cell.setCellValue(pg.getRawValue());
				isEmptyCol[i] = false;
			}
		}

	}

	/**
	 * @param destPath
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void export(String destPath) throws FileNotFoundException, IOException {
		/* hide empty columns */
		hideEmptyColumns();
		/* adjust column widths */
		int sheetCnt = workbook.getNumberOfSheets();
		for (int i = 0; i < sheetCnt; i++) {
			Sheet sheet = workbook.getSheetAt(i);
			if (sheet != null) {
				SheetProperties sProps = sheetData.get(sheet);
				int colCount = sProps.colIdList.size();
				for (int j = 0; j < colCount; j++) {
					sheet.autoSizeColumn(j);
				}
			}
		}
		try (FileOutputStream outputStream = new FileOutputStream(destPath)) {
			workbook.write(outputStream);
			logger.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "XLSX saved to " + destPath, null, logMsgSrcId);

		}
	}

	private int hideEmptyColumns() {
		int hiddenColCnt = 0;
		int sheetCnt = workbook.getNumberOfSheets();
		for (int i = 0; i < sheetCnt; i++) {
			Sheet sheet = workbook.getSheetAt(i);
			if (sheet != null) {
				SheetProperties sProps = sheetData.get(sheet);
				boolean[] isEmptyCol = sProps.isEmptyCol;
				for (int j = 0; j < isEmptyCol.length; j++) {
					if (isEmptyCol[j]) {
						sheet.setColumnHidden(j, true);
						hiddenColCnt++;
					}
				}
			}
		}
		logger.log(LogMgmt.LEV_INFO, logMsgDefaultTag,
				hiddenColCnt + " empty XLSX columns have been hidden on " + sheetCnt + " worksheets", null,
				logMsgSrcId);
		return hiddenColCnt;
	}
}
