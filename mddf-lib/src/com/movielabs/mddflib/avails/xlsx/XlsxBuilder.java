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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.movielabs.mddflib.avails.xlsx.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.XmlIngester;

import net.sf.json.JSONObject;

/**
 * Build an XLSX representation of an Avails that has been specified as an XML
 * document.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class XlsxBuilder {
	private static JSONObject mappings;
	protected XPathFactory xpfac = XPathFactory.instance();
	private LogMgmt logger;
	private Version xlsxVersion;
	private String rootPrefix = "avails:";

	private int logMsgDefaultTag = LogMgmt.TAG_AVAIL;
	protected String logMsgSrcId = "XlsxBuilder";
	private String MD_VER;
	private String MDMEC_VER;
	private Namespace mdNSpace;
	private String AVAIL_VER;
	private Namespace availsNSpace;
	private Element rootEl;
	private ArrayList<Element> tvAvailsList;
	private ArrayList<Element> movieAvailsList;
	private XSSFWorkbook workbook;
	private JSONObject mappingVersion;
	private String availPrefix;
	private String mdPrefix;
	private Map<String, XSSFCellStyle> headerColors = new HashMap<String, XSSFCellStyle>();
	private XSSFCellStyle defaultStyle;

	static {
		/*
		 * Load JSON file with the mapping (i.e., specification of how specific
		 * xlsx columns are populated from the XML
		 */
		try {
			InputStream inp = XlsxBuilder.class.getResourceAsStream("Mappings.json");
			InputStreamReader isr = new InputStreamReader(inp, "UTF-8");
			BufferedReader reader = new BufferedReader(isr);
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			String input = builder.toString();
			mappings = JSONObject.fromObject(input);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public XlsxBuilder(Element docRootEl, Version xlsxVersion, LogMgmt logger) {
		this.logger = logger;
		this.xlsxVersion = xlsxVersion;
		mappingVersion = mappings.getJSONObject(xlsxVersion.name());
		rootEl = docRootEl;

		String schemaVer = XmlIngester.identifyXsdVersion(rootEl);
		logger.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Using Schema Version " + schemaVer, null, logMsgSrcId);
		setXmlVersion(schemaVer);
		availPrefix = availsNSpace.getPrefix() + ":";
		mdPrefix = mdNSpace.getPrefix() + ":";
		sortAvails();
		initializeWorkbook();
		addMovieAvails();
		addTvAvails();
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
		headerStyle1.setFillPattern(CellStyle.SOLID_FOREGROUND);
		headerStyle1.setAlignment(CellStyle.ALIGN_CENTER);
		headerColors.put("Avail", headerStyle1);
		defaultStyle = headerStyle1;

		XSSFCellStyle headerStyle2 = workbook.createCellStyle();
		headerStyle2.setFont(font);
		XSSFColor c2 = new XSSFColor();
		c2.setARGBHex("B54E9B");
		headerStyle2.setFillForegroundColor(c2);
		headerStyle2.setFillPattern(CellStyle.SOLID_FOREGROUND);
		headerStyle2.setAlignment(CellStyle.ALIGN_CENTER);
		headerColors.put("AvailAsset", headerStyle2);

		XSSFCellStyle headerStyle3 = workbook.createCellStyle();
		headerStyle3.setFont(font);
		XSSFColor c3 = new XSSFColor();
		c3.setARGBHex("38761d");
		headerStyle3.setFillForegroundColor(c3);
		headerStyle3.setFillPattern(CellStyle.SOLID_FOREGROUND);
		headerStyle3.setAlignment(CellStyle.ALIGN_CENTER);
		headerColors.put("AvailMetadata", headerStyle3);

		XSSFCellStyle headerStyle4 = workbook.createCellStyle();
		headerStyle4.setFont(font);
		XSSFColor c4 = new XSSFColor();
		c4.setARGBHex("85200c");
		headerStyle4.setFillForegroundColor(c4);
		headerStyle4.setFillPattern(CellStyle.SOLID_FOREGROUND);
		headerStyle4.setAlignment(CellStyle.ALIGN_CENTER);
		headerColors.put("AvailTrans", headerStyle4);
	}

	/**
	 * 
	 */
	private void addTvAvails() {
		if (tvAvailsList.isEmpty()) {
			return;
		}
		XSSFSheet sheet = workbook.createSheet("TV");
		// get mappings that will be used for this specific sheet..
		JSONObject mappingDefs = mappingVersion.getJSONObject("TV");
		List<String> colIdList = new ArrayList<String>();
		colIdList.addAll(mappingDefs.keySet());
		/*
		 * First add TV-specific headers matching the template version being
		 * used
		 */
		addHeaderRows(sheet, colIdList);

		/* Initialize xpaths that implement the data mappings */
		Map<String, Map<String, XPathExpression>> xpathSets = initializeMappings(mappingDefs);

		/*
		 * XLSX format will only support TV avails of type 'episode' or
		 * 'season'. Anything else will have been filtered out when the Avails
		 * were sorted.
		 */
		for (int i = 0; i < tvAvailsList.size(); i++) {
			Element availEl = tvAvailsList.get(i);
			/*
			 * one row is added for each combination of Asset and Transaction
			 * included in the Avail which means there are usually multiple rows
			 * for each Avail. Start by getting the info that will be common to
			 * each row.
			 */
			Map<String, String> commonData = new HashMap<String, String>();
			Map<String, XPathExpression> availMappings = xpathSets.get("Avail");
			Iterator<String> colIt = availMappings.keySet().iterator();
			while (colIt.hasNext()) {
				String colId = colIt.next();
				XPathExpression xpe = availMappings.get(colId);
				if (xpe != null) {
					Element targetEl = (Element) xpe.evaluateFirst(availEl);
					if (targetEl != null) {
						String value = targetEl.getTextNormalize();
						commonData.put(colId, value);
					}
				}
			}
			/*
			 * now identify each Asset that is a child of this Avail and prepare
			 * its data.
			 */
			List<Element> assetList = availEl.getChildren("Asset", availsNSpace);
			List<Map<String, String>> perAssetData = new ArrayList<Map<String, String>>(assetList.size());
			for (int j = 0; j < assetList.size(); j++) {
				Element assetEl = assetList.get(j);
				Map<String, String> assetData = extractAssetData(assetEl, "AvailAsset", xpathSets);
				assetData.putAll(commonData);
				Map<String, String> assetMetadataData = extractAssetData(assetEl, "AvailMetadata", xpathSets);
				assetData.putAll(assetMetadataData);
				// now save it
				perAssetData.add(assetData);
			}
			/*
			 * now identify each Transaction that is a child of this Avail and
			 * prepare its data.
			 */
			List<Element> transList = availEl.getChildren("Transaction", availsNSpace);
			List<Map<String, String>> perTransData = new ArrayList<Map<String, String>>(transList.size());
			for (int j = 0; j < transList.size(); j++) {
				Element transEl = transList.get(j);
				Map<String, String> transData = extractTransData(transEl, xpathSets.get("AvailTrans"));
				// now save it
				perTransData.add(transData);
			}

			/* Now add 1 row for each unique combo of Asset and Transaction */
			for (int aIdx = 0; aIdx < perAssetData.size(); aIdx++) {
				Map<String, String> assetData = perAssetData.get(aIdx);
				for (int tIdx = 0; tIdx < perTransData.size(); tIdx++) {
					Map<String, String> rowData = perTransData.get(tIdx);
					rowData.putAll(assetData);
					addRow(colIdList, rowData, sheet);
				}
			}
		}

	}

	/**
	 * @param transEl
	 * @param xpathSets
	 * @return
	 */
	private Map<String, String> extractTransData(Element transEl, Map<String, XPathExpression> mappings) {
		Map<String, String> transData = new HashMap<String, String>();
		Iterator<String> keyIt = mappings.keySet().iterator();
		while (keyIt.hasNext()) {
			String mappingKey = keyIt.next();
			XPathExpression xpe = mappings.get(mappingKey);
			if (xpe != null) {
				Element targetEl = (Element) xpe.evaluateFirst(transEl);
				if (targetEl != null) {
					String value = targetEl.getTextNormalize();
					transData.put(mappingKey, value);
				}
			}
		}
		return transData;
	}

	/**
	 * @param assetEl
	 * @param xpathSets
	 * @return
	 */
	private Map<String, String> extractAssetData(Element assetEl, String category,
			Map<String, Map<String, XPathExpression>> xpathSets) {
		Map<String, String> assetData = new HashMap<String, String>();
		Map<String, XPathExpression> assetMappings = xpathSets.get(category);
		/*
		 * Mappings for Assets are in some cases dependent on the WorkType so
		 * 1st step is get that value
		 */
		String workType = assetEl.getChildTextNormalize("WorkType", availsNSpace);
		/* Now continue with everything else */
		Iterator<String> keyIt = assetMappings.keySet().iterator();
		while (keyIt.hasNext()) {
			String mappingKey = keyIt.next();
			XPathExpression xpe = null;
			if (mappingKey.contains("#")) {
				String[] parts = mappingKey.split("#");
				if (parts[1].equals(workType)) {
					xpe = assetMappings.get(mappingKey);
					mappingKey = parts[0];
				}
			} else {
				xpe = assetMappings.get(mappingKey);
			}
			if (xpe != null) {
				Element targetEl = (Element) xpe.evaluateFirst(assetEl);
				if (targetEl != null) {
					String value = targetEl.getTextNormalize();
					assetData.put(mappingKey, value);
					// System.out.println("mKey " + mappingKey + "='" + value +
					// "'");
				}
			}
		}
		return assetData;
	}

	/**
	 * @param colIdList
	 * @param cellData
	 */
	private void addRow(List<String> colIdList, Map<String, String> cellData, XSSFSheet sheet) {
		int rowCount = sheet.getLastRowNum();
		Row row = sheet.createRow(rowCount + 1);
		for (int i = 0; i < colIdList.size(); i++) {
			String colTag = colIdList.get(i);
			String cellValue = cellData.get(colTag);
			if ((cellValue != null) && !cellValue.isEmpty()) {
				Cell cell = row.createCell(i);
				cell.setCellValue(cellValue);
			}
		}
	}

	/**
	 * Group, filter, and re-format the XML-to-XSLX mappings to facilitate later
	 * usage.
	 * 
	 * @param mappingDefs
	 * @return
	 */
	private Map<String, Map<String, XPathExpression>> initializeMappings(JSONObject mappingDefs) {
		Map<String, Map<String, XPathExpression>> organizedMappings = new HashMap<String, Map<String, XPathExpression>>();
		List<String> colIdList = new ArrayList<String>();
		colIdList.addAll(mappingDefs.keySet());
		// .....................................
		/* AVAIL-related mappings.... */
		Map<String, XPathExpression> availMappings = new HashMap<String, XPathExpression>();
		for (int j = 0; j < colIdList.size(); j++) {
			String colKey = colIdList.get(j);
			if (colKey.startsWith("Avail:")) {
				String mapping = mappingDefs.optString(colKey, "foo");
				availMappings.put(colKey, createXPath(mapping));
			}
		}
		/*
		 * Special Case: an Avail-related column that doesn't start with
		 * 'Avail:'
		 */
		String colKey = "Disposition:EntryType";
		String mapping = mappingDefs.optString(colKey, "foo");
		availMappings.put(colKey, createXPath(mapping));
		organizedMappings.put("Avail", availMappings);
		// ..........................................
		/*
		 * Asset and Metadata mappings are more complex in that they **may** be
		 * dependent on the Asseet's WorkType.
		 */
		// ..........................................
		Map<String, XPathExpression> assetMappings = new HashMap<String, XPathExpression>();
		for (int j = 0; j < colIdList.size(); j++) {
			colKey = colIdList.get(j);
			if (colKey.startsWith("AvailAsset:")) {
				Object value = mappingDefs.opt(colKey);
				if (value instanceof String) {
					mapping = (String) value;
					assetMappings.put(colKey, createXPath(mapping));
				} else if (value instanceof JSONObject) {
					JSONObject mappingSet = (JSONObject) value;
					Iterator<String> typeIt = mappingSet.keySet().iterator();
					while (typeIt.hasNext()) {
						String nextType = typeIt.next();
						mapping = mappingSet.optString(nextType, "foo");
						assetMappings.put(colKey + "#" + nextType, createXPath(mapping));
					}
				}
			}
		}
		organizedMappings.put("AvailAsset", assetMappings);
		// .....................................
		Map<String, XPathExpression> metadataMappings = new HashMap<String, XPathExpression>();
		for (int j = 0; j < colIdList.size(); j++) {
			colKey = colIdList.get(j);
			if (colKey.startsWith("AvailMetadata:")) {
				Object value = mappingDefs.opt(colKey);
				if (value instanceof String) {
					mapping = (String) value;
					metadataMappings.put(colKey, createXPath(mapping));
				} else if (value instanceof JSONObject) {
					JSONObject mappingSet = (JSONObject) value;
					Iterator<String> typeIt = mappingSet.keySet().iterator();
					while (typeIt.hasNext()) {
						String nextType = typeIt.next();
						mapping = mappingSet.optString(nextType, "foo");
						metadataMappings.put(colKey + "#" + nextType, createXPath(mapping));
					}
				}
			}
		}
		organizedMappings.put("AvailMetadata", metadataMappings);
		// .....................................
		Map<String, XPathExpression> transMappings = new HashMap<String, XPathExpression>();
		return organizedMappings;
	}

	private XPathExpression createXPath(String mapping) {
		if (mapping.equals("foo")) {
			return null;
		}
		/*
		 * replace namespace placeholders with actual prefix being used
		 */
		String t1 = mapping.replaceAll("\\{avail\\}", availPrefix);
		String t2 = t1.replaceAll("\\{md\\}", mdPrefix);
		// Now format an XPath
		String xpath = "./" + t2;
		XPathExpression<Element> xpExpression = xpfac.compile(xpath, Filters.element(), null, availsNSpace, mdNSpace);
		return xpExpression;
	}

	/**
	 * 
	 */
	private void addMovieAvails() {
		if (movieAvailsList.isEmpty()) {
			return;
		}
		XSSFSheet sheet = workbook.createSheet("Movie");
	}

	/**
	 * Sort the Avails defined in the XML into two sets: one for movies and one
	 * for TV. Avails that have a type not supported by the XLSX version of
	 * Avails will be ignored.
	 */
	private void sortAvails() {
		movieAvailsList = new ArrayList<Element>();
		tvAvailsList = new ArrayList<Element>();
		XPathExpression<Element> xpExp01 = xpfac.compile(".//" + rootPrefix + "Avail/" + rootPrefix + "AvailType",
				Filters.element(), null, availsNSpace);
		List<Element> atElList = xpExp01.evaluate(rootEl);
		for (int i = 0; i < atElList.size(); i++) {
			Element typeEl = atElList.get(i);
			Element availEl = typeEl.getParentElement();
			String aType = typeEl.getTextNormalize();
			switch (aType) {
			case "single":
			case "short":
			case "collection":
				movieAvailsList.add(availEl);
				break;
			case "episode":
			case "season":
				tvAvailsList.add(availEl);
				break;
			default:
				logger.log(LogMgmt.LEV_WARN, logMsgDefaultTag,
						"Avails with AvailType='" + aType + "' are not supported by the Excel format", null,
						logMsgSrcId);
			}
		}
		logger.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Movie Avails count=" + movieAvailsList.size(), null,
				logMsgSrcId);

		logger.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "TV Avails count=" + tvAvailsList.size(), null, logMsgSrcId);
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
		}

	}

	/**
	 * Configure all XML-related functions to work with the specified version of
	 * the Avails XSD. This includes setting the correct version of the Common
	 * Metadata and MDMEC XSD that are used with the specified Avails version.
	 * If the <tt>availSchemaVer</tt> is not supported by the current version of
	 * <tt>mddf-lib</tt> an <tt>IllegalArgumentException</tt> will be thrown.
	 * 
	 * @param availSchemaVer
	 * @param availNsPrefix
	 * @throws IllegalArgumentException
	 */
	public void setXmlVersion(String availSchemaVer) throws IllegalArgumentException {
		switch (availSchemaVer) {
		case "2.2.1":
			MD_VER = "2.5";
			MDMEC_VER = "2.5";
			break;
		case "2.2":
		case "2.1":
			MD_VER = "2.4";
			MDMEC_VER = "2.4";
			break;
		default:
			throw new IllegalArgumentException("Unsupported Avails Schema version " + availSchemaVer);
		}
		AVAIL_VER = availSchemaVer;
		Namespace.getNamespace("md", "http://www.movielabs.com/schema/mdmec/v" + MDMEC_VER + "/mdmec");
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + MD_VER + "/md");
		availsNSpace = Namespace.getNamespace("avails",
				"http://www.movielabs.com/schema/avails/v" + AVAIL_VER + "/avails");
	}

	/**
	 * @param destPath
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void export(String destPath) throws FileNotFoundException, IOException {
		/* First adjust column widths */
		Sheet sheetTV = workbook.getSheet("TV");
		if (sheetTV != null) {
			int colCount = mappingVersion.getJSONObject("TV").size();
			for (int i = 0; i < colCount; i++) {
				sheetTV.autoSizeColumn(i);
			}
		}

		try (FileOutputStream outputStream = new FileOutputStream(destPath)) {
			workbook.write(outputStream);
			logger.log(LogMgmt.LEV_INFO, logMsgDefaultTag, "XLSX saved to " + destPath, null, logMsgSrcId);

		}

	}
}
