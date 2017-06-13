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
public class SeasonMetadata extends DefaultMetadata {

	private SeriesMetadata seriesMdBuilder;

	/**
	 * @param xb
	 */
	public SeasonMetadata(XmlBuilder xb) {
		super(xb);
		colPrefix = "Season";
		seriesMdBuilder = new SeriesMetadata(xb);
	}

	public void createAssetMetadata(Element parentEl, RowToXmlHelperV1_7 row) {
		Element seasonMetadataEl = new Element(colPrefix + "Metadata", xb.getAvailsNSpace());
		String key = colPrefix + "ContentID";
		row.process(seasonMetadataEl, key, xb.getAvailsNSpace(), "AvailAsset/" + key);

		// SeriesEIDR-URN not supported by Excel a/o v1.7

		key = colPrefix + "TitleDisplayUnlimited";
		row.process(seasonMetadataEl, key, xb.getAvailsNSpace(), "AvailMetadata/" + key);

		key = colPrefix + "TitleInternalAlias";
		row.process(seasonMetadataEl, key, xb.getAvailsNSpace(), "AvailMetadata/" + key);

		addSequenceInfo(row, seasonMetadataEl, colPrefix + "Number", "AvailMetadata/" + colPrefix + "Number");

		row.process(seasonMetadataEl, "ReleaseDate", xb.getAvailsNSpace(), "AvailMetadata/ReleaseYear");
		/*
		 * add a TEMPORARY holder for all ReleaseHistory elements. This will be
		 * removed later when the Metadata is FINALIZED.
		 */
		Element rhtEl = new Element("ReleaseHistoryTEMP");
		seasonMetadataEl.addContent(rhtEl);
		addReleaseHistory(seasonMetadataEl, "original", "AvailMetadata/ReleaseHistoryOriginal", row);
		addReleaseHistory(seasonMetadataEl, "DVD", "AvailMetadata/ReleaseHistoryPhysicalHV", row);

		addContentRating(seasonMetadataEl, row);

		addAltIdentifier(seasonMetadataEl, "SeasonAltIdentifier", "AvailMetadata/SeasonID", row);
		addAltIdentifier(seasonMetadataEl, "SeasonAltIdentifier", "AvailMetadata/SeasonAltID", row);

		row.process(seasonMetadataEl, "NumberOfEpisodes", xb.getAvailsNSpace(), "AvailMetadata/EpisodeCount");

		/*
		 * Last step is to add SeriesMetadata as child of the SeasonMetadata
		 * 
		 */
		seriesMdBuilder.createAssetMetadata(seasonMetadataEl, row);

		// ................
		// Attach generated Metadata node
		parentEl.addContent(seasonMetadataEl);
	}


}
