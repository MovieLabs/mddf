/**
 * Created May 25, 2016 
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.manifest.validation.ModelValidator;

/**
 * Validates conformance of a Manifest to the requirements of CPE Interactivity
 * Profile 1 (IP1) as specified in TR-CPE-IP1 v1.0 (April 15, 2016).
 * 
 * @see <a href=
 *      "http://www.movielabs.com/cpe/profiles/ip1/CPEProfile_IP1-v1.0.pdf">TR-
 *      CPE-IP1 v1.0</a>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class CpeIP1Validator extends ModelValidator implements ProfileValidator {

	private static final String LOGMSG_ID = "CPE_IP-1";
	private String curBranch;

	public CpeIP1Validator(LogMgmt loggingMgr) {
		super(loggingMgr);
		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_PROFILE;
	}

	/**
	 * @return
	 */
	public List<String> getSupportedProfiles() {
		List<String> pidList = new ArrayList<String>();
		pidList.add("IP-01");
		return pidList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddf.preProcess.manifest.Validator#getSupporteUseCases(
	 * java. lang.String)
	 */
	@Override
	public List<String> getSupporteUseCases(String profile) {
		List<String> pucList = new ArrayList<String>();
		// No use-cases at this time
		return pucList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddf.preProcess.manifest.Validator#process(java.io.File,
	 * java.lang.String, java.util.List)
	 */ 
	public boolean process(Element docRootEl, File xmlManifestFile, String profileId, List<String> useCases)
			throws JDOMException, IOException {
		return super.process(docRootEl, xmlManifestFile, profileId);
	}

	protected void validateInfoModel01(Element mainExpEl) {
		curBranch = "";
		super.validateInfoModel01(mainExpEl);
		/*
		 * Now handle any additional constraints not directly related to
		 * Experiences....
		 */
		validateInventory();
		validatePresentation();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddf.preProcess.manifest.ModelValidator#addToHierarchy(org.
	 * jdom2.Element, int, java.util.HashSet)
	 */
	protected void addToHierarchy(Element nextExpEl, int curTreeLevel, HashSet<String> visited) {
		List<Element> allChildList = nextExpEl.getChildren("ExperienceChild", manifestNSpace);
		int childCnt = allChildList.size();
		switch (curTreeLevel) {
		case 0:
			if (childCnt != 2) {
				curFileIsValid = false;
				String errMsg = "root must have EXACTLY Two ExperienceChild instances";
				loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, nextExpEl, errMsg, null, null, logMsgSrcId);
			} else {
				super.addToHierarchy(nextExpEl, curTreeLevel, visited);
			}
			break;
		case 1:
			// top-level grouping (i.e., either in-movie or out-of-movie)
			super.addToHierarchy(nextExpEl, curTreeLevel, visited);
			break;
		case 2:
			/*
			 * Intermediate node representing bonus group or tab group depending
			 * on branch (i.e., in-movie or out-of-movie)
			 */
			if (childCnt == 0) {
				curFileIsValid = false;
				String errMsg = "Empty Group or Tab: 1 or more ChildExperiences should be added.";
				loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, nextExpEl, errMsg, null, null, logMsgSrcId);
			} else {
				super.addToHierarchy(nextExpEl, curTreeLevel, visited);
			}
			break;
		case 3:
			/*
			 * Must be a Leaf node representing a bonus or tab depending on
			 * branch (i.e., in-movie or out-of-movie)
			 */
			if (childCnt != 0) {
				curFileIsValid = false;
				String errMsg = "Child Experiences not allowed: Max # of Experience levels exceeded";
				loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, nextExpEl, errMsg, null, null, logMsgSrcId);
			} else {
				super.addToHierarchy(nextExpEl, curTreeLevel, visited);
			}
			break;
		default:
			// curFileIsValid = false; ????
			String errMsg = "Maximum level of nesting exceeded (curLevel=" + curTreeLevel + ")";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, nextExpEl, errMsg, null, null, logMsgSrcId);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddf.preProcess.manifest.ModelValidator#
	 * validateModelMember( int,
	 * com.movielabs.mddf.preProcess.manifest.ModelValidator.NODE_TYPE,
	 * org.jdom2.Element, org.jdom2.Element)
	 */
	protected boolean validateModelMember(int curTreeLevel, NODE_TYPE type, Element expEl, Element metaDataEl) {
		boolean isValid = super.validateModelMember(curTreeLevel, type, expEl, metaDataEl);
		if (isValid) {
			// additional checks specific to this Profile...
			switch (curTreeLevel) {
			case 0:
				isValid = validateRoot(expEl, metaDataEl);
				break;
			case 1:
				/*
				 * top-level grouping (i.e., either in-movie or out-of-movie).
				 * Which branch will be specified via TitleSort. Following code
				 * is bullet-proof given the super class has already validated
				 * the metadata against the CPE Info Model rqmnts.
				 */
				Element basicMDEl = metaDataEl.getChild("BasicMetadata", manifestNSpace);
				Element locMDEl = basicMDEl.getChild("LocalizedInfo", mdNSpace);
				String title1 = locMDEl.getChildTextNormalize("TitleSort", mdNSpace);
				String title2 = locMDEl.getChildTextNormalize("TitleDisplayUnlimited", mdNSpace);
				switch (title1) {
				case "in-movie":
					curBranch = "in-movie";
					isValid = validateGroupIn(expEl, metaDataEl);
					break;
				case "out-of-movie":
					curBranch = "out-of-movie";
					isValid = validateGroupOut(expEl, metaDataEl);
					break;
				default:
					isValid = false;
					curBranch = "";
					String errMsg = "Invalid TitleSort; Must be 'in-movie' or 'out-of-movie'";
					loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, locMDEl, errMsg, null, null, logMsgSrcId);
				}
				if (!title1.equals(title2)) {
					isValid = false;
					curBranch = "";
					String errMsg = "Invalid LocalizedInfo; TitleSort must be same as TitleDisplayUnlimited";
					loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, locMDEl, errMsg, null, null, logMsgSrcId);
				}
				break;
			case 2:
				/*
				 * bonus or tab depending on branch (i.e., in-movie or
				 * out-of-movie)
				 */
				switch (curBranch) {
				case "in-movie":
					isValid = validateTab(expEl, metaDataEl);
					break;
				case "out-of-movie":
					isValid = validateBonus(expEl, metaDataEl);
					break;
				default:
				}
				break;
			case 3:
				break;
			default:
			}
		}
		curFileIsValid = curFileIsValid && isValid;
		return isValid;
	}

	/**
	 * @param expEl
	 * @param metaDataEl
	 * @return
	 */
	protected boolean validateRoot(Element expEl, Element metaDataEl) {
		boolean isValid = true;
		/*
		 * An Audiovisual instance must be included, referencing the main title.
		 * Type=‘Main’
		 */
		Element avEl = expEl.getChild("Audiovisual", manifestNSpace);
		if (avEl == null) {
			isValid = false;
			String errMsg = "An Audiovisual referencing the main title must be included.";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, expEl, errMsg, null, null, logMsgSrcId);
		} else {
			String avType = avEl.getChildTextNormalize("Type", manifestNSpace);
			if (!avType.equals("Main")) {
				isValid = false;
				String errMsg = "Root Audiovisual instance must reference the main title";
				loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, expEl, errMsg, null, null, logMsgSrcId);
			}
		}
		return isValid;
	}

	/**
	 * Validate out-of-movie Experience branch.
	 * 
	 * @param expEl
	 * @param metaDataEl
	 * @return
	 */
	protected boolean validateGroupOut(Element expEl, Element metaDataEl) {
		String msg = "Validating out-of-movie Experience branch";
		loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_DEBUG, expEl, msg, null, null, logMsgSrcId);
		List<Element> groupElList = getSortedChildren(expEl, "ExperienceChild", "ispartof", true);
		if (groupElList == null) {
			/*
			 * There was a problem with SequenceInfo. Specific errors will have
			 * been logged by getSortedChildren()
			 */
			return false;
		} else if (groupElList.isEmpty()) {
			String errMsg = "No groups defined for out-of-movie Experience";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_WARN, expEl, errMsg, null, null, logMsgSrcId);
			return true;
		} else {
			msg = "out-of-movie Experience has " + groupElList.size() + " groups";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_DEBUG, expEl, msg, null, null, logMsgSrcId);
			/* is one titled 'Featured' ? */
			boolean found = false;
			for (int i = 0; i < groupElList.size(); i++) {
				if (!found) {
					Element nextChildEl = groupElList.get(i);
					String expXRef = nextChildEl.getChildTextNormalize("ExperienceID", manifestNSpace);
					XPathExpression<Element> xpExpression = xpfac.compile(
							".//manifest:Experience[@ExperienceID='" + expXRef + "']", Filters.element(), null,
							manifestNSpace);
					List<Element> elementList = xpExpression.evaluate(curRootEl);
					Element childExpEl = elementList.get(0);
					Element childMdEl = getMetadataEl(childExpEl);
					Element basicMDEl = childMdEl.getChild("BasicMetadata", manifestNSpace);
					Element locMDEl = basicMDEl.getChild("LocalizedInfo", mdNSpace);
					String title1 = locMDEl.getChildTextNormalize("TitleSort", mdNSpace);
					found = title1.equals("Featured");
				}
			}
			if (!found) {
				String errMsg = "Out-of-movie Experience does not specify a FEATURED group";
				loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_WARN, expEl, errMsg, null, null, logMsgSrcId);
			}
			return true;
		}
	}

	/**
	 * @param expEl
	 * @param metaDataEl
	 * @return
	 */
	protected boolean validateBonus(Element expEl, Element metaDataEl) {
		String msg = "Validating BONUS Experience";
		loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_DEBUG, expEl, msg, null, null, logMsgSrcId);
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param expEl
	 * @param metaDataEl
	 * @return
	 */
	protected boolean validateGroupIn(Element expEl, Element metaDataEl) {
		String msg = "Validating in-movie Experience branch";
		loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_DEBUG, expEl, msg, null, null, logMsgSrcId);
		List<Element> groupElList = getSortedChildren(expEl, "ExperienceChild", "ispartof", true);
		if (groupElList == null) {
			/*
			 * There was a problem with SequenceInfo. Specific errors will have
			 * been logged by getSortedChildren()
			 */
			return false;
		} else if (groupElList.isEmpty()) {
			String errMsg = "No groups defined for in-movie Experience";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, expEl, errMsg, null, null, logMsgSrcId);
			return false;
		} else if (groupElList.size() > 3) {
			msg = "in-movie Experience has " + groupElList.size() + " tabs; Recommendation is no more than 3";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_WARN, expEl, msg, null, null, logMsgSrcId);
			return true;
		} else {
			msg = "in-movie Experience has " + groupElList.size() + " tabs";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_DEBUG, expEl, msg, null, null, logMsgSrcId);
			return true;
		}
	}

	/**
	 * Requirements:
	 * <ul>
	 * <li>Contains a ContentID referencing Basic Metadata in the Inventory that
	 * contains TitleDisplayUnlimited and TitleSort with the name of the tab
	 * (e.g., “Shorts”).</li>
	 * <li>Contains a TimedEventSequence referencing Presentations, Media
	 * Applications, Galleries and Text Groups. Note that these objects will be
	 * displayed in the order corresponding with TimedEvent/StartTimecode in
	 * TimedEventSequence.</li>
	 * <li>Contains Audiovisual, App and Gallery instances that correspond
	 * exactly with the Presentations, Media Applications and Galleries
	 * referenced in the TimedEventSequence
	 * </ul>
	 * 
	 * @param expEl
	 * @param metaDataEl
	 * @return
	 */
	protected boolean validateTab(Element expEl, Element metaDataEl) {
		String msg = "Validating tab Experience";
		loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_DEBUG, expEl, msg, null, null, logMsgSrcId);
		List<Element> tSeqList = expEl.getChildren("TimedSequenceID", manifestNSpace);
		if (tSeqList.size() < 1) {
			String errMsg = "No TimedSequenceID found for Tab Experience";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, expEl, errMsg, null, null, logMsgSrcId);
			return false;
		} else if (tSeqList.size() > 1) {
			String errMsg = "Only 1 TimedSequenceID allowed for Tab Experience";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, expEl, errMsg, null, null, logMsgSrcId);
			return false;
		}
		/*
		 * Make sure Audiovisual, App and Gallery instances in Experience
		 * correspond exactly with the Presentations, Media Applications and
		 * Galleries referenced in the TimedEventSequence. The easiest way to do
		 * that is to compile a single XPathExpression to isolate a type of ID
		 * (e.g. PresentationID) and then apply it twice: first in the context
		 * of the Experience and then in the context of the TimedEventSequence.
		 */
		String tSeqId = tSeqList.get(0).getTextNormalize();
		XPathExpression<Element> xpExpression = xpfac.compile(
				".//manifest:TimedEventSequence[@TimedSequenceID='" + tSeqId + "']", Filters.element(), null,
				manifestNSpace);
		List<Element> tsList = xpExpression.evaluate(curRootEl);
		if (tsList.size() < 1) {
			String errMsg = "Experience references unknown TimedEvenetSequence";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, expEl, errMsg, null, null, logMsgSrcId);
			return false;
		}
		Element tSeqEl = tsList.get(0);
		// Presentations...
		xpExpression = xpfac.compile(".//manifest:PresentationID", Filters.element(), null, manifestNSpace);
		List<Element> expList = xpExpression.evaluate(expEl);
		List<Element> tesList = xpExpression.evaluate(tSeqEl);
		boolean matches = compareIdSets(expList, tesList);

		// AppGroup...
		xpExpression = xpfac.compile(".//manifest:AppGroupID", Filters.element(), null, manifestNSpace);
		expList = xpExpression.evaluate(expEl);
		tesList = xpExpression.evaluate(tSeqEl);
		matches = (matches && compareIdSets(expList, tesList));

		// TextGroup...
		xpExpression = xpfac.compile(".//manifest:TextGroupID", Filters.element(), null, manifestNSpace);
		expList = xpExpression.evaluate(expEl);
		tesList = xpExpression.evaluate(tSeqEl);
		matches = (matches && compareIdSets(expList, tesList));

		return matches;
	}

	/**
	 * Compare the references in a Tabbed Experience to those in the associated
	 * TimedEventSequence.
	 * 
	 * @param expList
	 * @param tesList
	 */
	private boolean compareIdSets(List<Element> expList, List<Element> tesList) {
		boolean isValid = true;
		/*
		 * Extract the actual ID strings from each List of Elements. Maintain
		 * the same order to facilitate trace-back to the XML.
		 */
		List<String> expIdList = new ArrayList<String>();
		for (int i = 0; i < expList.size(); i++) {
			expIdList.add(expList.get(i).getTextNormalize());
		}
		List<String> tesIdList = new ArrayList<String>();
		for (int i = 0; i < tesList.size(); i++) {
			tesIdList.add(tesList.get(i).getTextNormalize());
		}
		if (expIdList.containsAll(tesIdList) && tesIdList.containsAll(expIdList)) {
			// [(A contains B) AND (B contains A)] IFF (A=B)
			return true;
		}
		// find and log any mis-match...
		if (!expIdList.containsAll(tesIdList)) {
			for (int i = 0; i < tesIdList.size(); i++) {
				if (!(expIdList.contains(tesIdList.get(i)))) {
					Element missingEl = tesList.get(i);
					String errMsg = "TimedEventSequence has content not referenced by associated Experience";
					loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, missingEl, errMsg, null, null,
							logMsgSrcId);
					isValid = false;
				}
			}
		}
		if (!tesIdList.containsAll(expIdList)) {
			for (int i = 0; i < expIdList.size(); i++) {
				if (!(tesIdList.contains(expIdList.get(i)))) {
					Element missingEl = expList.get(i);
					String errMsg = "Experience has content not referenced by associated TimedEventSequence";
					loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, missingEl, errMsg, null, null,
							logMsgSrcId);
					isValid = false;
				}
			}
		}
		return isValid;
	}

	/**
	 * 
	 */
	protected boolean validateInventory() {
		boolean isValid = true;
		XPathExpression<Element> xpExpression = xpfac.compile(".//manifest:Inventory/manifest:Video/manifest:Encoding",
				Filters.element(), null, manifestNSpace);
		List<Element> elList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < elList.size(); i++) {
			Element nextEl = elList.get(i);
			Element alEl = nextEl.getChild("ActualLength", mdNSpace);
			if ((alEl == null) || (alEl.getTextNormalize().isEmpty())) {
				String errMsg = "ActualLength of Video/Encoding is not specified";
				loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, nextEl, errMsg, null, null, logMsgSrcId);
				isValid = false;
			}
		}
		return isValid;
	}

	/**
	 * Validate profile-specific requirements for a <tt>Presentation</tt>.
	 * <p>
	 * According to Section 2.3 of <tt>TR-CPE-IP v1.0</tt>:
	 * <ul>
	 * <li>Each “Features” Presentation must include Chapters.</li>
	 * <li>Chapters should not be included in bonus content</li>
	 * </ul>
	 * Both of these statements are incorrect. Thus, the only actual additional
	 * validation test is for the inclusion of a <tt>DisplayLabel</tt> in each
	 * <tt>Chapter</tt>
	 * </p>
	 */
	protected boolean validatePresentation() {
		boolean isValid = true;
		XPathExpression<Element> xpExpression = xpfac.compile(
				".//manifest:Presentation/manifest:Chapters/manifest:Chapter", Filters.element(), null, manifestNSpace);
		List<Element> elList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < elList.size(); i++) {
			Element nextEl = elList.get(i);
			Element dlEl = nextEl.getChild("DisplayLabel", manifestNSpace);
			if ((dlEl == null) || (dlEl.getTextNormalize().isEmpty())) {
				String errMsg = "Chapter DisplayLabel is not specified";
				loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, nextEl, errMsg, null, null, logMsgSrcId);
				isValid = false;
			}
		}
		return isValid;
	}

	/* (non-Javadoc)
	 * @see com.movielabs.mddflib.manifest.validation.profiles.ProfileValidator#setLogger(com.movielabs.mddflib.logging.LogMgmt)
	 */
	@Override
	public void setLogger(LogMgmt logMgr) {
		this.loggingMgr = logMgr;
		
	}
}
