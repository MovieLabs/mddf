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
package com.movielabs.mddflib.testsupport;

import java.io.File;
import java.io.FileNotFoundException;

import org.jdom2.Document;
import org.jdom2.Element;

import com.movielabs.mddf.MddfContext.MDDF_TYPE;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.InterimMddfTarget;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class MddfTestTarget extends InterimMddfTarget {

	/**
	 * @param srcFile
	 * @param logMgr
	 * @throws FileNotFoundException
	 */
	public MddfTestTarget(File srcFile, LogMgmt logMgr) throws FileNotFoundException {
		super(srcFile, logMgr);
		// TODO Auto-generated constructor stub
	}

	public void setMddfType(MDDF_TYPE mddfType) {
		this.mddfType = mddfType;
	}
	
	public void setXmlDoc(Document doc) {
		xmlDoc = doc;
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
}
