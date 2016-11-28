/**
 * Created Oct 23, 2015 
 * Copyright Motion Picture Laboratories, Inc. 2015
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
package com.movielabs.mddf.tools.util;

import java.awt.Component;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * Simple wrapper around a <tt>JFileChooser</tt> that handles housekeeping
 * chores such as keeping track of default directories.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class FileChooserDialog {
	public static final String propPrefix = "fileChooser.";
	protected static Map<String, String> defaultDirMap = new HashMap<String, String>();

	/**
	 * Prompt user to select a file. If <tt>directoryPath</tt> is specified then
	 * that will be used as the starting point for file selection. If it is
	 * <tt>null</tt> then the <tt>key</tt> is used to look up the last directory
	 * associated with the specified key value. If one does not exist then the
	 * user's home directory is to be used as the starting point.
	 * <p>
	 * The optional <tt>filter</tt> is a standard Java <tt>FileFilter</tt> that
	 * gets passed to the <tt>JFileChooser</tt>. The <tt>parent</tt> (also
	 * optional) is used to position the dialog on the screen.
	 * </p>
	 * 
	 * @param promptText
	 * @param directoryPath
	 * @param filter
	 * @param key
	 * @param parent
	 * @return
	 */
	public static synchronized File getFilePath(String promptText, String directoryPath, FileFilter filter, Object key,
			Component parent) {
		File myFile = getPath(promptText, directoryPath, filter, key, parent, JFileChooser.FILES_ONLY);
		return myFile;
	}

	public static synchronized File getDirPath(String promptText, String directoryPath, FileFilter filter, Object key,
			Component parent) {
		File myFile = getPath(promptText, directoryPath, filter, key, parent, JFileChooser.DIRECTORIES_ONLY);
		return myFile;
	}

	public static synchronized File getPath(String promptText, String directoryPath, FileFilter filter, Object key,
			Component parent, int mode) {
		String myPath = null;
		if (directoryPath == null) {
			directoryPath = defaultDirMap.get(propPrefix + key);
			if (directoryPath == null) {
				directoryPath = System.getenv("HOME");
			}
			if (directoryPath == null) {
				// for Microsoft OS...
				directoryPath = System.getenv("HOMEPATH");
			}
		}
		JFileChooser myChooser = new JFileChooser(directoryPath);
		myChooser.setFileSelectionMode(mode);
		if (mode != JFileChooser.DIRECTORIES_ONLY) {
			if (filter != null) {
				myChooser.setFileFilter(filter);
			}
			File test = new File(directoryPath);
			/*
			 * The idea here is to show as already selected a default file name
			 * if a 'save-as' is being done. The problem is that doing a check
			 * for 'test.isFile()' will not work as a non-existent file would
			 * return FALSE. Hence the check for 'isDirectory' instead.
			 */
			if (!test.isDirectory()) {
				myChooser.setSelectedFile(test);
			}
		}
		Action details = myChooser.getActionMap().get("viewTypeDetails");
		details.actionPerformed(null);
		myChooser.setDialogTitle(promptText);
		int retval = myChooser.showDialog(parent, "OK");
		File myFile = null;
		if (retval == JFileChooser.APPROVE_OPTION) {
			myFile = myChooser.getSelectedFile();
			myPath = myFile.getPath();
			String newDefaultDir = myChooser.getCurrentDirectory().getPath();
			defaultDirMap.put(propPrefix + key, newDefaultDir);
		}
		return myFile;
	}

	public static void setDirMapping(String key, String path) {
		defaultDirMap.put(propPrefix + key, path);
	}

	/**
	 * @return Returns the defaultDirMap.
	 */
	public static Map<String, String> getDefaultDirMap() {
		return defaultDirMap;
	}

	/**
	 * Set the HashMap containing default directories. Typically this would be
	 * the same an application's <tt>Properties</tt> instance in which some of
	 * the properties begin with the appropriate prefix (i.e., "
	 * <tt>fileChooser.</tt>")
	 * 
	 * @param defaultDirMap
	 *            The defaultDirMap to set.
	 */
	public static synchronized void setDefaultDirMap(Map defaultDirMap) {
		FileChooserDialog.defaultDirMap = defaultDirMap;
	}
}
