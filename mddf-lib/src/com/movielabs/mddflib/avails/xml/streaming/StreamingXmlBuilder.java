/**
 * Copyright (c) 2019 MovieLabs

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
package com.movielabs.mddflib.avails.xml.streaming;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.SAXHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.CommentsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.avails.xml.RowDataSrc;
import com.movielabs.mddflib.avails.xml.AbstractXmlBuilder;
import com.movielabs.mddflib.avails.xml.MetadataBuilder;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.avails.xml.AvailsWrkBook.RESULT_STATUS;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.FormatConverter;
import com.movielabs.mddflib.util.xml.InterimMddfTarget;
import com.movielabs.mddflib.util.xml.SchemaWrapper;

/**
 * Converts an Avails file using the XLSX format to an XML DOM representation
 * using the <tt>XSSFReader</tt> API. This is an event-driven approach to SAX
 * processing that results in a greatly reduced memory footprint.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class StreamingXmlBuilder extends AbstractXmlBuilder {

	/**
	 * <tt>ContentHandler</tt> that allows termination of the ingest process by the
	 * <tt>StreamingXmlBuilder</tt> at any time. Termination is initiated by the
	 * handler throwing a <tt>XlsxDataTermination</tt> exception. The builder
	 * signals to the handler it should initiate termination by setting the field
	 * <tt>terminateNow</tt> to <tt>true</tt>.
	 * 
	 * @author L. Levin, Critical Architectures LLC
	 * 
	 */
	public class AvailSheetXMLHandler extends XSSFSheetXMLHandler implements ContentHandler {

		/**
		 * @param styles
		 * @param comments
		 * @param strings
		 * @param sheetContentsHandler
		 * @param dataFormatter
		 * @param formulasNotResults
		 */
		public AvailSheetXMLHandler(StylesTable styles, CommentsTable comments, ReadOnlySharedStringsTable strings,
				SheetContentsHandler sheetContentsHandler, DataFormatter dataFormatter, boolean formulasNotResults) {
			super(styles, comments, strings, sheetContentsHandler, dataFormatter, formulasNotResults);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler#startElement(java.lang
		 * .String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (terminateNow) {
				throw new XlsxDataTermination("Ran out of data on row " + lastDataRow);
			}
			super.endElement(uri, localName, qName);
		}

	}

	/**
	 * A <tt>SheetContentsHandler</tt> that uses the XSSF Event SAX helpers to do
	 * most of the work of parsing the Sheet XML. Data values are accumulated on a
	 * row-by-row basis. When the end of a row is signaled by the invoking of
	 * <tt>endRow()</tt>, the accumulated data is passed to
	 * <tt>StreamingXmlBuilder.processRow()</tt>.
	 */
	private class ParseByRow implements SheetContentsHandler {
		private boolean haveFirstRow = false;
		private boolean firstCellOfRow;
		private int currentRow = -1;
		private int currentCol = -1;
		private ArrayList<String> rowContentsAsList;
		private String[] rowContentsAsArray;
		private int curCell = 0;
		private boolean rowHasData;

		ParseByRow() {
			rowContentsAsList = new ArrayList<String>();
		}

		@Override
		public void startRow(int rowNum) {
			// Prepare for this row
			firstCellOfRow = true;
			currentCol = -1;
			currentRow = rowNum; // need if creating missing cellReference
			curCell = 0;
			if (haveFirstRow) {
				rowContentsAsArray = new String[rowContentsAsList.size()];
			} else {
				rowContentsAsArray = null;
			}
			rowHasData = false;
		}

		@Override
		public void endRow(int rowNum) {
			if (rowNum == 0) {
				rowContentsAsArray = new String[rowContentsAsList.size()];
				rowContentsAsArray = rowContentsAsList.toArray(rowContentsAsArray);
				haveFirstRow = true;
			}

			try {
				processRow(rowContentsAsArray, rowNum, rowHasData);
			} catch (XlsxDataTermination e) {
//				System.out.println("Empty rows; ronNum=" + rowNum);
			}
		}

		@Override
		public void cell(String cellReference, String formattedValue, XSSFComment comment) {
			// gracefully handle missing CellRef here in a similar way as XSSFCell does
			if (cellReference == null) {
				cellReference = new CellAddress(currentRow, currentCol).formatAsString();
			}
			// Did we miss any cells?
			int thisCol = (new CellReference(cellReference)).getCol();
			int missedCols = thisCol - currentCol - 1;
			for (int i = 0; i < missedCols; i++) {
				addDataToRow("");
			}
			currentCol = thisCol;
			addDataToRow(formattedValue);
		}

		private void addDataToRow(String data) {
			if (!haveFirstRow) {
				rowContentsAsList.add(data);
			} else if (curCell >= rowContentsAsArray.length) {
				// error
				String colID = LogMgmt.mapColNum(curCell);
				int rowID = currentRow + 1;
				String msg = "Ignoring cell " + colID + " in row " + rowID + ": out of bounds";
				logger.log(LogMgmt.LEV_WARN, LogMgmt.TAG_XLATE, msg, null, moduleId);
				return;
			} else {
				rowContentsAsArray[curCell++] = data;
			}
			if (data != null && !data.isEmpty()) {
				rowHasData = true;
			}
		}

		@Override
		public void headerFooter(String text, boolean isHeader, String tagName) {
			// TODO Auto-generated method stub
		}
	}

	// ==============================================
	/**
	 * FOR TESTING ONLY!!
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
	}

	public static final String moduleId = "XmlBuilder_2";

	/**
	 * Number of contiguous empty rows that will result in termination of input
	 * processing.
	 */
	public static final int TERMINATION_THRESHOLD = 5;

	private LogMgmt logger;
	private Version templateVersion;

	private File curSrcXslxFile;
	private String shortDesc;

	private String xsdVersion;
	private String mdMecVer;
	private String mdVer;

	private Namespace availsNSpace;
	private Namespace mdNSpace;
	private Namespace mdMecNSpace;

	private SchemaWrapper availsSchema;
	private SchemaWrapper mdSchema;
	private SchemaWrapper mdMecSchema;

	private HashMap<String, Integer> headerMap;
	private Map<Object, Pedigree> pedigreeMap = null;
	private Map<String, Element> availElRegistry = null;
	private Map<Element, List<Element>> avail2AssetMap;
	private Map<Element, List<Element>> avail2TransMap;
	private Map<Element, Map<String, Element>> avail2EntilementMap;
	private Map<Element, List<String>> entitlement2IdMap;
	private Map<String, Element> assetElRegistry;
	private Map<Element, StreamingRowIngester> element2SrcRowMap;

	private Element rootEl;

	private XPathFactory xpfac = XPathFactory.instance();

	private String[] headerRow_0;

	private boolean noPrefix = false;

	private int emptyRowCnt;

	private int lastDataRow;
	boolean terminateNow = false;

	private MetadataBuilder mdBuilder;

	/**
	 * @param logger
	 * @param sstVersion Avail XSLX version (i.e. '1.x')
	 */
	public StreamingXmlBuilder(LogMgmt logger, Version sstVersion) {
		this.logger = logger;
		this.templateVersion = sstVersion;
		switch (templateVersion) {
		case V1_8:
			setVersion("2.4");
			break;
		case V1_7_3:
			setVersion("2.3");
			break;
		case V1_7_2:
			setVersion("2.2.2");
			break;
		case V1_7:
			setVersion("2.2");
			break;
		default:
			logger.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, "Unsupported template version " + templateVersion, null,
					moduleId);
			throw new IllegalArgumentException("Unsupported Avails Schema version " + templateVersion);
		}
	}

	/**
	 * Set the Avail XML version to use for output.
	 * 
	 * @param availXsdVersion
	 * @return
	 */
	private boolean setVersion(String availXsdVersion) {
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
	 * Return the XML mddf version used when generating XML from the XLSX.
	 * 
	 * @return the xsdVersion
	 */
	public String getVersion() {
		return xsdVersion;
	}

	/**
	 * Convert one sheet within the Workbook to an XML representation. The results
	 * are returned in the form of a <tt>Map</tt> with the following content:
	 * <ul>
	 * <li><tt>results.get("xlsx")</tt>: the <tt>File</tt> srcXslxFile that was
	 * passed as input argument</li>
	 * <li><tt>results.get("xml")</tt>: the JDom2 <tt>Document</tt> that was created
	 * </li>
	 * <li><tt>results.get("pedigree"</tt>: a
	 * <tt>Map<Object, Pedigree> pedigreeMap</tt> instance linking XML elements to
	 * the Avail cell from which they were derived.</li>
	 * <li><tt>results.get("srcFmt")</tt>: <tt>FILE_FMT</tt> of the ingested
	 * XLSX</li>
	 * <li><tt>results.get("status")</tt>: <tt>RESULT_STATUS.COMPLETED</tt></li>
	 * </ul>
	 * The <tt>results</tt> returned will be <tt>null</tt> if XLSX has a FILE_FMT
	 * that is invalid or that can not be processed by the code as currently
	 * implemented.
	 * 
	 * @param srcXslxFile
	 * @param sheetNum
	 * @param shortDesc
	 * @return results
	 * @throws IllegalStateException
	 */
	public Map<String, Object> convert(File srcXslxFile, InputStream inStream, int sheetNum, String shortDesc)
			throws IllegalStateException {
		InterimMddfTarget mddfTarget =  new InterimMddfTarget(srcXslxFile,  logger); 
		Map<String, Object> results = new HashMap<String, Object>();
		FILE_FMT srcMddfFmt = null;
		switch (templateVersion) {
		case V1_8:
			srcMddfFmt = FILE_FMT.AVAILS_1_8;
			break;
		case V1_7_3:
			srcMddfFmt = FILE_FMT.AVAILS_1_7_3;
			break;
		case V1_7_2:
			srcMddfFmt = FILE_FMT.AVAILS_1_7_2;
			break;
		case V1_7:
			srcMddfFmt = FILE_FMT.AVAILS_1_7;
			break;
		case V1_6:
			logger.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL,
					"Version " + templateVersion + " has been deprecated and is no longer supported", mddfTarget,
					moduleId);
			return null;
		case UNK:
			logger.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, "Unable to identify XLSX format ", mddfTarget, moduleId);
			break;
		default:
			logger.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, "Unsupported template version " + templateVersion,
					mddfTarget, moduleId);
			return null;
		}
		Document xmlDoc = makeXmlAsJDom(mddfTarget, inStream, 0, shortDesc);
		results.put("xlsx", srcXslxFile);
		results.put("xml", xmlDoc);
		results.put("pedigree", pedigreeMap);
		results.put("srcFmt", srcMddfFmt);
		results.put("status", RESULT_STATUS.COMPLETED);
		return results;
	}

	/**
	 * @param srcXslxFile
	 * @param sheetNum
	 * @param shortDesc
	 * @return
	 * @throws IllegalStateException
	 */
	private Document makeXmlAsJDom(InterimMddfTarget mddfTarget ,InputStream inStream, int sheetNum, String shortDesc)
			throws IllegalStateException {
		this.shortDesc = shortDesc;
		this.curSrcXslxFile = mddfTarget.getSrcFile();
		if (xsdVersion == null) {
			String msg = "Unable to generate XML from XLSX: XSD version was not set or is unsupported.";
			logger.log(LogMgmt.LEV_ERR, LogMgmt.TAG_XLATE, msg, null, moduleId);
			throw new IllegalStateException("The XSD version was not set or is unsupported.");
		}
		Document doc = initializeDoc();
		rootEl = doc.getRootElement();
		// initialize data structures used to collect interim structures
		initializeMappings();

		mdBuilder = new MetadataBuilder(logger, this);
		/*
		 * initiate event-driven ingest. The streaming parser (i.e., the ParseByRow
		 * instance 'rowHandler) will invoke the processRow() method whenever it has
		 * ingested an entire row,.
		 */
		try {
			OPCPackage xlsxPackage = null; 
			if (inStream == null) {
				xlsxPackage = OPCPackage.open(curSrcXslxFile.getPath(), PackageAccess.READ);
			} else {
				xlsxPackage = OPCPackage.open(inStream);
			}
			XSSFReader xssfReader = new XSSFReader(xlsxPackage);
			StylesTable styles = xssfReader.getStylesTable();
			ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsxPackage);
			XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
			int index = 0;
			while (iter.hasNext()) {
				try (InputStream stream = iter.next()) {
					/*
					 * Is this the correct sheet? Note we identify sheet by the index number, not
					 * the name.
					 */
					// TODO: Allow use of sheet-name to ID the desired sheet
					if (index == sheetNum) {
						String sheetName = iter.getSheetName();
						String msg = "Ingesting Sheet " + sheetName;
						logger.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE, msg, null, moduleId);
						processSheet(styles, strings, stream);
					}
				}
				++index;
			}
		} catch (XlsxDataTermination e) {
			logger.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE, e.getMessage(), null, moduleId);
		} catch (Exception e) {
			e.printStackTrace();
			String msg = "Unable to ingest XLSX: verify correct version was specified";
			logger.log(LogMgmt.LEV_ERR, LogMgmt.TAG_XLATE, msg, null, moduleId);
			return null;
		}

		assembleDoc();

		finalizeDocument(doc, templateVersion);
		String msg = "Completed ingesting XLSX file";
		logger.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE, msg, mddfTarget, moduleId);
		/*
		 * re-set the interim structures to facilitate garbage collection
		 */
