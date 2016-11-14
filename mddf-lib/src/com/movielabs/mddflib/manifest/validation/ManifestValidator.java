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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.AbstractValidator;
import com.movielabs.mddflib.util.xml.SchemaWrapper;
import com.movielabs.mddflib.util.xml.XmlIngester;

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
public class ManifestValidator extends AbstractValidator {

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

		/*
		 * Is there a controlled vocab that is specific to a Manifest? Note the
		 * vocab set for validating Common Metadata will be loaded by the parent
		 * class AbstractValidator.
		 */

	}

	/**
	 * @param validateC
	 */
	public ManifestValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;

		rootNS = manifestNSpace;
		rootPrefix = "manifest:";

		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_MANIFEST;
	}

	public boolean process(Element docRootEl, File xmlManifestFile) throws IOException, JDOMException {
		curFile = xmlManifestFile;
		curFileName = xmlManifestFile.getName();
		curFileIsValid = true;
		curRootEl = null;  
		validateXml(xmlManifestFile,docRootEl);
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
		String manifestXsdFile = "./resources/manifest-v" + XmlIngester.MAN_VER + ".xsd";
		curFileIsValid = validateXml(srcFile, docRootEl, manifestXsdFile, logMsgSrcId);
		return curFileIsValid;
	}

	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateConstraints() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_MANIFEST, "Validating constraints", curFile, LOGMSG_ID);
		super.validateConstraints();

		SchemaWrapper availSchema = SchemaWrapper.factory("manifest-v" + XmlIngester.MAN_VER);
		List<String> reqElList = availSchema.getReqElList();
		for (int i = 0; i < reqElList.size(); i++) {
			String key = reqElList.get(i);
			validateNotEmpty(key);
		}
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
		boolean cmOk = validateCMVocab();

		// Now do any defined in Media Manifestspec..
		JSONArray allowed = new JSONArray();
		String[] vs1 = { "Retail", "App", "Gallery" };
		allowed.addAll(Arrays.asList(vs1));
		LogReference srcRef = LogReference.getRef("MMM", "1.5", "mmm001");
		// srcRef = "Section 6.2, TR-META-MMM (v1.5)";
		validateVocab(manifestNSpace, "PictureGroup", manifestNSpace, "Type", allowed, srcRef, true);

		allowed = new JSONArray();
		String[] vs2 = { "Synchronous", "Pause", "Stop", "Substitution" };
		allowed.addAll(Arrays.asList(vs2));
		srcRef = LogReference.getRef("MMM", "1.5", "mmm002");
		// srcRef = "Section 7.2.3, TR-META-MMM (v1.5)";
		validateVocab(manifestNSpace, "TimedEvent", manifestNSpace, "Type", allowed, srcRef, true);

		allowed = new JSONArray();
		String[] vs3 = { "Main", "Promotion", "Bonus", "Other" };
		allowed.addAll(Arrays.asList(vs3));
		srcRef = LogReference.getRef("MMM", "1.5", "mmm003");
		// srcRef = "Section 8.3.1, TR-META-MMM (v1.5)";
		validateVocab(manifestNSpace, "AudioVisual", manifestNSpace, "Type", allowed, srcRef, true);

	}


	/**
	 * @return
	 */
	protected boolean validateCMVocab() {
		boolean allOK = true;
		String mdVersion = "2.4";

		JSONArray allowed = cmVocab.optJSONArray("WorkType");
		LogReference srcRef = LogReference.getRef("CM", mdVersion, "cm002");
		allOK = validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "WorkType", allowed, srcRef, true) && allOK;

		allowed = cmVocab.optJSONArray("ColorType");
		srcRef = LogReference.getRef("CM", mdVersion, "cm003");
		allOK =  validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "PictureColorType", allowed, srcRef, true)
				&& allOK;

		allowed = cmVocab.optJSONArray("PictureFormat");
		srcRef = LogReference.getRef("CM", mdVersion, "cm004");
		allOK =  validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "PictureFormat", allowed, srcRef, true)
				&& allOK;

		allowed = cmVocab.optJSONArray("ReleaseType");
		srcRef = LogReference.getRef("CM", mdVersion, "cm005");
		allOK = validateVocab(mdNSpace, "ReleaseHistory", mdNSpace, "ReleaseType", allowed, srcRef, true) && allOK;

		allowed = cmVocab.optJSONArray("TitleAlternate@type");
		srcRef = LogReference.getRef("CM", mdVersion, "cm006");
		allOK = validateVocab(mdNSpace, "TitleAlternate", null, "@type", allowed, srcRef, true) && allOK;

		allowed = cmVocab.optJSONArray("Parent@relationshipType");
		srcRef = LogReference.getRef("CM", mdVersion, "cm007");
		allOK = validateVocab(mdNSpace, "Parent", null, "@relationshipType", allowed, srcRef, true) && allOK;

		allowed = cmVocab.optJSONArray("EntryClass");
		srcRef = LogReference.getRef("CM", mdVersion, "cm008");
		allOK = validateVocab(mdNSpace, "Entry", mdNSpace, "EntryClass", allowed, srcRef, true) && allOK;
		
		// --------------- Validate language codes ----------------------------------------
		/*
		 * Language codes in INVENTORY:
		 */
		allOK = validateLanguage(mdNSpace, "Language", null, null) && allOK;
		allOK = validateLanguage(mdNSpace, "SubtitleLanguage", null, null) && allOK;
		allOK = validateLanguage(mdNSpace, "SignedLanguage", null, null) && allOK;
		allOK = validateLanguage(mdNSpace, "PrimarySpokenLanguage", null, null) && allOK;
		allOK = validateLanguage(mdNSpace, "OriginalLanguage", null, null) && allOK;
		allOK = validateLanguage(mdNSpace, "VersionLanguage", null, null) && allOK;
		allOK = validateLanguage(mdNSpace, "LocalizedInfo", null, "@language") && allOK;
		allOK = validateLanguage(mdNSpace, "JobDisplay", null, "@language") && allOK;
		allOK = validateLanguage(mdNSpace, "DisplayName", null, "@language") && allOK;
		allOK = validateLanguage(mdNSpace, "SortName", null, "@language") && allOK;
		allOK = validateLanguage(mdNSpace, "TitleAlternate", null, "@language") && allOK;
		allOK = validateLanguage(manifestNSpace, "TextObject", null, "@language") && allOK;
		/*
		 * PRESENTATION:
		 */
		allOK = validateLanguage(manifestNSpace, "SystemLanguage", null, null) && allOK;
		allOK = validateLanguage(manifestNSpace, "AudioLanguage", null, null) && allOK;
		allOK = validateLanguage(manifestNSpace, "SubtitleLanguage", null, null) && allOK;
		allOK = validateLanguage(manifestNSpace, "DisplayLabel", null, "@language") && allOK;
		allOK = validateLanguage(manifestNSpace, "ImageID", null, "@language") && allOK;
		/*
		 * PLAYABLE SEQ:
		 */
		allOK = validateLanguage(manifestNSpace, "Clip", null, "@audioLanguage") && allOK;
		allOK = validateLanguage(manifestNSpace, "ImageClip", null, "@audioLanguage") && allOK;
		/*
		 * PICTURE GROUPS:
		 */
		allOK = validateLanguage(manifestNSpace, "LanguageInImage", null, null) && allOK;
		allOK = validateLanguage(manifestNSpace, "AlternateText", null, "@language") && allOK;
		allOK = validateLanguage(manifestNSpace, "Caption", null, "@language") && allOK;
		/*
		 * TEXT GROUP:
		 */
		allOK = validateLanguage(manifestNSpace, "TextGroup", null, "@language") && allOK;
		/*
		 * EXPERIENCES:
		 */
		allOK = validateLanguage(manifestNSpace, "Language", null, null) && allOK;
		allOK = validateLanguage(manifestNSpace, "ExcludedLanguage", null, null) && allOK;
		allOK = validateLanguage(manifestNSpace, "AppName", null, "@language") && allOK;
		allOK = validateLanguage(manifestNSpace, "GalleryName", null, "@language") && allOK;
		

		validateRatings();
		
		// ====================================
		// TODO: DIGITAL ASSET METADATA

		return allOK;
	}
}
