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
package com.movielabs.mddflib.avails.xml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
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

import com.movielabs.mddflib.avails.xlsx.AvailsSheet;
import com.movielabs.mddflib.logging.DefaultLogging;
import com.movielabs.mddflib.logging.LogMgmt;

/**
 * Code and functionality formerly in AvailsSheet
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class XmlBuilder {

	private static final String moduleId = "XmlBuilder";
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

	private Map<Object, Pedigree> pedigreeMap = null;
	private Map<String, Element> alidElMap = null;
	private Element root;
	private String shortDesc;
	private Map<Element, List<Element>> avail2AssetMap;
	private Map<Element, List<Element>> avail2TransMap;
	private Map<Element, RowToXmlHelper> element2SrcRowMap;
	private LogMgmt logger;

	public XmlBuilder(LogMgmt logger) {
		xpfac = XPathFactory.instance();
		this.logger = logger;
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
		String msg = "XmlBuilder initialized for v" + availXsdVersion;
		logger.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, null, moduleId);
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
	 * Create an Avails XML document based on the data in the spreadsheet.
	 * 
	 * @param shortDesc
	 *            a short description that will appear in the document
	 * @return a JAXP document
	 * @throws IllegalStateException
	 */
	public Document makeXmlAsJDom(AvailsSheet aSheet, String shortDesc) throws IllegalStateException {
		this.shortDesc = shortDesc;
		if (xsdVersion == null) {
			String msg = "Unable to generate XML from XLSX: XSD version was not set or is unsupported.";
			logger.log(LogMgmt.LEV_ERR, LogMgmt.TAG_AVAIL, msg, null, moduleId);
			throw new IllegalStateException("The XSD version was not set or is unsupported.");
		}
		// initialize data structures...
		pedigreeMap = new HashMap<Object, Pedigree>();
		alidElMap = new HashMap<String, Element>();
		avail2AssetMap = new HashMap<Element, List<Element>>();
		avail2TransMap = new HashMap<Element, List<Element>>();
		element2SrcRowMap = new HashMap<Element, RowToXmlHelper>();

		// Create and initialize Document...
		String xsdUri = "http://www.movielabs.com/schema/avails/v" + xsdVersion + "/avails";
		String xsdLoc = "http://www.movielabs.com/schema/avails/v" + xsdVersion + "/avails-v" + xsdVersion + ".xsd";
		String schemaLoc = xsdUri + " " + xsdLoc;
		Document doc = new Document();
		root = new Element("AvailList", availsNSpace);
		root.addNamespaceDeclaration(mdNSpace);
		root.addNamespaceDeclaration(mdMecNSpace);
		root.addNamespaceDeclaration(xsiNSpace);
		doc.setRootElement(root);

		// build document components row by row....
		try {
			for (Row row : aSheet.getRows()) {
				RowToXmlHelper xmlConverter = new RowToXmlHelper(aSheet, row);
				xmlConverter.makeAvail(this);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		// Final assembly in correct order..
		Iterator<Element> alidIt = alidElMap.values().iterator();
		while (alidIt.hasNext()) {
			Element nextAlidEl = alidIt.next();
			nextAlidEl.addContent(avail2AssetMap.get(nextAlidEl));
			nextAlidEl.addContent(avail2TransMap.get(nextAlidEl));

			root.addContent(nextAlidEl);
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
	 * Returns the <tt>JDom Element</tt> instantiating the <i>Avail</i>
	 * associated with the ALID specified by the row. If this is the first
	 * request for the specified Avail a new element is constructed and
	 * returned. Otherwise, a previously created element will be returned. Thus,
	 * there is never more than one XML element per ALID value.
	 * 
	 * @param curRow
	 * @return
	 */
	Element getAvailElement(RowToXmlHelper curRow) {
		Pedigree pg = curRow.getPedigreedData("Avail/ALID");
		/*
		 * TODO: next line throws a NullPtrException if column is missing.
		 * How do we handle?
		 */
		String alid = pg.getRawValue();
		Element availEL = alidElMap.get(alid);
		if (availEL == null) {
			availEL = new Element("Avail", getAvailsNSpace());
			/*
			 * availEl will get added to document at completion of sheet
			 * processing. For now, just store in HashMap.
			 */
			alidElMap.put(alid, availEL);
			/*
			 * Keeping track of row will facilitate later check to make sure any
			 * other row for same Avail has identical values where required.
			 */
			element2SrcRowMap.put(availEL, curRow);

			Element alidEl = curRow.mGenericElement("ALID", alid, getAvailsNSpace());
			availEL.addContent(alidEl);
			addToPedigree(alidEl, pg);

			availEL.addContent(curRow.mDisposition());
			availEL.addContent(curRow.mPublisher("Licensor", "Avail/DisplayName"));
			availEL.addContent(curRow.mPublisher("ServiceProvider", "Avail/ServiceProvider"));

			String availType = mapWorkType(curRow);
			Element atEl = curRow.mGenericElement("AvailType", availType, getAvailsNSpace());
			availEL.addContent(atEl);
			addToPedigree(atEl, curRow.getPedigreedData("AvailAsset/WorkType"));
			
			Element sdEl = curRow.mGenericElement("ShortDescription", shortDesc, getAvailsNSpace());
			availEL.addContent(sdEl);

			// Initialize data structures for collecting Assets and Transactions
			avail2AssetMap.put(availEL, new ArrayList<Element>());
			avail2TransMap.put(availEL, new ArrayList<Element>());
		} else {
			/*
			 * make sure key values are aligned...
			 */
			RowToXmlHelper srcRow = element2SrcRowMap.get(availEL);
			checkForMatch("Avail/ALID", srcRow, curRow);
			checkForMatch("Avail/DisplayName", srcRow, curRow);
			checkForMatch("Avail/ServiceProvider", srcRow, curRow);
			/*
			 * AvailAsset/WorkType is special case as different WorkTypes may
			 * map to same AvailType
			 */

			String definedValue = mapWorkType(srcRow);
			String curValue = mapWorkType(curRow);
			if (!definedValue.equals(curValue)) {
				// Generate error msg
				String msg = "Inconsistent WorkType; value not compatable with 1st definition of referenced Avail";
				String details = "AVAIL was 1st defined in row " + srcRow.getRowNumber()
						+ " which specifies AvailAsset/WorkType as " + srcRow.getData("AvailAsset/WorkType")
						+ " and requires WorkType=" + definedValue;
				logger.log(LogMgmt.LEV_ERR, LogMgmt.TAG_AVAIL, msg, null, curRow.getRowNumber(), moduleId, details,
						null);
			}
		}
		return availEL;
	}

	/**
	 * @param colKey
	 * @param srcRow
	 * @param rowHelper
	 */
	private void checkForMatch(String colKey, RowToXmlHelper srcRow, RowToXmlHelper curRow) {
		String definedValue = srcRow.getData(colKey);
		String curValue = curRow.getData(colKey);
		if (!definedValue.equals(curValue)) {
			// Generate error msg
			String msg = "Inconsistent AVAIL specification; value does not match 1st definition of referenced Avail";
			String details = "AVAIL was 1st defined in row " + srcRow.getRowNumber() + " which specifies " + colKey
					+ " as " + definedValue;
			logger.log(LogMgmt.LEV_ERR, LogMgmt.TAG_AVAIL, msg, null, curRow.getRowNumber(), moduleId, details, null);
		}

	}

	/**
	 * @param rowHelper
	 * @return
	 */
	private String mapWorkType(RowToXmlHelper rowHelper) {
		String workTypeSS = rowHelper.getData("AvailAsset/WorkType");
		String availType;
		switch (workTypeSS) {
		case "Movie":
		case "Short":
			availType = "single";
			break;
		case "Season":
			availType = "season";
			break;
		case "Episode":
			availType = "episode";
			break;
		default:
			availType = workTypeSS;
		}
		return availType;
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

	/**
	 * 
	 * @return the pedigreeMap
	 */
	public Map<Object, Pedigree> getPedigreeMap() {
		return pedigreeMap;
	}

	void addToPedigree(Object content, Pedigree source) {
		pedigreeMap.put(content, source);
	}

	/**
	 * @param avail
	 * @param assetEl
	 */
	public void addAsset(Element avail, Element assetEl) {
		List<Element> assetList = avail2AssetMap.get(avail);
		assetList.add(assetEl);
	}

	/**
	 * @param avail
	 * @param e
	 */
	public void addTransaction(Element avail, Element transEl) {
		List<Element> transactionList = avail2TransMap.get(avail);
		transactionList.add(transEl);
	}
}
