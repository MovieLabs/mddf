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
package com.movielabs.mddflib.util.xml;

import java.io.File;
import java.io.FileNotFoundException;
import com.movielabs.mddf.MddfContext.MDDF_TYPE;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.ReusableInputStream;

/**
 * Used as a temporary <tt>MddfTarget</tt> while ingesting a file. It may be
 * used in calls to the <tt>logMgmt</tt> API until processing reaches the stage
 * where a complete <tt>MddfTarget</tt> may be constructed. Specifically, this
 * class should be used during any stage of processing prior to when an XML file
 * has been read in.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class InterimMddfTarget extends MddfTarget {

	/**
	 * @param srcFile
	 * @param logMgr
	 * @throws FileNotFoundException
	 */
	public InterimMddfTarget(File srcFile, LogMgmt logMgr)  {
		super( );
		this.srcFile = srcFile;
		this.logMgr = logMgr;
		logTag = LogMgmt.TAG_N_A;
		mddfType = MDDF_TYPE.AVAILS;
		logFolder = logMgr.assignFileFolder(this);
	}
	 
 

	public ReusableInputStream getXmlStreamSrc() {
		throw new UnsupportedOperationException();
	}

}
