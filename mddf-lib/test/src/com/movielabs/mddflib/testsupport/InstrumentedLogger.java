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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.jdom2.located.Located;

import com.movielabs.mddflib.logging.DefaultLogging;
import com.movielabs.mddflib.logging.LogEntryFolder;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.xml.MddfTarget;

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
	private List<String> msgList;
	private PrintWriter logWriter;

	public InstrumentedLogger() {
		clearLog();
		minLevel = LogMgmt.LEV_NOTICE; // default
	}

	public InstrumentedLogger(File saveFile) {
		try {
			logWriter = new PrintWriter(saveFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		clearLog();
		minLevel = LogMgmt.LEV_NOTICE; // default
	}

	/* (non-Javadoc)
	 * @see com.movielabs.mddflib.logging.LogMgmt#assignFileFolder(com.movielabs.mddflib.util.xml.MddfTarget)
	 */
	@Override
	public LogEntryFolder assignFileFolder(MddfTarget target) {
		String id = target.getSrcFile().getName();
		 LogEntryFolder folder  = new LogEntryFolder(id, -1, "ID_"+id); 
		 return folder;
	}

	/**
	 * @param level
	 * @param tag
	 * @param line
	 * @param msg
	 */
	private void record(int level, int tag, int line, String msg) {
		if (level < 0) {
			// good place for breakpoint when debugging
			int foo = 0;
		}
		countByLevel[level]++;
		countByTag[tag]++;
		if (level < minLevel) {
			return;
		}
		if (level == LogMgmt.LEV_ERR || level == LogMgmt.LEV_FATAL) {
			// good place for breakpoint when debugging
			int foo = 0;
		}
		String key = line + ":" + tag + ":" + level;
		msgMap.put(key, msg);
		msgList.add("ILOG:  " + LogMgmt.logLevels[level].toUpperCase() + ":\t line " + line + ": " + msg);
		if (printToConsole) {
			System.out.println("ILOG:  " + LogMgmt.logLevels[level].toUpperCase() + ":\t line " + line + ": " + msg);
		}
		if (logWriter != null) {
			logWriter
					.write("ILOG:  " + LogMgmt.logLevels[level].toUpperCase() + ":\t line " + line + ": " + msg + "\n");
			logWriter.flush();
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

	/**
	 * Return number of log entries tagged with the specified severity level
	 * 
	 * @param level
	 * @return
	 */
	public int getCountForLevel(int level) {
		return countByLevel[level];
	}

	public int getCountForTag(int tag) {
		return countByTag[tag];
	}
	protected void append(int level, int tag, String msg, LogEntryFolder fileFolder, int line, String moduleID,
			String details, LogReference srcRef) {

		record(level, tag, line, msg);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#log(int, int, java.lang.String,
	 * java.io.File, java.lang.String)
	 */
	@Override
	public void log(int levInfo, int logTag, String msg, MddfTarget targetFile, String moduleId) {
		record(levInfo, logTag, -1, msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#log(int, int, java.lang.String,
	 * java.io.File, int, java.lang.String, java.lang.String,
	 * com.movielabs.mddflib.logging.LogReference)
	 */
	@Override
	public void log(int level, int ltag, String msg, MddfTarget targetFile, Object targetData, String moduleId,
			String details, LogReference srcRef) {
		int lineNum = LogMgmt.resolveLineNumber(targetData);
		record(level, ltag, lineNum, msg);
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
		msgList = new ArrayList<String>();
	}

	public void closeLog() {
		if (logWriter != null) {
			logWriter.flush();
			logWriter.close();
		}
	}

	/**
	 * Print log to console
	 */
	public void printLog() {
		for (int i = 0; i < msgList.size(); i++) {
			System.out.println(msgList.get(i));
		}
	}

	/**
	 * @param logFile
	 */
	public void saveLog(File logFile) {
		PrintWriter po;
		try {
			po = new PrintWriter(logFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		for (int i = 0; i < msgList.size(); i++) {
			po.println(msgList.get(i));
		}
		po.flush();
		po.close();
	}
 
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#saveAs(java.io.File,
	 * java.lang.String)
	 */
	@Override
	public void saveAs(File outFile, String format) throws IOException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#setCurrentFile(java.io.File)
	 */
	@Override
	public void setCurrentFile(File srcfile, boolean clear) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#getMinLevel()
	 */
	@Override
	public int getMinLevel() {
		throw new UnsupportedOperationException();
	}

//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see com.movielabs.mddflib.logging.LogMgmt#setInfoIncluded(boolean)
//	 */
//	@Override
//	public void setInfoIncluded(boolean flag) {
//		// ignore as we always do this
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#isInfoIncluded()
	 */
	@Override
	public boolean isInfoIncluded() {
		throw new UnsupportedOperationException();
	}

}
