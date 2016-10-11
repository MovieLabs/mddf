/**
 * Created Feb 23, 2016 
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
package com.movielabs.mddf.tools.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import com.movielabs.mddf.tools.GenericTool;

/**
 * Creates a pop-up JDialog with a standardized corporate look-n-feel. This
 * class is typically extended by an application to content specific to that
 * app.
 * <p>
 * The JDialog will have:
 * <ul>
 * <li>a header panel with the application's name and logo,</li>
 * <li>a tab panel in the middle with distinct tabs for presenting information
 * about various aspects of the app (e.g., copyright, build/version, licensing,
 * etc)</li>
 * </ul>
 * </p>
 * 
 * @author L. J. Levin
 */
public class AboutDialog extends JDialog {
	private JPanel jContentPane = null;
	private JPanel buttonPanel;
	private JTabbedPane tabbedPane;
	private HashMap<String, JLabel> tabPanes = new HashMap<String, JLabel>();
	private HashMap<String, ArrayList<?>> tabEntries = new HashMap<String, ArrayList<?>>();
	private static String logoPath = GenericTool.imageRsrcPath +"logo_movielabs.jpg";

	/**
	 * Create dialog using the default  logo
	 * 
	 * @param title
	 * @param subtitle
	 */
	public AboutDialog(String title, String subtitle) {
		this(title, subtitle, logoPath);
	}

	/**
	 * Create dialog using the provided logo
	 * 
	 * @param title
	 * @param subtitle
	 * @param appLogoPath
	 */
	public AboutDialog(String title, String subtitle, String appLogoPath) {
		super();
		setTitle("About");
		// get the icon with the logo for the application
		ImageIcon appLogo = new ImageIcon(getClass().getResource(appLogoPath));
		initialize();
		// Add the header panel
		if (subtitle != null || appLogo != null) {
			DialogHeaderPanel header = new DialogHeaderPanel(appLogo, title, subtitle);
			getContentPane().add(header, BorderLayout.NORTH);
		}
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

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() { 
		this.setSize(620, 520);
		this.setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
			tabbedPane = new JTabbedPane();
			jContentPane.add(tabbedPane, BorderLayout.CENTER);
		}
		return jContentPane;
	}

	public void addTab(String name) {
		String initText = "<html>t.b.d. ::" + name + "</html>";
		JLabel tabContents = new JLabel(initText);
		tabContents.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));
		tabContents.setVerticalAlignment(SwingConstants.TOP);
		tabbedPane.add(name, tabContents);
		tabPanes.put(name, tabContents);
		tabEntries.put(name, new ArrayList<Object>());
	}

	public void addEntry(String name, String text) {
		ArrayList<String> entries = (ArrayList<String>) tabEntries.get(name);
		entries.add(text);
		// now reset contents shown in the pane
		String htmlText = "<html><ul>";
		if (!entries.isEmpty()) {
			for (int i = 0; i < entries.size(); i++) {
				String next = (String) entries.get(i);
				htmlText = htmlText + "<li>" + next + "<br></li>";
			}
		}
		htmlText = htmlText + "</ul></html>";
		setTabText(name, htmlText);
	}

	protected void setTabText(String name, String htmlText) {
		JLabel tabContents = (JLabel) tabPanes.get(name);
		tabContents.setText(htmlText);
	}
}