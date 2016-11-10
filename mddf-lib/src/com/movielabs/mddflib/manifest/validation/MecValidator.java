/**
 * Created Nov 07, 2016 
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
package com.movielabs.mddflib.manifest.validation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import com.movielabs.mddflib.util.xml.RatingSystem;
import com.movielabs.mddflib.util.xml.SchemaWrapper;
import com.movielabs.mddflib.util.xml.XmlIngester;

public class MecValidator extends AbstractValidator {

	public static final String LOGMSG_ID = "MecValidator";

	static final String DOC_VER = "2.4";
	// static final LogReference AVAIL_RQMT_srcRef =
	// LogReference.getRef("AVAIL", DOC_VER, "avail01");

	private static Properties iso3166_1_codes;

	static {
		id2typeMap = new HashMap<String, String>();

		try {
			/*
			 * There is no controlled vocab that is specific to a MEC file. Note
			 * the vocab set for validating Common Metadata will be loaded by
			 * the parent class AbstractValidator.
			 */

			/*
			 * ISO codes are simple so we use Properties
			 */
			String iso3166RsrcPath = "/com/movielabs/mddf/resources/ISO3166-1.properties";
			iso3166_1_codes = loadProperties(iso3166RsrcPath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @param validateC
	 */
	public MecValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;
		rootNS = mdmecNSpace;
		rootPrefix = "mdmec:";

		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_MEC;
	}

	public boolean process(File xmlFile, Element docRootEl) throws IOException, JDOMException {
		curRootEl = null;
		curFile = xmlFile;
		curFileName = xmlFile.getName();
		curFileIsValid = true;
		validateXml(xmlFile, docRootEl);
		// }
		if (!curFileIsValid) {
			String msg = "Schema validation check FAILED";
			loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, msg, curFile, logMsgSrcId);
			// return false;
		} else {
			curRootEl = docRootEl; // getAsXml(xmlFile);
			String msg = "Schema validation check PASSED";
			loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, msg, curFile, logMsgSrcId);
			if (validateC) {
				validateConstraints();
			}
		}
		// clean up and go home
		return curFileIsValid;
	}

	/**
	 * Validate everything that is fully specified via the XSD.
	 * 
	 * @param xmlFile
	 */
	protected boolean validateXml(File srcFile, Element docRootEl) {
		String xsdFile = XmlIngester.defaultRsrcLoc + "mdmec-v" + XmlIngester.MDMEC_VER + ".xsd";
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		curFileIsValid = validateXml(srcFile, docRootEl, xsdFile, logMsgSrcId);
		return curFileIsValid;
	}

	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateConstraints() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Validating constraints", curFile, LOGMSG_ID);
		super.validateConstraints();

		SchemaWrapper availSchema = SchemaWrapper.factory("mdmec-v" + XmlIngester.MDMEC_VER);
		List<String> reqElList = availSchema.getReqElList();
		for (int i = 0; i < reqElList.size(); i++) {
			String key = reqElList.get(i);
			validateNotEmpty(key);
		}

		/*
		 * Validate the usage of controlled vocab (i.e., places where XSD
		 * specifies a xs:string but the documentation specifies an enumerated
		 * set of allowed values).
		 */
		 validateCMVocab();

		validateRatings();
	}

	/**
	 * @return
	 */
	protected boolean validateCMVocab() {
		boolean allOK = true;
		String mdVersion = "2.4";

		/*
		 * Validate use of Country identifiers....
		 */
		JSONArray iso3691 = cmVocab.optJSONArray("ISO3691");
		LogReference srcRef = LogReference.getRef("CM", mdVersion, "cm_regions");

		allOK = validateVocab(mdNSpace, "DistrTerritory", mdNSpace, "country", iso3691, srcRef, true) && allOK;
		allOK = validateVocab(mdNSpace, "CountryOfOrigin", mdNSpace, "country", iso3691, srcRef, true) && allOK;
		// in multiple places
		allOK = validateVocab(mdNSpace, "Region", mdNSpace, "country", iso3691, srcRef, true) && allOK;

		/* Validate language codes */
		JSONArray rfc5646 = cmVocab.optJSONArray("RFC5646");
		srcRef = LogReference.getRef("CM", mdVersion, "cm_lang");
		allOK = validateVocab(mdNSpace, "LocalizedInfo", null, "@language", rfc5646, srcRef, true) && allOK;
		allOK = validateVocab(mdNSpace, "TitleAlternate", null, "@language", rfc5646, srcRef, true) && allOK;
		allOK = validateVocab(mdNSpace, "DisplayName", null, "@language", rfc5646, srcRef, true) && allOK;
		allOK = validateVocab(mdNSpace, "SortName", null, "@language", rfc5646, srcRef, true) && allOK;
		allOK = validateVocab(mdNSpace, "DisplayString", null, "@language", rfc5646, srcRef, true) && allOK;
		allOK = validateVocab(mdmecNSpace, "Basic", mdNSpace, "PrimarySpokenLanguage", rfc5646, srcRef, true) && allOK;
		allOK = validateVocab(mdmecNSpace, "Basic", mdNSpace, "OriginalLanguage", rfc5646, srcRef, true) && allOK;
		allOK = validateVocab(mdmecNSpace, "Basic", mdNSpace, "VersionLanguage", rfc5646, srcRef, true) && allOK;

		return allOK;
	}

	// ########################################################################

	private void validateRatings() {
		XPathExpression<Element> xpExp01 = xpfac.compile(".//md:RatingSet/md:Rating", Filters.element(), null,
				mdmecNSpace, mdNSpace);
		List<Element> ratingElList = xpExp01.evaluate(curRootEl);
		rLoop: for (int i = 0; i < ratingElList.size(); i++) {
			Element ratingEl = ratingElList.get(i);
			String system = ratingEl.getChildTextNormalize("System", mdNSpace);
			RatingSystem rSystem = RatingSystem.factory(system);
			if (rSystem == null) {
				String msg = "Unrecognized Rating System '" + system + "'";
				String explanation = null;
				logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_ERR, ratingEl, msg, explanation, null, logMsgSrcId);
				curFileIsValid = false;
				continue;
			}
			Element valueEl = ratingEl.getChild("Value", mdNSpace);
			String rating = valueEl.getTextNormalize();
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

			/*
			 * Is the RatingSystem in use in the specified country or
			 * countryRegion?
			 * 
			 */
			LogReference srcRef = LogReference.getRef("CM", "2.4", "cm_regions");
			Element regionEl = ratingEl.getChild("Region", mdNSpace);
			String region = null;
			String subRegion = null;
			Element target = regionEl.getChild("country", mdNSpace);
			if (target == null) {
				target = regionEl.getChild("countryRegion", mdNSpace);
				region = target.getText();
				/*
				 * We don't check validity of ISO-3166-2 codes as there are too
				 * many defined but very few used for ratings
				 */
				if (!rSystem.isUsedInSubRegion(region)) {
					String msg = "RatingSystem not used in specified Country SubRegion";
					String explanation = "The " + system
							+ " Rating System has not been officially adopted in SubRegion '" + region + "'";
					logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_WARN, target, msg, explanation, null, logMsgSrcId);
					curFileIsValid = false;
				}
			} else {
				region = target.getText();
				/* Is it valid ISO-3166-1 code? */
				if (!iso3166_1_codes.containsKey(region)) {
					String msg = "Invalid code for country";
					String explanation = "A country should be specified as a ISO 3166-1 Alpha-2 code";
					logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_ERR, target, msg, explanation, srcRef, logMsgSrcId);
					curFileIsValid = false;
				} else if (!rSystem.isUsedInRegion(region)) {
					String msg = "RatingSystem not used in specified Region";
					String explanation = "The " + system + " Rating System has not been officially adopted in Region '"
							+ region + "'";
					logIssue(LogMgmt.TAG_CR, LogMgmt.LEV_WARN, target, msg, explanation, null, logMsgSrcId);
					curFileIsValid = false;
				}
			}
		}
	}

}
