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
public class SeriesMetadata extends DefaultMetadata {

	/**
	 * @param xb
	 */
	public SeriesMetadata(XmlBuilder xb) {
		super(xb);
		colPrefix = "Series";
	}

	public void createAssetMetadata(Element parentEl, RowToXmlHelper row) {
		Element seriesMetadataEl = new Element(colPrefix + "Metadata", xb.getAvailsNSpace());
		String key = colPrefix + "ContentID";
		row.process(seriesMetadataEl, key, xb.getAvailsNSpace(), "AvailAsset/" + key);

		// SeriesEIDR-URN not supported by Excel a/o v1.7
		
		key = colPrefix + "TitleDisplayUnlimited";
		row.process(seriesMetadataEl, key, xb.getAvailsNSpace(), "AvailMetadata/" + key);
		
		key = colPrefix + "TitleInternalAlias";
		row.process(seriesMetadataEl, key, xb.getAvailsNSpace(), "AvailMetadata/" + key);

		key = colPrefix + "ID"; 
		addAltIdentifier(seriesMetadataEl, "SeriesAltIdentifier", "AvailMetadata/"+ key, row);

		row.process(seriesMetadataEl, "NumberOfSeasons", xb.getAvailsNSpace(), "AvailMetadata/SeasonCount");
	
		addCredits(seriesMetadataEl, row);		
		// ................
		
		
		// Attach generated Metadata node
		parentEl.addContent(seriesMetadataEl);
	}


}
