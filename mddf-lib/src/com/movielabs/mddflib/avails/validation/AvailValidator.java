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
		allOK = allOK
				&& validateVocab(primaryNS, "Transaction", primaryNS, "ExperienceCondition", allowed, srcRef, true)
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
