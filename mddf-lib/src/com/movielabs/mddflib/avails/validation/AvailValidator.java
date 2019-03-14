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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jdom2.Namespace;

import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.logging.IssueLogger;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.CMValidator;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.SchemaWrapper;
import com.movielabs.mddflib.util.xml.XsdValidation;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * Validates an Avails file as conforming to EMA Content Availability Data
 * (Avails) as specified in <tt>TR-META-AVAIL</tt>. Validation also includes
 * testing for conformance with the appropriate version of the
 * <tt>Common Metadata (md)</tt> specification.
 * 
 * @see <a href= "http://www.movielabs.com/md/avails/v2.1/Avails_v2.1.pdf"> TR-
 *      META-AVAIL (v2.1)</a>
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class AvailValidator extends CMValidator implements IssueLogger {

	public static final String LOGMSG_ID = "AvailValidator";
	protected static HashMap<String, String> avail_id2typeMap;

	static {
		avail_id2typeMap = new HashMap<String, String>();
		// if any avail-specific ID type-mapping, put here
	}

	static final LogReference AVAIL_RQMT_srcRef = LogReference.getRef("AVAIL", "avail01");

	private Map<Object, Pedigree> pedigreeMap;

	private String availSchemaVer;

	/**
	 * @param validateC
	 * @param loggingMgr
	 */
	public AvailValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;

		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_AVAIL;
		id2typeMap = avail_id2typeMap;
	}

	/**
	 * Validate a single Avails document. The Avails data is structured as an XML
	 * document regardless of the original source file's format. This means that
	 * when an Avails has been provided as an Excel file, it will have already been
	 * converted to an XML document. Invoking the <tt>process()</tt> method will
	 * validate that XML regardless of its original source format. The
	 * <tt>pedigreeMap</tt> will link a specific element in the XML being processed
	 * back to its original source (i.e., either a row and cell in an Excel
	 * spreadsheet or a line in an XML file).
	 * 
	 * @param target
	 * @param pedigreeMap
	 * @return
	 */
	public boolean process(MddfTarget target, Map<Object, Pedigree> pedigreeMap) {
		String msg = "Begining validation of Avails...";
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_AVAIL, msg, target.getSrcFile(), logMsgSrcId);

		availSchemaVer = identifyXsdVersion(target);
		loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, "Validating using Avails Schema Version " + availSchemaVer,
				curFile, logMsgSrcId);
		setAvailVersion(availSchemaVer);
		rootNS = availsNSpace;

		curTarget = target;
		curRootEl = null;
		curFile = target.getSrcFile();
		curFileName = curFile.getName();
		this.pedigreeMap = pedigreeMap;
		curFileIsValid = true;

		validateXml(target);
		if (!curFileIsValid) {
			msg = "Schema validation check FAILED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, curFile, logMsgSrcId);
		} else {
			curRootEl = target.getXmlDoc().getRootElement();
			msg = "Schema validation check PASSED";
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
	 * Validate everything that is fully specified via the XSD schemas.
	 * 
	 * @param target wrapper containing MDDF construct to be validated
	 * @return true if construct passes without errors.
	 */
	protected boolean validateXml(MddfTarget target) {
		String xsdFile = XsdValidation.defaultRsrcLoc + "avails-v" + availSchemaVer + ".xsd";
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		curFileIsValid = xsdHelper.validateXml(target, xsdFile, logMsgSrcId);
		return curFileIsValid;
	}

	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateConstraints() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.LEV_INFO, "Validating constraints", curFile, LOGMSG_ID);
		super.validateConstraints();

		SchemaWrapper availSchema = SchemaWrapper.factory("avails-v" + availSchemaVer);

		validateNotEmpty(availSchema);

		/*
		 * Validate the usage of controlled vocab (i.e., places where XSD specifies a
		 * xs:string but the documentation specifies an enumerated set of allowed
		 * values).
		 */
		// start with Common Metadata spec..
		validateCMVocab();

		// Now do any defined in Avails spec..
		validateAvailVocab();

		validateUsage();

		switch (availSchemaVer) {
		case "2.4":
			/* Validate indexed sequences that must be continuously increasing */
			validateIndexing("BundledAsset", availsNSpace, "sequence", "Asset", availsNSpace, true, true, true, false);
		case "2.3":
		case "2.2.2":
		case "2.2.1":
		case "2.2":
		case "2.1":
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.util.CMValidator#validateIdSet()
	 */
	protected void validateIdSet() {
		// do set-up and init....
		super.validateIdSet();

		// TODO: Load from JSON file....
		validateId("ALID", null, true, false);
	}

	/**
	 * @return
	 */
	private void validateAvailVocab() {
		Namespace primaryNS = availsNSpace;
		String doc = "AVAIL";
		String vocabVer = availSchemaVer;
		/*
		 * handle any case of backwards (or forwards) compatibility between versions.
		 */
		switch (availSchemaVer) {
		case "2.2.1":
			vocabVer = "2.2";
		}

		JSONObject availVocab = (JSONObject) getVocabResource("avail", vocabVer);
		if (availVocab == null) {
			String msg = "Unable to validate controlled vocab: missing resource file";
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, msg, curFile, logMsgSrcId);
			curFileIsValid = false;
			return;
		}

		JSONArray allowed = availVocab.optJSONArray("AvailType");
		LogReference docRef = LogReference.getRef(doc, "avail01");
		validateVocab(primaryNS, "Avail", primaryNS, "AvailType", allowed, docRef, true, true);

		allowed = availVocab.optJSONArray("EntryType");
		docRef = LogReference.getRef(doc, "avail02");
		validateVocab(primaryNS, "Disposition", primaryNS, "EntryType", allowed, docRef, true, true);

		allowed = availVocab.optJSONArray("AltIdentifier@scope");
		docRef = LogReference.getRef(doc, "avail03");
		validateVocab(primaryNS, "AltIdentifier", null, "@scope", allowed, docRef, true, true);

		allowed = availVocab.optJSONArray("LocalizationOffering");
		docRef = LogReference.getRef(doc, "avail03");
		validateVocab(primaryNS, "Metadata", primaryNS, "LocalizationOffering", allowed, docRef, true, true);
		validateVocab(primaryNS, "EpisodeMetadata", primaryNS, "LocalizationOffering", allowed, docRef, true, true);

		allowed = availVocab.optJSONArray("SeasonStatus");
		docRef = LogReference.getRef(doc, "avail04");
		validateVocab(primaryNS, "SeasonMetadata", primaryNS, "SeasonStatus", allowed, docRef, true, true);

		allowed = availVocab.optJSONArray("SeriesStatus");
		docRef = LogReference.getRef(doc, "avail05");
		validateVocab(primaryNS, "SeriesMetadata", primaryNS, "SeriesStatus", allowed, docRef, true, true);

		allowed = availVocab.optJSONArray("DateTimeCondition");
		docRef = LogReference.getRef(doc, "avail06");
		validateVocab(primaryNS, "Transaction", primaryNS, "StartCondition", allowed, docRef, true, false);
		validateVocab(primaryNS, "Transaction", primaryNS, "EndCondition", allowed, docRef, true, false);

		allowed = availVocab.optJSONArray("LicenseType");
		docRef = LogReference.getRef(doc, "avail07");
		validateVocab(primaryNS, "Transaction", primaryNS, "LicenseType", allowed, docRef, true, false);

		allowed = availVocab.optJSONArray("Language@asset");
		docRef = LogReference.getRef(doc, "avail07");
		validateVocab(primaryNS, "AllowedLanguage", null, "@asset", allowed, docRef, true, true);
		validateVocab(primaryNS, "AssetLanguage", null, "@asset", allowed, docRef, true, true);
		validateVocab(primaryNS, "HoldbackLanguage", null, "@asset", allowed, docRef, true, true);

		// allowed = availVocab.optJSONArray("LicenseRightsDescription");
		// srcRef = LogReference.getRef(doc, "avail07");
		// validateVocab(primaryNS, "Transaction", primaryNS,
		// "LicenseRightsDescription", allowed, srcRef, true, false);

		allowed = availVocab.optJSONArray("FormatProfile");
		docRef = LogReference.getRef(doc, "avail07");
		validateVocab(primaryNS, "Transaction", primaryNS, "FormatProfile", allowed, docRef, true, true);

		allowed = availVocab.optJSONArray("ExperienceCondition");
		docRef = LogReference.getRef(doc, "avail07");
		validateVocab(primaryNS, "Transaction", primaryNS, "ExperienceCondition", allowed, docRef, true, true);

		allowed = availVocab.optJSONArray("Term@termName");
		docRef = LogReference.getRef(doc, "avail08");
		Collection<Namespace> nSpaces = new HashSet<Namespace>();
		nSpaces.add(primaryNS);
		int tag4log = getLogTag(primaryNS, null);
		boolean strict = false; // allows for contract-specific terminology
		validateVocab(nSpaces, "//avails:Term/@termName", true, allowed, docRef, true, strict, tag4log, "@termName");

		allowed = availVocab.optJSONArray("SharedEntitlement@ecosystem");
		docRef = LogReference.getRef(doc, "avail09");
		validateVocab(primaryNS, "SharedEntitlement", null, "@ecosystem", allowed, docRef, true, true);

		// ===========================================================
		/* For Transactions in US, check USACaptionsExemptionReason */
		allowed = availVocab.optJSONArray("USACaptionsExemptionReason");
		docRef = LogReference.getRef(doc, "avail03");
		validateVocab(primaryNS, "Asset", primaryNS, "USACaptionsExemptionReason", allowed, docRef, true, true);
		validateVocab(primaryNS, "EpisodeMetadata", primaryNS, "USACaptionsExemptionReason", allowed, docRef, true,
				true);
		validateVocab(primaryNS, "SeasonMetadata", primaryNS, "USACaptionsExemptionReason", allowed, docRef, true,
				true);
		validateVocab(primaryNS, "SeriesMetadata", primaryNS, "USACaptionsExemptionReason", allowed, docRef, true,
				true);

		// added for v2.3
		tag4log = getLogTag(primaryNS, null);
		allowed = availVocab.optJSONArray("@termName='TitleStatus'");
		nSpaces = new HashSet<Namespace>();
		nSpaces.add(primaryNS);
		validateVocab(nSpaces, "//avails:Term/avails:Text[../@termName='TitleStatus']", false, allowed, docRef, true,
				true, tag4log, "@termName='TitleStatus'");

	}

	/**
	 * Checks values specified via enumerations that are not contained in the XSD
	 * schemas.
	 */
	protected void validateCMVocab() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_AVAIL, "Validating use of controlled vocabulary...", curFile,
				LOGMSG_ID);
		Namespace primaryNS = availsNSpace;
		/*
		 * Validate use of Country identifiers starting with use of @region attribute
		 */
		validateRegion(primaryNS);

		// added for CM v2.7, Avails v2.4:
		JSONObject cmVocab = (JSONObject) getVocabResource("cm", CM_VER);
		if (cmVocab == null) {
			String msg = "Unable to validate controlled vocab: missing resource file";
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, msg, curFile, logMsgSrcId);
			curFileIsValid = false;
			return;
		}
		JSONArray expectedValues = cmVocab.optJSONArray("GroupingEntity/Type");
		LogReference docRef = LogReference.getRef("CM", "cm_gType");
		validateVocab(primaryNS, "GroupingEntity", mdNSpace, "Type", expectedValues, docRef, true, false);

		expectedValues = cmVocab.optJSONArray("Gender");
		docRef = LogReference.getRef("CM", CM_VER, "cm_gender");
		validateVocab(availsNSpace, "People", mdNSpace, "Gender", expectedValues, docRef, true, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.util.CMValidator#validateUsage()
	 */
	protected void validateUsage() {
		super.validateUsage();
		/*
		 * Load JSON that defines various constraints on structure of an Avails. This is
		 * version-specific but not all schema versions have their own unique struct
		 * file (e.g., a minor release may be compatible with a previous release).
		 */
		String structVer = null;
		switch (availSchemaVer) {
		case "2.3":
			structVer = "2.3";
			break;
		case "2.2.2":
			structVer = "2.2.2";
			break;
		case "2.1":
		case "2.2":
		case "2.2.1":
		default:
			structVer = "2.2";
		}

		loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL,
				"Validating structure using v" + structVer + " requirements", curFile, LOGMSG_ID);

		JSONObject availStructDefs = XmlIngester.getMddfResource("structure_avail", structVer);
		if (availStructDefs == null) {
			// LOG a FATAL problem.
			String msg = "Unable to process; missing structure definitions for Avails v" + availSchemaVer;
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, msg, curFile, logMsgSrcId);
			return;
		}

		JSONObject rqmtSet = availStructDefs.getJSONObject("StrucRqmts");
		Iterator<String> keys = rqmtSet.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject rqmtSpec = rqmtSet.getJSONObject(key);
			// NOTE: This block of code requires a 'targetPath' be defined
			if (rqmtSpec.has("targetPath")) {
				loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_AVAIL, "Structure check; key= " + key, curFile,
						logMsgSrcId);
				curFileIsValid = structHelper.validateDocStructure(curRootEl, rqmtSpec) && curFileIsValid;
			}
		}
		return;
	}
	// ########################################################################

	/**
	 * Log an issue after first determining the appropriate <i>target</i> to
	 * associate with the log entry. The target indicates a construct within a file
	 * being validated and should be specified as either
	 * <ul>
	 * <li>an JDOM Element within an XML file, or</li>
	 * <li>a <tt>POI Cell</tt> instance used to identify a cell in an XLSX
	 * spreadsheet.</li>
	 * </ul>
	 * Note that validation of XLSX files is only supported by the Avails validator
	 * and that other validator classes do not, therefore, require this intermediate
	 * stage when logging.
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
