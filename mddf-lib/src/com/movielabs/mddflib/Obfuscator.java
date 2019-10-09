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
package com.movielabs.mddflib;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.movielabs.mddflib.logging.LogMgmt;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class Obfuscator {
	/**
	 * identifies a type of Avails field that may contain sensitive information.
	 * 
	 *
	 */
	public static enum DataTarget {
	Money("Term", "refered to as 'PriceValue' when formatted as Excel", ".//avails:Term/avails:Money"),
	ContractID("Transaction", "not supported in Excel formatted Avails", ".//avails:Transaction/avails:ContractID");

		private String qualifier;
		private String toolTip;
		String xpath;

		private DataTarget(String qualifier, String toolTip, String xpath) {
			this.qualifier = qualifier;
			this.toolTip = toolTip;
			this.xpath = xpath;
		}

		/**
		 * @return the toolTip
		 */
		public String getToolTip() {
			return toolTip;
		}

		public String toString() {
			return qualifier + "/" + this.name();
		}
	}

	private static Namespace availsNSpace;
	private static Namespace mdNSpace;
	private static XPathFactory xpfac = XPathFactory.instance();

	/**
	 * @param xmlDoc
	 * @param replacementMap
	 * @param logMgr
	 * @return a new Document with the indicated substitutions
	 */
	public static Document process(Document xmlDoc, Map<DataTarget, String> replacementMap, LogMgmt logMgr) {
		Map<DataTarget, XPathExpression<?>> xpeMap = initialize(xmlDoc);
		Document cleanXmlDoc = doReplacements(xmlDoc, xpeMap, replacementMap);
		return cleanXmlDoc;
	}

	/**
	 * @param xmlDoc
	 * @param xpeMap
	 * @param replacementMap
	 * @return
	 */
	private static Document doReplacements(Document originalDoc, Map<DataTarget, XPathExpression<?>> xpeMap,
			Map<DataTarget, String> replacementMap) {
		Document cleanDoc = originalDoc.clone();
		Element rootEl = cleanDoc.getRootElement();
		Iterator<DataTarget> tIt = replacementMap.keySet().iterator();
		while (tIt.hasNext()) {
			DataTarget nextKey = tIt.next();
			String replacementText = replacementMap.get(nextKey);
			if ((replacementText != null) && (!replacementText.isEmpty())) {
				XPathExpression<?> xpExpression = xpeMap.get(nextKey);
				List<?> replElList = xpExpression.evaluate(rootEl);
				for (int i = 0; i < replElList.size(); i++) {
					Object next = replElList.get(i);
					if (next instanceof Element) {
						Element nextEl = (Element) next;
						nextEl.setText(replacementText);
					} else if (next instanceof Attribute) {
						Attribute nextAtt = (Attribute) next;
						nextAtt.setValue(replacementText);
					}
				}
			}
		}
		return cleanDoc;
	}

	/**
	 * @param xmlDoc
	 */
	private static Map<DataTarget, XPathExpression<?>> initialize(Document xmlDoc) {
		List<Namespace> nsList = xmlDoc.getRootElement().getNamespacesInScope();
		for (int i = 0; i < nsList.size(); i++) {
			Namespace nextNS = nsList.get(i);
			System.out.println("Obfuscator: Found nspace=" + nextNS.getPrefix());
			switch (nextNS.getPrefix()) {
			case "avails":
				availsNSpace = nextNS;
				break;
			case "md":
				mdNSpace = nextNS;
				break;
			default:
				// ignore.
			}
		}
		Map<DataTarget, XPathExpression<?>> xpeMap = new HashMap<DataTarget, XPathExpression<?>>();
		EnumSet<DataTarget> targetSet = EnumSet.allOf(DataTarget.class);
		Iterator<DataTarget> tIt = targetSet.iterator();
		while (tIt.hasNext()) {
			DataTarget next = tIt.next();
			XPathExpression<Element> xpExpression = xpfac.compile(next.xpath, Filters.element(), null, availsNSpace,
					mdNSpace);
			xpeMap.put(next, xpExpression);
		}

		return xpeMap;
	}

}
