/**
 * Created Oct 30, 2015 
 * Copyright Motion Picture Laboratories, Inc. 2015
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.CMValidator;
import com.movielabs.mddflib.util.PathUtilities;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.SchemaWrapper;
import com.movielabs.mddflib.util.xml.XmlIngester;
import com.movielabs.mddflib.util.xml.XsdValidation;

/**
 * Validates a Manifest file as conforming to the Common Media Manifest (CMM) as
 * specified in <tt>TR-META-MMM (v1.5)</tt>. Validation also includes testing
 * for conformance with the appropriate version of the
 * <tt>Common Metadata (md)</tt> specification.
 * 
 * @see <a href= "http://www.movielabs.com/md/manifest/v1.5/Manifest_v1.5.pdf">
 *      TR-META-MMM (v1.5)</a>
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class ManifestValidator extends CMValidator {

	public static final String LOGMSG_ID = "ManifestValidator";
	protected static HashMap<String, String> mmm_id2typeMap;
	static {
		mmm_id2typeMap = new HashMap<String, String>();
		mmm_id2typeMap.put("AudioTrackID", "audtrackid");
		mmm_id2typeMap.put("VideoTrackID", "vidtrackid");
		mmm_id2typeMap.put("SubtitleTrackID", "subtrackid");
		mmm_id2typeMap.put("InteractiveTrackID", "interactiveid");
		mmm_id2typeMap.put("ProductID", "alid");
		mmm_id2typeMap.put("ContentID", "cid");
		mmm_id2typeMap.put("TextObjectID", "textobjid");
		mmm_id2typeMap.put("ExternalManifestID", "manifestid");
	}

	private Map<String, List<Element>> supportingRsrcLocations;

	/**
	 * @param validateC
	 */
	public ManifestValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;

		rootNS = manifestNSpace;

		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_MANIFEST;

		id2typeMap = mmm_id2typeMap;
	}

	/**
	 * @param target
	 * @return
	 * @throws IOException
	 * @throws JDOMException
	 */
	public boolean process(MddfTarget target) throws IOException, JDOMException {
		String schemaVer = identifyXsdVersion(target);
		loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, "Validating using Schema Version " + schemaVer, curFile,
				logMsgSrcId);
		setManifestVersion(schemaVer);
		rootNS = manifestNSpace;

		curTarget = target;
		curFile = target.getSrcFile();
		curFileName = curFile.getName();
		curFileIsValid = true;
		curRootEl = null;
		supportingRsrcLocations = new HashMap<String, List<Element>>();

		validateXml(target);
		if (!curFileIsValid) {
			String msg = "Schema validation check FAILED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MANIFEST, msg, curFile, logMsgSrcId);
			return false;
		}
		curRootEl = target.getXmlDoc().getRootElement();
		String msg = "Schema validation check PASSED";
		loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MANIFEST, msg, curFile, logMsgSrcId);
		if (validateC) {
			validateConstraints();
		}
		return curFileIsValid;
	}

	/**
	 * Validate everything that is fully specified via the XSD.
	 * 
	 * @param target
	 */
	protected boolean validateXml(MddfTarget target) {
		String manifestXsdFile = XsdValidation.defaultRsrcLoc + "manifest-v" + XmlIngester.MAN_VER + ".xsd";
		curFileIsValid = xsdHelper.validateXml(target, manifestXsdFile, logMsgSrcId);
		return curFileIsValid;
	}

	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateConstraints() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_MANIFEST, "Validating constraints", curFile, LOGMSG_ID);
		super.validateConstraints();

		SchemaWrapper targetSchema = SchemaWrapper.factory("manifest-v" + XmlIngester.MAN_VER);
		validateNotEmpty(targetSchema);

		/* Validate indexed sequences that must be monotonically increasing */
		validateIndexing("Chapter", manifestNSpace, "index", "Chapters", manifestNSpace, true, false, true, true);
		validateIndexing("Clip", manifestNSpace, "sequence", "PlayableSequence", manifestNSpace, false, true, false,
				true);
		validateIndexing("ImageClip", manifestNSpace, "sequence", "PlayableSequence", manifestNSpace, false, true,
				false, true);
		// ?? PictureGroup/Picture/Sequence
		validateIndexing("TextString", manifestNSpace, "index", "TextObject", manifestNSpace, false, true, false, false);
		// ?? ExperienceChild/SequenceInfo/{md}Number ??? Note other 3 domain-specific
		// numbers
		validateIndexing("TextGroupID", manifestNSpace, "index", "TimedEvent", manifestNSpace, false, true, false,
				false);

		// -------------------------------------------------------------------------------------

		/*
		 * Validate the usage of controlled vocab (i.e., places where XSD specifies a
		 * xs:string but the documentation specifies an enumerated set of allowed values
		 * or otherwise constrained).
		 */
		// start with Common Metadata spec..
		validateCMVocab();
		validateResolution("//{md}LocalizedInfo/{md}ArtReference/@resolution");
		validateResolution("//{manifest}Picture/{manifest}ImageID/@resolution");
		validateResolution("//{manifest}Picture/{manifest}ThumbnailImageID/@resolution");

		// Now do any defined in Manifest spec..
		validateManifestVocab();

		validateLocations();

		validateMetadata();

		validateUsage();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.util.CMValidator#validateIdSet()
	 */
	protected void validateIdSet() {
		// do set-up and init....
		super.validateIdSet();

		// TODO: Load from JSON file....
		/*
		 * Check ID for any Inventory components....
		 */
		validateId("Audio", "AudioTrackID", true, true);
		validateId("Video", "VideoTrackID", true, true);
		validateId("Image", "ImageID", true, true);
		validateId("Subtitle", "SubtitleTrackID", true, true);
		validateId("Interactive", "InteractiveTrackID", true, true);
		validateId("Ancillary", "AncillaryTrackID", true, true);
		validateId("TextObject", "TextObjectID", true, true);
		validateId("Metadata", "ContentID", true, true);

		// added in v1.7:
		validateId("ExternalManifest", "ManifestID", true, true);
		validateId("ExternalManifestID", null, true, true);

		/*
		 * Check ID for everything else...
		 */
		validateId("PictureGroup", "PictureGroupID", true, true);
		validateId("TextGroup", "TextGroupID", true, true);
		validateId("AppGroup", "AppGroupID", true, true);
		validateId("App", "AppID", true, true);
		validateId("Presentation", "PresentationID", true, true);
		validateId("PlayableSequence", "PlayableSequenceID", true, true);
		validateId("TimedEventSequence", "TimedSequenceID", true, true);
		validateId("Experience", "ExperienceID", true, true);
		validateId("Gallery", "GalleryID", false, true);

		/* Now validate cross-references */
		validateXRef(".//manifest:Experience/manifest:ContentID", "Metadata");
		validateXRef(".//manifest:Experience/manifest:PictureGroupID", "PictureGroup");
		validateXRef(".//manifest:Experience/manifest:TextGroupID", "TextGroup");
		validateXRef(".//manifest:Experience/manifest:TimedSequenceID", "TimedEventSequence");
		String xpath = ".//manifest:ExperienceChild/manifest:ExperienceID[not(../manifest:ExternalManifestID)]";
		validateXRef(xpath, "Experience");

		validateXRef(".//manifest:Gallery/manifest:PictureGroupID", "PictureGroup");
		validateXRef(".//manifest:Gallery/manifest:ContentID", "Metadata");

		validateXRef(".//manifest:Audiovisual/@ContentID", "Metadata");
		validateXRef(".//manifest:Audiovisual/manifest:PresentationID", "Presentation");
		validateXRef(".//manifest:Audiovisual/manifest:PlayableSequenceID", "PlayableSequence");

		validateXRef(".//manifest:Clip/manifest:PresentationID", "Presentation");
		
		validateXRef(".//manifest:ImageClip/manifest:ImageID", "Image");
		validateXRef(".//manifest:Chapter/manifest:ImageID", "Image");
		validateXRef(".//manifest:Picture/manifest:ImageID", "Image");
		validateXRef(".//manifest:Picture/manifest:ThumbnailImageID", "Image");
		String msgAR1 = "Identification of ArtReference by means other that ImageID should only be done under bi-lateral agreement";
		validateXRef(".//md:LocalizedInfo/md:ArtReference", "Image", LogMgmt.LEV_NOTICE, msgAR1);

		validateXRef(".//manifest:VideoTrackReference/manifest:VideoTrackID", "Video");
		validateXRef(".//manifest:AudioTrackReference/manifest:AudioTrackID", "Audio");
		validateXRef(".//manifest:AncillaryTrackReference/manifest:AncillaryTrackID", "Ancillary");
		validateXRef(".//manifest:SubtitleTrackReference/manifest:SubtitleTrackID", "Subtitle");
		validateXRef(".//manifest:TextObject/manifest:SubtitleID", "Subtitle");

		validateXRef(".//manifest:TextGroup/manifest:TextObjectID", "TextObject");

		validateXRef(".//manifest:InteractiveTrackReference/manifest:InteractiveTrackID", "Interactive");

		validateXRef(".//manifest:Experience/manifest:App/manifest:AppGroupID", "AppGroup");

		validateXRef(".//manifest:TimedEventSequence/manifest:PresentationID", "Presentation");
		validateXRef(".//manifest:TimedEventSequence/manifest:PlayableSequenceID", "PlayableSequence");

		validateXRef(".//manifest:TimedEvent/manifest:PresentationID", "Presentation");
		validateXRef(".//manifest:TimedEvent/manifest:PlayableSequenceID", "PlayableSequence");
		validateXRef(".//manifest:TimedEvent/manifest:ExperienceID", "Experience");
		validateXRef(".//manifest:TimedEvent/manifest:GalleryID", "Gallery");
		validateXRef(".//manifest:TimedEvent/manifest:AppGroupID", "AppGroup");
		validateXRef(".//manifest:TimedEvent/manifest:AppID", "App");
		validateXRef(".//manifest:TimedEvent/manifest:TextGroupID", "TextGroup");

		validateXRef(".//manifest:ALIDExperienceMap/manifest:ExperienceID", "Experience");

		// added in v1.7:
		xpath = ".//manifest:Inventory/manifest:ExternalManifest/@ManifestID";
		validateXRef(xpath, "ExternalManifestID");

		/*
		 * SPECIAL CASE: For v1.7 and after.... When ExternalManifestID is present in a
		 * ExperienceChild, there SHALL NOT be an Experience with that ID contained in
		 * the same file. That is, the Manifest is valid only if the Experience is NOT
		 * present."
		 */
		HashSet<String> idSet = idSets.get("Experience");
		xpath = ".//manifest:ExperienceChild/manifest:ExperienceID[../manifest:ExternalManifestID]";
		XPathExpression<Element> xpExpression = xpfac.compile(xpath, Filters.element(), null, manifestNSpace);
		List<Element> elementList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < elementList.size(); i++) {
			Element refEl = (Element) elementList.get(i);
			String targetId = refEl.getTextNormalize();
			if (idSet.contains(targetId)) {
				String msg = "When ExternalManifestID is  present in a ExperienceChild, there may not be an Experience with that ID contained in the same file";
				logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_ERR, refEl, msg, null, null, logMsgSrcId);
				curFileIsValid = false;
			}
		}
		/*
		 * SPECIAL CASE: For v1.7 and after.... When ExternalManifestID is present in a
		 * ExperienceChild, there may not yet be an ExternalManifest in the Inventory.
		 * Hence, 'checkForOrphans()' should ignore
		 */
		idXRefCounts.remove("ExternalManifest");
		checkForOrphans();
	}

	/**
	 * 
	 */
	private void validateManifestVocab() {
		/*
		 * handle any case of backwards (or forwards) compatibility between versions.
		 */
		String vocabVer = MAN_VER;
		switch (MAN_VER) { 
		case "1.8.1":
			vocabVer = "1.8";
			break; 
		}

		JSONObject manifestVocab = (JSONObject) getVocabResource("manifest", vocabVer);
		if (manifestVocab == null) {
			String msg = "Unable to validate controlled vocab: missing resource file vocab_manifest_v"+vocabVer;
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_MANIFEST, msg, curFile, logMsgSrcId);
			curFileIsValid = false;
			return;
		}

		// Now do any defined in the Manifest spec..
		JSONArray allowed;

		allowed = manifestVocab.optJSONArray("PictureGroupType");
		LogReference srcRef = LogReference.getRef("MMM", MAN_VER, "mmm001");
		validateVocab(manifestNSpace, "PictureGroup", manifestNSpace, "Type", allowed, srcRef, true);

		allowed = manifestVocab.optJSONArray("TimedEventType");
		srcRef = LogReference.getRef("MMM", MAN_VER, "mmm002");
		validateVocab(manifestNSpace, "TimedEvent", manifestNSpace, "Type", allowed, srcRef, true);

		allowed = manifestVocab.optJSONArray("AudiovisualType");
		srcRef = LogReference.getRef("MMM", MAN_VER, "mmm003");
		validateVocab(manifestNSpace, "Audiovisual", manifestNSpace, "Type", allowed, srcRef, true);

		allowed = manifestVocab.optJSONArray("ExperienceAppType");
		srcRef = LogReference.getRef("CM", MAN_VER, "mmm_expAppType");
		validateVocab(manifestNSpace, "App", manifestNSpace, "Type", allowed, srcRef, true);

		JSONObject availVocab = (JSONObject) getVocabResource("avail", AVAIL_VER);
		if (availVocab != null) {
			allowed = availVocab.optJSONArray("ExperienceCondition");
			srcRef = LogReference.getRef("CM", CM_VER, "cm007");
			validateVocab(manifestNSpace, "ExperienceID", null, "@condition", allowed, srcRef, true);
		}

	}

	/**
	 * 
	 */
	protected void validateCMVocab() {
		validateBasicMetadata();

		JSONArray expectedValues;
		LogReference docRef;
		switch (MAN_VER) {
		case "1.8":
			JSONObject cmVocab = (JSONObject) getVocabResource("cm", CM_VER);
			if (cmVocab == null) {
				String msg = "Unable to validate controlled vocab: missing resource file";
				loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_MANIFEST, msg, curFile, logMsgSrcId);
				curFileIsValid = false;
				return;
			}
			expectedValues = cmVocab.optJSONArray("WorkType");
			docRef = LogReference.getRef("CM", CM_VER, "cm002");
			validateVocab(manifestNSpace, "Purpose", manifestNSpace, "WorkType", expectedValues, docRef, true, true);
			break;
		}
	}

	// ########################################################################

	/**
	 * Validate a local <tt>ContainerLocation</tt>. These should be a
	 * <i>relative</i> path where the base location is that of the XML file being
	 * processed. Note that network locations (i.e., URLs) are <b>not</b> verified.
	 * Neither is there any requirement that the URL schema is HTTP as proprietary
	 * schemes are allowed (e.g., <tt>flixster://foobar/BigBuckBunny.mp4</tt>)
	 */
	protected void validateLocations() {
		String pre = manifestNSpace.getPrefix();
		String baseLoc = curFile.getAbsolutePath();
		LogReference srcRef = LogReference.getRef("MMM", "mmm_locType");
		XPathExpression<Element> xpExp01 = xpfac.compile(".//" + pre + ":ContainerLocation", Filters.element(), null,
				manifestNSpace);
		List<Element> cLocElList = xpExp01.evaluate(curRootEl);
		outterLoop: for (int i = 0; i < cLocElList.size(); i++) {
			Element clocEl = cLocElList.get(i);
			String containerPath = clocEl.getTextNormalize();
			if (containerPath.startsWith("file:")) {
				String errMsg = "Invalid syntax for local file location";
				String details = "Location of a local file must be specified as a relative path";
				logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_ERR, clocEl, errMsg, details, srcRef, logMsgSrcId);
				curFileIsValid = false;
				continue outterLoop;
			}
			/*
			 * at this point we have either (1) a relative path to a local file OR (2) a
			 * full URL. No further validation takes place but we save the location in case
			 * the Validation Controller being used wants to perform additional checks.
			 */
			try {
				String targetLoc = PathUtilities.convertToAbsolute(baseLoc, containerPath);
				String dbgMsg = "Possible MDDF file to validate at ContainerLocation " + targetLoc;
				logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_DEBUG, clocEl, dbgMsg, null, null, logMsgSrcId);
				List<Element> usage = supportingRsrcLocations.get(targetLoc);
				if (usage == null) {
					usage = new ArrayList<Element>();
				}
				usage.add(clocEl);
				supportingRsrcLocations.put(targetLoc, usage);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	protected void validateMetadata() {
		String pre = manifestNSpace.getPrefix();
		/**
		 * The ContentID alias mechanism is filter for a peer BasicMetadata that results
		 * in a new BasicMetadata instance with a subset of the LocalizedInfo instance
		 * in the original BasicMetadata instance. Thus:
		 * <ul>
		 * <li>If the a Metatadata element contains a child Alias, it must also have as
		 * a child a BasicMetadata element for the Alias to filter. This may be
		 * identified either directly via a child BasicMetadata element or indirectly
		 * via a ContainerReference.</li>
		 * 
		 * <li>If the Alias has a LocalizedPair/LanguageIncluded = 'foobar' then the
		 * BasicMetadata element must have a child
		 * LocalizedInfo[@language='foobar']</li>
		 * </ul>
		 */
		XPathExpression<Element> xpExp01 = xpfac.compile(".//" + pre + ":Alias", Filters.element(), null,
				manifestNSpace);
		List<Element> aliasElList = xpExp01.evaluate(curRootEl);
		for (Element aliasEl : aliasElList) {
			Element mdEl = aliasEl.getParentElement();
			Element basicMDataEl = mdEl.getChild("BasicMetadata", manifestNSpace);
			if (basicMDataEl == null) {
				String errMsg = "Metadata/Alias requires peer BasicMetadata";
				LogReference srcRef = LogReference.getRef("MMM", "metadataAlias");
				logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_ERR, aliasEl, errMsg, null, srcRef, logMsgSrcId);
				curFileIsValid = false;
				continue;
			}
			XPathExpression<Element> xpExp02 = xpfac.compile(".//" + pre + ":LanguageIncluded", Filters.element(), null,
					manifestNSpace);
			List<Element> langElList = xpExp02.evaluate(aliasEl);
			for (Element langEl : langElList) {
				String includedLang = langEl.getTextNormalize();
				XPathExpression<Element> xpExp03 = xpfac.compile(
						".//" + mdNSpace.getPrefix() + ":LocalizedInfo[@language='" + includedLang + "']",
						Filters.element(), null, manifestNSpace, mdNSpace);
				Element locInfoEL = xpExp03.evaluateFirst(basicMDataEl);
				if (locInfoEL == null) {
					String errMsg = "IncludedLanguage not supported by BasicMetadata/LocalizedInfo";
					LogReference srcRef = LogReference.getRef("MMM", "metadataAlias");
					logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_ERR, langEl, errMsg, null, srcRef, logMsgSrcId);
					curFileIsValid = false;

				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.util.CMValidator#validateUsage()
	 */
	protected void validateUsage() {
		super.validateUsage();
		/*
		 * Load JSON that defines various constraints on structure of the XML This is
		 * version-specific but not all schema versions have their own unique struct
		 * file (e.g., a minor release may be compatible with a previous release).
		 */
		String structVer = null;
		switch (MAN_VER) {
		case "1.5":
		case "1.6":
			structVer = "1.6";
			break;
		case "1.7":
			structVer = "1.7";
			break;
		case "1.8":
		case "1.8.1":
			structVer = "1.8";
			break;
		default:
			// Not supported for the version
			String msg = "Unable to process; missing structure definitions for Manifest v" + MAN_VER;
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_MANIFEST, msg, curFile, logMsgSrcId);
			return;
		}

		JSONObject structDefs = XmlIngester.getMddfResource("structure_manifest", structVer);
		if (structDefs == null) {
			// LOG a FATAL problem.
			String msg = "Unable to process; missing structure definitions for Manifest v" + MAN_VER;
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_MANIFEST, msg, curFile, logMsgSrcId);
			return;
		}

		JSONObject rqmtSet = structDefs.getJSONObject("StrucRqmts");
		Iterator<String> keys = rqmtSet.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject rqmtSpec = rqmtSet.getJSONObject(key);
			// NOTE: This block of code requires a 'targetPath' be defined
			if (rqmtSpec.has("targetPath")) {
				loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_MANIFEST, "Structure check; key= " + key, curFile,
						logMsgSrcId);
				curFileIsValid = structHelper.validateDocStructure(curRootEl, rqmtSpec) && curFileIsValid;
			}
		}

		return;
	}


	/**
	 * Return all <tt>ContainerLocations</tt> found in the last MDDF file processed.
	 * Associated with each location is a <tt>List</tt> of all <tt>Elements</tt>
	 * where the location was used. Thus, a single location may be referenced in
	 * multiple locations.
	 * 
	 * 
	 * @return the supportingRsrcLocations
	 */
	public Map<String, List<Element>> getSupportingRsrcLocations() {
		return supportingRsrcLocations;
	}

}
