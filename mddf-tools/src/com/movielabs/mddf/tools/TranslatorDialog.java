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
import java.awt.Color;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddf.tools.util.FileChooserDialog;

import javax.swing.border.EtchedBorder;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.awt.event.ActionEvent;

public class TranslatorDialog extends JDialog {

	private static TranslatorDialog singleton;
	private final JPanel contentPanel = new JPanel();
	private JTextField destTextField;
	private ArrayList<JCheckBox> cBoxList = new ArrayList<JCheckBox>();
	private HashMap<FILE_FMT, JCheckBox> cBoxMap = new HashMap<FILE_FMT, JCheckBox>();
	private JTextField fileNameField;
	private String ttipFileName = "File names will be prefixed with this name followed by a version ID and a format suffix (e.g., 'xyz_vx.y.xlsx')";
	private EnumSet<FILE_FMT> selections;
	private JLabel curFmtLabel;
	private String curFmtPrefix = "Current Format: ";
	private FILE_FMT curFmt;
	private static Set<JCheckBox>supported = new HashSet();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			TranslatorDialog dialog = new TranslatorDialog();
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static TranslatorDialog getDialog() {
		if (singleton == null) {
			singleton = new TranslatorDialog();
			singleton.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		}
		return singleton;
	}

	/**
	 * Create the dialog.
	 */
	TranslatorDialog() {
		setModalityType(ModalityType.APPLICATION_MODAL);
		setTitle("Avails Translation");
		setBounds(100, 100, 593, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0 };
		contentPanel.setLayout(gbl_contentPanel);
		curFmtLabel = new JLabel(curFmtPrefix);
		curFmtLabel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		curFmtLabel.setBackground(Color.WHITE);
		curFmtLabel.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_curFmtLabel = new GridBagConstraints();
		gbc_curFmtLabel.fill = GridBagConstraints.HORIZONTAL;
		gbc_curFmtLabel.anchor = GridBagConstraints.LINE_START;
		gbc_curFmtLabel.insets = new Insets(2, 2, 15, 2);
		gbc_curFmtLabel.gridwidth = 4;
		gbc_curFmtLabel.gridx = 0;
		gbc_curFmtLabel.gridy = 0;
		contentPanel.add(curFmtLabel, gbc_curFmtLabel);

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

		JLabel lblNewLabel_2 = new JLabel("Name Prefix:");
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
		addVersionSelctors();

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
					cbx.setSelected(supported.contains(cbx));
				}
				// then clear the checkBox for the curFmt
				JCheckBox cBox = cBoxMap.get(curFmt);
				if (cBox != null) {
					cBox.setSelected(false);
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
					Iterator<FILE_FMT> typeIt = cBoxMap.keySet().iterator();
					while (typeIt.hasNext()) {
						FILE_FMT nextType = typeIt.next();
						JCheckBox cb = cBoxMap.get(nextType);
						if (cb.isSelected()&& cb.isEnabled()) {
							selections.add(nextType);
						}

					}
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

	private void addVersionSelctors() {
		JCheckBox cbXlsxV1_7 = new JCheckBox("XLSX 1.7");
		GridBagConstraints gBC1 = new GridBagConstraints();
		gBC1.gridwidth = 1;
		gBC1.gridx = 1;
		gBC1.gridy = 5;
		gBC1.insets = new Insets(3, 6, 5, 6);
		contentPanel.add(cbXlsxV1_7, gBC1);
		cBoxList.add(cbXlsxV1_7);
		cBoxMap.put(FILE_FMT.AVAILS_1_7, cbXlsxV1_7);

		JCheckBox cbXlsxV1_6 = new JCheckBox("XLSX 1.6");
		GridBagConstraints gBC2 = new GridBagConstraints();
		gBC2.gridwidth = 1;
		gBC2.gridx = 1;
		gBC2.gridy = 6;
		gBC2.insets = new Insets(3, 6, 5, 6);
		contentPanel.add(cbXlsxV1_6, gBC2);
		cBoxList.add(cbXlsxV1_6);
		cBoxMap.put(FILE_FMT.AVAILS_1_6, cbXlsxV1_6);

		JCheckBox cbXmlV2_2 = new JCheckBox("XML 2.2");
		GridBagConstraints gBC3 = new GridBagConstraints();
		gBC3.gridwidth = 1;
		gBC3.gridx = 2;
		gBC3.gridy = 5;
		gBC3.insets = new Insets(3, 6, 5, 6);
		contentPanel.add(cbXmlV2_2, gBC3);
		cBoxList.add(cbXmlV2_2);
		cBoxMap.put(FILE_FMT.AVAILS_2_2, cbXmlV2_2);

		JCheckBox cbXmlV2_2_1 = new JCheckBox("XML 2.2.1");
		GridBagConstraints gBC4 = new GridBagConstraints();
		gBC4.gridwidth = 1;
		gBC4.gridx = 2;
		gBC4.gridy = 6;
		gBC4.insets = new Insets(3, 6, 5, 6);
		contentPanel.add(cbXmlV2_2_1, gBC4);
		cBoxList.add(cbXmlV2_2_1);
		cBoxMap.put(FILE_FMT.AVAILS_2_2_1, cbXmlV2_2_1);

		/* TEMPORARY during dev... indicate which are currently working */
		supported.add(cbXlsxV1_7);
		supported.add(cbXmlV2_2);
	}

	protected void selectDestination() {
		File targetDir = FileChooserDialog.getPath("Source Folder", null, null, "srcManifest", this,
				JFileChooser.DIRECTORIES_ONLY);
		if (targetDir != null) {
			destTextField.setText(targetDir.getAbsolutePath());
		}

	}

	public void setContext(FILE_FMT curFmt, File srcFile) {
		this.curFmt = curFmt;
		String srcFileName = srcFile.getName();
		// strip file-type suffix...
		srcFileName = srcFileName.replaceAll("\\.\\w+$", "");
		fileNameField.setText(srcFileName);
		if (destTextField.getText().isEmpty()) {
			destTextField.setText(srcFile.getParent());
		}  
		curFmtLabel.setText(curFmtPrefix+ curFmt.toString());
		// first enable all..
		for (int i = 0; i < cBoxList.size(); i++) {
			JCheckBox cbx = cBoxList.get(i);
			cbx.setEnabled(supported.contains(cbx));
		}
		// then disable the checkBox for the curFmt
		JCheckBox cBox = cBoxMap.get(curFmt);
		if (cBox != null) {
			cBox.setEnabled(false);
		}
		selections = EnumSet.noneOf(FILE_FMT.class);
	}

	public EnumSet<FILE_FMT> getSelections() {
		return selections;
	}

	public String getOutputDir() {
		return destTextField.getText();
	}

	public String getOutputFilePrefix() {
		return fileNameField.getText();
	}

}
