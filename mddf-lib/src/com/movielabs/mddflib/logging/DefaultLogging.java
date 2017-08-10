/**
 * Created June 27, 2016 
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.jdom2.located.Located;

/**
 * Implements a basic logging capability not linked to a GUI. Intended usage is
 * to support logging functions when running from a CLI or as a cloud-based
 * service. Instances of this class therefore serve the same purpose as the
 * <tt>LogPanel</tt> does in a GUI environment.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class DefaultLogging implements LogMgmt {

	private LogEntryFolder rootLogNode = new LogEntryFolder("", -1);
	private int masterSeqNum;
	private List<LogEntryNode> entryList;
	private File curInputFile;
	protected int minLevel = LogMgmt.LEV_WARN;
	protected boolean printToConsole = false;
	protected boolean infoIncluded;
	private Map<File, LogEntryFolder> fileFolderMap = new HashMap<File, LogEntryFolder>();

	/**
	 * 
	 */
	public DefaultLogging() {
		clearLog();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#logXmlIssue(int, int,
	 * org.jdom2.Element, java.lang.String, java.lang.String,
	 * com.movielabs.mddflib.logging.LogReference)
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
		log(level, tag, msg, curInputFile, lineNum, moduleId, explanation, srcRef);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#log(int, int,
	 * java.lang.String, java.io.File, java.lang.String)
	 */
	@Override
	public void log(int level, int tag, String msg, File file, String moduleId) {
		append(level, tag, msg, file, -1, moduleId, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#log(int, int,
	 * java.lang.String, java.io.File, int, java.lang.String, java.lang.String,
	 * com.movielabs.mddflib.logging.LogReference)
	 */
	@Override
	public void log(int level, int tag, String msg, File file, int lineNumber, String moduleId, String details,
			LogReference srcRef) {
		append(level, tag, msg, file, lineNumber, moduleId, details, srcRef);
	}

	protected void append(int level, int tag, String msg, File xmlFile, int line, String moduleID, String details,
			LogReference srcRef) {
		if (level < minLevel) {
			return;
		}
		if (xmlFile == null) {
			// xmlFile = curInputFile;
			if (printToConsole) {
				System.out.println(LogMgmt.logLevels[level]+": "+msg);
			} 
			return;
		}
		// First get correct 'folder'
		String tagAsText = LogMgmt.logTags[tag];
		LogEntryFolder byManifestFile = getFileFolder(xmlFile);
		LogEntryFolder byLevel = (LogEntryFolder) byManifestFile.getChild(LogMgmt.logLevels[level]);
		LogEntryFolder tagNode = (LogEntryFolder) byLevel.getChild(tagAsText);
		if (tagNode == null) {
			tagNode = createTagNode(tag, level, byLevel);
		}
		LogEntryNode entryNode = new LogEntryNode(level, tagNode, msg, byManifestFile, line, moduleID, masterSeqNum++,
				details, srcRef);
		tagNode.addMsg(entryNode);
		/*
		 * add to the flat (i.e., sequential but non-hierarchical) list too...
		 */
		entryList.add(entryNode);
		if (printToConsole) {
			entryNode.print();
		} else if (level == LogMgmt.LEV_INFO) {
			System.out.println(msg);
		}
	}

	/**
	 * Add tag-specific node for previously unused folder
	 * 
	 * @param tagAsText
	 * @param level
	 * @param byLevel
	 * @return
	 */
	private LogEntryFolder createTagNode(int tag, int level, LogEntryFolder byLevel) {

		String tagAsText = LogMgmt.logTags[tag];
		LogEntryFolder tagNode = new LogEntryFolder(tagAsText, level);
		/*
		 * need to keep order consistent so figure where to insert. Allow for
		 * situation where it is the first node or the child set is empty
		 */
		int tagIdx = getTagIndex(tagAsText);
		byLevel.add(tagNode);
		return tagNode;
	}

	/**
	 * @param tagAsText
	 * @return
	 */
	private int getTagIndex(String tagAsText) {
		for (int i = 0; i < LogMgmt.logTags.length; i++) {
			if (tagAsText.equals(LogMgmt.logTags[i])) {
				return i;
			}
		}
		throw new IllegalArgumentException("Unsupported tag '" + tagAsText + "'");
	}

	public LogEntryFolder getFileFolder(File targetFile) {
		LogEntryFolder fileFolder = fileFolderMap.get(targetFile);
		if (fileFolder == null) {
			/*
			 * Deal with possibility of multiple files with the same name being
			 * processed (i.e. /foo/myFile.xml vs /bar/myFile.xml).
			 */
			int suffix = 1;
			String label = targetFile.getName();
			String qualifiedName = label;
			fileFolder = (LogEntryFolder) rootLogNode.getChild(label);
			while (fileFolder != null) {
				suffix++;
				qualifiedName = label + " (" + suffix + ")";
				fileFolder = (LogEntryFolder) rootLogNode.getChild(qualifiedName);
			}
			fileFolder = new LogEntryFolder(qualifiedName, -1);
			fileFolder.setFile(targetFile);
			fileFolderMap.put(targetFile, fileFolder);
			rootLogNode.add(fileFolder);
			for (int i = 0; i < LogMgmt.logLevels.length; i++) {
				LogEntryFolder levelTNode = new LogEntryFolder(LogMgmt.logLevels[i], i);
				fileFolder.add(levelTNode);
			}
		}
		return fileFolder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddf.util.UiLogger#clearLog()
	 */
	public void clearLog() {
		entryList = new ArrayList<LogEntryNode>();
		rootLogNode = new LogEntryFolder("", -1);
		masterSeqNum = 0;
	}

	/**
	 * Save the log messages in the desired location and format.
	 * 
	 * @param outFile
	 * @param format
	 * @throws IOException
	 */
	public void saveAs(File outFile, String format) throws IOException {
		switch (format) {
		case "csv":
			saveAsCsv(outFile);
			break;
		case "xml":
		default:
			// TO BE COMPLETED.......
		}
	}

	/**
	 * @param outFile
	 * @throws IOException
	 */
	private void saveAsCsv(File outFile) throws IOException {
		String suffix = ".csv";
		if (!outFile.getName().endsWith(suffix)) {
			String fPath = outFile.getAbsolutePath() + suffix;
			outFile = new File(fPath);
		}
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
		/* first row has column names */
		int cCnt = LogEntryNode.fieldNames.length;
		String colSep = LogEntryNode.colSep;
		String headerRow = LogEntryNode.fieldNames[0];
		for (int i = 1; i < cCnt; i++) {
			headerRow = headerRow + colSep + LogEntryNode.fieldNames[i];
		}
		/*
		 * 'Notes' is special case for the tooltip (a.k.a 'added detail' or
		 * 'drill-down')
		 */
		headerRow = headerRow + colSep + "Notes";
		writer.write(headerRow + "\n");
		/* add data rows */
		for (int i = 0; i < entryList.size(); i++) {
			LogEntryNode entry = entryList.get(i);
			String trimedRow = entry.toCSV();
			writer.write(trimedRow + "\n");
		}
		writer.flush();
		writer.close();

	}

	/**
	 * @param printToConsole
	 *            the printToConsole to set
	 */
	public void setPrintToConsole(boolean printToConsole) {
		this.printToConsole = printToConsole;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddf.util.Logger#setCurrentFile(java.lang.String)
	 */
	@Override
	public void setCurrentFile(File targetFile) {
		this.curInputFile = targetFile;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#setMinLevel(int)
	 */
	@Override
	public void setMinLevel(int level) {
		this.minLevel = level;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#getMinLevel()
	 */
	@Override
	public int getMinLevel() {
		return minLevel;
	}

	/**
	 * @return the infoIncluded
	 */
	public boolean isInfoIncluded() {
		return infoIncluded;
	}

	/**
	 * @param infoIncluded
	 *            the infoIncluded to set
	 */
	public void setInfoIncluded(boolean infoIncluded) {
		this.infoIncluded = infoIncluded;
	}

}
