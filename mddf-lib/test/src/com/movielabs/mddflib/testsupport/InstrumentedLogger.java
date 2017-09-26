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
package com.movielabs.mddflib.testsupport;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.jdom2.located.Located;

import com.movielabs.mddflib.logging.DefaultLogging;
import com.movielabs.mddflib.logging.LogEntryFolder;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;

/**
 * Logger that facilitates JUnit testing by providing <tt>getCountForXYZ()</tt>
 * methods.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class InstrumentedLogger extends DefaultLogging implements LogMgmt {

	private int[] countByLevel;
	private int[] countByTag;
	private Map<String, String> msgMap;

	public InstrumentedLogger() {
		clearLog();
	}

	/**
	 * @param level
	 * @param tag
	 * @param line
	 * @param msg
	 */
	private void record(int level, int tag, int line, String msg) {
		countByLevel[level]++;
		countByTag[tag]++;
		String key = line + ":" + tag + ":" + level;
		msgMap.put(key, msg);
		if (printToConsole) {
			System.out.println("ILOG--->" + LogMgmt.logLevels[level] + "(line " + line + ")" + "--" + msg);
			if (level == LogMgmt.LEV_FATAL) {
				System.out.println("DEAD AGAIN");
			}
		}
	}

	/**
	 * @param level
	 * @param tag
	 * @param line
	 * @return
	 */
	public String getMsg(int level, int tag, int line) {
		String key = line + ":" + tag + ":" + level;
		return (msgMap.get(key));
	}

	public int getCountForLevel(int level) {
		return countByLevel[level];
	}

	public int getCountForTag(int tag) {
		return countByTag[tag];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#log(int, int,
	 * java.lang.String, java.io.File, java.lang.String)
	 */
	@Override
	public void log(int levInfo, int logTag, String msg, File curFile, String moduleId) {
		record(levInfo, logTag, -1, msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#log(int, int,
	 * java.lang.String, java.io.File, int, java.lang.String, java.lang.String,
	 * com.movielabs.mddflib.logging.LogReference)
	 */
	@Override
	public void log(int level, int ltag, String msg, File file, int lineNumber, String moduleId, String details,
			LogReference srcRef) {
		record(level, ltag, lineNumber, msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#logIssue(int, int,
	 * java.lang.Object, java.lang.String, java.lang.String,
	 * com.movielabs.mddflib.logging.LogReference, java.lang.String)
	 */
	@Override
	public void logIssue(int tag, int level, Object target, String msg, String explanation, LogReference srcRef,
			String moduleId) {
		int lineNum = -1;
		if (target != null) {
			if (target instanceof Located) {
				lineNum = ((Located) target).getLine();
			} else if (target instanceof Cell) {
				lineNum = ((Cell) target).getRowIndex();
			}
		}
		if (explanation != null) {
			msg = msg + "; " + explanation;
		}
		record(level, tag, lineNum, msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#clearLog()
	 */
	@Override
	public void clearLog() {
		countByLevel = new int[LogMgmt.logLevels.length];
		countByTag = new int[LogMgmt.logTags.length];
		msgMap = new HashMap<String, String>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#getFileFolder(java.io.File)
	 */
	@Override
	public LogEntryFolder getFileFolder(File targetFile) {
		// Not used or required for JUnit tests
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#saveAs(java.io.File,
	 * java.lang.String)
	 */
	@Override
	public void saveAs(File outFile, String format) throws IOException {
		// Not used or required for JUnit tests
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#setCurrentFile(java.io.File)
	 */
	@Override
	public void setCurrentFile(File srcfile) {
		// Not used or required for JUnit tests

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#setMinLevel(int)
	 */
	@Override
	public void setMinLevel(int level) {
		// Not used or required for JUnit tests
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#getMinLevel()
	 */
	@Override
	public int getMinLevel() {
		// Not used or required for JUnit tests
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#setInfoIncluded(boolean)
	 */
	@Override
	public void setInfoIncluded(boolean flag) {
		// Not used or required for JUnit tests

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#isInfoIncluded()
	 */
	@Override
	public boolean isInfoIncluded() {
		// Not used or required for JUnit tests
		return false;
	}

}