//		initializeMappings();

		return doc;
	}

	/**
	 * 
	 */
	private Document initializeDoc() {
		// Create and initialize Document...
		Document doc = new Document();
		Element root = new Element("AvailList", availsNSpace);
		root.addNamespaceDeclaration(mdNSpace);
		root.addNamespaceDeclaration(mdMecNSpace);
		root.addNamespaceDeclaration(SchemaWrapper.xsiNSpace);
		doc.setRootElement(root);
		return doc;
	}

	/**
	 * 
	 */
	private void initializeMappings() {
		pedigreeMap = new HashMap<Object, Pedigree>();
		availElRegistry = new HashMap<String, Element>();
		assetElRegistry = new HashMap<String, Element>();
		avail2AssetMap = new HashMap<Element, List<Element>>();
		avail2TransMap = new HashMap<Element, List<Element>>();
		avail2EntilementMap = new HashMap<Element, Map<String, Element>>();
		entitlement2IdMap = new HashMap<Element, List<String>>();
		element2SrcRowMap = new HashMap<Element, StreamingRowIngester>();
	}

	/**
	 * Parses the content of one sheet using event-driven ingest. The streaming
	 * parser (i.e., the <tt>ParseByRow</tt> instance 'rowHandler) will invoke the
	 * <tt>processRow()</tt> method whenever it has ingested an entire row.
	 *
	 *
	 * @param styles           The table of styles that may be referenced by cells
	 * @param strings          The table of strings that may be referenced by cells
	 *                         in the sheet
	 * @param sheetInputStream The stream to read the sheet-data from.
	 * 
	 * @exception java.io.IOException An IO exception from the parser, possibly from
	 *            a byte stream or character stream supplied by the application.
	 * @throws SAXException if parsing the XML data fails.
	 */
	void processSheet(StylesTable styles, ReadOnlySharedStringsTable strings, InputStream sheetInputStream)
			throws IOException, SAXException, XlsxDataTermination {

		ParseByRow rowHandler = new ParseByRow();
		DataFormatter formatter = new DataFormatter();
		InputSource sheetSource = new InputSource(sheetInputStream);
		try {
			XMLReader sheetParser = SAXHelper.newXMLReader();
			ContentHandler handler = new AvailSheetXMLHandler(styles, null, strings, rowHandler, formatter, false);
			sheetParser.setContentHandler(handler);
			sheetParser.parse(sheetSource);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new RuntimeException("SAX parser appears to be broken - " + e.getMessage());
		}
		int availCnt = availElRegistry.values().size();
		String msg = "Avail count for WorkSheet = " + availCnt;
		logger.log(LogMgmt.LEV_INFO, LogMgmt.TAG_XLATE, msg, null, moduleId);

	}

	/**
	 * The streaming parser (i.e., the <tt>ParseByRow</tt> instance 'rowHandler)
	 * will invoke the <tt>processRow()</tt> method whenever it has ingested an
	 * entire row. The row data will be handed off to an <tt>Ingester</tt> instance
	 * for conversion to XML. Empty rows are, however, ignored.
	 * <p>
	 * This method also implements the logic that will determine if stream
	 * processing should be terminated prior to the end of file being detected. This
	 * is based on the <tt>TERMINATION_THRESHOLD</tt> value.
	 * </p>
	 * 
	 * @param row
	 * @param rowNum
	 * @param rowHasData
	 * @throws XlsxDataTermination
	 */
	void processRow(String[] row, int rowNum, boolean rowHasData) throws XlsxDataTermination {
//		System.out.println("ProcessRow():: rowNum=" + rowNum + ", emptyRowCnt=" + emptyRowCnt);
		// if we hit 5 empty rows one after another we terminate processing
		if (rowHasData) {
			emptyRowCnt = 0;
			lastDataRow = rowNum;
		} else {
			emptyRowCnt++;
		}
		if (emptyRowCnt > TERMINATION_THRESHOLD) {
			terminateNow = true;
		}
		// rows '0' and '1' are the headers with column keys
		switch (rowNum) {
		case 0:
			// save for now
			headerRow_0 = row;
			return;
		case 1:
			headerMap = new HashMap<String, Integer>();
			for (int i = 0; i < row.length; i++) {
				String colKey = headerRow_0[i] + "/" + row[i];
				Integer colPtr = Integer.valueOf(i);
				headerMap.put(colKey, colPtr);
			}
			headerRow_0 = null; // not needed anymore so GC it
			return;
		case 2:
			/* 3rd row may contain either comments or data */
			if (row == null || (!rowHasData)) {
				// 3rd row has been left blank (i.e., no '//OPT or //REQ comments)
				return;
			}
			if (row[1].startsWith("//")) {
				// row contains comments
				return;
			}
		default:
			// ...................
		}
		/*
		 * process a data row
		 */
		if (rowHasData) {
			try {
				IngesterV1_7 ingester = new IngesterV1_7(row, rowNum, this, logger);
			} catch (Exception e) { 
				e.printStackTrace();
				int rowID = rowNum + 1;
				String msg = "Unable to ingest data in row " + rowID+"; Exception while processing: "+e.getMessage() ;
				logger.log(LogMgmt.LEV_ERR, LogMgmt.TAG_XLATE, msg, null, moduleId);
			}
		}
	}

	/**
	 * Final assembly in correct order..
	 */
	private void assembleDoc() {
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
			rootEl.addContent(nextAvailEl);
		}
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

	/**
	 * Finalization deals with any issue requiring multiple rows and/or Avails be
	 * examined collectively. This can best be done when the entire XML document has
	 * been assembled.
	 * 
	 * @param doc     the XML generated from the xlsx
	 * @param version the version of the xlsx file
	 */
	protected void finalizeDocument(Document doc, Version version) {
		switch (version) {
		case V1_8:
			finalizeVolumes(doc);
			break;
		default:
			return;
		}
	}

	/**
	 * If any <tt>Volumes</tt> are defined then the <tt>VolumeMetadata</tt> needs to
	 * be completed by identifying the correct number of Episodes it contains.
	 * 
	 * @param doc
	 */
	private void finalizeVolumes(Document doc) {
		String avPrefix = availsNSpace.getPrefix();
		String xpath_VolMD = "//" + avPrefix + ":VolumeMetadata";
		XPathExpression<Element> xpExp_VolMetadata = xpfac.compile(xpath_VolMD, Filters.element(), null, availsNSpace);
		String xpath_EpisodeNum = "./" + avPrefix + ":EpisodeMetadata/" + avPrefix + ":EpisodeNumber/"
				+ mdNSpace.getPrefix() + ":Number";
		XPathExpression<Element> xpExp_EpisodeNum = xpfac.compile(xpath_EpisodeNum, Filters.element(), null,
				availsNSpace, mdNSpace);

		List<Element> volList = xpExp_VolMetadata.evaluate(doc.getRootElement());
		if (volList.isEmpty()) {
			logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_DEBUG, null, "No Volumes found", null, null, moduleId);
			return;
		}
		logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_INFO, null, "Finalizing " + volList.size() + " Volume(s)", null,
				null, moduleId);
		/**
		 * For each Volume we need to find all Assets with
		 * <ul>
		 * <li>WorkType of 'Episode' and with SeasonContentID that matches the Volume's
		 * SeasonContentID AND</li>
		 * <li>with a @volumeNumber that matches the
		 * <tt>VolumeMetadata/VolumeNumber</tt></li>
		 * </ul>
		 */
		String xpath_SeasonCID = "./" + avPrefix + ":SeasonMetadata/" + avPrefix + ":SeasonContentID";
		String xpath_VolNum = "./" + avPrefix + ":VolumeNumber/" + mdNSpace.getPrefix() + ":Number";
		XPathExpression<Element> xpExp_scid = xpfac.compile(xpath_SeasonCID, Filters.element(), null, availsNSpace);
		XPathExpression<Element> xpExp_vNum = xpfac.compile(xpath_VolNum, Filters.element(), null, availsNSpace,
				mdNSpace);
		for (Element volMdEl : volList) {
			String scid = null;
			String vNum = null;
			Element scidEl = xpExp_scid.evaluateFirst(volMdEl);
			if (scidEl != null) {
				scid = scidEl.getTextNormalize();
			}
			Element vNumEl = xpExp_vNum.evaluateFirst(volMdEl);
			if (vNumEl != null) {
				vNum = vNumEl.getTextNormalize();
			}
			if ((scid == null) || (vNum == null)) {
				// missing key info. This will get flagged downstream when XML is validated
				continue;
			}
			logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_DEBUG, null, "Finalizing Volume " + vNum + ", scid=" + scid,
					null, null, moduleId);
			// find matching Episode Assets
			String xpath_Episodes = "//" + avPrefix + ":Asset[@volNum='" + vNum + "' and ./" + avPrefix
					+ ":EpisodeMetadata/" + avPrefix + ":SeasonMetadata[" + avPrefix + ":SeasonContentID/text()='"
					+ scid + "']]";
			XPathExpression<Element> xpExp_episodes = xpfac.compile(xpath_Episodes, Filters.element(), null,
					availsNSpace);
			List<Element> episodeList = xpExp_episodes.evaluate(doc.getRootElement());
			logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_DEBUG, null,
					"Found " + episodeList.size() + " matching Episode Assets", null, null, moduleId);
			/*
			 * Identify 1st Episode. At the remove the temporary @volNum attribute since it
			 * violates the XSD
			 * 
			 */
			int first = Integer.MAX_VALUE;
			for (Element assetEl : episodeList) {
				assetEl.removeAttribute("volNum");
				Element episodeNumEl = xpExp_EpisodeNum.evaluateFirst(assetEl);
				if (episodeNumEl != null) {
					int eNum = Integer.parseInt(episodeNumEl.getTextNormalize());
					first = Integer.min(first, eNum);
				}
			}
			Element volNoEpEl = volMdEl.getChild("VolumeNumberOfEpisodes", availsNSpace);
			volNoEpEl.setText(Integer.toString(episodeList.size()));
			// VolumeFirstEpisodeNumber goes immediately before the VolumeNumberOfEpisodes
			int index = volMdEl.indexOf(volNoEpEl);
			Element vFirstEl = new Element("VolumeFirstEpisodeNumber", availsNSpace);
			vFirstEl.setText(Integer.toString(first));
			volMdEl.addContent(index, vFirstEl);
		}
	}

	/**
	 * Returns the (zero-based) column number that matches the key. The key is a
	 * composite of the two header columns (e.g., "AvailTrans/Territory")
	 * 
	 * @param key
	 * @return column number or -1 if key does not match a know column header.
	 */
	public int getColumnIdx(String key) {
		if (noPrefix) {
			String[] parts = key.split("/");
			key = parts[1];
		}
		Integer colIdx = headerMap.get(key);
		if (colIdx == null) {
			return -1;
		}
		return colIdx.intValue();
	}

	// =======================================
	// methods used by RowBuilders
	// .........................................

	/**
	 * Returns the <tt>JDom Element</tt> instantiating the <i>Avail</i> associated
	 * with the ALID specified by the row. If this is the first request for the
	 * specified Avail a new element is constructed and returned. Otherwise, a
	 * previously created element will be returned. Thus, there is never more than
	 * one XML element per ALID value.
	 * 
	 * @param curRow
	 * @return
	 */
	Element getAvailElement(StreamingRowIngester curRow) {
		Pedigree alidPedigree = curRow.getPedigreedData("Avail/ALID");
		/*
		 * TODO: next line throws a NullPtrException if column is missing. How do we
		 * handle?
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
			 * No data value for the Avail element itself but for purposes of error logging
			 * we link it to the ALID
			 */
			addToPedigree(availEL, alidPedigree);
			/*
			 * availEl will get added to document at completion of sheet processing. For
			 * now, just store in HashMap.
			 */
			availElRegistry.put(alid, availEL);
			/*
			 * Keeping track of row will facilitate later check to make sure any other row
			 * for same Avail has identical values where required.
			 */
			element2SrcRowMap.put(availEL, curRow);

			Element alidEl = mGenericElement("ALID", alid, getAvailsNSpace());
			availEL.addContent(alidEl);
			addToPedigree(alidEl, alidPedigree);

			availEL.addContent(curRow.mDisposition());
			availEL.addContent(curRow.mPublisher("Licensor", "Avail/DisplayName"));
			availEL.addContent(curRow.mPublisher("ServiceProvider", "Avail/ServiceProvider"));

			String availType = mapWorkType(curRow);
			Element atEl = mGenericElement("AvailType", availType, getAvailsNSpace());
			availEL.addContent(atEl);
			addToPedigree(atEl, curRow.getPedigreedData("AvailAsset/WorkType"));

			Element sdEl = mGenericElement("ShortDescription", shortDesc, getAvailsNSpace());
			availEL.addContent(sdEl);

			// Exception Flag
			curRow.process(availEL, "ExceptionFlag", getAvailsNSpace(), "Avail/ExceptionFlag");

			/*
			 * Initialize data structures for collecting Assets, Transactions, and
			 * Entitlements.
			 */
			avail2AssetMap.put(availEL, new ArrayList<Element>());
			avail2TransMap.put(availEL, new ArrayList<Element>());
			avail2EntilementMap.put(availEL, new HashMap<String, Element>());
		} else {
			/*
			 * make sure key values are aligned...
			 */
			RowDataSrc srcRow = element2SrcRowMap.get(availEL);
			checkForMatch("Avail/ALID", srcRow, curRow, "Avail");
			checkForMatch("Avail/DisplayName", srcRow, curRow, "Avail");
			checkForMatch("Avail/ServiceProvider", srcRow, curRow, "Avail");
			checkForMatch("Avail/ExceptionFlag", srcRow, curRow, "Avail");
			/*
			 * AvailAsset/WorkType is special case as different WorkTypes may map to same
			 * AvailType
			 */

			String definedValue = mapWorkType(srcRow);
			String curValue = mapWorkType(curRow);
			if (!definedValue.equals(curValue)) {
				// Generate error msg
				String msg = "Inconsistent WorkType; value not compatable with 1st definition of referenced Avail";
				Integer row4log = Integer.valueOf(srcRow.getRowNumber() + 1);
				String details = "AVAIL was 1st defined in row " + row4log + " which specifies AvailAsset/WorkType as "
						+ srcRow.getData("AvailAsset/WorkType") + " and requires WorkType=" + definedValue;
				logger.logIssue(LogMgmt.TAG_XLSX, LogMgmt.LEV_ERR, row4log, msg, details, null, moduleId);
			}
		}
		return availEL;
	}

	private boolean checkForMatch(String colKey, RowDataSrc srcRow, RowDataSrc curRow, String entityName) {
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
			Integer source = Integer.valueOf(row4log);
			logger.logIssue(LogMgmt.TAG_XLSX, LogMgmt.LEV_ERR, source, msg, details, null, moduleId);
			return false;
		}

	}

	/**
	 * @param rowHelper
	 * @return
	 */
	private String mapWorkType(RowDataSrc rowHelper) {
		String workTypeSS = rowHelper.getData("AvailAsset/WorkType");
		String availType;
		switch (workTypeSS) {
		case "Movie":
		case "Short":
			availType = "single";
			break;
		case "Volume":
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
	public Namespace getAvailsNSpace() {
		return availsNSpace;
	}

	/**
	 * @return the mdNSpace
	 */
	public Namespace getMdNSpace() {
		return mdNSpace;
	}

	/**
	 * @return the mdMecNSpace
	 */
	public Namespace getMdMecNSpace() {
		return mdMecNSpace;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddflib.avails.xml.AbstractXmlBuilder#isRequired(java.lang.
	 * String, java.lang.String)
	 */
	public boolean isRequired(String elementName, String schema)
			throws IllegalStateException, IllegalArgumentException {
		if (xsdVersion == null) {
			throw new IllegalStateException("The XSD version was not set or is unsupported.");
		}
		return getSchema(schema).isRequired(elementName);
	}

	/**
	 * 
	 * @return the pedigreeMap
	 */
	Map<Object, Pedigree> getPedigreeMap() {
		return pedigreeMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddflib.avails.xml.AbstractXmlBuilder#addToPedigree(java.lang.
	 * Object, com.movielabs.mddflib.avails.xml.Pedigree)
	 */
	public void addToPedigree(Object content, Pedigree source) {
		pedigreeMap.put(content, source);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddflib.avails.xml.AbstractXmlBuilder#mGenericElement(java.lang
	 * .String, java.lang.String, org.jdom2.Namespace)
	 */
	public Element mGenericElement(String name, String val, Namespace ns) {
		Element el = new Element(name, ns);
		String formatted = formatForType(name, ns, val);
		el.setText(formatted);
		return el;
	}

	/**
	 * @param name
	 * @param ns
	 * @param inputValue
	 * @return
	 */
	private String formatForType(String elementName, Namespace ns, String inputValue)
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
			formattedValue = FormatConverter.dateTimeToXml(formattedValue, elementName.startsWith("Start"));
			break;
		default:
			// throw new IllegalArgumentException("Data type '" + type + "' not
			// supported by code :(");

		}
		return formattedValue;
	}

	/**
	 * @param curRow
	 */
	public void createAsset(StreamingRowIngester curRow) {
		/*
		 * Gen unique key and see if there is a matching Element. Unfortunately the
		 * key's structure is based on contentID which is sensitive to the WorkType.
		 */
		String workType = curRow.getData("AvailAsset/WorkType");
		String cidPrefix = "";
		switch (workType) {
		case "Season":
		case "Episode":
		case "Volume":
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
		 * Check the consistency of the Asset info as originally specified with the same
		 * fields in the current row.
		 */
		RowDataSrc srcRow = element2SrcRowMap.get(assetEl);
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
			Integer source = Integer.valueOf(row4log);
			logger.logIssue(LogMgmt.TAG_XLATE, LogMgmt.LEV_DEBUG, source, msg, details, null, moduleId);
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
	 * Metadata is created and added as content to the <tt>Asset</tt> element
	 * 
	 * @param assetEl  - <tt>Asset</tt> element the metadata is appended to.
	 * @param workType - determinines structure of the metadata being added.
	 * @param row      - a <tt>RowDataSrc</tt>
	 */
	public void createAssetMetadata(Element assetEl, String workType, RowDataSrc row) {
		Element metadataEl = mdBuilder.appendMData(row, workType);
		assetEl.addContent(metadataEl);
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

	void addTransaction(Element avail, Element transEl) {
		List<Element> transactionList = avail2TransMap.get(avail);
		transactionList.add(transEl);
	}
}
