/**
 * Created Jun 23, 2016 
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

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.border.LineBorder;
import java.awt.Color;

/**
 * Dialog used to view or set the <i>Use Cases</i> against which a Manifest may
 * be validated. Use Cases are associated with specific Profiles but not all
 * Profiles will have defined Use Cases.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class UseCaseDialog extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JPanel checkBoxPane;
	private String[] availUsesCases = null;
	private HashMap<String, JCheckBox> chkBoxMap;
	private List<String> previousList;

	/**
	 * Launch the application (for UNIT TESTING ONLY)
	 */
	public static void main(String[] args) {
		try {
			UseCaseDialog dialog = new UseCaseDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public UseCaseDialog() {
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);

		JPanel buttonPane = new JPanel();
		buttonPane.setBorder(new LineBorder(new Color(70, 130, 180), 1, true));
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		{
			JButton allButton = new JButton("Select All");
			allButton.setActionCommand("ALL");
			allButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					handleBulkSelection(true);
				}
			});
			buttonPane.add(allButton);
		}
		{
			JButton clearButton = new JButton("Clear All");
			clearButton.setActionCommand("CLEAR");
			clearButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					handleBulkSelection(false);
				}
			});
			buttonPane.add(clearButton);
		}
		{
			JButton okButton = new JButton("OK");
			okButton.setActionCommand("OK");
			buttonPane.add(okButton);
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}
			});
			getRootPane().setDefaultButton(okButton);
		}
		{
			JButton cancelButton = new JButton("Cancel");
			cancelButton.setActionCommand("CANCEL");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// reset to initial selections before closing
					 handleResetOfSelections();
					setVisible(false);
				}
			});
			buttonPane.add(cancelButton);
		}
		// empty for now. setAvailableUseCases() must be invoked to fill
		checkBoxPane = new JPanel();
		checkBoxPane.setBorder(new LineBorder(new Color(70, 130, 180), 1, true));
		getContentPane().add(checkBoxPane, BorderLayout.CENTER);
		checkBoxPane.setLayout(new GridLayout(0, 2, 5, 5));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Dialog#setVisible(boolean)
	 */
	public void setVisible(boolean isVisible) {
		previousList = getSelectedUseCases();
		super.setVisible(isVisible);
	}

	void handleBulkSelection(boolean isSelected) {
		if (chkBoxMap != null && !chkBoxMap.isEmpty()) {
			Iterator<JCheckBox> cbIt = chkBoxMap.values().iterator();
			while (cbIt.hasNext()) {
				JCheckBox next = cbIt.next();
				next.setSelected(isSelected);
			}
		}
	}

	void handleResetOfSelections() {
		handleBulkSelection(false);
		if (chkBoxMap != null && !chkBoxMap.isEmpty()) {
			for (int i = 0; i < previousList.size(); i++) {
				JCheckBox next = chkBoxMap.get(previousList.get(i));
				next.setSelected(true);
			}
		}
	}

	/**
	 * Set the use cases that will be displayed to a user for possible
	 * selection. If the <tt>useCases</tt> argument is <tt>null</tt> the display
	 * pane will be empty.
	 * 
	 * @param useCases
	 */
	public void setAvailableUseCases(String[] useCases) {
		checkBoxPane.removeAll();
		availUsesCases = useCases;
		chkBoxMap = new HashMap<String, JCheckBox>();
		if (useCases == null) {
			return;
		}
		for (int i = 0; i < useCases.length; i++) {
			JCheckBox chkBox = new JCheckBox(useCases[i]);
			chkBox.setToolTipText(useCases[i]);
			chkBox.setSelected(false);
			checkBoxPane.add(chkBox);
			chkBoxMap.put(useCases[i], chkBox);
		}
		previousList = getSelectedUseCases();
	}

	/**
	 * @return
	 */
	public List<String> getSelectedUseCases() {
		List<String> selectedUseCases = new ArrayList<String>();
		if (availUsesCases != null) {
			for (int i = 0; i < availUsesCases.length; i++) {
				JCheckBox chkBox = chkBoxMap.get(availUsesCases[i]);
				if (chkBox.isSelected()) {
					selectedUseCases.add(availUsesCases[i]);
				}
			}
		}
		return selectedUseCases;
	}

}
