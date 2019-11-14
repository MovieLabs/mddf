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
package com.movielabs.mddflib.logging;

import java.io.File;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.jdom2.located.Located;

import com.movielabs.mddflib.util.xml.MddfTarget;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public interface LogMgmt extends IssueLogger {

	/**
	 * Logging levels. Sequence matches associated value
	 */
	public static String[] logLevels = { "Debug", "Notice", "Warning", "Error", "Fatal", "Info" };
	public static final int LEV_DEBUG = 0;
	public static final int LEV_NOTICE = 1;
	public static final int LEV_WARN = 2;
	public static final int LEV_ERR = 3;
	public static final int LEV_FATAL = 4;
	public static final int LEV_INFO = 5;

	public static String[] logTags = { "Manifest", "CPE Model", "Profile", "Best Prac.", "Action", "Other", "Avail",
			"Common MD", "Rating", "XML", "MEC", "XLSX", "XLATE", "Offer" };
	public static final int TAG_MANIFEST = 0;
	public static final int TAG_MODEL = 1;
	public static final int TAG_PROFILE = 2;
	public static final int TAG_BEST = 3;
	public static final int TAG_ACTION = 4; // user triggered some action via
											// GUI
	public static final int TAG_N_A = 5;
	public static final int TAG_AVAIL = 6;
	public static final int TAG_MD = 7;
	public static final int TAG_CR = 8;
	public static final int TAG_XSD = 9;
	public static final int TAG_MEC = 10;
	public static final int TAG_XLSX = 11;
	public static final int TAG_XLATE = 12;
	public static final int TAG_OFFER = 13;

	public static final String DEFAULT_TOOL_FOLDER_KEY = "%VALIDATOR";
	public static final String DEFAULT_TOOL_FOLDER_LABEL = "Validator";
	public static final String KEY_SEP = "<--";

	/**
	 * @param lev
	 * @param tag
	 * @param msg
	 * @param targetFile
	 * @param moduleId
	 */
	void log(int lev, int tag, String msg, MddfTarget targetFile, String moduleId);

	/**
	 * @param level
	 * @param tag
	 * @param msg
	 * @param targetFile
	 * @param targetData may be a XML Element, an XLSX cell, or <tt>null</tt>
	 * @param moduleId
	 * @param details
	 * @param srcRef
	 */
	void log(int level, int tag, String msg, MddfTarget targetFile, Object targetData, String moduleId, String details,
			LogReference srcRef);

	/** 
	 */
	public void clearLog();
	
	public void clearLog(MddfTarget target);

	public LogEntryFolder assignFileFolder(MddfTarget target);

	/**
	 * Save the log messages in the desired location and format.
	 * 
	 * @param outFile
	 * @param format
	 * @throws IOException
	 */
	public void saveAs(File outFile, String format) throws IOException;

	/**
	 * Set the file currently being processed. Until the next invocation of
	 * <tt>setCurrentFile()</tt>, all subsequent log entries will, be default, will
	 * be associated with this file unless a different <tt>File</tt> is explicitly
	 * identified.
	 * 
	 * @param srcfile
	 * @deprecated use <tt>pushFileContext()</tt> and <tt>popFileContext()</tt>
	 */
	public void setCurrentFile(File srcfile, boolean clear);

	public abstract LogEntryFolder pushFileContext(MddfTarget targetFile);

	public abstract void popFileContext(MddfTarget targetFile);

	/**
	 * @param level
	 */
	public void setMinLevel(int level);

	public int getMinLevel();

	public void setInfoIncluded(boolean flag);

	public boolean isInfoIncluded();

	/**
	 * Convert a XLSX column number to the letter ID displayed to user. Examples:
	 * <ul>
	 * <li>Col# 0 -> 'A'</li>
	 * <li>Col# 77 -> 'BZ'</li>
	 * </ul>
	 * 
	 * @param colNum
	 * @return
	 */
	static String mapColNum(int colNum) {
		if (colNum >= 0 && colNum < 26)
			return String.valueOf((char) ('A' + colNum));
		else if (colNum > 25)
			return mapColNum((colNum / 26) - 1) + mapColNum(colNum % 26);
		else
			return "#";
	}

	/**
	 * @param severity
	 * @return
	 */
	public static int text2Level(String severity) {
		// its a short list so no need to get clever....
		for (int i = 0; i < logLevels.length; i++) {
			if (logLevels[i].equals(severity)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the <tt>folderKey</tt> for the targeted file. Key is generated using
	 * the processing hierarchy indicated by the sequence of 'parent' MddfTargets.
	 * 
	 * @param target
	 * @return
	 */
	public static String genFolderKey(MddfTarget target) {
		String folderKey = null;
		if(target == null) {
			/* If the target is NULL then put it in the Validator's folder */
			folderKey = DEFAULT_TOOL_FOLDER_KEY;
			return folderKey;
		} 
		File targetFile = target.getSrcFile();
		if (targetFile != null) {
			folderKey = targetFile.getPath();
		} else {
			/* If the targetFile is NULL then put it in the Validator's folder */
			folderKey = DEFAULT_TOOL_FOLDER_KEY;
		}
		MddfTarget parent = target.getParentTarget();
		while (parent != null) {
			File parentFile = parent.getSrcFile();
			if (targetFile != null) {
				folderKey = folderKey + KEY_SEP + parentFile.getPath();
				parent = parent.getParentTarget();
			} else {
				/* If the targetFile is NULL then put it in the Validator's folder */
				folderKey = DEFAULT_TOOL_FOLDER_KEY;
				parent = null;
			}
		}
		return folderKey;
	}

	public static int resolveLineNumber(Object targetData) {
		int lineNum = -1;
		if (targetData != null) {
			if (targetData instanceof Located) {
				lineNum = ((Located) targetData).getLine();
			} else if (targetData instanceof Cell) {
				/*
				 * Add 1 to line number for display purposes. Code is zero-based index but Excel
				 * spreadsheet displays using 1 as the 1st row.
				 */
				lineNum = ((Cell) targetData).getRowIndex() + 1;
				/* Prefix an 'explanation' with column ID (e.g., 'X', 'AA') */
				int colNum = ((Cell) targetData).getColumnIndex();
				String prefix = "Column " + LogMgmt.mapColNum(colNum);
			} else if (targetData instanceof Integer) {
				lineNum = ((Integer) targetData).intValue();
			}
		}
		return lineNum;
	}

}
