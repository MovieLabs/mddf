/**
 * Created Jun 18, 2016 
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
package com.movielabs.mddflib.manifest.validation.profiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.manifest.validation.ManifestValidator;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.XmlIngester;

import net.sf.json.JSONObject;

/**
 * Validates conformance of a Manifest to the requirements of the Media Manifest
 * Delivery Core (MMC) as specified in TR-META-MMC v1.0 (Jan 4, 2016).
 * 
 *
 * @see <a href= "http://www.movielabs.com/md/mmc/">Media Manifest Core (MMC)
 *      Overview</a>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class MMCoreValidator extends ManifestValidator implements ProfileValidator {

	private static final String LOGMSG_ID = "META_MMC";

	enum AVStructure {
		ENGLISH_ONLY, MULTI_DUB, MULTI_NO_DUB, MULTI_SUBTITLES, MULTI_TRAILERS
	}

	private String curBranch;
	private String curProfileId;
	private String curUseCase;
	private ArrayList<String> pidList = null;
	private Profiler profiler;
	/**
	 * MMC Specification version supported by this class. Manifest MUST specify
	 * <tt>MediaManifest/Compatibility/SpecVersion</tt> that matches.
	 */
	public static final String XSD_VERSION = "1.5";

	public MMCoreValidator(LogMgmt loggingMgr) {
		super(true, loggingMgr);
		this.validateC = true;
		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_PROFILE; // or LogMgmt.TAG_MMC ???
		profiler = new Profiler(loggingMgr, logMsgSrcId, "profiles_mmc_v2.0");
	}

	/**
	 * @return
	 */
	public List<String> getSupportedProfiles() {
		if (pidList == null) {
			pidList = new ArrayList<String>();
			pidList.add("MMC-1");
		}
		return pidList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddf.preProcess.manifest.Validator#getSupporteUseCases(
	 * java. lang.String)
	 */
	@Override
	public List<String> getSupporteUseCases(String profile) {
		// make sure stuff is initialized..
		getSupportedProfiles();
		List<String> pucList = new ArrayList<String>();
		switch (profile) {
		case "MMC-1":
			// pucList.add("Simple");
			// pucList.add("Movie-with-Extras");
			// pucList.add("Episode");
			// pucList.add("Series");
			// pucList.add("Multi-Lang (no dub cards)");
			// pucList.add("Multi-Lang (forced subtitles)");
			// pucList.add("Multi-Lang (mult trailers)");
			// pucList.add("Multi-Lang (dub cards)");
			// pucList.add("Multi-lang (ratings pre-roll, dub card post-roll)");
			// pucList.add("Pre-Roll");
			// pucList.add("Pre-Order");
			break;
		}
		return pucList;
	}

	public boolean process(MddfTarget target, String profileId, List<String> useCases)
			throws JDOMException, IOException {
		super.process(target);
		if (curFileIsValid) {
			validateProfileConstraints();
		}
		return curFileIsValid;
	}

	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateProfileConstraints() {
		// TESTING NEW and EXPERIMENTAL CODE....
		List<String> matchedUseCases = profiler.evaluate(curRootEl);

		// ..................................
		Element compEl = curRootEl.getChild("Compatibility", manifestNSpace);
		Element profileEl = compEl.getChild("Profile", manifestNSpace);
		String profile = profileEl.getTextNormalize();
		/*
		 * Load JSON that defines various constraints on structure of the XML This is
		 * version-specific but not all MMC versions have their own unique struct file
		 * (e.g., a minor release may be compatible with a previous release).
		 */
		String structVer = null;
		switch (profile) {
		case "MMC-1":
		case "MMC-2":
			structVer = profile.toLowerCase();
			break;
		default:
			// Not supported for the version
			String msg = "Unrecognized MMC profile";
			String details = profile + " is not a recognized MMC Profile";
			LogReference srcRef = LogReference.getRef("MMC", "mmc01");
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, profileEl, msg, details, srcRef, logMsgSrcId);
			curFileIsValid = false;
			return;
		}
		/*
		 * First stage checks using the 'structure validation' mechanism
		 */
		JSONObject structDefs = XmlIngester.getMddfResource("structure_" + structVer);
		if (structDefs == null) {
			// LOG a FATAL problem.
			String msg = "Unable to process; missing structure definitions for MMC Profile " + structVer;
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_PROFILE, msg, curFile, logMsgSrcId);
			return;
		}
		JSONObject rqmtSet = structDefs.getJSONObject("StrucRqmts");
		Iterator<String> keys = rqmtSet.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject rqmtSpec = rqmtSet.getJSONObject(key);
			// NOTE: This block of code requires a 'targetPath' be defined
			if (rqmtSpec.has("targetPath")) {
				loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_PROFILE, "Structure check; key= " + key, curFile,
						logMsgSrcId);
				curFileIsValid = structHelper.validateDocStructure(curRootEl, rqmtSpec) && curFileIsValid;
			}
		}
		// --------------------------------------------------------------------------
		/*
		 * now check the additional constraints identified in MMC Section 2.1.2. This
		 * are currently NOT supportable via the JSON-based 'structure validation'
		 * mechanism
		 */
		LogReference srcRef = LogReference.getRef("MMC", "mmc01");

		/*
		 * Validate Experiences
		 */
		Element expSetEl = curRootEl.getChild("Experiences", manifestNSpace);
		XPathExpression<Element> xpExpression = xpfac.compile("./manifest:Experience", Filters.element(), null,
				manifestNSpace);
		List<Element> expElList = xpExpression.evaluate(expSetEl);
		int rootExpCount = 0;
		for (int i = 0; i < expElList.size(); i++) {
			Element expEl = (Element) expElList.get(i);
			/*
			 * in order to validate that there is a single top-level Experience (i.e. we
			 * have a hierarchical tree-like structure) we keep track of how many
			 * ExperienceIDs are NOT referenced w/in an ExperienceChild.
			 */
			String expId = expEl.getAttributeValue("ExperienceID").trim(); 
			String targetXPath = "./manifest:Experience/manifest:ExperienceChild/manifest:ExperienceID[.='" + expId
					+ "']";
			XPathExpression<Element> xpExpression2 = xpfac.compile(targetXPath, Filters.element(), null, manifestNSpace);
			List<Element> targetEl = xpExpression2.evaluate(expSetEl);
			if (targetEl.isEmpty()) {
				rootExpCount++;
			}
 
		}
		if (rootExpCount > 1) {
			String msg = "Only one root Experience is allowed";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, expSetEl, msg, null, srcRef, logMsgSrcId);
			curFileIsValid = false;
		} else if (rootExpCount < 1) {
			String msg = "Root Experience not found";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, expSetEl, msg, null, srcRef, logMsgSrcId);
			curFileIsValid = false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.manifest.validation.profiles.ProfileValidator#
	 * setLogger(com.movielabs.mddflib.logging.LogMgmt)
	 */
	@Override
	public void setLogger(LogMgmt logMgr) {
		this.loggingMgr = logMgr;

	}
	 
}
