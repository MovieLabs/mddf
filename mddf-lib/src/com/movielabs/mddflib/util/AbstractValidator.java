/**
 * Created Oct 30, 2015 
 * Copyright Motion Picture Laboratories, Inc. 2015
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
package com.movielabs.mddflib.util;

import java.io.File;
import java.io.IOException;
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

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * Validates a XML file for conformance with the <tt>Common Metadata (md)</tt>
 * specification as defined in <tt>TR-META-CM (v2.4)</tt>. Extending subclasses
 * will provide additional customization to check for conformance with
 * specifications built on top of the CM (e.g., as conforming to the Common
 * Media Manifest (CMM) as specified in <tt>TR-META-MMM (v1.5)</tt>)
 * 
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public abstract class AbstractValidator extends XmlIngester {

	/**
	 * @param loggingMgr
	 */
	public AbstractValidator(LogMgmt loggingMgr) {
		super(loggingMgr);
		// TODO Auto-generated constructor stub
	}

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
				logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_WARN, targetEl, msg, explanation, null, logMsgSrcId);
			}
		}

	}

	public static final String LOGMSG_ID = "ManifestValidator";

	protected static HashMap<String, String> id2typeMap;
	protected static JSONObject cmVocab;
	
	static{

		try {
			cmVocab = loadVocab(vocabRsrcPath, "CM");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// protected int logTag;
	protected Namespace rootNS;
	protected String rootPrefix;

	protected boolean validateC;
	protected Element curRootEl;
	protected boolean curFileIsValid;
	protected Map<String, HashSet<String>> idSets;
	protected Map<String, Map<String, XrefCounter>> idXRefCounts;
	protected Map<String, Map<String, Element>> id2XmlMappings;

	/**
	 * @param validateC
	 */
	public AbstractValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;
		logMsgSrcId = LOGMSG_ID;
	}

	public abstract boolean process(File xmlManifestFile) throws IOException, JDOMException;

	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateConstraints() {
		idSets = new HashMap<String, HashSet<String>>();
		idXRefCounts = new HashMap<String, Map<String, XrefCounter>>();
		id2XmlMappings = new HashMap<String, Map<String, Element>>();

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
		XPathExpression<Element> xpExpression = xpfac.compile(".//manifest:" + parentName, Filters.element(), null,
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

	protected void validateNotEmpty(String key) {

		String[] parts = key.split("@");
		String elementName = parts[0];
		XPathExpression<Element> xpExpression = xpfac.compile(".// " + rootPrefix + elementName, Filters.element(),
				null, rootNS);
		String label = elementName;
		String attributeName = null;
		if (parts.length > 1) {
			attributeName = parts[1];
			label = label + "[@" + attributeName + "]";
		}
		List<Element> elementList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < elementList.size(); i++) {
			Element targetEl = (Element) elementList.get(i);
			String value = null;
			if (attributeName == null) {
				value = targetEl.getTextNormalize();
			} else {
				value = targetEl.getAttributeValue(attributeName);
			}
			if ((value == null) || (value.isEmpty())) {
				String msg = label + " not specified. A value must be provided";
				logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, targetEl, msg, null, null, logMsgSrcId);
				curFileIsValid = false;
			}
		}
	}

	// ..................

	/**
	 * Check for the presence of an ID attribute and, if provided, verify it is
	 * unique.
	 * 
	 * @param elementName
	 * @param idAttribute
	 * @return the <tt>Set</tt> of unique IDs found.
	 */
	protected HashSet<String> validateId(String elementName, String idAttribute, boolean reqUniqueness,
			boolean chkSyntax) {
		XPathExpression<Element> xpExpression = xpfac.compile(".// " + rootPrefix + elementName, Filters.element(),
				null, rootNS);
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
				String msg = idAttribute + " not specified. References to this " + elementName
						+ " will not be supportable.";
				logIssue(logMsgDefaultTag, LogMgmt.LEV_WARN, targetEl, msg, null, null, logMsgSrcId);
			} else {
				if (!idSet.add(idValue)) {
					LogReference srcRef = LogReference.getRef("CM", "2.4", "cm001a");
					String msg = idAttribute + " is not unique";
					int msgLevel;
					if (reqUniqueness) {
						msgLevel = LogMgmt.LEV_ERR;
						curFileIsValid = false;
					} else {
						msgLevel = LogMgmt.LEV_WARN;
					}
					logIssue(logMsgDefaultTag, msgLevel, targetEl, msg, null, srcRef, logMsgSrcId);
				}
				if (chkSyntax) {
					/*
					 * Validate identifier structure conforms with Sec 2.1 of
					 * Common Metadata spec (v2.4)
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
						validateIdType(idType, idAttribute, targetEl);
					}
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
	protected void validateIdType(String idType, String idAttribute, Element targetEl) {
		/*
		 * Check syntax of the 'type' as defined in Best Practices Section 3.1.7
		 */
		String type = id2typeMap.get(idAttribute);
		if (type == null) {
			type = idAttribute.toLowerCase();
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
	protected void validateIdScheme(String idScheme, Element targetEl) {
		if (!idScheme.startsWith("eidr")) {
			String msg = "Use of EIDR identifiers is recommended";
			LogReference srcRef = LogReference.getRef("MMM-BP", "1.0", "mmbp01.1");
			logIssue(LogMgmt.TAG_BEST, LogMgmt.LEV_WARN, targetEl, msg, null, srcRef, logMsgSrcId);
		}
	}

	protected void validateIdSsid(String idSSID, String idScheme, Element targetEl) {
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
			logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_INFO, targetEl, msg, "ssid='" + idSSID + "'", null, logMsgSrcId);
			return;
		}
		boolean match = idSSID.matches(idPattern);
		if (!match) {
			String msg = "Invalid SSID syntax for " + idScheme + " scheme";
			logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, targetEl, msg, "ssid='" + idSSID + "'", srcRef, logMsgSrcId);
			curFileIsValid = false;
		}
	}

	/**
	 * @param srcElement
	 * @param xrefElement
	 * @param targetElType
	 */
	protected void validateXRef(String srcElement, String xrefElement, String targetElType) {
		HashSet<String> idSet = idSets.get(targetElType);
		Map<String, XrefCounter> idXRefCounter = idXRefCounts.get(targetElType);
		XPathExpression<Element> xpExpression = xpfac.compile(".//manifest:" + srcElement + "/manifest:" + xrefElement,
				Filters.element(), null, manifestNSpace);
		List<Element> elementList = xpExpression.evaluate(curRootEl);
		String debugMsg = "Found " + elementList.size() + " cross-refs by a " + srcElement + " to a " + targetElType;
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_MANIFEST, debugMsg, curFile, LOGMSG_ID);
		for (int i = 0; i < elementList.size(); i++) {
			Element refEl = (Element) elementList.get(i);
			String targetId = refEl.getTextNormalize();
			if (!idSet.contains(targetId)) {
				String msg = "Invalid cross-reference: the referenced " + targetElType
						+ " is not defined in this manifest";
				logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, refEl, msg, null, null, logMsgSrcId);
				curFileIsValid = false;
			} else {
				XrefCounter count = idXRefCounter.get(targetId);
				if (count == null) {
					System.out.println("TRAPPED :" + targetId);
					count = new XrefCounter(targetElType, targetId);
					idXRefCounter.put(targetId, count);
				}
				count.increment();
			}
		}
	}

	/**
	 * 
	 */
	protected void checkForOrphans() {
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
	protected abstract boolean validateCMVocab();

	/**
	 * @param primaryNS
	 * @param primaryEl
	 * @param childNS
	 * @param child
	 * @param expected
	 * @param srcRef
	 * @param caseSensitive
	 * @return
	 */
	protected boolean validateVocab(Namespace primaryNS, String primaryEl, Namespace childNS, String child,
			JSONArray expected, LogReference srcRef, boolean caseSensitive) {
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
		if (tagNS == manifestNSpace) {
			tag4log = LogMgmt.TAG_MANIFEST;
		} else if (tagNS == availsNSpace) {
			tag4log = LogMgmt.TAG_AVAIL;
		} else if (tagNS == mdNSpace) {
			tag4log = LogMgmt.TAG_MD;
		}
		String optionsString = expected.toString().toLowerCase();
		for (int i = 0; i < elementList.size(); i++) {
			String text = null;
			String errMsg = null;
			Element logMsgEl = null;
			Element targetEl = (Element) elementList.get(i);
			if (!child.startsWith("@")) {
				Element subElement = targetEl.getChild(child, childNS);
				logMsgEl = subElement;
				if (subElement != null) {
					text = subElement.getTextNormalize();
					errMsg = "Unrecognized value '" + text + "' for " + primaryEl + "/" + child; 
				}
			} else {
				String targetAttb = child.replaceFirst("@", "");
				text = targetEl.getAttributeValue(targetAttb);
				logMsgEl = targetEl;
				errMsg = "Unrecognized value '" + text + "' for attribute " + child; 
			}
			if (text != null) {
				if (caseSensitive) {
					if (!expected.contains(text)) {
						String explanation = "Values are case-sensitive";
						// TODO: Is this ERROR or WARNING???
						logIssue(tag4log, LogMgmt.LEV_ERR, logMsgEl, errMsg, explanation, srcRef, logMsgSrcId);
						allOK = false;
						curFileIsValid = false;

					}
				} else {
					String checkString = "\"" + text.toLowerCase() + "\"";
					if (!optionsString.contains(checkString)) {
						// System.out.println(checkString + " not in " +
						// optionsString);
						logIssue(tag4log, LogMgmt.LEV_ERR, logMsgEl, errMsg, null, srcRef, logMsgSrcId);
						allOK = false;
						curFileIsValid = false;
					}
				}
			}
		}
		return allOK;
	}

	protected void logIssue(int tag, int level, Element xmlElement, String msg, String explanation, LogReference srcRef,
			String moduleId) {
		loggingMgr.logIssue(tag, level, xmlElement, msg, explanation, srcRef, moduleId);
	}
}
