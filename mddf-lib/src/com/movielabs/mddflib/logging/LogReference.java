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

import com.movielabs.mddf.MddfContext;

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

	/**
	 * Identical to <tt>getRef(String standard, String refID)</tt> in that the
	 * <tt>version</tt> argument is ignored.
	 * 
	 * @param standard
	 * @param version
	 * @param refID
	 * @return
	 * @deprecated
	 */
	public static LogReference getRef(String standard, String version, String refID) {
		return getRef(standard, refID);
	}

	/**
	 * Return a <tt>LogReference</tt> to documentation matching the query
	 * arguments. The arguments are:
	 * <ul>
	 * <li>standard: indicates an MDDF standards document using the appropriate
	 * code (e.g., 'MMM' for Common Media Manifest Metadata)</li>
	 * <li>refID: indicates topic.</li>
	 * </ul>
	 * If a match for a topic can not be found specific to the latest version of
	 * the specified standard, earlier versions of the document will be checked.
	 * A <tt>null</tt> value indicates no matches for the specified query could
	 * be found.
	 * 
	 * @param standard
	 * @param refID
	 * @return
	 */
	public static LogReference getRef(String standard, String refID) {
		if (rootEl == null) {
			try {
				loadXml(new File(srcPath));
			} catch (Exception e) {
				return null;
			}
		}
		String version = getPriorVersion(standard, null);
		return resolveRef(standard, version, refID);
	}

	private static LogReference resolveRef(String standard, String version, String refID) {

		String xPath = "./Specification[@id='" + standard + "']/Ref[@id='" + refID + "']/Version[@id='" + version
				+ "']";
		XPathExpression<Element> xpExpression = xpfac.compile(xPath, Filters.element());
		Element definingEl = xpExpression.evaluateFirst(rootEl);
		if (definingEl == null) {
			String priorVer = getPriorVersion(standard, version);
			if (priorVer == null) {
				return null;
			} else {
				return resolveRef(standard, priorVer, refID);
			}
		} else {
			Element specEl = definingEl.getParentElement().getParentElement();
			String specName = specEl.getAttributeValue("label");
			String label = definingEl.getChildTextNormalize("Label") + ", " + specName + " (v" + version + ")";
			String url = definingEl.getChildTextNormalize("RelURL");
			return new LogReference(label, url);
		}
	}

	/**
	 * @param standard
	 * @param curVersion
	 * @return
	 */
	private static String getPriorVersion(String standard, String curVersion) {
		String[] supportedVersions = MddfContext.getSupportedVersions(standard);
		if (supportedVersions == null) {
			return null;
		}
		if (curVersion == null) {
			return supportedVersions[0];
		}
		/*
		 * Versions are listed from most recent to oldest. We are looking for
		 * the version prior to the current version. Start by finding ptr to
		 * current version.
		 */
		int ptr = -1;
		for (int i = 0; i < supportedVersions.length; i++) {
			if (supportedVersions[i].equals(curVersion)) {
				ptr = i;
				break;
			}
		}
		if ((ptr < 0) || (ptr >= (supportedVersions.length - 1))) {
			return null;
		} else {
			return supportedVersions[ptr + 1];
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
