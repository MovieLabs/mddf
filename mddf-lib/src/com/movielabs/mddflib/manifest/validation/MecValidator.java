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
import org.jdom2.JDOMException;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.CMValidator;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.SchemaWrapper;
import com.movielabs.mddflib.util.xml.XsdValidation;
import com.movielabs.mddflib.util.xml.XmlIngester;

public class MecValidator extends CMValidator {

	public static final String LOGMSG_ID = "MecValidator";

	static final String DOC_VER = "2.4";

	static {
		id2typeMap = new HashMap<String, String>();
	}

	/**
	 * @param validateC
	 */
	public MecValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;

		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_MEC;
	}

	public boolean process(MddfTarget target) throws IOException, JDOMException {
		curTarget = target;
		curRootEl = null;
		curFile = target.getSrcFile();
		curFileName = curFile.getName();
		curFileIsValid = true;

		String schemaVer = identifyXsdVersion(target);
		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Using Schema Version " + schemaVer, srcFile, logMsgSrcId);
		setMdMecVersion(schemaVer);
		rootNS = mdmecNSpace;

		validateXml(target);
		// }
		if (!curFileIsValid) {
			String msg = "Schema validation check FAILED";
			loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, msg, curFile, logMsgSrcId);
			// return false;
		} else {
			curRootEl = target.getXmlDoc().getRootElement();
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
	protected boolean validateXml(MddfTarget target) {
		String xsdFile = XsdValidation.defaultRsrcLoc + "mdmec-v" + XmlIngester.MDMEC_VER + ".xsd";
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		curFileIsValid = xsdHelper.validateXml(target, xsdFile, logMsgSrcId);
		return curFileIsValid;
	}


	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateConstraints() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Validating constraints", curFile, LOGMSG_ID);
		super.validateConstraints();

		SchemaWrapper mecSchema = SchemaWrapper.factory("mdmec-v" + XmlIngester.MDMEC_VER);
		validateNotEmpty(mecSchema);

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
	protected void validateCMVocab() { 
		/*
		 * Validate use of Country identifiers....
		 */
		validateRegion(mdNSpace, "DistrTerritory", mdNSpace, "country");
		validateRegion(mdNSpace, "CountryOfOrigin", mdNSpace, "country");
		// in multiple places
		validateRegion(mdNSpace, "Region", mdNSpace, "country");

		/* Validate language codes */
		
		/* First check all usage of the '@language' attribute */
		validateLanguage(manifestNSpace);
		
		// Now check any other usage....
		validateLanguage(mdmecNSpace, "Basic", mdNSpace, "PrimarySpokenLanguage");
		validateLanguage(mdmecNSpace, "Basic", mdNSpace, "OriginalLanguage");
		validateLanguage(mdmecNSpace, "Basic", mdNSpace, "VersionLanguage"); 
	}

}
