/**
 * Created Nov 12, 2019 
 * Copyright Motion Picture Laboratories, Inc. 2019
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
package com.movielabs.mddflib.status.offer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

/**
 * Validates an OfferStatus file as conforming to the specification and
 * best-practice guidance.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class OfferStatusValidator extends CMValidator implements IssueLogger {

	public static final String LOGMSG_ID = "OStatusValidator"; 
 

	private Map<Object, Pedigree> pedigreeMap;

	private String availSchemaVer;

	/**
	 * @param validateC
	 * @param loggingMgr
	 */
	public OfferStatusValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;

		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_OFFER; 
	}

	public boolean process(MddfTarget target) {
		return process(target, null);
	}

	/**
	 * Validate a single Offer Status document. The data is structured as an XML
	 * document regardless of the original source file's format. This means that
	 * when a file has been provided as an Excel file, it will have already been
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
		curTarget = target;
		curFile = target.getSrcFile();
		curLoggingFolder = loggingMgr.pushFileContext(target);
		String msg = "Begining validation of Offer Status...";

		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_OFFER, msg, curTarget, logMsgSrcId);

		availSchemaVer = identifyXsdVersion(target);

		switch (availSchemaVer) {
		case "2.6":
			break;
		case "2.5":
			break;
		default:
			msg = "Schema version " + availSchemaVer + " does not support Offer Status";
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_OFFER, msg, curTarget, logMsgSrcId);
			return false;
		}
		loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, "Validating using Avail Schema Version " + availSchemaVer,
				curTarget, logMsgSrcId);
		setAvailVersion(availSchemaVer);
		rootNS = availsNSpace;

		curRootEl = null;
		curFileName = curFile.getName();
		this.pedigreeMap = pedigreeMap;
		curFileIsValid = true;

		validateXml(target);
		if (!curFileIsValid) {
			msg = "Schema validation check FAILED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_OFFER, msg, curTarget, logMsgSrcId);
		} else {
			curRootEl = target.getXmlDoc().getRootElement();
			msg = "Schema validation check PASSED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_OFFER, msg, curTarget, logMsgSrcId);
			if (validateC) {
				initializeIdChecks();
				validateConstraints();
			}
		}
		// clean up and go home
		this.pedigreeMap = null;
		loggingMgr.popFileContext(target);
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
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.LEV_INFO, "Validating constraints", curTarget, LOGMSG_ID);
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
//		switch (availSchemaVer) {
//		case "2.2.1":
//			vocabVer = "2.2";
//		}

		JSONObject availVocab = (JSONObject) getVocabResource("avail", vocabVer);
		if (availVocab == null) {
			String msg = "Unable to validate controlled vocab: missing resource file vocab_avail_v" + vocabVer;
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_OFFER, msg, curTarget, logMsgSrcId);
			curFileIsValid = false;
			return;
		}

		JSONArray allowed;
		LogReference docRef;

		allowed = availVocab.optJSONArray("LicenseType");
		docRef = LogReference.getRef(doc, "avail07");
		validateVocab(primaryNS, "OfferStatus", primaryNS, "LicenseType", allowed, docRef, true, false);

		allowed = availVocab.optJSONArray("DateTimeCondition");
		docRef = LogReference.getRef(doc, "avail06");
		validateVocab(primaryNS, "OverallStatus", primaryNS, "StartCondition", allowed, docRef, true, true);
		validateVocab(primaryNS, "OverallStatus", primaryNS, "EndCondition", allowed, docRef, true, true);
		validateVocab(primaryNS, "FeatureStatus", primaryNS, "StartCondition", allowed, docRef, true, true);
		validateVocab(primaryNS, "FeatureStatus", primaryNS, "EndCondition", allowed, docRef, true, true);
		validateVocab(primaryNS, "BonusStatus", primaryNS, "StartCondition", allowed, docRef, true, true);
		validateVocab(primaryNS, "BonusStatus", primaryNS, "EndCondition", allowed, docRef, true, true);

		allowed = availVocab.optJSONArray("Language@asset");
		docRef = LogReference.getRef(doc, "avail07");
		validateVocab(primaryNS, "AssetLanguage", null, "@asset", allowed, docRef, true, true);

		allowed = availVocab.optJSONArray("FormatProfile");
		docRef = LogReference.getRef(doc, "avail07");
		validateVocab(primaryNS, "Transaction", primaryNS, "FormatProfile", allowed, docRef, true, true);

		allowed = availVocab.optJSONArray("ProgressCode");
		docRef = null;// LogReference.getRef(doc, "avail07");
		validateVocab(primaryNS, "OverallStatus", primaryNS, "ProgressCode", allowed, docRef, true, true);
		validateVocab(primaryNS, "FeatureStatus", primaryNS, "ProgressCode", allowed, docRef, true, true);
		validateVocab(primaryNS, "BonusStatus", primaryNS, "ProgressCode", allowed, docRef, true, true);

		allowed = availVocab.optJSONArray("Term@termName");
		docRef = LogReference.getRef(doc, "avail08");
		Collection<Namespace> nSpaces = new HashSet<Namespace>();
		nSpaces.add(primaryNS);
		int tag4log = getLogTag(primaryNS, null);
		boolean strict = false; // allows for contract-specific terminology
		validateVocab(nSpaces, "//avails:Term/@termName", true, allowed, docRef, true, strict, tag4log, "@termName");

	}

	/**
	 * Checks values specified via enumerations that are not contained in the XSD
	 * schemas.
	 */
	protected void validateCMVocab() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_OFFER, "Validating use of controlled vocabulary...", curTarget,
				LOGMSG_ID);
		Namespace primaryNS = availsNSpace;
		/*
		 * Validate use of Country identifiers starting with use of @region attribute
		 */
		validateRegion(primaryNS);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.util.CMValidator#validateUsage()
	 */
	protected void validateUsage() {
		/*
		 * so far the only structural checks at the CM level involve Digital Assets,
		 * Since OrderStatus doesn't include any of those elements, we can skip the call
		 * to super.validateUsage();
		 */
		// super.validateUsage();
		
		// No struct checks YET for OrderStatus so we're done.
	}
 

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddflib.util.CMValidator#setParent(com.movielabs.mddflib.util.
	 * CMValidator)
	 */
	@Override
	protected void setParent(CMValidator parent) {
		this.parent = parent;
	}
}
