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

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public abstract class AbstractXmlBuilder {

	public abstract Element mGenericElement(String name, String val, Namespace ns);

	/**
	 * @return
	 */
	public abstract String getVersion();

	public abstract void addToPedigree(Object content, Pedigree source);

	public abstract Namespace getAvailsNSpace();

	public abstract Namespace getMdMecNSpace();

	public abstract Namespace getMdNSpace();

	/**
	 * @param elementName
	 * @param schema
	 * @return
	 * @throws IllegalStateException    if supported schema version was not
	 *                                  previously set
	 * @throws IllegalArgumentException if <tt>schema</tt> is unrecognized or
	 *                                  <tt>elementName</tt> is not defined by the
	 *                                  <tt>schema</tt>
	 */
	public abstract boolean isRequired(String elementName, String schema)
			throws IllegalStateException, IllegalArgumentException;

}
