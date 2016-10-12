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
package com.movielabs.mddflib.util.xml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * Wrapper for an XSD specification. This class provides functions supporting
 * queries and comparisons that facilitate checking an XML file for
 * compatibility with a given schema. It is not intended to replace the classes
 * in the <tt>javax.xml.validation</tt> package but it is rather a supplement
 * intended to make some types of checks easier.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class SchemaWrapper {
	public static final Namespace xsNSpace = Namespace.getNamespace("xs", "http://www.w3.org/2001/XMLSchema");
	public static final Namespace xsiNSpace = Namespace.getNamespace("xsi",
			"http://www.w3.org/2001/XMLSchema-instance");

	public static final String RSRC_PACKAGE = "/com/movielabs/mddf/resources/";
	private static Map<String, SchemaWrapper> cache = new HashMap<String, SchemaWrapper>();

	private Document schemaXSD;

	private Element rootEl;

	private XPathFactory xpfac = XPathFactory.instance();

	public static SchemaWrapper factory(String xsdRsrc) {
		synchronized (cache) {
			SchemaWrapper target = cache.get(xsdRsrc);
			if (target == null) {
				target = new SchemaWrapper(xsdRsrc);
				cache.put(xsdRsrc, target);
			}
			return target;
		}
	}

	/**
	 * Return the XSD resource with the specified schema. If the requested
	 * version is not supported a <tt>null</tt> value is returned.
	 * 
	 * @param xsdRsrc
	 * @return
	 */
	private static Document getSchemaXSD(String xsdRsrc) {
		String rsrcPath = RSRC_PACKAGE + xsdRsrc + ".xsd";
		SAXBuilder builder = new SAXBuilder();
		builder.setJDOMFactory(new LocatedJDOMFactory());
		InputStream inp = SchemaWrapper.class.getResourceAsStream(rsrcPath);
		if (inp == null) {
			// Unsupported version of an MDDF Schema
			return null;
		}
		try {
			InputStreamReader isr = new InputStreamReader(inp, "UTF-8");
			Document schemaDoc = builder.build(isr);
			return schemaDoc;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private SchemaWrapper(String xsdRsrc) {
		schemaXSD = getSchemaXSD(xsdRsrc);
		rootEl = schemaXSD.getRootElement();
	}

	public String getType(String elementName) {
		Element target = getElement(elementName);
		if (target == null) {
			throw new IllegalArgumentException(
					"Schema '" + schemaXSD + "' does not define element '" + elementName + "'");
		}
		String type = target.getAttributeValue("type", "xs:string");
		/*
		 * WHAT ABOUT:::::> <xs:element name="Event"> <xs:simpleType> <xs:union
		 * memberTypes="xs:dateTime xs:date"/> </xs:simpleType> </xs:element>
		 */
		if (type.contains("xs:date")) {
			type = "xs:date";
		}
		return type;
	}

	/**
	 * @param elementName
	 * @return
	 * @throws IllegalArgumentException
	 *             if <tt>schema</tt> is unrecognized or <tt>elementName</tt> is
	 *             not defined by the <tt>schema</tt>
	 */
	public boolean isRequired(String elementName) throws IllegalStateException, IllegalArgumentException { 
		Element target = getElement(elementName);
		if (target == null) {
			// TODO: Maybe its an attribute?
			throw new IllegalArgumentException(
					"Schema '" + schemaXSD + "' does not define element '" + elementName + "'");
		}
		String minVal = target.getAttributeValue("minOccurs", "1");
		return (!minVal.equals("0"));
	}

	private Element getElement(String elementName) {
		XPathExpression<Element> xpExpression = xpfac.compile(".//xs:element[@name='" + elementName + "']",
				Filters.element(), null, xsNSpace);
		Element target = xpExpression.evaluateFirst(rootEl);
		return target;
	}

}
