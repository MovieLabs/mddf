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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.output.DOMOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
//import org.w3c.dom.Attr;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;

import com.movielabs.mddflib.AvailsSheet;

/**
 * Code and functionality formerly in AvailsSheet
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class XmlBuilder {

	public static Namespace xsiNSpace = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	public static Namespace xsNSpace = Namespace.getNamespace("xs", "http://www.w3.org/2001/XMLSchema");

	private String xsdVersion;
	private String mdMecVer;
	private String mdVer;

	private Namespace availsNSpace;
	private Namespace mdNSpace;
	private Namespace mdMecNSpace;

	private Document availsXsd;

	private Document mdXsd;
	private Document mdMecXsd;

	private XPathFactory xpfac;

	public XmlBuilder() {
		xpfac = XPathFactory.instance();
	}

	public boolean setVersion(String availXsdVersion) {
		mdXsd = null;
		mdMecXsd = null;
		availsXsd = null;
		xsdVersion = null;
		String xsdRsrc = "avails-v" + availXsdVersion;
		availsXsd = getSchema(xsdRsrc);
		if (availsXsd == null) {
			return false;
		}
		availsNSpace = Namespace.getNamespace("avails",
				"http://www.movielabs.com/schema/avails/v" + availXsdVersion + "/avails");
		// Load supporting schemas:
		switch (availXsdVersion) {
		case "2.1":
		case "2.2":
			mdMecVer = "2.4";
			mdVer = "2.4";
			break;
		default:
			xsdVersion = null;
			return false;
		}
		mdMecXsd = getSchema("mdmec-v" + mdMecVer);
		mdMecNSpace = Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v" + mdMecVer);

		mdXsd = getSchema("md-v" + mdVer);
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + mdVer + "/md");

		if (mdMecXsd == null || (mdXsd == null)) {
			xsdVersion = null;
			return false;
		}
		xsdVersion = availXsdVersion;
		ingest("avail", availsXsd);
		System.out.println("XmlBuilder initialized for v" + availXsdVersion);
		return true;
	}

	/**
	 * @param namespace
	 * @param xsdDoc
	 */
	private void ingest(String string, org.jdom2.Document xsdDoc) {

	}

	/**
	 * Return the XSD resource with the specified schema. If the requested
	 * version is not supported a <tt>null</tt> value is returned.
	 * 
	 * @param xsdRsrc
	 * @return
	 */
	private org.jdom2.Document getSchema(String xsdRsrc) {
		String rsrcPath = "/com/movielabs/mddf/resources/" + xsdRsrc + ".xsd";
		SAXBuilder builder = new SAXBuilder();
		builder.setJDOMFactory(new LocatedJDOMFactory());
		InputStream inp = XmlBuilder.class.getResourceAsStream(rsrcPath);
		if (inp == null) {
			// Unsupported version of an MDDF Schema
			return null;
		}
		try {
			InputStreamReader isr = new InputStreamReader(inp, "UTF-8");
			org.jdom2.Document schemaDoc = builder.build(isr);
			return schemaDoc;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return the xsdVersion
	 */
	public String getVersion() {
		return xsdVersion;
	}

	/**
	 * Create an Avails XML document based on the data in the spreadsheet
	 * 
	 * @param shortDesc
	 *            a short description that will appear in the document
	 * @return a JAXP document
	 * @throws IllegalStateException
	 */
	public Document makeXmlAsJDom(AvailsSheet aSheet, String shortDesc) throws IllegalStateException {
		if (xsdVersion == null) {
			throw new IllegalStateException("The XSD version was not set or is unsupported.");
		}
		String xsdUri = "http://www.movielabs.com/schema/avails/v" + xsdVersion + "/avails";
		String xsdLoc = "http://www.movielabs.com/schema/avails/v" + xsdVersion + "/avails-v" + xsdVersion + ".xsd";
		String schemaLoc = xsdUri + " " + xsdLoc;
		Document doc = new Document();
		Element root = new Element("AvailList", availsNSpace);
		root.addNamespaceDeclaration(mdNSpace);
		root.addNamespaceDeclaration(mdMecNSpace);
		root.addNamespaceDeclaration(xsiNSpace);
		// root.setAttribute("schemaLocation", schemaLoc, xsiNSpace);

		doc.setRootElement(root);
		try {
			for (int i = 0; i < aSheet.getRowCount(); i++) {
				RowToXmlHelper xmlConverter = new RowToXmlHelper(aSheet, i, shortDesc);
				Element e = xmlConverter.makeAvail(this);
				root.addContent(e);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return doc;
	}

	public org.w3c.dom.Document makeXmlAsW3C(AvailsSheet aSheet, String shortDesc) throws IllegalStateException {
		Document jdomDoc = makeXmlAsJDom(aSheet, shortDesc);
		DOMOutputter domOut = new DOMOutputter();
		try {
			return domOut.output(jdomDoc);
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @return the availsNSpace
	 */
	Namespace getAvailsNSpace() {
		return availsNSpace;
	}

	/**
	 * @return the mdNSpace
	 */
	Namespace getMdNSpace() {
		return mdNSpace;
	}

	/**
	 * @return the mdMecNSpace
	 */
	Namespace getMdMecNSpace() {
		return mdMecNSpace;
	}

	/**
	 * @param elementName
	 * @param schema
	 * @return
	 * @throws IllegalStateException
	 *             if supported schema version was not previously set
	 * @throws IllegalArgumentException
	 *             if <tt>schema</tt> is unrecognized or <tt>elementName</tt> is
	 *             not defined by the <tt>schema</tt>
	 */
	boolean isRequired(String elementName, String schema) throws IllegalStateException, IllegalArgumentException {
		if (xsdVersion == null) {
			throw new IllegalStateException("The XSD version was not set or is unsupported.");
		}
		Element target = getElement(elementName, schema);
		if (target == null) {
			// TODO: Maybe its an attribute?
			throw new IllegalArgumentException("Schema '" + schema + "' does not define element '" + elementName + "'");
		}
		String minVal = target.getAttributeValue("minOccurs", "1");
		return (!minVal.equals("0"));
	}

	/**
	 * @param name
	 * @param ns
	 * @param inputValue
	 * @return
	 */
	String formatForType(String elementName, Namespace ns, String inputValue)
			throws IllegalStateException, IllegalArgumentException {
		if (xsdVersion == null) {
			throw new IllegalStateException("The XSD version was not set or is unsupported.");
		}
		String schema = ns.getPrefix();
		Element target = getElement(elementName, schema);
		if (target == null) {
			throw new IllegalArgumentException("Schema '" + schema + "' does not define element '" + elementName + "'");
		}
		if (inputValue == null) {
			inputValue = "";
		}
		/*
		 * remove any leading or trailing whitespace
		 */
		String formattedValue = inputValue.replaceAll("[\\s]*$", "");
		formattedValue = formattedValue.replaceAll("^[\\s]*", "");
		String type = target.getAttributeValue("type", "xs:string");
		/*
		 * WHAT ABOUT:::::> <xs:element name="Event"> <xs:simpleType> <xs:union
		 * memberTypes="xs:dateTime xs:date"/> </xs:simpleType> </xs:element>
		 */
		if (type.contains("xs:date")) {
			type = "xs:date";
		}
		switch (type) {
		case "xs:string":
		case "md:id-type":
		case "md:string-ContentID-Identifier":
		case "xs:anyURI":
			break;
		case "xs:duration":
			// input is 'hh:mm:ss'
			// output as 'PThhHmmMssS'
			formattedValue = "PT" + formattedValue.replaceFirst(":", "H");
			formattedValue = formattedValue.replaceFirst(":", "M") + "S";
			break;
		case "xs:boolean": 
			if (formattedValue.equals("Yes")) {
				formattedValue = "true";
			} else if (formattedValue.equals("No")) {
				formattedValue = "false";
			} 
			break;
		default:
			// throw new IllegalArgumentException("Data type '" + type + "' not
			// supported by code :(");

		}
		return formattedValue;
	}

	private Element getElement(String elementName, String schema) {
		XPathExpression<Element> xpExpression = xpfac.compile(".//xs:element[@name='" + elementName + "']",
				Filters.element(), null, xsNSpace);
		Element rootEl = null;
		switch (schema) {
		case "avails":
			rootEl = availsXsd.getRootElement();
			break;
		case "mdmec":
			rootEl = mdMecXsd.getRootElement();
			break;
		case "md":
			rootEl = mdXsd.getRootElement();
			break;
		default:
			throw new IllegalArgumentException("Schema '" + schema + "' is unsupported.");
		}
		Element target = xpExpression.evaluateFirst(rootEl);
		return target;
	}
}
