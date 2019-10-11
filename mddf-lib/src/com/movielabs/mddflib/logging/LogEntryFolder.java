/**
 * Created Jun 30, 2016 
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jdom2.Document;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddf.MddfContext.MDDF_TYPE;
import com.movielabs.mddflib.util.xml.MddfTarget;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class LogEntryFolder extends LogEntry {
 
	private ArrayList<LogEntryNode> msgList;
	private int level; 
	private FILE_FMT mddfFormat; 
	private String key = null;
	private MddfTarget mddfTarget;

	public LogEntryFolder(String label, int severityLevel) {
		super(label);
		this.setTag(label);
		this.level = severityLevel;
		msgList = new ArrayList<LogEntryNode>();
	}

	/**
	 * @param label
	 * @param severityLevel
	 */
	public LogEntryFolder(String label, int severityLevel, String key) {
		super(label);
		this.setTag(label);
		this.level = severityLevel;
		this.key = key;
		msgList = new ArrayList<LogEntryNode>();
	}

	/**
	 * Override default implementation to return the folder's <tt>label</tt>
	 * prefixed with the current message count. For example, if the the folder's
	 * label is <tt>Errors</tt> and it contains 12 messages, the returned string is
	 * '[12] Errors'.
	 * 
	 * @see javax.swing.tree.DefaultMutableTreeNode#toString()
	 */
	public String toString() {
		int msgCnt = getMsgCnt();
		if (msgCnt > 0) {
			return "[" + msgCnt + "] " + getLabel();
		} else {
			return getLabel();
		}
	}

	/**
	 * Return the <tt>label</tt> (i.e., <i>name</i> of the folder.
	 * 
	 * @return
	 */
	public String getLabel() {
		return super.toString();
	}

	/**
	 * @param entryNode
	 */
	public void addMsg(LogEntryNode entryNode) {
		msgList.add(entryNode);
	}

	public MddfTarget getMddfTarget() {
		return mddfTarget;
	}

	public void setFile(MddfTarget myTarget) {
		if (mddfTarget != null) {
			// OK to replace but only if the KEYs match
			String curKey = LogMgmt.genFolderKey(mddfTarget);
			String newKey = LogMgmt.genFolderKey(myTarget);
			if (!newKey.equals(curKey)) {
				throw new IllegalStateException("Replacement MddfTarget has non-matching key");
			}
		}
		this.myFile = myTarget.getSrcFile();
		this.mddfTarget = myTarget;
	}

	public File getFile() {
		if (myFile != null) {
			return myFile;
		} else {
			// Must be an intermediate-level folder so go up
			LogEntryFolder fileEntry = (LogEntryFolder) getPath()[1];
			return fileEntry.myFile;
		}
	}
 
	public MDDF_TYPE getMddfType() {
		return mddfTarget.getMddfType();
	}

	public void setMddfFormat(FILE_FMT format) {
		mddfFormat = format;
	}


	public FILE_FMT getMddfFormat() {
		return mddfFormat;
	}

	/**
	 * Return the XML encoding of the MDDF file associated with this folder.
	 * 
	 * @return
	 */
	public Document getXml() {
		return mddfTarget.getXmlDoc();
	}

	public DefaultMutableTreeNode getChild(String id) {
		Enumeration<LogEntry> kinder = this.children();
		while (kinder.hasMoreElements()) {
			LogEntry nextNode = kinder.nextElement();
			if (nextNode.getTagAsText().equals(id)) {
				return (nextNode);
			}
		}
		return null;
	}

	/**
	 * @return
	 */
	public int getMsgCnt() {
		int msgCnt = msgList.size();
		Enumeration<LogEntry> kinder = this.children();
		while (kinder.hasMoreElements()) {
			LogEntryFolder nextChild = (LogEntryFolder) kinder.nextElement();
			msgCnt = msgCnt + nextChild.getMsgCnt();
		}
		return msgCnt;

	}

	/**
	 * @return
	 */
	public List<LogEntryNode> getMsgList() {
		List<LogEntryNode> fullList = new ArrayList<LogEntryNode>();
		fullList.addAll(msgList);
		Enumeration<LogEntry> kinder = this.children();
		while (kinder.hasMoreElements()) {
			LogEntryFolder nextChild = (LogEntryFolder) kinder.nextElement();
			fullList.addAll(nextChild.getMsgList());
		}
		return fullList;
	}

	/**
	 * Recursively determine if this node or any of its descendants contains any
	 * entries with a severity of <tt>LogMgmt.LEV_ERR</tt>.
	 * 
	 * @param forErrors
	 * @return
	 */
	public boolean hasErrorsMsgs(boolean forErrors) {
		/* is this a 'leaf' folder or intermediate? */
		Enumeration<LogEntry> kinder = this.children();
		if (kinder.hasMoreElements()) {
			boolean isErrFolder = this.getTagAsText().equals(LogMgmt.logLevels[LogMgmt.LEV_ERR]);
			while (kinder.hasMoreElements()) {
				LogEntryFolder nextChild = (LogEntryFolder) kinder.nextElement();
				if (nextChild.hasErrorsMsgs(forErrors || isErrFolder)) {
					return true;
				}
			}
		} else {
			if (msgList.isEmpty()) {
				return false;
			} else {
				/*
				 * if this folder or any ancestor folder has tag indicating UiLogger.LEV_ERR
				 * then return TRUE
				 */
				return forErrors;
			}
		}
		return false;
	}

	/**
	 * Return the highest level (i.e., severity) found among all log entries for
	 * this node and any descendant nodes.
	 * 
	 * @return
	 */
	public int getHighestLevel() {
		int highest = -1;
		/* is this a 'leaf' folder or intermediate? */
		Enumeration<LogEntry> kinder = this.children();
		if (kinder.hasMoreElements()) {
			LogEntryFolder infoFolder = null;
			while (kinder.hasMoreElements()) {
				LogEntryFolder nextChild = (LogEntryFolder) kinder.nextElement();
				if (nextChild.level != LogMgmt.LEV_INFO) {
					highest = Math.max(highest, nextChild.getHighestLevel());
				} else {
					infoFolder = nextChild;
				}
			}
			if ((infoFolder != null) && (highest <= LogMgmt.LEV_DEBUG) && (!infoFolder.msgList.isEmpty())) {
				highest = LogMgmt.LEV_INFO;
			}
		} else {
			if (msgList != null && (!msgList.isEmpty())) {
				highest = Math.max(highest, this.level);
			}
		}
		return highest;
	}

	/**
	 * 
	 */
	public void deleteMsgs() {
		msgList = new ArrayList<LogEntryNode>();
		Enumeration<LogEntry> kinder = this.children();
		while (kinder.hasMoreElements()) {
			LogEntryFolder nextChild = (LogEntryFolder) kinder.nextElement();
			nextChild.deleteMsgs();
		}

	}


}
