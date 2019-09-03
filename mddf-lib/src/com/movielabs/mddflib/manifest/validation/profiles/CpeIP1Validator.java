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

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultTreeModel;

import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.manifest.validation.CpeValidator;
import com.movielabs.mddflib.manifest.validation.CpeValidator.ExperienceNode;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * Validates conformance of a CPE Manifest to the requirements of CPE
 * Interactivity Profile 1 (IP1) as specified in TR-CPE-IP1 v1.0 (April 15,
 * 2016).
 * 
 * 
 * @see <a href=
 *      "http://www.movielabs.com/cpe/profiles/ip1/CPEProfile_IP1-v1.0.pdf">TR-
 *      CPE-IP1 v1.0</a>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class CpeIP1Validator {

	private static final String LOGMSG_ID = "CPE_IP-1";
	private String logMsgSrcId;
	private LogMgmt loggingMgr;
	private boolean curFileIsValid = true;
	private CpeValidator cpeValidator;
	private XPathFactory xpfac = XPathFactory.instance();

	public CpeIP1Validator(CpeValidator cpeValidator, LogMgmt loggingMgr) {
		this.cpeValidator = cpeValidator;
		this.loggingMgr = loggingMgr;
		logMsgSrcId = LOGMSG_ID;
	}

	/**
	 * @return
	 */
	public List<String> getSupportedProfiles() {
		List<String> pidList = new ArrayList<String>();
		pidList.add("IP-1");
		pidList.add("IP-01");
		return pidList;
	}

	public void validateInfoModel(DefaultTreeModel infoModel) {
		validateStructure(infoModel);
		/*
		 * Now handle any additional constraints not directly related to
		 * Experiences....
		 */
		validateInventory(infoModel);
		validatePresentation(infoModel);
	}

	protected boolean validateStructure(DefaultTreeModel infoModel) {
		ExperienceNode manifestRoot = (ExperienceNode) infoModel.getRoot();
		List<ExperienceNode> kinder = manifestRoot.getChildren();
		if (kinder.size() != 1) {
			curFileIsValid = false;
			String errMsg = "A single InfoModel hierarchy must be specified. Found " + kinder.size();
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, manifestRoot.getExpEl(), errMsg, null, null,
					logMsgSrcId);
			return false;
		}
		ExperienceNode mainExpNode = kinder.get(0);
		validateRoot(mainExpNode);
		/*
		 * Level 2: the main grouping nodes (i.e., “in-movie” or“out-of-movie”).
		 */
		List<ExperienceNode> mainGroupNodes = mainExpNode.getChildren();
		if (mainGroupNodes.size() != 2) {
			curFileIsValid = false;
			String errMsg = "root must have EXACTLY Two ExperienceChild instances";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, mainExpNode.getExpEl(), errMsg, null, null,
					logMsgSrcId);
			return false;
		}
		for (ExperienceNode mainGroupNode : mainGroupNodes) {
			validateMainGroup(mainGroupNode); 
		}
		return curFileIsValid;
	}

	/**
	 * 
	 * Validates 2nd level of the IP-1 Info Model: the main grouping nodes
	 * (i.e., “in-movie” or“out-of-movie”).
	 * 
	 * @param mainGroupNode
	 */
	private void validateMainGroup(ExperienceNode mainGroupNode) {
		/*
		 * top-level grouping (i.e., either in-movie or out-of-movie). Which
		 * branch will be specified via TitleSort. Following code is
		 * bullet-proof given the super class has already validated the metadata
		 * against the CPE Info Model rqmnts.
		 */
		Element basicMDEl = mainGroupNode.getMetadata();
		Element locMDEl = basicMDEl.getChild("LocalizedInfo", cpeValidator.mdNSpace);
		String curBranch = locMDEl.getChildTextNormalize("TitleSort", cpeValidator.mdNSpace);
		switch (curBranch) {
		case "in-movie":
			curBranch = "in-movie";
			validateGroupIn(mainGroupNode);
			break;
		case "out-of-movie":
			curBranch = "out-of-movie";
			validateGroupOut(mainGroupNode);
			break;
		default:
			curFileIsValid = false;
			curBranch = "";
			String errMsg = "Invalid TitleSort; Must be 'in-movie' or 'out-of-movie'";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, locMDEl, errMsg, null, null, logMsgSrcId);
		}
	}

	/**
	 * @param expEl
	 * @param metaDataEl
	 * @return
	 */
	private boolean validateRoot(ExperienceNode mainExpNode) {
		boolean isValid = true;
		/*
		 * An Audiovisual instance must be included, referencing the main title.
		 * Type=‘Main’
		 */
		Element expEl = mainExpNode.getExpEl();
		Element avEl = expEl.getChild("Audiovisual", cpeValidator.manifestNSpace);
		if (avEl == null) {
			isValid = false;
			String errMsg = "An Audiovisual referencing the main title must be included.";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, expEl, errMsg, null, null, logMsgSrcId);
		} else {
			String avType = avEl.getChildTextNormalize("Type", cpeValidator.manifestNSpace);
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
	private boolean validateGroupOut(ExperienceNode groupNode) {
		Element expEl = groupNode.getExpEl();
		String msg = "Validating out-of-movie Experience branch " + groupNode.getCid();
		loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_DEBUG, expEl, msg, null, null, logMsgSrcId);
		List<Element> groupElList = cpeValidator.getSortedChildren(expEl, "ExperienceChild", "ispartof", true);
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
		}
		msg = "out-of-movie Experience has " + groupElList.size() + " groups";
		loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_DEBUG, expEl, msg, null, null, logMsgSrcId);
		/* is one titled 'Featured' ? */
		boolean found = false;
		Element curRootEl = expEl.getDocument().getRootElement();
		for (int i = 0; i < groupElList.size(); i++) {
			if (!found) {
				Element nextChildEl = groupElList.get(i);
				String expXRef = nextChildEl.getChildTextNormalize("ExperienceID", cpeValidator.manifestNSpace);
				XPathExpression<Element> xpExpression = xpfac.compile(
						".//manifest:Experience[@ExperienceID='" + expXRef + "']", Filters.element(), null,
						cpeValidator.manifestNSpace);
				List<Element> elementList = xpExpression.evaluate(curRootEl);
				Element childExpEl = elementList.get(0);
				Element basicMDEl = cpeValidator.getMetadataEl(childExpEl);
				Element locMDEl = basicMDEl.getChild("LocalizedInfo", cpeValidator.mdNSpace);
				String title1 = locMDEl.getChildTextNormalize("TitleSort", cpeValidator.mdNSpace);
				found = title1.equals("Featured");
			}
		}

		if (!found) {
			String errMsg = "Out-of-movie Experience does not specify a FEATURED group";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_WARN, expEl, errMsg, null, null, logMsgSrcId);
		}
		return found;

	}

	/**
	 * @param expEl
	 * @param metaDataEl
	 * @return
	 */
	private boolean validateBonusGroup(ExperienceNode bonusNode) {
		Element expEl = bonusNode.getExpEl();
		String msg = "Validating Bonus Group Experience " + bonusNode.getCid();
		loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_DEBUG, expEl, msg, null, null, logMsgSrcId);
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param groupNode
	 * @return
	 */
	private boolean validateGroupIn(ExperienceNode groupNode) {
		Element expEl = groupNode.getExpEl();
		String msg = "Validating in-movie Experience branch " + groupNode.getCid();
		loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_DEBUG, expEl, msg, null, null, logMsgSrcId);
		List<Element> groupElList = cpeValidator.getSortedChildren(expEl, "ExperienceChild", "ispartof", true);
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
		}
		boolean allOk = true;
		for (ExperienceNode subGroupNode : groupNode.getChildren()) {
			allOk = validateTabGroup(subGroupNode) && allOk;
		}
		return allOk;
	}

	/**
	 * Validates time-oriented (tied to the video timeline) collections of
	 * material that is displayed in conjunction with playback. This is referred
	 * to as ‘in movie’.
	 * <p>
	 * Requirements (Sec 2.2):
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
	 * </p>
	 * 
	 * @param expEl
	 * @param metaDataEl
	 * @return
	 */
	private boolean validateTabGroup(ExperienceNode tabGroupNode) {
		Element expEl = tabGroupNode.getExpEl();
		String msg = "Validating Tab Group " + tabGroupNode.getCid();
		loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_DEBUG, expEl, msg, null, null, logMsgSrcId);
		List<Element> tSeqList = expEl.getChildren("TimedSequenceID", cpeValidator.manifestNSpace);
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
				cpeValidator.manifestNSpace);
		Element curRootEl = expEl.getDocument().getRootElement();
		List<Element> tsList = xpExpression.evaluate(curRootEl);
		if (tsList.size() < 1) {
			String errMsg = "Experience references unknown TimedEvenetSequence";
			loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, expEl, errMsg, null, null, logMsgSrcId);
			return false;
		}
		Element tSeqEl = tsList.get(0);
		// Presentations...
		/*
		 * Slightly different than AppGroup, TextGroup, etc. The problem is
		 * there is (usually) a Presentation as a child of the
		 * TimedEvenetSequence and we want to exclude it and only collect the
		 * Presentation elements that are the child of a TimedEvent.
		 */
		XPathExpression<Element> xpEx1 = xpfac.compile(".//manifest:PresentationID", Filters.element(), null,
				cpeValidator.manifestNSpace);
		List<Element> expList = collectTabContent(tabGroupNode, xpEx1);
		XPathExpression<Element> xpEx2 = xpfac.compile(".//manifest:TimedEvent/manifest:PresentationID",
				Filters.element(), null, cpeValidator.manifestNSpace);
		List<Element> tesList = xpEx2.evaluate(tSeqEl);
		boolean matches = compareIdSets(expList, tesList);

		// AppGroup...
		xpExpression = xpfac.compile(".//manifest:AppGroupID", Filters.element(), null, cpeValidator.manifestNSpace);
		expList = collectTabContent(tabGroupNode, xpExpression);
		tesList = xpExpression.evaluate(tSeqEl);
		matches = (compareIdSets(expList, tesList) && matches);

		// TextGroup...
		xpExpression = xpfac.compile(".//manifest:TextGroupID", Filters.element(), null, cpeValidator.manifestNSpace);
		expList = collectTabContent(tabGroupNode, xpExpression);
		tesList = xpExpression.evaluate(tSeqEl);
		matches = (compareIdSets(expList, tesList) && matches);

		return matches;
	}

	/**
	 * @param tabGroupNode
	 * @param xpExpression
	 * @return
	 */
	private List<Element> collectTabContent(ExperienceNode tabGroupNode, XPathExpression<Element> xpExpression) {
		List<Element> expList = new ArrayList<Element>();
		Element tabGroupEl = tabGroupNode.getExpEl();
		expList.addAll(xpExpression.evaluate(tabGroupEl));
		// Now include content from the children:
		List<ExperienceNode> tabMemberNodes = tabGroupNode.getChildren();
		for (ExperienceNode memberNode : tabMemberNodes) {
			Element memberEl = memberNode.getExpEl();
			expList.addAll(xpExpression.evaluate(memberEl));
		}
		return expList;
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
	private boolean validateInventory(DefaultTreeModel infoModel) {
		ExperienceNode manifestRoot = (ExperienceNode) infoModel.getRoot();
		Element curRootEl = manifestRoot.getExpEl().getDocument().getRootElement();
		boolean isValid = true;
		XPathExpression<Element> xpExpression = xpfac.compile(".//manifest:Inventory/manifest:Video/manifest:Encoding",
				Filters.element(), null, cpeValidator.manifestNSpace);
		List<Element> elList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < elList.size(); i++) {
			Element nextEl = elList.get(i);
			Element alEl = nextEl.getChild("ActualLength", cpeValidator.mdNSpace);
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
	private boolean validatePresentation(DefaultTreeModel infoModel) {
		ExperienceNode manifestRoot = (ExperienceNode) infoModel.getRoot();
		Element curRootEl = manifestRoot.getExpEl().getDocument().getRootElement();
		boolean isValid = true;
		XPathExpression<Element> xpExpression = xpfac.compile(
				".//manifest:Presentation/manifest:Chapters/manifest:Chapter", Filters.element(), null,
				cpeValidator.manifestNSpace);
		List<Element> elList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < elList.size(); i++) {
			Element nextEl = elList.get(i);
			Element dlEl = nextEl.getChild("DisplayLabel", cpeValidator.manifestNSpace);
			if ((dlEl == null) || (dlEl.getTextNormalize().isEmpty())) {
				String errMsg = "Chapter DisplayLabel is not specified";
				loggingMgr.logIssue(LogMgmt.TAG_PROFILE, LogMgmt.LEV_ERR, nextEl, errMsg, null, null, logMsgSrcId);
				isValid = false;
			}
		}
		return isValid;
	}

	public void setLogger(LogMgmt logMgr) {
		this.loggingMgr = logMgr;

	}

}
