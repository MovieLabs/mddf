/**
 * Copyright (c) 2020 MovieLabs

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
package com.movielabs.mddflib.delivery;

import java.io.IOException;

import org.jdom2.JDOMException;
import org.jdom2.Namespace;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.logging.IssueLogger;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.CMValidator;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.SchemaWrapper;
import com.movielabs.mddflib.util.xml.XsdValidation;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Validates files based on the <b>Asset Order and Delivery</b> (AOD)
 * specification. There are three types of file we may be dealing with:
 * 
 * <ol>
 * <li>AssetAvailability</li>
 * <li>AssetOrder</li>
 * <li>ProductStatus</li>
 * </ol>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class AodValidator extends CMValidator implements IssueLogger {

	public static final String LOGMSG_ID = "AODValidator";
	private String deliverySchemaVer;
	private String aod_type;

	/**
	 * @param validateC
	 * @param loggingMgr
	 */
	public AodValidator(boolean validateC, LogMgmt loggingMgr) {
		super(validateC, loggingMgr);
		this.validateC = validateC;
		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_OFFER;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddflib.util.CMValidator#process(com.movielabs.mddflib.util.xml
	 * .MddfTarget)
	 */
	@Override
	public boolean process(MddfTarget target) throws IOException, JDOMException {
		curTarget = target;
		curFile = target.getSrcFile();
		curLoggingFolder = loggingMgr.pushFileContext(target);

		String msg = "Begining validation of AssetOrder...";
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_AOD, msg, curTarget, logMsgSrcId);

		deliverySchemaVer = identifyXsdVersion(target);
		loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag,
				"Validating using Delivery Schema Version " + deliverySchemaVer, curTarget, logMsgSrcId);

		FILE_FMT deliveryFileFmt = MddfContext.identifyMddfFormat("delivery", deliverySchemaVer);
		if (deliveryFileFmt == null) {
			throw new IllegalArgumentException("Unsupported Delivery Schema version " + deliverySchemaVer);
		}
		setMddfVersions(deliveryFileFmt);
		
		
		rootNS = deliveryNSpace;
		curRootEl = null;
		curFileName = curFile.getName();
		curFileIsValid = true;
//		this.pedigreeMap = pedigreeMap;

		validateXml(target);
		// ==========================

		if (!curFileIsValid) {
			msg = "Schema validation check FAILED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AOD, msg, curTarget, logMsgSrcId);
		} else {
			curRootEl = target.getXmlDoc().getRootElement();
			msg = "Schema validation check PASSED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AOD, msg, curTarget, logMsgSrcId);
			/**
			 * There are three types of AOD content we may be dealing with:
			 * 
			 * <pre>
			 *  1)AssetAvailability
			 *  2) AssetOrder
			 *  3) ProductStatus
			 * </pre>
			 */
			aod_type = curRootEl.getName();
			switch (aod_type) {
			case "AssetOrder":
			case "AssetAvailability":
			case "ProductStatus":
				loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AOD, "AOD file will be processed as a " + aod_type,
						curTarget, logMsgSrcId);
				break;
			default:
				loggingMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_AOD, "Unsupported content type" + aod_type, curTarget,
						logMsgSrcId);
				return false;
			}

			if (validateC) {
				initializeIdChecks();
				validateConstraints();
			}
		}
		// clean up and go home
//		this.pedigreeMap = null;
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
		String xsdFile = XsdValidation.defaultRsrcLoc + "delivery-v" + deliverySchemaVer + ".xsd";
		curFileIsValid = xsdHelper.validateXml(target, xsdFile, logMsgSrcId);
		return curFileIsValid;
	}

	protected void validateConstraints() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.LEV_INFO, "Validating constraints", curTarget, LOGMSG_ID);
		super.validateConstraints();

		SchemaWrapper aodSchema = SchemaWrapper.factory("delivery-v" + deliverySchemaVer);
		validateNotEmpty(aodSchema);

		// Now do anything defined in AOD spec..
		validateDeliveryVocab();

		validateUsage();
	}

	/**
	 * 
	 */
	private void validateDeliveryVocab() {
		Namespace primaryNS = rootNS;
		String doc = "AOD";
		String vocabVer = deliverySchemaVer;
		String vocab_file_id = "aod";
		JSONObject vocab = (JSONObject) getVocabResource(vocab_file_id, vocabVer);
		if (vocab == null) {
			String msg = "Unable to validate controlled vocab: missing resource file vocab_" + vocab_file_id + "_v"
					+ vocabVer;
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_OFFER, msg, curTarget, logMsgSrcId);
			curFileIsValid = false;
			return;
		}

		JSONArray allowed;
		LogReference docRef;
		switch (aod_type) {
		case "AssetAvailability":
			allowed = vocab.optJSONArray("AssetAvailability/StatusCode");
			docRef = LogReference.getRef(doc, "AOD02");
			validateVocab(primaryNS, "AssetDisposition", primaryNS, "StatusCode", allowed, docRef, true, true);
			break;
		case "AssetOrder":
			allowed = vocab.optJSONArray("AssetOrder/RequestCode");
			docRef = LogReference.getRef(doc, "AOD03");
			validateVocab(primaryNS, "AssetDisposition", primaryNS, "RequestCode", allowed, docRef, true, true);
			break;
		case "ProductStatus":
			allowed = vocab.optJSONArray("ProductStatus/Category");
			docRef = LogReference.getRef(doc, "AOD04");
			validateVocab(primaryNS, "ObjectStatus", primaryNS, "Category", allowed, docRef, true, true);

			allowed = vocab.optJSONArray("Progress@component");
			docRef = LogReference.getRef(doc, "AOD05");
			validateVocab(primaryNS, "Progress", null, "@component", allowed, docRef, true, true);

			allowed = vocab.optJSONArray("FullOrPartialQC");
			docRef = LogReference.getRef(doc, "AOD06");
			validateVocab(primaryNS, "ErrorDescription", primaryNS, "FullOrPartialQC", allowed, docRef, true, true);

			if(deliverySchemaVer.equals("1.0")) {
				break;
			}

			allowed = vocab.optJSONArray("ErrorDescription/Severity");
			docRef = LogReference.getRef(doc, "AOD06");
			validateVocab(primaryNS, "ErrorDescription", primaryNS, "Severity", allowed, docRef, true, true);
			//TODO: validate the QC ErrorCategory and ErrorTerm in accordance with QC Vocabulary
			break;
		}
	}
	
	protected void validateUsage() {
		super.validateUsage();
		
		//TODO: see Sec 2.1.1 in TR-META-AOD
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
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

}
