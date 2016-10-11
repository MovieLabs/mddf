/**
 * Created Oct 26, 2015 
 * Copyright Motion Picture Laboratories, Inc. 2015
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
import java.awt.SystemColor;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import com.movielabs.mddf.tools.GenericTool;
import com.movielabs.mddf.tools.ValidatorTool;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class HeaderPanel extends JPanel {
	public static final String imgLoc = GenericTool.imageRsrcPath;
	private JLabel logoLabel;
	private JLabel bannerLabel;
	private JPanel menuPanel;
	private JMenuBar menuBar;
	private ValidatorTool.Context context;

	/**
	 * Create the panel.
	 * 
	 * @param context
	 * 
	 * @param jMenuBar
	 */
	public HeaderPanel(JMenuBar menuBar, ValidatorTool.Context context) {
		this.menuBar = menuBar;
		this.context = context;
		setBackground(GuiSettings.backgroundHdrPanel);
		setLayout(new BorderLayout(0, 0));
		add(getLogoLabel(), BorderLayout.WEST);
		add(getBannerLabel(), BorderLayout.CENTER);
		add(getMenuPanel(), BorderLayout.SOUTH);

	}

	/**
	 * @return
	 */
	private JPanel getMenuPanel() {
		if (menuPanel == null) {
			menuPanel = new JPanel();
			menuPanel.setBorder(new LineBorder(SystemColor.activeCaption, 2));
			menuPanel.setLayout(new BorderLayout(0, 0));
			menuPanel.add(menuBar, BorderLayout.WEST);
		}
		return menuPanel;
	}

	private JLabel getLogoLabel() {
		if (logoLabel == null) {
			logoLabel = new JLabel();
			Icon ico = new ImageIcon(this.getClass().getResource(imgLoc + "logo_movielabs.jpg"));
			logoLabel.setIcon(ico);
		}
		return logoLabel;
	}

	private JLabel getBannerLabel() {
		if (bannerLabel == null) {
			bannerLabel = new JLabel();
			Icon ico = null;
			switch (context) {
			case MANIFEST:
				ico = new ImageIcon(this.getClass().getResource(imgLoc + "banner_Manifest_Validator.png"));
				break;
			case AVAILS:
				ico = new ImageIcon(this.getClass().getResource(imgLoc + "banner_Avails_Validator.png"));
				break;
			default:
				ico = new ImageIcon(this.getClass().getResource(imgLoc + "home_banner.jpg"));
			}
			bannerLabel.setIcon(ico);
		}
		return bannerLabel;
	}

}
