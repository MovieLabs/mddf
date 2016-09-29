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

import org.jdom2.Element;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public interface LogMgmt {

	public static String[] logLevels = { "Debug", "Warning", "Error", "Info" };
	public static final int LEV_DEBUG = 0;
	public static final int LEV_WARN = 1;
	public static final int LEV_ERR = 2;
	public static final int LEV_INFO = 3;

	public static String[] logTags = { "Manifest", "CPE Model", "Profile", "Best Prac.", "Action", "Other", "Avail",
			"Common MD" };
	public static final int TAG_MANIFEST = 0;
	public static final int TAG_MODEL = 1;
	public static final int TAG_PROFILE = 2;
	public static final int TAG_BEST = 3;
	public static final int TAG_ACTION = 4; // user triggered some action via
											// GUI
	public static final int TAG_N_A = 5;
	public static final int TAG_AVAIL = 6;
	public static final int TAG_MD = 7;

	/**
	 * @param levInfo
	 * @param tagAvail
	 * @param msg
	 * @param curFile
	 * @param moduleId
	 */
	void log(int levInfo, int tagAvail, String msg, File curFile, String moduleId);

	/**
	 * @param level
	 * @param tag
	 * @param msg
	 * @param file
	 * @param lineNumber
	 * @param moduleId
	 * @param details
	 * @param srcRef
	 */
	void log(int level, int ltag, String msg, File file, int lineNumber, String moduleId, String details,
			LogReference srcRef);

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
	void logIssue(int tag, int level, Object target, String msg, String explanation, LogReference srcRef,
			String moduleId);

	/** 
	 */
	public void clearLog();

	/**
	 * @param fileName
	 * @return
	 */
	public LogEntryFolder getFileFolder(String fileName);

	/**
	 * Save the log messages in the desired location and format.
	 * 
	 * @param outFile
	 * @param format
	 * @throws IOException
	 */
	public void saveAs(File outFile, String format) throws IOException;

	/**
	 * Set the file currently being processed. Until the next
	 * invocation of <tt>setCurrentFile()</tt>, all subsequent log entries will
	 * be associated with this file.
	 * 
	 * @param curFileID
	 */
	public void setCurrentFile(File srcfile);

	/**
	 * @param levDebug
	 */
	public void setMinLevel(int level);

	public int getMinLevel();
}
