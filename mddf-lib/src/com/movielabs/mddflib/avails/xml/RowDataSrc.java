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
package com.movielabs.mddflib.avails.xml;

import org.jdom2.Element;
import org.jdom2.Namespace;

import com.movielabs.mddflib.avails.xml.Pedigree;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public interface RowDataSrc {

	/**
	 * 
	 * @param colKey
	 * @return
	 */
	Pedigree getPedigreedData(String colKey);

	/**
	 * @param colKey
	 * @return
	 */
	String getData(String colKey);

	/**
	 * Same as invoking
	 * <tt>process(Element parentEl, String childName, Namespace ns, String cellKey, String separator) </tt>
	 * with a <tt>null</tt> separator. A single child element will therefore be
	 * created.
	 * 
	 * @param parentEl
	 * @param childName
	 * @param ns
	 * @param cellKey
	 * @return
	 */
	Element process(Element parentEl, String childName, Namespace ns, String cellKey);

	/**
	 * Add zero or more child elements with the specified name and namespace. The
	 * number of child elements created will be determined by the contents of the
	 * indicated cell. If <tt>separator</tt> is not <tt>null</tt>, then it will be
	 * used to split the string value in the cell with each resulting sub-string
	 * being used to create a distinct child element.
	 * 
	 * @param parentEl
	 * @param childName
	 * @param ns
	 * @param cellKey
	 * @param separator
	 * @return an array of child <tt>Element</tt> instances
	 */
	Element[] process(Element parentEl, String childName, Namespace ns, String cellKey, String separator);

	/**
	 * @return
	 */
	int getRowNumber();
	
	public void addRegion(Element parentEl, String regionType, Namespace ns, String cellKey);

}