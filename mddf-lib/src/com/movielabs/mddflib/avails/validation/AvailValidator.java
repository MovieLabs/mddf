/**
 * Created Aug 31, 2016 
 * Copyright Motion Picture Laboratories, Inc. 2016
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.movielabs.mddflib.avails.validation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;

import com.movielabs.mddflib.ManifestIngester;
import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;

/**
 * Validates an Avails file as conforming to EMA Content Availability Data
 * (Avails) as specified in <tt>TR-META-AVAIL (v2.1)</tt>. Validation also
 * includes testing for conformance with the <tt>Common Metadata (md)</tt>
 * specification as defined in <tt>TR-META-CM (v2.4)</tt>
 * 
 * @see <a href= "http://www.movielabs.com/md/avails/v2.1/Avails_v2.1.pdf"> TR-
 *      META-AVAIL (v2.1)</a>
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class AvailValidator extends ManifestIngester {

	/**
	 * Used to facilitate keeping track of cross-references and identifying
	 * 'orphan' elements.
	 * 
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	protected class XrefCounter {
		private int count = 0;
		private String elType;
		private String elId;

		/**
		 * @param elType
		 * @param elId
		 */
		XrefCounter(String elType, String elId) {
			super();
			this.elType = elType;
			this.elId = elId;
			// System.out.println("CONSTRUCT: "+elId+", cnt="+count);
		}

		int increment() {
			count++;
			return count;
		}

		void validate() {
			// System.out.println(" VALIDATE: "+elId+", cnt="+count);
			if (count > 0) {
				return;
			} else {
				Map<String, Element> idMap = id2XmlMappings.get(elType);
				Element targetEl = idMap.get(elId);
				String explanation = "The element is never referenced by it's ID";
				String msg = "Unreferenced <" + elType + "> Element";
				logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_WARN, targetEl, msg, explanation, null, logMsgSrcId);
			}
		}

	}

	public static final String LOGMSG_ID = "AvailValidator";
	private static HashMap<String, String> id2typeMap;
	private static JSONObject availVocab;

	protected boolean validateC;
	protected Element curRootEl;
	protected boolean curFileIsValid;
	protected Map<String, HashSet<String>> idSets;
	protected Map<String, Map<String, XrefCounter>> idXRefCounts;
	protected Map<String, Map<String, Element>> id2XmlMappings;
	private Map<Object, Pedigree> pedigreeMap;

	static {
		id2typeMap = new HashMap<String, String>();
		try {
			availVocab = loadVocab(cmVrcPath, "Avail");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @param validateC
	 */
	public AvailValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;
		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_AVAIL;
	}

	public boolean process(File xmlFile, Element rootEl, Map<Object, Pedigree> pedigreeMap)
			throws IOException, JDOMException {
		curFile = xmlFile;
		curFileName = xmlFile.getName();
		this.pedigreeMap = pedigreeMap;
		curFileIsValid = true;
		if (xmlFile.getName().endsWith(".xml")) {
			validateXml(xmlFile);
		}
		if (!curFileIsValid) {
			String msg = "Schema validation check FAILED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, curFile, logMsgSrcId);
			// return false;
		} else {
			String msg = "Schema validation check PASSED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, curFile, logMsgSrcId);
			curRootEl = rootEl; // getAsXml(xmlFile);
			if (validateC) {
				validateConstraints();
			}
		}
		// clean up and go home
		this.pedigreeMap = null;
		return curFileIsValid;
	}

	/**
	 * Validate everything that is fully specified via the XSD.
	 * 
	 * @param xmlFile
	 */
	protected boolean validateXml(File xmlFile) {
		String xsdFile = "./resources/avails-v" + ManifestIngester.AVAIL_VER + ".xsd";
		curFileIsValid = validateXml(xmlFile, xsdFile, logMsgSrcId);
		return curFileIsValid;
	}

	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateConstraints() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_AVAIL, "Validating constraints", curFile, LOGMSG_ID);
		idSets = new HashMap<String, HashSet<String>>();
		idXRefCounts = new HashMap<String, Map<String, XrefCounter>>();
		id2XmlMappings = new HashMap<String, Map<String, Element>>();

		// TODO: Load from JSON file....
		validateId("ALID", null);

		/* Now validate cross-references */
		// validateXRef("Experience", "ContentID", "Metadata");
		// checkForOrphans();

		/* Validate indexed sequences that must be monotonically increasing */
		// validateIndexing("Chapter", "index", "Chapters");

		/*
		 * Validate the usage of controlled vocab (i.e., places where XSD
		 * specifies a xs:string but the documentation specifies an enumerated
		 * set of allowed values).
		 */
		// start with Common Metadata spec..
		boolean cmOk = validateCMVocab();

		// Now do any defined in Avails spec..
		boolean availOk = validateAvailVocab();

		validateCardinality();
	}

	/**
	 * 
	 */
	private void validateCardinality() {
		/*
		 * Sec 4.2.7: At least one instance of ContainerReference or
		 * BasicMetadata must be included for each Inventory/Metadata instance
		 */

	}

	/**
	 * @param elementName
	 * @param idxAttribute
	 * @param parentName
	 */
	protected void validateIndexing(String elementName, String idxAttribute, String parentName) {
		XPathExpression<Element> xpExpression = xpfac.compile(".//avails:" + parentName, Filters.element(), null,
				manifestNSpace);
		List<Element> parentElList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < parentElList.size(); i++) {
			Element parentEl = (Element) parentElList.get(i);
			List<Element> childList = parentEl.getChildren(elementName, manifestNSpace);
			Boolean[] validIndex = new Boolean[childList.size()];
			Arrays.fill(validIndex, Boolean.FALSE);
			for (int cPtr = 0; cPtr < childList.size(); cPtr++) {
				Element nextChildEl = childList.get(cPtr);
				/*
				 * Each child Element must have @index attribute
				 */
				String indexAsString = nextChildEl.getAttributeValue(idxAttribute);
				if (!isValidIndex(indexAsString)) {
					String msg = "Invalid value for indexing attribute (non-negative integer required)";
					logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, nextChildEl, msg, null, null, logMsgSrcId);
					curFileIsValid = false;
				} else {
					int index = Integer.parseInt(indexAsString);
					if (index >= validIndex.length) {
						String msg = "value for indexing attribute out of range";
						logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, nextChildEl, msg, null, null, logMsgSrcId);

					} else if (validIndex[index]) {
						// duplicate value
						String msg = "Invalid value for indexing attribute (duplicate value)";
						logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, nextChildEl, msg, null, null, logMsgSrcId);
						curFileIsValid = false;
					} else {
						validIndex[index] = true;
					}
				}
			}
			// now make sure all values were covered..
			boolean allValues = true;
			for (int j = 0; j < validIndex.length; j++) {
				allValues = allValues && validIndex[j];
			}
			if (!allValues) {
				String msg = "Invalid indexing of " + elementName + " sequence: be monotonically increasing";
				logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, parentEl, msg, null, null, logMsgSrcId);
				curFileIsValid = false;
			}
		}
	}

	/**
	 * Check for the presence of an ID attribute and, if provided, verify it is
	 * unique. If <tt>idAttribute</tt> is <tt>null</tt> then the element itself
	 * specifies the if value.
	 * 
	 * @param elementName
	 * @param idAttribute
	 * @return the <tt>Set</tt> of unique IDs found.
	 */
	protected HashSet<String> validateId(String elementName, String idAttribute) {
		XPathExpression<Element> xpExpression = xpfac.compile(".//avails:" + elementName, Filters.element(), null,
				availsNSpace);
		HashSet<String> idSet = new HashSet<String>();

		/*
		 * idXRefCounter is initialized here but will 'filled in' by
		 * validateXRef();
		 */
		HashMap<String, XrefCounter> idXRefCounter = new HashMap<String, XrefCounter>();
		/*
		 * id2XmlMap is provided for look-ups later as an alternative to using
		 * XPaths.
		 */
		HashMap<String, Element> id2XmlMap = new HashMap<String, Element>();

		List<Element> elementList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < elementList.size(); i++) {
			/*
			 * XSD may specify ID attribute as OPTIONAL but we need to verify
			 * cross-references and uniqueness.
			 */
			Element targetEl = (Element) elementList.get(i);
			String idValue = null;
			String idKey = null;
			if (idAttribute == null) {
				idValue = targetEl.getTextNormalize();
				idKey = elementName;
			} else {
				idValue = targetEl.getAttributeValue(idAttribute);
				idKey = idAttribute;
			}
			id2XmlMap.put(idValue, targetEl);
			XrefCounter count = new XrefCounter(elementName, idValue);
			idXRefCounter.put(idValue, count);

			if ((idValue == null) || (idValue.isEmpty())) {
				String msg = "ID not specified. References to this " + elementName + " will not be supportable.";
				logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_WARN, targetEl, msg, null, null, logMsgSrcId);
			} else {
				if (!idSet.add(idValue)) {
					LogReference srcRef = LogReference.getRef("CM", "2.4", "cm001a");
					String msg = idKey + " is not unique";
					logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, targetEl, msg, null, srcRef, logMsgSrcId);
					curFileIsValid = false;
				}
				/*
				 * Validate identifier structure conforms with Sec 2.1 of Common
				 * Metadata spec (v2.4)
				 */

				String idSyntaxPattern = "[\\S-[:]]+:[\\S-[:]]+:[\\S-[:]]+:[\\S]+$";
				if (!idValue.matches(idSyntaxPattern)) {
					String msg = "Invalid Identifier syntax";
					LogReference srcRef = LogReference.getRef("CM", "2.4", "cm001b");
					logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, targetEl, msg, null, srcRef, logMsgSrcId);
					curFileIsValid = false;
				} else {
					String[] idParts = idValue.split(":");
					String idNid = idParts[0];
					String idType = idParts[1];
					String idScheme = idParts[2];
					String idSSID = idValue.split(":" + idScheme + ":")[1];
					validateIdScheme(idScheme, targetEl);
					validateIdSsid(idSSID, idScheme, targetEl);
					validateIdType(idType, idKey, targetEl);
				}
			}
		}
		idSets.put(elementName, idSet);
		id2XmlMappings.put(elementName, id2XmlMap);
		idXRefCounts.put(elementName, idXRefCounter);
		return idSet;
	}

	/**
	 * @param idType
	 * @param targetEl
	 */
	private void validateIdType(String idType, String idKey, Element targetEl) {
		/*
		 * Check syntax of the 'type' as defined in Best Practices Section 3.1.7
		 */
		String type = id2typeMap.get(idKey);
		if (type == null) {
			type = idKey.toLowerCase();
		}
		if (!idType.equals(type)) {
			LogReference srcRef = LogReference.getRef("MMM-BP", "1.0", "mmbp01.3");
			String msg = "ID <type> does not conform to recommendation (i.e. '" + type + "')";
			logIssue(LogMgmt.TAG_BEST, LogMgmt.LEV_WARN, targetEl, msg, null, srcRef, logMsgSrcId);
		}

	}

	/**
	 * @param idSchema
	 * @param targetEl
	 */
	private void validateIdScheme(String idScheme, Element targetEl) {
		if (!idScheme.startsWith("eidr")) {
			String msg = "Use of EIDR identifiers is recommended";
			LogReference srcRef = LogReference.getRef("MMM-BP", "1.0", "mmbp01.1");
			logIssue(LogMgmt.TAG_BEST, LogMgmt.LEV_WARN, targetEl, msg, null, srcRef, logMsgSrcId);
		}
	}

	private void validateIdSsid(String idSSID, String idScheme, Element targetEl) {
		String idPattern = null;
		LogReference srcRef = null;
		switch (idScheme) {
		case "eidr":
			srcRef = LogReference.getRef("MMM-BP", "1.0", "mmbp01.2");
			String msg = "Use of EIDR-x or EIDR-s identifiers is recommended";
			logIssue(LogMgmt.TAG_BEST, LogMgmt.LEV_WARN, targetEl, msg, null, srcRef, logMsgSrcId);

			srcRef = null;
			idPattern = "10\\.[\\d]{4}/[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-Z]";
			break;
		case "eidr-s":
			srcRef = LogReference.getRef("EIDR-IDF", "1.3", "eidr01-s");
			idPattern = "[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-Z]";
			break;
		case "eidr-x":
			srcRef = LogReference.getRef("EIDR-IDF", "1.3", "eidr01-x");
			idPattern = "[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-Z]:[\\S]+";
			break;
		default:
			msg = "ID uses scheme '" + idScheme + "', SSID format will not be verified";
			logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_INFO, targetEl, msg, "ssid='" + idSSID + "'", null, logMsgSrcId);
			return;
		}
		boolean match = idSSID.matches(idPattern);
		if (!match) {
			String msg = "Invalid SSID syntax for " + idScheme + " scheme";
			logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, targetEl, msg, "ssid='" + idSSID + "'", srcRef, logMsgSrcId);
			curFileIsValid = false;
		}
	}

	/**
	 * 
	 */
	private void checkForOrphans() {
		// Map<String, Map<String, XrefCounter>> idXRefCounts;
		Iterator<Map<String, XrefCounter>> allCOunters = idXRefCounts.values().iterator();
		while (allCOunters.hasNext()) {
			// get the type-specific collection of counters...
			Map<String, XrefCounter> nextCSet = allCOunters.next();
			// now iterate thru the instance-specific counts..
			Iterator<XrefCounter> typeCounters = nextCSet.values().iterator();
			while (typeCounters.hasNext()) {
				XrefCounter nextCount = typeCounters.next();
				nextCount.validate();
			}
		}

	}

	/**
	 * @return
	 */
	private boolean validateAvailVocab() {
		boolean allOK = true;
		Namespace primaryNS = availsNSpace;
		String doc = "AVAIL";
		String docVer = "2.1";

		JSONArray allowed = availVocab.optJSONArray("AvailType");
		LogReference srcRef = LogReference.getRef(doc, docVer, "avail01");
		allOK = allOK && validateVocab(primaryNS, "Avail", primaryNS, "AvailType", allowed, srcRef);

		allowed = availVocab.optJSONArray("EntryType");
		srcRef = LogReference.getRef(doc, docVer, "avail02");
		allOK = allOK && validateVocab(primaryNS, "Disposition", primaryNS, "EntryType", allowed, srcRef);

		allowed = availVocab.optJSONArray("AltIdentifier@scope");
		srcRef = LogReference.getRef(doc, docVer, "avail03");
		allOK = allOK && validateVocab(primaryNS, "AltIdentifier", primaryNS, "@scope", allowed, srcRef);

		allowed = availVocab.optJSONArray("LocalizationOffering");
		srcRef = LogReference.getRef(doc, docVer, "avail03");
		allOK = allOK && validateVocab(primaryNS, "Metadata", primaryNS, "LocalizationOffering", allowed, srcRef);
		allOK = allOK
				&& validateVocab(primaryNS, "EpisodeMetadata", primaryNS, "LocalizationOffering", allowed, srcRef);

		allowed = availVocab.optJSONArray("SeasonStatus");
		srcRef = LogReference.getRef(doc, docVer, "avail04");
		allOK = allOK && validateVocab(primaryNS, "SeasonMetadata", primaryNS, "SeasonStatus", allowed, srcRef);

		allowed = availVocab.optJSONArray("SeriesStatus");
		srcRef = LogReference.getRef(doc, docVer, "avail05");
		allOK = allOK && validateVocab(primaryNS, "SeriesMetadata", primaryNS, "SeriesStatus", allowed, srcRef);

		allowed = availVocab.optJSONArray("DateTimeCondition");
		srcRef = LogReference.getRef(doc, docVer, "avail06");
		allOK = allOK && validateVocab(primaryNS, "Transaction", primaryNS, "StartCondition", allowed, srcRef);
		allOK = allOK && validateVocab(primaryNS, "Transaction", primaryNS, "EndCondition", allowed, srcRef);

		allowed = availVocab.optJSONArray("LicenseType");
		srcRef = LogReference.getRef(doc, docVer, "avail07");
		allOK = allOK && validateVocab(primaryNS, "Transaction", primaryNS, "LicenseType", allowed, srcRef);

		allowed = availVocab.optJSONArray("LicenseRightsDescription");
		srcRef = LogReference.getRef(doc, docVer, "avail07");
		allOK = allOK
				&& validateVocab(primaryNS, "Transaction", primaryNS, "LicenseRightsDescription", allowed, srcRef);

		allowed = availVocab.optJSONArray("FormatProfile");
		srcRef = LogReference.getRef(doc, docVer, "avail07");
		allOK = allOK && validateVocab(primaryNS, "Transaction", primaryNS, "FormatProfile", allowed, srcRef);

		allowed = availVocab.optJSONArray("ExperienceCondition");
		srcRef = LogReference.getRef(doc, docVer, "avail07");
		allOK = allOK && validateVocab(primaryNS, "Transaction", primaryNS, "ExperienceCondition", allowed, srcRef);

		allowed = availVocab.optJSONArray("Term@termName");
		srcRef = LogReference.getRef(doc, docVer, "avail08");
		allOK = allOK && validateVocab(primaryNS, "Term", primaryNS, "@termName", allowed, srcRef);

		allowed = availVocab.optJSONArray("SharedEntitlement@ecosystem");
		srcRef = LogReference.getRef(doc, docVer, "avail09");
		allOK = allOK && validateVocab(primaryNS, "SharedEntitlement", primaryNS, "@ecosystem", allowed, srcRef);
		return allOK;
	}

	/**
	 * @return
	 */
	protected boolean validateCMVocab() {
		boolean allOK = true;
		String mdVersion = "2.4";
		Namespace primaryNS = availsNSpace;

		// JSONArray allowed = cmVocab.optJSONArray("WorkType");
		// LogReference srcRef = LogReference.getRef("CM", mdVersion, "cm002");
		// allOK = allOK && validateVocab(primaryNS, "BasicMetadata", mdNSpace,
		// "WorkType", allowed, srcRef);
		//
		// allowed = cmVocab.optJSONArray("ColorType");
		// srcRef = LogReference.getRef("CM", mdVersion, "cm003");
		// allOK = allOK && validateVocab(primaryNS, "BasicMetadata", mdNSpace,
		// "PictureColorType", allowed, srcRef);

		return allOK;
	}

	/**
	 * @param primaryNS
	 * @param primaryEl
	 * @param childNS
	 * @param child
	 * @param expected
	 * @param srcRef
	 * @return
	 */
	protected boolean validateVocab(Namespace primaryNS, String primaryEl, Namespace childNS, String child,
			JSONArray expected, LogReference srcRef) {
		XPathExpression<Element> xpExpression = xpfac.compile(".//" + primaryNS.getPrefix() + ":" + primaryEl,
				Filters.element(), null, primaryNS);
		List<Element> elementList = xpExpression.evaluate(curRootEl);
		boolean allOK = true;
		int tag4log = LogMgmt.TAG_N_A;
		Namespace tagNS;
		if (childNS != null) {
			tagNS = childNS;
		} else {
			tagNS = primaryNS;
		}
		if (tagNS == availsNSpace) {
			tag4log = LogMgmt.TAG_AVAIL;
		} else if (tagNS == mdNSpace) {
			tag4log = LogMgmt.TAG_MD;
		}
		for (int i = 0; i < elementList.size(); i++) {
			Element targetEl = (Element) elementList.get(i);
			if (!child.startsWith("@")) {
				Element subElement = targetEl.getChild(child, childNS);
				if (subElement != null) {
					String text = subElement.getTextNormalize();
					if (!expected.contains(text)) {
						String explanation = "Values are case-sensitive";
						String msg = "Unrecognized value for " + primaryEl + "/" + child;
						// TODO: Is this ERROR or WARNING???
						logIssue(tag4log, LogMgmt.LEV_ERR, subElement, msg, explanation, srcRef, logMsgSrcId);
						allOK = false;
						curFileIsValid = false;
					}
				}
			} else {
				String targetAttb = child.replaceFirst("@", "");
				String text = targetEl.getAttributeValue(targetAttb);
				if (!expected.contains(text)) {
					String explanation = "Values are case-sensitive";
					String msg = "Unrecognized value for attribute " + child;
					// TODO: Is this ERROR or WARNING???
					logIssue(tag4log, LogMgmt.LEV_ERR, targetEl, msg, explanation, srcRef, logMsgSrcId);
					allOK = false;
					curFileIsValid = false;
				}
			}
		}
		return allOK;
	}

	/**
	 * Log an issue after first determining the appropriate <i>target</i> to
	 * associate with the log entry. The target indicates a construct within a
	 * file being validated and should be specified as either
	 * <ul>
	 * <li>an JDOM Element within an XML file, or</tt>
	 * <li>a <tt>POI Cell</tt> instance used to identify a cell in an XLSX
	 * spreadsheet.</li>
	 * </ul>
	 * Note that validation of XLSX files is only supported by the Avails
	 * validator and that other validator classes do not, therefore, require
	 * this intermediate stage when logging.
	 * 
	 * 
	 * @param tag
	 * @param level
	 * @param xmlElement
	 * @param msg
	 * @param explanation
	 * @param srcRef
	 * @param moduleId
	 */
	protected void logIssue(int tag, int level, Element xmlElement, String msg, String explanation, LogReference srcRef,
			String moduleId) {
		Object target = null;
		if (pedigreeMap == null) {
			target = xmlElement;
		} else {
			Pedigree ped = pedigreeMap.get(xmlElement);
			if (ped != null) {
				target = ped.getSource();
			}
		}
		loggingMgr.logIssue(tag, level, target, msg, explanation, srcRef, moduleId);
	}
}
