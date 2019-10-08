/**
 * Copyright (c) 2018 MovieLabs

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.SAXParseException;

import com.gc.iotools.stream.is.InputStreamFromOutputStream;
import com.movielabs.mddf.MddfContext.MDDF_TYPE;
import com.movielabs.mddflib.logging.LogMgmt;

/**
 * Wrapper class for an MDDF file that is to be processed. The function of the
 * wrapper is to provide a uniform structure regardless of the type of file, its
 * encoding (i.e., XML or XLSX), or the processing context (i.e., desktop or
 * server).
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class MddfTarget {
	private File srcFile;
	private ReusableInputStream streamSrc;
	private Document xmlDoc = null;
	private LogMgmt logMgr;
	private MDDF_TYPE mddfType; 
	private int logTag;

	/**
	 * Construct target where the MDDF source is an XML file on the local file
	 * system.
	 * 
	 * <p>
	 * This constructor should NOT be used for cloud-based configurations unless
	 * an uploaded file is first saved to the server's file system.
	 * </p>
	 * 
	 * @param srcFile
	 * @param logMgr
	 * @throws FileNotFoundException
	 */
	public MddfTarget(File srcFile, LogMgmt logMgr) throws FileNotFoundException {
		super();
		this.srcFile = srcFile;
		this.logMgr = logMgr;
		InputStream mainStream = new FileInputStream(srcFile);
		this.streamSrc = new ReusableInputStream(mainStream);
		init();
	}

	/**
	 * Construct target where the MDDF source is an <tt>InputStream</tt>. The
	 * <tt>srcFile</tt> may or may not exist or be readable on the <i>local</i>
	 * file system.
	 * 
	 * @param srcFile
	 * @param xmlStreamSrc
	 */
	public MddfTarget(File srcFile, InputStream xmlStreamSrc, LogMgmt logMgr) {
		super();
		this.srcFile = srcFile;
		this.logMgr = logMgr;
		this.streamSrc = new ReusableInputStream(xmlStreamSrc);
		init();
	}

	/**
	 * Construct target where the MDDF source is not an XML file (i.e., an
	 * XLSX-encoded Avails). An XML-formatted translation must therefore be
	 * provided as an argument to the constructor.
	 * 
	 * @param srcFile
	 * @param xmlDoc
	 * @param logMgr
	 */
	public MddfTarget(File srcFile, Document xmlDoc, LogMgmt logMgr) {
		super();
		this.srcFile = srcFile;
		this.logMgr = logMgr;
		this.xmlDoc = xmlDoc;
		buildStreamSrc();
		init();
	}

	/**
	 * 
	 */
	private void init() {
		/*
		 * Identify type of XML file (i.e., Manifest, Avail, etc)
		 */
		Element docRootEl = getXmlDoc().getRootElement();
		String nSpaceUri = docRootEl.getNamespaceURI();  
		if (nSpaceUri.contains("manifest")) { 
			logTag = LogMgmt.TAG_MANIFEST;
			mddfType = MDDF_TYPE.MANIFEST;
		} else if (nSpaceUri.contains("avails")) { 
			logTag = LogMgmt.TAG_AVAIL;
			mddfType = MDDF_TYPE.AVAILS;
		} else if (nSpaceUri.contains("mdmec")) { 
			logTag = LogMgmt.TAG_MEC;
			mddfType = MDDF_TYPE.MEC;
		} else {
			mddfType = null;
		}
		
	}

	/**
	 * @see http://io-tools.sourceforge.net/easystream/user_guide/
	 *      convert_outputstream_to_inputstream.html
	 */
	private void buildStreamSrc() {
		final InputStreamFromOutputStream<String> xmlStreamSrc = new InputStreamFromOutputStream<String>() {
			@Override
			protected String produce(final OutputStream dataSink) throws Exception {
				XMLOutputter outputter = new XMLOutputter();
				outputter.output(xmlDoc, dataSink);
				return "FooBar";
			}
		};
		this.streamSrc = new ReusableInputStream(xmlStreamSrc);
	}

	/**
	 * @return the srcFile
	 */
	public File getSrcFile() {
		return srcFile;
	}

	/**
	 * Returns an <tt>ReusableInputStream</tt> that can be used to read an XML
	 * representation of the MDDF file.
	 * 
	 * @return the streamSrc
	 */
	public ReusableInputStream getXmlStreamSrc() {
		return streamSrc;
	}

 
	/**
	 * @return XML representation of the MDDF construct
	 */
	public Document getXmlDoc() {
		if (xmlDoc == null) {
			try {
				xmlDoc = XmlIngester.getAsXml(streamSrc);
			} catch (SAXParseException e) {
				int ln = e.getLineNumber();
				String errMsg = "Invalid XML on or before line " + e.getLineNumber();
				String supplemental = e.getMessage();
				logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, srcFile, ln, "XML", supplemental, null);
				return null;
			} catch (IOException e) {
				String errMsg = "Unable to access input source";
				String supplemental = e.getMessage();
				logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, srcFile, -1, "XML", supplemental, null);
				return null;
			}
		}
		return xmlDoc;
	}

	/**
	 * @return the mddfType
	 */
	public MDDF_TYPE getMddfType() {
		return mddfType;
	}

	/**
	 * @return the logTag
	 */
	public int getLogTag() {
		return logTag;
	}
}
