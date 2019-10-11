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
import java.util.HashMap;
import java.util.List;

import com.movielabs.mddflib.logging.LogEntryFolder;
import com.movielabs.mddflib.logging.LogEntryNode;
import com.movielabs.mddflib.util.xml.MddfTarget;

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
	private HashMap<String, SimpleXmlEditor> editorMap = new HashMap<String, SimpleXmlEditor>();

	public static EditorMgr getSingleton() {
		if (singleton == null) {
			singleton = new EditorMgr();
		}
		return singleton;
	}
  

	public SimpleXmlEditor getEditorFor(MddfTarget target,  Component guiParent, LogEntryNode logEntry) {
		// Key is the MddfTarget's 'key'
		String key = target.getKey();
		if (key == null || (key.isEmpty())) {
			return null;
		}
		SimpleXmlEditor editor = editorMap.get(key);

		if (editor == null) {
			editor = SimpleXmlEditor.spawn(target);
			if (editor == null) {
				// probably a problem with the underlying XML file
				return null;
			}
			editorMap.put(key, editor);
			editor.setMonitor(this);
			if (guiParent != null) {
				Point point = guiParent.getLocationOnScreen();
				int offset = editorMap.size() * 20;
				point.translate(offset, offset);
				editor.setLocation(point);
			}
		}
		/* Make sure the displayed line markers are up-to-date */
		LogEntryFolder folder = target.getLogFolder();
		List<LogEntryNode> msgList = folder.getMsgList();
		editor.showLogMarkers(msgList);
		
		if (logEntry != null) {
			LogEntryFolder file1 =  logEntry.getFolder();
			LogEntryFolder fileEntry = (LogEntryFolder) file1.getPath()[1]; 
			editor.goTo((LogEntryNode) logEntry);
		} else { 
			if (!msgList.isEmpty()) {
				editor.goTo(msgList.get(0));
			} else {
				editor.goTo(0);
			}
		}
		return editor; 
	}

	/**
	 * Returns the editor instance for the designated file. If one does not already
	 * exist, a <tt>null</tt> value is returned.
	 * 
	 * @param filePath
	 * @return
	 */
	public SimpleXmlEditor getEditorFor(MddfTarget target) {
		SimpleXmlEditor editor = editorMap.get(target.getKey());
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
		editorMap.remove(editor.getMddfTarget().getKey());
	}
}
