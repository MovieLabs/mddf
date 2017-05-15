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
package com.movielabs.mddflib.cpe.itunes;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.located.LocatedElement;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.SAXParseException;

import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * Imports an iTunes Extras Package specification and converts it to a CPE
 * Manifest.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class Importer {

	private static final String moduleId = "iTunesImport";
	private static final int LOG_TAG = LogMgmt.TAG_PROFILE;
	public static final String profileVer = "ITE-1";
	public static final String ITE_NS_PREFIX = "itx";

	protected XPathFactory xpfac = XPathFactory.instance();
	private LogMgmt logger;
	private Namespace manifestNS;
	private Namespace mdNS;
	private String manifestVer;
	private Namespace iteNSpace;
	private File iteFile;
	private Document iteDoc;
	private Element mmInventoryEl;
	private Element mmPresentationsEl;
	private Element mmPlayableSequencesEl;
	private Element mmPictureGroupsEl;
	private Element mmExperiencesEl;
	private Map<String, Element> audioInv;
	private Map<String, Element> videoInv;
	private Map<String, Element> subtitleInv;

	public static enum CpeFileType {
		MANIFEST, STYLE, APP_DATA
	}

	public Importer(File iteFile, String cpeVersion, LogMgmt logger) throws SAXParseException, IOException {
		this.iteFile = iteFile;
		iteDoc = null;
		this.logger = logger;
		setMddfVersion(cpeVersion);
		if (manifestNS == null) {
			String msg = "Unsupported Manifest version; processing terminated";
			logger.log(LogMgmt.LEV_FATAL, LOG_TAG, msg, null, -1, moduleId, null, null);
			throw new IllegalArgumentException("Unsupported Manifest version " + cpeVersion);
		}
		this.manifestVer = cpeVersion;
		try {
			iteDoc = XmlIngester.getAsXml(iteFile);
		} catch (Exception e) {
			String msg = "Exception while trying to access iTunes file";
			String details = e.getMessage();
			logger.log(LogMgmt.LEV_FATAL, LOG_TAG, msg, iteFile, -1, moduleId, details, null);
			throw e;
		}
	}

	public Importer(Document iteDoc, String cpeVersion, LogMgmt logger) {
		this.iteFile = null;
		this.iteDoc = iteDoc;
		this.logger = logger;
		setMddfVersion(cpeVersion);
		if (manifestNS == null) {
			String msg = "Unsupported Manifest version; processing terminated";
			logger.log(LogMgmt.LEV_FATAL, LOG_TAG, msg, null, -1, null, null, null);
			throw new IllegalArgumentException("Unsupported Manifest version " + cpeVersion);
		}
		this.manifestVer = cpeVersion;
	}

	/**
	 * @param manifestVersion
	 */
	private void setMddfVersion(String manifestVer) {
		manifestNS = null;
		this.manifestVer = null;
		XmlIngester.setManifestVersion(manifestVer);
		manifestNS = XmlIngester.manifestNSpace;
		mdNS = XmlIngester.mdNSpace;
	}

	/**
	 * @param iteRootPackageEl
	 * @return
	 */
	private Map<CpeFileType, Document> initialize(Element iteRootPackageEl) {
		// need a Namespace for the iTunes doc
		Namespace nsUsed = iteRootPackageEl.getNamespace();
		String itePrefix = nsUsed.getPrefix();
		if (itePrefix.isEmpty()) {
			iteNSpace = Namespace.getNamespace(ITE_NS_PREFIX, nsUsed.getURI());
		} else {
			iteNSpace = nsUsed;
		}

		Map<CpeFileType, Document> results = new HashMap<CpeFileType, Document>();
		/* Initialize CPE-Manifest skeleton */
		Element rootManifestEl = new Element("MediaManifest", manifestNS);
		rootManifestEl.addNamespaceDeclaration(mdNS);
		Document manifestDoc = new Document(rootManifestEl);
		Element compEl = new Element("Compatibility", manifestNS);
		rootManifestEl.addContent(compEl);
		Element specVer = new Element("SpecVersion", manifestNS);
		specVer.setText(manifestVer);
		compEl.addContent(specVer);
		Element profileEl = new Element("Profile", manifestNS);
		profileEl.setText(profileVer);
		compEl.addContent(profileEl);
		// Add required stuff that gets filed in later (?)
		mmInventoryEl = new Element("Inventory", manifestNS);
		rootManifestEl.addContent(mmInventoryEl);
		audioInv = new HashMap<String, Element>();
		videoInv = new HashMap<String, Element>();
		subtitleInv = new HashMap<String, Element>();

		mmPresentationsEl = new Element("Presentations", manifestNS);
		rootManifestEl.addContent(mmPresentationsEl);

		mmPlayableSequencesEl = new Element("PlayableSequences", manifestNS);
		rootManifestEl.addContent(mmPlayableSequencesEl);

		mmPictureGroupsEl = new Element("PictureGroups", manifestNS);
		rootManifestEl.addContent(mmPictureGroupsEl);

		mmExperiencesEl = new Element("Experiences", manifestNS);
		rootManifestEl.addContent(mmExperiencesEl);

		results.put(CpeFileType.MANIFEST, manifestDoc);
		return results;
	}

	/**
	 * Convert an iTunes extra package into one or more MDDF CPE documents. The
	 * types of CPE docs that may be generated are indicated by the
	 * CpeFileTypes. Note that while a <tt>MANIFEST</tt> will always be included
	 * in the results, the <tt>STYLE</tt> and <tt>APP_DATA</tt> documents should
	 * be considered future enhancements for the time being.
	 * 
	 * @param iteDoc
	 * @param cpeVersion
	 *            version of MDDF Manifest XSD to be used
	 * @return results in the form of a Map&lt;CpeFileType, Document&gt;
	 */
	public Map<CpeFileType, Document> convert() {
		Element iteRootPackageEl = iteDoc.getRootElement();
		Map<CpeFileType, Document> results = initialize(iteRootPackageEl);
		/*
		 * retrieve the root element for the Manifest docs and start xfering
		 * data
		 * 
		 */
		Element mmRootEl = results.get(CpeFileType.MANIFEST).getRootElement();
		// Manifest ID:
		List<Element> vidElList = getItxElList(iteRootPackageEl, "/#package/#itunes_extra/#vendor_id", false);
		String manifestId = xlateId(vidElList.get(0), "manifestid");
		mmRootEl.setAttribute("ManifestID", manifestId);

		List<Element> rootnodeList = getItxElList(iteRootPackageEl, "/#package/#itunes_extra/#rootnodes/#rootnode",
				false);
		if (rootnodeList.isEmpty()) {
			String msg = "No <rootnode> elements found";
			logger.log(LogMgmt.LEV_ERR, LOG_TAG, msg, null, -1, moduleId, null, null);
			return results;
		}
		for (Element rootnodeEl : rootnodeList) {
			convertRootnode(rootnodeEl);
		}
		// Final Assembly:
		mmInventoryEl.addContent(audioInv.values());
		mmInventoryEl.addContent(videoInv.values());
		mmInventoryEl.addContent(subtitleInv.values());

		// .............................................
		return results;
	}

	/**
	 * @param rootnodeEl
	 * @param experiencesEl
	 */
	private void convertRootnode(Element rootnodeEl) {
		// for root-node the 'otherStuff is empty but still needs to be passed
		// in.
		Map<String, Element> otherStuff = new HashMap<String, Element>();
		Element experienceEl = addExperience(rootnodeEl, otherStuff, "root");

		List<Element> territoryList = getItxElList(rootnodeEl, "./#territories/#territory", false);
		for (Element iteTerrEl : territoryList) {
			Element regionEl = new Element("Region", manifestNS);
			experienceEl.addContent(regionEl);
			Element countryEl = new Element("country", mdNS);
			countryEl.setText(iteTerrEl.getTextNormalize());
			regionEl.addContent(countryEl);
		}
		Element rootNodeVID = rootnodeEl.getChild("vendor_id", iteNSpace);
		String cid = xlateId(rootNodeVID, "cid");
		Element cidEl = new Element("ContentID", manifestNS);
		cidEl.setText(cid);
		experienceEl.addContent(cidEl);

		/*
		 * The Audiovisual/PresentationID will come from the iTunes <navnode>
		 * with @type='play'
		 */
		List<Element> navnodeList = getItxElList(rootnodeEl, "./#navnodes/#navnode[./@type='play']", false);
		if (navnodeList.size() > 1) {
			String msg = "Multiple 'play' navnodes found";
			int line = resolveLine(rootnodeEl);
			logger.log(LogMgmt.LEV_ERR, LOG_TAG, msg, null, line, moduleId, null, null);
		}
		Element playNode = navnodeList.get(0);
		Element avEl = new Element("Audiovisual", manifestNS);
		avEl.setAttribute("ContentID", cid);
		experienceEl.addContent(avEl);

		Element avTypeEl = new Element("Type", manifestNS);
		avEl.addContent(avTypeEl);
		avTypeEl.setText("Main");

		Element pidEl = new Element("PresentationID", manifestNS);
		avEl.addContent(pidEl);
		Element presVIdEl = playNode.getChild("vendor_id", iteNSpace);
		String pid = xlateId(presVIdEl, "presentationid");
		pidEl.setText(pid);
		/* add all Elements required to implement the referenced Presentation */
		addPresentation(pid, presVIdEl);

		/*
		 * Adding AV elements to the Inventory is a different process when
		 * dealing with a root_node.
		 */

		addMainAudioVideo(rootnodeEl);

		navnodeList = getItxElList(rootnodeEl, "./#navnodes/#navnode", false);
		for (Element navnode : navnodeList) {
			// Handling is dependent on 'type'
			String navNodeType = navnode.getAttributeValue("type");
			switch (navNodeType) {
			case "play":
				// already processed so we can skip
				break;
			case "gallery":
				convertGallery(navnode, experienceEl);
				break;
			case "scenes":
				break;
			case "menu":
				break;
			case "cast":
				break;
			case "related":
				break;
			}
		}
	}

	/**
	 * Adds &lt;Audio&gt; and &lt;Video&gt; to the &lt;Inventory&gt; for the
	 * main feature. These are defined by a &lt;itx:root_node&gt; instance and
	 * are a special case in that there will not be any &lt;itx:Asset&gt;
	 * elements to provide a definition. Instead, the &lt;ContainerLocation&gt;
	 * is inferred using the root node's vendor-ID. This works because of the
	 * package structure mandated by Apple for iTunes delivery.
	 * 
	 * @param itxRootNodeEl
	 */
	private void addMainAudioVideo(Element itxRootNodeEl) {
		Element itxRootVendorIdEl = itxRootNodeEl.getChild("vendor_id", iteNSpace);
		String fileName = "./" + itxRootVendorIdEl.getText() + "-source.mov";
		/*
		 * Step 1: add Audio to Inventory
		 */
		Element audioEl = new Element("Audio", manifestNS);
		String aTrackId = xlateId(itxRootVendorIdEl, "audtrackid");
		audioEl.setAttribute("AudioTrackID", aTrackId);
		List<Element> itxLocales = getItxElList(itxRootNodeEl, "./#locales/#locale", true);

		for (Element itxLocale : itxLocales) {
			String lang = itxLocale.getAttributeValue("name");
			if (lang != null && !lang.isEmpty()) {
				Element langEl = new Element("Language", mdNS);
				langEl.setText(lang);
				audioEl.addContent(langEl);
			} else {
				int line = resolveLine(itxLocale);
				String msg = "<locale> element does not specify 'name' attribute";
				logger.log(LogMgmt.LEV_ERR, LOG_TAG, msg, null, line, moduleId, null, null);
			}
		}
		Element containerRefEl = new Element("ContainerReference", manifestNS);
		audioEl.addContent(containerRefEl);
		Element containerLocEl = new Element("ContainerLocation", manifestNS);
		containerRefEl.addContent(containerLocEl);
		containerLocEl.setText(fileName);
		audioInv.put(aTrackId, audioEl);

		/*
		 * Step 2: add Video
		 */
		Element videoEL = new Element("Audio", manifestNS);
		String vTrackId = xlateId(itxRootVendorIdEl, "vidtrackid");
		videoEL.setAttribute("VideoTrackID", vTrackId);
		containerRefEl = new Element("ContainerReference", manifestNS);
		videoEL.addContent(containerRefEl);
		containerLocEl = new Element("ContainerLocation", manifestNS);
		containerRefEl.addContent(containerLocEl);
		containerLocEl.setText(fileName);
		videoInv.put(vTrackId, videoEL);
	}

	/**
	 * Add a &lt;Presentation&gt; element along with the supporting
	 * &lt;Audio&gt; and &lt;Video&gt; elements. The later two go in the
	 * &lt;Inventory&gt; and are assigned ID values generated from the
	 * <tt>idSrcEl</tt>. This will be a &lt;itx:vendor_id&gt; from an
	 * &lt;itx:gallery_item&gt or &lt;itx:navnode&gt
	 * 
	 * @param pid
	 * @param idSrcEl
	 */
	private void addPresentation(String pid, Element idSrcEl) {
		/*
		 * Step 1: build the Presentation
		 */
		Element presEl = new Element("Presentation", manifestNS);
		presEl.setAttribute("PresentationID", pid);
		mmPresentationsEl.addContent(presEl);

		Element tmEl = new Element("TrackMetadata", manifestNS);
		presEl.addContent(tmEl);
		Element tsNumEl = new Element("TrackSelectionNumber", manifestNS);
		tsNumEl.setText("0");
		tmEl.addContent(tsNumEl);

		Element vidioTRefEl = new Element("VideoTrackReference", manifestNS);
		String vTrackId = xlateId(idSrcEl, "vidtrackid");
		vidioTRefEl.setText(vTrackId);
		tmEl.addContent(vidioTRefEl);

		Element audioTRefEl = new Element("AudioTrackReference", manifestNS);
		String aTrackId = xlateId(idSrcEl, "audtrackid");
		audioTRefEl.setText(aTrackId);
		tmEl.addContent(audioTRefEl);

		// TODO: SubtitleTrackReference

	}

	/**
	 * Convert &lt;navnode&gt; instances with type=“gallery”. The resultant
	 * <tt>&lt;manifest:Experience&gt;</tt> elements depends on whether the
	 * gallery has one item or multiple items. In either case, an Experience is
	 * created for each gallery_item. If there is one gallery_item, the
	 * Experience for this gallery_item is referenced by the parent node
	 * (rootnode or navnode). If there is more than one gallery_item, an
	 * additional Experience is created for the grouping. This Experience is
	 * referenced by the parent node.
	 * 
	 * <p>
	 * Other constructs created while converting a Gallery are:
	 * <ul>
	 * <li>A <tt>PictureGroup</tt> is created for all images used by the
	 * Gallery.</li>
	 * </ul>
	 * 
	 * 
	 * @param itNavnode
	 * @param experienceEl
	 */
	private void convertGallery(Element itNavnode, Element mmParentExpEl) {
		// retrieve referenced <ite:gallery>
		Element gLinkEl = itNavnode.getChild("gallery_link", iteNSpace);
		String gallery_vid = gLinkEl.getAttributeValue("vendor_id");
		Element iteRootPackageEl = iteDoc.getRootElement();
		List<Element> galleryList = getItxElList(iteRootPackageEl,
				"/#package/#itunes_extra/#galleries/#gallery[./#vendor_id/text()='" + gallery_vid + "']", false);
		if (galleryList.size() > 1) {
			String msg = "Invalid ITE: Multiple <gallery> elements found with same ID";
			int line = resolveLine(itNavnode);
			logger.log(LogMgmt.LEV_ERR, LOG_TAG, msg, null, line, moduleId, null, null);
			return;
		} else if (galleryList.isEmpty()) {
			return;
		}
		/*
		 * OK to continue
		 */
		Element iteGalleryEl = galleryList.get(0);

		List<Element> itemList = getItxElList(iteGalleryEl, "./#gallery_items/#gallery_item", false);
		Map<String, Element> otherStuff = new HashMap<String, Element>();
		/* idTemplate will be used to create pictureIDs, imageIDs, etc. */
		String idTemplate = xlateId(iteGalleryEl.getChild("vendor_id", iteNSpace), "TBD");
		Element mmPicGrpEl = buildPictureGroup(itemList, idTemplate);
		if (mmPicGrpEl != null) {
			otherStuff.put("PictureGroup", mmPicGrpEl);
		}
		Element mmAvEl = buildAudioVisual(itemList, idTemplate);
		if (mmAvEl != null) {
			otherStuff.put("AudioVisual", mmAvEl);
		}

		if (itemList.size() > 1) {
			/*
			 * if multiple items we need to create an Experience for the gallery
			 * AND then add Experiences for each gallery_item
			 */
			Element mmGalleryExpEl = addExperience(iteGalleryEl, otherStuff, "gallery?");
			addExperienceChild(mmParentExpEl, iteGalleryEl);
			for (Element iteGItemEl : itemList) {
				addExperience(iteGItemEl, otherStuff, "gallery_item?");
				addExperienceChild(mmGalleryExpEl, iteGItemEl);
			}

		} else if (itemList.size() == 1) {
			/* Add an Experience for the single gallery_item */
			addExperience(itemList.get(0), otherStuff, "gallery_item?");
			addExperienceChild(mmParentExpEl, itemList.get(0));
		} else {
			// Bad ITE file ??
			String msg = "Invalid ITE: Empty <gallery> without any <gallery_items>";
			int line = resolveLine(iteGalleryEl);
			logger.log(LogMgmt.LEV_ERR, LOG_TAG, msg, null, line, moduleId, null, null);
			return;
		}
		for (Element itxItem : itemList) {
			Element itxVendorIdEl = getItxEl(itxItem, "./#vendor_id", false);
			List<Element> assetList = getItxElList(itxItem, "./#assets/#asset", false);
			for (Element itxAssetEl : assetList) {
				addAVInventory(itxAssetEl, itxVendorIdEl);
			}
		}
	}

	/**
	 * Adds &lt;Audio&gt; and &lt;Video&gt; to the &lt;Inventory&gt; for
	 * anything that has a &lt;itx:Asset&gt; element to provide a definition.
	 * 
	 * @param itxAssetEl
	 * @param vIdEl
	 */
	private void addAVInventory(Element itxAssetEl, Element vIdEl) {
		String assetType = itxAssetEl.getAttributeValue("type");
		if (assetType == null) {
			String msg = "Invalid ITE: <asset> does not specify 'type' ";
			int line = resolveLine(itxAssetEl);
			logger.log(LogMgmt.LEV_ERR, LOG_TAG, msg, null, line, moduleId, null, null);
			return;
		} else if (!assetType.equals("full")) {
			// ignore
			return;
		}
		/*
		 * First we do AUDIO
		 * 
		 */
		String aTrackId = xlateId(vIdEl, "audtrackid");
		Element audioEl = new Element("Audio", manifestNS);
		audioEl.setAttribute("AudioTrackID", aTrackId);

		// should be only 1
		Element itxAudioDataFileEl = getItxEl(itxAssetEl, "./#data_file[@role='audio']", false);
		if (itxAudioDataFileEl != null) {
			List<Element> itxLocales = getItxElList(itxAudioDataFileEl, "./#locale", false);
			for (Element itxLocale : itxLocales) {
				String lang = itxLocale.getAttributeValue("name");
				if (lang != null && !lang.isEmpty()) {
					Element langEl = new Element("Language", mdNS);
					langEl.setText(lang);
					audioEl.addContent(langEl);
				} else {
					int line = resolveLine(itxLocale);
					String msg = "<locale> element does not specify 'name' attribute";
					logger.log(LogMgmt.LEV_ERR, LOG_TAG, msg, null, line, moduleId, null, null);
				}
			}
			Element containerRefEl = new Element("ContainerReference", manifestNS);
			audioEl.addContent(containerRefEl);

			Element containerLocEl = new Element("ContainerLocation", manifestNS);
			containerRefEl.addContent(containerLocEl);
			Element itxFName = getItxEl(itxAudioDataFileEl, "./#file_name", false);
			if (itxFName != null) {
				containerLocEl.setText("./" + itxFName.getText());
			}

			Element lengthEl = new Element("Length", manifestNS);
			containerRefEl.addContent(lengthEl);
			Element itxSizeEl = getItxEl(itxAudioDataFileEl, "./#size", false);
			if (itxSizeEl != null) {
				lengthEl.setText(itxSizeEl.getText());
			}

			Element hashEl = new Element("Hash", manifestNS);
			containerRefEl.addContent(hashEl);
			Element itxchecksumEl = getItxEl(itxAudioDataFileEl, "./#checksum", false);
			if (itxchecksumEl != null) {
				hashEl.setText(itxchecksumEl.getText());
				hashEl.setAttribute("method", itxchecksumEl.getAttributeValue("type"));
			}
		}
		audioInv.put(aTrackId, audioEl);

		/*
		 * Next we do VIDEO
		 * 
		 */
		String vidTrackId = xlateId(vIdEl, "vidtrackid");
		Element videoEl = new Element("Video", manifestNS);
		videoEl.setAttribute("VideoTrackID", vidTrackId);

		// should be only 1
		Element itxVidioDataFileEl = getItxEl(itxAssetEl, "./#data_file[@role='source']", false);
		if (itxVidioDataFileEl != null) {
			/*
			 * No Locales for Video
			 */
			Element containerRefEl = new Element("ContainerReference", manifestNS);
			videoEl.addContent(containerRefEl);

			Element containerLocEl = new Element("ContainerLocation", manifestNS);
			containerRefEl.addContent(containerLocEl);
			Element itxFName = getItxEl(itxVidioDataFileEl, "./#file_name", false);
			if (itxFName != null) {
				containerLocEl.setText(itxFName.getText());
			}

			Element lengthEl = new Element("Length", manifestNS);
			containerRefEl.addContent(lengthEl);
			Element itxSizeEl = getItxEl(itxVidioDataFileEl, "./#size", false);
			if (itxSizeEl != null) {
				lengthEl.setText(itxSizeEl.getText());
			}

			Element hashEl = new Element("Hash", manifestNS);
			containerRefEl.addContent(hashEl);
			Element itxchecksumEl = getItxEl(itxVidioDataFileEl, "./#checksum", false);
			if (itxchecksumEl != null) {
				hashEl.setText(itxchecksumEl.getText());
				hashEl.setAttribute("method", itxchecksumEl.getAttributeValue("type"));
			}
		}
		videoInv.put(vidTrackId, videoEl);
	}

	/**
	 * @param itemList
	 * @param idTemplate
	 * @return
	 */
	private Element buildAudioVisual(List<Element> itemList, String idTemplate) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Create a <tt>&lt;manifest:PictureGroup&gt;</tt> for all
	 * <tt>gallery_items</tt> with <tt>type="image"</tt>. A <tt>null</tt> value
	 * is returned if the <tt>iteItemList</tt> contains no images.
	 * 
	 * @param iteItemList
	 * @param idTemplate
	 *            a template for generating Manifest ID values
	 * @return a <tt>&lt;manifest:PictureGroup&gt;</tt> or <tt>null</tt>
	 */
	private Element buildPictureGroup(List<Element> iteItemList, String idTemplate) {
		Element mmPicGrpEl = new Element("PictureGroup", manifestNS);
		int picCnt = 0;
		for (Element itemEl : iteItemList) {
			String giType = itemEl.getAttributeValue("type");
			if (giType.equalsIgnoreCase("image")) {
				Element mmPicEl = new Element("Picture", manifestNS);
				String picID = idTemplate.replaceFirst("TBD", "pictureid") + "." + picCnt;
				Element mmPicIdEl = new Element("PictureID", manifestNS);
				mmPicIdEl.setText(picID);
				mmPicEl.addContent(mmPicIdEl);
				String imageID = idTemplate.replaceFirst("TBD", "imageid") + "." + picCnt;
				Element mmImageIdEl = new Element("ImageID", manifestNS);
				mmImageIdEl.setText(imageID);
				mmPicEl.addContent(mmImageIdEl);
				mmPicGrpEl.addContent(mmPicEl);
				picCnt++;
			}
		}
		if (picCnt > 0) {
			String picGrpID = idTemplate.replaceFirst("TBD", "picturegroupid");
			mmPicGrpEl.setAttribute("PictureGroupID", picGrpID);
			mmPictureGroupsEl.addContent(mmPicGrpEl);
			return mmPicGrpEl;
		} else {
			return null;
		}
	}

	private Element addExperience(Element iteEl, Map<String, Element> otherStuff, String expType) {
		String expId = xlateId(iteEl.getChild("vendor_id", iteNSpace), "experienceid");
		/*
		 * If an Experience with this ID already exists than we're done
		 */
		String xpath = "./#Experience[@ExperienceID='" + expId + "']";
		List<Element> eList = getManifestEl(mmExperiencesEl, xpath, true);
		if (!eList.isEmpty()) {
			return eList.get(0);
		}
		Element experienceEl = new Element("Experience", manifestNS);
		mmExperiencesEl.addContent(experienceEl);
		experienceEl.setAttribute("ExperienceID", expId);
		experienceEl.setAttribute("version", manifestVer);

		Element expTypeEl = new Element("Type", manifestNS);
		expTypeEl.setText(expType);
		experienceEl.addContent(expTypeEl);

		Element expSubTypeEl = new Element("SubType", manifestNS);
		expSubTypeEl.setText("ITE");
		experienceEl.addContent(expSubTypeEl);
		/*
		 * What happens next depends on the properties of the iteEl
		 */
		String iteElName = iteEl.getName();
		switch (iteElName) {
		case "rootnode":
			break;
		case "gallery":
			String cid = xlateId(iteEl.getChild("vendor_id", iteNSpace), "cid");
			System.out.println("iteElName=" + iteElName + ", cid=" + cid);
			Element cidEl = new Element("ContentID", manifestNS);
			cidEl.setText(cid);
			experienceEl.addContent(cidEl);

			// need type of gallery
			Element iteGTypeEl = iteEl.getChild("gallery_type", iteNSpace);
			String gType = iteGTypeEl.getTextNormalize();
			System.out.println("    gallery type=" + gType);
			switch (gType) {
			case "generic":
				Element picGrpEl = otherStuff.get("PictureGroup");
				if (picGrpEl != null) {
					String picGrpId = picGrpEl.getAttributeValue("PictureGroupID");
					Element pgIdEl = new Element("PictureGroupID", manifestNS);
					pgIdEl.setText(picGrpId);
					experienceEl.addContent(pgIdEl);
				}
				break;
			case "video":
				Element avEl = otherStuff.get("Audiovisual");
				if (avEl != null) {
					experienceEl.addContent(avEl);
				}
				// is there a PicGroup with artwork?
				picGrpEl = otherStuff.get("PictureGroup");
				if (picGrpEl != null) {
					String picGrpId = picGrpEl.getAttributeValue("PictureGroupID");
					Element pgIdEl = new Element("PictureGroupID", manifestNS);
					pgIdEl.setText(picGrpId);
					experienceEl.addContent(pgIdEl);
				}
				break;
			case "image":
				break;
			default:
				break;
			}
			break;
		case "gallery_item":
			// need type of gallery_item
			gType = iteEl.getAttributeValue("type");
			System.out.println("    gallery_item type=" + gType);
			break;
		default:
			break;
		}

		return experienceEl;
	}

	/**
	 * Add an <tt>ExperienceChild</tt> to the Manifest.
	 * 
	 * @param parentExpEl
	 *            an <tt>Experience</tt> element in the Manifest.
	 * @param iteNode
	 *            an element in the ITE providing the <tt>vendor_id</tt> of the
	 *            child.
	 */
	private void addExperienceChild(Element parentExpEl, Element iteNode) {
		Element expChildEl = new Element("ExperienceChild", manifestNS);
		parentExpEl.addContent(expChildEl);

		Element relEl = new Element("Relationship", manifestNS);
		relEl.setText("ispartof");
		expChildEl.addContent(relEl);

		String childExpId = xlateId(iteNode.getChild("vendor_id", iteNSpace), "experienceid");
		Element childExpIdEl = new Element("ExperienceID", manifestNS);
		childExpIdEl.setText(childExpId);
		expChildEl.addContent(childExpIdEl);
	}

	/**
	 * @param mmContextEl
	 * @param xpath
	 * @return
	 */
	private List<Element> getManifestEl(Element mmContextEl, String xpath, boolean optional) {
		String completeXPath = xpath.replaceAll("#", manifestNS.getPrefix() + ":");
		XPathExpression<Element> xpExpression = xpfac.compile(completeXPath, Filters.element(), null, manifestNS, mdNS);
		List<Element> mmElList = xpExpression.evaluate(mmContextEl);
		if (!optional && mmElList.isEmpty()) {
			int line = resolveLine(mmContextEl);
			String msg = "Missing required content";
			String details = "XPath '" + completeXPath + "' failed to return any matching elements";
			logger.log(LogMgmt.LEV_ERR, LOG_TAG, msg, iteFile, line, moduleId, null, null);
			System.out.println("       " + completeXPath);
		}
		return mmElList;
	}

	private Element getItxEl(Element iteContextel, String xpath, boolean optional) {
		List<Element> elList = getItxElList(iteContextel, xpath, optional);
		if (elList.isEmpty()) {
			return null;
		}
		if (elList.size() > 1) {
			int line = resolveLine(iteContextel);
			String msg = "Multiple Elements found where one expected";
			String details = "XPath '" + xpath + "' returned more than one matching elements. Extras will be ignored.";
			logger.log(LogMgmt.LEV_ERR, LOG_TAG, msg, iteFile, line, moduleId, null, null);
		}
		return elList.get(0);
	}

	/**
	 * @param iteContextel
	 * @param xpath
	 * @return
	 */
	private List<Element> getItxElList(Element iteContextel, String xpath, boolean optional) {
		String completeXPath = xpath.replaceAll("#", iteNSpace.getPrefix() + ":");
		XPathExpression<Element> xpExpression = xpfac.compile(completeXPath, Filters.element(), null, iteNSpace);
		List<Element> itExElList = xpExpression.evaluate(iteContextel);
		if (!optional && itExElList.isEmpty()) {
			int line = resolveLine(iteContextel);
			String msg = "Missing required content";
			String details = "XPath '" + completeXPath + "' failed to return any matching elements";
			logger.log(LogMgmt.LEV_ERR, LOG_TAG, msg, iteFile, line, moduleId, null, null);
			System.out.println("       " + completeXPath);
		}
		return itExElList;
	}

	private String xlateId(Element iteIdEl, String mdPrefix) {
		String iteId = iteIdEl.getTextTrim();
		String mdId = "md:" + mdPrefix + ":org:vendor_id.itunes.com:" + iteId;
		return mdId;
	}

	private int resolveLine(Element iteEl) {
		int line = -1;
		if (iteFile != null) {
			line = ((LocatedElement) iteEl).getLine();
		}
		return line;
	}

}
