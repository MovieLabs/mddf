/**
 * Copyright (c) 2019 MovieLabs

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
package com.movielabs.mddflib.avails.xml.streaming;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;

import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.logging.LogMgmt;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class IngesterV1_7 extends StreamingRowIngester {

	/**
	 * @param row
	 * @param rowNum
	 * @param builder
	 * @param logger
	 */
	IngesterV1_7(String[] row, int rowNum, StreamingXmlBuilder builder, LogMgmt logger) {
		super(row, rowNum, builder, logger);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddflib.avails.xml.streaming.StreamingRowIngester#mDisposition(
	 * )
	 */
	@Override
	protected Element mDisposition() {
		Element disp = new Element("Disposition", builder.getAvailsNSpace());
		process(disp, "EntryType", builder.getAvailsNSpace(), "Disposition/EntryType");
		return disp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddflib.avails.xml.streaming.StreamingRowIngester#mPublisher(
	 * java.lang.String, java.lang.String)
	 */
	@Override
	protected Element mPublisher(String elName, String colKey) {
		Element pubEl = new Element(elName, builder.getAvailsNSpace());

		process(pubEl, "DisplayName", builder.getMdNSpace(), colKey);

		/*
		 * if ContactInfo is mandatory we can't get this info from the spreadsheet
		 */
		if (builder.isRequired("ContactInfo", "mdmec")) {
			Element e = new Element("ContactInfo", builder.getMdMecNSpace());
			Element e2 = new Element("Name", builder.getMdNSpace());
			e.addContent(e2);
			e2 = new Element("PrimaryEmail", builder.getMdNSpace());
			e.addContent(e2);
			pubEl.addContent(e);
		}
		return pubEl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddflib.avails.xml.streaming.StreamingRowIngester#buildAsset()
	 */
	@Override
	protected Element buildAsset() {
		Namespace availNS = builder.getAvailsNSpace();
		Element assetEl = new Element("Asset", availNS);
		Element wtEl = new Element("WorkType", availNS);
		wtEl.setText(workType);
		builder.addToPedigree(wtEl, workTypePedigree);
		assetEl.addContent(wtEl);
		/*
		 * Source key for 'contentID' depends (unfortunately) on the WorkType of the
		 * Asset.
		 */
		String colKey = locateContentID(workType);
		Pedigree pg = getPedigreedData(colKey);
		if (Pedigree.isSpecified(pg)) {
			String contentID = pg.getRawValue();
			Attribute attEl = new Attribute("contentID", contentID);
			assetEl.setAttribute(attEl);
			builder.addToPedigree(attEl, pg);
			builder.addToPedigree(assetEl, pg);
		}
		builder.createAssetMetadata(assetEl, workType, this);

		pg = getPedigreedData("Avail/BundledALIDs");
		if (Pedigree.isSpecified(pg)) {
			String[] alidList = pg.getRawValue().split(";");
			for (int i = 0; i < alidList.length; i++) {
				Element bAssetEl = new Element("BundledAsset", availNS);
				builder.addToPedigree(bAssetEl, pg);
				Element bAlidEl = new Element("BundledALID", availNS);
				bAlidEl.setText(alidList[i]);
				builder.addToPedigree(bAlidEl, pg);
				bAssetEl.addContent(bAlidEl);
				assetEl.addContent(bAssetEl);
			}
		}
		return assetEl;
	}

	protected String locateContentID(String workType) {
		String cidPrefix = "";
		switch (workType) {
		case "Series":
		case "Season":
		case "Episode":
			cidPrefix = workType;
			break;
		default:
		}
		String colKey = "AvailAsset/" + cidPrefix + "ContentID";
		return colKey;
	}
}
