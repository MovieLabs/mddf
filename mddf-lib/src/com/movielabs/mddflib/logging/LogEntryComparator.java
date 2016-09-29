/**
 * Created Jul 27, 2016 
 * Copyright Motion Picture Laboratories, Inc. 2016
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.movielabs.mddflib.logging;

import java.util.Comparator;

import com.movielabs.mddflib.logging.LogEntryNode.Field;

/**
 * Compares two <tt>LogEntryNodes</tt> using values in the specified field.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class LogEntryComparator implements Comparator<LogEntryNode> {

	private Field field;

	public LogEntryComparator(LogEntryNode.Field field) {
		this.field = field;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(LogEntryNode o1, LogEntryNode o2) { 
		switch (field) {
		case Num:
			return (o1.getEntryNumber() - o2.getEntryNumber());
		case Level:
			return (o1.getLevel() - o2.getLevel());
		case Tag:
			return compareString(o1.getFolder().getLabel(), o2.getFolder().getLabel());
		case Details:
			return compareString(o1.getSummary(), o2.getSummary());
		case Manifest:
			return compareString(o1.getManifestName(), o2.getManifestName());
		case Line:
			int v1 = o1.getLine();
			int v2 = o2.getLine();
			// System.out.println("Comparing " + v1 + " to " + v2);
			return (v1 - v2);
		case Reference:
			return compareString(o1.getReference(), o2.getReference());
		case Module:
			return compareString(o1.getModuleID(), o2.getModuleID());
		}

		return -1;
	}

	private int compareString(String obj1, String obj2) { 
		if (obj1 == obj2) {
			return 0;
		}
		if (obj1 == null) {
			return -1;
		}
		if (obj2 == null) {
			return 1;
		}
		return obj1.compareTo(obj2);
	}
}
