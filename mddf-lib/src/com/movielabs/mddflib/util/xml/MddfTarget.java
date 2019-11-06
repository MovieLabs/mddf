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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.jdom2.Document;
import org.jdom2.Element;
import org.xml.sax.SAXParseException;

import com.movielabs.mddf.MddfContext.MDDF_TYPE;
import com.movielabs.mddflib.logging.LogEntryFolder;
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
public class MddfTarget extends DefaultMutableTreeNode {
	protected File srcFile;
	private ReusableInputStream streamSrc = null;
	protected Document xmlDoc = null;
	protected LogMgmt logMgr;
	protected MDDF_TYPE mddfType;
	protected int logTag;
	protected LogEntryFolder logFolder;
	private boolean srcIsXml = true;

	protected MddfTarget() {
		super();
	}

	/**
	 * Construct target where the MDDF source is an XML file on the local file
	 * system.
	 * 
	 * <p>
	 * This constructor should NOT be used for cloud-based configurations unless an
	 * uploaded file is first saved to the server's file system.
	 * </p>
	 * 
	 * @param srcFile
	 * @param logMgr
	 * @throws FileNotFoundException
	 */
	public MddfTarget(File srcFile, LogMgmt logMgr) {
		super();
		this.srcFile = srcFile;
		srcIsXml = srcFile.getName().toLowerCase().endsWith(".xml");
		this.logMgr = logMgr;
		logFolder = logMgr.assignFileFolder(this); 
	}

	/**
	 * Construct target where the MDDF source is an XML file on the local file
	 * system and it is being used as a secondary source of data (i.e., a MEC file
	 * referenced by a Manifest).
	 * 
	 * <p>
	 * This constructor should NOT be used for cloud-based configurations unless an
	 * uploaded file is first saved to the server's file system.
	 * </p>
	 * 
	 * @param parent
	 * @param srcFile
	 * @param logMgr
	 */
	public MddfTarget(MddfTarget parent, File srcFile, LogMgmt logMgr) {
		super();
		this.srcFile = srcFile;
		srcIsXml = srcFile.getName().toLowerCase().endsWith(".xml");
		this.logMgr = logMgr;
		if (parent != null) {
			parent.add(this);
		}
		logFolder = logMgr.assignFileFolder(this); 
	}

	/**
	 * Construct target where the MDDF source is an <tt>InputStream</tt> that may be
	 * used to obtain an MDDDF file. The <tt>srcFile</tt> may or may not exist or be
	 * readable on the <i>local</i> file system. If the MDDF data being targeted is an
	 * XLSX-formatted Avail file the <tt>isXMl</tt> flag should be set to <tt>false</tt>.
	 * 
	 * @param srcFile
	 * @param streamSrc
	 * @param logMgr
	 * @param isXml
	 */
	public MddfTarget(File srcFile, InputStream streamSrc, LogMgmt logMgr, boolean isXml) {
		this(null, srcFile, streamSrc, logMgr, isXml);
	}

	/**
	 * Construct target where the MDDF source is an <tt>InputStream</tt> that may be
	 * used to obtain an MDDF file that is used as a secondary source of data (e.g.,
	 * a MEC file referenced by a Manifest). The <tt>srcFile</tt> may or may not
	 * exist or be readable on the <i>local</i> file system. If the MDDF data being targeted is an
	 * XLSX-formatted Avail file the <tt>isXMl</tt> flag should be set to <tt>false</tt>.
	 * 
	 * @param parent
	 * @param srcFile
	 * @param streamSrc
	 * @param logMgr
	 * @param isXml
	 */
	public MddfTarget(MddfTarget parent, File srcFile, InputStream streamSrc, LogMgmt logMgr, boolean isXml) {
		super();
		this.srcFile = srcFile;
		this.logMgr = logMgr;
		this.srcIsXml = isXml;
		if (parent != null) {
			parent.add(this);
		}
		logFolder = logMgr.assignFileFolder(this);
		this.streamSrc = new ReusableInputStream(streamSrc); 
	}

