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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.manifest.validation.ManifestValidator.XrefCounter;
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
public class ManifestValidator extends XmlIngester {

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
	private static HashMap<String, String> id2typeMap;
	private static JSONObject cmVocab;

	protected boolean validateC;
	protected Element curRootEl;
	protected boolean curFileIsValid;
	protected Map<String, HashSet<String>> idSets;
	protected Map<String, Map<String, XrefCounter>> idXRefCounts;
	protected Map<String, Map<String, Element>> id2XmlMappings;

	static {
		id2typeMap = new HashMap<String, String>();
		id2typeMap.put("AudioTrackID", "audtrackid");
		id2typeMap.put("VideoTrackID", "vidtrackid");
		id2typeMap.put("SubtitleTrackID", "subtrackid");
		id2typeMap.put("InteractiveTrackID", "interactiveid");
		id2typeMap.put("ProductID", "alid");
		id2typeMap.put("ContentID", "cid");
 
		try {
			cmVocab = loadVocab(cmVrcPath, "CM");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
 

	/**
	 * @param validateC
	 */
	public ManifestValidator(boolean validateC, LogMgmt loggingMgr) {
		super(loggingMgr);
		this.validateC = validateC;
		logMsgSrcId = LOGMSG_ID;
		logMsgDefaultTag = LogMgmt.TAG_MANIFEST;
	}

	public boolean process(File xmlManifestFile) throws IOException, JDOMException {
		curFile = xmlManifestFile;
		curFileName = xmlManifestFile.getName();
		curFileIsValid = true;
		validateXml(xmlManifestFile);
		if (!curFileIsValid) {
			String msg = "Schema validation check FAILED";
			loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MANIFEST, msg, curFile, logMsgSrcId);
			return false;
		}
		String msg = "Schema validation check PASSED";
		loggingMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MANIFEST, msg, curFile, logMsgSrcId);
		curRootEl = getAsXml(xmlManifestFile);
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
	protected boolean validateXml(File manifestFile) {
		String manifestXsdFile = "./resources/manifest-v" + XmlIngester.MAN_VER + ".xsd";
		curFileIsValid = validateXml(manifestFile, manifestXsdFile, logMsgSrcId);
		return curFileIsValid;
	}

	/**
	 * Validate everything that is not fully specified via the XSD.
	 */
	protected void validateConstraints() {
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_MANIFEST, "Validating constraints", curFile, LOGMSG_ID);
		idSets = new HashMap<String, HashSet<String>>();
		idXRefCounts = new HashMap<String, Map<String, XrefCounter>>();
		id2XmlMappings = new HashMap<String, Map<String, Element>>();

		// TODO: Load from JSON file....
		/*
		 * Check ID for any Inventory components....
		 */
		validateId("Audio", "AudioTrackID", true);
		validateId("Video", "VideoTrackID", true);
		validateId("Image", "ImageID", true);
		validateId("Subtitle", "SubtitleTrackID", true);
		validateId("Interactive", "InteractiveTrackID", true);
		validateId("Ancillary", "AncillaryTrackID", true);
		validateId("TextObject", "TextObjectID", true);
		validateId("Metadata", "ContentID", true);
		// validateId("BasicMetadata", "ContentID");

		/*
		 * Check ID for everything else...
		 */
		validateId("PictureGroup", "PictureGroupID", true);
		validateId("TextGroup", "TextGroupID", true);
		validateId("AppGroup", "AppGroupID", true);
		validateId("Presentation", "PresentationID", true);
		validateId("PlayableSequence", "PlayableSequenceID", true);
		validateId("TimedEventSequence", "TimedSequenceID", true);
		validateId("Experience", "ExperienceID", true);
		validateId("Gallery", "GalleryID", false);

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
		validateVocab(manifestNSpace, "PictureGroup", manifestNSpace, "Type", allowed, srcRef);

		allowed = new JSONArray();
		String[] vs2 = { "Synchronous", "Pause", "Stop", "Substitution" };
		allowed.addAll(Arrays.asList(vs2));
		srcRef = LogReference.getRef("MMM", "1.5", "mmm002");
		// srcRef = "Section 7.2.3, TR-META-MMM (v1.5)";
		validateVocab(manifestNSpace, "TimedEvent", manifestNSpace, "Type", allowed, srcRef);

		allowed = new JSONArray();
		String[] vs3 = { "Main", "Promotion", "Bonus", "Other" };
		allowed.addAll(Arrays.asList(vs3));
		srcRef = LogReference.getRef("MMM", "1.5", "mmm003");
		// srcRef = "Section 8.3.1, TR-META-MMM (v1.5)";
		validateVocab(manifestNSpace, "AudioVisual", manifestNSpace, "Type", allowed, srcRef);

		validateCardinality();
	}

