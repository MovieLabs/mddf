/**
 * Created May 27, 2016
 * Copyright Motion Picture Laboratories, Inc. (2016)
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * 
 * Validates a Manifest file as conforming to a specified structure (a.k.a.
 * 'model') as defined by the CPE-Manifest specification. The CPE-Manifest
 * defines a set of structural requirements that, while <b>more</b> restrictive
 * than then the restrictions specified by the Common Media Manifest (CMM)
 * specification, is <b>less</b> restrictive than those required to conform to a
 * specific CPE Profile. Validation of profile-specific requirements may be
 * addressed by extending this class and overriding various methods.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class ModelValidator extends XmlIngester {

	private static final String LOGMSG_ID = "ModelValidator";
	protected Element curRootEl;
	protected boolean curFileIsValid;

	public static enum NODE_TYPE {
		ROOT, INTERMEDIATE, LEAF
	};

	/**
	 * @param validate
	 * @param invGen
	 * 
	 */
	public ModelValidator(LogMgmt loggingMgr) {
		super(loggingMgr);
		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_MODEL;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddf.preProcess.manifest.Validator#process(java.io.File)
	 */
	@SuppressWarnings("deprecation")
	public boolean process(Element docRootEl, File xmlManifestFile, String profileId) throws JDOMException, IOException {
		curFile = xmlManifestFile;
		curFileName = xmlManifestFile.getName();
		curFileIsValid = true;
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_MODEL, "Validating Model", curFile, LOGMSG_ID);

		curRootEl = docRootEl;  

		/*
		 * Get all ALID and identify for each the 'root' (i.e., main) Experience
		 * element
		 */
		List<String> primaryExpSet = extractAlidMap(curRootEl);
		if (primaryExpSet == null || (primaryExpSet.isEmpty())) {
			String msg = "Terminating Model validation due to empty or missing ALIDExperienceMaps";
			loggingMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_MODEL, msg, curFile, LOGMSG_ID);
			return false;
		}
		for (int i = 0; i < primaryExpSet.size(); i++) {
			String nextExpId = primaryExpSet.get(i);
			XPathExpression<Element> xpExpression = xpfac.compile(
					".//manifest:Experience[@ExperienceID='" + nextExpId + "']", Filters.element(), null,
					manifestNSpace);
			List<Element> elementList = xpExpression.evaluate(curRootEl);
			if (elementList.isEmpty()) {
				String errMsg = "Unable to locate ALID's root experience; expId = " + nextExpId;
				loggingMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_MODEL, errMsg, curFile, LOGMSG_ID);
				curFileIsValid = false;
			} else {
				/*
				 * In future there may be more than 1 structural model supported
				 * but for now.....
				 */
				validateInfoModel01(elementList.get(0));
			}

		}

		return curFileIsValid;

	}

	/**
	 * @param mainExpEl
	 *            root Experience for a tree of Experiences.
	 * 
	 */
	protected void validateInfoModel01(Element mainExpEl) {
		// Validate tree-like hierarchical structure.
		HashSet<String> visited = new HashSet<String>();
		int curTreeLevel = 0;
		addToHierarchy(mainExpEl, curTreeLevel, visited);
	}

	/**
	 * @param mainExpEl
	 * @param curTreeLevel
	 * @param visited
	 * @return
	 */
	protected void addToHierarchy(Element nextExpEl, int curTreeLevel, HashSet<String> visited) {
		String expId = nextExpEl.getAttributeValue("ExperienceID");
		if (visited.add(expId)) {
			// New addition to tree
			List<Element> allChildList = nextExpEl.getChildren("ExperienceChild", manifestNSpace);
			// Validate metadata appropriate to position in tree.
			Element metaDataEl = getMetadataEl(nextExpEl);
			NODE_TYPE curType;
			if (allChildList.isEmpty()) {
				// leaf node
				curType = NODE_TYPE.LEAF;
			} else if (curTreeLevel > 0) {
				// intermediate grouping node
				curType = NODE_TYPE.INTERMEDIATE;
			} else {
				// root node
				curType = NODE_TYPE.ROOT;
			}
			validateModelMember(curTreeLevel, curType, nextExpEl, metaDataEl);
			// Recursively descend tree
			for (int i = 0; i < allChildList.size(); i++) {
				Element nextChildEl = allChildList.get(i);
				String expXRef = nextChildEl.getChildTextNormalize("ExperienceID", manifestNSpace);
				XPathExpression<Element> xpExpression = xpfac.compile(".//manifest:Experience[@ExperienceID='" + expXRef + "']",
						Filters.element(), null, manifestNSpace);
				List<Element> elementList = xpExpression.evaluate(curRootEl);
				if (elementList.isEmpty()) {
					String errMsg = "Unable to locate child experience; expId = " + expXRef;
					loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_ERR, nextChildEl, errMsg, null, null, logMsgSrcId);
					curFileIsValid = false;
				} else {
					Element childExpEl = elementList.get(0);
					addToHierarchy(childExpEl, curTreeLevel + 1, visited);
				}
			}
		} else {
			// Looped to previously added Experience
			String errMsg = "Child experience is already part of Experience hierarchy; expId = " + expId;
			loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_ERR, nextExpEl, errMsg, null, null, logMsgSrcId);
			curFileIsValid = false;
		}
	}

	/**
	 * Validate a specific Experience in the context of it's location within the
	 * overall model. Profile-specific requirements should be addressed by
	 * extending this class and overriding this method.
	 * 
	 * @param curTreeLevel
	 * @param type
	 * @param expEl
	 * @param metaDataEl
	 */
	protected boolean validateModelMember(int curTreeLevel, NODE_TYPE type, Element expEl, Element metaDataEl) {
		String expLabel = "--unk--"; // used for debug log msgs
		Element basicMDEl = metaDataEl.getChild("BasicMetadata", manifestNSpace);
		if (basicMDEl == null) {
			String errMsg = "BasicMetadata with at least one instance of LocalizedInfo is required.";
			loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_ERR, metaDataEl, errMsg, null, null, logMsgSrcId);
			curFileIsValid = false;
			return false;
		}
		boolean memberIsValid = true;
		List<Element> localMDataList = basicMDEl.getChildren("LocalizedInfo", mdNSpace);
		switch (curTreeLevel) {
		case 0:
			expLabel = "ROOT";
			break;
		case 1:
			/*
			 * The following rules apply to top-level grouping nodes : [a]There
			 * is only one instance of LocalizedInfo [b] That instance has
			 * TitleSort set to the name of the grouping category (e.g.,
			 * “in-movie” or “out-of-movie”). [c] LocalizedInfo@language may
			 * contain any language code, and it is ignored. [d] No other
			 * metadata is present unless it is a required element or attribute
			 * in the schema.
			 * 
			 */
			int liCnt = localMDataList.size();
			if (localMDataList.size() == 1) {
				Element locMDEl = localMDataList.get(0);
				String title = locMDEl.getChildTextNormalize("TitleSort", mdNSpace);
				if (title.isEmpty()) {
					memberIsValid = false;
					String errMsg = "TitleSort must specify the name of the grouping category";
					loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_ERR, basicMDEl, errMsg, null, null, logMsgSrcId);
				} else {
					expLabel = title;
				}
			} else {
				memberIsValid = false;
				String errMsg = "Top-level grouping nodes must have exactly one instance of LocalizedInfo, found "
						+ liCnt;
				loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_ERR, basicMDEl, errMsg, null, null, logMsgSrcId);
			}
			break;
		default:
			/**
			 * <ul>
			 * <li>An instance of LocalizedInfo should be included for each
			 * language supported for the title.</li>
			 * <li>LocalizedInfo/TitleDisplayUnlimited and TitleSort contains
			 * the user-visible name for that node. Note that even when an image
			 * is the intended UI element, text should still be provided for
			 * accessibility (text to speech).</li>
			 * <li>LocalizedInfo/ArtReference includes images associated with
			 * the node. Implementers note: Implementations should accept
			 * ImageID, PictureID, PictureGroupID and URL.</li> *
			 * </ul>
			 */
			for (int i = 0; i < localMDataList.size(); i++) {
				Element locMDEl = localMDataList.get(i);
				String title = locMDEl.getChildTextNormalize("TitleSort", mdNSpace);
				if (title.isEmpty()) {
					memberIsValid = false;
					String errMsg = "TitleSort is empty";
					loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_ERR, locMDEl, errMsg, null, null, logMsgSrcId);
				} else {

					expLabel = title;
				}
				String titleDU = locMDEl.getChildTextNormalize("TitleDisplayUnlimited", mdNSpace);
				if ((titleDU == null) || (titleDU.isEmpty())) {
					memberIsValid = false;
					String errMsg = "TitleDisplayUnlimited is empty or missing";
					loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_ERR, locMDEl, errMsg, null, null, logMsgSrcId);
				}
			}
			break;
		}
		String debugMsg = "Validating Experience '" + expLabel + "' at Level=" + curTreeLevel;
		loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_DEBUG, expEl, debugMsg, null, null, logMsgSrcId);
		curFileIsValid = curFileIsValid && memberIsValid;
		return memberIsValid;
	}

	/**
	 * @param expEl
	 * @return
	 */
	protected Element getMetadataEl(Element expEl) {
		/*
		 * ASSUMPTION: Validation against CMM has already been done so the
		 * Metadata exists and is retrieved correctly.
		 */
		String cid = expEl.getChildTextNormalize("ContentID", manifestNSpace);
		XPathExpression<Element> xpExpression = xpfac.compile(".//manifest:Metadata[@ContentID='" + cid + "']",
				Filters.element(), null, manifestNSpace);
		List<Element> elementList = xpExpression.evaluate(curRootEl);
		Element metaDataEl = elementList.get(0);
		return metaDataEl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddf.preProcess.manifest.Validator#getSupportedProfiles()
	 */
	public List<String> getSupportedProfiles() {
		// return an empty list
		return new ArrayList<String>();
	}
}