	/**
	 * Construct target where the MDDF source is not an XML file (i.e., an
	 * XLSX-encoded Avails). An XML-formatted translation must therefore be provided
	 * as an argument to the constructor.
	 * 
	 * @param xmlDoc
	 * @param srcFile
	 * @param logMgr
	 */
	public MddfTarget(Document xmlDoc, File srcFile, LogMgmt logMgr) {
		super();
		this.srcFile = srcFile;
		srcIsXml = srcFile.getName().toLowerCase().endsWith(".xml");
		this.logMgr = logMgr;
		this.xmlDoc = xmlDoc;
		logTag = LogMgmt.TAG_AVAIL;
		mddfType = MDDF_TYPE.AVAILS;
		logFolder = logMgr.assignFileFolder(this);
	}

	/**
	 * @return the srcFile
	 */
	public File getSrcFile() {
		return srcFile;
	}

	/**
	 * Returns an <tt>ReusableInputStream</tt> that can be used to read an MDDF file.
	 * 
	 * @return the streamSrc
	 */
	public ReusableInputStream getXmlStreamSrc() {
		return streamSrc;
	}

	public boolean srcIsXml() {
		return srcIsXml;
	}

	/**
	 * @return XML representation of the MDDF construct
	 */
	public Document getXmlDoc() {
		if (xmlDoc == null) {
			try {
				loadXml();
			} catch (SAXParseException e) {
				int ln = e.getLineNumber();
				String errMsg = "Invalid XML on or before line " + e.getLineNumber();
				String supplemental = e.getMessage();
				logMgr.log(LogMgmt.LEV_ERR, logTag, errMsg, this, ln, "XML", supplemental, null);
			}
		}
		return xmlDoc;
	}

	private void loadXml() throws SAXParseException {
		if(!srcIsXml) {
			throw new IllegalStateException("Can not load XML from a non-XML source");
		}
		if (streamSrc == null) {
			try {
				InputStream mainStream = new FileInputStream(srcFile);
				this.streamSrc = new ReusableInputStream(mainStream);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (streamSrc == null) {
			return;
		}
		try {
			xmlDoc = XmlIngester.getAsXml(streamSrc);
			identifyXmlContext();
		} catch (IOException e) {
			String errMsg = "Unable to access input source";
			String supplemental = e.getMessage();
			logMgr.log(LogMgmt.LEV_ERR, logTag, errMsg, this, -1, "XML", supplemental, null);
		} finally {
			try {
				streamSrc.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public MDDF_TYPE setXmlDoc(Document doc) {
		this.xmlDoc = doc;
		identifyXmlContext();
		return mddfType;
	}

	/**
	 * Identify type of XML file (i.e., Manifest, Avail, etc)
	 */
	protected void identifyXmlContext() {
		if (xmlDoc == null) {
			return;
		}
		Element docRootEl = xmlDoc.getRootElement();
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
			mddfType = MDDF_TYPE.UNK;
		}
	}

	/**
	 * Returns the type of MDDF file (i.e., Manifest, Avails, etc.).
	 * 
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

	/**
	 * @return
	 */
	public LogEntryFolder getLogFolder() {
		return logFolder;
	}

	public MddfTarget getParentTarget() {
		return (MddfTarget) this.getParent();
	}

	/**
	 * when dealing with files uploaded to the cloud, all we have to work with is
	 * the file's name... any info re the original path is lost. As a result, we
	 * have a problem re the use of the MddfTarget's 'key' which is (partially)
	 * path-based.
	 * 
	 * The solution (for now) is to accept the limitation to the cloud validator's
	 * ability to differentiate 2 files with same name
	 * 
	 * @return
	 */
	public String getKey() {
		return LogMgmt.genFolderKey(this);
	}

	public void setParent(MutableTreeNode newParent) {
		super.setParent(newParent);
	}
}
