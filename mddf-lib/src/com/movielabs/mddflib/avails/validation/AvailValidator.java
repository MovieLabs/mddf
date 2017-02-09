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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.logging.IssueLogger;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.AbstractValidator;
import com.movielabs.mddflib.util.xml.SchemaWrapper;
import com.movielabs.mddflib.util.xml.XsdValidation;
import com.movielabs.mddflib.util.xml.XmlIngester;

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
public class AvailValidator extends AbstractValidator implements IssueLogger {

	/**
	 * Used to facilitate keeping track of the presence or absence of required
	 * Assets within an Avail.
	 * 
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */

	protected class AvailRqmt {
		private JSONObject rqmt;
		private JSONArray included;
		private int counter = 0;

		AvailRqmt(JSONObject rqmt) {
			this.rqmt = rqmt;
			included = rqmt.getJSONArray("WorkType");
		}

		boolean notePresenceOf(String workType) {
			if (included.contains(workType)) {
				counter++;
				return true;
			} else {
				return false;
			}
		}

		/**
		 * @param availEl
		 * @return
		 */
		public boolean violated(Element availTypeEl) {
			int min = rqmt.optInt("min", 0);
			int max = rqmt.optInt("max", -1);
			Element availEl = availTypeEl.getParentElement();
			String availType = availTypeEl.getTextTrim();
			String msg = "Invalid Asset structure for specified AvailType";
			if (counter < min) {
				String explanation = "Avail of type='" + availType + "' must have at least " + min
						+ " Assets of WorkType=" + included.toString();
				logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, availEl, msg, explanation, AVAIL_RQMT_srcRef, logMsgSrcId);
				return true;
			}

