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

import org.apache.poi.ss.usermodel.Cell;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class Pedigree {
//	private int row;
//	private int column;
//	private String sheet;
	private String rawValue;
	private Object source;

	/**
	 * @param row
	 * @param column
	 * @param sheet
	 */
//	Pedigree(int row, int column, String sheet, String rawValue) {
//		super();
//		this.row = row;
//		this.column = column;
//		this.sheet = sheet;
//		this.rawValue = rawValue;
//	}

	/**
	 * @param sourceCell
	 * @param value
	 */
	public Pedigree(Object source, String value) {
		this.source = source;
		this.rawValue = value;
	}
//
//	/**
//	 * @return the row
//	 */
//	public int getRow() {
//		return row;
//	}
//
//	/**
//	 * @return the column
//	 */
//	public int getColumn() {
//		return column;
//	}
//
//	/**
//	 * @return the sheet name
//	 */
//	public String getSheet() {
//		return sheet;
//	}

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
