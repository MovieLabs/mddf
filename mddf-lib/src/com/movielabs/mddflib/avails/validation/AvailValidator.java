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
import java.lang.reflect.Array;
import java.util.Arrays;
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

import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.AbstractValidator;
import com.movielabs.mddflib.util.xml.SchemaWrapper;
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
public class AvailValidator extends AbstractValidator {

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

	private static final String DOC_VER = "2.1";

	private static JSONObject availVocab;

	private Map<Object, Pedigree> pedigreeMap;

	static {
		id2typeMap = new HashMap<String, String>();

		/*
		 * Is there a controlled vocab that is specific to a Manifest? Note the
		 * vocab set for validating Common Metadata will be loaded by the parent
		 * class AbstractValidator.
		 */
		try {
			availVocab = loadVocab(vocabRsrcPath, "Avail");
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
		rootNS = availsNSpace;
		rootPrefix = "avails:";

		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_AVAIL;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.util.AbstractValidator#process(java.io.File)
	 */
	@Override
	public boolean process(File xmlManifestFile) throws IOException, JDOMException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean process(File xmlFile, Element rootEl, Map<Object, Pedigree> pedigreeMap)
			throws IOException, JDOMException {
		curFile = xmlFile;
		curFileName = xmlFile.getName();
		this.pedigreeMap = pedigreeMap;
		curFileIsValid = true;
		// if (xmlFile.getName().endsWith(".xml")) {
		validateXml(xmlFile);
		// }
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
		String xsdFile = "./resources/avails-v" + XmlIngester.AVAIL_VER + ".xsd";
		// String xsdFile = SchemaWrapper.RSRC_PACKAGE + "avails-v" +
		// XmlIngester.AVAIL_VER + ".xsd";
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		curFileIsValid = validateXml(xmlFile, curRootEl, xsdFile, logMsgSrcId);
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
		String docVer = "2.1";

		JSONArray allowed = availVocab.optJSONArray("AvailType");
		LogReference srcRef = LogReference.getRef(doc, docVer, "avail01");
		allOK = validateVocab(primaryNS, "Avail", primaryNS, "AvailType", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("EntryType");
		srcRef = LogReference.getRef(doc, docVer, "avail02");
		allOK = validateVocab(primaryNS, "Disposition", primaryNS, "EntryType", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("AltIdentifier@scope");
		srcRef = LogReference.getRef(doc, docVer, "avail03");
		allOK = validateVocab(primaryNS, "AltIdentifier", primaryNS, "@scope", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("LocalizationOffering");
		srcRef = LogReference.getRef(doc, docVer, "avail03");
		allOK = validateVocab(primaryNS, "Metadata", primaryNS, "LocalizationOffering", allowed, srcRef, true) && allOK;
		allOK = validateVocab(primaryNS, "EpisodeMetadata", primaryNS, "LocalizationOffering", allowed, srcRef, true)
				&& allOK;
		allowed = availVocab.optJSONArray("SeasonStatus");
		srcRef = LogReference.getRef(doc, docVer, "avail04");
		allOK = validateVocab(primaryNS, "SeasonMetadata", primaryNS, "SeasonStatus", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("SeriesStatus");
		srcRef = LogReference.getRef(doc, docVer, "avail05");
		allOK = validateVocab(primaryNS, "SeriesMetadata", primaryNS, "SeriesStatus", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("DateTimeCondition");
		srcRef = LogReference.getRef(doc, docVer, "avail06");
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "StartCondition", allowed, srcRef, true) && allOK;
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "EndCondition", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("LicenseType");
		srcRef = LogReference.getRef(doc, docVer, "avail07");
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "LicenseType", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("LicenseRightsDescription");
		srcRef = LogReference.getRef(doc, docVer, "avail07");
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "LicenseRightsDescription", allowed, srcRef, true)
				&& allOK;

		allowed = availVocab.optJSONArray("FormatProfile");
		srcRef = LogReference.getRef(doc, docVer, "avail07");
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "FormatProfile", allowed, srcRef, true) && allOK;

		allowed = availVocab.optJSONArray("ExperienceCondition");
		srcRef = LogReference.getRef(doc, docVer, "avail07");
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "ExperienceCondition", allowed, srcRef, true)
				&& allOK;

		allowed = availVocab.optJSONArray("Term@termName");
		srcRef = LogReference.getRef(doc, docVer, "avail08");
		allOK = validateVocab(primaryNS, "Term", primaryNS, "@termName", allowed, srcRef, false) && allOK;

		allowed = availVocab.optJSONArray("SharedEntitlement@ecosystem");
		srcRef = LogReference.getRef(doc, docVer, "avail09");
		allOK = validateVocab(primaryNS, "SharedEntitlement", primaryNS, "@ecosystem", allowed, srcRef, true) && allOK;
		return allOK;
	}

	/**
	 * @return
	 */
	protected boolean validateCMVocab() {
		boolean allOK = true;
		String mdVersion = "2.4";
		Namespace primaryNS = availsNSpace; /*
											 * Validate use of Country
											 * identifiers....
											 */
		JSONArray iso3691 = cmVocab.optJSONArray("ISO3691");
		LogReference srcRef = LogReference.getRef("CM", mdVersion, "cm_regions");

		// In 'Metadata/Release History/DistrTerritory'
		allOK = validateVocab(mdNSpace, "DistrTerritory", mdNSpace, "country", iso3691, srcRef, true) && allOK;

		// in Transaction/Territory...
		allOK = validateVocab(primaryNS, "Territory", mdNSpace, "country", iso3691, srcRef, true) && allOK;
		allOK = validateVocab(primaryNS, "TerritoryExcluded", mdNSpace, "country", iso3691, srcRef, true) && allOK;

		// in 'Term/Region
		allOK = validateVocab(primaryNS, "Region", mdNSpace, "country", iso3691, srcRef, true) && allOK;

		// in multiple places
		allOK = validateVocab(mdNSpace, "Region", mdNSpace, "country", iso3691, srcRef, true) && allOK;

		/* Validate language codes */

		JSONArray rfc5646 = cmVocab.optJSONArray("RFC5646");
		srcRef = LogReference.getRef("CM", mdVersion, "cm_lang");
		allOK = validateVocab(primaryNS, "LocalSeriesTitle", primaryNS, "@language", rfc5646, srcRef, true) && allOK;
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "AllowedLanguage", rfc5646, srcRef, true) && allOK;
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "AssetLanguage", rfc5646, srcRef, true) && allOK;
		allOK = validateVocab(primaryNS, "Transaction", primaryNS, "HoldbackLanguage", rfc5646, srcRef, true) && allOK;
		allOK = validateVocab(primaryNS, "Term", primaryNS, "Language", rfc5646, srcRef, true) && allOK;
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
		LogReference srcRef = LogReference.getRef("AVAIL", DOC_VER, "avail01");
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
		// TODO: Load from JSON

		String[] allowedWTypes = { "Movie", "Short" };
		validateTypeCompatibility("single", allowedWTypes, 1);

		allowedWTypes = (String[]) Array.newInstance(String.class, 1);
		allowedWTypes[0] = "Episode";
		validateTypeCompatibility("episode", allowedWTypes, 1);

		allowedWTypes = (String[]) Array.newInstance(String.class, 1);
		allowedWTypes[0] = "Season";
		validateTypeCompatibility("season", allowedWTypes, 1);

		allowedWTypes = (String[]) Array.newInstance(String.class, 1);
		allowedWTypes[0] = "Series";
		validateTypeCompatibility("series", allowedWTypes, -1);
		validateTypeCompatibility("miniseries", allowedWTypes, -1);

		allowedWTypes = (String[]) Array.newInstance(String.class, 1);
		allowedWTypes[0] = "Collection";
		validateTypeCompatibility("bundle", allowedWTypes, -1);

		allowedWTypes = (String[]) Array.newInstance(String.class, 1);
		allowedWTypes[0] = "Supplemental";
		validateTypeCompatibility("supplement", allowedWTypes, -1);

		allowedWTypes = (String[]) Array.newInstance(String.class, 1);
		allowedWTypes[0] = "Promotion";
		validateTypeCompatibility("promotion", allowedWTypes, -1);
		// ===========================================================
		/* For Transactions in US, check USACaptionsExemptionReason */

		return;
	}

	/**
	 * Check that the Avail/AvailType is compatible with all child
	 * Asset/WorkTypes
	 * 
	 * @param availType
	 * @param allowedWTypes
	 */
	private void validateTypeCompatibility(String availType, String[] allowedWTypes, int max) {
		LogReference srcRef = LogReference.getRef("AVAIL", DOC_VER, "avail01");
		List<String> allowedTypeList = Arrays.asList(allowedWTypes);

		String path = ".//" + rootPrefix + "AvailType[text()='" + availType + "']";
		XPathExpression<Element> xpExp01 = xpfac.compile(path, Filters.element(), null, availsNSpace);
		List<Element> availTypeElList = xpExp01.evaluate(curRootEl);
		for (int i = 0; i < availTypeElList.size(); i++) {
			Element availTypeEl = availTypeElList.get(i);
			Element availEl = availTypeEl.getParentElement();
			// Now get the descendant WorkType element
			XPathExpression<Element> xpExp02 = xpfac.compile("./" + rootPrefix + "Asset/" + rootPrefix + "WorkType",
					Filters.element(), null, availsNSpace);
			List<Element> workTypeElList = xpExp02.evaluate(availEl);
			for (int j = 0; j < workTypeElList.size(); j++) {
				Element wrkTypeEl = workTypeElList.get(j);
				String wtValue = wrkTypeEl.getText();
				if (!(allowedTypeList.contains(wtValue))) {
					String msg = "AvailType is incompatible with WorkType";
					String explanation = null;
					logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, wrkTypeEl, msg, explanation, srcRef, logMsgSrcId);
					curFileIsValid = false;
				}

			}
			/*
			 * Might as well use the Asset/WorkType to check the number of Assets is within limits
			 */
			if(max > 0 && ( workTypeElList.size()> max)){
				String msg = "Invalid Asset structure for specified AvailType";
				String explanation = "Avail of type='"+availType+"' is only allowed a max of "+max+" Assets";
				logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, availTypeEl, msg, explanation, srcRef, logMsgSrcId);
				curFileIsValid = false;
			}
		}

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
