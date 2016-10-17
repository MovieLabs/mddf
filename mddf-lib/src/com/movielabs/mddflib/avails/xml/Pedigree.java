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
package com.movielabs.mddflib.avails.xml;

/**
 * Instantiates the linkage of a data value back to its original source. The
 * intent is to provide the logging components with a way to point the user to
 * the specific location in a file that specified a problematic value. The
 * <tt>source</tt> will normally be an XML Element but, in the case of
 * Avails, may also be a cell in a XLSX spreadsheet.
 * 
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class Pedigree {
	private String rawValue;
	private Object source;

	/**
	 * @param sourceCell
	 * @param value
	 */
	public Pedigree(Object source, String value) {
		this.source = source;
		this.rawValue = value;
	}

	public String getRawValue() {
		return rawValue;
	}

	/**
	 * @return the source
	 */
	public Object getSource() {
		return source;
	}

}
