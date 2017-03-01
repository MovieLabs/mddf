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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private static Namespace availsNSpace;
	private static Namespace mdNSpace;
	private static XPathFactory xpfac = XPathFactory.instance();

	public static Document process(Document xmlDoc, Map<String, String> replacementMap, LogMgmt logMgr) {
		Map<String, XPathExpression<?>> xpeMap = initialize(xmlDoc);
		Document cleanXxmlDoc = doReplacements(xmlDoc, xpeMap, replacementMap);
		return cleanXxmlDoc;
	}

	/**
	 * @param xmlDoc
	 * @param xpeMap
	 * @param replacementMap
	 * @return
	 */
	private static Document doReplacements(Document xmlDoc, Map<String, XPathExpression<?>> xpeMap,
			Map<String, String> replacementMap) {
		// TODO Auto-generated method stub

		return null;
	}

	/**
	 * @param xmlDoc
	 */
	private static Map<String, XPathExpression<?>> initialize(Document xmlDoc) {
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
		Map<String, XPathExpression<?>> xpeMap = new HashMap<String, XPathExpression<?>>();

		String key = "Term/Money";
		String xpath = "../avails:Term/avails:Money";
		XPathExpression<Element> xpExpression = xpfac.compile(xpath, Filters.element(), null, availsNSpace, mdNSpace);
		xpeMap.put(key, xpExpression);

		key = "Transaction/ContractID";
		xpath = "./avails:Transaction/avails:ContractID";
		xpExpression = xpfac.compile(xpath, Filters.element(), null, availsNSpace, mdNSpace);
		xpeMap.put(key, xpExpression);

		return xpeMap;
	}

}
