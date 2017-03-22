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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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
import com.movielabs.mddflib.util.xml.SchemaWrapper;
import com.movielabs.mddflib.util.xml.XmlIngester;
import com.movielabs.mddflib.util.xml.XsdValidation;

/**
 * Validates a Manifest file as conforming to the Common Media Manifest (CMM) as
 * specified in <tt>TR-META-MMM (v1.5)</tt>. Validation also includes testing
 * for conformance with the <tt>Common Metadata (md)</tt> specification as
 * defined in <tt>TR-META-CM (v2.4)</tt>
 * 
 * @see <a href= "http://www.movielabs.com/md/manifest/v1.5/Manifest_v1.5.pdf">
 *      TR-META-MMM (v1.5)</a>
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class ManifestValidator extends CMValidator {

	/**
	 * Used to facilitate keeping track of cross-references and identifying
	 * 'orphan' elements.
	 * 
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	protected class XrefCounter {
		private int count = 0;
		private String elType;
		private String elId;

		/**
		 * @param elType
		 * @param elId
		 */
		XrefCounter(String elType, String elId) {
			super();
			this.elType = elType;
			this.elId = elId;
			// System.out.println("CONSTRUCT: "+elId+", cnt="+count);
		}

		int increment() {
			count++;
			return count;
		}

		void validate() {
			// System.out.println(" VALIDATE: "+elId+", cnt="+count);
			if (count > 0) {
				return;
			} else {
				Map<String, Element> idMap = id2XmlMappings.get(elType);
				Element targetEl = idMap.get(elId);
				String explanation = "The element is never referenced by it's ID";
				String msg = "Unreferenced <" + elType + "> Element";
				loggingMgr.logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_WARN, targetEl, msg, explanation, null,
						logMsgSrcId);
			}
		}

	}

	public static final String LOGMSG_ID = "ManifestValidator";

	static {
		id2typeMap = new HashMap<String, String>();
		id2typeMap.put("AudioTrackID", "audtrackid");
		id2typeMap.put("VideoTrackID", "vidtrackid");
		id2typeMap.put("SubtitleTrackID", "subtrackid");
		id2typeMap.put("InteractiveTrackID", "interactiveid");
		id2typeMap.put("ProductID", "alid");
		id2typeMap.put("ContentID", "cid");
	}

	/**
	 * @param validateC
	 */
	public ManifestValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;

		rootNS = manifestNSpace;

		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_MANIFEST;
	}

	public boolean process(Element docRootEl, File xmlManifestFile) throws IOException, JDOMException {
		curFile = xmlManifestFile;
		curFileName = xmlManifestFile.getName();
		curFileIsValid = true;
		curRootEl = null;

		String schemaVer = identifyXsdVersion(docRootEl);
		loggingMgr.log(LogMgmt.LEV_INFO, logMsgDefaultTag, "Validating using Schema Version " + schemaVer, srcFile,
				logMsgSrcId);
		setManifestVersion(schemaVer);
		rootNS = manifestNSpace;

		validateXml(xmlManifestFile, docRootEl);
		if (!curFileIsValid) {
			String msg = "Schema validation check FAILED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MANIFEST, msg, curFile, logMsgSrcId);
			return false;
		}
		curRootEl = docRootEl;
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
	 * @param manifestFile
	 */
	protected boolean validateXml(File srcFile, Element docRootEl) {
		String manifestXsdFile = XsdValidation.defaultRsrcLoc + "manifest-v" + XmlIngester.MAN_VER + ".xsd";
		curFileIsValid = xsdHelper.validateXml(srcFile, docRootEl, manifestXsdFile, logMsgSrcId);
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
		// validateId("BasicMetadata", "ContentID");

		/*
		 * Check ID for everything else...
		 */
		validateId("PictureGroup", "PictureGroupID", true, true);
		validateId("TextGroup", "TextGroupID", true, true);
		validateId("AppGroup", "AppGroupID", true, true);
		validateId("Presentation", "PresentationID", true, true);
		validateId("PlayableSequence", "PlayableSequenceID", true, true);
		validateId("TimedEventSequence", "TimedSequenceID", true, true);
		validateId("Experience", "ExperienceID", true, true);
		validateId("Gallery", "GalleryID", false, true);

		/* Now validate cross-references */
		validateXRef("Experience", "ContentID", "Metadata");
		validateXRef("Experience", "PictureGroupID", "PictureGroup");
		validateXRef("Experience", "TextGroupID", "TextGroup");
		validateXRef("Experience", "TimedSequenceID", "TimedEventSequence");
		validateXRef("ExperienceChild", "ExperienceID", "Experience");

		validateXRef("Gallery", "PictureGroupID", "PictureGroup");
		validateXRef("Gallery", "ContentID", "Metadata");

		validateXRef("Audiovisual", "ContentID", "Metadata");
		validateXRef("Audiovisual", "PresentationID", "Presentation");
		validateXRef("Audiovisual", "PlayableSequenceID", "PlayableSequence");

		validateXRef("Clip", "PresentationID", "Presentation");
		validateXRef("ImageClip", "ImageID", "Image");

		validateXRef("Chapter", "ImageID", "Image");

		validateXRef("Picture", "ImageID", "Image");
		validateXRef("Picture", "ThumbnailImageID", "Image");

		validateXRef("VideoTrackReference", "VideoTrackID", "Video");
		validateXRef("AudioTrackReference", "AudioTrackID", "Audio");
		validateXRef("AncillaryTrackReference", "AncillaryTrackID", "Ancillary");
		validateXRef("SubtitleTrackReference", "SubtitleTrackID", "Subtitle");

		validateXRef("TimedEventSequence", "PresentationID", "Presentation");
		validateXRef("TimedEventSequence", "PlayableSequenceID", "PlayableSequence");

		validateXRef("TimedEvent", "PresentationID", "Presentation");
		validateXRef("TimedEvent", "PlayableSequenceID", "PlayableSequence");
		validateXRef("TimedEvent", "ExperienceID", "Experience");
		validateXRef("TimedEvent", "GalleryID", "Gallery");
		validateXRef("TimedEvent", "AppGroupID", "AppGroup");
		validateXRef("TimedEvent", "TextGroupID", "TextGroup");

		validateXRef("ALIDExperienceMap", "ExperienceID", "Experience");

		checkForOrphans();

		/* Validate indexed sequences that must be monotonically increasing */
		validateIndexing("Chapter", "index", "Chapters");

		/*
		 * Validate the usage of controlled vocab (i.e., places where XSD
		 * specifies a xs:string but the documentation specifies an enumerated
		 * set of allowed values).
		 */
		// start with Common Metadata spec..
		validateCMVocab();

		// Now do any defined in Avails spec..
		validateManifestVocab();

		validateLocations();

		validateMetadata();

	}

	/**
	 * 
	 */
	private void validateManifestVocab() {

		JSONObject manifestVocab = (JSONObject) getMddfResource("manifest", MAN_VER);
		if (manifestVocab == null) {
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

		allowed = manifestVocab.optJSONArray("ExperienceType");
		srcRef = LogReference.getRef("CM", MAN_VER, "mmm_expType");
		validateVocab(manifestNSpace, "Experience", manifestNSpace, "Type", allowed, srcRef, true);

		allowed = manifestVocab.optJSONArray("AudiovisualType");
		srcRef = LogReference.getRef("MMM", MAN_VER, "mmm003");
		validateVocab(manifestNSpace, "Audiovisual", manifestNSpace, "Type", allowed, srcRef, true);

		allowed = manifestVocab.optJSONArray("ExperienceAppType");
		srcRef = LogReference.getRef("CM", MAN_VER, "mmm_expAppType");
		validateVocab(manifestNSpace, "Experience", manifestNSpace, "Type", allowed, srcRef, true);

		JSONObject availVocab = (JSONObject) getMddfResource("avail", AVAIL_VER);
		if (availVocab != null) {
			allowed = availVocab.optJSONArray("ExperienceCondition");
			srcRef = LogReference.getRef("CM", MD_VER, "cm007");
			validateVocab(manifestNSpace, "ExperienceID", null, "@condition", allowed, srcRef, true);
		}

	}

	/**
	 * @return
	 */
	protected void validateCMVocab() {
		JSONObject cmVocab = (JSONObject) getMddfResource("cm", MD_VER);
		if (cmVocab == null) {
			String msg = "Unable to validate controlled vocab: missing resource file";
			loggingMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_MANIFEST, msg, curFile, logMsgSrcId);
			curFileIsValid = false;
			return;
		}

		JSONArray allowed = cmVocab.optJSONArray("WorkType");
		LogReference srcRef = LogReference.getRef("CM", MD_VER, "cm002");
		validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "WorkType", allowed, srcRef, true);

		allowed = cmVocab.optJSONArray("ColorType");
		srcRef = LogReference.getRef("CM", MD_VER, "cm003");
		validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "PictureColorType", allowed, srcRef, true);

		allowed = cmVocab.optJSONArray("PictureFormat");
		srcRef = LogReference.getRef("CM", MD_VER, "cm004");
		validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "PictureFormat", allowed, srcRef, true);

		allowed = cmVocab.optJSONArray("ReleaseType");
		srcRef = LogReference.getRef("CM", MD_VER, "cm005");
		validateVocab(mdNSpace, "ReleaseHistory", mdNSpace, "ReleaseType", allowed, srcRef, true);

		allowed = cmVocab.optJSONArray("TitleAlternate@type");
		srcRef = LogReference.getRef("CM", MD_VER, "cm006");
		validateVocab(mdNSpace, "TitleAlternate", null, "@type", allowed, srcRef, true);

		allowed = cmVocab.optJSONArray("Parent@relationshipType");
		srcRef = LogReference.getRef("CM", MD_VER, "cm007");
		validateVocab(mdNSpace, "Parent", null, "@relationshipType", allowed, srcRef, true);

		allowed = cmVocab.optJSONArray("EntryClass");
		srcRef = LogReference.getRef("CM", MD_VER, "cm008");
		validateVocab(mdNSpace, "Entry", mdNSpace, "EntryClass", allowed, srcRef, true);

		allowed = cmVocab.optJSONArray("Parent@relationshipType");
		srcRef = LogReference.getRef("CM", MD_VER, "cm007");
		validateVocab(manifestNSpace, "ExperienceChild", manifestNSpace, "Relationship", allowed, srcRef, true);

		// ----------------------------------------
		/*
		 * Validate use of Country identifiers....
		 */
		// Usage in Common Metadata XSD...
		validateRegion(mdNSpace, "Region", mdNSpace, "country");
		validateRegion(mdNSpace, "DistrTerritory", mdNSpace, "country");
		validateRegion(mdNSpace, "CountryOfOrigin", mdNSpace, "country");

		// Usage in Manifest XSD....
		validateRegion(manifestNSpace, "Region", mdNSpace, "country");
		validateRegion(manifestNSpace, "RegionIncluded", mdNSpace, "country");
		validateRegion(manifestNSpace, "ExcludedRegion", mdNSpace, "country");

		// --------------- Validate language codes
		// ----------------------------------------
		/*
		 * Language codes in INVENTORY:
		 */
		validateLanguage(mdNSpace, "Language", null, null);
		validateLanguage(mdNSpace, "SubtitleLanguage", null, null);
		validateLanguage(mdNSpace, "SignedLanguage", null, null);
		validateLanguage(mdNSpace, "PrimarySpokenLanguage", null, null);
		validateLanguage(mdNSpace, "OriginalLanguage", null, null);
		validateLanguage(mdNSpace, "VersionLanguage", null, null);
		validateLanguage(mdNSpace, "LocalizedInfo", null, "@language");
		validateLanguage(mdNSpace, "JobDisplay", null, "@language");
		validateLanguage(mdNSpace, "DisplayName", null, "@language");
		validateLanguage(mdNSpace, "SortName", null, "@language");
		validateLanguage(mdNSpace, "TitleAlternate", null, "@language");
		validateLanguage(manifestNSpace, "TextObject", null, "@language");
		/*
		 * PRESENTATION:
		 */
		validateLanguage(manifestNSpace, "SystemLanguage", null, null);
		validateLanguage(manifestNSpace, "AudioLanguage", null, null);
		validateLanguage(manifestNSpace, "SubtitleLanguage", null, null);
		validateLanguage(manifestNSpace, "DisplayLabel", null, "@language");
		validateLanguage(manifestNSpace, "ImageID", null, "@language");
		/*
		 * PLAYABLE SEQ:
		 */
		validateLanguage(manifestNSpace, "Clip", null, "@audioLanguage");
		validateLanguage(manifestNSpace, "ImageClip", null, "@audioLanguage");
		/*
		 * PICTURE GROUPS:
		 */
		validateLanguage(manifestNSpace, "LanguageInImage", null, null);
		validateLanguage(manifestNSpace, "AlternateText", null, "@language");
		validateLanguage(manifestNSpace, "Caption", null, "@language");
		/*
		 * TEXT GROUP:
		 */
		validateLanguage(manifestNSpace, "TextGroup", null, "@language");
		/*
		 * EXPERIENCES:
		 */
		validateLanguage(manifestNSpace, "Language", null, null);
		validateLanguage(manifestNSpace, "ExcludedLanguage", null, null);
		validateLanguage(manifestNSpace, "AppName", null, "@language");
		validateLanguage(manifestNSpace, "GalleryName", null, "@language");

		validateRatings();

		// ====================================
		// TODO: DIGITAL ASSET METADATA

	}

	// ########################################################################

	/**
	 * Validate a local <tt>ContainerLocation</tt>. These should be a
	 * <i>relative</i> path where the base location is that of the XML file
	 * being processed. Note that network locations (i.e, full URLs) are
	 * <b>not</b> verified.
	 */
	protected void validateLocations() {
		String pre = manifestNSpace.getPrefix();
		String baseLoc = curFile.getAbsolutePath();
		LogReference srcRef = LogReference.getRef("MMM", "1.5", "mmm_locType");
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
			if (!PathUtilities.isRelative(containerPath)) {
				// We do not validate absolute or network-based URLs
				continue outterLoop;
			}
			try {
				String targetLoc = PathUtilities.convertToAbsolute(baseLoc, containerPath);
				File target = new File(targetLoc);
				if (!target.exists()) {
					String errMsg = "Referenced container not found";
					logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_WARN, clocEl, errMsg, null, null, logMsgSrcId);
					continue outterLoop;

				}
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
		 * The ContentID alias mechanism is filter for a peer BasicMetadata that
		 * results in a new BasicMetadata instance with a subset of the
		 * LocalizedInfo instance in the original BasicMetadata instance. Thus:
		 * <ul>
		 * <li>If the a Metatadata element contains a child Alias, it must also
		 * have as a child a BasicMetadata element for the Alias to filter. This
		 * may be identified either directly via a child BasicMetadata element
		 * or indirectly via a ContainerReference.</li>
		 * 
		 * <li>If the Alias has a LocalizedPair/LanguageIncluded = 'foobar' then
		 * the BasicMetadata element must have a child
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
}
