/**
 * Created Jul 21, 2016 
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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import javax.swing.border.EtchedBorder;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.border.TitledBorder;

import com.movielabs.mddf.tools.GenericTool;
import com.movielabs.mddf.tools.util.xml.SimpleXmlEditor.EditActionListener;

import java.awt.Toolkit;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class FindReplaceDialog extends JFrame {

	private JPanel contentPanel;
	private JPanel buttonPanel;
	private JPanel fieldPanel;
	private JPanel findPanel;
	private JTextField findTextField;
	private JTextField replaceTextField;
	private JPanel replacePanel;
	private JPanel optionsPanel;
	private JPanel actionsPanel;
	private JButton findNextBtn;
	private JButton findPrevBtn;
	private JButton replaceBtn;
	private JButton replAndFindBtn;
	private JButton replaceAllBtn;
	private Insets btnInsets = new Insets(1, 3, 1, 3);
	private Font ctrlFont = new Font("Dialog", Font.PLAIN, 11);
	private JCheckBox wordChkBox;
	private JCheckBox caseChkBox;
	private JCheckBox regexChkBox;
	private JLabel label_1;
	private JLabel statusLabel;
	private SimpleXmlEditor editor;
	private ActionListener editActionListener = null;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			FindReplaceDialog dialog = new FindReplaceDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	FindReplaceDialog() {
		this.editor = null;
		this.editActionListener = null;
		initializeGui();
	}

	/**
	 * @param simpleXmlEditor
	 */
	public FindReplaceDialog(SimpleXmlEditor xmlEditor) {
		this.editor = xmlEditor;
		editActionListener = editor.getEditActionListener();
		initializeGui();
	}

	/**
	 * 
	 */
	private void initializeGui() {
		setIconImage(Toolkit.getDefaultToolkit()
				.getImage(FindReplaceDialog.class.getResource(GenericTool.imageRsrcPath +"icon_movielabs.jpg")));
		setTitle("Find/Replace");
		setBounds(100, 100, 400, 267);
		BorderLayout borderLayout = new BorderLayout();
		borderLayout.setVgap(10);
		getContentPane().setLayout(borderLayout);
		getContentPane().add(getContentPanel(), BorderLayout.CENTER);
		getContentPane().add(getBtnPanel(), BorderLayout.SOUTH);
		getContentPane().add(getFieldPanel(), BorderLayout.NORTH);
	}

	private JPanel getFieldPanel() {
		if (fieldPanel == null) {
			fieldPanel = new JPanel();
			fieldPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
			fieldPanel.setLayout(new BorderLayout(0, 3));
			fieldPanel.add(getFindPanel(), BorderLayout.NORTH);
			fieldPanel.add(getReplacePanel(), BorderLayout.SOUTH);
		}
		return fieldPanel;
	}

	private JPanel getFindPanel() {
		if (findPanel == null) {
			findPanel = new JPanel();
			findPanel.setLayout(new BorderLayout(10, 0));
			JLabel findLabel = new JLabel("Find:");
			findLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			findLabel.setMaximumSize(new Dimension(100, 15));
			findLabel.setPreferredSize(new Dimension(100, 15));
			findLabel.setMinimumSize(new Dimension(100, 15));
			findPanel.add(findLabel, BorderLayout.WEST);
			findPanel.add(getFindTextField(), BorderLayout.CENTER);
		}
		return findPanel;
	}

	private JTextField getFindTextField() {
		if (findTextField == null) {
			findTextField = new JTextField();
			findTextField.setColumns(10);
		}
		return findTextField;
	}

	private JPanel getReplacePanel() {
		if (replacePanel == null) {
			replacePanel = new JPanel();
			replacePanel.setLayout(new BorderLayout(10, 0));
			JLabel replLabel = new JLabel("Replace with:");
			replLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			replLabel.setMaximumSize(new Dimension(100, 15));
			replLabel.setPreferredSize(new Dimension(100, 15));
			replLabel.setMinimumSize(new Dimension(100, 15));
			replacePanel.add(replLabel, BorderLayout.WEST);
			replacePanel.add(getReplaceTextField(), BorderLayout.CENTER);
		}
		return replacePanel;
	}

	private JTextField getReplaceTextField() {
		if (replaceTextField == null) {
			replaceTextField = new JTextField();
			replaceTextField.setColumns(10);
		}
		return replaceTextField;
	}

	/**
	 * @return
	 */
	private Component getBtnPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			{
				JButton closeBtn = new JButton("Close");
				// closeBtn.setActionCommand("OK");
				closeBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setVisible(false);
					}
				});
				buttonPanel.setLayout(new BorderLayout(0, 0));
				buttonPanel.add(getStatusLabel(), BorderLayout.CENTER);
				buttonPanel.add(closeBtn, BorderLayout.EAST);
				getRootPane().setDefaultButton(closeBtn);
			}
		}
		return buttonPanel;
	}

	private JPanel getContentPanel() {
		if (contentPanel == null) {
			contentPanel = new JPanel();
			contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			contentPanel.setLayout(new BorderLayout(0, 0));
			contentPanel.add(getOptionsPanel(), BorderLayout.CENTER);
			contentPanel.add(getActionsPanel(), BorderLayout.EAST);
		}
		return contentPanel;
	}

	private JPanel getOptionsPanel() {
		if (optionsPanel == null) {
			optionsPanel = new JPanel();
			optionsPanel
					.setBorder(new TitledBorder(null, "Options", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			optionsPanel.setLayout(new GridLayout(0, 1, 0, 0));
			optionsPanel.add(getWordChkBox());
			optionsPanel.add(getCaseChkBox());
			optionsPanel.add(getRegexBox());
			optionsPanel.add(getLabel_1());
		}
		return optionsPanel;
	}

	private JPanel getActionsPanel() {
		if (actionsPanel == null) {
			actionsPanel = new JPanel();
			actionsPanel.setLayout(new GridLayout(5, 1, 0, 2));
			actionsPanel.add(getFindNextBtn());
			actionsPanel.add(getFindPrevBtn());
			actionsPanel.add(getReplaceBtn());
			actionsPanel.add(getReplaceFindBtn());
			actionsPanel.add(getReplaceAllBtn());
		}
		return actionsPanel;
	}

	private JButton getFindNextBtn() {
		if (findNextBtn == null) {
			findNextBtn = new JButton("Find Next");
			findNextBtn.setMargin(btnInsets);
			findNextBtn.setFont(ctrlFont);
			findNextBtn.setActionCommand(EditActionListener.AC_FN);
			findNextBtn.addActionListener(editActionListener);
		}
		return findNextBtn;
	}

	private JButton getFindPrevBtn() {
		if (findPrevBtn == null) {
			findPrevBtn = new JButton("Find Prev.");
			findPrevBtn.setMargin(btnInsets);
			findPrevBtn.setFont(ctrlFont);
			findPrevBtn.setActionCommand(EditActionListener.AC_FP);
			findPrevBtn.addActionListener(editActionListener);
		}
		return findPrevBtn;
	}

	private JButton getReplaceBtn() {
		if (replaceBtn == null) {
			replaceBtn = new JButton("Replace");
			replaceBtn.setMargin(btnInsets);
			replaceBtn.setFont(ctrlFont);
			replaceBtn.setActionCommand(EditActionListener.AC_REP);
			replaceBtn.addActionListener(editActionListener);
		}
		return replaceBtn;
	}

	private JButton getReplaceFindBtn() {
		if (replAndFindBtn == null) {
			replAndFindBtn = new JButton("Replace/Find");
			replAndFindBtn.setMargin(btnInsets);
			replAndFindBtn.setFont(ctrlFont);
			replAndFindBtn.setActionCommand(EditActionListener.AC_REP_F);
			replAndFindBtn.addActionListener(editActionListener);
		}
		return replAndFindBtn;
	}

	private JButton getReplaceAllBtn() {
		if (replaceAllBtn == null) {
			replaceAllBtn = new JButton("Replace All");
			replaceAllBtn.setMargin(btnInsets);
			replaceAllBtn.setFont(ctrlFont);
			replaceAllBtn.setActionCommand(EditActionListener.AC_REP_ALL);
			replaceAllBtn.addActionListener(editActionListener);
		}
		return replaceAllBtn;
	}

	JCheckBox getWordChkBox() {
		if (wordChkBox == null) {
			wordChkBox = new JCheckBox("Match whole word only");
			wordChkBox.setFont(ctrlFont);
			wordChkBox.setEnabled(false);
			wordChkBox.setToolTipText("This capability is not yet implemented");
		}
		return wordChkBox;
	}

	JCheckBox getCaseChkBox() {
		if (caseChkBox == null) {
			caseChkBox = new JCheckBox("Match case");
			caseChkBox.setFont(ctrlFont);
		}
		return caseChkBox;
	}

	JCheckBox getRegexBox() {
		if (regexChkBox == null) {
			regexChkBox = new JCheckBox("regular expression");
			regexChkBox.setFont(ctrlFont);
			regexChkBox.setEnabled(false);
			regexChkBox.setToolTipText("This capability is not yet implemented");

		}
		return regexChkBox;
	}

	private JLabel getLabel_1() {
		if (label_1 == null) {
			label_1 = new JLabel("");
		}
		return label_1;
	}

	private JLabel getStatusLabel() {
		if (statusLabel == null) {
			statusLabel = new JLabel("");
		}
		return statusLabel;
	}

	String getFindText() {
		return getFindTextField().getText();
	}

	String getReplaceText() {
		return getReplaceTextField().getText();
	}

	/**
	 * @param string
	 */
	public void setStatusMsg(String msg) {
		getStatusLabel().setText(msg);

	}

	/**
	 * @param targetText
	 */
	public void setFindText(String targetText) {
		getReplaceTextField().setText(targetText);

	}
}
