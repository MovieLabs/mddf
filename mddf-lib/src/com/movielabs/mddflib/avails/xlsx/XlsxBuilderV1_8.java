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
package com.movielabs.mddflib.avails.xlsx;

import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;

import com.movielabs.mddflib.avails.xlsx.TemplateWorkBook;
import com.movielabs.mddflib.avails.xlsx.XlsxBuilder;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class XlsxBuilderV1_8 extends XlsxBuilder {

	public static final String DEFAULT_CONTEXT = "-default-";

	/**
	 * @param docRootEl
	 * @param xlsxVersion
	 * @param logger
	 */
	public XlsxBuilderV1_8(Element docRootEl, Version xlsxVersion, LogMgmt logger) {
		super(docRootEl, xlsxVersion, logger);
	}

	/**
	 * Prior to v1.8, Avails were sorted into two sets: Movies and TV. Each had it's
	 * own sheet in the workbook. Starting with v1.8, all Avails are entered on a
	 * single sheet.
	 */
	protected void process() {
		workbook = new TemplateWorkBook(logger);
		preProcess();
		/* No sorting or separation required for v1.8 */
		XPathExpression<Element> xpExp01 = xpfac.compile(".//" + rootPrefix + "Avail", Filters.element(), null,
				availsNSpace);
		List<Element> availList = xpExp01.evaluate(rootEl);
		addAvails("AllAvails", availList);
	}

	/**
	 * Pre-process XML to facilitate translation to XLSX. This involves adding
	 * temporary attributes to the XML. Since these do NOT conform with the XSD,
	 * they will be removed later.
	 * <p>
	 * Pre-processing for v1.8 is focused on processing Volumes. For each Volume we
	 * need to find all Assets with
	 * <ul>
	 * <li>WorkType of 'Episode' and with SeasonContentID that matches the Volume's
	 * SeasonContentID AND</li>
	 * <li>with a EpisodeNumber that matches the episode range for that Volume</li>
	 * </ul>
	 * Whenever we find a matching Episode we add a @volNumber attribute to the
	 * %lt;avails:Asset%gt; element
	 * </p>
	 */
	protected void preProcess() {
		/*
		 * NOTE to anyone defining an XPath: the prefix already has the ":" included
		 */
		String xpath_VolMD = "//" + availPrefix + "VolumeMetadata";
		String xpath_VolNum = "./" + availPrefix + "VolumeNumber/" + mdPrefix + "Number";
		String xpath_SeasonCID = "./" + availPrefix + "SeasonMetadata/" + availPrefix + "SeasonContentID";
		String xpath_EpisodeNum = "./" + availPrefix + "EpisodeMetadata/" + availPrefix + "EpisodeNumber/" + mdPrefix + "Number";
		XPathExpression<Element> xpExp_VolMetadata = xpfac.compile(xpath_VolMD, Filters.element(), null, availsNSpace);
		XPathExpression<Element> xpExp_VolNum = xpfac.compile(xpath_VolNum, Filters.element(), null, availsNSpace,
				mdNSpace);
		XPathExpression<Element> xpExp_scid = xpfac.compile(xpath_SeasonCID, Filters.element(), null, availsNSpace);
		XPathExpression<Element> xpExp_EpisodeNum = xpfac.compile(xpath_EpisodeNum, Filters.element(), null,
				availsNSpace, mdNSpace);
		/* find all Volumes... */
		List<Element> volList = xpExp_VolMetadata.evaluate(rootEl);
		if (volList.isEmpty()) {
			logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_DEBUG, null, "PreProcesor found no Volumes ", null, null,
					logMsgSrcId);
			return;
		}
		logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_INFO, null, "Pre-processing " + volList.size() + " Volume(s)",
				null, null, logMsgSrcId);
		for (Element volMdEl : volList) {
			/* get the Volume-specific info... */
			String scid = null;
			Element scidEl = xpExp_scid.evaluateFirst(volMdEl);
			if (scidEl != null) {
				scid = scidEl.getTextNormalize();
			}
			if ((scid == null) || (scid.isEmpty())) {
				logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_ERR, volMdEl,
						"Volume lacks SeasonContentID; Conversion will be incomplete ", null, null, logMsgSrcId);
				continue;
			}
			String volNum = null;
			Element vNumEl = xpExp_VolNum.evaluateFirst(volMdEl);
			if (scidEl != null) {
				volNum = vNumEl.getTextNormalize();
			}
			if ((volNum == null) || (volNum.isEmpty())) {
				logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_ERR, volMdEl,
						"Volume lacks SeasonContentID; Conversion will be incomplete ", null, null, logMsgSrcId);
				continue;
			}
			int firstEpisodeNum = Integer
					.parseInt(volMdEl.getChildTextNormalize("VolumeFirstEpisodeNumber", availsNSpace));
			int episodeCount = Integer.parseInt(volMdEl.getChildTextNormalize("VolumeNumberOfEpisodes", availsNSpace));
			int lastEpisodeNum = firstEpisodeNum + episodeCount - 1;
			/*
			 * Now find all matching Assets. This is 2-step process. First get all Episodes
			 * that are for the same Season as the Volume, then check if the Episode number
			 * is in-range.
			 */
			String xpath_Episodes = "//" + availPrefix + "Asset[./" + availPrefix + "WorkType/text()='Episode' and ./"
					+ availPrefix + "EpisodeMetadata/" + availPrefix + "SeasonMetadata[" + availPrefix
					+ "SeasonContentID/text()='" + scid + "']]";
			XPathExpression<Element> xpExp_episodes = xpfac.compile(xpath_Episodes, Filters.element(), null,
					availsNSpace);
			List<Element> episodesForSeasonList = xpExp_episodes.evaluate(rootEl);
			for (Element assetEl : episodesForSeasonList) {
				Element epNumEl = xpExp_EpisodeNum.evaluateFirst(assetEl);
				int episodeNum = Integer.parseInt(epNumEl.getTextNormalize());
				if ((episodeNum >= firstEpisodeNum) && (episodeNum <= lastEpisodeNum)) {
					assetEl.setAttribute("volNumber", volNum);
				}
			}
		}
	}

	protected Map<String, String> extractData(Element baseEl, Map<String, List<XPathExpression>> categoryMappings,
			String workType) {
		switch (workType) {
		case "Episode":
		case "Season":
		case "Series":
		case "Volume":
			break;
		default:
			workType = DEFAULT_CONTEXT;
		}
		return super.extractData(baseEl, categoryMappings, workType);
	}
}
