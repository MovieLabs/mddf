/**
 * Created Feb 27, 2017 
 * Copyright Motion Picture Laboratories, Inc. 2017
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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import com.movielabs.mddf.tools.util.FileChooserDialog;
import com.movielabs.mddflib.Obfuscator;
import com.movielabs.mddflib.Obfuscator.DataTarget;

import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.awt.event.ActionEvent;

public class MaskerDialog extends JDialog {

	private static MaskerDialog singleton;
	private final JPanel contentPanel = new JPanel();
	private JTextField destTextField;
	private ArrayList<JCheckBox> cBoxList = new ArrayList<JCheckBox>();
	private HashMap<DataTarget, JCheckBox> cBoxMap = new HashMap<DataTarget, JCheckBox>();
	private HashMap<DataTarget, JTextField> tFieldMap = new HashMap<DataTarget, JTextField>();
	private JTextField fileNameField;
	private String ttipFileName = "Default name is same as input file's. Change either the file name or the directory unless you want to overwite the input file.";
	private File curSrcFile;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			MaskerDialog dialog = new MaskerDialog();
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static MaskerDialog getDialog() {
		if (singleton == null) {
			singleton = new MaskerDialog();
			singleton.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		}
		return singleton;
	}

	/**
	 * Create the dialog.
	 */
	MaskerDialog() {
		setModalityType(ModalityType.APPLICATION_MODAL);
		setTitle("Avails Masking");
		setBounds(100, 100, 593, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWeights = new double[] { 0.0, 1.0, 1.0, 0.0 };
		contentPanel.setLayout(gbl_contentPanel);

		JLabel lblOutputLocation = new JLabel("Output Location:");
		GridBagConstraints gbc_lblOutputLocation = new GridBagConstraints();
		gbc_lblOutputLocation.insets = new Insets(0, 0, 5, 5);
		gbc_lblOutputLocation.gridx = 0;
		gbc_lblOutputLocation.gridy = 1;
		contentPanel.add(lblOutputLocation, gbc_lblOutputLocation);
		JLabel lblNewLabel_1 = new JLabel("Folder:");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 2;
		contentPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);
		destTextField = new JTextField();
		GridBagConstraints gbc_destField = new GridBagConstraints();
		gbc_destField.gridwidth = 2;
		gbc_destField.anchor = GridBagConstraints.WEST;
		gbc_destField.insets = new Insets(0, 0, 5, 5);
		gbc_destField.fill = GridBagConstraints.HORIZONTAL;
		gbc_destField.gridx = 1;
		gbc_destField.gridy = 2;
		contentPanel.add(destTextField, gbc_destField);
		destTextField.setColumns(10);
		JButton btnBrowse = new JButton("Browse..");
		GridBagConstraints gbc_btnBrowse = new GridBagConstraints();
		gbc_btnBrowse.insets = new Insets(0, 0, 5, 0);
		gbc_btnBrowse.gridx = 3;
		gbc_btnBrowse.gridy = 2;
		contentPanel.add(btnBrowse, gbc_btnBrowse);
		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectDestination();
			}
		});

		JLabel lblNewLabel_2 = new JLabel("Name:");
		lblNewLabel_2.setToolTipText(ttipFileName);
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 3;
		contentPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);

		fileNameField = new JTextField();
		fileNameField.setToolTipText(ttipFileName);
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.gridwidth = 2;
		gbc_textField.insets = new Insets(0, 0, 5, 5);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 3;
		contentPanel.add(fileNameField, gbc_textField);
		fileNameField.setColumns(10);

		/* Add a check-box for all supported versions */
		addFieldSelectors();

		JButton clearAllBtn = new JButton("Clear All");
		clearAllBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < cBoxList.size(); i++) {
					cBoxList.get(i).setSelected(false);
				}
			}
		});

		GridBagConstraints gbc_btnClear = new GridBagConstraints();
		gbc_btnClear.insets = new Insets(10, 3, 0, 5);
		gbc_btnClear.gridx = 1;
		gbc_btnClear.gridy = 8;
		contentPanel.add(clearAllBtn, gbc_btnClear);

		JButton selectAllBtn = new JButton("Select All");
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.insets = new Insets(10, 3, 0, 5);
		gbc_btnNewButton.gridx = 2;
		gbc_btnNewButton.gridy = 8;
		contentPanel.add(selectAllBtn, gbc_btnNewButton);
		selectAllBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < cBoxList.size(); i++) {
					JCheckBox cbx = cBoxList.get(i);
					cbx.setSelected(true);
				}
			}
		});

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		{
			JButton okButton = new JButton("OK");
			okButton.setActionCommand("OK");
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					genMaskedAvails();
					setVisible(false);
				}
			});
			buttonPane.add(okButton);
			getRootPane().setDefaultButton(okButton);
		}
		{
			JButton cancelButton = new JButton("Cancel");
			cancelButton.setActionCommand("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// reset to initial selections before closing
					setVisible(false);
				}
			});
			buttonPane.add(cancelButton);
		}
	}

	private void addFieldSelectors() {

		JLabel lblField = new JLabel("Field:");
		GridBagConstraints gbc_lblField = new GridBagConstraints();
		gbc_lblField.insets = new Insets(0, 0, 5, 5);
		gbc_lblField.gridx = 1;
		gbc_lblField.gridy = 4;
		contentPanel.add(lblField, gbc_lblField);

		JLabel lblReplacementValue = new JLabel("Replacement Value:");
		GridBagConstraints gbc_lblReplacementValue = new GridBagConstraints();
		gbc_lblReplacementValue.insets = new Insets(0, 0, 5, 5);
		gbc_lblReplacementValue.gridx = 2;
		gbc_lblReplacementValue.gridy = 4;
		contentPanel.add(lblReplacementValue, gbc_lblReplacementValue);
		// ....................................
		DataTarget target = DataTarget.Money;
		JCheckBox cbTermMoney = new JCheckBox(target.toString());
		cbTermMoney.setHorizontalAlignment(SwingConstants.LEFT);
		cbTermMoney.setToolTipText(target.getToolTip());
		GridBagConstraints gbc_cbTermMoney = new GridBagConstraints();
		gbc_cbTermMoney.gridwidth = 1;
		gbc_cbTermMoney.gridx = 1;
		gbc_cbTermMoney.gridy = 5;
		gbc_cbTermMoney.insets = new Insets(3, 6, 5, 6);
		contentPanel.add(cbTermMoney, gbc_cbTermMoney);
		cBoxList.add(cbTermMoney);
		cBoxMap.put(target, cbTermMoney);

		JTextField textField_Money = new JTextField();
		GridBagConstraints gbc_textField_Money = new GridBagConstraints();
		gbc_textField_Money.insets = new Insets(0, 0, 5, 5);
		gbc_textField_Money.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_Money.gridx = 2;
		gbc_textField_Money.gridy = 5;
		contentPanel.add(textField_Money, gbc_textField_Money);
		textField_Money.setColumns(10);
		tFieldMap.put(target, textField_Money);

		// ....................................
		target = DataTarget.ContractID;
		JCheckBox cbTransContract = new JCheckBox(target.toString());
		cbTransContract.setHorizontalAlignment(SwingConstants.LEFT);
		cbTransContract.setToolTipText(target.getToolTip());
		GridBagConstraints gbc_cbTransContract = new GridBagConstraints();
		gbc_cbTransContract.gridwidth = 1;
		gbc_cbTransContract.gridx = 1;
		gbc_cbTransContract.gridy = 6;
		gbc_cbTransContract.insets = new Insets(3, 6, 5, 6);
		contentPanel.add(cbTransContract, gbc_cbTransContract);
		cBoxList.add(cbTransContract);
		cBoxMap.put(target, cbTransContract);

		JTextField textField_Contract = new JTextField();
		GridBagConstraints gbc_textField_Contract = new GridBagConstraints();
		gbc_textField_Contract.insets = new Insets(0, 0, 5, 5);
		gbc_textField_Contract.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_Contract.gridx = 2;
		gbc_textField_Contract.gridy = 6;
		contentPanel.add(textField_Contract, gbc_textField_Contract);
		textField_Contract.setColumns(10);
		tFieldMap.put(target, textField_Contract);
	}

	protected void selectDestination() {
		File targetDir = FileChooserDialog.getPath("Source Folder", null, null, "srcManifest", this,
				JFileChooser.DIRECTORIES_ONLY);
		if (targetDir != null) {
			destTextField.setText(targetDir.getAbsolutePath());
		}

	}

	protected void genMaskedAvails() {
		/*
		 * 'curSrcFile' will be used as the input file. The output file has a
		 * name that is same as inputs but with the addition of the specified
		 * prefix.
		 */
		String outFileName = fileNameField.getText();
		if (!outFileName.endsWith(".xml")) {
			outFileName = outFileName + ".xml";
		}
		String outDirPath = destTextField.getText();
		File outputFile = new File(outDirPath, outFileName);
		// need to get replacement strings....
		Map<DataTarget, String> replacementMap = new HashMap<DataTarget, String>();
		Iterator<DataTarget> keyIt = cBoxMap.keySet().iterator();
		while (keyIt.hasNext()) {
			DataTarget key = keyIt.next();
			JCheckBox cbx = cBoxMap.get(key);
			if (cbx.isSelected()) {
				JTextField jtf = tFieldMap.get(key);
				replacementMap.put(key, jtf.getText());
			}
		}
		ValidationController ctrl = ValidatorTool.getTool().getController();
		ctrl.obfuscateAvail(curSrcFile, outputFile, replacementMap);
	}

	public void setContext(File srcFile) {
		curSrcFile = srcFile;
		String srcFileName = srcFile.getName();
		// strip file-type suffix...
		srcFileName = srcFileName.replaceAll("\\.\\w+$", "");
		fileNameField.setText(srcFileName);
		if (destTextField.getText().isEmpty()) {
			destTextField.setText(srcFile.getParent());
		}
		// first enable all..
		for (int i = 0; i < cBoxList.size(); i++) {
			JCheckBox cbx = cBoxList.get(i);
			cbx.setEnabled(true);
		}
	}
}