			if ((max > 0) && (counter > max)) {
				String explanation = "Avail of type='" + availType + "' must have no more than " + max
						+ " Assets of WorkType=" + included.toString();
				logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, availTypeEl, msg, explanation, AVAIL_RQMT_srcRef,
						logMsgSrcId);
				return true;
			}
			return false;
		}
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
				logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_WARN, targetEl, msg, explanation, null, logMsgSrcId);
			}
		}

	}

	public static final String LOGMSG_ID = "AvailValidator";
 
	static final LogReference AVAIL_RQMT_srcRef = LogReference.getRef("AVAIL",  "avail01");

	private static JSONObject availVocab;

	private static JSONObject availTypeStruct;

	private static JSONArray genericAvailTypes;

	private static JSONObject workTypeStruct;

	private Map<Object, Pedigree> pedigreeMap;

	static {
		id2typeMap = new HashMap<String, String>();

		try {
			/*
			 * Is there a controlled vocab that is specific to a Manifest? Note
			 * the vocab set for validating Common Metadata will be loaded by
			 * the parent class AbstractValidator.
			 */
			availVocab = loadVocab(vocabRsrcPath, "Avail");

			/*
			 * Load JSON that defines various constraints on structure of an
			 * Avails
			 */
			String availRsrcPath = MddfContext.RSRC_PATH + "avail_structure.json";
			JSONObject availStruct = loadJSON(availRsrcPath);
			availTypeStruct = availStruct.getJSONObject("AvailType");
			genericAvailTypes = availTypeStruct.getJSONArray("common");

			workTypeStruct = availStruct.getJSONObject("WorkTypeStruct");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @param validateC
	 * @param loggingMgr
	 */
	public AvailValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;
		rootPrefix = "avails:";

		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_AVAIL;
	}

	/**
	 * Validate a single Avails document. The Avails data is structured as an
	 * XML document regardless of the original source file's format. This means
	 * that when an Avails has been provided as an Excel file, it will have
	 * already been converted to an XML document. Invoking the
	 * <tt>process()</tt> method will validate that XML regardless of its
	 * original source format. The <tt>pedigreeMap</tt> will link a specific
	 * element in the XML being processed back to its original source (i.e.,
	 * either a row and cell in an Excel spreadsheet or a line in an XML file).
	 * 
	 * @param docRootEl
	 *            root of the Avail
	 * @param pedigreeMap
	 *            links XML Elements to their original source (used for logging
	 *            only)
	 * @param srcFile
	 *            is source from which XML was obtained (used for logging only)
	 * @return
	 * @throws IOException
	 * @throws JDOMException
	 */
	public boolean process(Element docRootEl, Map<Object, Pedigree> pedigreeMap, File xmlFile)
			throws IOException, JDOMException {
		curRootEl = null;
		curFile = xmlFile;
		curFileName = xmlFile.getName();
		this.pedigreeMap = pedigreeMap;
		curFileIsValid = true;

		String schemaVer = identifyXsdVersion(docRootEl);
		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Using Schema Version " + schemaVer, srcFile, logMsgSrcId);
		setAvailVersion(schemaVer);
		rootNS = availsNSpace;

		validateXml(xmlFile, docRootEl);
		if (!curFileIsValid) {
			String msg = "Schema validation check FAILED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, curFile, logMsgSrcId);
			// return false;
		} else {
			curRootEl = docRootEl; // getAsXml(xmlFile);
			String msg = "Schema validation check PASSED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, curFile, logMsgSrcId);
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
	protected boolean validateXml(File srcFile, Element docRootEl) {
		String xsdFile = XsdValidation.defaultRsrcLoc + "avails-v" + XmlIngester.AVAIL_VER + ".xsd";
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		curFileIsValid = xsdHelper.validateXml(srcFile, docRootEl, xsdFile, logMsgSrcId);
		return curFileIsValid;
	}

	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateConstraints() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_AVAIL, "Validating constraints", curFile, LOGMSG_ID);
		super.validateConstraints();

		SchemaWrapper availSchema = SchemaWrapper.factory("avails-v" + XmlIngester.AVAIL_VER);
		List<String> reqElList = availSchema.getReqElList();
		for (int i = 0; i < reqElList.size(); i++) {
			String key = reqElList.get(i);
			validateNotEmpty(key);
		}

		// TODO: Load from JSON file....

		validateId("ALID", null, true, false);

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
		validateCMVocab();

		// Now do any defined in Avails spec..
		validateAvailVocab();

		validateUsage();
	}

	/**
	 * @return
	 */
	private boolean validateAvailVocab() {
		boolean allOK = true;
		Namespace primaryNS = availsNSpace;
		String doc = "AVAIL";

		JSONArray allowed = availVocab.optJSONArray("AvailType");
		LogReference srcRef = LogReference.getRef(doc, "avail01");
		allOK = validateVocab(primaryNS, "Avail", primaryNS, "AvailType", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("EntryType");
		srcRef = LogReference.getRef(doc, "avail02");
		allOK = validateVocab(primaryNS, "Disposition", primaryNS, "EntryType", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("AltIdentifier@scope");
		srcRef = LogReference.getRef(doc, "avail03");
		allOK = validateVocab(primaryNS, "AltIdentifier", primaryNS, "@scope", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("LocalizationOffering");
		srcRef = LogReference.getRef(doc, "avail03");
		allOK = validateVocab(primaryNS, "Metadata", primaryNS, "LocalizationOffering", allowed, srcRef, true) && allOK;
		allOK = validateVocab(primaryNS, "EpisodeMetadata", primaryNS, "LocalizationOffering", allowed, srcRef, true)
				&& allOK;
		
		allowed = availVocab.optJSONArray("SeasonStatus");
		srcRef = LogReference.getRef(doc, "avail04");
		allOK = validateVocab(primaryNS, "SeasonMetadata", primaryNS, "SeasonStatus", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("SeriesStatus");
		srcRef = LogReference.getRef(doc, "avail05");
		allOK = validateVocab(primaryNS, "SeriesMetadata", primaryNS, "SeriesStatus", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("DateTimeCondition");
		srcRef = LogReference.getRef(doc, "avail06");
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "StartCondition", allowed, srcRef, true) && allOK;
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "EndCondition", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("LicenseType");
		srcRef = LogReference.getRef(doc, "avail07");
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "LicenseType", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("LicenseRightsDescription");
		srcRef = LogReference.getRef(doc, "avail07");
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "LicenseRightsDescription", allowed, srcRef, true)
				&& allOK;

		allowed = availVocab.optJSONArray("FormatProfile");
		srcRef = LogReference.getRef(doc, "avail07");
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "FormatProfile", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("ExperienceCondition");
		srcRef = LogReference.getRef(doc, "avail07");
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "ExperienceCondition", allowed, srcRef, true)
				&& allOK;

		allowed = availVocab.optJSONArray("Term@termName");
		srcRef = LogReference.getRef(doc, "avail08");
		allOK = validateVocab(primaryNS, "Term", primaryNS, "@termName", allowed, srcRef, false) && allOK;

		allowed = availVocab.optJSONArray("SharedEntitlement@ecosystem");
		srcRef = LogReference.getRef(doc, "avail09");
		allOK = validateVocab(primaryNS, "SharedEntitlement", primaryNS, "@ecosystem", allowed, srcRef, true) && allOK;
		
		// ===========================================================
		/* For Transactions in US, check USACaptionsExemptionReason */
		allowed = availVocab.optJSONArray("USACaptionsExemptionReason");
		srcRef = LogReference.getRef(doc, "avail03");
		allOK = validateVocab(primaryNS, "Asset", primaryNS, "USACaptionsExemptionReason", allowed, srcRef, true) && allOK;
		allOK = validateVocab(primaryNS, "EpisodeMetadata", primaryNS, "USACaptionsExemptionReason", allowed, srcRef, true)
				&& allOK;
		allOK = validateVocab(primaryNS, "SeasonMetadata", primaryNS, "USACaptionsExemptionReason", allowed, srcRef, true)
				&& allOK;
		allOK = validateVocab(primaryNS, "SeriesMetadata", primaryNS, "USACaptionsExemptionReason", allowed, srcRef, true)
				&& allOK;
		
		return allOK;
	}

	/**
	 * @return
	 */
	protected boolean validateCMVocab() {
		boolean allOK = true;
		Namespace primaryNS = availsNSpace;
		/*
		 * Validate use of Country identifiers....
		 */
		// In 'Metadata/Release History/DistrTerritory'
		allOK = validateRegion(mdNSpace, "DistrTerritory", mdNSpace, "country") && allOK;

		// in Transaction/Territory...
		allOK = validateRegion(primaryNS, "Territory", mdNSpace, "country") && allOK;
		allOK = validateRegion(primaryNS, "TerritoryExcluded", mdNSpace, "country") && allOK;

		// in 'Term/Region
		allOK = validateRegion(primaryNS, "Region", mdNSpace, "country") && allOK;

		// in multiple places
		allOK = validateRegion(mdNSpace, "Region", mdNSpace, "country") && allOK;

		/* Validate language codes */

		allOK = validateLanguage(primaryNS, "LocalSeriesTitle", primaryNS, "@language") && allOK;
		allOK = validateLanguage(primaryNS, "Transaction", primaryNS, "AllowedLanguage") && allOK;
		allOK = validateLanguage(primaryNS, "Transaction", primaryNS, "AssetLanguage") && allOK;
		allOK = validateLanguage(primaryNS, "Transaction", primaryNS, "HoldbackLanguage") && allOK;
		allOK = validateLanguage(primaryNS, "Term", primaryNS, "Language") && allOK;
		return allOK;
	}

	/**
	 * Check for consistent usage. This typically means that an OPTIONAL element
	 * will be either REQUIRED or INVALID for certain use-cases (e.g.
	 * BundledAsset is only allowed when WorkType is 'Collection').
	 * 
	 * @return
	 */
	private void validateUsage() {
		/*
		 * Check terms associated with Pre-Orders: If LicenseType is 'POEST'
		 * than (1) a SuppressionLiftDate term is required and (2) a
		 * PreOrderFulfillDate term is allowed.
		 */
		// 1) Check all Transactions with LicenseType = 'POEST'
		XPathExpression<Element> xpExp01 = xpfac.compile(".//" + rootPrefix + "LicenseType[.='POEST']",
				Filters.element(), null, availsNSpace);
		XPathExpression<Element> xpExp02 = xpfac.compile(".//" + rootPrefix + "Term[@termName='SuppressionLiftDate']",
				Filters.element(), null, availsNSpace);

		List<Element> ltElList = xpExp01.evaluate(curRootEl);
		for (int i = 0; i < ltElList.size(); i++) {
			Element lTypeEl = ltElList.get(i);
			Element transEl = lTypeEl.getParentElement();
			List<Element> termElList = xpExp02.evaluate(transEl);
			if (termElList.size() < 1) {
				String msg = "SuppressionLiftDate term is missing";
				String explanation = "If LicenseType is 'POEST' than a SuppressionLiftDate term is required";
				logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, lTypeEl, msg, explanation, null, logMsgSrcId);
				curFileIsValid = false;
			} else if (termElList.size() > 1) {
				String msg = "Too many SuppressionLiftDate terms";
				String explanation = "More than one SuppressionLiftDate terms is contradictory. Interpretation can not be guaranteed.";
				logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_WARN, termElList.get(0), msg, explanation, null, logMsgSrcId);
			}
		}

		// 2) Check all Transactions with a PreOrderFulfilDate term
		xpExp01 = xpfac.compile(".//" + rootPrefix + "Term[@termName='PreOrderFulfillDate']", Filters.element(), null,
				availsNSpace);
		List<Element> termElList = xpExp01.evaluate(curRootEl);
		for (int i = 0; i < termElList.size(); i++) {
			Element termEl = termElList.get(i);
			Element transEl = termEl.getParentElement();
			Element lTypeEl = transEl.getChild("LicenseType", availsNSpace);
			if (!(lTypeEl.getText().equals("POEST"))) {
				String msg = "Invalid term for LicenseType";
				String explanation = "PreOrderFulfillDate term is only allowed if LicenseType='POEST'";
				logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, termEl, msg, explanation, null, logMsgSrcId);
				curFileIsValid = false;
			}
		}
		// ===========================================================
		/*
		 * Asset/BundledAsset is only allowed when Asset/WorkType is
		 * 'Collection'
		 * 
		 */
		LogReference srcRef = LogReference.getRef("AVAIL", "struc01");
		xpExp01 = xpfac.compile(".//" + rootPrefix + "Asset[" + rootPrefix + "BundledAsset]", Filters.element(), null,
				availsNSpace);
		List<Element> assetElList = xpExp01.evaluate(curRootEl);
		for (int i = 0; i < assetElList.size(); i++) {
			Element assetEl = assetElList.get(i);
			Element wrkTypeEl = assetEl.getChild("WorkType", availsNSpace);
			if (!(wrkTypeEl.getText().equals("Collection"))) {
				String msg = "Invalid Asset structure for specified WorkType";
				String explanation = "Asset/BundledAsset is only allowed when Asset/WorkType is 'Collection'";
				logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, wrkTypeEl, msg, explanation, srcRef, logMsgSrcId);
				curFileIsValid = false;
			}
		}
		// ===========================================================
		/*
		 * Match the Avail/AvailType with all child Asset/WorkTypes as
		 * restrictions apply.
		 * 
		 */
		validateTypeCompatibility("single");
		validateTypeCompatibility("episode");
		validateTypeCompatibility("season");
		validateTypeCompatibility("series");
		validateTypeCompatibility("miniseries");
		validateTypeCompatibility("bundle");
		validateTypeCompatibility("supplement");
		validateTypeCompatibility("promotion");

		// ==============================================

		/* Validate any Ratings information */
		validateRatings();

		return;
	}
	// ########################################################################

	/**
	 * Validate the Avail's structure is compatible with the specified
	 * AvailType. The structural constraints are defined via JSON in a
	 * <tt>structureDef</tt> JSONObject. The 'structureDef' has two components:
	 * <ol>
	 * <li>"requirement": an ARRAY of 1 or more JSONObjects, each of which
	 * defines a REQUIREMENT</li>
	 * <li>"allowed": an optional ARRAY of 1 or more Strings listing allowed
	 * Asset WorkTypes. Any WorkType listed in the 'allowed' set should NOT be
	 * included in any of the requirements.
	 * </ol>
	 * 
	 * @param availType
	 */
	private void validateTypeCompatibility(String availType) {
		LogReference srcRef = LogReference.getRef("AVAIL",   "struc01");

		JSONObject structureDef = availTypeStruct.getJSONObject(availType);
		if (structureDef == null || structureDef.isNullObject()) {
			System.out.println("No structureDef for AvailType='" + availType + "'");
			return;
		}
		JSONArray allAllowedWTypes = getAllowedWTypes(availType);

		String path = ".//" + rootPrefix + "AvailType[text()='" + availType + "']";
		XPathExpression<Element> xpExp01 = xpfac.compile(path, Filters.element(), null, availsNSpace);
		List<Element> availTypeElList = xpExp01.evaluate(curRootEl);
		/* Loop thru all the Avail instances... */
		for (int i = 0; i < availTypeElList.size(); i++) {
			Element availTypeEl = availTypeElList.get(i);
			Element availEl = availTypeEl.getParentElement();
			AvailRqmt[] allRqmts = getRequirements(availType);
			// Now get the descendant WorkType element
			XPathExpression<Element> xpExp02 = xpfac.compile("./" + rootPrefix + "Asset/" + rootPrefix + "WorkType",
					Filters.element(), null, availsNSpace);
			List<Element> workTypeElList = xpExp02.evaluate(availEl);
			for (int j = 0; j < workTypeElList.size(); j++) {
				/*
				 * Check each Asset's WorkType to see if it allowed for the
				 * current AvailType
				 */
				Element wrkTypeEl = workTypeElList.get(j);
				String wtValue = wrkTypeEl.getText();
				if (!(allAllowedWTypes.contains(wtValue))) {
					String msg = "AvailType is incompatible with WorkType";
					String explanation = null;
					logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, wrkTypeEl, msg, explanation, srcRef, logMsgSrcId);
					curFileIsValid = false;
				} else {
					/*
					 * Yes its allowed, but is there any cardinality constraint?
					 */
					if (allRqmts != null) {
						for (int k = 0; k < allRqmts.length; k++) {
							allRqmts[k].notePresenceOf(wtValue);
						}
					}
				}
				// Check if Asset's structure is compatible with the WorkType
				validateAssetStructure(wrkTypeEl);

			}
			/*
			 * Next step for each Avail is to check accumulated counts for any
			 * violation of a cardinality constraint.
			 */

			if (allRqmts != null) {
				for (int k = 0; k < allRqmts.length; k++) {
					if (allRqmts[k].violated(availTypeEl)) {
						curFileIsValid = false;
					}
				}
			}
		}

	}

	/**
	 * Make sure that each Asset has a structure compatible with it's WorkType.
	 * 
	 * @param wrkTypeEl
	 * 
	 */
	private void validateAssetStructure(Element wrkTypeEl) {
		// Retrieve structure defs specific to the WorkType
		String wtValue = wrkTypeEl.getText();
		JSONObject wtStrucDef = workTypeStruct.optJSONObject(wtValue);
		if (wtStrucDef == null) {
			System.out.println("No structureDef for WorkType='" + wtValue + "'");
			return;
		}
		Element assetEl = wrkTypeEl.getParentElement();
		JSONArray rqmts = wtStrucDef.getJSONArray("requirement");
		for (int i = 0; i < rqmts.size(); i++) {
			JSONObject nextRqmt = rqmts.getJSONObject(i);
			if(!structHelper.validateStructure(assetEl, nextRqmt)){
				curFileIsValid = false;
			} 
		}

	}
	// ######################################################################

	/**
	 * @param availType
	 * @return
	 */
	private AvailRqmt[] getRequirements(String availType) {
		JSONObject structureDef = availTypeStruct.getJSONObject(availType);
		JSONArray requirementJA = structureDef.getJSONArray("requirement");
		if (requirementJA == null) {
			// no cardinality requirements for this type
			return null;
		}
		AvailRqmt[] arSet = new AvailRqmt[requirementJA.size()];
		for (int i = 0; i < requirementJA.size(); i++) {
			JSONObject reqJO = requirementJA.getJSONObject(i);
			arSet[i] = new AvailRqmt(reqJO);
		}
		return arSet;
	}

	/**
	 * Adds all required and GENERIC WorkTypes to the list of allowed WorkTypes
	 * and returns a single unified collection.
	 * 
	 * @param availType
	 * @return
	 */
	private static JSONArray getAllowedWTypes(String availType) {
		JSONObject structureDef = availTypeStruct.getJSONObject(availType);
		JSONArray allowedWTypes = structureDef.getJSONArray("allowed");
		allowedWTypes.addAll(genericAvailTypes);
		JSONArray reqmts = structureDef.getJSONArray("requirement");
		for (int i = 0; i < reqmts.size(); i++) {
			JSONObject nextRqmt = reqmts.getJSONObject(i);
			JSONArray wtArray = nextRqmt.optJSONArray("WorkType");
			if (wtArray != null) {
				allowedWTypes.addAll(wtArray);
			}
		}
		return allowedWTypes;
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
	public void logIssue(int tag, int level, Object xmlElement, String msg, String explanation, LogReference srcRef,
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