	/**
	 * 
	 */
	private void validateCardinality() {
		/*
		 * Sec 4.2.7: At least one instance of ContainerReference or
		 * BasicMetadata must be included for each Inventory/Metadata instance
		 */

	}

	/**
	 * @param elementName
	 * @param idxAttribute
	 * @param parentName
	 */
	protected void validateIndexing(String elementName, String idxAttribute, String parentName) {
		XPathExpression<Element> xpExpression = xpfac.compile(".//manifest:" + parentName, Filters.element(), null,
				manifestNSpace);
		List<Element> parentElList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < parentElList.size(); i++) {
			Element parentEl = (Element) parentElList.get(i);
			List<Element> childList = parentEl.getChildren(elementName, manifestNSpace);
			Boolean[] validIndex = new Boolean[childList.size()];
			Arrays.fill(validIndex, Boolean.FALSE);
			for (int cPtr = 0; cPtr < childList.size(); cPtr++) {
				Element nextChildEl = childList.get(cPtr);
				/*
				 * Each child Element must have @index attribute
				 */
				String indexAsString = nextChildEl.getAttributeValue(idxAttribute);
				if (!isValidIndex(indexAsString)) {
					String msg = "Invalid value for indexing attribute (non-negative integer required)";
					loggingMgr.logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, nextChildEl, msg, null, null, logMsgSrcId);
					curFileIsValid = false;
				} else {
					int index = Integer.parseInt(indexAsString);
					if (index >= validIndex.length) {
						String msg = "value for indexing attribute out of range";
						loggingMgr.logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, nextChildEl, msg, null, null,
								logMsgSrcId);

					} else if (validIndex[index]) {
						// duplicate value
						String msg = "Invalid value for indexing attribute (duplicate value)";
						loggingMgr.logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, nextChildEl, msg, null, null,
								logMsgSrcId);
						curFileIsValid = false;
					} else {
						validIndex[index] = true;
					}
				}
			}
			// now make sure all values were covered..
			boolean allValues = true;
			for (int j = 0; j < validIndex.length; j++) {
				allValues = allValues && validIndex[j];
			}
			if (!allValues) {
				String msg = "Invalid indexing of " + elementName + " sequence: be monotonically increasing";
				loggingMgr.logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, parentEl, msg, null, null, logMsgSrcId);
				curFileIsValid = false;
			}
		}
	}

	/**
	 * Check for the presence of an ID attribute and, if provided, verify it is
	 * unique.
	 * 
	 * @param elementName
	 * @param idAttribute
	 * @return the <tt>Set</tt> of unique IDs found.
	 */
	protected HashSet<String> validateId(String elementName, String idAttribute, boolean reqUniqueness) {
		XPathExpression<Element> xpExpression = xpfac.compile(".//manifest:" + elementName, Filters.element(), null,
				manifestNSpace);
		HashSet<String> idSet = new HashSet<String>();

		/*
		 * idXRefCounter is initialized here but will 'filled in' by
		 * validateXRef();
		 */
		HashMap<String, XrefCounter> idXRefCounter = new HashMap<String, XrefCounter>();
		/*
		 * id2XmlMap is provided for look-ups later as an alternative to using
		 * XPaths.
		 */
		HashMap<String, Element> id2XmlMap = new HashMap<String, Element>();

		List<Element> elementList = xpExpression.evaluate(curRootEl);
		for (int i = 0; i < elementList.size(); i++) {
			/*
			 * XSD may specify ID attribute as OPTIONAL but we need to verify
			 * cross-references and uniqueness.
			 */
			Element targetEl = (Element) elementList.get(i);
			String idValue = targetEl.getAttributeValue(idAttribute);
			id2XmlMap.put(idValue, targetEl);
			XrefCounter count = new XrefCounter(elementName, idValue);
			idXRefCounter.put(idValue, count);

			if ((idValue == null) || (idValue.isEmpty())) {
				String msg = idAttribute + " not specified. References to this " + elementName
						+ " will not be supportable.";
				loggingMgr.logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_WARN, targetEl, msg, null, null, logMsgSrcId);
			} else {
				if (!idSet.add(idValue)) {
					LogReference srcRef = LogReference.getRef("CM", "2.4", "cm001a");
					String msg = idAttribute + " is not unique";
					int msgLevel;
					if (reqUniqueness) {
						msgLevel = LogMgmt.LEV_ERR;
						curFileIsValid = false;
					} else {
						msgLevel = LogMgmt.LEV_WARN;
					}
					loggingMgr.logIssue(LogMgmt.TAG_MANIFEST, msgLevel, targetEl, msg, null, srcRef, logMsgSrcId);
				}
				/*
				 * Validate identifier structure conforms with Sec 2.1 of Common
				 * Metadata spec (v2.4)
				 */

				String idSyntaxPattern = "[\\S-[:]]+:[\\S-[:]]+:[\\S-[:]]+:[\\S]+$";
				if (!idValue.matches(idSyntaxPattern)) {
					String msg = "Invalid Identifier syntax";
					LogReference srcRef = LogReference.getRef("CM", "2.4", "cm001b");
					loggingMgr.logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_ERR, targetEl, msg, null, srcRef, logMsgSrcId);
					curFileIsValid = false;
				} else {
					String[] idParts = idValue.split(":");
					String idNid = idParts[0];
					String idType = idParts[1];
					String idScheme = idParts[2];
					String idSSID = idValue.split(":" + idScheme + ":")[1];
					validateIdScheme(idScheme, targetEl);
					validateIdSsid(idSSID, idScheme, targetEl);
					validateIdType(idType, idAttribute, targetEl);
				}
			}
		}
		idSets.put(elementName, idSet);
		id2XmlMappings.put(elementName, id2XmlMap);
		idXRefCounts.put(elementName, idXRefCounter);
		return idSet;
	}

	/**
	 * @param idType
	 * @param targetEl
	 */
	private void validateIdType(String idType, String idAttribute, Element targetEl) {
		/*
		 * Check syntax of the 'type' as defined in Best Practices Section 3.1.7
		 */
		String type = id2typeMap.get(idAttribute);
		if (type == null) {
			type = idAttribute.toLowerCase();
		}
		if (!idType.equals(type)) {
			LogReference srcRef = LogReference.getRef("MMM-BP", "1.0", "mmbp01.3");
			String msg = "ID <type> does not conform to recommendation (i.e. '" + type + "')";
			loggingMgr.logIssue(LogMgmt.TAG_BEST, LogMgmt.LEV_WARN, targetEl, msg, null, srcRef, logMsgSrcId);
		}

	}

	/**
	 * @param idSchema
	 * @param targetEl
	 */
	private void validateIdScheme(String idScheme, Element targetEl) {
		if (!idScheme.startsWith("eidr")) {
			String msg = "Use of EIDR identifiers is recommended";
			LogReference srcRef = LogReference.getRef("MMM-BP", "1.0", "mmbp01.1");
			loggingMgr.logIssue(LogMgmt.TAG_BEST, LogMgmt.LEV_WARN, targetEl, msg, null, srcRef, logMsgSrcId);
		}
	}

	private void validateIdSsid(String idSSID, String idScheme, Element targetEl) {
		String idPattern = null;
		LogReference srcRef = null;
		switch (idScheme) {
		case "eidr":
			srcRef = LogReference.getRef("MMM-BP", "1.0", "mmbp01.2");
			String msg = "Use of EIDR-x or EIDR-s identifiers is recommended";
			loggingMgr.logIssue(LogMgmt.TAG_BEST, LogMgmt.LEV_WARN, targetEl, msg, null, srcRef, logMsgSrcId);

			srcRef = null;
			idPattern = "10\\.[\\d]{4}/[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-Z]";
			break;
		case "eidr-s":
			srcRef = LogReference.getRef("EIDR-IDF", "1.3", "eidr01-s");
			idPattern = "[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-Z]";
			break;
		case "eidr-x":
			srcRef = LogReference.getRef("EIDR-IDF", "1.3", "eidr01-x");
			idPattern = "[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-F]{4}-[\\dA-Z]:[\\S]+";
			break;
		default:
			msg = "ID uses scheme '" + idScheme + "', SSID format will not be verified";
			loggingMgr.logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_INFO, targetEl, msg, "ssid='" + idSSID + "'", null,
					logMsgSrcId);
			return;
		}
		boolean match = idSSID.matches(idPattern);
		if (!match) {
			String msg = "Invalid SSID syntax for " + idScheme + " scheme";
			loggingMgr.logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_ERR, targetEl, msg, "ssid='" + idSSID + "'",
					srcRef, logMsgSrcId);
			curFileIsValid = false;
		}
	}

	/**
	 * @param srcElement
	 * @param xrefElement
	 * @param targetElType
	 */
	protected void validateXRef(String srcElement, String xrefElement, String targetElType) {
		HashSet<String> idSet = idSets.get(targetElType);
		Map<String, XrefCounter> idXRefCounter = idXRefCounts.get(targetElType);
		XPathExpression<Element> xpExpression = xpfac.compile(".//manifest:" + srcElement + "/manifest:" + xrefElement,
				Filters.element(), null, manifestNSpace);
		List<Element> elementList = xpExpression.evaluate(curRootEl);
		String debugMsg = "Found " + elementList.size() + " cross-refs by a " + srcElement + " to a " + targetElType;
		loggingMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_MANIFEST, debugMsg, curFile, LOGMSG_ID);
		for (int i = 0; i < elementList.size(); i++) {
			Element refEl = (Element) elementList.get(i);
			String targetId = refEl.getTextNormalize();
			if (!idSet.contains(targetId)) {
				String msg = "Invalid cross-reference: the referenced " + targetElType
						+ " is not defined in this manifest";
				loggingMgr.logIssue(LogMgmt.TAG_MANIFEST, LogMgmt.LEV_ERR, refEl, msg, null, null, logMsgSrcId);
				curFileIsValid = false;
			} else {
				XrefCounter count = idXRefCounter.get(targetId);
				if (count == null) {
					System.out.println("TRAPPED :" + targetId);
					count = new XrefCounter(targetElType, targetId);
					idXRefCounter.put(targetId, count);
				}
				count.increment();
			}
		}
	}

	/**
	 * 
	 */
	private void checkForOrphans() {
		// Map<String, Map<String, XrefCounter>> idXRefCounts;
		Iterator<Map<String, XrefCounter>> allCOunters = idXRefCounts.values().iterator();
		while (allCOunters.hasNext()) {
			// get the type-specific collection of counters...
			Map<String, XrefCounter> nextCSet = allCOunters.next();
			// now iterate thru the instance-specific counts..
			Iterator<XrefCounter> typeCounters = nextCSet.values().iterator();
			while (typeCounters.hasNext()) {
				XrefCounter nextCount = typeCounters.next();
				nextCount.validate();
			}
		}

	}

	/**
	 * @return
	 */
	protected boolean validateCMVocab() {
		boolean allOK = true;
		String mdVersion = "2.4";

		JSONArray allowed = cmVocab.optJSONArray("WorkType");
		LogReference srcRef = LogReference.getRef("CM", mdVersion, "cm002");
		allOK = allOK && validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "WorkType", allowed, srcRef);

		allowed = cmVocab.optJSONArray("ColorType");
		srcRef = LogReference.getRef("CM", mdVersion, "cm003");
		allOK = allOK && validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "PictureColorType", allowed, srcRef);

		allowed = cmVocab.optJSONArray("PictureFormat");
		srcRef = LogReference.getRef("CM", mdVersion, "cm004");
		allOK = allOK && validateVocab(manifestNSpace, "BasicMetadata", mdNSpace, "PictureFormat", allowed, srcRef);

		allowed = cmVocab.optJSONArray("ReleaseType");
		srcRef = LogReference.getRef("CM", mdVersion, "cm005");
		allOK = allOK && validateVocab(mdNSpace, "ReleaseHistory", mdNSpace, "ReleaseType", allowed, srcRef);

		allowed = cmVocab.optJSONArray("TitleAlternate@type");
		srcRef = LogReference.getRef("CM", mdVersion, "cm006");
		allOK = allOK && validateVocab(mdNSpace, "TitleAlternate", null, "@type", allowed, srcRef);

		allowed = cmVocab.optJSONArray("Parent@relationshipType");
		srcRef = LogReference.getRef("CM", mdVersion, "cm007");
		allOK = allOK && validateVocab(mdNSpace, "Parent", null, "@relationshipType", allowed, srcRef);

		allowed = cmVocab.optJSONArray("EntryClass");
		srcRef = LogReference.getRef("CM", mdVersion, "cm008");
		allOK = allOK && validateVocab(mdNSpace, "Entry", mdNSpace, "EntryClass", allowed, srcRef);

		// ====================================
		// TODO: DIGITAL ASSET METADATA

		return allOK;
	}

	/**
	 * @param primaryNS
	 * @param primaryEl
	 * @param childNS
	 * @param child
	 * @param expected
	 * @param srcRef
	 * @return
	 */
	protected boolean validateVocab(Namespace primaryNS, String primaryEl, Namespace childNS, String child,
			JSONArray expected, LogReference srcRef) {
		XPathExpression<Element> xpExpression = xpfac.compile(".//" + primaryNS.getPrefix() + ":" + primaryEl,
				Filters.element(), null, primaryNS);
		List<Element> elementList = xpExpression.evaluate(curRootEl);
		boolean allOK = true;
		int tag4log = LogMgmt.TAG_N_A;
		Namespace tagNS;
		if (childNS != null) {
			tagNS = childNS;
		} else {
			tagNS = primaryNS;
		}
		if (tagNS == manifestNSpace) {
			tag4log = LogMgmt.TAG_MANIFEST;
		} else if (tagNS == mdNSpace) {
			tag4log = LogMgmt.TAG_MD;
		}
		for (int i = 0; i < elementList.size(); i++) {
			Element targetEl = (Element) elementList.get(i);
			if (!child.startsWith("@")) {
				Element subElement = targetEl.getChild(child, childNS);
				if (subElement != null) {
					String text = subElement.getTextNormalize();
					if (!expected.contains(text)) {
						String explanation = "Values are case-sensitive";
						String msg = "Unrecognized value for " + primaryEl + "/" + child;
						// TODO: Is this ERROR or WARNING???
						loggingMgr.logIssue(tag4log, LogMgmt.LEV_ERR, subElement, msg, explanation, srcRef,
								logMsgSrcId);
						allOK = false;
						curFileIsValid = false;
					}
				}
			} else {
				String targetAttb = child.replaceFirst("@", "");
				String text = targetEl.getAttributeValue(targetAttb);
				if (!expected.contains(text)) {
					String explanation = "Values are case-sensitive";
					String msg = "Unrecognized value for attribute " + child;
					// TODO: Is this ERROR or WARNING???
					loggingMgr.logIssue(tag4log, LogMgmt.LEV_ERR, targetEl, msg, explanation, srcRef, logMsgSrcId);
					allOK = false;
					curFileIsValid = false;
				}
			}
		}
		return allOK;
	}
}
