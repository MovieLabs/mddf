/**
 * Created Jan 25, 2017 
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
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import java.awt.Insets;
import java.awt.Point;

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
import java.awt.Label;
import java.awt.Font;
import java.awt.Panel;
import javax.swing.UIManager;
import java.awt.Checkbox;

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
	private JPanel ctrlBtnPanel;
	private JPopupMenu xmlFmtMenu;
	private JButton selXmlFmtBtn;
	private JButton selExcelFmtBtn;
	private JPopupMenu excelFmtMenu;
	private JCheckBox addVersionToNameCBx;
	private static Set<JCheckBox> supported = new HashSet<JCheckBox>();

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
		gbl_contentPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0 };
		gbl_contentPanel.columnWeights = new double[] { 0.0, 1.0, 0.0, 1.0 };
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

		JLabel lblNewLabel_2 = new JLabel("File Name(s):");
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
		addVersionSelectors();
		getContentPane().add(ctrlBtnPanel(), BorderLayout.SOUTH);
	}

	private JPanel ctrlBtnPanel() {
		if (ctrlBtnPanel == null) {
			ctrlBtnPanel = new JPanel();
			ctrlBtnPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

			JButton selectAllBtn = new JButton("Select All");
			ctrlBtnPanel.add(selectAllBtn);

			JButton clearAllBtn = new JButton("Clear All");
			ctrlBtnPanel.add(clearAllBtn);
			clearAllBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					for (int i = 0; i < cBoxList.size(); i++) {
						cBoxList.get(i).setSelected(false);
					}
				}
			});
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Iterator<FILE_FMT> typeIt = cBoxMap.keySet().iterator();
						while (typeIt.hasNext()) {
							FILE_FMT nextType = typeIt.next();
							JCheckBox cb = cBoxMap.get(nextType);
							if (cb.isSelected() && cb.isEnabled()) {
								selections.add(nextType);
							}

						}
						setVisible(false);
					}
				});
				ctrlBtnPanel.add(okButton);
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
				ctrlBtnPanel.add(cancelButton);
			}
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
		}
		return ctrlBtnPanel;
	}

	private void addVersionSelectors() {
		/*
		 * getting the menus now doesn't make them visible but will ensure that
		 * they are instantiated regardless of the order in which UI components
		 * (e.g the 'select all' button) are used.
		 */
		getXmlFormatMenu();
		getExcelFormatMenu();

		addVersionToNameCBx = new JCheckBox("Append version");
		addVersionToNameCBx.setToolTipText("version will be appended to file name(s)");
		addVersionToNameCBx.setFont(new Font("Dialog", Font.BOLD, 12));
		addVersionToNameCBx.setSelected(true);
		GridBagConstraints gbc_checkbox = new GridBagConstraints();
		gbc_checkbox.insets = new Insets(0, 0, 5, 0);
		gbc_checkbox.gridx = 3;
		gbc_checkbox.gridy = 3;
		contentPanel.add(addVersionToNameCBx, gbc_checkbox);

		Panel selectionPanel = new Panel();
		FlowLayout flowLayout = (FlowLayout) selectionPanel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		GridBagConstraints gbc_selectionPanel = new GridBagConstraints();
		gbc_selectionPanel.gridwidth = 0;
		gbc_selectionPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_selectionPanel.gridx = 0;
		gbc_selectionPanel.gridy = 4;
		contentPanel.add(selectionPanel, gbc_selectionPanel);

		Label selectionLabel = new Label("Selected formats:");
		selectionPanel.add(selectionLabel);
		selectionLabel.setFont(new Font("Dialog", Font.BOLD, 12));
		selectionPanel.add(getSelExcelFmtBtn());
		selectionPanel.add(getSelXmlFmtBtn());
	}

	private JButton getSelExcelFmtBtn() {
		if (selExcelFmtBtn == null) {
			selExcelFmtBtn = new JButton("Excel");
			selExcelFmtBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JPopupMenu menu = getExcelFormatMenu();
					if (!menu.isVisible()) {
						Point p = selExcelFmtBtn.getLocationOnScreen();
						menu.setInvoker(selExcelFmtBtn);
						menu.setLocation((int) p.getX(), (int) p.getY() + selExcelFmtBtn.getHeight());
						menu.setVisible(true);
					} else {
						menu.setVisible(false);
					}
				}
			});
		}
		return selExcelFmtBtn;
	}

	private JPopupMenu getExcelFormatMenu() {
		if (excelFmtMenu == null) {
			excelFmtMenu = new JPopupMenu();

			JCheckBox cbXlsxV1_7 = new JCheckBox("XLSX 1.7");
			excelFmtMenu.add(cbXlsxV1_7);
			cBoxList.add(cbXlsxV1_7);
			cBoxMap.put(FILE_FMT.AVAILS_1_7, cbXlsxV1_7);
			supported.add(cbXlsxV1_7);

			JCheckBox cbXlsxV1_7_2 = new JCheckBox("XLSX 1.7.2");
			excelFmtMenu.add(cbXlsxV1_7_2);
			cBoxList.add(cbXlsxV1_7_2);
			cBoxMap.put(FILE_FMT.AVAILS_1_7_2, cbXlsxV1_7_2);
			supported.add(cbXlsxV1_7_2);
		}
		return excelFmtMenu;
	}

	private JButton getSelXmlFmtBtn() {
		if (selXmlFmtBtn == null) {
			selXmlFmtBtn = new JButton("XML");
			selXmlFmtBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JPopupMenu menu = getXmlFormatMenu();
					if (!menu.isVisible()) {
						Point p = selXmlFmtBtn.getLocationOnScreen();
						menu.setInvoker(selXmlFmtBtn);
						menu.setLocation((int) p.getX(), (int) p.getY() + selXmlFmtBtn.getHeight());
						menu.setVisible(true);
					} else {
						menu.setVisible(false);
					}
				}
			});
		}
		return selXmlFmtBtn;
	}

	private JPopupMenu getXmlFormatMenu() {
		if (xmlFmtMenu == null) {
			xmlFmtMenu = new JPopupMenu();

			JCheckBox cbXmlV2_2_2 = new JCheckBox("XML 2.2.2");
			xmlFmtMenu.add(cbXmlV2_2_2);
			cBoxList.add(cbXmlV2_2_2);
			cBoxMap.put(FILE_FMT.AVAILS_2_2_2, cbXmlV2_2_2);
			supported.add(cbXmlV2_2_2);

			JCheckBox cbXmlV2_2_1 = new JCheckBox("XML 2.2.1");
			xmlFmtMenu.add(cbXmlV2_2_1);
			cBoxList.add(cbXmlV2_2_1);
			cBoxMap.put(FILE_FMT.AVAILS_2_2_1, cbXmlV2_2_1);
			supported.add(cbXmlV2_2_1);

			JCheckBox cbXmlV2_2 = new JCheckBox("XML 2.2");
			xmlFmtMenu.add(cbXmlV2_2);
			cBoxList.add(cbXmlV2_2);
			cBoxMap.put(FILE_FMT.AVAILS_2_2, cbXmlV2_2);
			supported.add(cbXmlV2_2);

		}
		return xmlFmtMenu;
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
		curFmtLabel.setText(curFmtPrefix + curFmt.toString());
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

	public boolean addVersion() {
		return addVersionToNameCBx.isSelected();
	}

}
