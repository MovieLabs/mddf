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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
 
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.manifest.validation.ManifestValidator;

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

	// private Element manifestRootEl;
	private String curBranch;
	// private boolean curFileIsValid;
	private String curProfileId;
	private String curUseCase;
	private ArrayList<String> pidList = null;
	/**
	 * MMC Specification version supported by this class. Manifest MUST specify
	 * <tt>MediaManifest/Compatibility/SpecVersion</tt> that matches.
	 */
	public static final String XSD_VERSION = "1.5";

	public MMCoreValidator(LogMgmt loggingMgr) {
		super(true, loggingMgr);
		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_PROFILE; // or LogMgmt.TAG_MMC ???
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
	 * @see
	 * com.movielabs.mddf.preProcess.manifest.Validator#getSupporteUseCases(
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

	public boolean process(Element docRootEl,File xmlManifestFile, String profileId, List<String> useCases)
			throws JDOMException, IOException {
		super.process(docRootEl,xmlManifestFile);
		// boolean isOk = validateUseCases(profileId, useCases);
		return curFileIsValid;
	}

	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateConstraints() {
		super.validateConstraints();
		/*
		 * now check the additional constraints identified in MMC Section
		 * 2.1.2....
		 */
		LogReference srcRef = LogReference.getRef("MMC", "1.0", "mmc01");
		String rootType = curRootEl.getName();
		if (!rootType.equals("MediaManifest")) {
			String msg = "Missing required Element 'MediaManifest'";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, curRootEl, msg, null, srcRef, logMsgSrcId);
			curFileIsValid = false;
			return;
		}
		if (curRootEl.getAttribute("ManifestID") == null) {
			String msg = "Missing required Attribute 'ManifestID'";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, curRootEl, msg, null, srcRef, logMsgSrcId);
			curFileIsValid = false;
		}
		Element compEl = curRootEl.getChild("Compatibility", manifestNSpace);
		Element profileEl = compEl.getChild("Profile", manifestNSpace);
		String profile = profileEl.getTextNormalize();
		if (!profile.equals("MMC-1")) {
			String msg = "Incompatible value";
			String details = "Manifest must be specified as compatible with Profile 'MMC-1'";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, profileEl, msg, details, srcRef, logMsgSrcId);
			curFileIsValid = false;
		}
		/*
		 * Validate Presentations and TrackMetadata
		 */
		Element presEl = curRootEl.getChild("Presentations", manifestNSpace);
		XPathExpression<Element> xpExpression = xpfac.compile("./manifest:Presentation/manifest:TrackMetadata",
				Filters.element(), null, manifestNSpace);
		List<Element> tMdElList = xpExpression.evaluate(presEl);
		for (int i = 0; i < tMdElList.size(); i++) {
			Element trackMDataEl = (Element) tMdElList.get(i);
			/*
			 * AT LEAST ONE instance of a TrackReference (either Video, Audio,
			 * Subtitle, or Ancillary) is required for each TrackMetadata in a
			 * Presentation. Since the XSD requires a single TrackSlectionNumber
			 * also be defined as a child element of the TrackMetadata, all we
			 * need to do is check to see that there are TWO OR MORE child
			 * elements of the TrackMetadata.
			 */
			List<Element> childList = trackMDataEl.getChildren();
			if (childList.size() < 2) {
				String msg = "Missing TrackReference";
				String details = "AT LEAST ONE instance of a TrackReference (either Video, Audio, Subtitle, or Ancillary) is required ";
				loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, trackMDataEl, msg, details, srcRef, logMsgSrcId);
				;
				curFileIsValid = false;
			}
		}

		/*
		 * Validate Experiences
		 */
		xpExpression = xpfac.compile("./manifest:Experiences/manifest:Experience", Filters.element(), null,
				manifestNSpace);
		List<Element> expElList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < expElList.size(); i++) {
			Element expEl = (Element) expElList.get(i);
			if (expEl.getAttribute("ExperienceID") == null) {
				String msg = "Missing required Attribute 'ExperienceID'";
				loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, expEl, msg, null, srcRef, logMsgSrcId);
				curFileIsValid = false;
			}
			Element mdEl = null;
			String cid = expEl.getChildTextNormalize("ContentID", manifestNSpace);
			if ((cid == null) || (cid.isEmpty())) {
				String msg = "Missing 'ContentID'";
				loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, expEl, msg, null, srcRef, logMsgSrcId);
				curFileIsValid = false;
			} else {
				/* Retrieve referenced Metadata. */
				xpExpression = xpfac.compile("./manifest:Inventory/manifest:Metadata[@ContentID='" + cid + "']",
						Filters.element(), null, manifestNSpace);
				mdEl = xpExpression.evaluateFirst(curRootEl);
			}
			if (mdEl != null) {
				/*
				 * PictureGroupID should be included if the ContentID references
				 * Metadata with images (i.e., LocalizedInfo/ArtReference).
				 * Additional instances may be included.
				 * 
				 */
				xpExpression = xpfac.compile("../manifest:LocalizedInfo/manifest:ArtReference", Filters.element(), null,
						manifestNSpace);
				List<Element> artRefList = xpExpression.evaluate(mdEl);
				boolean hasArtRefs = !artRefList.isEmpty();
				if (hasArtRefs) {
					Element pgIdEl = expEl.getChild("PictureGroupID", manifestNSpace);
					if (pgIdEl == null) {
						String msg = "Use of 'PictureGroupID' is recommended";
						String details = "Any images used as ArtReferences should be accesible via a PictureGroup,";
						loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_WARN, expEl, msg, details, srcRef,
								logMsgSrcId);
					}
				}
			}

		}
		/*
		 * Validate ALIDExperienceMap. This is Required but only one Experience
		 * (top-level Experience) can be referenced.
		 */

		Element aeMapsEl = curRootEl.getChild("ALIDExperienceMaps", manifestNSpace);
		if (aeMapsEl == null) {
			String msg = "Missing required Element 'ALIDExperienceMaps'";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, curRootEl, msg, null, srcRef, logMsgSrcId);
			curFileIsValid = false;
		} else {
			xpExpression = xpfac.compile("./manifest:ALIDExperienceMap/manifest:ExperienceID", Filters.element(), null,
					manifestNSpace);
			expElList = xpExpression.evaluate(aeMapsEl);
			if (expElList.size() != 1) {
				String msg = "only one Experience can be referenced by 'ALIDExperienceMaps'";
				loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, aeMapsEl, msg, null, srcRef, logMsgSrcId);
				curFileIsValid = false;
			}
		}

	}

	// #############################################################
	// Code from this point on deals with Use Case validation and is
	// to considered incomplete and experimental.
	// -------------------------------------------------------------

	/**
	 * W.I.P.
	 * 
	 * @param profileId
	 * @return
	 */
	private boolean validateUseCases(String profileId, List<String> useCases) {
		/*
		 * From this point on we are dealing with the use-cases (an
		 * as-yet-to-be-groked capability)
		 */
		curProfileId = profileId;
		boolean isValid = true;
		for (int i = 0; i < useCases.size(); i++) {
			curUseCase = useCases.get(i);
			String msg;
			switch (curUseCase) {
			case "Simple":
				isValid = validateSimple(curRootEl) && isValid;
				break;
			case "Multi-Lang (no dub cards)":
				break;
			default:
				msg = "W.I.P.--> validation for Use Case " + curUseCase + " is YET TO BE IMPLEMENTED";
				loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, msg, curFile, logMsgSrcId);
				break;
			}
		}
		return isValid;
	}

	/**
	 * 
	 * ##### DEAD END??? #######
	 * 
	 * @param manifestRootEl
	 * @return
	 */
	private boolean validateSimple(Element manifestRootEl) {
		loggingMgr.log(LogMgmt.LEV_DEBUG, logMsgDefaultTag, "Validating for Use-Case '" + curUseCase + "'", curFile,
				logMsgSrcId);

		boolean isValid = true;
		// ------------------------------------------------------------------
		// NOTE: Following assumes validateXml() has already run and verified
		// XML structure is correct for MMC (i.e., a constrained subset of the
		// full CMM schema). It also assumes ManifestValidator.process()
		// has already verified any ID cross-references are valid.
		// ------------------------------------------------------------------
		/*
		 * ALIDExperienceMaps will contain a single ALIDExperienceMap that will
		 * in turn specify a single ExperienceID. This will be the 'feature'
		 * Experience. That in turn will have a single ExperienceChild
		 * indicating the trailer.
		 * 
		 */
		// String srcRef = "See Section 3.1.2 of " + srcDoc;
		LogReference srcRef = LogReference.getRef("MMC", "1.0", "mmc02");
		XPathExpression<Element> xpExpression = xpfac.compile(
				".//manifest:ALIDExperienceMaps/manifest:ALIDExperienceMap/manifest:ExperienceID", Filters.element(),
				null, manifestNSpace);
		Element expIdEl = xpExpression.evaluateFirst(manifestRootEl);
		String featureId = expIdEl.getTextNormalize();
		Element expSetEl = manifestRootEl.getChild("Experiences", manifestNSpace);
		List<Element> expElList = expSetEl.getChildren("Experience", manifestNSpace);
		if (expElList.size() > 2) {
			String msg = "Extra Experience elements found; Use-Case '" + curUseCase
					+ "' limited to TWO (feature and trailer)";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_WARN, expSetEl, msg, null, srcRef, logMsgSrcId);
		}
		// Retrieve the 'feature' Experience
		xpExpression = xpfac.compile("./manifest:Experience[@ExperienceID='" + featureId + "']", Filters.element(),
				null, manifestNSpace);
		Element featureExpEl = xpExpression.evaluateFirst(expSetEl);
		/*
		 * Retrieve the 'trailer' Experience which will be singleton
		 * ExperienceChild.
		 */
		Element trailerExpEl = null;
		List<Element> expChildElList = featureExpEl.getChildren("ExperienceChild", manifestNSpace);
		if (expChildElList.isEmpty() || expChildElList.size() < 1) {
			String msg = "Feature must have ExperienceChild for trailer; Use-Case '" + curUseCase + "'";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, featureExpEl, msg, null, srcRef, logMsgSrcId);
			isValid = false;
		} else if (expChildElList.size() > 1) {
			String msg = "Feature has extra ExperienceChild elements; Use-Case '" + curUseCase
					+ "' limited to ONE trailer";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, featureExpEl, msg, null, srcRef, logMsgSrcId);
			isValid = false;
		} else {
			Element expChildEl = expChildElList.get(0);
			Element relEl = expChildEl.getChild("Relationship", manifestNSpace);
			String relation = relEl.getTextNormalize();
			String reqdText = "ispromotionfor";
			if (!relation.equals(reqdText)) {
				String msg = "Invalid relationship; Should be '" + reqdText + "'";
				loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, relEl, msg, null, srcRef, logMsgSrcId);
				isValid = false;
			}
			String trailerExpId = expChildEl.getChildTextNormalize("ExperienceID", manifestNSpace);
			xpExpression = xpfac.compile("./manifest:Experience[@ExperienceID='" + trailerExpId + "']",
					Filters.element(), null, manifestNSpace);
			trailerExpEl = xpExpression.evaluateFirst(expSetEl);
		}
		validateExperience(featureExpEl, srcRef);
		if (trailerExpEl != null) {
			validateExperience(trailerExpEl, srcRef);
		}

		return isValid;
	}

	/**
	 * ##### DEAD END??? #######
	 * <p>
	 * Validate that a generic Experience is constructed as follows:
	 * </p>
	 * <ol>
	 * <li>A unique ExperienceID in the md: format, preferably using the eidr-s
	 * or eidr-x format.</li>
	 * <li>ContentID references metadata for the feature (i.e., matches the
	 * ContentID in the Inventory)</li>
	 * <li>PictureGroupID for metadata images</li>
	 * <li>An Audiovisual instance that references the Presentation or
	 * PlayableSequence</li>
	 * </ol>
	 * <p>
	 * The first two tests will have been made as part of general Manifest
	 * Validation. Remainder are 'Core-specific'.
	 * </p>
	 * 
	 * @param expEl
	 * @param srcRef
	 * @return
	 */
	private boolean validateExperience(Element expEl, LogReference srcRef) {
		boolean isValid = true;
		Element avEl = expEl.getChild("Audiovisual", manifestNSpace);
		if (avEl == null) {
			String msg = "Missing required child 'Audiovisual'; Use-Case '" + curUseCase + "'";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, expEl, msg, null, srcRef, logMsgSrcId);
			isValid = false;
		} else {
			/* Validate Presentation and it's associated Inventory entries */
			isValid = isValid && validatePresentation(avEl, AVStructure.ENGLISH_ONLY);
		}
		// check for 1 OR MORE PictureGroupID elements
		List<Element> pGrpIDList = expEl.getChildren("PictureGroupID", manifestNSpace);
		if (pGrpIDList.isEmpty()) {
			String msg = "One or more 'PictureGroupID' elements must be provided; Use-Case '" + curUseCase + "'";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_ERR, expEl, msg, null, srcRef, logMsgSrcId);
			isValid = false;
		}
		return isValid;
	}

	/**
	 * 
	 * ##### DEAD END??? #######
	 * <p>
	 * Verify that the Audiovisual element and its referenced constructs conform
	 * to the structural requirements of the indicated use case.
	 * </p>
	 * 
	 * @param avEl
	 * @param structure
	 * @return
	 */
	private boolean validatePresentation(Element avEl, AVStructure structure) {
		boolean isValid = true;
		/*
		 * Identify the underlying Presentations. If PlayableSequences are used,
		 * multiple Presentations may be involved.
		 */
		List<String> pidList = new ArrayList<String>();
		/*
		 * AV element will have as child 1 and only 1 of the following:
		 * PresentationID, PlayableSequenceID, or PlayableSequence.
		 */
		Element pidEl = avEl.getChild("PresentationID", manifestNSpace);
		Element pSeqidEl = avEl.getChild("PlayableSequenceID", manifestNSpace);
		Element pSeqEl = avEl.getChild("PlayableSequence", manifestNSpace);
		if (pidEl != null) {
			pidList.add(pidEl.getTextNormalize());
			String msg = "Audiovisual uses PresentationID";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_DEBUG, avEl, msg, null, null, logMsgSrcId);
		} else if (pSeqidEl != null) {
			String msg = "Audiovisual uses PlayableSequenceID";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_DEBUG, avEl, msg, null, null, logMsgSrcId);
			String pSeqId = pSeqidEl.getTextNormalize();
			XPathExpression<Element> xpExpression = xpfac.compile(
					"./manifest:PlayableSequences/manifest:PlayableSequence[@PlayableSequenceID='" + pSeqId + "']",
					Filters.element(), null, manifestNSpace);
			Element targetPSeqEl = xpExpression.evaluateFirst(curRootEl);
			if (targetPSeqEl == null) {
				msg = "The referenced PlayableSequenceID does not exisit.";
				loggingMgr.logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_ERR, pSeqidEl, msg, null, null, logMsgSrcId);
				isValid = false;
			} else {
				/*
				 * get PresentationID for each included Clips (i.e., there may
				 * be 1 or more Clips)
				 */
				xpfac.compile("./manifest:Clip/manifest:PresentationID", Filters.element(), null, manifestNSpace);
				List<Element> pidElList = xpExpression.evaluate(targetPSeqEl);
			}

		} else if (pSeqEl != null) {
			String msg = "Audiovisual uses PlayableSequence";
			loggingMgr.logIssue(logMsgDefaultTag, LogMgmt.LEV_DEBUG, avEl, msg, null, null, logMsgSrcId);

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
