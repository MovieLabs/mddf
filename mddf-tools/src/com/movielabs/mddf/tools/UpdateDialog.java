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
package com.movielabs.mddf.tools;

import javax.swing.JDialog;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import javax.swing.JLabel;

import net.sf.json.JSONObject;
import net.sf.json.groovy.JsonSlurper;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import java.awt.Insets;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UpdateDialog extends JDialog {

	/**
	 * Launch the application. [for TESTING ONLY]
	 */
	public static void main(String[] args) {
		try {
			UpdateDialog dialog = new UpdateDialog();
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * [for TESTING ONLY]
	 */
	public UpdateDialog() {
		String fakeStatus = "{\"latest\":{\"build.timestamp\":\"2019-Feb-27 19:16:36 UTC\",\"mddf.lib.version\":\"1.5.1.rc5-SNAPSHOT\",\"mddf.tool.build\":\"J\",\"mddf.tool.version\":\"1.5.1.rc4\"},\"jarUrl\":\"https://github.com/MovieLabs/mddf/raw/master/binaries/mddf-tools-1.5.1.rc4.jar\",\"status\":\"UPDATE\"}";
		JsonSlurper slurper = new JsonSlurper();
		JSONObject statusCheck = (JSONObject) slurper.parseText(fakeStatus);
		initialize(statusCheck, "1.0.1", null);
	}

	public UpdateDialog(JSONObject statusCheck, String curVersion, Component parent) {
		initialize(statusCheck, curVersion, parent);
	}

	private void initialize(JSONObject statusCheck, String curVersion, Component parent) {

		setBounds(100, 100, 593, 300);
		setIconImage(Toolkit.getDefaultToolkit()
				.getImage(UpdateDialog.class.getResource("/com/movielabs/mddf/tools/images/icon_movielabs.jpg")));
		setTitle("Update Available");
		JSONObject latestReleaseDesc = statusCheck.getJSONObject("latest");
		String jarUrl = statusCheck.getString("jarUrl");
		String msg = "<html>A newer version of the MDDF Toolkit is available";
		msg = msg + "<ul><li>Current version: " + curVersion + "</li>";
		msg = msg + "<li>Latest release: " + latestReleaseDesc.getString("mddf.tool.version") + "</li></ul>";
		msg = msg + "<p>To download the latest release, copy this URL, then paste it into you browser:<br/><a href='"
				+ jarUrl + "'>" + jarUrl + "</a></p>";
		msg = msg + "</html>";
		JLabel lblAMoreRecent = new JLabel(msg);
		lblAMoreRecent.setHorizontalAlignment(SwingConstants.CENTER);
		getContentPane().add(lblAMoreRecent, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setHgap(10);
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton copyBtn = new JButton("");
		copyBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println("Copying " + jarUrl);
				StringSelection stringSelection = new StringSelection(jarUrl);
				Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
				clpbrd.setContents(stringSelection, null);
			}
		});
		copyBtn.setMargin(new Insets(1, 0, 1, 0));
		copyBtn.setToolTipText("Copy URL to clipboard");
		copyBtn.setIcon(new ImageIcon(
				UpdateDialog.class.getResource("/com/movielabs/mddf/tools/images/copy-to-clipboard.png")));
		panel.add(copyBtn);

		JButton btnOk = new JButton("OK");
		btnOk.setMargin(new Insets(9, 12, 9, 12));
		panel.add(btnOk);
		btnOk.setSize(new Dimension(55, 25));
		btnOk.setHorizontalAlignment(SwingConstants.RIGHT);

		btnOk.setActionCommand("OK");
		
		JLabel lblNewLabel = new JLabel("");
		lblNewLabel.setIcon(new ImageIcon(UpdateDialog.class.getResource("/com/movielabs/mddf/tools/images/attention-64.png")));
		getContentPane().add(lblNewLabel, BorderLayout.NORTH);
		btnOk.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}

		});
	}

}
