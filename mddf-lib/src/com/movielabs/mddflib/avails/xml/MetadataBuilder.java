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
package com.movielabs.mddflib.avails.xml;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.FormatConverter;
import com.movielabs.mddflib.util.xml.RatingSystem;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Add to XML representation the Metadata portion of an Avails using the
 * mappings specified in the JSON file <tt>MetadataMappings.json</tt>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class MetadataBuilder {
	private static final String FUNCTION_KEY = "%FUNCTION";
	private static final String REF_KEY = "#REF:";
	public static final String ALT_ID_NAMESPACE_PREFIX = "org:mddf";
	private static JSONObject mappings;
	private LogMgmt logger;
	private int logMsgDefaultTag = LogMgmt.TAG_XLATE;
	protected String logMsgSrcId = "MetadataBuilder";
	protected XPathFactory xpfac = XPathFactory.instance();

	private XmlBuilder xmlBldr;
	private AbstractRowHelper row;
	private JSONObject mapping4Version;
	private JSONObject mapping4type;

	static {
		/*
		 * Load the JSON file with the mappings (i.e., specification of how
		 * specific XML elements are constructed from the XLSX cells
		 */
		try {
			InputStream inp = MetadataBuilder.class.getResourceAsStream("MetadataMappings.json");
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

	/**
	 * @param xlsxVersion
	 * @param logger
	 */
	public MetadataBuilder(Version xlsxVersion, LogMgmt logger, XmlBuilder xmlBldr) {
		this.logger = logger;
		this.xmlBldr = xmlBldr;
		String schemaVer = "V" + xmlBldr.getVersion();
		mapping4Version = mappings.getJSONObject(schemaVer);
		logger.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Using Schema Version " + schemaVer, null, logMsgSrcId);

	}

	/**
	 * Convert the relevant information in a spreadsheet row to an XML Metadata
	 * structure that may be appended to an <tt>&lt;avail:Asset&gt;</tt>
	 * element. The <tt>assetWorkType</tt> determines the specific Metadata
	 * element that will be returned. Valid <tt>assetWorkType</tt> values are:
	 * <ul>
	 * <li>Movie: returns a <tt>&lt;avail:Metadata&gt;</tt></li>
	 * <li>Episode: returns a <tt>&lt;avail:EpisodeMetadata&gt;</tt></li>
	 * <li>Season: returns a <tt>&lt;avail:SeasonMetadata&gt;</tt></li>
	 * <li>Series: returns a <tt>&lt;avail:SeriesMetadata&gt;</tt></li>
	 * </ul>
	 * 
	 * @param row
	 * @param assetWorkType
	 * @return a metadata <tt>Element</tt>
	 */
	public Element appendMData(AbstractRowHelper row, String assetWorkType) {
		this.row = row;
		/*
		 * Need to determine what metadata structure to use based on the
		 * Asset/WorkType
		 */
		switch (assetWorkType) {
		case "Season":
			mapping4type = mapping4Version.getJSONObject("Season");
			break;
		case "Episode":
			mapping4type = mapping4Version.getJSONObject("Episode");
			break;
		case "Series":
			break;
		default:
			// must be a Movie
			mapping4type = mapping4Version.getJSONObject("Movies");
			break;
		}
		if (mapping4type != null) {
			return buildMetadata();
		}
		throw new UnsupportedOperationException("Invalid JSON: Unsupported Asset work-type: " + assetWorkType);

	}

	private Element buildMetadata() {
		/*
		 * there should be a single key. The key will also serve as the name
		 * assigned to the metadata element (e.g. key is
		 * "{avail}EpisodeMetadata")
		 */
		Set<String> keys = mapping4type.keySet();
		if (keys.size() != 1) {
			throw new UnsupportedOperationException("Invalid JSON: too many primary keys");
		}
		List<String> tempList = new ArrayList<String>(keys);
		String mdKey = tempList.get(0);
		Element mdEl = buildElement(mdKey);
		JSONObject mdMappings = mapping4type.getJSONObject(mdKey);
		processLevel(mdEl, mdMappings);

		// --------------------
		// clean up (i.e., garbage collection) and return
		this.row = null;
		this.mapping4type = null;
		return mdEl;
	}

	/**
	 * @param parentEl
	 * @param mdMappings
	 */
	private void processLevel(Element parentEl, JSONObject mdMappings) {
		/*
		 * Each key defines an XML element to be created. To have valid XML they
		 * must be processed in order.
		 */
		List<String> mdKeyList = new ArrayList<String>(mdMappings.keySet());
		for (String nextKey : mdKeyList) {
			logger.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "processing JSON key " + nextKey, null, logMsgSrcId);

			Object nextValue = mdMappings.get(nextKey);
			if (nextValue instanceof String) {
				String value = (String) nextValue;
				if (value.startsWith(REF_KEY)) {
					processReference(nextKey, value, parentEl);
				} else {
					processString(nextKey, value, parentEl);
				}
			} else if (nextValue instanceof JSONObject) {
				processObject(nextKey, (JSONObject) nextValue, parentEl);
			} else if (nextValue instanceof JSONArray) {
				JSONArray jArray = (JSONArray) nextValue;
				Iterator<?> jit = jArray.iterator();
				while (jit.hasNext()) {
					Object nextInArray = jit.next();
					if (nextInArray instanceof String) {
						processString(nextKey, (String) nextInArray, parentEl);
					} else if (nextInArray instanceof JSONObject) {
						processObject(nextKey, (JSONObject) nextInArray, parentEl);
					}
				}
			} else {
				throw new UnsupportedOperationException("Invalid JSON: Unsupportable content under key=" + nextKey);
			}
		}
	}

	/**
	 * Process a metadata sub-section whose definition is NOT defined as a
	 * sub-component (i.e. child) of the section currently being processed. This
	 * is analogous to an XML element defined in terms of <tt>type</tt> defined
	 * elsewhere in an XSD.
	 * <p>
	 * Example:
	 * <tt>"{avail}SeasonMetadata": "#REF:Season/{avail}SeasonMetadata"</tt>
	 * <br/>
	 * where <tt>Season/{avail}SeasonMetadata</tt> is the path.
	 * </p>
	 * 
	 * @param key
	 *            is name of XML element to be created
	 * @param path
	 *            relative to the root of <tt>mapping4Version</tt>
	 * @param parentEl
	 */
	private void processReference(String key, String pathRef, Element parentEl) {
		String path = pathRef.replaceFirst(REF_KEY, "");
		logger.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "processing REF to " + path, null, logMsgSrcId);
		String[] parts = path.split("/");
		JSONObject mappingsTarget = mapping4Version;
		for (int i = 0; i < parts.length; i++) {
			mappingsTarget = mappingsTarget.getJSONObject(parts[i]);
		}
		Element curEl = buildElement(key);
		parentEl.addContent(curEl);
		processLevel(curEl, mappingsTarget);
	}

	/**
	 * Process a single-value mapping. An Attribute or child Element is added to
	 * a parent element and then assigned a value. The <tt>xmlId</tt> is a
	 * actually a key from the JSON <i>mappings</i> file and takes the form of
	 * either <tt>{<i>namespace</i>}elementName</tt> or <tt>@attrbuteName</tt>.
	 * Examples:
	 * <uL>
	 * <li>{avails}RunLength</li>
	 * <li>@contentID</li>
	 * </ul>
	 * 
	 * @param xmlId
	 *            name of an Attribute or Element to be added
	 * @param valueSrc
	 *            identifies a cell containing the value
	 * @param mdEl
	 *            parent element
	 */
	private void processString(String xmlId, String valueSrc, Element mdEl) {
		// 'valueSrc' identifies a column in the spreadsheet row
		Pedigree pg = row.getPedigreedData(valueSrc);
		if (pg == null) {
			throw new UnsupportedOperationException("Invalid JSON: unsupported column '" + valueSrc + "'");
		}
		// key defines the name of the child element or attribute
		if (xmlId.startsWith("@")) {
			// process as attribute
			// TODO
		} else {
			// process as element
			if (pg.isEmpty() && !isRequired(xmlId)) {
				return;
			}
			Element childEl = buildElement(xmlId);
			mdEl.addContent(childEl);
			childEl.setText(pg.getRawValue());
			xmlBldr.addToPedigree(childEl, pg);
		}
	}

	/**
	 * @param curKey
	 * @param curDefs
	 * @param parentEl
	 */
	private void processObject(String curKey, JSONObject curDefs, Element parentEl) {
		// is it a FUNCTION?
		if (curDefs.containsKey(FUNCTION_KEY)) {
			JSONObject funcDef = curDefs.getJSONObject(FUNCTION_KEY);
			processFunction(funcDef, curKey, parentEl);
		} else {
			// recursively process another level
			Element curEl = buildElement(curKey);
			parentEl.addContent(curEl);
			processLevel(curEl, curDefs);
		}
	}

	/**
	 * @param functionDef
	 * @param targetEl
	 */
	private void processFunction(JSONObject functionDef, String curKey, Element parentEl) {
		String funcName = functionDef.getString("name");
		switch (funcName) {
		case "altId":
			func_altId(functionDef, curKey, parentEl);
			break;
		case "contentRating":
			func_contentRating(functionDef, curKey, parentEl);
			break;
		case "eidr":
			func_eidr(functionDef, curKey, parentEl);
			break;
		case "formatType":
			func_format(functionDef, curKey, parentEl);
			break;
		case "people":
			func_people(functionDef, curKey, parentEl);
			break;
		case "releaseHistory":
			func_releaseHistory(functionDef, curKey, parentEl);
			break;
		case "channelGrouping":
			func_channelGrouping(functionDef, curKey, parentEl);
			break;
		default:
			throw new UnsupportedOperationException("Invalid JSON: unsupported function '" + funcName + "'");
		}

	}

	/**
	 * Set an <tt>AltIdentifier</tt>. The <tt>functionDef</tt> may include two
	 * optional arguments:
	 * <ul>
	 * <li><tt>namespace</tt>: if present, this value is used as the
	 * <tt>AltIdentifier/Namespace</tt>. If not specified, the default value
	 * specified by <tt>ALT_ID_NAMESPACE_PREFIX</tt> will be used.</li>
	 * <li><tt>filterEidr</tt>: If <tt>yes</tt>, EIDR values will be dropped. If
	 * <tt>no</tt> an EIDR value may still be used as an <tt>AltIdentifier</tt>.
	 * If not specified, the default value is <tt>yes</tt>.</li>
	 * </ul>
	 * 
	 * @param functionDef
	 * @param altIdEl
	 */
	protected void func_altId(JSONObject functionDef, String curKey, Element parentEl) {
		JSONObject functionArgs = functionDef.getJSONObject("args");
		String colKey = functionArgs.getString("col");
		logger.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "func_altId prcessing Col key " + colKey, null, logMsgSrcId);

		Pedigree pg = row.getPedigreedData(colKey);
		if (pg == null) {
			throw new UnsupportedOperationException("Invalid XLSX: unsupported column '" + colKey + "'");
		}
		if (pg.isEmpty() && !isRequired(curKey)) {
			return;
		}
		String idValue = pg.getRawValue();
		String filter = functionArgs.optString("filterEidr", "yes");
		if (filter.equalsIgnoreCase("yes")) {
			// what's the namespace (i.e., encoding fmt)?
			String format = parseIdFormat(idValue);
			if (format.startsWith("eidr")) {
				return;
			}
		}

		Element altIdEl = buildElement(curKey);
		parentEl.addContent(altIdEl);

		String namespace = functionArgs.optString("namespace", ALT_ID_NAMESPACE_PREFIX);
		String[] srcId = colKey.split("/");
		idValue = srcId[srcId.length - 1] + ":" + idValue;
		xmlBldr.addToPedigree(altIdEl, pg);

		Element nsEl = row.mGenericElement("Namespace", namespace, xmlBldr.getMdNSpace());
		altIdEl.addContent(nsEl);
		xmlBldr.addToPedigree(nsEl, pg);
		Element idEl = row.mGenericElement("Identifier", idValue, xmlBldr.getMdNSpace());
		altIdEl.addContent(idEl);
		xmlBldr.addToPedigree(idEl, pg);
	}

	/**
	 * @param functionDef
	 * @param curKey
	 * @param parentEl
	 */
	private void func_channelGrouping(JSONObject functionDef, String curKey, Element parentEl) {
		JSONObject functionArgs = functionDef.getJSONObject("args");
		Pedigree pg = row.getPedigreedData(functionArgs.getString("col"));
		if (pg.isEmpty() && !isRequired(curKey)) {
			return;
		}
		String grpId = pg.getRawValue();
		String grpName = grpId;
		String grpType = functionArgs.getString("type");

		Element grpEl = buildElement(curKey);
		parentEl.addContent(grpEl);

		Element typeEl = buildElement("{md}Type");
		grpEl.addContent(typeEl);
		typeEl.setText(grpType);

		Element idEl = buildElement("{md}GroupIdentity");
		grpEl.addContent(idEl);
		idEl.setText(grpId);

		Element nameEl = buildElement("{md}DisplayName");
		grpEl.addContent(nameEl);
		nameEl.setText(grpName);

		xmlBldr.addToPedigree(grpEl, pg);
		xmlBldr.addToPedigree(idEl, pg);
		xmlBldr.addToPedigree(nameEl, pg);
	}

	/**
	 * @param functionDef
	 * @param targetEl
	 */
	private void func_contentRating(JSONObject functionDef, String curKey, Element parentEl) {
		JSONObject functionArgs = functionDef.getJSONObject("args");

		Element ratings = parentEl.getChild("Ratings", xmlBldr.getAvailsNSpace());
		boolean addToParent;
		if (ratings == null) {
			ratings = new Element("Ratings", xmlBldr.getAvailsNSpace());
			addToParent = true;
		} else {
			addToParent = false;
		}
		/* These are the source columns for the data items.. */
		String rSysCol = functionArgs.getString("system");
		String rValueCol = functionArgs.getString("value");
		String rReasonCol = functionArgs.getString("reason");
		String rRegionCol = functionArgs.getString("region");

		String ratingSystem = row.getData(rSysCol);
		String ratingValue = row.getData(rValueCol);
		/*
		 * According to XML schema, both values are REQUIRED for a Rating. If
		 * any has been specified than we add the Rating element and let XML
		 * validation worry about completeness,
		 */
		boolean add = row.isSpecified(ratingSystem) || row.isSpecified(ratingValue);
		if (!add) {
			return;
		}

		/*
		 * Before adding another rating to a pre-existing set we check for
		 * uniqueness and ignore duplicates.
		 */
		Namespace mdNS = xmlBldr.getMdNSpace();
		String mdPrefix = mdNS.getPrefix();
		String xpath = "./" + mdPrefix + ":Rating[./" + mdPrefix + ":System='" + ratingSystem + "' and ./" + mdPrefix
				+ ":Value='" + ratingValue + "']";
		XPathExpression<Element> xpExpression = xpfac.compile(xpath, Filters.element(), null, xmlBldr.getAvailsNSpace(),
				xmlBldr.getMdNSpace());
		Element matching = xpExpression.evaluateFirst(ratings);
		if (matching != null) {
			// ignore pre-existing match
			// System.out.println("ignore pre-existing match::
			// "+ratingSystem+"-"+ratingValue);
			return;
		}

		Element rat = new Element("Rating", xmlBldr.getMdNSpace());
		ratings.addContent(rat);

		row.addRegion(rat, "Region", xmlBldr.getMdNSpace(), rRegionCol);
		Element rSysEl = row.process(rat, "System", xmlBldr.getMdNSpace(), rSysCol);
		row.process(rat, "Value", xmlBldr.getMdNSpace(), rValueCol);
		/*
		 * IF RatingSys provides defined reason codes then look for a comma
		 * separated listed of codes ELSE allow any single string value (i.e.,
		 * commas do not denote multiple reasons).
		 * 
		 */
		String system = rSysEl.getText();
		RatingSystem rSystem = RatingSystem.factory(system);
		/*
		 * Note that the 'system' has not yet been validated so we may have a
		 * null rSystem!
		 */
		if (rSystem == null || !(rSystem.providesReasons())) {
			row.process(rat, "Reason", xmlBldr.getMdNSpace(), rReasonCol, null);
		} else {
			// TODO???
			Element[] reasonList = row.process(rat, "Reason", xmlBldr.getMdNSpace(), rReasonCol, ",");
		}
		if (addToParent) {
			parentEl.addContent(ratings);
		}

	}

	/**
	 * Create an element that requires an EIDR value. While doing so, ensure all
	 * EIDR values are in URN format that is compatible with XML. If the cell's
	 * value is NOT a valid EIDR, handling depends on the <tt>filter</tt>
	 * argument specified by the <tt>functionDef</tt>.
	 * 
	 * @param functionDef
	 * @param curKey
	 * @param parentEl
	 */
	private void func_eidr(JSONObject functionDef, String curKey, Element parentEl) {
		JSONObject functionArgs = functionDef.getJSONObject("args");
		Pedigree pg = row.getPedigreedData(functionArgs.getString("col"));
		if (pg.isEmpty() && !isRequired(curKey)) {
			return;
		}

		String idValue = pg.getRawValue();
		// what's the namespace (i.e., encoding fmt)?
		String namespace = parseIdFormat(idValue);
		switch (namespace) {
		case "eidr-5240":
			idValue = idValue.replaceFirst("10.5240/", "urn:eidr:10.5240:");
		case "eidr-URN":
			break;
		default:
			/* value is not an EIDR. */
			String filter = functionArgs.optString("filter", "yes");
			if (filter.equalsIgnoreCase("yes")) {
				return;
			}
			/*
			 * the non-EIDR value will be used. Assumption is that validation
			 * procedure will catch and flag the problem.
			 */
			break;
		}
		Element targetEl = buildElement(curKey);
		parentEl.addContent(targetEl);
		targetEl.setText(idValue);
	}

	/**
	 * Handle cases where value has to be translated or reformatted. This
	 * happens, for example, with durations where XSD specifies xs:duration
	 * syntax.
	 * 
	 * @param input
	 * @return
	 */
	private void func_format(JSONObject functionDef, String curKey, Element parentEl) {
		JSONObject functionArgs = functionDef.getJSONObject("args");
		String colKey = functionArgs.getString("col");
		Pedigree pg = row.getPedigreedData(colKey);
		if (pg == null) {
			throw new UnsupportedOperationException("Invalid JSON: unsupported column '" + colKey + "'");
		}
		if (pg.isEmpty() && !isRequired(curKey)) {
			return;
		}
		Element targetEl = buildElement(curKey);
		parentEl.addContent(targetEl);
		String type = functionArgs.getString("type");
		String value = null;
		switch (type) {
		case "xs:boolean":
			value = FormatConverter.booleanToXml(pg.getRawValue());
			break;
		case "xs:dateTime":
			boolean rounding = functionArgs.getString("roundOff").equals("true");
			value = FormatConverter.dateTimeToXml(pg.getRawValue(), rounding);
			break;
		case "xs:duration":
			value = FormatConverter.durationToXml(pg.getRawValue());
			break;
		default:
			throw new UnsupportedOperationException("Invalid JSON: unsupported format type '" + type + "'");
		}
		targetEl.setText(value);
	}

	/**
	 * @param functionDef
	 * @param rHistoryEl
	 */
	private void func_people(JSONObject functionDef, String curKey, Element parentEl) {
		JSONObject functionArgs = functionDef.getJSONObject("args");
		Pedigree pg = row.getPedigreedData(functionArgs.getString("col"));
		if (pg.isEmpty() && !isRequired(curKey)) {
			return;
		}
		Element peopleEl = buildElement(curKey);
		parentEl.addContent(peopleEl);

		Element jobEl = buildElement("{md}Job");
		peopleEl.addContent(jobEl);
		Element jobFEl = buildElement("{md}JobFunction");
		jobEl.addContent(jobFEl);
		String type = functionArgs.getString("job");
		jobFEl.setText(type);

		Element nameEl = buildElement("{md}Name");
		peopleEl.addContent(nameEl);
		Element dNameEl = buildElement("{md}DisplayName");
		nameEl.addContent(dNameEl);
		dNameEl.setText(pg.getRawValue());
		xmlBldr.addToPedigree(dNameEl, pg);
	}

	/**
	 * @param functionDef
	 * @param rHistoryEl
	 */
	private void func_releaseHistory(JSONObject functionDef, String curKey, Element parentEl) {
		JSONObject functionArgs = functionDef.getJSONObject("args");
		Pedigree pg = row.getPedigreedData(functionArgs.getString("col"));
		if (pg.isEmpty() && !isRequired(curKey)) {
			return;
		}
		Element rHistoryEl = buildElement(curKey);
		parentEl.addContent(rHistoryEl);

		Element rTypeEl = buildElement("{md}ReleaseType");
		String type = functionArgs.getString("type");
		rTypeEl.setText(type);
		rHistoryEl.addContent(rTypeEl);

		Element dateEl = buildElement("{md}Date");
		dateEl.setText(pg.getRawValue());
		xmlBldr.addToPedigree(dateEl, pg);
		rHistoryEl.addContent(dateEl);
	}

	/**
	 * @param xmlId
	 * @return
	 */
	private boolean isRequired(String xmlId) {
		String name = xmlId.split("}")[1];
		String nsId = null;
		if (xmlId.startsWith("{avail}")) {
			nsId = "avails";
		} else if (xmlId.startsWith("{md}")) {
			nsId = "md";
		}
		return xmlBldr.isRequired(name, nsId);
	}

	/**
	 * @param key
	 * @return
	 */
	private Element buildElement(String key) {
		if (!key.startsWith("{")) {
			return new Element(key);
		}
		Namespace childNs = null;
		if (key.startsWith("{avail}")) {
			childNs = xmlBldr.getAvailsNSpace();
		} else if (key.startsWith("{md}")) {
			childNs = xmlBldr.getMdNSpace();
		} else {
			throw new UnsupportedOperationException("Can not proceed due to malformed JSON; key=" + key);
		}
		String name = key.split("}")[1];
		return new Element(name, childNs);
	}

	/**
	 * Determine the format of an id string. Possible return values are:
	 * <ul>
	 * <li>eidr-URN</li>
	 * <li>eidr-5240</li>
	 * <li>MovieLabs (e.g. <tt>md:cid:org:mpm.craigsmovies.com:20938</tt>)</li>
	 * <li>user: anything else</li>
	 * </ul>
	 * 
	 * @param idValue
	 * @return
	 */
	public static String parseIdFormat(String idValue) {
		if (idValue.startsWith("urn:eidr:")) {
			return "eidr-URN";
		} else if (idValue.startsWith("10.5240/")) {
			return "eidr-5240";
		} else if (idValue.startsWith("md:")) {
			return "MovieLabs";
		} else {
			// Set it w/o change and let the Validation code will flag as error
			return "user";
		}
	}
}
