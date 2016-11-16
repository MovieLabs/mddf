/**
 * Created Sep 13, 2016 
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

package com.movielabs.mddf.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class ManifestTool extends ValidatorTool {
	private static final String MANIFEST_APP_VER = "v1.2";
	private static final String MANIFEST_DOC_VER = "v1.2";


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
					window = new ManifestTool();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}


	/**
	 * @param context
	 */
	public ManifestTool() {
		super(Context.MANIFEST);
		super.appVersion = MANIFEST_APP_VER; 
		htmlDocUrl = "http://www.movielabs.com/md/manifest/validator/" + MANIFEST_DOC_VER + "/";
	}

	/**
	 * Extends parent class implementation to include UI support specific to
	 * Manifest processing.
	 * 
	 * @see com.movielabs.mddf.preProcess.ValidatorTool#getValidationTools()
	 */
	protected JToolBar getValidationTools() {
		if (validatorToolBar == null) {
			super.getValidationTools();

			JLabel profileLabel = new JLabel("Profile:");
			profileLabel.setMaximumSize(new Dimension(71, 15));
			profileLabel.setPreferredSize(new Dimension(71, 15));
			profileLabel.setMinimumSize(new Dimension(71, 15));
			profileLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			profileLabel.setForeground(Color.BLUE);
			validatorToolBar.add(profileLabel);

			validatorToolBar.add(getProfileComboBox());
		}
		return validatorToolBar;
	}
	
	
	/* (non-Javadoc)
	 * @see com.movielabs.mddf.preProcess.ValidatorTool#getProcessingMenu()
	 */
	protected JMenu getProcessingMenu(){
		if(processingMenu==null){
			processingMenu = new JMenu("Processing"); 

			JMenu validationMenu = new JMenu("Validate for");
			processingMenu.add(validationMenu);

			validateConstraintsCBox = new JCheckBoxMenuItem("constraints");
			validateConstraintsCBox.setSelected(true);
			validationMenu.add(validateConstraintsCBox);

			validateBestPracCBox = new JCheckBoxMenuItem("Best Practices");
			validateBestPracCBox.setSelected(false);
			validateBestPracCBox.setEnabled(false);
			validationMenu.add(validateBestPracCBox);

			compressCBoxMenuItem = new JCheckBoxMenuItem("Compress");
			compressCBoxMenuItem.setToolTipText("generate JSON output without whitespace or linefeeds.");
			compressCBoxMenuItem.setEnabled(false);
			processingMenu.add(compressCBoxMenuItem);
		}
		return processingMenu;
	}
}
