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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.jdom2.JDOMException;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.CMValidator;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.SchemaWrapper;
import com.movielabs.mddflib.util.xml.XsdValidation;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.movielabs.mddflib.util.xml.XmlIngester;

public class MecValidator extends CMValidator {

	public static final String LOGMSG_ID = "MecValidator";

	protected static HashMap<String, String> mec_id2typeMap;
	static {
		mec_id2typeMap = new HashMap<String, String>();
		mec_id2typeMap.put("ContentID", "cid");
		mec_id2typeMap.put("ParentContentID", "cid");
	}

	/**
	 * @param validateC
	 */
	public MecValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;

		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_MEC;

		id2typeMap = mec_id2typeMap;
	}

	public boolean process(MddfTarget target) throws IOException, JDOMException {
		curFile = target.getSrcFile();
		loggingMgr.pushFileContext(target);
		String schemaVer = identifyXsdVersion(target);
		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Using Schema Version " + schemaVer, target, logMsgSrcId);
		setMdMecVersion(schemaVer);
		rootNS = mdmecNSpace;

		curTarget = target;
		curRootEl = null;
		curFileName = curFile.getName();
		curFileIsValid = true;

		validateXml(target);
		// }
		if (!curFileIsValid) {
			String msg = "Schema validation check FAILED";
			loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, msg, curTarget, logMsgSrcId);
			// return false;
		} else {
			curRootEl = target.getXmlDoc().getRootElement();
			String msg = "Schema validation check PASSED";
			loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, msg, curTarget, logMsgSrcId);
			if (validateC) {
				initializeIdChecks();
				validateConstraints();
			}
		}
		// clean up and go home
		loggingMgr.popFileContext(target);
		return curFileIsValid;
	}

	/**
	 * Validate everything that is fully specified via the XSD.
	 * 
	 * @param target
	 */
	protected boolean validateXml(MddfTarget target) {
		String xsdFile = XsdValidation.defaultRsrcLoc + "mdmec-v" + MDMEC_VER + ".xsd";
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		curFileIsValid = xsdHelper.validateXml(target, xsdFile, logMsgSrcId);
		return curFileIsValid;
	}

	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateConstraints() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Validating constraints", curTarget, LOGMSG_ID);
		super.validateConstraints();

		SchemaWrapper mecSchema = SchemaWrapper.factory("mdmec-v" + MDMEC_VER);
		validateNotEmpty(mecSchema);

		/*
		 * Validate the usage of controlled vocab (i.e., places where XSD specifies a
		 * xs:string but the documentation specifies an enumerated set of allowed
		 * values).
		 */
		validateMecVocab();
		validateCMVocab();
		validateUsage();
	}

	/**
	 * 
	 */
	private void validateCMVocab() {
		validateBasicMetadata();
	}

	/**
	 * 
	 */
	protected void validateMecVocab() {
		/*
		 * There is no MEC-specific terminology so this is a pass-thru method.
		 */

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.util.CMValidator#validateUsage()
	 */
	protected void validateUsage() {

		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Validating constraints of MEC v" + MDMEC_VER, curTarget,
				LOGMSG_ID);

		super.validateUsage();
		/*
		 * Load JSON that defines various constraints on structure of the XML This is
		 * version-specific but not all schema versions have their own unique struct
		 * file (e.g., a minor release may be compatible with a previous release).
		 */
		String structVer = null;
		switch (MDMEC_VER) {
		case "2.4":
		case "2.5":
			/*
			 * v2.4 and 2.5 differ only in which version of Common Metadata is referenced.
			 */
			structVer = "2.4";
			break;
		case "2.6":
		case "2.7":
		case "2.7.1":
			/*
			 * v2.6, 2.7 differ only in which version of Common Metadata is referenced.
			 * V2.7.1 removes the enumeration for DisplayIndicators which does not impact
			 * structural constraints.
			 */
			structVer = "2.6";
			break;
		case "2.8": 
		case "2.9": 
			structVer = "2.8";
			break;
		default:
			// Not supported for the version
			String msg = "Unable to process; missing structure definitions for MEC v" + MDMEC_VER;
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_MEC, msg, curTarget, logMsgSrcId);
			return;
		}

		JSONObject structDefs = XmlIngester.getMddfResource("structure_mec", structVer);
		if (structDefs == null) {
			// LOG a FATAL problem.
			String msg = "Unable to process; missing structure definitions for MEC v" + MDMEC_VER;
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_MEC, msg, curTarget, logMsgSrcId);
			return;
		}

		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag,
				"Validating constraints with MEC structure-defs_v" + structVer, curTarget, LOGMSG_ID);

		JSONObject rqmtSet = structDefs.getJSONObject("StrucRqmts");
		Iterator<String> keys = rqmtSet.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject rqmtSpec = rqmtSet.getJSONObject(key);
			// NOTE: This block of code requires a 'targetPath' be defined
			if (rqmtSpec.has("targetPath")) {
				loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_MEC, "Structure check; key= " + key, curTarget,
						logMsgSrcId);
				curFileIsValid = structHelper.validateDocStructure(curRootEl, rqmtSpec, curTarget, null)
						&& curFileIsValid;
			}
		}

		return;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddflib.util.CMValidator#setParent(com.movielabs.mddflib.util.
	 * CMValidator)
	 */
	@Override
	public void setParent(CMValidator parent) {
		this.parent = parent;
	}

}
