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

import org.xml.sax.SAXException;

/**
 * 
 * Exception indicating empty rows have been encountered when ingesting a
 * spreadsheet. By extending <tt>SAXException</tt>, this exception can be
 * used by a <tt>ContentHandler</tt> to indicate that ingest processing may be
 * terminated.
 * 
 * @see StreamingXmlBuilder.makeXmlAsJDom()
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class XlsxDataTermination extends SAXException {

	/**
	 * 
	 * @param msg
	 */
	public XlsxDataTermination(String msg) {
		super(msg);
	}
}
