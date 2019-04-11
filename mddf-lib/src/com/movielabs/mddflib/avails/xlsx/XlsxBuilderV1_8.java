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
		/* No sorting or separation required for v1.8 */
		XPathExpression<Element> xpExp01 = xpfac.compile(".//" + rootPrefix + "Avail", Filters.element(), null,
				availsNSpace);
		List<Element> availList = xpExp01.evaluate(rootEl);
		addAvails("AllAvails", availList);
	}

	protected Map<String, String> extractData(Element baseEl, Map<String, List<XPathExpression>> categoryMappings,
			String workType) {
		switch (workType) {
		case "Episode":
		case "Season":
		case "Series":
			break;
		default:
			workType = DEFAULT_CONTEXT;
		}
		return super.extractData(baseEl, categoryMappings, workType);
	}
}
