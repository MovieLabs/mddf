/**
 * Created Jun 29, 2016 
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

import java.io.File;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class LogEntryNode extends LogEntry {

	public static enum Field {
		Num, Level, Tag, Details, File, Line, Reference, Module
	};

	public static final String fieldNames[] = { "Num", "Level", "Tag", "Summary", "File", "Line", "Reference",
			"Module" };
	/**
	 * Column separator to use when generating CSV file
	 */
	public static String colSep = "\t";
	private static int cCnt;

	static {
		cCnt = fieldNames.length;
	}

	private int level;
	private String summary;
	private String locPath = null;
	private String locFile = null;
	private int locLine;
	private String moduleID;
	private int msgSeqNum;
	private String tooltip;
	private LogReference srcRef;
	private LogEntryFolder folder;

	/**
	 * @param level
	 * @param tag
	 * @param summary
	 * @param fileName
	 * @param line
	 * @param moduleID
	 * @param msgSeqNum
	 * @param tooltip
	 * @param srcRef
	 */
	public LogEntryNode(int level, LogEntryFolder tagNode, String msg, File xmlFile, int line, String moduleID,
			int msgSeqNum, String tooltip, LogReference srcRef) {
		super();
		if (tagNode == null) {
			throw new IllegalArgumentException("NULL tagNode argument");
		}
		if (msg == null) {
			throw new IllegalArgumentException("NULL Details argument");
		}
		this.level = level;
		this.folder = tagNode;
		this.setTag(tagNode.getTagAsText());
		this.summary = msg;
		this.myFile = xmlFile;
		if (xmlFile != null) {
			this.locPath = xmlFile.getAbsolutePath();
			this.locFile = xmlFile.getName();
		}
		this.locLine = line;
		this.moduleID = moduleID;
		this.msgSeqNum = msgSeqNum;
		this.tooltip = tooltip;
		this.srcRef = srcRef;
	}

	public String toString() {
		return summary.substring(0, Math.min(10, summary.length()));
	}

	public String toCSV() {
		String trimedRow = "";
		for (int j = 0; j < cCnt; j++) {
			switch (fieldNames[j]) {
			case "Num":
				trimedRow = trimedRow + Integer.toString(msgSeqNum) + colSep;
				break;
			case "Level":
				trimedRow = trimedRow + LogMgmt.logLevels[level] + colSep;
				break;
			case "Type":
			case "Tag":
				trimedRow = trimedRow + getTagAsText() + colSep;
				break;
			case "Summary":
				trimedRow = trimedRow + summary + colSep;
				break;
			case "File":
				trimedRow = trimedRow + locFile + colSep;
				break;
			case "Line":
				trimedRow = trimedRow + locLine + colSep;
				break;
			case "Module":
				// Skip as not of interest to end-users...
				// trimedRow = trimedRow + moduleID + colSep;
				break;
			case "Reference":
				trimedRow = trimedRow + getReference() + colSep;
				break;
			default:
				trimedRow = trimedRow + " N.A. " + colSep;
				break;
			}
		}
		if (tooltip != null && !tooltip.isEmpty() && !tooltip.equalsIgnoreCase(summary)) {
			trimedRow = trimedRow + tooltip + colSep;
		} else {
			trimedRow = trimedRow + " " + colSep;
		}
		if (srcRef != null) {
			trimedRow = trimedRow + srcRef.getLabel() + colSep;
		} else {
			trimedRow = trimedRow + " " + colSep;
		}
		return trimedRow;
	}

	/**
	 * Return the absolute path of the XML file associated with the log entry.
	 * 
	 * @return the locPath
	 */
	public String getManifestPath() {
		return locPath;
	}

	public String getManifestName() {
		return locFile;
	}

	/**
	 * Return the line in the XML Manifest file associated with the log entry.
	 * 
	 * @return
	 */
	public int getLine() {
		return locLine;
	}

	/**
	 * Return the severity level of the log entry.
	 * 
	 * @return the level
	 * @see Logger.logLevels
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * @return the summary text
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * @return the moduleID
	 */
	public String getModuleID() {
		return moduleID;
	}

	/**
	 * @return the tooltip
	 */
	public String getTooltip() {
		return tooltip;
	}

	/**
	 * @return
	 */
	public LogEntryFolder getFolder() {
		return folder;
	}

	/**
	 * Return a reference (i.e., citation) associated with this entry. This will
	 * refer to a document and, optionally, section or page number. For example,
	 * a reference might be <i> 'Section 3.1.2 of Media Manifest Delivery Core,
	 * TR-META-MMC (v1.0)' </i>
	 * <p>
	 * If no reference is associated with the entry and empty string is
	 * returned.
	 * </p>
	 * 
	 * @return
	 */
	public String getReference() {
		if (srcRef == null) {
			return "";
		} else {
			return srcRef.getLabel();
		}
	}

	/**
	 * Return the URI for the reference (i.e., citation) associated with this
	 * entry. If no reference is associated with the entry and empty string is
	 * returned.
	 * 
	 * @return a URI or empty string
	 */
	public String getReferenceUri() {
		if (srcRef == null) {
			return "";
		} else {
			return srcRef.getUri();
		}
	}

	/**
	 * @return the msgSeqNum
	 */
	public int getEntryNumber() {
		return msgSeqNum;
	}

	/**
	 * 
	 */
	public void print() {
		String msg = msgSeqNum + ": " + LogMgmt.logLevels[level] + ": " + tag + ": " + summary;
		System.out.println(msg);

	}

}
