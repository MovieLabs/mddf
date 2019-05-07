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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.CMValidator.SeqEntry;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.RatingSystem;
import com.movielabs.mddflib.util.xml.SchemaWrapper;
import com.movielabs.mddflib.util.xml.StructureValidation;
import com.movielabs.mddflib.util.xml.XsdValidation;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * Base class for implementation of validators of a XML file for conformance
 * with the <tt>Common Metadata (md)</tt> specifications. Extending subclasses
 * will provide additional customization to check for conformance with
 * specifications built on top of the CM (e.g., as conforming to the Common
 * Media Manifest (CMM)).
 * 
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public abstract class CMValidator extends XmlIngester {

	/**
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	public class SeqEntry implements Comparable {

		private Element seqEl;
		private int value;

		/**
		 * @param index
		 * @param nextChildEl
		 */
		public SeqEntry(int value, Element seqEl) {
			this.value = value;
			this.seqEl = seqEl;
		}

		Element getSeqEl() {
			return seqEl;
		}

		int getValue() {
			return value;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(Object other) {
			int result = this.value - ((SeqEntry) other).getValue();
			return result;
		}
	}

	/**
	 * Used to facilitate keeping track of cross-references and identifying 'orphan'
	 * elements.
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
		}

		int increment() {
			count++;
			return count;
		}

		void validate() {
			if (count > 0) {
				return;
			} else {
				Map<String, Element> idMap = id2XmlMappings.get(elType);
				Element targetEl = idMap.get(elId);
				String explanation = "The element is never referenced by it's ID";
				String msg = "Unreferenced <" + elType + "> Element";
				logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_WARN, targetEl, msg, explanation, null, logMsgSrcId);
			}
		}

	}

	public static final String LOGMSG_ID = "AbstractValidator";

	protected HashMap<String, String> id2typeMap;

	private static Properties iso3166_1_codes;
	private static Properties iso4217_codes;

	private static HashSet<String> specialRatings = new HashSet<String>();

	private static JSONArray iso639_2;

	private static JSONArray iso639_3;

	private static JSONArray unM49;

	private static JSONArray rfc5646Variant;

	private static JSONArray rfc5646Script;

	static {
		specialRatings.add("ALL");
		specialRatings.add("UNRATED");
		specialRatings.add("ADULT");
		specialRatings.add("PROSCRIBED");
		try {
			/*
			 * Language codes are in their own file
			 */
			JSONObject jsonRsrc = getMddfResource("rfc5646");
			JSONObject rfc5646 = jsonRsrc.getJSONObject("rfc5646");
			iso639_2 = rfc5646.getJSONArray("iso639-2");
			iso639_3 = rfc5646.getJSONArray("iso639-3");
			unM49 = rfc5646.getJSONArray("UN-M49");
			rfc5646Variant = rfc5646.getJSONArray("variant");
			rfc5646Script = rfc5646.getJSONArray("script");
			/*
			 * ISO country and currency codes are simple so we use Properties
			 */
			String isoRsrcPath = MddfContext.RSRC_PATH + "ISO3166-1.properties";
			iso3166_1_codes = loadProperties(isoRsrcPath);
			isoRsrcPath = MddfContext.RSRC_PATH + "ISO4217.properties";
			iso4217_codes = loadProperties(isoRsrcPath);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected Namespace rootNS;

	protected boolean validateC;
	protected Element curRootEl;
	/**
	 * Set to <tt>true</tt> when starting validation of a file, then set to false
	 * when any error is detected.
	 */
	protected boolean curFileIsValid;
	protected Map<String, HashSet<String>> idSets;
	protected Map<String, Map<String, XrefCounter>> idXRefCounts;
	protected Map<String, Map<String, Element>> id2XmlMappings;

	protected XsdValidation xsdHelper;

	protected StructureValidation structHelper;

	/**
	 * @param loggingMgr
	 */
	public CMValidator(LogMgmt loggingMgr) {
		super(loggingMgr);
		xsdHelper = new XsdValidation(loggingMgr);
		structHelper = new StructureValidation(this, logMsgSrcId);
	}

	/**
	 * @param validateC
	 */
	public CMValidator(boolean validateC, LogMgmt loggingMgr) {
		this(loggingMgr);
		this.validateC = validateC;
		logMsgSrcId = LOGMSG_ID;
	}
	
	public abstract boolean process(MddfTarget target) throws IOException, JDOMException;

	/**
	 * Validate everything that is not fully specified via the XSD. This method
	 * should be invoked when validating MEC and Manifest files. Avails has it's own
	 * unique Metadata schema and therefore does not need to invoke this method.
	 */
	protected void validateConstraints() {
		validateIdSet();
		validateCountries();
		validateLanguageCodes();
		validateCurrencyCodes();
		validateRatings();
	}

	/**
	 * Validates any terminology used in conjunction with
	 * <tt>md:BasicMetadata-type</tt>
	 */
	protected void validateBasicMetadata() {
		/*
		 * handle any case of backwards (or forwards) compatibility between versions.
		 */
		String vocabVer = CM_VER;
		switch (CM_VER) { 
		case "2.7.1":
			vocabVer = "2.7";
			break; 
		}
		JSONObject cmVocab = (JSONObject) getVocabResource("cm", vocabVer);
		if (cmVocab == null) { 
			String msg = "Unable to validate controlled vocab: missing resource file vocab_cm_v"+vocabVer;
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_MANIFEST, msg, curFile, logMsgSrcId);
			curFileIsValid = false;
			return;
		}

		JSONArray expectedValues = cmVocab.optJSONArray("WorkType");
		LogReference docRef = LogReference.getRef("CM", CM_VER, "cm002");
		validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "WorkType", expectedValues, docRef, true, true);
		validateVocab(mdNSpace, "Work", mdNSpace, "WorkType", expectedValues, docRef, true, true);

		expectedValues = cmVocab.optJSONArray("ColorType");
		docRef = LogReference.getRef("CM", CM_VER, "cm003");
		validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "PictureColorType", expectedValues, docRef, true,
				true);

		expectedValues = cmVocab.optJSONArray("PictureFormat");
		docRef = LogReference.getRef("CM", CM_VER, "cm004");
		validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "PictureFormat", expectedValues, docRef, true, true);

		expectedValues = cmVocab.optJSONArray("ReleaseType");
		docRef = LogReference.getRef("CM", CM_VER, "cm005");
		validateVocab(mdNSpace, "ReleaseHistory", mdNSpace, "ReleaseType", expectedValues, docRef, true, true);

		expectedValues = cmVocab.optJSONArray("TitleAlternate@type");
		docRef = LogReference.getRef("CM", CM_VER, "cm006");
		validateVocab(mdNSpace, "TitleAlternate", null, "@type", expectedValues, docRef, true, true);

		expectedValues = cmVocab.optJSONArray("Parent@relationshipType");
		docRef = LogReference.getRef("CM", CM_VER, "cm007");
		validateVocab(mdNSpace, "Parent", null, "@relationshipType", expectedValues, docRef, true, true);

		expectedValues = cmVocab.optJSONArray("EntryClass");
		docRef = LogReference.getRef("CM", CM_VER, "cm008");
		validateVocab(mdNSpace, "Entry", mdNSpace, "EntryClass", expectedValues, docRef, true, true);

		expectedValues = cmVocab.optJSONArray("Parent@relationshipType");
		docRef = LogReference.getRef("CM", CM_VER, "cm007");
		validateVocab(manifestNSpace, "ExperienceChild", manifestNSpace, "Relationship", expectedValues, docRef, true,
				true);

		expectedValues = cmVocab.optJSONArray("Compliance/Disposition");
		docRef = LogReference.getRef("CM", CM_VER, "cm_disp");
		validateVocab(mdNSpace, "Compliance", mdNSpace, "Disposition", expectedValues, docRef, true, true);

		expectedValues = cmVocab.optJSONArray("Gender");
		docRef = LogReference.getRef("CM", CM_VER, "cm_gender");
		validateVocab(mdNSpace, "People", mdNSpace, "Gender", expectedValues, docRef, true, true);

		expectedValues = cmVocab.optJSONArray("GroupingEntity/Type");
		docRef = LogReference.getRef("CM", "cm_gType");
		validateVocab(mdNSpace, "GroupingEntity", mdNSpace, "Type", expectedValues, docRef, true, false);

		expectedValues = cmVocab.optJSONArray("Relationship/Type");
		docRef = LogReference.getRef("CM", "cm_gType");
		validateVocab(mdNSpace, "Relationship", mdNSpace, "Type", expectedValues, docRef, true, false);

	}

	/**
	 * 
	 */
	protected void validateIdSet() {
		idSets = new HashMap<String, HashSet<String>>();
		idXRefCounts = new HashMap<String, Map<String, XrefCounter>>();
		id2XmlMappings = new HashMap<String, Map<String, Element>>();
	}

	/**
	 * Check for consistent usage. This typically means that an OPTIONAL element
	 * will be either REQUIRED or INVALID for certain use-cases (e.g. BundledAsset
	 * is only allowed when WorkType is 'Collection').
	 * <p>
	 * Validation is based primarily on the <i>structure definitions</i> defined in
	 * a version-specific JSON file. This will define various criteria that must be
	 * satisfied for a given usage. The criteria are specified in the form of
	 * XPATHs.
	 * </p>
	 * 
	 * @see com.movielabs.mddflib.util.xml.StructureValidation
	 */
	protected void validateUsage() {

		/*
		 * Load JSON that defines various constraints on structure of the XML This is
		 * version-specific but not all schema versions have their own unique struct
		 * file (e.g., a minor release may be compatible with a previous release).
		 */
		String structVer = null;
		switch (CM_VER) {
		case "2.7":
		case "2.6":
		case "2.5":
			structVer = CM_VER;
			break;
		default:
			// Not supported for the version
			return;
		}

		JSONObject structDefs = XmlIngester.getMddfResource("structure_cm", structVer);
		if (structDefs == null) {
			// LOG a FATAL problem.
			String msg = "Unable to process; missing structure definitions for Common Metadata v" + CM_VER;
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_MD, msg, curFile, logMsgSrcId);
			return;
		}

		JSONObject rqmtSet = structDefs.getJSONObject("StrucRqmts");
		Iterator<String> keys = rqmtSet.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject rqmtSpec = rqmtSet.getJSONObject(key);
			// NOTE: This block of code requires a 'targetPath' be defined
			if (rqmtSpec.has("targetPath")) {
				loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_MD, "Structure check; key= " + key, curFile, logMsgSrcId);
				curFileIsValid = structHelper.validateDocStructure(curRootEl, rqmtSpec) && curFileIsValid;
			}
		}

		return;
	}

	/**
	 * 
	 */
	private void validateCardinality() {
		/*
		 * Sec 4.2.7: At least one instance of ContainerReference or BasicMetadata must
		 * be included for each Inventory/Metadata instance
		 */

	}

	/**
	 * Validate sequencing of a set of <tt>targetEl</tt> where the set consists of
	 * all targets belonging to a specific parentEl. *
	 * <p>
	 * In most cases indexing is optional. If it is a required attribute and is
	 * missing then the XSD schema validation will fail before reaching this part of
	 * the code. So in the current context of this code block we can consider
	 * indexing to always be optional. HOWEVER if ANY child of a specific parent has
	 * an index value specified then ALL child elements of that parent MUST also
	 * specify an index value.
	 * </p>
	 * 
	 * Thus
	 * 
	 * <pre>
	 * for each parentEl  containing a targetEl with the index attribute present:
	 *    Set targetSet = all targetEl with (parent == parentEl)
	 *    validate sequencing/indexing of <tt>targetSet</tt>
	 * </pre>
	 * 
	 * @param targetEl
	 * @param targetNSpace
	 * @param idxAttribute
	 * @param parentEl
	 * @param parentNSpace
	 * @param unique       if <tt>true</tt> values must be unique
	 * @param negOK        if <tt>true</tt> non-negative values are allowed
	 * @param continuous   if <tt>true</tt> sequence range must be continuous
	 * @param inclZero     if <tt>true</tt> sequence must include a zero value
	 */
	protected void validateIndexing(String targetEl, Namespace targetNSpace, String idxAttribute, String parentEl,
			Namespace parentNSpace, boolean unique, boolean negOK, boolean continuous, boolean inclZero) {
		String childPath = targetNSpace.getPrefix() + ":" + targetEl + "[@" + idxAttribute + "]";
		String xpath = ".//" + parentNSpace.getPrefix() + ":" + parentEl + "[" + childPath + "]";
		XPathExpression<Element> xpExpression = xpfac.compile(xpath, Filters.element(), null, parentNSpace,
				targetNSpace);
		List<Element> parentElList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < parentElList.size(); i++) {
			Element nextParent = (Element) parentElList.get(i);

			List<Element> childList = nextParent.getChildren(targetEl, targetNSpace);
			if (childList.isEmpty()) {
				continue;
			}
			SeqEntry[] indexValues = new SeqEntry[childList.size()];
			Arrays.fill(indexValues, null);
			int ivCnt = 0;
			for (int cPtr = 0; cPtr < childList.size(); cPtr++) {
				Element nextChildEl = childList.get(cPtr);
				/*
				 * Each child Element must have @index attribute
				 */
				String indexAsString = nextChildEl.getAttributeValue(idxAttribute);
				if (!isValidIndex(indexAsString, negOK)) {
					String msg = "Invalid value for indexing attribute";
					if (!negOK) {
						msg = msg + " (non-negative integer required)";
					}
					logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, nextChildEl, msg, null, null, logMsgSrcId);
					curFileIsValid = false;
				} else {
					int index = Integer.parseInt(indexAsString);
					SeqEntry next = new SeqEntry(index, nextChildEl);
					indexValues[ivCnt++] = next;
				}
			}
			// now make sure all constraints are met
			Arrays.sort(indexValues);
			boolean hasZero = false;
			for (int ptr = 0; ptr < indexValues.length; ptr++) {
				SeqEntry next = indexValues[ptr];
				if (next.getValue() == 0) {
					hasZero = true;
				}
				if (ptr > 0) {
					SeqEntry previous = indexValues[ptr - 1];
					if (unique) {
						if (previous.getValue() == next.getValue()) {
							String msg = "Duplicate value (must be unique within sequence)";
							logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, next.getSeqEl(), msg, null, null, logMsgSrcId);
							curFileIsValid = false;
						}
					}
					if (continuous) {
						if (previous.getValue() != (next.getValue() - 1)) {
							String msg = "Invalid indexing of " + targetEl + " sequence: must be continuous";
							logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, next.getSeqEl(), msg, null, null, logMsgSrcId);
							curFileIsValid = false;
						}
					}
				}
			}
			if (inclZero && !hasZero) {
				String msg = "Invalid indexing of " + targetEl + " sequence: ZERO index value not defined";
				logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, nextParent, msg, null, null, logMsgSrcId);
				curFileIsValid = false;

			}
		}
	}

	/**
	 * returns true if string defines a non-negative integer value
	 * 
	 * @param indexValue
	 * @return
	 */
	public boolean isValidIndex(String indexValue, boolean negOK) {
		try {
			int iv = Integer.parseInt(indexValue);
			if (negOK) {
				return true;
			} else {
				return (iv >= 0);
			}
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Check all Elements (or attributes) that are REQUIRED by a MDDF Specification
	 * to provide a value actually do. This is a necessary step as validation
	 * against the XSD only verify the presence of an Element. Thus, an XML file
	 * with the Element <tt>&lt;Foo&gt;&lt;/Foo&gt;</tt> would pass the XSD check
	 * even though it fails to provide a required value.
	 * 
	 * @param targetSchema
	 */
	protected void validateNotEmpty(SchemaWrapper targetSchema) {
		List<XPathExpression<?>> reqElXpList = targetSchema.getReqElList();
		for (XPathExpression<?> xpExp : reqElXpList) {
			List<?> elementList = xpExp.evaluate(curRootEl);
			for (Object next : elementList) {
				String value = null;
				String label = null;
				Element targetEl = null;
				if (next instanceof Element) {
					targetEl = (Element) next;
					value = targetEl.getTextNormalize();
					label = targetEl.getName();
				} else if (next instanceof Attribute) {
					Attribute targetAtt = (Attribute) next;
					value = targetAtt.getValue();
					targetEl = targetAtt.getParent();
					label = targetEl.getName() + "->" + targetAtt.getName();
				}

				if ((value == null) || (value.isEmpty())) {
					String msg = label + " not specified. A value must be provided";
					logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, targetEl, msg, null, null, logMsgSrcId);
					curFileIsValid = false;
				}
			}
		}
	}

	// ..................

	/**
	 * Check for the presence of an ID and, if provided, verify it is unique and has
	 * the correct structure and syntax as defined in Section 3 of
	 * <tt>Manifest/Avails Delivery Best Practices (BP-META-MMMD)</tt>. All
	 * extracted ID values will be saved as an <tt>idSet</tt> that can latter be
	 * used to validate cross-references.
	 * <p>
	 * The value of the ID is extracted using the <tt>idElement</tt> and (optional)
	 * <tt>idAttribute</tt> arguments. If the <tt>idAttribute</tt> is <tt>null</tt>
	 * the value used is the idElement's text.
	 * </p>
	 * 
	 * @param idElement
	 * @param idAttribute   (optional)
	 * @param reqUniqueness
	 * @param chkSyntax
	 * @return the <tt>Set</tt> of unique IDs found.
	 */
	protected HashSet<String> validateId(String idElement, String idAttribute, boolean reqUniqueness,
			boolean chkSyntax) {
		XPathExpression<Element> xpExpression = xpfac.compile(".//" + rootNS.getPrefix() + ":" + idElement,
				Filters.element(), null, rootNS);
		HashSet<String> idSet = new HashSet<String>();

		/*
		 * idXRefCounter is initialized here but will 'filled in' by validateXRef();
		 */
		HashMap<String, XrefCounter> idXRefCounter = new HashMap<String, XrefCounter>();
		/*
		 * id2XmlMap is provided for look-ups later as an alternative to using XPaths.
		 */
		HashMap<String, Element> id2XmlMap = new HashMap<String, Element>();

		List<Element> elementList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < elementList.size(); i++) {
			/*
			 * XSD may specify ID attribute as OPTIONAL but we need to verify
			 * cross-references and uniqueness.
			 */
			Element targetEl = (Element) elementList.get(i);
			if (targetEl.getParentElement().getName().equals("Audiovisual")) {
				// special case... do nothing
				continue;
			}
			String idValue = null;
			String idKey = null;
			if (idAttribute == null) {
				idValue = targetEl.getTextNormalize();
				idKey = idElement;
			} else {
				idValue = targetEl.getAttributeValue(idAttribute);
				idKey = idAttribute;
			}
			id2XmlMap.put(idValue, targetEl);
			XrefCounter count = new XrefCounter(idElement, idValue);
			idXRefCounter.put(idValue, count);

			if ((idValue == null) || (idValue.isEmpty())) {
				String srcLabel = null;
				String targetLabel = null;
				if (idAttribute != null) {
					srcLabel = idElement + "@" + idAttribute;
					targetLabel = idElement;
				} else {
					targetLabel = targetEl.getParentElement().getName();
					srcLabel = targetLabel + "/" + idElement;
				}
				String msg = srcLabel + " not specified. References to this " + targetLabel
						+ " will not be supportable.";
				logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_WARN, targetEl, msg, null, null, logMsgSrcId);
			} else {
				if (!idSet.add(idValue)) {
					LogReference srcRef = LogReference.getRef("CM", "cm001a");
					String msg = "ID " + idKey + " is not unique";
					int msgLevel;
					if (reqUniqueness) {
						msgLevel = LogMgmt.LEV_ERR;
						curFileIsValid = false;
					} else {
						msgLevel = LogMgmt.LEV_WARN;
					}
					logIssue(LogMgmt.TAG_MD, msgLevel, targetEl, msg, null, srcRef, logMsgSrcId);
				}
				if (chkSyntax) {
					/*
					 * Validate identifier structure conforms with Sec 2.1 of Common Metadata spec
					 * (v2.4)
					 */

					String idSyntaxPattern = "[\\S-[:]]+:[\\S-[:]]+:[\\S-[:]]+:[\\S]+$";
					if (!idValue.matches(idSyntaxPattern)) {
						String msg = "ID syntax does not conform to recommendations.";
						String details = "Best Practice is use of 'md:<type>:<scheme>:<SSID> syntax";
						LogReference srcRef = LogReference.getRef("MMM-BP", "mmbp01.3");
						logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_WARN, targetEl, msg, details, srcRef, logMsgSrcId);
						// curFileIsValid = false;
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
		}
		idSets.put(idElement, idSet);
		id2XmlMappings.put(idElement, id2XmlMap);
		idXRefCounts.put(idElement, idXRefCounter);
		return idSet;
	}

	/**
	 * @param idType
	 * @param idKey
	 * @param targetEl
	 */
	private void validateIdType(String idType, String idKey, Element targetEl) {
		/*
		 * Check syntax of the 'type' as defined in Manifest/Avails Delivery Best
		 * Practices (BP-META-MMMD) Section 3.1.7
		 */
		String type = id2typeMap.get(idKey);
		if (type == null) {
			type = idKey.toLowerCase();
		}
		if (!idType.equals(type)) {
			LogReference srcRef = LogReference.getRef("MMM-BP", "mmbp01.3");
			String msg = "ID <type> does not conform to recommendation (i.e. '" + type + "')";
			logIssue(LogMgmt.TAG_BEST, LogMgmt.LEV_NOTICE, targetEl, msg, null, srcRef, logMsgSrcId);

		}

	}

	/**
	 * @param idSchema
	 * @param targetEl
	 */
	private void validateIdScheme(String idScheme, Element targetEl) {
		if (!idScheme.startsWith("eidr")) {
			String msg = "Use of EIDR-based identifiers is recommended";
			String details = "Best Practice is to derive IDs from an EIDR-base ALID";
			LogReference srcRef = LogReference.getRef("MMM-BP", "mmbp01.1");
			logIssue(LogMgmt.TAG_BEST, LogMgmt.LEV_NOTICE, targetEl, msg, details, srcRef, logMsgSrcId);
		}
	}

	private void validateIdSsid(String idSSID, String idScheme, Element targetEl) {
		String idPattern = null;
		LogReference srcRef = null;
		switch (idScheme) {
		case "eidr":
			srcRef = LogReference.getRef("MMM-BP", "mmbp01.2");
			String msg = "Use of EIDR-x or EIDR-s identifiers is recommended";
			logIssue(LogMgmt.TAG_BEST, LogMgmt.LEV_NOTICE, targetEl, msg, null, srcRef, logMsgSrcId);

			srcRef = null;
			idPattern = "10\\.[\\d]{4}/[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-Z]";
			break;
		case "eidr-s":
			srcRef = LogReference.getRef("EIDR-IDF", "eidr01-s");
			idPattern = "[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-Z]";
			break;
		case "eidr-x":
			srcRef = LogReference.getRef("EIDR-IDF", "eidr01-x");
			idPattern = "[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-Z]:[\\S]+";
			break;
		case "eidr-urn":
			srcRef = LogReference.getRef("EIDR-IDF", "eidr01-urn");
			idPattern = "urn:eidr:10\\.5240:[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-Z]";
			break;
		default:
			msg = "ID uses scheme '" + idScheme + "', SSID format will not be verified";
			logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_INFO, targetEl, msg, "ssid='" + idSSID + "'", null, logMsgSrcId);
			return;
		}
		boolean match = idSSID.matches(idPattern);
		if (!match) {
			String msg = "Invalid SSID syntax for " + idScheme + " scheme";
			logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, targetEl, msg, "ssid='" + idSSID + "'", srcRef, logMsgSrcId);
			curFileIsValid = false;
		}
	}

	/**
	 * @param srcElement
	 * @param xrefElement
	 * @param targetElType
	 * @deprecated
	 */
	protected void validateXRef(String srcElement, String xrefElement, String targetElType) {
		String xpath = ".//manifest:" + srcElement + "/manifest:" + xrefElement;
		validateXRef(xpath, targetElType);
	}

	/**
	 * @param xpath
	 * @param targetElType
	 */
	protected void validateXRef(String xpath, String targetElType) {
		HashSet<String> idSet = idSets.get(targetElType);
		Map<String, XrefCounter> idXRefCounter = idXRefCounts.get(targetElType);
		if (xpath.contains("@")) {
			XPathExpression<Attribute> xpExpression = xpfac.compile(xpath, Filters.attribute(), null, manifestNSpace);
			List<Attribute> attributeList = xpExpression.evaluate(curRootEl);
			for (int i = 0; i < attributeList.size(); i++) {
				Attribute refAtt = (Attribute) attributeList.get(i);
				String targetId = refAtt.getValue();
				if (!idSet.contains(targetId)) {
					String msg = "Invalid cross-reference: the referenced " + targetElType
							+ " is not defined in this manifest";
					logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, refAtt.getParent(), msg, null, null, logMsgSrcId);
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
		} else {
			XPathExpression<Element> xpExpression = xpfac.compile(xpath, Filters.element(), null, manifestNSpace);
			List<Element> elementList = xpExpression.evaluate(curRootEl);
			for (int i = 0; i < elementList.size(); i++) {
				Element refEl = (Element) elementList.get(i);
				String targetId = refEl.getTextNormalize();
				if (!idSet.contains(targetId)) {
					String msg = "Invalid cross-reference: the referenced " + targetElType
							+ " is not defined in this manifest";
					logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, refEl, msg, null, null, logMsgSrcId);
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
	}

	/**
	 * Identify elements with ID values that are never referenced anywhere else in
	 * the file. This may be valid but it may also be the result of a typo or
	 * editing error. Thus, a WARNING, rather than an ERROR, will be generated.
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

	// ########################################################################

	/**
	 * Use the specified <tt>xpath</tt> to retrieve and validate image resolution.
	 * The resolution must be a String in the form colxrow (e.g., 800x600 would mean
	 * an image 800 pixels wide and 600 pixels tall).
	 * 
	 * @param xpath
	 */
	protected void validateResolution(String xpath) {
		LogReference docRef = LogReference.getRef("CM", "cm_res");
		String msg = "Invalid image resolution";
		String details = "resolution must be in the form colxrow (e.g. 800x600)";
		String pattern = "\\d+x\\d+";
		XPathExpression<?> xpExpression = StructureValidation.resolveXPath(xpath);
		List<?> targetList = xpExpression.evaluate(curRootEl);
		for (Object target : targetList) {
			String text = null;
			Element targetEl = null;
			Attribute targetAt = null;
			if (target instanceof Element) {
				targetEl = (Element) target;
				text = targetEl.getTextNormalize();
			} else if (target instanceof Attribute) {
				targetAt = (Attribute) target;
				text = targetAt.getValue();
			}
			if (target != null) {
				boolean match = text.matches(pattern);
				if (!match) {
					if (targetEl == null) {
						targetEl = targetAt.getParent();
					}
					logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_ERR, targetEl, msg, details, docRef, logMsgSrcId);
				}
			}
		}
	}

	/**
	 * Locate and validate all instances of a &lt;md:Rating&gt; Element. Validation
	 * checks will be based on the data contained in the latest release of the
	 * MovieLabs Common Metadata Ratings.
	 * 
	 * @see <a href="http://www.movielabs.com/md/ratings/index.html">MovieLabs
	 *      Common Metadata Ratings</a>
	 */
	protected void validateRatings() {
		XPathExpression<Element> xpExp01 = xpfac.compile(".//md:Rating", Filters.element(), null, mdNSpace);
		List<Element> ratingElList = xpExp01.evaluate(curRootEl);
		rLoop: for (int i = 0; i < ratingElList.size(); i++) {
			Element ratingEl = ratingElList.get(i);
			Element rSysEl = ratingEl.getChild("System", mdNSpace);
			String system = rSysEl.getTextNormalize();
			if (system.isEmpty()) {
				String msg = "Rating System not specified";
				String explanation = null;
				logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_ERR, rSysEl, msg, explanation, null, logMsgSrcId);
				curFileIsValid = false;
				continue;
			}
			if(system.startsWith("Custom:")) {
				String msg = "Ignoring custom Rating System '"+system+"'";
				String explanation = null;
				logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_DEBUG, rSysEl, msg, explanation, null, logMsgSrcId);
				continue;
			}			
			RatingSystem rSystem = RatingSystem.factory(system);
			if (rSystem == null) {
				String msg = "Unrecognized Rating System '" + system + "'";
				String explanation = null;
				logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_ERR, rSysEl, msg, explanation, null, logMsgSrcId);
				curFileIsValid = false;
				continue;
			}
			Element valueEl = ratingEl.getChild("Value", mdNSpace);
			String rating = valueEl.getTextNormalize();
			if (!specialRatings.contains(rating)) {
				if (!rSystem.isValid(rating)) {
					String msg = "Invalid Rating for RatingSystem";
					String explanation = "The " + system + " Rating System does not include the '" + rating + "'";
					logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_ERR, valueEl, msg, explanation, null, logMsgSrcId);
					curFileIsValid = false;
				} else if (rSystem.isDeprecated(rating)) {
					String msg = "Deprecated Rating";
					String explanation = "The " + system + " Rating System has deprecated the '" + rating
							+ "' rating. A more recent rating should be used if one is available";
					logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_WARN, valueEl, msg, explanation, null, logMsgSrcId);
				}
			}
			/*
			 * Is the RatingSystem in use in the specified country or countryRegion?
			 * 
			 */
			LogReference srcRef = LogReference.getRef("CM", "2.4", "cm_regions");
			Element regionEl = ratingEl.getChild("Region", mdNSpace);
			String region = null;
			Element target = regionEl.getChild("country", mdNSpace);
			if (target == null) {
				target = regionEl.getChild("countryRegion", mdNSpace);
				region = target.getText();
				/*
				 * We don't check validity of ISO-3166-2 codes as there are too many defined but
				 * very few used for ratings
				 */
				if (!rSystem.isUsedInSubRegion(region)) {
					String msg = "RatingSystem not used in specified Country SubRegion";
					String explanation = "The " + system
							+ " Rating System has not been officially adopted in SubRegion '" + region + "'";
					logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_WARN, target, msg, explanation, null, logMsgSrcId);
				}
			} else {
				region = target.getText();
				/* Is it valid ISO-3166-1 code? */
				if (!iso3166_1_codes.containsKey(region)) {
					String msg = "Invalid country code";
					String explanation = "A country should be specified as a ISO 3166-1 Alpha-2 code";
					logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_ERR, target, msg, explanation, srcRef, logMsgSrcId);
					curFileIsValid = false;
					;
				} else if (!rSystem.isUsedInRegion(region)) {
					String msg = "RatingSystem not used in specified Region";
					String explanation = "The " + system + " Rating System has not been officially adopted in Region '"
							+ region + "'";
					logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_WARN, target, msg, explanation, null, logMsgSrcId);
				}
			}
			/*
			 * Validation of Reasons: IF RatingSys provides defined reason codes then
			 * validate zero or more codes ELSE allow any string value.
			 * 
			 */
			if (rSystem.providesReasons()) {
				List<Element> reasonList = ratingEl.getChildren("Reason", mdNSpace);
				for (int j = 0; j < reasonList.size(); j++) {
					Element reasonEl = reasonList.get(j);
					String reason = reasonEl.getTextNormalize();
					if (!rSystem.hasReason(reason)) {
						String msg = "Invalid Reason code";
						String explanation = "Rating System uses pre-defined Reason-Codes which do not include '"
								+ reason + "'";
						logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_ERR, reasonEl, msg, explanation, null, logMsgSrcId);
						curFileIsValid = false;
					}
				}
			}
		}
	}

	protected void validateCurrencyCodes() {
		XPathExpression<Attribute> xpExpression = xpfac.compile(".//@currency", Filters.attribute(), null, availsNSpace,
				manifestNSpace, mdNSpace);
		List<Attribute> attList = xpExpression.evaluate(curRootEl);
		int tag4log = getLogTag(mdNSpace, null);
		for (int i = 0; i < attList.size(); i++) {
			Attribute targetAtt = (Attribute) attList.get(i);
			String text = targetAtt.getValue();
			if (!iso4217_codes.containsKey(text)) {
				String errMsg = "Invalid currency identifier '" + text + "'";
				String details = "Currency encoding must conform to ISO-4217";
				LogReference srcRef = LogReference.getRef("CM", CM_VER, "cm_currency");
				logIssue(tag4log, LogMgmt.LEV_ERR, targetAtt.getParent(), errMsg, details, srcRef, logMsgSrcId);
				curFileIsValid = false;
			}
		}
	}

	/**
	 * Validate a language entry conforms to RFC5646. Language codes used as both
	 * element values and attribute values will be checked.
	 * <p>
	 * This code assumes that any element or attribute with a name that ends with
	 * the string 'language' will contain an RFC5646 code. Matching is case
	 * insensitive. For example, all of the following will be tested:
	 * </p>
	 * <ul>
	 * <li>an element with the name 'AllowedLanguage'</li>
	 * <li>an element with the name 'Spokenlanguage'</li>
	 * <li>an attribute with the name 'assetLanguage'</li>
	 * <li>an attribute with the name 'language'</li>
	 * </ul>
	 * <p>
	 * On the other hand, an element named 'Dialect' would not be checked.
	 * </p>
	 * *
	 * <p>
	 * Use of RFC5646 is limited to a subset of the complete syntax in that only the
	 * <tt>Language</tt>, <tt>Region</tt>, and <tt>Variant</tt> subtags are
	 * supported. Use of the <tt>Script</tt> subtag is not supported.
	 * </p>
	 * 
	 * 
	 */

	protected void validateLanguageCodes() {
		validateLanguageElements();
		validateLanguageAttributes();
	}

	protected void validateLanguageElements() {
		XPathExpression<Element> xpExpression = xpfac.compile(
				".//*[substring(name(), string-length(name()) - string-length('anguage') +1) = 'anguage']",
				Filters.element(), null, availsNSpace, manifestNSpace, mdNSpace);
		List<Element> elementList = xpExpression.evaluate(curRootEl);
		String text = null;
		int tag4log = getLogTag(mdNSpace, null);
		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "valRegion1: element count = " + elementList.size(), null,
				logMsgSrcId);
		for (int i = 0; i < elementList.size(); i++) {
			Element targetEl = (Element) elementList.get(i);
			text = targetEl.getTextNormalize();
			if (!checkLangTag(text)) {
				reportLangError(targetEl, tag4log, text);
			}
		}
	}

	protected void validateLanguageAttributes() {
		XPathExpression<Attribute> xpExpression = xpfac.compile(
				".//@*[substring(name(), string-length(name()) - string-length('anguage') +1) = 'anguage']",
				Filters.attribute(), null, availsNSpace, manifestNSpace, mdNSpace);
		List<Attribute> attList = xpExpression.evaluate(curRootEl);
		int tag4log = getLogTag(mdNSpace, null);
		for (int i = 0; i < attList.size(); i++) {
			Attribute targetAtt = (Attribute) attList.get(i);
			String text = targetAtt.getValue();
			if (!checkLangTag(text)) {
				reportLangError(targetAtt.getParent(), tag4log, text);
			}
		}
	}

	private boolean checkLangTag(String text) {
		if (text == null) {
			/*
			 * something was missing. If it was REQ the XSD-based (i.e., schema) validation
			 * will flag it.
			 */
			return true;
		}
		if (text.isEmpty()) {
			/*
			 * The habit some folks have is to enter a required element (which passes schema
			 * check) but leave the value empty. THAT IS AN ERROR!
			 */
			return false;
		}
		/*
		 * RFC4647 states matching of language codes is case-insensitive. The codes have
		 * been converted and stored as all lowercase so we do the same conversion of
		 * the value we are checking.
		 * 
		 */
		text = text.toLowerCase();
		String[] langSubfields = text.split("-");
		boolean passed = true;
		/*
		 * 1st field should be specified in ISO639-2 or ISO639-3 and will be MANDATORY
		 */
		String subTag = langSubfields[0];
		switch (subTag.length()) {
		case 2:
			passed = iso639_2.contains(subTag);
			break;
		case 3:
			passed = iso639_3.contains(subTag);
			break;
		default:
			passed = false;
		}
		if (!passed) {
			return false;
		}
		if (langSubfields.length < 2) {
			return true;
		}

		/*
		 * 2nd field will be script or region or a variant. Which it is can be
		 * determined by the length of string.
		 */
		subTag = langSubfields[1];
		boolean foundRegion = false;
		switch (subTag.length()) {
		case 2:
		case 3:
			passed = iso3166_1_codes.containsKey(subTag.toUpperCase());
			foundRegion = true;
			break;
		case 4:
			passed = rfc5646Script.contains(subTag);
			break;
		default:
			passed = rfc5646Variant.contains(subTag);
		}
		if (!passed) {
			return false;
		}
		if (langSubfields.length < 3) {
			return true;
		}

		/*
		 * 3rd field may be region or a variant. Make sure we didn't already process a
		 * region in subtag #2.
		 */
		subTag = langSubfields[2];
		boolean foundVariant = false;
		if ((subTag.length() == 2) && !foundRegion) {
			passed = iso3166_1_codes.containsKey(subTag.toUpperCase());
		} else {
			passed = rfc5646Variant.contains(subTag);
			foundVariant = true;
		}
		if (!passed) {
			return false;
		}
		if (langSubfields.length < 4) {
			return true;
		}

		/*
		 * 4th field can only be a variant. Make sure we didn't already process a
		 * variant in prior subtag.
		 */
		if (foundVariant) {
			passed = false;
		} else {
			passed = rfc5646Variant.contains(langSubfields[3]);
		}
		return passed;
	}

	private void reportLangError(Element targetEl, int tag4log, String langTag) {
		/*
		 * Build an appropriate log entry based on nature and structure of the value
		 * source
		 */
		String errMsg = "Invalid Language code value '" + langTag + "'";
		String details = "Language encoding must conform to RFC5646 syntax and use registered subtag value";
		LogReference srcRef = LogReference.getRef("CM", CM_VER, "cm_lang");
		logIssue(tag4log, LogMgmt.LEV_ERR, targetEl, errMsg, details, srcRef, logMsgSrcId);
		curFileIsValid = false;
	}

	/**
	 * Validate proper encoding of the <tt>&lt;md:country&gt;</tt> element. Values
	 * are checked for conformance with ISO3166-1 Alpha2 or UN M.49 code sets.
	 * <p>
	 * Note that <tt>&lt;md:countrRegion&gt;</tt> elements are <b>not</b> subject to
	 * validation other than for conformance to the required pattern specified in
	 * the XSD.
	 * </p>
	 * 
	 * @return
	 */
	protected boolean validateCountries() {
		boolean allOK = true;
		/*
		 * check for use of the <tt>&lt;md:country&gt;</tt> element.
		 */
		XPathExpression<Element> xpExpression = xpfac.compile(".//" + mdNSpace.getPrefix() + ":country",
				Filters.element(), null, mdNSpace);
		List<Element> elementList = xpExpression.evaluate(curRootEl);
		String text = null;
		Element logMsgEl;
		int tag4log = getLogTag(mdNSpace, null);
		LogReference srcRef = LogReference.getRef("CM", "cm_regions");
		String errMsg = null;
		for (int i = 0; i < elementList.size(); i++) {
			Element targetEl = (Element) elementList.get(i);
			logMsgEl = targetEl;
			text = targetEl.getTextNormalize();
			errMsg = "Unrecognized value '" + text + "' for country or region";
			if (text != null) {
				if (!iso3166_1_codes.containsKey(text)) {
					logIssue(tag4log, LogMgmt.LEV_ERR, logMsgEl, errMsg, null, srcRef, logMsgSrcId);
					allOK = false;
					curFileIsValid = false;
				}
			}
		}
		return allOK;
	}

	/**
	 * Validate country codes specified in any <tt>@region</tt> attribute are
	 * conforming to either the ISO3166-1 Alpha2 or UN M.49 code sets.
	 * 
	 * @param primaryNS used only for log messages
	 */
	protected boolean validateRegion(Namespace primaryNS) {
		boolean allOK = true;
		String errMsg = "Unrecognized value for @region attribute";
		LogReference srcRef = LogReference.getRef("CM", "cm_regions");
		XPathExpression<Attribute> xpExpression = xpfac.compile("//@region", Filters.attribute(), null, primaryNS);
		List<Attribute> attList = xpExpression.evaluate(curRootEl);
		int tag4log = getLogTag(primaryNS, null);
		for (int i = 0; i < attList.size(); i++) {
			Attribute targetAtt = (Attribute) attList.get(i);
			String text = targetAtt.getValue();
			if (!iso3166_1_codes.containsKey(text)) {
				logIssue(tag4log, LogMgmt.LEV_ERR, targetAtt.getParent(), errMsg, null, srcRef, logMsgSrcId);
				allOK = false;
				curFileIsValid = false;
			}
		}
		return allOK;
	}

	protected boolean validateCode(Namespace primaryNS, String primaryEl, Namespace childNS, String child,
			Properties codes, LogReference srcRef, boolean caseSensitive) {
		boolean allOK = true;
		int tag4log = getLogTag(primaryNS, childNS);
		XPathExpression<Element> xpExpression = xpfac.compile(".//" + primaryNS.getPrefix() + ":" + primaryEl,
				Filters.element(), null, primaryNS);
		List<Element> elementList = xpExpression.evaluate(curRootEl);
		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "valCode: element count = " + elementList.size(), null,
				logMsgSrcId);

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
				if (!caseSensitive) {
					text = text.toUpperCase();
				}
				if (!codes.containsKey(text)) {
					logIssue(tag4log, LogMgmt.LEV_ERR, logMsgEl, errMsg, null, srcRef, logMsgSrcId);
					allOK = false;
					curFileIsValid = false;
				}
			}
		}
		return allOK;
	}

	/**
	 * @param targetSelectionPath xPath to identify one or more target elements
	 * @param baseEl              Element that is starting point for
	 *                            <tt>targetSelectionPath</tt> evaluation.
	 * @param rqmtSet             JSON-formatted requirements that all target
	 *                            selections should satisfy
	 */
	protected void validateDigitalAsset(String targetSelectionPath, Element baseEl, JSONObject rqmtSet) {
		Collection<Namespace> nSpaces = new HashSet<Namespace>();
		nSpaces.add(rootNS);
		nSpaces.add(mdNSpace);
		XPathExpression<Element> xpExpression = xpfac.compile(targetSelectionPath, Filters.element(), null, nSpaces);
		List<Element> assetList = xpExpression.evaluate(baseEl);
		if (assetList.isEmpty()) {
			return;
		}

		Iterator<String> keys = rqmtSet.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject rqmtSpec = rqmtSet.getJSONObject(key);
			loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_MD, "DigitalAsset rqmt check; key= " + key, curFile,
					logMsgSrcId);
			String xPath = rqmtSpec.getString("targetPath");
			if (rqmtSpec.containsKey("children")) {
				/* Recursive descent */
				loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_MD, "DigitalAsset Recursive descent; key= " + key,
						curFile, logMsgSrcId);
				JSONObject innerRqmtSet = rqmtSpec.getJSONObject("children");
				for (Element nextAssetEl : assetList) {
					validateDigitalAsset(xPath, nextAssetEl, innerRqmtSet);
				}

			} else {
				/**
				 * Rules for the xPath used for DigitalAsset are different from those used
				 * elsewhere and are much simpler:
				 * 
				 * <pre>
				 * 1) explicitly identifies the namespace prefix (i.e. md:Foo instead of {md}Foo
				 * 2) can only include elements in the common metadata (i.e., md) namespace
				 * </pre>
				 */
				boolean isAttribute;
				XPathExpression xpExp;
				if (xPath.contains("@")) {
					isAttribute = true;
					xpExp = xpfac.compile(xPath, Filters.attribute(), null, XmlIngester.mdNSpace);
				} else {
					isAttribute = false;
					xpExp = xpfac.compile(xPath, Filters.element(), null, XmlIngester.mdNSpace);
				}

				String logLabel = targetSelectionPath + "/" + key;
				/*
				 * Code allows for either vocab checks or structural checks (but not both in the
				 * same rqmt).
				 */
				String documentRef = rqmtSpec.optString("ref");
				if (rqmtSpec.containsKey("values")) {
					// vocab check
					JSONArray expected = rqmtSpec.getJSONArray("values");
					for (Element nextAssetEl : assetList) {
						List<Element> targetElList = (List<Element>) xpExp.evaluate(nextAssetEl);
						validateVocabUse(targetElList, expected, isAttribute, documentRef, true, true, LogMgmt.TAG_MD,
								logLabel);
					}
				} else {
					// structure check ??
				}
			}
		}
	}

	/**
	 * Equivalent to calling
	 * <tt>validateVocab(Namespace primaryNS, String primaryEl, Namespace childNS, String child,
			JSONArray expected, LogReference srcRef, boolean caseSensitive, boolean strict)</tt>
	 * with <tt>strict = true</tt>
	 * 
	 * @param primaryNS
	 * @param primaryEl
	 * @param childNS
	 * @param child
	 * @param expected
	 * @param srcRef
	 * @param caseSensitive
	 * @deprecated use validateVocab(Namespace primaryNS, String primaryEl,
	 *             Namespace childNS, String child, JSONArray expected, LogReference
	 *             srcRef, boolean caseSensitive, boolean strict)
	 */
	protected void validateVocab(Namespace primaryNS, String primaryEl, Namespace childNS, String child,
			JSONArray expected, LogReference srcRef, boolean caseSensitive) {
		validateVocab(primaryNS, primaryEl, childNS, child, expected, srcRef, caseSensitive, true);
	}

	/**
	 * @param primaryNS
	 * @param primaryEl
	 * @param childNS
	 * @param child
	 * @param expected      expected values
	 * @param srcRef        source of supplementary documentation for log entries
	 *                      (optional)
	 * @param caseSensitive if <tt>true</tt> matching to expected values is
	 *                      case-sensitive.
	 * @param strict        if <tt>true</tt> non-matches are treated as errors,
	 *                      otherwise as warnings.
	 */
	protected void validateVocab(Namespace primaryNS, String primaryEl, Namespace childNS, String child,
			JSONArray expected, LogReference srcRef, boolean caseSensitive, boolean strict) {
		int tag4log = getLogTag(primaryNS, childNS);
		String logLabel;
		String xpathString;
		Collection<Namespace> nSpaces = new HashSet<Namespace>();
		nSpaces.add(primaryNS);
		if (childNS != null) {
			xpathString = ".//" + primaryNS.getPrefix() + ":" + primaryEl + "/" + childNS.getPrefix() + ":" + child;
			nSpaces.add(childNS);
		} else {
			xpathString = ".//" + primaryNS.getPrefix() + ":" + primaryEl + "/" + child;
		}
		boolean isAttribute;
		if (!child.startsWith("@")) {
			logLabel = primaryEl + "/" + child;
			isAttribute = false;
		} else {
			logLabel = primaryEl + child;
			isAttribute = true;
		}

		validateVocab(nSpaces, xpathString, isAttribute, expected, srcRef, caseSensitive, strict, tag4log, logLabel);
	}

	/**
	 * @param nSpaces
	 * @param xpath
	 * @param isAttribute
	 * @param expected
	 * @param srcRef
	 * @param caseSensitive
	 * @param strict
	 * @param logTag
	 * @param logLabel
	 */
	protected void validateVocab(Collection<Namespace> nSpaces, String xpath, boolean isAttribute, JSONArray expected,
			LogReference srcRef, boolean caseSensitive, boolean strict, int logTag, String logLabel) {
		if (expected == null || expected.isEmpty()) {
			/*
			 * The version of the schema being used does not define an enumerated set of
			 * valid terms for the target element.
			 * 
			 */
			return;
		}

		XPathExpression xpExpression;
		if (isAttribute) {
			xpExpression = xpfac.compile(xpath, Filters.attribute(), null, nSpaces);
		} else {
			xpExpression = xpfac.compile(xpath, Filters.element(), null, nSpaces);
		}
		List targetList = xpExpression.evaluate(curRootEl);
		validateVocabUse(targetList, expected, isAttribute, srcRef, caseSensitive, strict, logTag, logLabel);
	}
	// .=============================================

	/**
	 * @param targetList
	 * @param expected
	 * @param isAttribute
	 * @param srcRef
	 * @param caseSensitive
	 * @param strict
	 * @param logTag
	 * @param logLabel
	 */
	private void validateVocabUse(List targetList, JSONArray expected, boolean isAttribute, Object srcRef,
			boolean caseSensitive, boolean strict, int logTag, String logLabel) {
		String optionsString = expected.toString().toLowerCase();
		int logLevel;
		String explanation;
		String docSec = null;
		LogReference docRef = null;
		if (srcRef instanceof LogReference) {
			docRef = (LogReference) srcRef;
		} else if (srcRef instanceof String) {
			docSec = (String) srcRef;
			if (docSec.isEmpty()) {
				docSec = null;
			}
		}
		if (strict) {
			logLevel = LogMgmt.LEV_ERR;
			explanation = "Value specified does not match one of the allowed strings.";
			if (docSec != null) {
				explanation = explanation + " (see CM " + docSec + ")";
			}
			if (caseSensitive) {
				explanation = explanation + " Note that string-matching is case-sensitive";
			}
		} else {
			logLevel = LogMgmt.LEV_WARN;
			explanation = "Value specified doesn't match one of the recommended strings. Others may be used if all parties agree";
			if (docSec != null) {
				explanation = explanation + " (see CM " + docSec + ")";
			}
		}
		for (Object next : targetList) {
			String text = null;
			String errMsg = null;
			Element logMsgEl = null;

			if (isAttribute) {
				Attribute targetAt = (Attribute) next;
				logMsgEl = targetAt.getParent();
				text = targetAt.getValue().trim();
				errMsg = "Unrecognized value '" + text + "' for attribute " + logLabel;

			} else {
				Element targetEl = (Element) next;
				logMsgEl = targetEl;
				text = targetEl.getTextNormalize();
				errMsg = "Unrecognized value '" + text + "' for " + logLabel;
			}
			if (text != null) {
				boolean matched = true;
				if (caseSensitive) {
					if (!expected.contains(text)) {
						logIssue(logTag, logLevel, logMsgEl, errMsg, explanation, docRef, logMsgSrcId);
						matched = false;

					}
				} else {
					String checkString = "\"" + text.toLowerCase() + "\"";
					if (!optionsString.contains(checkString)) {
						logIssue(logTag, logLevel, logMsgEl, errMsg, explanation, docRef, logMsgSrcId);
						matched = false;
					}
				}
				if (!matched && strict) {
					curFileIsValid = false;
				}
			}
		}
	}

	/**
	 * @param primaryNS
	 * @param childNS
	 * @return
	 */
	public static int getLogTag(Namespace primaryNS, Namespace childNS) {
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
		return tag4log;
	}
}
