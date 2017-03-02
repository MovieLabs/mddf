/**
 * Created Sep 13, 2016 
 * Copyright Motion Picture Laboratories, Inc. 2016
 * 
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
package com.movielabs.mddf.tools;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.io.IOException;
import java.util.List;

import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.movielabs.mddflib.logging.LogMgmt;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class AvailsTool extends ValidatorTool {

	public static final String AVAIL_APP_VER = "v1.1.2";
	private static final String AVAIL_DOC_VER = "v1.1";

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			System.out.println("Error setting Java LAF: " + e);
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					tool = new AvailsTool();
					tool.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * @param context
	 */
	public AvailsTool() {
		super(Context.AVAILS);
		super.appVersion = AVAIL_APP_VER;
		htmlDocUrl = "http://www.movielabs.com/md/avails/validator/" + AVAIL_DOC_VER + "/";
		/*
		 * allow use of XLSX formatted Avails files..
		 */
		inputFileFilter = new FileNameExtensionFilter("Avails file", "xml", "xlsx");
	}

	protected JToolBar getValidationTools() {
		if (validatorToolBar == null) {
			super.getValidationTools();

			// nothing to add.
		}
		return validatorToolBar;
	}

	public void runTool() {
		frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		String srcPath = fileInputDir.getAbsolutePath();
		updateUsageHistory(srcPath);
		fileOutDir.getAbsolutePath();
		controller = getController();
		controller.setValidation(true, true, false);
		inputSrcTFieldLocked = true;
		consoleLogger.collapse(); 
		// ....................................................
		runInBackground(srcPath, null, null);
		// .................................................
		frame.setCursor(null); // turn off the wait cursor
		consoleLogger.expand();
		inputSrcTFieldLocked = false;
	}
}
