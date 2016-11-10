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
public class EpisodeMetadata extends DefaultMetadata {

	private SeasonMetadata seasonMdBuilder;

	/**
	 * @param xb
	 */
	EpisodeMetadata(XmlBuilder xb) {
		super(xb);
		colPrefix = "Episode";
		seasonMdBuilder = new SeasonMetadata(xb);
	}

	protected void createAssetMetadata(Element parentEl, RowToXmlHelper row) {
		Element episodeMetadataEl = new Element(colPrefix + "Metadata", xb.getAvailsNSpace());
		addTitles(episodeMetadataEl, row);

		addIdentifier(episodeMetadataEl, row);

		addStandardMetadata(episodeMetadataEl, row);

		addSequenceInfo(row, episodeMetadataEl, colPrefix + "Number", "AvailMetadata/" + colPrefix + "Number");
		/*
		 * Last step is to add SeasonMetadata as child of the EpisodeMetadata
		 * 
		 */
		seasonMdBuilder.createAssetMetadata(episodeMetadataEl, row);
		// Attach generated Metadata node
		parentEl.addContent(episodeMetadataEl);
	}

	/**
	 * @param episodeMetadataEl
	 * @param string
	 * @param string2
	 * @param row
	 */
	private void addIdentifier(Element episodeMetadataEl, RowToXmlHelper row) {
		Pedigree pg = row.getPedigreedData("AvailMetadata/EpisodeID");
		if (!row.isSpecified(pg)) {
			return;
		}
		String idValue = pg.getRawValue();
		// what's the namespace (i.e., encoding fmt)?
		String namespace = parseIdFormat(idValue);
		if (namespace.equals("eider-5240")) {
			idValue = idValue.replaceFirst("10.5240/", "urn:eidr:10.5240:");
		}
		switch (namespace) {
		case "eidr-5240":
			idValue = idValue.replaceFirst("10.5240/", "urn:eidr:10.5240:");
		case "eidr-URN":
			// create an EditEIDR-URN
			Element idEl = row.mGenericElement("EditEIDR-URN", idValue, xb.getAvailsNSpace());
			episodeMetadataEl.addContent(idEl);
			xb.addToPedigree(idEl, pg);
			break;
		default:
			// create an AltID
			Element altIdEl = new Element("AltIdentifier", xb.getAvailsNSpace());
			xb.addToPedigree(altIdEl, pg);
			Element nsEl = row.mGenericElement("Namespace", namespace, xb.getMdNSpace());
			altIdEl.addContent(nsEl);
			xb.addToPedigree(nsEl, pg);
			idEl = row.mGenericElement("Identifier", idValue, xb.getMdNSpace());
			altIdEl.addContent(idEl);
			xb.addToPedigree(idEl, pg);
			episodeMetadataEl.addContent(altIdEl);
		}
	}
}
