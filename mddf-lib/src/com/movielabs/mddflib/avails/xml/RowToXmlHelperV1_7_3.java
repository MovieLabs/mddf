/**
 * Copyright (c) 2018 MovieLabs

 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.movielabs.mddflib.avails.xml;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;

import com.movielabs.mddflib.logging.LogMgmt;

/**
 * Create XML document from a v1.7.3 Excel spreadsheet. The XML generated will
 * be based on v2.3 of the Avails XSD and reflects a "best effort" in that there
 * is no guarantee that it is valid.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class RowToXmlHelperV1_7_3 extends RowToXmlHelperV1_7_2 {

	static final String MISSING = "--FUBAR (missing)";

	/**
	 * @param logger
	 * @param fields
	 */
	RowToXmlHelperV1_7_3(AvailsSheet sheet, Row row, LogMgmt logger) {
		super(sheet, row, logger);
	}

	protected void processTransactionBody(Element transactionEl) {
		String prefix = "AvailTrans/";
		addLicensee(transactionEl); // added v1.7.3

		process(transactionEl, "LicenseType", xb.getAvailsNSpace(), prefix + "LicenseType");
		process(transactionEl, "Description", xb.getAvailsNSpace(), prefix + "Description");
		/*
		 * for v1.7.3, Territory allows CSV list of multiple regions for SVOD
		 */
		addRegion(transactionEl, "Territory", xb.getAvailsNSpace(), prefix + "Territory", ",");
		/* TerritoryExclusion add in v1.7.3 */
		addRegion(transactionEl, "TerritoryExcluded", xb.getAvailsNSpace(), prefix + "TerritoryExclusion", ",");

		// Start or StartCondition
		processCondition(transactionEl, "Start", xb.getAvailsNSpace(), prefix + "Start");
		// End or EndCondition
		processCondition(transactionEl, "End", xb.getAvailsNSpace(), prefix + "End");

		/* WindowDuration is new for v1.7.3 */
		process(transactionEl, "WindowDuration", xb.getAvailsNSpace(), "AvailTrans/WindowDuration");

		processLanguage(transactionEl, "AllowedLanguage", xb.getAvailsNSpace(), prefix + "AllowedLanguages", ",", true);
		processLanguage(transactionEl, "HoldbackLanguage", xb.getAvailsNSpace(), prefix + "HoldbackLanguage", ",",
				true);
		Element[] assetLangEls = processLanguage(transactionEl, "AssetLanguage", xb.getAvailsNSpace(),
				prefix + "AssetLanguage", ",", true);
		processReqLanguages(assetLangEls);

		process(transactionEl, "LicenseRightsDescription", xb.getAvailsNSpace(), prefix + "LicenseRightsDescription");
		process(transactionEl, "FormatProfile", xb.getAvailsNSpace(), prefix + "FormatProfile");
		process(transactionEl, "ContractID", xb.getAvailsNSpace(), prefix + "ContractID");
		process(transactionEl, "ReportingID", xb.getAvailsNSpace(), prefix + "ReportingID");

		addAllTerms(transactionEl);

		process(transactionEl, "OtherInstructions", xb.getAvailsNSpace(), prefix + "OtherInstructions");

	}

	/**
	 * Handles the special case of the <tt>RequiredFulfillmentLanguages</tt>. If
	 * an AssetLanguage is also listed as a RequiredFulfillmentLanguage then
	 * <tt>AssetLanguage@assetProvided</tt> is set to <tt>true</tt>.
	 * 
	 * @param assetLangEls
	 */
	private void processReqLanguages(Element[] assetLangEls) {
		// 1st get the list of required fulfillment language:
		Pedigree pg = getPedigreedData("AvailTrans/RequiredFulfillmentLanguages");
		if (pg == null) {
			return;
		}
		String value = pg.getRawValue();
		String[] valueArray;
		String separator = ",";
		valueArray = value.split(separator);
		Collection<String> reqLanguages = new HashSet<String>(Arrays.asList(valueArray));
		Collection<String> providedLanguages = new HashSet<String>();
		for (int i = 0; i < assetLangEls.length; i++) {
			Element aEl = assetLangEls[i];
			String langCode = aEl.getValue(); 
			Attribute aa = aEl.getAttribute("asset");
			if(aa != null){
				langCode = langCode +":"+aa.getValue();
			}
			if (reqLanguages.contains(langCode)) {
				aEl.setAttribute("assetProvided", "true");
			}
			providedLanguages.add(langCode);
		}
		// Now check for a REQUIRED language that IS NOT included in the set of
		// Asset Languages
		reqLanguages.removeAll(providedLanguages);
		reqLanguages.remove("");  // not the same as null
		// Anything left is probably an error
		if (!reqLanguages.isEmpty()) {
			Cell sourceCell = (Cell) pg.getSource();
			String details = "A RequiredFulfillmentLanguage should also be identified as an AssetLanguage that the content owner intends to provide. ";
			for (String reqLangCode : reqLanguages) {
				String errMsg = "Required fulfillment language '" + reqLangCode + "' is not an AssetLanguage";
				logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_ERR, sourceCell, errMsg, details, null,
						XmlBuilder.moduleId);
			}
		}
	}

	/**
	 * Process language related values that may or may not have an optional
	 * 'asset' suffix.
	 * 
	 * @param transactionEl
	 * @param childName
	 * @param ns
	 * @param cellKey
	 * @param listSep
	 * @param allowAssetSuffix
	 */
	protected Element[] processLanguage(Element transactionEl, String childName, Namespace ns, String cellKey,
			String listSep, boolean allowAssetSuffix) {
		// start with basic processing...
		Element[] elementList = process(transactionEl, childName, ns, cellKey, listSep);
		if (elementList == null) {
			return elementList;
		}
		/*
		 * if adding a suffix to the language code is allowed, we need to do
		 * some extra work. Otherwise return what we already have.
		 */
		if (!allowAssetSuffix) {
			return elementList;
		}
		/*
		 * specific assets can be specified for a given language by adding one
		 * of the following suffixes: "sub" for subtitle only, "dub" for dub
		 * only, and "subdub" for both subs and dubs. All of the following are
		 * therefore valid: "en", "fr-ca:subdub", "es-419:dub". If the colon
		 * delimiter is present, the suffix needs to be removed from the element
		 * value and converted to an attribute.
		 * 
		 */
		for (Element next : elementList) {
			String curValue = next.getText();
			String[] parts = curValue.split(":");
			if (parts.length == 2) {
				next.setText(parts[0]);
				next.setAttribute("asset", parts[1]);
			}
		}
		return elementList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddflib.avails.xml.RowToXmlHelperV1_7#processCondition(org.
	 * jdom2.Element, java.lang.String, org.jdom2.Namespace, java.lang.String)
	 */
	protected Element processCondition(Element parentEl, String childName, Namespace ns, String cellKey) {
		Element condEl = super.processCondition(parentEl, childName, ns, cellKey);
		/*
		 * New for v1.7.3 is "lag". Need to see if a start or end lag was
		 * specified and, if so is it allowed.
		 */
		String lagKey = childName + "Lag";
		String lagCell = "AvailTrans/" + lagKey;
		Pedigree lagData = getPedigreedData(lagCell);
		if (!isSpecified(lagData)) {
			/* nothing specified so just return what we already have */
			return condEl;
		}
		/*
		 * Now we need to verify we have either a valid StartCondition or
		 * EndCondition as 'Lag' is not compatible with the plain vanilla Start
		 * or End.
		 */
		String anchorKey = childName + " Condition";
		if (condEl == null) {
			Cell sourceCell = (Cell) lagData.getSource();
			String errMsg = "Invalid use of '" + lagKey + "'; Missing " + anchorKey;
			String details = lagKey + " is an offest from a " + anchorKey + ". The " + anchorKey
					+ " value, however, was not specified";
			logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_ERR, sourceCell, errMsg, details, null, XmlBuilder.moduleId);
			return condEl;
		}
		// Have a base value but is it the right type?
		if (!condEl.getName().endsWith("Condition")) {
			Cell sourceCell = (Cell) lagData.getSource();
			String errMsg = "Invalid use of '" + lagKey + "'; Base value must be conditional ";
			String details = lagKey + " is an offest from a " + anchorKey + ". The " + childName
					+ " value, however, was specified as an absolute date/time";
			logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_ERR, sourceCell, errMsg, details, null, XmlBuilder.moduleId);
			return condEl;
		}
		// All checks passed so we can add the attribute
		condEl.setAttribute("lag", lagData.getRawValue());
		xb.addToPedigree(condEl.getAttribute("lag"), lagData);
		return condEl;
	}

	/**
	 * @param transactionEl
	 */
	private void addLicensee(Element transactionEl) {
		Pedigree pg = getPedigreedData("Avail/Licensee");
		if (isSpecified(pg)) {
			Element licenseeEl = new Element("Licensee", xb.getAvailsNSpace());
			Element nameEl = process(licenseeEl, "DisplayName", xb.getMdNSpace(), "Avail/Licensee");
			transactionEl.addContent(licenseeEl);
		}

	}

	protected void addAllTerms(Element transactionEl) {
		super.addAllTerms(transactionEl);

		/* Now add Terms that are new for v1.7.3 */
		String prefix = "AvailTrans/";
		addTerm(transactionEl, prefix + "Download", "Download", "Text");
		addTerm(transactionEl, prefix + "Exclusive", "Exclusive", "Boolean");
		addTerm(transactionEl, prefix + "ExclusiveAttributes", "ExclusiveAttributes", "Text");
		addTerm(transactionEl, prefix + "BrandingRights", "BrandingRights", "Boolean");
		addTerm(transactionEl, prefix + "BrandingRightsAttributes", "BrandingRightsAttributes", "Text");
		addTerm(transactionEl, prefix + "TitleStatus", "TitleStatus", "Text");
	}

}
