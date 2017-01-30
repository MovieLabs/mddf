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
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddflib.avails.xlsx.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.XmlIngester;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * <tt>XlsxBuilder</tt> creates an XLSX representation of an Avails that has
 * been specified as an XML document.
 * 
 * <h3>Usage</h3>
 * <p>
 * Invoking the constructor will result in the immediate generation of a XLSX
 * workbook. The workbook will contain a separate worksheet for TV avails and
 * movie avails. The <tt>export()</tt> method can then be used to write the
 * worksheet to a file.
 * </p>
 * <h3>Output</h3>
 * <p>
 * The generated worksheets will be based on the
 * <a href="http://www.movielabs.com/md/avails/">Avails spreadsheet template for
 * Film and TV</a>. The specific version of the template that will be used may
 * be specified when invoking the constructor.
 * </p>
 * <p>
 * An Avails Excel workbook is expected to contain a single spreadsheet with
 * <i>either</i> move avails <i>or</i>TV avails. In contrast, a single Avails
 * XML document may contain a mix of both TV and movie avails. An
 * <tt>XlsxBuilder</tt> will deal with this by generating from each XML file a
 * single Excel file with two separate worksheets: one for any movie avails
 * contained in the XML and another for any TV avails. With the exception of
 * including more than one sheet in a single workbook, the output of the
 * <tt>XlsxBuilder</tt> will fully conform to the version of the Avails
 * spreadsheet template that is being used.
 * </p>
 * 
 * <h3>Limitations</h3>
 * <p>
 * The Excel version of Avails does not support all of the semantics available
 * via the XML format. As a result, some information contained in an XML Avails
 * document may be lost when converting to the XLSX format. Warning messages
 * will be added to the log output when a situation is encountered that results
 * in a loss of information.
 * </p>
 * 
 * <h3>Version Support</h3>
 * <p>
 * <tt>XlsxBuilder</tt> has the ability to ingest multiple versions of the XML
 * Avails schema and output multiple versions of the XSLX Avails schema. The
 * specifics of how to match between specific XML versions and specific XSLX
 * versions is determined by the contents of the <tt>Mappings.json</tt> file.
 * </p>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class XlsxBuilder {
	private static final String DURATION_REGEX = "P([0-9]+Y)?([0-9]+M)?([0-9]+D)?(T([0-9]+H)?([0-9]+M)?([0-9]+(\\.[0-9]+)?S)?)?";
	private static DecimalFormat durFieldFmt = new DecimalFormat("00");
	private static JSONObject mappings;
	private static Pattern p_xsDuration;
	private static String warnMsg1 = "XLSX xfer dropping additional XYZ values";
	private static String warnDetail1 = "The Excel version of Avails only allows 1 value for this field. Additional XML elements will be ignored";
	protected XPathFactory xpfac = XPathFactory.instance();
	private LogMgmt logger;
	private String rootPrefix = "avails:";

	private int logMsgDefaultTag = LogMgmt.TAG_XLSX;
	protected String logMsgSrcId = "XlsxBuilder";
	private String MD_VER;
	private String MDMEC_VER;
	private Namespace mdNSpace;
	private String AVAIL_VER;
	private Namespace availsNSpace;
	private Element rootEl;
	private ArrayList<Element> tvAvailsList;
	private ArrayList<Element> movieAvailsList;
	private HashSet<XPathExpression<?>> allowsMultiples = new HashSet<XPathExpression<?>>();
	private XSSFWorkbook workbook;
	private JSONObject mappingVersion;
	private String availPrefix;
	private String mdPrefix;
	private Map<String, XSSFCellStyle> headerColors = new HashMap<String, XSSFCellStyle>();
	private XSSFCellStyle defaultStyle;
	private XSSFCellStyle headerStyleFill;

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
		/*
		 * Compile Pattern used to identify an xs:duration value that requires
		 * translation before being added to the spreadsheet.
		 * 
		 */
		p_xsDuration = Pattern.compile(DURATION_REGEX);
	}

	/**
	 * @param docRootEl
	 * @param xlsxVersion
	 * @param logger
	 */
	public XlsxBuilder(Element docRootEl, Version xlsxVersion, LogMgmt logger) {
		this.logger = logger;
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

		headerStyleFill = workbook.createCellStyle();
		headerStyleFill.setFont(font);
		XSSFColor c5 = new XSSFColor();
		c5.setARGBHex("0c0c0c");
		headerStyleFill.setFillForegroundColor(c5);
		headerStyleFill.setFillPattern(CellStyle.SOLID_FOREGROUND);
		headerStyleFill.setAlignment(CellStyle.ALIGN_CENTER);
	}

	/**
	 * 
	 */
	private void addMovieAvails() {
		if (movieAvailsList.isEmpty()) {
			return;
		}
		addAvails("Movie", movieAvailsList);
	}

	/**
	 * 
	 */
	private void addTvAvails() {
		/*
		 * XLSX format will only support TV avails of type 'episode' or
		 * 'season'. Anything else will have been filtered out when the Avails
		 * were sorted.
		 */
		if (tvAvailsList.isEmpty()) {
			return;
		}
		addAvails("TV", tvAvailsList);
	}

	private void addAvails(String category, List<Element> availList) {
		XSSFSheet sheet = workbook.createSheet(category);
		// get mappings that will be used for this specific sheet..
		JSONObject mappingDefs = mappingVersion.getJSONObject(category);
		List<String> colIdList = new ArrayList<String>();
		colIdList.addAll(mappingDefs.keySet());
		/*
		 * First add TV-specific headers matching the template version being
		 * used
		 */
		addHeaderRows(sheet, colIdList);

		/* Initialize xpaths that implement the data mappings */
		Map<String, Map<String, List<XPathExpression>>> xpathSets = initializeMappings(mappingDefs);

		for (int i = 0; i < availList.size(); i++) {
			Element availEl = availList.get(i);
			/*
			 * one row is added for each combination of Asset and Transaction
			 * included in the Avail which means there are usually multiple rows
			 * for each Avail. Start by getting the info that will be common to
			 * each row.
			 */
			Map<String, List<XPathExpression>> availMappings = xpathSets.get("Avail");
			Map<String, String> commonData = extractData(availEl, availMappings, "");
			/*
			 * now identify each Asset that is a child of this Avail and prepare
			 * its data.
			 */
			List<Element> assetList = availEl.getChildren("Asset", availsNSpace);
			Map<String, List<XPathExpression>> assetMappings = xpathSets.get("AvailAsset");
			Map<String, List<XPathExpression>> metadataMappings = xpathSets.get("AvailMetadata");
			List<Map<String, String>> perAssetData = new ArrayList<Map<String, String>>(assetList.size());
			for (int j = 0; j < assetList.size(); j++) {
				Element assetEl = assetList.get(j);
				/*
				 * Mappings for Assets are in some cases dependent on the
				 * WorkType so 1st step is get that value
				 */
				String context = assetEl.getChildTextNormalize("WorkType", availsNSpace);
				Map<String, String> assetData = extractData(assetEl, assetMappings, context);
				assetData.putAll(commonData);
				Map<String, String> assetMetadataData = extractData(assetEl, metadataMappings, context);
				assetData.putAll(assetMetadataData);
				// now save it
				perAssetData.add(assetData);
			}
			/*
			 * now identify each Transaction that is a child of this Avail and
			 * prepare its data.
			 */
			List<Element> transList = availEl.getChildren("Transaction", availsNSpace);
			Map<String, List<XPathExpression>> transMappings = xpathSets.get("AvailTrans");
			List<Map<String, String>> perTransData = new ArrayList<Map<String, String>>(transList.size());
			for (int j = 0; j < transList.size(); j++) {
				Element transEl = transList.get(j);
				Map<String, String> transData = extractData(transEl, transMappings, "");
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
	 * @param baseEl
	 * @param mappings
	 * @param context
	 * @return
	 */
	private Map<String, String> extractData(Element baseEl, Map<String, List<XPathExpression>> mappings,
			String context) {
		Map<String, String> dataMap = new HashMap<String, String>();
		/* Now continue with everything else */
		Iterator<String> keyIt = mappings.keySet().iterator();
		while (keyIt.hasNext()) {
			String mappingKey = keyIt.next();
			List<XPathExpression> xpeList = null;
			if (mappingKey.contains("#")) {
				String[] parts = mappingKey.split("#");
				if (parts[1].equals(context)) {
					xpeList = mappings.get(mappingKey);
					mappingKey = parts[0];
				}
			} else {
				xpeList = mappings.get(mappingKey);
			}
			if (xpeList != null) {
				for (int i = 0; i < xpeList.size(); i++) {
					XPathExpression xpe = xpeList.get(i);
					String value = null;
					if (allowsMultiples.contains(xpe)) {
						value = extractMultiple(xpe, baseEl);
					} else {
						value = extractSingleton(xpe, baseEl, mappingKey);
					}
					if (value != null) {
						dataMap.put(mappingKey, value);
						break;
					}
				}
			}
		}
		return dataMap;
	}

	/**
	 * @param xpe
	 * @param baseEl
	 * @return
	 */
	private String extractMultiple(XPathExpression xpe, Element baseEl) {
		String value = null;
		List targetList = xpe.evaluate(baseEl);
		int matchCnt = 0;
		if (targetList != null && (!targetList.isEmpty())) {
			for (int i = 0; i < targetList.size(); i++) {
				Object target = targetList.get(i);
				String nextValue = null;
				if (target instanceof Element) {
					Element targetEl = (Element) target;
					nextValue = targetEl.getTextNormalize();
				} else if (target instanceof Attribute) {
					Attribute targetAt = (Attribute) target;
					nextValue = targetAt.getValue();
				}
				if (nextValue != null) {
					matchCnt++;
					/*
					 * check for special cases where value has to be translated.
					 * This mainly happens with durations where XSD specifies
					 * xs:duration syntax.
					 */
					nextValue = convertDuration(nextValue);
					if (matchCnt == 1) {
						value = nextValue;
					} else {
						value = value + ", " + nextValue;
					}
				}
			}
		}
		return value;

	}

	/**
	 * Return a data value when only one Element matching an XPath may be used.
	 * 
	 * @param xpe
	 * @param baseEl
	 */
	private String extractSingleton(XPathExpression xpe, Element baseEl, String mappingKey) {
		String value = null;
		List targetList = xpe.evaluate(baseEl);
		int matchCnt = 0;
		if (targetList != null && (!targetList.isEmpty())) {
			for (int i = 0; i < targetList.size(); i++) {
				Object target = targetList.get(i);
				if (target instanceof Element) {
					matchCnt++;
					Element targetEl = (Element) target;
					if (matchCnt == 1) {
						value = targetEl.getTextNormalize();
					} else if (matchCnt > 1) {
						String msg = warnMsg1.replace("XYZ", mappingKey);
						logger.logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_WARN, targetEl, msg, warnDetail1, null,
								logMsgSrcId);
					}
				} else if (target instanceof Attribute) {
					matchCnt++;
					Attribute targetAt = (Attribute) target;
					if (matchCnt == 1) {
						value = targetAt.getValue();
					} else if (matchCnt > 1) {
						Element targetEl = targetAt.getParent();
						String msg = warnMsg1.replace("XYZ", mappingKey);
						logger.logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_WARN, targetEl, msg, warnDetail1, null,
								logMsgSrcId);
					}
				}

			}
			if (value != null) {
				/*
				 * check for special cases where value has to be translated.
				 * This mainly happens with durations where XSD specifies
				 * xs:duration syntax.
				 */
				value = convertDuration(value);
			}
		}
		return value;

	}

	/**
	 * Convert any string formatted in compliance with W3C xs:Duration syntax to
	 * <tt>hh:mm:ss</tt> syntax. Inputs that do not match the xs:duration syntax
	 * will be returned unchanged. After conversion any trailing fields with a
	 * zero value will be dropped (i.e., hh:mm:00s becomes hh:mm). The first
	 * field will always indicate hours, even if it contains a zero value (i.e.,
	 * 00:mm:00s becomes 00:mm).
	 * 
	 * @param input
	 * @return
	 */
	private String convertDuration(String input) {
		Matcher m = p_xsDuration.matcher(input);
		if (!m.matches()) {
			return input;
		}
		String temp1 = input.replaceFirst("P", "");
		String[] parts = temp1.split("T");
		long totalHrs = 0;
		long totalMin = 0;
		long totalSec = 0;
		if (!parts[0].isEmpty()) {
			/* ignore Y and M, and only allow D fields */
			if (parts[0].contains("Y") || parts[0].contains("M")) {
				logger.log(LogMgmt.LEV_WARN, logMsgDefaultTag,
						"Conversion of duration '" + input + "' will ignore YEAR and MONTH fields", null, logMsgSrcId);
			}
			Pattern dp = Pattern.compile("[0-9]+D");
			Matcher dm = dp.matcher(parts[0]);
			if (dm.find()) {
				String dayPart = dm.group();
				totalHrs = totalHrs + Integer.parseInt(dayPart.replace("D", ""));
			}
			// covert accumulated days to hours
			totalHrs = totalHrs * 24;

		}
		if (parts.length > 1 && (!parts[1].isEmpty())) {
			// handle H, M, and S fields
			Pattern dp = Pattern.compile("[0-9]+H");
			Matcher dm = dp.matcher(parts[1]);
			if (dm.find()) {
				String hourPart = dm.group();
				totalHrs = totalHrs + Integer.parseInt(hourPart.replace("H", ""));
			}
			dp = Pattern.compile("[0-9]+M");
			dm = dp.matcher(parts[1]);
			if (dm.find()) {
				String mmPart = dm.group();
				totalMin = Integer.parseInt(mmPart.replace("M", ""));
			}
			dp = Pattern.compile("[0-9]+S");
			dm = dp.matcher(parts[1]);
			if (dm.find()) {
				String ssPart = dm.group();
				totalSec = Integer.parseInt(ssPart.replace("S", ""));
			}

		}
		String hh = Integer.toString((int) totalHrs);
		String mm = Integer.toString((int) totalMin);
		String ss = Integer.toString((int) totalSec);
		String output = durFieldFmt.format(totalHrs);
		if ((totalMin + totalSec) > 0) {
			output = output + ":" + durFieldFmt.format(totalMin);
			if (totalSec > 0) {
				output = output + ":" + durFieldFmt.format(totalSec);
			}
		}
		return output;
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
	private Map<String, Map<String, List<XPathExpression>>> initializeMappings(JSONObject mappingDefs) {
		Map<String, Map<String, List<XPathExpression>>> organizedMappings = new HashMap<String, Map<String, List<XPathExpression>>>();
		List<String> colIdList = new ArrayList<String>();
		colIdList.addAll(mappingDefs.keySet());
		// .....................................
		/* AVAIL-related mappings.... */
		Map<String, List<XPathExpression>> availMappings = initCategoryMappings(mappingDefs, "Avail");
		/*
		 * Special Case: an Avail-related column that doesn't start with
		 * 'Avail:'
		 */
		String colKey = "Disposition:EntryType";
		String mapping = mappingDefs.optString(colKey, "n.a");
		List<XPathExpression> xpeList = new ArrayList<XPathExpression>();
		xpeList.add(createXPath(mapping));
		availMappings.put(colKey, xpeList);
		organizedMappings.put("Avail", availMappings);
		// ..........................................
		Map<String, List<XPathExpression>> assetMappings = initCategoryMappings(mappingDefs, "AvailAsset");
		organizedMappings.put("AvailAsset", assetMappings);
		// .....................................
		Map<String, List<XPathExpression>> metadataMappings = initCategoryMappings(mappingDefs, "AvailMetadata");
		organizedMappings.put("AvailMetadata", metadataMappings);
		// .....................................
		Map<String, List<XPathExpression>> transMappings = initCategoryMappings(mappingDefs, "AvailTrans");
		organizedMappings.put("AvailTrans", transMappings);
		return organizedMappings;
	}

	private Map<String, List<XPathExpression>> initCategoryMappings(JSONObject mappingDefs, String category) {
		List<String> colIdList = new ArrayList<String>();
		colIdList.addAll(mappingDefs.keySet());
		Map<String, List<XPathExpression>> mappingLists = new HashMap<String, List<XPathExpression>>();
		for (int j = 0; j < colIdList.size(); j++) {
			String colKey = colIdList.get(j);
			if (colKey.startsWith(category + ":")) {
				Object value = mappingDefs.opt(colKey);
				String mapping;
				if (value instanceof String) {
					mapping = (String) value;
					if (!mapping.equals("n.a")) {
						List<XPathExpression> xpeList = new ArrayList<XPathExpression>();
						xpeList.add(createXPath(mapping));
						mappingLists.put(colKey, xpeList);
					}
				} else if (value instanceof JSONObject) {
					JSONObject mappingSet = (JSONObject) value;
					Iterator<String> typeIt = mappingSet.keySet().iterator();
					while (typeIt.hasNext()) {
						String nextType = typeIt.next();
						mapping = mappingSet.optString(nextType, "n.a");
						if (!mapping.equals("n.a")) {
							List<XPathExpression> xpeList = new ArrayList<XPathExpression>();
							xpeList.add(createXPath(mapping));
							mappingLists.put(colKey + "#" + nextType, xpeList);
						}
					}
				} else if (value instanceof JSONArray) {
					JSONArray mappingSet = (JSONArray) value;
					List<XPathExpression> xpeList = new ArrayList<XPathExpression>();
					for (int i = 0; i < mappingSet.size(); i++) {
						mapping = mappingSet.getString(i);
						xpeList.add(createXPath(mapping));
					}
					mappingLists.put(colKey, xpeList);
				}
			}
		}
		return mappingLists;
	}

	private XPathExpression createXPath(String mapping) {
		if (mapping.equals("n.a")) {
			return null;
		}
		/*
		 * replace namespace placeholders with actual prefix being used
		 */
		String t1 = mapping.replaceAll("\\{avail\\}", availPrefix);
		String t2 = t1.replaceAll("\\{md\\}", mdPrefix);
		/*
		 * Check for cardinality indicator. A leading '*' indicates that if
		 * multiple elements match the xpath then their values are concatenated
		 * into a single string of comma separated values. Otherwise when
		 * multiple values are encountered a warning is logged and only the
		 * first value is mapped to the excel.
		 */
		boolean multipleOk = t2.startsWith("*");
		String xpath = "./" + t2.replace("*", "");

		// Now compile the XPath
		XPathExpression xpExpression;
		/**
		 * The following are examples of xpaths that return an attribute value
		 * and that we therefore need to identity:
		 * <ul>
		 * <li>avail:Term/@termName</li>
		 * <li>avail:Term[@termName[.='Tier' or .='WSP' or .='DMRP']</li>
		 * <li>@contentID</li>
		 * </ul>
		 * whereas the following should NOT match:
		 * <ul>
		 * <li>avail:Term/avail:Event[../@termName='AnnounceDate']</li>
		 * </ul>
		 */
		if (xpath.matches(".*/@[\\w]++(\\[.+\\])?")) {
			// must be an attribute value we're after..
			xpExpression = xpfac.compile(xpath, Filters.attribute(), null, availsNSpace, mdNSpace);
		} else {
			xpExpression = xpfac.compile(xpath, Filters.element(), null, availsNSpace, mdNSpace);
		}
		if (multipleOk) {
			allowsMultiples.add(xpExpression);
		}
		return xpExpression;
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
	private void setXmlVersion(String availSchemaVer) throws IllegalArgumentException {
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
				MddfContext.NSPACE_AVAILS_PREFIX + AVAIL_VER + MddfContext.NSPACE_AVAILS_SUFFIX);
	}

	/**
	 * @param destPath
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void export(String destPath) throws FileNotFoundException, IOException {
		/* First adjust column widths */
		int sheetCnt = workbook.getNumberOfSheets();
		for (int i = 0; i < sheetCnt; i++) { 
			Sheet sheet = workbook.getSheetAt(i);
			if (sheet != null) {
				String name = sheet.getSheetName();
				int colCount = mappingVersion.getJSONObject(name).size();
				for (int j = 0; j < colCount; j++) {
					sheet.autoSizeColumn(j);
				}
			}

		}
		try (FileOutputStream outputStream = new FileOutputStream(destPath)) {
			workbook.write(outputStream);
			logger.log(LogMgmt.LEV_INFO, logMsgDefaultTag, "XLSX saved to " + destPath, null, logMsgSrcId);

		}

	}
}
