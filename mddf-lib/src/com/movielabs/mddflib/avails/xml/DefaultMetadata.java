/**
 * Copyright (c) 2016 MovieLabs

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
package com.movielabs.mddflib.avails.xml;

import org.jdom2.Element;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class DefaultMetadata {

	protected XmlBuilder xb;
	protected String colPrefix = "";

	DefaultMetadata(XmlBuilder xb) {
		this.xb = xb;
	}

	/**
	 * Construct Element instantiating the <tt>AvailMetadata-type</tt>. This
	 * method may be extended or overridden by sub-classes specific to a given
	 * work type (i.e., Episode, Season, etc.)
	 * 
	 * @param assetEl
	 * @return
	 */
	protected void createAssetMetadata(Element assetEl, RowToXmlHelper row) {
		Element metadata = new Element("Metadata", xb.getAvailsNSpace());
		addTitles(metadata, row);

		addEIDR(metadata, "EditEIDR-URN", "AvailAsset/EditID", row);
		addEIDR(metadata, "TitleEIDR-URN", "AvailAsset/TitleID", row);

		addStandardMetadata(metadata, row);

		addCredits(metadata, row);
		// Attach generated Metadata node
		assetEl.addContent(metadata);
	}

	protected void addEIDR(Element metadata, String childName, String colKey, RowToXmlHelper row) {
		Pedigree pg = row.getPedigreedData(colKey);
		if (!row.isSpecified(pg)) {
			return;
		}
		String idValue = pg.getRawValue();
		// what's the namespace (i.e., encoding fmt)?
		String namespace = parseIdFormat(idValue);
		if (namespace.equals("eider-5240")) {
			idValue = idValue.replaceFirst("10.5240/", "urn:eidr:10.5240:");
		}
		Element idEl = row.mGenericElement(childName, idValue, xb.getAvailsNSpace());
		metadata.addContent(idEl);
		xb.addToPedigree(idEl, pg);

	}

	/**
	 * Determine the format of an id string. Possible return values are:
	 * <ul>
	 * <li>eidr-URN</li>
	 * <li>eidr-5240</li>
	 * <li>MovieLabs (e.g. <tt>md:cid:org:mpm.craigsmovies.com:20938</tt>)</li>
	 * <li>user: anything else</li>
	 * </ul>
	 * 
	 * @param idValue
	 * @return
	 */
	protected String parseIdFormat(String idValue) {
		if (idValue.startsWith("urn:eidr:")) {
			return "eidr-URN";
		} else if (idValue.startsWith("10.5240/")) {
			return "eidr-5240";
		} else if (idValue.startsWith("md:")) {
			return "MovieLabs";
		} else {
			// Set it w/o change and let the Validation code will flag as error
			return "user";
		}
	}

	protected void addTitles(Element metadata, RowToXmlHelper row) {
		/*
		 * TitleDisplayUnlimited is OPTIONAL in SS but REQUIRED in XML;
		 * workaround by assigning it internal alias value which is REQUIRED in
		 * SS.
		 */
		Pedigree titleDuPg = row.getPedigreedData("AvailMetadata/" + colPrefix + "TitleDisplayUnlimited");
		Pedigree titleAliasPg = row.getPedigreedData("AvailMetadata/" + colPrefix + "TitleInternalAlias");
		if (!row.isSpecified(titleDuPg)) {
			titleDuPg = titleAliasPg;
		}
		Element e = row.mGenericElement("TitleDisplayUnlimited", titleDuPg.getRawValue(), xb.getAvailsNSpace());
		xb.addToPedigree(e, titleDuPg);
		metadata.addContent(e);

		// TitleInternalAlias
		if (xb.isRequired("TitleInternalAlias", "avails") || row.isSpecified(titleAliasPg)) {
			e = row.mGenericElement("TitleInternalAlias", titleAliasPg.getRawValue(), xb.getAvailsNSpace());
			metadata.addContent(e);
			xb.addToPedigree(e, titleAliasPg);
		}
	}

	protected void addStandardMetadata(Element metadata, RowToXmlHelper row) {

		row.process(metadata, "ReleaseDate", xb.getAvailsNSpace(), "AvailMetadata/ReleaseYear");
		row.process(metadata, "RunLength", xb.getAvailsNSpace(), "AvailMetadata/TotalRunTime");

		addReleaseHistory(metadata, "original", "AvailMetadata/ReleaseHistoryOriginal", row);
		addReleaseHistory(metadata, "DVD", "AvailMetadata/ReleaseHistoryPhysicalHV", row);

		row.process(metadata, "USACaptionsExemptionReason", xb.getAvailsNSpace(), "AvailMetadata/CaptionExemption");

		addContentRating(metadata, row);

		row.process(metadata, "EncodeID", xb.getAvailsNSpace(), "AvailAsset/EncodeID");
		row.process(metadata, "LocalizationOffering", xb.getAvailsNSpace(), "AvailMetadata/LocalizationType");

	}

	/**
	 * @param metadataEl
	 * @param childName
	 * @param colKey
	 */
	void addSequenceInfo(RowToXmlHelper row, Element metadataEl, String childName, String colKey) {
		Element childEl = new Element(childName, xb.getAvailsNSpace());
		Element numberEl = row.process(childEl, "Number", xb.getMdNSpace(), colKey);
		if (numberEl != null) {
			metadataEl.addContent(childEl);
		}
	}

	/**
	 * @param parentEl
	 * @param type
	 * @param cellKey
	 * @param row
	 */
	void addReleaseHistory(Element parentEl, String type, String cellKey, RowToXmlHelper row) {
		Pedigree pg = row.getPedigreedData(cellKey);
		if (!row.isSpecified(pg)) {
			return;
		}
		Element rh = new Element("ReleaseHistory", xb.getAvailsNSpace());
		Element rt = new Element("ReleaseType", xb.getMdNSpace());
		rt.setText(type);
		rh.addContent(rt);
		Element dateEl = row.mGenericElement("Date", pg.getRawValue(), xb.getMdNSpace());
		rh.addContent(dateEl);
		xb.addToPedigree(dateEl, pg);
		parentEl.addContent(rh);
	}

	protected void addContentRating(Element parentEl, RowToXmlHelper row) {
		String ratingSystem = row.getData("AvailMetadata/RatingSystem");
		String ratingValue = row.getData("AvailMetadata/RatingValue");
		/*
		 * According to XML schema, both values are REQUIRED for a Rating. If
		 * any has been specified than we add the Rating element and let XML
		 * validation worry about completeness,
		 */
		boolean add = row.isSpecified(ratingSystem) || row.isSpecified(ratingValue);
		if (!add) {
			return;
		}
		Element ratings = new Element("Ratings", xb.getAvailsNSpace());

		/*
		 * XML can support multiple Rating instances but Excel only allows 1
		 */
		Element rat = new Element("Rating", xb.getMdNSpace());
		ratings.addContent(rat);

		row.addRegion(rat, "Region", xb.getMdNSpace(), "AvailTrans/Territory");
		row.process(rat, "System", xb.getMdNSpace(), "AvailMetadata/RatingSystem");
		row.process(rat, "Value", xb.getMdNSpace(), "AvailMetadata/RatingValue");
		row.process(rat, "Reason", xb.getMdNSpace(), "AvailMetadata/RatingReason", ",");

		parentEl.addContent(ratings);
	}

	/**
	 * @param metadata
	 * @param row
	 */
	protected void addCredits(Element metadata, RowToXmlHelper row) {
		if (row.isSpecified(row.getData("AvailMetadata/CompanyDisplayCredit"))) {
			Element cdcEl = new Element("CompanyDisplayCredit", xb.getAvailsNSpace());
			row.process(cdcEl, "DisplayString", xb.getMdNSpace(), "AvailMetadata/CompanyDisplayCredit");
			metadata.addContent(cdcEl);
		}
	}

	/**
	 * @param parentEl
	 * @param childName
	 * @param key
	 * @param row
	 */
	protected void addAltIdentifier(Element parentEl, String childName, String key, RowToXmlHelper row) {
		Pedigree pg = row.getPedigreedData(key);
		if (!row.isSpecified(pg)) {
			return;
		}
		String idValue = pg.getRawValue();
		// what's the namespace?
		String namespace = "";
		if (idValue.startsWith("urn:eidr:")) {
			namespace = "eidr";
		} else if (idValue.startsWith("md:")) {
			namespace = "movielabs";
		} else {
			// ??? Not sure how to handle
			namespace = "user";
		}
		Element altIdEl = new Element(childName, xb.getAvailsNSpace());
		xb.addToPedigree(altIdEl, pg);

		Element nsEl = row.mGenericElement("Namespace", namespace, xb.getMdNSpace());
		altIdEl.addContent(nsEl);
		xb.addToPedigree(nsEl, pg);
		Element idEl = row.mGenericElement("Identifier", idValue, xb.getMdNSpace());
		altIdEl.addContent(idEl);
		xb.addToPedigree(idEl, pg);

		parentEl.addContent(altIdEl);

	}

}
