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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.FormatConverter;
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
	private Map<Element, Map<String, Element>> avail2EntilementMap;
	private Map<Element, List<String>> entitlement2IdMap;
	private Map<String, Element> assetElRegistry;
	private Map<Element, AbstractRowHelper> element2SrcRowMap;
	private LogMgmt logger;
	private Version templateVersion;
	private File curSrcXslxFile;
	private MetadataBuilder mdBuilder;

	/**
	 * @param logger
	 * @param sstVersion
	 *            the template version used by the spreadsheet
	 */
	public XmlBuilder(LogMgmt logger, Version sstVersion) {
		this.logger = logger;
		this.templateVersion = sstVersion;
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

		FILE_FMT availsFmt = MddfContext.identifyMddfFormat("avails", availXsdVersion);
		if (availsFmt == null) {
			throw new IllegalArgumentException("Unsupported Avails Schema version " + availXsdVersion);
		}
		Map<String, String> uses = MddfContext.getReferencedXsdVersions(availsFmt);

		mdMecVer = uses.get("MDMEC");
		mdVer = uses.get("MD");
		mdMecSchema = SchemaWrapper.factory("mdmec-v" + mdMecVer);
		mdMecNSpace = Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v" + mdMecVer);

		mdSchema = SchemaWrapper.factory("md-v" + mdVer);
		mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v" + mdVer + "/md");

		if (mdMecSchema == null || (mdSchema == null)) {
			xsdVersion = null;
			return false;
		}
		xsdVersion = availXsdVersion;
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
	 * @param aSheet
	 * @param shortDesc
	 *            a short description that will appear in the document
	 * @param srcXslxFile
	 *            original file (used for logging; may be <tt>null</tt>
	 * @return a JAXP document
	 * @throws IllegalStateException
	 */
	public Document makeXmlAsJDom(AvailsSheet aSheet, String shortDesc, File srcXslxFile) throws IllegalStateException {
		this.shortDesc = shortDesc;
		this.curSrcXslxFile = srcXslxFile;
		if (xsdVersion == null) {
			String msg = "Unable to generate XML from XLSX: XSD version was not set or is unsupported.";
			logger.log(LogMgmt.LEV_ERR, LogMgmt.TAG_XLATE, msg, null, moduleId);
			throw new IllegalStateException("The XSD version was not set or is unsupported.");
		}
		// initialize data structures...
		pedigreeMap = new HashMap<Object, Pedigree>();
		availElRegistry = new HashMap<String, Element>();
		assetElRegistry = new HashMap<String, Element>();
		avail2AssetMap = new HashMap<Element, List<Element>>();
		avail2TransMap = new HashMap<Element, List<Element>>();
		avail2EntilementMap = new HashMap<Element, Map<String, Element>>();
		entitlement2IdMap = new HashMap<Element, List<String>>();
		element2SrcRowMap = new HashMap<Element, AbstractRowHelper>();

		mdBuilder = new MetadataBuilder(aSheet.getVersion(), logger, this);

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
		String msg = "Converting Excel Avails to XML v" + xsdVersion;
		logger.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE, msg, srcXslxFile, moduleId);
		msg = "Processing spreadsheet '" + aSheet.getName() + "'; RowCount=" + aSheet.getRowCount();
		logger.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE, msg, srcXslxFile, moduleId);

		// build document components row by row.
		try {
			rowLoop: for (Row row : aSheet.getRows()) {
				msg = "Converting row " + row.getRowNum();
				logger.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_XLATE, msg, null, moduleId);
				AbstractRowHelper rowHelper = AbstractRowHelper.createHelper(aSheet, row);
				if (rowHelper != null) {
					rowHelper.makeAvail(this);
				} else {
					logger.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_XLATE, "Unsupported XLSX version", srcXslxFile, moduleId);
					break rowLoop;
				}
			}
		} catch (Exception e) {
			msg = "Exception while ingesting XLSX: " + e.getLocalizedMessage();
			logger.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_XLATE, msg, srcXslxFile, moduleId);
			return null;
		}

		// Final assembly in correct order..
		Iterator<Element> alidIt = availElRegistry.values().iterator();
		while (alidIt.hasNext()) {
			Element nextAvailEl = alidIt.next();
			Element sDescEl = nextAvailEl.getChild("ShortDescription", availsNSpace);
			int index = nextAvailEl.indexOf(sDescEl) + 1;
			Map<String, Element> seMap = avail2EntilementMap.get(nextAvailEl);
			if (seMap != null && !seMap.isEmpty()) {
				Collection<Element> seSet = seMap.values();
				nextAvailEl.addContent(index, seSet);
			}
			nextAvailEl.addContent(index, avail2TransMap.get(nextAvailEl));
			nextAvailEl.addContent(index, avail2AssetMap.get(nextAvailEl));
			finalizeAssetMetadata(nextAvailEl);
			root.addContent(nextAvailEl);
		}
		msg = "Completed ingesting XLSX file";
		logger.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE, msg, srcXslxFile, moduleId);
		return doc;
	}

	public org.w3c.dom.Document makeXmlAsW3C(AvailsSheet aSheet, String shortDesc) throws IllegalStateException {
		Document jdomDoc = makeXmlAsJDom(aSheet, shortDesc, null);
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
	Element getAvailElement(AbstractRowHelper curRow) {
		Pedigree alidPedigree = curRow.getPedigreedData("Avail/ALID");
		/*
		 * TODO: next line throws a NullPtrException if column is missing. How
		 * do we handle?
		 */
		String alid = alidPedigree.getRawValue();
		logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_DEBUG, curSrcXslxFile,
				"Looking for Avail with ALID=[" + alid + "]", null, null, moduleId);
		Element availEL = availElRegistry.get(alid);
		if (availEL == null) {
			logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_DEBUG, curSrcXslxFile,
					"Building Avail with ALID=[" + alid + "]", null, null, moduleId);
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

			/*
			 * Initialize data structures for collecting Assets, Transactions,
			 * and Entitlements.
			 */
			avail2AssetMap.put(availEL, new ArrayList<Element>());
			avail2TransMap.put(availEL, new ArrayList<Element>());
			avail2EntilementMap.put(availEL, new HashMap<String, Element>());
		} else {
			/*
			 * make sure key values are aligned...
			 */
			AbstractRowHelper srcRow = element2SrcRowMap.get(availEL);
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
				logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_ERR, sourceCell, msg, details, null, moduleId);
			}
		}
		return availEL;
	}

	/**
	 * @param colKey
	 * @param srcRow
	 * @param rowHelper
	 */
	private boolean checkForMatch(String colKey, AbstractRowHelper srcRow, AbstractRowHelper curRow,
			String entityName) {
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
			logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_ERR, sourceCell, msg, details, null, moduleId);
			return false;
		}

	}

	/**
	 * @param rowHelper
	 * @return
	 */
	private String mapWorkType(AbstractRowHelper rowHelper) {
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
			formattedValue = FormatConverter.durationToXml(formattedValue);
			break;
		case "xs:boolean":
			formattedValue = FormatConverter.booleanToXml(formattedValue);
			break;
		case "xs:date":
			break;
		case "xs:dateTime":
			formattedValue = FormatConverter.dateTimeToXml(formattedValue, elementName.startsWith("End"));
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

	void addEntitlement(Element avail, String ecosysId, Element eidEl) {
		Map<String, Element> entitlmentMap = avail2EntilementMap.get(avail);
		Element seEl = entitlmentMap.get(ecosysId);
		if (seEl == null) {
			// new ecosystem for this Avail
			seEl = new Element("SharedEntitlement", getAvailsNSpace());
			seEl.setAttribute("ecosystem", ecosysId);
			entitlmentMap.put(ecosysId, seEl);
			entitlement2IdMap.put(seEl, new ArrayList<String>());
		}
		/*
		 * Multiple IDs are allowed for any given ecosystem BUT we want to avoid
		 * redundant entries.
		 */
		List<String> idList = entitlement2IdMap.get(seEl);
		String eid = eidEl.getText();
		if (idList.contains(eid)) {
			return;
		} else {
			seEl.addContent(eidEl);
			idList.add(eid);
		}
	}

	/**
	 * @param row
	 */
	void createAsset(AbstractRowHelper curRow) {
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
		AbstractRowHelper srcRow = element2SrcRowMap.get(assetEl);
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
			logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_DEBUG, sourceCell, msg, details, null, moduleId);
		}
		/*
		 * When dealing with a Movie or Episode, more that 1 ReleaseHistory and
		 * Rating may be specified per-Asset. We therefore need to check for new
		 * data on the current row and, if unique, append it to existing Asset
		 * Metadata. The WorkType will determine which type of Metadata element
		 * we are looking for.
		 */
		// Element metadataEl = null;
		// switch (workType) {
		// case "Movie":
		// metadataEl =mdBuilder.buildMovieMData(curRow);
		// break;
		// case "Episode":
		// metadataEl =mdBuilder.buildTvMData(curRow);
		// break;
		// default:
		// }
		// assetEl.addContent(metadataEl);
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
	void createAssetMetadata(Element assetEl, String assetWorkType, AbstractRowHelper row) {
		Element metadataEl = mdBuilder.appendMData(row, assetWorkType);
		assetEl.addContent(metadataEl);
	}

	/**
	 * @param availEl
	 */
	private void finalizeAssetMetadata(Element availEl) {
		List<Element> assetList = availEl.getChildren("Asset", availsNSpace);
		for (Element nextEl : assetList) {
			String assetWorkType = nextEl.getChildText("WorkType", availsNSpace);
			Element mdEl = null;
			switch (assetWorkType) {
			case "Season":
				mdEl = nextEl.getChild("SeasonMetadata", availsNSpace);
				break;
			case "Episode":
				mdEl = nextEl.getChild("EpisodeMetadata", availsNSpace);
				break;
			case "Series":
				mdEl = nextEl.getChild("SeriesMetadata", availsNSpace);
				break;
			default:
				mdEl = nextEl.getChild("Metadata", availsNSpace);
				break;
			}
			if (mdEl != null) {
				/*
				 * Is this still needed given new mode of md building?
				 */
				// mdHelper_basic.finalize(mdEl);
			}
		}
	}

	void appendToLog(String msg, int logLevel, Cell target) {
		logger.logIssue(LogMgmt.TAG_XLATE, logLevel, target, msg, null, null, moduleId);
	}
}
