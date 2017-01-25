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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.output.DOMOutputter;
import com.movielabs.mddflib.avails.xlsx.AvailsSheet;
import com.movielabs.mddflib.avails.xlsx.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.SchemaWrapper;

/**
 * <tt>XmlBuilder</tt> creates an XML representation of an Avails that has been
 * specified as an XLSX document. The XML may be constructed as either a W3C DOM
 * document or a JDOM2 Document.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class XmlBuilder {

	private static final String moduleId = "XmlBuilder";
	private String xsdVersion;
	private String mdMecVer;
	private String mdVer;

	private Namespace availsNSpace;
	private Namespace mdNSpace;
	private Namespace mdMecNSpace;

	private SchemaWrapper availsSchema;
	private SchemaWrapper mdSchema;
	private SchemaWrapper mdMecSchema;

	private Map<Object, Pedigree> pedigreeMap = null;
	private Map<String, Element> availElRegistry = null;
	private Element root;
	private String shortDesc;
	private Map<Element, List<Element>> avail2AssetMap;
	private Map<Element, List<Element>> avail2TransMap;
	private Map<String, Element> assetElRegistry;
	private Map<Element, RowToXmlHelper> element2SrcRowMap;
	private LogMgmt logger;
	private DefaultMetadata mdHelper_basic;
	private EpisodeMetadata mdHelper_episode;
	private SeasonMetadata mdHelper_season;
	private Version templateVersion;

	/**
	 * @param logger
	 * @param sstVersion
	 *            the template version used by the spreadsheet
	 */
	public XmlBuilder(LogMgmt logger, Version sstVersion) {
		this.logger = logger;
		this.templateVersion = sstVersion;
		mdHelper_basic = new DefaultMetadata(this);
		mdHelper_episode = new EpisodeMetadata(this);
		mdHelper_season = new SeasonMetadata(this);
	}

	public boolean setVersion(String availXsdVersion) {
		availsSchema = null;
		mdSchema = null;
		mdMecSchema = null;
		xsdVersion = null;
		String xsdRsrc = "avails-v" + availXsdVersion;
		availsSchema = SchemaWrapper.factory(xsdRsrc);
		if (availsSchema == null) {
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
		mdMecSchema = SchemaWrapper.factory("mdmec-v" + mdMecVer);
		mdMecNSpace = Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v" + mdMecVer);

		mdSchema = SchemaWrapper.factory("md-v" + mdVer);
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + mdVer + "/md");

		if (mdMecSchema == null || (mdSchema == null)) {
			xsdVersion = null;
			return false;
		}
		xsdVersion = availXsdVersion;
		String msg = "XmlBuilder initialized for v" + availXsdVersion;
		logger.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, null, moduleId);
		return true;
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
		availElRegistry = new HashMap<String, Element>();
		assetElRegistry = new HashMap<String, Element>();
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
		root.addNamespaceDeclaration(SchemaWrapper.xsiNSpace);
		doc.setRootElement(root);

		// build document components row by row.
		try {
			switch (templateVersion) {
			case V1_7:
				for (Row row : aSheet.getRows()) {
					RowToXmlHelper xmlConverter = new RowToXmlHelper(aSheet, row);
					xmlConverter.makeAvail(this);
				}
				break;
			case V1_6:
				for (Row row : aSheet.getRows()) {
					RowToXmlHelper xmlConverter = new RowToXmlHelperV1_6(aSheet, row);
					xmlConverter.makeAvail(this);
				}
				break;
			default:
				break;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		// Final assembly in correct order..
		Iterator<Element> alidIt = availElRegistry.values().iterator();
		while (alidIt.hasNext()) {
			Element nextAvailEl = alidIt.next();
			Element sDescEl = nextAvailEl.getChild("ShortDescription", availsNSpace);
			int index = nextAvailEl.indexOf(sDescEl) + 1;
			nextAvailEl.addContent(index, avail2TransMap.get(nextAvailEl));
			nextAvailEl.addContent(index, avail2AssetMap.get(nextAvailEl));

			root.addContent(nextAvailEl);
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
		Pedigree alidPedigree = curRow.getPedigreedData("Avail/ALID");
		/*
		 * TODO: next line throws a NullPtrException if column is missing. How
		 * do we handle?
		 */
		String alid = alidPedigree.getRawValue();
		logger.logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_DEBUG, null, "Looking for Avail with ALID=["+alid+"]", null, null, moduleId);
		Element availEL = availElRegistry.get(alid);
		if (availEL == null) {
			logger.logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_DEBUG, null, "Building Avail with ALID=["+alid+"]", null, null, moduleId);
			availEL = new Element("Avail", getAvailsNSpace());
			/*
			 * No data value for the Avail element itself but for purposes of
			 * error logging we link it to the ALID
			 */
			addToPedigree(availEL, alidPedigree);
			/*
			 * availEl will get added to document at completion of sheet
			 * processing. For now, just store in HashMap.
			 */
			availElRegistry.put(alid, availEL);
			/*
			 * Keeping track of row will facilitate later check to make sure any
			 * other row for same Avail has identical values where required.
			 */
			element2SrcRowMap.put(availEL, curRow);

			Element alidEl = curRow.mGenericElement("ALID", alid, getAvailsNSpace());
			availEL.addContent(alidEl);
			addToPedigree(alidEl, alidPedigree);

			availEL.addContent(curRow.mDisposition());
			availEL.addContent(curRow.mPublisher("Licensor", "Avail/DisplayName"));
			availEL.addContent(curRow.mPublisher("ServiceProvider", "Avail/ServiceProvider"));

			String availType = mapWorkType(curRow);
			Element atEl = curRow.mGenericElement("AvailType", availType, getAvailsNSpace());
			availEL.addContent(atEl);
			addToPedigree(atEl, curRow.getPedigreedData("AvailAsset/WorkType"));

			Element sdEl = curRow.mGenericElement("ShortDescription", shortDesc, getAvailsNSpace());
			availEL.addContent(sdEl);

			// Exception Flag
			curRow.process(availEL, "ExceptionFlag", getAvailsNSpace(), "Avail/ExceptionFlag");

			// Initialize data structures for collecting Assets and Transactions
			avail2AssetMap.put(availEL, new ArrayList<Element>());
			avail2TransMap.put(availEL, new ArrayList<Element>());
		} else {
			/*
			 * make sure key values are aligned...
			 */
			RowToXmlHelper srcRow = element2SrcRowMap.get(availEL);
			checkForMatch("Avail/ALID", srcRow, curRow, "Avail");
			checkForMatch("Avail/DisplayName", srcRow, curRow, "Avail");
			checkForMatch("Avail/ServiceProvider", srcRow, curRow, "Avail");
			checkForMatch("Avail/ExceptionFlag", srcRow, curRow, "Avail");
			/*
			 * AvailAsset/WorkType is special case as different WorkTypes may
			 * map to same AvailType
			 */

			String definedValue = mapWorkType(srcRow);
			String curValue = mapWorkType(curRow);
			if (!definedValue.equals(curValue)) {
				// Generate error msg
				String msg = "Inconsistent WorkType; value not compatable with 1st definition of referenced Avail";
				int row4log = srcRow.getRowNumber() + 1;
				String details = "AVAIL was 1st defined in row " + row4log + " which specifies AvailAsset/WorkType as "
						+ srcRow.getData("AvailAsset/WorkType") + " and requires WorkType=" + definedValue;
				Cell sourceCell = curRow.sheet.getCell("AvailAsset/WorkType", curRow.getRowNumber());
				logger.logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, sourceCell, msg, details, null, moduleId);
			}
		}
		return availEL;
	}

	/**
	 * @param colKey
	 * @param srcRow
	 * @param rowHelper
	 */
	private boolean checkForMatch(String colKey, RowToXmlHelper srcRow, RowToXmlHelper curRow, String entityName) {
		String definedValue = srcRow.getData(colKey);
		if (definedValue == null) {
			// col not defined so we consider it a match
			return true;
		}
		String curValue = curRow.getData(colKey);
		if (definedValue.equals(curValue)) {
			return true;
		} else {
			// Generate error msg
			String msg = "Inconsistent specification; value does not match 1st definition of referenced " + entityName;
			int row4log = srcRow.getRowNumber() + 1;
			String details = entityName + " was 1st defined in row " + row4log + " which specifies " + colKey + " as '"
					+ definedValue + "'";
			Cell sourceCell = curRow.sheet.getCell(colKey, curRow.getRowNumber());
			logger.logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, sourceCell, msg, details, null, moduleId);
			return false;
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
		case "Episode":
		case "Collection":
		default:
			availType = workTypeSS.toLowerCase();
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
		return getSchema(schema).isRequired(elementName);
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
		if (inputValue == null) {
			inputValue = "";
		}
		/*
		 * remove any leading or trailing whitespace
		 */
		String formattedValue = inputValue.replaceAll("[\\s]*$", "");
		formattedValue = formattedValue.replaceAll("^[\\s]*", "");

		String schema = ns.getPrefix();
		String type = getSchema(schema).getType(elementName);
		switch (type) {
		case "xs:string":
		case "md:id-type":
		case "md:string-ContentID-Identifier":
		case "xs:anyURI":
			break;
		case "xs:duration":
			/**
			 * Input is one of the following:
			 * <ul>
			 * <li>hh</li>
			 * <li>hh:mm</li>
			 * <li>hh:mm:ss</li>
			 * </ul>
			 * The output format is 'PThhHmmMssS'
			 */
			String parts[] = formattedValue.split(":");
			String xmlValue = "PT" + parts[0] + "H";
			if (parts.length > 1) {
				xmlValue = xmlValue + parts[1] + "M";
				if (parts.length > 2) {
					xmlValue = xmlValue + parts[2] + "S";
				}
			}
			formattedValue = xmlValue;
			break;
		case "xs:boolean":
			if (formattedValue.equals("Yes")) {
				formattedValue = "true";
			} else if (formattedValue.equals("No")) {
				formattedValue = "false";
			}
			break;
		case "xs:date":
			break;
		case "xs:dateTime":
			if (formattedValue.matches("[\\d]{4}-[\\d]{2}-[\\d]{2}")) {
				if (elementName.startsWith("End")) {
					formattedValue = formattedValue + "T23:59:59";
				} else {
					formattedValue = formattedValue + "T00:00:00";
				}
			}
			break;
		default:
			// throw new IllegalArgumentException("Data type '" + type + "' not
			// supported by code :(");

		}
		return formattedValue;
	}

	private SchemaWrapper getSchema(String schema) {
		switch (schema) {
		case "avails":
			return availsSchema;
		case "mdmec":
			return mdMecSchema;
		case "md":
			return mdSchema;
		default:
			throw new IllegalArgumentException("Schema '" + schema + "' is unsupported.");
		}
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
	 * @param e
	 */
	void addTransaction(Element avail, Element transEl) {
		List<Element> transactionList = avail2TransMap.get(avail);
		transactionList.add(transEl);
	}

	/**
	 * @param row
	 */
	void createAsset(RowToXmlHelper curRow) {
		/*
		 * Gen unique key and see if there is a matching Element. Unfortunately
		 * the key's structure is based on contentID which is sensitive to the
		 * WorkType.
		 */
		String workType = curRow.getData("AvailAsset/WorkType");
		String cidPrefix = "";
		switch (workType) {
		case "Season":
		case "Episode":
			cidPrefix = workType;
			break;
		default:
		}
		String cidSrc = cidPrefix + "ContentID";
		String cidColKey = "AvailAsset/" + cidSrc;
		String contentID = curRow.getData(cidColKey);
		// concatenate with the ALID
		String alid = curRow.getData("Avail/ALID");
		String assetKey = contentID + "__" + alid;
		Element assetEl = assetElRegistry.get(assetKey);
		if (assetEl == null) {
			assetEl = curRow.buildAsset();
			assetElRegistry.put(assetKey, assetEl);
			element2SrcRowMap.put(assetEl, curRow);
			/* add Asset to the Avail */
			Element availEL = availElRegistry.get(alid);
			addAsset(availEL, assetEl);
			return;
		}
		/*
		 * Check the consistency of the Asset info as originally specified with
		 * the same fields in the current row.
		 */
		RowToXmlHelper srcRow = element2SrcRowMap.get(assetEl);
		boolean match = true;
		match = checkForMatch("AvailAsset/WorkType", srcRow, curRow, "Asset") && match;
		match = checkForMatch("AvailAsset/ContentID", srcRow, curRow, "Asset") && match;
		match = checkForMatch("AvailAsset/EpisodeContentID", srcRow, curRow, "Asset") && match;
		match = checkForMatch("AvailAsset/SeasonContentID", srcRow, curRow, "Asset") && match;
		match = checkForMatch("AvailAsset/SeriesContentID", srcRow, curRow, "Asset") && match;
		if (match) {
			// Generate msg
			String msg = "Ignoring redundant Asset information";
			int row4log = curRow.getRowNumber() + 1;
			String details = "An Asset with " + cidSrc + "=" + contentID
					+ " was previously defined. Asset-specific fields in row " + row4log + " will be ignored";
			Cell sourceCell = curRow.sheet.getCell(cidColKey, curRow.getRowNumber());
			logger.logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_DEBUG, sourceCell, msg, details, null, moduleId);
		}
	}

	/**
	 * @param avail
	 * @param assetEl
	 */
	void addAsset(Element avail, Element assetEl) {
		List<Element> assetList = avail2AssetMap.get(avail);
		assetList.add(assetEl);
	}

	/**
	 * @param assetEl
	 * @param assetWorkType
	 * @param row
	 */
	void createAssetMetadata(Element assetEl, String assetWorkType, RowToXmlHelper row) {
		/*
		 * Need to determine what metadata structure to use based on the
		 * Asset/WorkType
		 */
		switch (assetWorkType) {
		case "Season":
			mdHelper_season.createAssetMetadata(assetEl, row);
			return;
		case "Episode":
			mdHelper_episode.createAssetMetadata(assetEl, row);
			return;
		case "Series":
			return;
		default:
			mdHelper_basic.createAssetMetadata(assetEl, row);
			return;
		}
	}
}
