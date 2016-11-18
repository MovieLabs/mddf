/**
 * Created Aug 3, 2016 
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
package com.movielabs.mddf.tools.util.xml;

import java.awt.Component;
import java.awt.Point;
import java.io.File;
import java.util.HashMap;
import java.util.List;

import com.movielabs.mddf.tools.GenericTool;
import com.movielabs.mddf.tools.util.logging.LoggerWidget;
import com.movielabs.mddflib.logging.LogEntryFolder;
import com.movielabs.mddflib.logging.LogEntryNode;

/**
 * This class acts as the coordinator of editor instances. The primary function
 * is ensuring that there is never more than one editor instantiated for a given
 * file.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class EditorMgr implements EditorMonitor {

	private static EditorMgr singleton;
	private HashMap<String, SimpleXmlEditor> editorMap = new HashMap();

	public static EditorMgr getSingleton() {
		if (singleton == null) {
			singleton = new EditorMgr();
		}
		return singleton;
	}

	/**
	 * Display an editor showing the file associated with the specified
	 * <tt>LogEntryNode</tt> and then scroll to the line specified by the
	 * <tt>LogEntryNode</tt>. If an editor has not yet been created for the
	 * designated file, a new will be instantiated.
	 * 
	 * @param logEntry
	 */
	public SimpleXmlEditor getEditor(LogEntryNode logEntry, Component parent) {
		// Key is the ABSOLUTE path to XML file
		String key = logEntry.getManifestPath();
		if (key == null || (key.isEmpty())) {
			return null;
		}
		SimpleXmlEditor editor = editorMap.get(key);
		if (editor == null) {
			editor = SimpleXmlEditor.spawn(logEntry);
			editorMap.put(key, editor);
			editor.setMonitor(this);
			if (parent != null) {
				Point point = parent.getLocationOnScreen();
				int offset = editorMap.size() * 20;
				point.translate(offset, offset);
				editor.setLocation(point);
			}
		} else {
			editor.goTo(logEntry);
		}
		LogEntryFolder file1 = (LogEntryFolder) logEntry.getFolder();
		LogEntryFolder fileEntry = (LogEntryFolder) file1.getPath()[1];
		List<LogEntryNode> msgList = fileEntry.getMsgList();
		editor.showLogMarkers(msgList);
		return editor;
	}

	/**
	 * Display an editor showing the designated file. If an editor has not yet
	 * been created for the designated file, a new will be instantiated.
	 * 
	 * @param filePath
	 * @param parent
	 * @return
	 */
	public SimpleXmlEditor getEditor(String filePath, Component parent) {
		if (filePath == null || (filePath.isEmpty())) {
			return null;
		}
		SimpleXmlEditor editor = editorMap.get(filePath);
		if (editor == null) {
			editor = SimpleXmlEditor.spawn(filePath);
			editorMap.put(filePath, editor);
			editor.setMonitor(this);
			if (parent != null) {
				Point point = parent.getLocationOnScreen();
				int offset = editorMap.size() * 20;
				point.translate(offset, offset);
				editor.setLocation(point);
			}
		}
		/*
		 * Since there is no LogEntry, we need to get the LogEntryFolder in
		 * order to set any line markers.
		 */
		LoggerWidget logger = GenericTool.consoleLogger;
		File file = new File(filePath);
		LogEntryFolder logFolder = logger.getFileFolder(file.getName());
		List<LogEntryNode> msgList = logFolder.getMsgList();
		editor.showLogMarkers(msgList);
		return editor;
	}

	/**
	 * Returns the editor instance for the designated file. If one does not
	 * already exist, a <tt>null</tt> value is returned.
	 * 
	 * @param filePath
	 * @return
	 */
	public SimpleXmlEditor getEditorFor(String filePath) {
		SimpleXmlEditor editor = editorMap.get(filePath);
		return editor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddf.util.xml.EditorMonitor#editorHasClosed()
	 */
	@Override
	public void editorHasClosed(SimpleXmlEditor editor) {
		// Key is the ABSOLUTE path to XML file
		editorMap.remove(editor.getFilePath());

	}
}
