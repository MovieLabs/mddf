/**
 * Copyright (c) 2016 MovieLabs
 * 
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
package com.movielabs.mddflib.xml;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.movielabs.mddflib.AvailsSheet;

/**
 * Code and functionality formerly in AvailsSheet
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class XmlBuilder {

	public XmlBuilder() {

	}

	/**
	 * Create an Avails XML document based on the data in this spreadsheet
	 * 
	 * @param shortDesc
	 *            a short description that will appear in the document
	 * @return a JAXP document
	 * @throws Exception
	 *             if any errors are encountered
	 */
	public Document makeXML(AvailsSheet aSheet, String shortDesc) throws Exception {
		Document dom = null;
		Element root;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(true);
		// try {
		// get an instance of builder
		DocumentBuilder db = dbf.newDocumentBuilder();

		// create an instance of DOM
		dom = db.newDocument();
		root = dom.createElement("AvailList");
		// Make it the root element of this new document
		Attr xmlns = dom.createAttribute("xmlns");
		xmlns.setValue("http://www.movielabs.com/schema/avails/v2.1/avails");
		root.setAttributeNode(xmlns);

		xmlns = dom.createAttribute("xmlns:xsi");
		xmlns.setValue("http://www.w3.org/2001/XMLSchema-instance");
		root.setAttributeNode(xmlns);

		xmlns = dom.createAttribute("xmlns:md");
		xmlns.setValue("http://www.movielabs.com/schema/md/v2.4/md");
		root.setAttributeNode(xmlns);

		xmlns = dom.createAttribute("xmlns:mdmec");
		xmlns.setValue("http://www.movielabs.com/schema/mdmec/v2.4");
		root.setAttributeNode(xmlns);

		xmlns = dom.createAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation");
		xmlns.setValue("http://www.movielabs.com/schema/avails/v2.1/avails"
				+ "http://www.movielabs.com/schema/avails/v2.1/avails-v2.1.xsd");
		xmlns.setPrefix("xsi");
		root.setAttributeNode(xmlns);

		dom.appendChild(root); 
		for (int i=0; i < aSheet.getRowCount(); i++) {
			RowToXmlHelper xmlConverter = new RowToXmlHelper(aSheet, i, shortDesc); 
			Element e = xmlConverter.makeAvail(dom);
			root.appendChild(e);
		}

		return dom;
	}


}
