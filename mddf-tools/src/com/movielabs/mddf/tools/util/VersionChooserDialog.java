/**
 * Copyright (c) 2019 MovieLabs

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
package com.movielabs.mddf.tools.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.tools.GenericTool;
import com.movielabs.mddflib.avails.xml.AvailsSheet;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;

public class VersionChooserDialog extends JDialog implements ActionListener {
	private JPanel choicePanel = null;
	private JPanel buttonPanel;
	private ButtonGroup btnGroup;
	private AvailsSheet.Version selectedVersion;
	private static String title = "Version Chooser";
	private static String logoPath = GenericTool.imageRsrcPath + "logo_movielabs.jpg";

	public VersionChooserDialog() {
		super(null, Dialog.ModalityType.APPLICATION_MODAL);
		setTitle(title);
		// center of screen (default may be overridden by caller)
		setLocationRelativeTo(null);
		// get the icon with the logo for the application
		ImageIcon appLogo = new ImageIcon(getClass().getResource(logoPath));
		initialize();

		// Add the header panel
		DialogHeaderPanel header = new DialogHeaderPanel(appLogo, title, "Avails XLSX version");
		getContentPane().add(header, BorderLayout.NORTH);

		getContentPane().add(getChoicePanel(), BorderLayout.CENTER);

		// Adds the button panel
		buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY), BorderFactory.createEmptyBorder(16, 8, 8, 8)));
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		// Adds OK button to close window
		JButton okButton = new JButton("OK");
		buttonPanel.add(okButton, BorderLayout.EAST);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		getRootPane().setDefaultButton(okButton);
	}

	private void initialize() {
		this.setSize(300, 500);

	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getChoicePanel() {
		if (choicePanel == null) {
			choicePanel = new JPanel();
			choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
			choicePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
			addButtons();
		}
		return choicePanel;
	}

	private void addButtons() {
		AvailsSheet.Version[] verEnums = AvailsSheet.Version.class.getEnumConstants();

		int numButtons = verEnums.length;
		JRadioButton[] radioButtons = new JRadioButton[numButtons];
		btnGroup = new ButtonGroup();
		for (int i = 0; i < numButtons; i++) {
			String nextVer = verEnums[i].toString();
			radioButtons[i] = new JRadioButton(nextVer);
			radioButtons[i].setActionCommand(verEnums[i].name());
			if (verEnums[i] == Version.UNK) {
				radioButtons[i].setSelected(true);
				selectedVersion = verEnums[i];
			} else {
				radioButtons[i].setSelected(false);
			}
			btnGroup.add(radioButtons[i]);
			radioButtons[i].addActionListener(this);
			choicePanel.add(radioButtons[i]);
		}
		// Select the first button by default.
//		radioButtons[0].setSelected(true);

	}

	public AvailsSheet.Version getSelected() {
		return selectedVersion;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String value = e.getActionCommand();
		selectedVersion = AvailsSheet.Version.valueOf(value);
	}
}
