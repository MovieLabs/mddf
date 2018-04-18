/**
 * Copyright (c) 2017 MovieLabs

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
package com.movielabs.mddflib.manifest.validation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.manifest.validation.profiles.CpeIP1Validator;
import com.movielabs.mddflib.manifest.validation.profiles.ProfileValidator;
import com.movielabs.mddflib.util.xml.MddfTarget;

/**
 * Handles validation of a CPE-Manifest as specified in TR-CPE-M1. As a
 * CPE-Manifest must also conform to the requirements of a Common Media Manifest
 * (CMM), the <tt>CpeValidator</tt> class extends the <tt>ManifestValidator</tt>
 * class.
 * <p>
 * <tt>CpeValidator</tt> supports profile validation by building an
 * <i>information model</i>. The tests to see if the model conforms to a
 * specific profile will, however, be performed by helper classes specific to
 * each CPE profile.
 * </p>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class CpeValidator extends ManifestValidator implements ProfileValidator {
	/**
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	public class ExperienceNode extends DefaultMutableTreeNode {

		private Element xmlEl;
		private String cid;

		/**
		 * @param curRootEl
		 */
		public ExperienceNode(Element xmlEl) {
			this.xmlEl = xmlEl;
			cid = xmlEl.getChildTextNormalize("ContentID", manifestNSpace);
		}

		/**
		 * @return
		 */
		public Element getExpEl() {
			return xmlEl;
		}

		public List<ExperienceNode> getChildren() {
			Enumeration<ExperienceNode> kinder = this.children();
			return Collections.list(kinder);
		}

		public List<ExperienceNode> getDescendents() {
			List<ExperienceNode> dList = new ArrayList<ExperienceNode>();
			for (ExperienceNode child : this.getChildren()) {
				dList.addAll(child.getDescendents());
				dList.add(child);
			}
			return dList;
		}

		/**
		 * @return
		 */
		public Element getMetadata() {
			return cid2MDataMap.get(cid);
		}

		/**
		 * @return the cid
		 */
		public String getCid() {
			return cid;
		}

	}

	public static final String LOGMSG_ID = "CpeValidator";
	private static final String LOGREFDOC = "CPR";
	protected HashMap<String, Element> cid2MDataMap;
	private CpeIP1Validator profileIP1Val;

	/**
	 * @param validateC
	 * @param loggingMgr
	 */
	public CpeValidator(LogMgmt loggingMgr) {
		super(true, loggingMgr);

		rootNS = manifestNSpace;

		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_MANIFEST;

		profileIP1Val = new CpeIP1Validator(this, loggingMgr);
	}

	public boolean process(MddfTarget target,  String profileId, List<String> useCases)
			throws JDOMException, IOException {
		super.process( target );
		if (!curFileIsValid) {
			String msg = "CPE validation terminated.. file is not a valid Media Manifest";
			loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, msg, curFile, logMsgSrcId);
			return curFileIsValid;
		}
		/*
		 * Continue with additional checks for compliance with CPE-Manifest
		 * spec.
		 */
		/*
		 * Note that validateConstraints() will invoke validateMetadata() which
		 * will in turn initialize the 'cid2MDataMap'.
		 */
		validateConstraints();
		DefaultTreeModel infoModel = buildInfoModel();
		validateModel(infoModel);
		if (profileId == null || (profileId.isEmpty())) {
			return curFileIsValid;
		}
		if (!curFileIsValid) {
			String msg = "CPE validation terminated prior to Profile Validation.. file is not a valid CPE Manifest";
			loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, msg, curFile, logMsgSrcId);
			return curFileIsValid;
		}
		switch (profileId) {
		case "IP-0":
			/*
			 * Profile IP-0 assumes no specific interactivity guidance within
			 * the Manifest and supports any content structure. This is used
			 * when the Retailer determines where and how bonus material is
			 * displayed. Validation is, therefore, not required (i.e., it is
			 * equivalent to profile='none'.
			 */
			break;
		case "IP-01":
			String msg = "Profile ID 'IP-01' has been deprecated. 'IP-1' should be used instead.";
			loggingMgr.log(LogMgmt.LEV_WARN, logMsgDefaultTag, msg, curFile, logMsgSrcId);
		case "IP-1":
			profileIP1Val.validateInfoModel(infoModel);
			break;
		default:
			msg = "Unrecognized CPE Profile '" + profileId + "'";
			loggingMgr.log(LogMgmt.LEV_ERR, logMsgDefaultTag, msg, curFile, logMsgSrcId);
			return false;
		}
		return curFileIsValid;
	}

	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateConstraints() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Validating constraints", curFile, LOGMSG_ID);
		/*
		 * Start with constraints defined for all Media Manifest files.
		 */
		super.validateConstraints();
	}

	/**
	 * Check for conformance with Sec 5.1 of CPE-Manifest (TR-CPE-M1)
	 * Specification.
	 */
	protected void validateMetadata() {
		super.validateMetadata();
		cid2MDataMap = new HashMap<String, Element>();
		/*
		 * Each Experience instance must include a ContentID element referencing
		 * metadata (i.e., ContentID is mandatory). The referenced metadata must
		 * be in the Inventory (i.e., Inventory/Metadata). The Metadata/Alias
		 * mechanism may be used.
		 */
		XPathExpression<Element> xpe1 = xpfac.compile(".//" + manifestNSpace.getPrefix() + ":Experience",
				Filters.element(), null, manifestNSpace);
		List<Element> elementList = xpe1.evaluate(curRootEl);
		for (Element expEl : elementList) {
			String cid = expEl.getChildTextNormalize("ContentID", manifestNSpace);
			// ContentID is mandatory for CPE
			if (cid == null || cid.isEmpty()) {
				String msg = "Missing required ContentID";
				LogReference srcRef = LogReference.getRef(LOGREFDOC, "MData01");
				loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, expEl, msg, null, srcRef, logMsgSrcId);
				curFileIsValid = false;
				continue;
			}
			/*
			 * The referenced metadata must be in the Inventory (i.e.,
			 * Inventory/Metadata).
			 */
			XPathExpression<Element> xpe2 = xpfac.compile(
					".//" + manifestNSpace.getPrefix() + ":Metadata[@ContentID='" + cid + "']", Filters.element(), null,
					manifestNSpace);
			Element metaDataEl = xpe2.evaluateFirst(curRootEl);
			if (metaDataEl == null) {
				String msg = "Missing required Metadata";
				String details = "Experience CID must reference metadata in Inventory";
				LogReference srcRef = LogReference.getRef(LOGREFDOC, "MData01");
				loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, expEl, msg, details, srcRef, logMsgSrcId);
				curFileIsValid = false;
				continue;
			} else {
				/*
				 * Find the BasicMetadata and save in HashMap to facilitate
				 * later processing. The Metadata/Alias mechanism may be used.
				 */
				Element basicMDEl = metaDataEl.getChild("BasicMetadata", manifestNSpace);
				if (basicMDEl == null) {
					// must be using indirect Alias mechanism.
					Element aliasMDEl = metaDataEl.getChild("Alias", manifestNSpace);
					if (aliasMDEl == null) {
						/*
						 * Only way to pass XSD checks and reach this point is
						 * if they used a ContainerReference.
						 */
						String msg = "Missing required Metadata/BasicMetadata or Metadata/Alias";
						String details = "Experience CID must reference metadata in Inventory";
						LogReference srcRef = LogReference.getRef(LOGREFDOC, "MData01");
						loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, expEl, msg, details, srcRef,
								logMsgSrcId);
						curFileIsValid = false;
					} else {
						// make sure Alias points to BasicMetadata in Inventory
						String aliasedCid = aliasMDEl.getAttributeValue("ContentID", "not specified");
						XPathExpression<Element> xpe3 = xpfac.compile(
								".//" + manifestNSpace.getPrefix() + ":BasicMetadata[@ContentID='" + aliasedCid + "']",
								Filters.element(), null, manifestNSpace);
						basicMDEl = xpe3.evaluateFirst(curRootEl);
						if (basicMDEl == null) {
							String msg = "Metadata/Alias does not reference BasicMetadata in Inventory";
							String details = "Experience CID must reference metadata in Inventory";
							LogReference srcRef = LogReference.getRef(LOGREFDOC, "MData01");
							loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, aliasMDEl, msg, details, srcRef,
									logMsgSrcId);
							curFileIsValid = false;
						}
					}
				}
				cid2MDataMap.put(cid, basicMDEl);
				/*
				 * Any additional checking of the actual content of the metadata
				 * must wait until the Info Model has been built.
				 */
			}
		}
	}

	/**
	 * Validates general model requirements (i.e., not specific to a given
	 * Profile). This includes checking an Experience's metadata entry for
	 * compliance with the Experience's position and context within the
	 * Information Model. See Section 5.1.2.3 of [TR-CPE-M1]
	 * 
	 * @param infoModel
	 */
	protected void validateModel(DefaultTreeModel infoModel) {
		ExperienceNode modelRoot = (ExperienceNode) infoModel.getRoot();
		Enumeration<ExperienceNode> kinder = modelRoot.children();
		for (ExperienceNode topNode : Collections.list(kinder)) {
			validateTopMetadata(topNode);
			List<ExperienceNode> descendants = topNode.getDescendents();
			for (ExperienceNode lowerNode : descendants) {
				validateLowerMetadata(lowerNode);
			}
		}
	}

	/**
	 * The following rules apply to top-level grouping nodes (i.e., those that
	 * are not presented to the user), such as “in-movie” and “out-of-movie” in
	 * the examples above:
	 * <ul>
	 * <li>There is only one instance of LocalizedInfo</li>
	 * <li>That instance has TitleSort set to the name of the grouping category
	 * (e.g., “in-movie” or “out-of-movie”).</li>
	 * <li>LocalizedInfo@language may contain any language code, and it is
	 * ignored.</li>
	 * <li>No other metadata is present unless it is a required element or
	 * attribute in the schema.</li>
	 * </ul>
	 * 
	 * @param topNode
	 */
	private void validateTopMetadata(ExperienceNode topNode) {
		Element basicMDataEl = topNode.getMetadata();
		List<Element> locElList = basicMDataEl.getChildren("LocalizedInfo", mdNSpace);
		if (locElList.size() != 1) {
			String errMsg = "top-level grouping nodes require EXACTLY one instance of LocalizedInfo";
			loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_ERR, basicMDataEl, errMsg, null, null, LOGMSG_ID);
			curFileIsValid = false;
		}
	}

	/**
	 * <ul>
	 * <li>An instance of LocalizedInfo should be included for each language
	 * supported for the title.</li>
	 * <li>LocalizedInfo/TitleDisplayUnlimited and TitleSort contains the
	 * user-visible name for that node. Note that even when an image is the
	 * intended UI element, text should still be provided for accessibility
	 * (text to speech).</li>
	 * <li>LocalizedInfo/ArtReference includes images associated with the node.
	 * Implementers note: Implementations should accept ImageID, PictureID,
	 * PictureGroupID and URL.</li>
	 * </ul>
	 * 
	 * @param lowerNode
	 */
	private void validateLowerMetadata(ExperienceNode lowerNode) {
		Element basicMDataEl = lowerNode.getMetadata();
		List<Element> localMDataList = basicMDataEl.getChildren("LocalizedInfo", mdNSpace);
		for (int i = 0; i < localMDataList.size(); i++) {
			Element locMDEl = localMDataList.get(i);
			String title = locMDEl.getChildTextNormalize("TitleSort", mdNSpace);
			if (title.isEmpty()) {
				curFileIsValid = false;
				String errMsg = "TitleSort is empty";
				loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_ERR, locMDEl, errMsg, null, null, logMsgSrcId);
			}
			String titleDU = locMDEl.getChildTextNormalize("TitleDisplayUnlimited", mdNSpace);
			if ((titleDU == null) || (titleDU.isEmpty())) {
				curFileIsValid = false;
				String errMsg = "TitleDisplayUnlimited is empty or missing";
				loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_ERR, locMDEl, errMsg, null, null, logMsgSrcId);
			}
		}
	}

	/**
	 * Identify all ALID and determine which experience is used when the ALID is
	 * accessed by a consumer.
	 * 
	 * @param root
	 * @return
	 */
	public List<Element> extractAlidMap(Element root) {
		Set<String> idSet = new HashSet<String>();
		List<Element> primaryExpSet = new ArrayList<Element>();
		Element mapsEl = root.getChild("ALIDExperienceMaps", manifestNSpace);
		if (mapsEl == null) {
			return null;
		}
		List<Element> mapEList = mapsEl.getChildren("ALIDExperienceMap", manifestNSpace);
		Object[] targets = mapEList.toArray();
		for (int i = 0; i < targets.length; i++) {
			Element target = (Element) targets[i];
			/* get the values for ALID and ExperienceID */
			Element expIdEl = target.getChild("ExperienceID", manifestNSpace);
			String expId = expIdEl.getTextNormalize();
			if (!idSet.contains(expId)) {
				idSet.add(expId);
				XPathExpression<Element> xpExpression = xpfac.compile(
						".//" + manifestNSpace.getPrefix() + ":Experience[@ExperienceID='" + expId + "']",
						Filters.element(), null, manifestNSpace);
				Element expEl = xpExpression.evaluateFirst(root);
				if (expEl != null) {
					primaryExpSet.add(expEl);
				} else {
					String errMsg = "Unable to locate ALID's root experience; expId = " + expId;
					loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_ERR, expIdEl, errMsg, null, null, LOGMSG_ID);
					curFileIsValid = false;
				}
			}
		}
		return primaryExpSet;
	}

	/**
	 * Builds a hierarchical structure of Experience Elements to facilitate
	 * validation of specific Information Models.
	 */
	protected DefaultTreeModel buildInfoModel() {
		ExperienceNode rootNode = new ExperienceNode(curRootEl);
		DefaultTreeModel infoModel = new DefaultTreeModel(rootNode);
		/*
		 * Get all ALID and identify for each the 'root' (i.e., main) Experience
		 * element
		 */
		List<Element> primaryExpSet = extractAlidMap(curRootEl);
		if (primaryExpSet == null || (primaryExpSet.isEmpty())) {
			String msg = "Terminating Model validation due to empty or missing ALIDExperienceMaps";
			loggingMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_MODEL, msg, curFile, LOGMSG_ID);
			curFileIsValid = false;
			return infoModel;
		}
		for (int i = 0; i < primaryExpSet.size(); i++) {
			Element nextExpEL = primaryExpSet.get(i);
			ExperienceNode nextExpNode = new ExperienceNode(nextExpEL);
			rootNode.add(nextExpNode);
			addChildExperiences(nextExpNode);
		}
		return infoModel;

	}

	/**
	 * Expand hierarchical structure of Experience Elements by recursively
	 * descending and adding all <tt>ExperienceChild</tt> elements found.
	 * 
	 * @param nextExpNode
	 */
	private void addChildExperiences(ExperienceNode curExpNode) {
		Element curExpEl = curExpNode.getExpEl();
		if (curExpEl == null) {
			return;
		}
		List<Element> allChildList = curExpEl.getChildren("ExperienceChild", manifestNSpace);
		// Recursively descend tree
		for (int i = 0; i < allChildList.size(); i++) {
			Element nextChildEl = allChildList.get(i);
			String expXRef = nextChildEl.getChildTextNormalize("ExperienceID", manifestNSpace);
			XPathExpression<Element> xpExpression = xpfac.compile(
					".//manifest:Experience[@ExperienceID='" + expXRef + "']", Filters.element(), null, manifestNSpace);
			Element childExpEl = xpExpression.evaluateFirst(curRootEl);
			if (childExpEl == null) {
				String errMsg = "Unable to locate child experience; expId = " + expXRef;
				loggingMgr.logIssue(LogMgmt.TAG_MODEL, LogMgmt.LEV_ERR, nextChildEl, errMsg, null, null, LOGMSG_ID);
				curFileIsValid = false;
			} else {
				ExperienceNode nextExpNode = new ExperienceNode(childExpEl);
				curExpNode.add(nextExpNode);
				addChildExperiences(nextExpNode);
			}
		}
	}

	/**
	 * Return an ordered list of child Elements that have the specified
	 * relationship with the parent or null if unable to sort the children. A
	 * return value of <tt>null</tt> is, therefore, not the same as returning an
	 * empty List as the later indicates no matching child Elements were found
	 * rather than a problem while sorting.
	 * <p>
	 * Ordering is determined in one of two ways. If
	 * <tt>&lt;SequenceInfo&gt;</tt> elements are present they will be used to
	 * determine the order of the list entries returned. Otherwise ordering is
	 * indeterminate.
	 * </p>
	 * <p>
	 * Additional restrictions on the use of <tt>&lt;SequenceInfo&gt;</tt>:
	 * <ul>
	 * <li><tt>&lt;SequenceInfo&gt;</tt> elements shall be used for either all
	 * of the children or none of the children. Mixed usage is not allowed.
	 * <li>SequenceInfo/Number starts with one and increases monotonically for
	 * each child</li>
	 * </ul>
	 * </p>
	 * 
	 * @param parentEl
	 * @param childName
	 * @param relationship
	 * @param seqInfoRequired
	 * @return <tt>List</tt> of child Elements or null if unable to sort the
	 *         children.
	 */
	public List<Element> getSortedChildren(Element parentEl, String childName, String relationship,
			boolean seqInfoRequired) {
		boolean hasErrors = false;
		// ..........
		List<Element> allChildList = parentEl.getChildren(childName, manifestNSpace);
		Element[] firstPass = new Element[allChildList.size()];
		int lastIndex = -1;
		for (int i = 0; i < allChildList.size(); i++) {
			Element nextEl = allChildList.get(i);
			String relType = nextEl.getChildTextNormalize("Relationship", manifestNSpace);
			if (relType == null) {
				String summaryMsg = "Missing Relationship element in " + childName + " element";
				loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_WARN, nextEl, summaryMsg, null, null, logMsgSrcId);
			} else if (relType.equalsIgnoreCase(relationship)) {
				Element seqEl = nextEl.getChild("SequenceInfo", manifestNSpace);
				String seqNum = null;
				if (seqEl != null) {
					seqNum = seqEl.getChildTextNormalize("Number", mdNSpace);
					// check for empty string..
					if (seqNum.isEmpty()) {
						seqNum = null;
					}
				}
				if (seqInfoRequired && (seqNum == null)) {
					// Flag as ERROR
					loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, seqEl, "Missing required SequenceInfo", null,
							null, logMsgSrcId);
					hasErrors = true;
				} else if (seqNum != null) {
					// seqNum should be monotonically increasing starting with 1
					int index = -1;
					try {
						index = Integer.parseInt(seqNum) - 1;
					} catch (NumberFormatException e) {
					}
					if (index < 0) {
						loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, seqEl,
								"Invalid Number for SequenceInfo (must be a positive integer).", null, null,
								logMsgSrcId);
						hasErrors = true;
					} else if (index >= firstPass.length) {
						loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, seqEl,
								"Invalid Number for SequenceInfo (must increase monotonically).", null, null,
								logMsgSrcId);
						hasErrors = true;

					} else {
						if (firstPass[index] != null) {
							loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, seqEl,
									"Duplicate Number in SequenceInfo", null, null, logMsgSrcId);
							hasErrors = true;
						} else {
							firstPass[index] = nextEl;
							lastIndex = Math.max(lastIndex, index);
						}
					}
				} else {
					/*
					 * Explicit SeqInfo is neither provided nor required so use
					 * as-is order
					 */
					firstPass[i] = nextEl;
					lastIndex = i;
				}
			}
		}
		if (hasErrors) {
			return null;
		}

		List<Element> childElList = new ArrayList<Element>();
		/*
		 * Transfer the Elements from the 'firstPass' array to the List. While
		 * transferring, check to make sure the SequenceInfo/Number starts with
		 * one and increases monotonically for each child.
		 * 
		 */
		for (int i = 0; i <= lastIndex; i++)

		{
			if (firstPass[i] != null) {
				childElList.add(firstPass[i]);
			} else {
				int gap = i + 1;
				loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, parentEl,
						"Incomplete SequenceInfo... missing Number=" + gap, null, null, logMsgSrcId);
				hasErrors = true;
			}
		}
		if (hasErrors) {
			return null;
		}
		return childElList;
	}

	/**
	 * @param expEl
	 * @return
	 */
	public Element getMetadataEl(Element expEl) {
		String cid = expEl.getChildTextNormalize("ContentID", manifestNSpace);
		Element metaDataEl = cid2MDataMap.get(cid);
		if (metaDataEl == null) {
			// do it the hard way
			XPathExpression<Element> xpExpression = xpfac.compile(".//manifest:Metadata[@ContentID='" + cid + "']",
					Filters.element(), null, manifestNSpace);
			List<Element> elementList = xpExpression.evaluate(curRootEl);
			metaDataEl = elementList.get(0);
		}
		return metaDataEl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.manifest.validation.profiles.ProfileValidator#
	 * getSupportedProfiles()
	 */
	@Override
	public List<String> getSupportedProfiles() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.manifest.validation.profiles.ProfileValidator#
	 * getSupporteUseCases(java.lang.String)
	 */
	@Override
	public List<String> getSupporteUseCases(String profile) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.manifest.validation.profiles.ProfileValidator#
	 * setLogger(com.movielabs.mddflib.logging.LogMgmt)
	 */
	@Override
	public void setLogger(LogMgmt logMgr) {
		// TODO Auto-generated method stub

	}
}
