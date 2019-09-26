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
			"Common MD", "Rating", "XML", "MEC", "XLSX", "XLATE" };
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

	/**
	 * @param lev
	 * @param tag
	 * @param msg
	 * @param curFile
	 * @param moduleId
	 */
	void log(int lev, int tag, String msg, File curFile, String moduleId);

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
	void log(int level, int tag, String msg, File file, int lineNumber, String moduleId, String details,
			LogReference srcRef);

	/**
	 * Log an issue with a specific construct within a file. The <tt>target</tt>
	 * indicates the construct within the file and should be specified as either
	 * <ul>
	 * <li>an JDOM Element within an XML file, or</li>
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
//	void logIssue(int tag, int level, Object target, String msg, String explanation, LogReference srcRef,
//			String moduleId);

	/** 
	 */
	public void clearLog();

	/**
	 * @param targetFile
	 * @return a <tt>LogEntryFolder</tt>
	 */
	public LogEntryFolder getFileFolder(File targetFile);

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

//	/**
//	 * *
//	 * 
//	 * Set the file currently being processed and, if the file folder needs to be
//	 * created, append it as the <i>child</i> of the designated <tt>parentFile</tt>.
//	 * If a file folder already exists, the <tt>parentFile</tt> is ignored but the
//	 * <tt>clear</tt> flag will be used to determine if the existing contents of the
//	 * folder should be deleted.
//	 * 
//	 * @param srcfile
//	 * @param parentFile
//	 * @param clear
//	 * @return
//	 */
//	public abstract boolean addChildFile(File srcfile, File parentFile, boolean clear)
	public abstract LogEntryFolder pushFileContext(File targetFile, boolean clear);

	public abstract void popFileContext(File targetFile);
	
	/**
	 * @param level
	 */
	public void setMinLevel(int level);

	public int getMinLevel();

	public void setInfoIncluded(boolean flag);

	public boolean isInfoIncluded();

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
}
