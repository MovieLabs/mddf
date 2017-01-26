/**
 * Copyright (c) 2017 MovieLabs

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
package com.movielabs.mddflib.logging;

import org.jdom2.Element;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public interface IssueLogger {
	/**
	 * Log an issue with a specific construct within a file. The <tt>target</tt>
	 * indicates the construct within the file and should be specified as either
	 * <ul>
	 * <li>an JDOM Element within an XML file, or</tt>
	 * <li>a <tt>POI Cell</tt> instance used to identify a cell in an XLSX
	 * spreadsheet.</li>
	 * </ul>
	 * 
	 * @param tag
	 * @param level
	 * @param target
	 * @param msg
	 * @param explanation
	 * @param srcRef
	 * @param moduleId
	 */
	public void logIssue(int tag, int level, Object target, String msg, String explanation, LogReference srcRef,
			String moduleId);
}
