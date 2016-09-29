/**
 * Created Jul 7, 2016 
 * Copyright Motion Picture Laboratories, Inc. 2016
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.movielabs.mddflib.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * Provides mechanism for linking a <tt>LogEntry</tt> to reference material that
 * may be used to obtain information regarding why a problem was determined to
 * exist and how it may be corrected.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class LogReference {

	private static XPathFactory xpfac = XPathFactory.instance();
	private static Element rootEl;
	private static final String srcPath = "./resources/DocReferences.xml";
	private String label;
	private String uri;

	public static LogReference getRef(String standard, String version, String refID) {
		if (rootEl == null) {
			try {
				loadXml(new File(srcPath));
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		String xPath = "./Specification[@id='" + standard + "']/Ref[@id='" + refID + "']/Version[@id='" + version
				+ "']";
		XPathExpression<Element> xpExpression = xpfac.compile(xPath, Filters.element());
		Element definingEl = xpExpression.evaluateFirst(rootEl);
		if (definingEl == null) {
			return null;
		} else {
			Element specEl = definingEl.getParentElement().getParentElement();
			String specName = specEl.getAttributeValue("label");
			String label = definingEl.getChildTextNormalize("Label") + ", " + specName + " (v" + version + ")";
			String url = definingEl.getChildTextNormalize("RelURL");
			return new LogReference(label, url);
		}
	}

	protected static void loadXml(File inputFile) throws JDOMException, IOException {
		InputStreamReader isr = new InputStreamReader(new FileInputStream(inputFile), "UTF-8");
		SAXBuilder builder = new SAXBuilder();
		builder.setJDOMFactory(new LocatedJDOMFactory());
		Document xmlDoc = builder.build(isr);
		rootEl = xmlDoc.getRootElement();
	}

	/**
	 * @param label
	 * @param uri
	 */
	LogReference(String label, String uri) {
		super();
		this.label = label;
		this.uri = uri;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

}
