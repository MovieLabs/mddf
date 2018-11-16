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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.avails.xml.MetadataBuilder;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.FormatConverter;
import com.movielabs.mddflib.util.xml.SchemaWrapper;
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
	private static final String CONTEXT_DELIM = "#";
	private static final String FUNCTION_IDENTIFIER = "%FUNCTION";
	protected static JSONObject mappings;
	private static String warnMsg1 = "XLSX xfer dropping additional XYZ values";
	private static String warnDetail1 = "The Excel version of Avails only allows 1 value for this field. Additional XML elements will be ignored";
	protected XPathFactory xpfac = XPathFactory.instance();
	private LogMgmt logger;
	private String rootPrefix = "avails:";

	private int logMsgDefaultTag = LogMgmt.TAG_XLATE;
	protected String logMsgSrcId = "XlsxBuilder";
	private String MD_VER;
	private String MDMEC_VER;
	private Namespace mdNSpace;
	private SchemaWrapper mdSchema;
	private String AVAIL_VER;
	private Namespace availsNSpace;
	private SchemaWrapper availsSchema;
	private Element rootEl;
	private ArrayList<Element> tvAvailsList;
	private ArrayList<Element> movieAvailsList;
	private HashSet<XPathExpression<?>> allowsMultiples = new HashSet<XPathExpression<?>>();
	private TemplateWorkBook workbook;
	private JSONObject mappingVersion;
	private String availPrefix;
	private String mdPrefix;
	private HashMap<String, JSONObject> functionList;
	private JSONObject mappingDefs;

	static {
		/*
		 * Load JSON file with the mapping (i.e., specification of how specific xlsx
		 * columns are populated from the XML
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
		// p_xsDuration = Pattern.compile(DURATION_REGEX);
		// p_xsDateTime = Pattern.compile(DATETIME_REGEX);

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
		workbook = new TemplateWorkBook(logger);
		addMovieAvails();
		addTvAvails();
	}

	/**
	 * 
	 */
	private void addMovieAvails() {
		if (movieAvailsList.isEmpty()) {
			return;
		}
		addAvails("Movies", movieAvailsList);
	}

	/**
	 * 
	 */
	private void addTvAvails() {
		/*
		 * XLSX format will only support TV avails of type 'episode' or 'season'.
		 * Anything else will have been filtered out when the Avails were sorted.
		 */
		if (tvAvailsList.isEmpty()) {
			return;
		}
		addAvails("TV", tvAvailsList);
	}

	private void addAvails(String category, List<Element> availList) {
		// get mappings that will be used for this specific sheet..
		mappingDefs = mappingVersion.getJSONObject(category);
		ArrayList<String> colIdList = new ArrayList<String>();
		colIdList.addAll(mappingDefs.keySet());

		XSSFSheet sheet = workbook.addSheet(category, colIdList);

		/* Initialize xpaths that implement the data mappings */
		Map<String, Map<String, List<XPathExpression>>> xpathSets = initializeMappings(mappingDefs);

		for (int i = 0; i < availList.size(); i++) {
			Element availEl = availList.get(i);
			/*
			 * one row is added for each combination of Asset and Transaction included in
			 * the Avail which means there are usually multiple rows for each Avail. Start
			 * by getting the info that will be common to each row.
			 */
			Map<String, List<XPathExpression>> availMappings = xpathSets.get("Avail");
			Map<String, String> commonData = extractData(availEl, availMappings, "");
			Set<String> foo = commonData.keySet();
			/*
			 * now identify each Asset that is a child of this Avail and prepare its data.
			 */
			List<Element> assetList = availEl.getChildren("Asset", availsNSpace);
			Map<String, List<XPathExpression>> assetMappings = xpathSets.get("AvailAsset");
			Map<String, List<XPathExpression>> metadataMappings = xpathSets.get("AvailMetadata");
			List<Map<String, String>> perAssetData = new ArrayList<Map<String, String>>(assetList.size());
			for (int j = 0; j < assetList.size(); j++) {
				Element assetEl = assetList.get(j);
				/*
				 * Mappings for Assets are in some cases dependent on the WorkType so 1st step
				 * is get that value
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
			 * now identify each Transaction that is a child of this Avail and prepare its
			 * data.
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
					workbook.addDataRow(rowData, sheet);
				}
			}
		}

	}

	/**
	 * @param baseEl
	 * @param categoryMappings
	 * @param context
	 * @return
	 */
	private Map<String, String> extractData(Element baseEl, Map<String, List<XPathExpression>> categoryMappings,
			String context) {
		Map<String, String> dataMap = new HashMap<String, String>();
		/* Now continue with everything else */
		Iterator<String> keyIt = categoryMappings.keySet().iterator();
		while (keyIt.hasNext()) {
			String mappingKey = keyIt.next();
			if (mappingKey.contains(FUNCTION_IDENTIFIER)) {
				try {
					String value = processFunction(mappingKey, baseEl, context, categoryMappings);
					if (value != null) {
						String colKey = mappingKey.replace(FUNCTION_IDENTIFIER, "");
						dataMap.put(colKey, value);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				continue;
			}
			List<XPathExpression> xpeList = null;
			if (mappingKey.contains(CONTEXT_DELIM)) {
				String[] parts = mappingKey.split(CONTEXT_DELIM);
				if (parts[1].equals(context)) {
					xpeList = categoryMappings.get(mappingKey);
					mappingKey = parts[0];
				}
			} else {
				xpeList = categoryMappings.get(mappingKey);
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
					nextValue = convertValue(nextValue, target);
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
		Object target = null;
		List targetList = xpe.evaluate(baseEl);
		int matchCnt = 0;
		if (targetList != null && (!targetList.isEmpty())) {
			for (int i = 0; i < targetList.size(); i++) {
				target = targetList.get(i);
				if (target instanceof Element) {
					matchCnt++;
					Element targetEl = (Element) target;
					if (matchCnt == 1) {
						value = targetEl.getTextNormalize();
					} else if (matchCnt > 1) {
						String msg = warnMsg1.replace("XYZ", mappingKey);
						logger.logIssue(logMsgDefaultTag, LogMgmt.LEV_WARN, targetEl, msg, warnDetail1, null,
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
						logger.logIssue(logMsgDefaultTag, LogMgmt.LEV_WARN, targetEl, msg, warnDetail1, null,
								logMsgSrcId);
					}
				}

			}
			if (value != null) {
				value = convertValue(value, target);
			}
		}
		return value;
	}

	/**
	 * @param mappingKey
	 * @param baseEl
	 * @param context
	 * @param categoryMappings
	 * @return
	 */
	private String processFunction(String mappingKey, Element baseEl, String context,
			Map<String, List<XPathExpression>> categoryMappings) {
		String fKey = mappingKey.replaceAll(FUNCTION_IDENTIFIER, "");
		JSONObject functionDef = functionList.get(fKey);
		String funcName = functionDef.getString("name");
		switch (funcName) {
		case "caption":
			return func_captions(functionDef, context, baseEl, categoryMappings);
		case "eidr":
			return func_eidr(functionDef, context, baseEl);
		default:
			throw new UnsupportedOperationException("Invalid JSON: unsupported function '" + funcName + "'");
		}
	}

	/**
	 * Convert all EIDR values to "eidr-5240" format. This is the default format
	 * used for an XLSX formatted Avails.
	 * 
	 * @param functionDef
	 * @param context
	 * @param baseEl
	 * @return
	 */
	private String func_eidr(JSONObject functionDef, String context, Element baseEl) {
		JSONObject functionArgs = functionDef.getJSONObject("args");
		String xpath = functionArgs.getString("xpath");
		XPathExpression<Element> srcElPath = (XPathExpression<Element>) createXPath(xpath);
		Element targetEl = srcElPath.evaluateFirst(baseEl);
		if (targetEl == null) {
			return null;
		}
		String value = targetEl.getTextNormalize();
		// what's the namespace (i.e., encoding fmt)?
		String namespace = MetadataBuilder.parseIdFormat(value);
		switch (namespace) {
		case "eidr-URN":
			value = value.replaceFirst("urn:eidr:10.5240:", "10.5240/");
			break;
		case "eidr-5240":
		default:
			break;
		}

		return value;

	}

	/**
	 * Handles special case of US caption exemptions. This function should only be
	 * used with the <tt>AvailMetadata/CaptionIncluded</tt> field. This is required
	 * in the US. Non-US territories may leave it blank.
	 * <p>
	 * Since there is no equivalent field in XML it must be inferred. The rule is:
	 * 
	 * <pre>
	 * IF (CaptionExemption is NOT set AND the Territory == 'US") THEN
	 *     CaptionIncluded = YES
	 * ELSE
	 *    CaptionIncluded = NO
	 * </pre>
	 * 
	 * Note that this is therefore a 'no argument' function in so far as the
	 * functionDef goes. All that is required is the <tt>context</tt> and
	 * <tt>baseEl</tt> parameters.
	 * </p>
	 * <p>
	 * Since scope is limited to Avails applicable to US, that is a key issue. The
	 * only territorial info in an Avails is part of a Transaction. The problem is
	 * that there can be multiple Transactions and a single Transaction may specify
	 * zero or many Territories. Thus, if there is <b>any</b> Transaction row with
	 * “US” in-scope, then <tt>CaptionIncluded</tt> should be set.
	 * 
	 * </p>
	 * 
	 * @param functionDef
	 * @param context
	 * @param baseEl
	 * @param categoryMappings
	 * @return
	 */
	private String func_captions(JSONObject functionDef, String context, Element baseEl,
			Map<String, List<XPathExpression>> categoryMappings) {
		// Q1: is <avails:CaptionExemption> set?
		XPathExpression<Element> cePath = null;
		if((context != null)&& (!context.isEmpty())) {
			String contextKey = "AvailMetadata:CaptionExemption" + CONTEXT_DELIM + context;
			if (categoryMappings.containsKey(contextKey)) {
				cePath = categoryMappings.get(contextKey).get(0);
			} 
		}
		if (cePath == null) {
			cePath = categoryMappings.get("AvailMetadata:CaptionExemption").get(0);
		}   
		Element targetEl = cePath.evaluateFirst(baseEl);
		boolean exemptionProvided;
		if (targetEl == null) {
			exemptionProvided = false;
		} else {
			String value = targetEl.getTextTrim();
			exemptionProvided = !(value.isEmpty());
		}
		if (exemptionProvided) {
			/*
			 * CaptionIncluded should be set to NO on assumption that since a reason is
			 * provided US must be in-scope.
			 */
			return "No";
		}
		// Need to know if US is in-scope.
		Element parentAssetEl = baseEl.getParentElement();
		String includedPath = "./{avail}Transaction/{avail}Territory/{md}country[. = 'US']";
		@SuppressWarnings("unchecked")
		XPathExpression<Element> inPath = (XPathExpression<Element>) createXPath(includedPath);
		List<Element> inList = inPath.evaluate(parentAssetEl);
		boolean inScope = !inList.isEmpty();
		if (inScope) {
			if (!exemptionProvided) {
				return "Yes";
			} else {
				return "No";
			}
		}
		return null;
	}

	/**
	 * check for special cases where value has to be translated. This happens where
	 * XSD specifies xs:duration or xs:dateTime and the Excel specifies some simpler
	 * format. It also covers special cases such as translating the 'EpisodeWSP'
	 * <tt>termName</tt> attribute.
	 * 
	 * @param input
	 * @return
	 */
	private String convertValue(String input, Object xmlSrc) {
		if (xmlSrc instanceof Attribute) {
			// handle attributes w/o a namespace prefix
			Attribute xmlAtt = (Attribute) xmlSrc;
			String name = xmlAtt.getName();
			if (name.equals("termName")) {
				return convertTermName(input);
			}
			// default handling...
			String interim = FormatConverter.durationFromXml(input);
			interim = FormatConverter.dateFromXml(interim);
			return interim;
		}
		Element xmlEl = (Element) xmlSrc;
		String name = xmlEl.getName();
		String nsPrefix = xmlEl.getNamespacePrefix();
		if (nsPrefix != null) {
			SchemaWrapper sw;
			switch (nsPrefix) {
			case "avails":
				sw = availsSchema;
				break;
			case "md":
				sw = mdSchema;
				break;
			default:
				return input;
			}
			String type = sw.getType(name);
			switch (type) {
			case "xs:duration":
				return FormatConverter.durationFromXml(input);
			case "xs:dateTime":
				return FormatConverter.dateFromXml(input);
			default:
				return convertAltID(input, xmlSrc);
			}
		}
		return input;
	}

	/**
	 * 
	 * @param input
	 * @return
	 */
	private String convertTermName(String input) {
		switch (input) {
		case "EpisodeWSP":
		case "SeasonWSP":
			return "WSP";
		}
		return input;
	}

	/**
	 * @param input
	 * @param xmlSrc
	 * @return
	 */
	private String convertAltID(String input, Object xmlSrc) {
		if (!(xmlSrc instanceof Element)) {
			return input;
		}
		Element srcEl = (Element) xmlSrc;
		if (!srcEl.getName().equals("Identifier")) {
			return input;
		}
		if (srcEl.getNamespace() != mdNSpace) {
			return input;
		}
		Element parentEl = srcEl.getParentElement();
		// check namespace
		Element nsEl = parentEl.getChild("Namespace", mdNSpace);
		if (nsEl == null) {
			return input;
		}
		/*
		 * If we got this far then we are dealing with some form of content identifier.
		 * Now find out if its being used as an AltID...
		 */
		if (!(nsEl.getText().startsWith(MetadataBuilder.ALT_ID_NAMESPACE_PREFIX))) {
			return input;
		}
		String[] parts = input.split(":", 2);
		return parts[1];
	}

	/**
	 * Group, filter, and re-format the XML-to-XSLX mappings to facilitate later
	 * usage.
	 * 
	 * @param mappingDefs
	 * @return
	 */
	private Map<String, Map<String, List<XPathExpression>>> initializeMappings(JSONObject mappingDefs) {
		functionList = new HashMap<String, JSONObject>();
		Map<String, Map<String, List<XPathExpression>>> organizedMappings = new HashMap<String, Map<String, List<XPathExpression>>>();
		List<String> colIdList = new ArrayList<String>();
		colIdList.addAll(mappingDefs.keySet());
		// .....................................
		/* AVAIL-related mappings.... */
		Map<String, List<XPathExpression>> availMappings = initCategoryMappings(mappingDefs, "Avail");
		/*
		 * Special Case #1: an Avail-related column that doesn't start with 'Avail:'
		 */
		String colKey = "Disposition:EntryType";
		String mapping = mappingDefs.optString(colKey, "n.a");
		List<XPathExpression> xpeList = new ArrayList<XPathExpression>();
		xpeList.add(createXPath(mapping));
		availMappings.put(colKey, xpeList);
		/*
		 * Special Case #2 (part 1): Avail-prefixed col is actually transaction-scoped
		 * data
		 */
		List<XPathExpression> availIdXpe = availMappings.remove("Avail:AvailID");
		List<XPathExpression> reportIdXpe = availMappings.remove("Avail:ReportingID");

		organizedMappings.put("Avail", availMappings);
		// ..........................................
		Map<String, List<XPathExpression>> assetMappings = initCategoryMappings(mappingDefs, "AvailAsset");
		organizedMappings.put("AvailAsset", assetMappings);
		// .....................................
		Map<String, List<XPathExpression>> metadataMappings = initCategoryMappings(mappingDefs, "AvailMetadata");
		organizedMappings.put("AvailMetadata", metadataMappings);
		// .....................................
		Map<String, List<XPathExpression>> transMappings = initCategoryMappings(mappingDefs, "AvailTrans");
		/* Part 2 of Special Case #2.. */
		transMappings.put("Avail:AvailID", availIdXpe);
		transMappings.put("Avail:ReportingID", reportIdXpe);
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
					JSONObject innerMapping = (JSONObject) value;
					/*
					 * 'innerMapping' is either (a) a single %FUNCTION or (b) a set of mappings,
					 * each one for a specific "context" (i.e., workType).
					 */
					Iterator<String> typeIt = innerMapping.keySet().iterator();
					while (typeIt.hasNext()) {
						String nextType = typeIt.next();
						if (nextType.equals(FUNCTION_IDENTIFIER)) {
							mappingLists.put(colKey + FUNCTION_IDENTIFIER, null);
							JSONObject functionDef = innerMapping.getJSONObject(nextType);
							functionList.put(colKey, functionDef);
						} else {
							mapping = innerMapping.optString(nextType, "n.a");
							if (!mapping.equals("n.a")) {
								List<XPathExpression> xpeList = new ArrayList<XPathExpression>();
								xpeList.add(createXPath(mapping));
								mappingLists.put(colKey + CONTEXT_DELIM + nextType, xpeList);
							}
						}
					}
				} else if (value instanceof JSONArray) {
					/*
					 * Note that a %FUNCTION can not be used in an array
					 */
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

	/**
	 * Similar to <tt>StructureValidation.resolveXPath()</tt> except that if deals
	 * with the possible presence of a <i>cardinality indicator</i>.
	 * 
	 * @param mapping
	 * @return
	 */
	private XPathExpression<?> createXPath(String mapping) {
		if (mapping.equals("n.a")) {
			return null;
		}
		/*
		 * replace namespace placeholders with actual prefix being used
		 */
		String t1 = mapping.replaceAll("\\{avail\\}", availPrefix);
		String t2 = t1.replaceAll("\\{md\\}", mdPrefix);
		/*
		 * Check for cardinality indicator. A leading '*' indicates that if multiple
		 * elements match the xpath then their values are concatenated into a single
		 * string of comma separated values. Otherwise when multiple values are
		 * encountered a warning is logged and only the first value is mapped to the
		 * excel.
		 */
		boolean multipleOk = t2.startsWith("*");
		String xpath = "./" + t2.replace("*", "");

		// Now compile the XPath
		XPathExpression xpExpression;
		/**
		 * The following are examples of xpaths that return an attribute value and that
		 * we therefore need to identity:
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
	 * Sort the Avails defined in the XML into two sets: one for movies and one for
	 * TV. Avails that have a type not supported by the XLSX version of Avails will
	 * be ignored.
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
	 * Configure all XML-related functions to work with the specified version of the
	 * Avails XSD. This includes setting the correct version of the Common Metadata
	 * and MDMEC XSD that are used with the specified Avails version. If the
	 * <tt>availSchemaVer</tt> is not supported by the current version of
	 * <tt>mddf-lib</tt> an <tt>IllegalArgumentException</tt> will be thrown.
	 * 
	 * @param availSchemaVer
	 * @param availNsPrefix
	 * @throws IllegalArgumentException
	 */
	private void setXmlVersion(String availSchemaVer) throws IllegalArgumentException {
		switch (availSchemaVer) {
		case "2.3":
			MD_VER = "2.6";
			MDMEC_VER = "2.6";
			break;
		case "2.2.2":
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

		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + MD_VER + "/md");
		availsNSpace = Namespace.getNamespace("avails",
				MddfContext.NSPACE_AVAILS_PREFIX + AVAIL_VER + MddfContext.NSPACE_AVAILS_SUFFIX);

		mdSchema = SchemaWrapper.factory("md-v" + MD_VER);
		availsSchema = SchemaWrapper.factory("avails-v" + AVAIL_VER);
	}

	/**
	 * @return the workbook
	 */
	public TemplateWorkBook getWorkbook() {
		return workbook;
	}

}
