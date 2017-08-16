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
import java.util.Iterator;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.logging.IssueLogger;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.CMValidator;
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
public class AvailValidator extends CMValidator implements IssueLogger {

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

	static final LogReference AVAIL_RQMT_srcRef = LogReference.getRef("AVAIL", "avail01");

	// private JSONObject availTypeStruct;
	private JSONArray genericAvailTypes;
	private Map<Object, Pedigree> pedigreeMap;

	private String availSchemaVer;

	static {
		id2typeMap = new HashMap<String, String>();

	}

	/**
	 * @param validateC
	 * @param loggingMgr
	 */
	public AvailValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;

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
		String msg = "Begining validation of Avails...";
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_AVAIL, msg, curFile, logMsgSrcId);
		curRootEl = null;
		curFile = xmlFile;
		curFileName = xmlFile.getName();
		this.pedigreeMap = pedigreeMap;
		curFileIsValid = true;

		availSchemaVer = identifyXsdVersion(docRootEl);
		loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, "Validating using Avails Schema Version " + availSchemaVer,
				srcFile, logMsgSrcId);
		setAvailVersion(availSchemaVer);
		rootNS = availsNSpace;

		validateXml(xmlFile, docRootEl);
		if (!curFileIsValid) {
			msg = "Schema validation check FAILED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, curFile, logMsgSrcId);
		} else {
			curRootEl = docRootEl; // getAsXml(xmlFile);
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
	 * Validate everything that is fully specified via the XSD.
	 * 
	 * @param xmlFile
	 */
	protected boolean validateXml(File srcFile, Element docRootEl) {
		String xsdFile = XsdValidation.defaultRsrcLoc + "avails-v" + availSchemaVer + ".xsd";
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		curFileIsValid = xsdHelper.validateXml(srcFile, docRootEl, xsdFile, logMsgSrcId);
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

		// TODO: Load from JSON file....

		validateId("ALID", null, true, false);

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
	private void validateAvailVocab() {
		Namespace primaryNS = availsNSpace;
		String doc = "AVAIL";
		String vocabVer = availSchemaVer;
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
		LogReference srcRef = LogReference.getRef(doc, "avail01");
		validateVocab(primaryNS, "Avail", primaryNS, "AvailType", allowed, srcRef, true);

		allowed = availVocab.optJSONArray("EntryType");
		srcRef = LogReference.getRef(doc, "avail02");
		validateVocab(primaryNS, "Disposition", primaryNS, "EntryType", allowed, srcRef, true);

		allowed = availVocab.optJSONArray("AltIdentifier@scope");
		srcRef = LogReference.getRef(doc, "avail03");
		validateVocab(primaryNS, "AltIdentifier", primaryNS, "@scope", allowed, srcRef, true);

		allowed = availVocab.optJSONArray("LocalizationOffering");
		srcRef = LogReference.getRef(doc, "avail03");
		validateVocab(primaryNS, "Metadata", primaryNS, "LocalizationOffering", allowed, srcRef, true);
		validateVocab(primaryNS, "EpisodeMetadata", primaryNS, "LocalizationOffering", allowed, srcRef, true);

		allowed = availVocab.optJSONArray("SeasonStatus");
		srcRef = LogReference.getRef(doc, "avail04");
		validateVocab(primaryNS, "SeasonMetadata", primaryNS, "SeasonStatus", allowed, srcRef, true);

		allowed = availVocab.optJSONArray("SeriesStatus");
		srcRef = LogReference.getRef(doc, "avail05");
		validateVocab(primaryNS, "SeriesMetadata", primaryNS, "SeriesStatus", allowed, srcRef, true);

		allowed = availVocab.optJSONArray("DateTimeCondition");
		srcRef = LogReference.getRef(doc, "avail06");
		validateVocab(primaryNS, "Transaction", primaryNS, "StartCondition", allowed, srcRef, true, false);
		validateVocab(primaryNS, "Transaction", primaryNS, "EndCondition", allowed, srcRef, true, false);

		allowed = availVocab.optJSONArray("LicenseType");
		srcRef = LogReference.getRef(doc, "avail07");
		validateVocab(primaryNS, "Transaction", primaryNS, "LicenseType", allowed, srcRef, true, false);

		allowed = availVocab.optJSONArray("Language@asset");
		srcRef = LogReference.getRef(doc, "avail07");
		validateVocab(primaryNS, "AllowedLanguage", null, "@asset", allowed, srcRef, true);
		validateVocab(primaryNS, "AssetLanguage", null, "@asset", allowed, srcRef, true);
		validateVocab(primaryNS, "AssetLanguage", null, "@assetProvided", allowed, srcRef, true);
		validateVocab(primaryNS, "HoldbackLanguage", null, "@asset", allowed, srcRef, true);

		// allowed = availVocab.optJSONArray("LicenseRightsDescription");
		// srcRef = LogReference.getRef(doc, "avail07");
		// validateVocab(primaryNS, "Transaction", primaryNS,
		// "LicenseRightsDescription", allowed, srcRef, true, false);

		allowed = availVocab.optJSONArray("FormatProfile");
		srcRef = LogReference.getRef(doc, "avail07");
		validateVocab(primaryNS, "Transaction", primaryNS, "FormatProfile", allowed, srcRef, true);

		allowed = availVocab.optJSONArray("ExperienceCondition");
		srcRef = LogReference.getRef(doc, "avail07");
		validateVocab(primaryNS, "Transaction", primaryNS, "ExperienceCondition", allowed, srcRef, true);

		allowed = availVocab.optJSONArray("Term@termName");
		srcRef = LogReference.getRef(doc, "avail08");
		validateVocab(primaryNS, "Term", primaryNS, "@termName", allowed, srcRef, false);

		allowed = availVocab.optJSONArray("SharedEntitlement@ecosystem");
		srcRef = LogReference.getRef(doc, "avail09");
		validateVocab(primaryNS, "SharedEntitlement", primaryNS, "@ecosystem", allowed, srcRef, true);

		// ===========================================================
		/* For Transactions in US, check USACaptionsExemptionReason */
		allowed = availVocab.optJSONArray("USACaptionsExemptionReason");
		srcRef = LogReference.getRef(doc, "avail03");
		validateVocab(primaryNS, "Asset", primaryNS, "USACaptionsExemptionReason", allowed, srcRef, true);
		validateVocab(primaryNS, "EpisodeMetadata", primaryNS, "USACaptionsExemptionReason", allowed, srcRef, true);
		validateVocab(primaryNS, "SeasonMetadata", primaryNS, "USACaptionsExemptionReason", allowed, srcRef, true);
		validateVocab(primaryNS, "SeriesMetadata", primaryNS, "USACaptionsExemptionReason", allowed, srcRef, true);
	}

	/**
	 * @return
	 */
	protected void validateCMVocab() {
		loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.LEV_INFO, "Validating use of controlled vocabulary...", curFile,
				LOGMSG_ID);
		Namespace primaryNS = availsNSpace;
		/*
		 * Validate use of Country identifiers....
		 */
		// In 'Metadata/Release History/DistrTerritory'
		validateRegion(mdNSpace, "DistrTerritory", mdNSpace, "country");

		// in Transaction/Territory...
		validateRegion(primaryNS, "Territory", mdNSpace, "country");
		validateRegion(primaryNS, "TerritoryExcluded", mdNSpace, "country");

		// in 'Term/Region
		validateRegion(primaryNS, "Region", mdNSpace, "country");

		// in multiple places
		validateRegion(mdNSpace, "Region", mdNSpace, "country");

		/* Validate language codes */

		validateLanguage(primaryNS, "LocalSeriesTitle", primaryNS, "@language");
		validateLanguage(primaryNS, "Transaction", primaryNS, "AllowedLanguage");
		validateLanguage(primaryNS, "Transaction", primaryNS, "AssetLanguage");
		validateLanguage(primaryNS, "Transaction", primaryNS, "HoldbackLanguage");
		validateLanguage(primaryNS, "Term", primaryNS, "Language");

		/*
		 * Additions for v2.2.2
		 */
		validateRegion(primaryNS, "TitleInternalAlias", null, "@region");
		validateRegion(primaryNS, "SeasonTitleInternalAlias", null, "@region");
		validateRegion(primaryNS, "SeriesTitleInternalAlias", null, "@region");

		validateLanguage(primaryNS, "TitleDisplayUnlimited", primaryNS, "@language");
		validateLanguage(primaryNS, "SeasonTitleDisplayUnlimited", primaryNS, "@language");
		validateLanguage(primaryNS, "SeriesTitleDisplayUnlimited", primaryNS, "@language");
	}

	/**
	 * Check for consistent usage. This typically means that an OPTIONAL element
	 * will be either REQUIRED or INVALID for certain use-cases (e.g.
	 * BundledAsset is only allowed when WorkType is 'Collection').
	 * <p>
	 * Validation is based primarily on the <i>structure definitions</i> defined
	 * in a version-specific JSON file. This will define various criteria that
	 * must be satisfied for a given usage. The criteria are specified in the
	 * form of XPATHs.
	 * </p>
	 * 
	 * @return
	 */
	private void validateUsage() {
		loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.LEV_INFO, "Validating structure...", curFile, LOGMSG_ID);

		/*
		 * Load JSON that defines various constraints on structure of an Avails.
		 * This is version-specific but not all schema versions have their own
		 * unique struct file (e.g., a minor release may be compatible with a
		 * previous release).
		 */
		String structVer = null;
		switch (availSchemaVer) {
		case "2.1":
		case "2.2":
		case "2.2.1":
		case "2.2.2": 
		default:
			structVer = "2.2";
		}

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
		// ==============================================

		/* Validate any Ratings information */
		validateRatings();

		return;
	}
	// ########################################################################

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
