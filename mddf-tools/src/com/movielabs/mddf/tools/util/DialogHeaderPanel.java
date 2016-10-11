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

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JLabel;

public class DialogHeaderPanel extends JPanel {
    private JLabel logoLabel = null;
    private ImageIcon logo;
    private String title = "Title goes here";
    private String subTitle = "SubTitle goes here";;
    private JPanel textPanel = null;
    private JLabel textLabel = null;
    private Color startColor;
    private Color endColor;

    /**
     * This is the default constructor
     */
    public DialogHeaderPanel(ImageIcon logo, String title, String subtitle) {
        super();
        this.logo = logo;
        this.title = title;
        this.subTitle = subtitle;
        this.startColor = Color.white;
        this.endColor = getBackground();
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        logoLabel = new JLabel(logo); 
        this.setLayout(new BorderLayout());
        this.setSize(497, 68);
        this.setOpaque(false);
        this.setBorder(javax.swing.BorderFactory.createMatteBorder(1,0,2,0,java.awt.Color.gray));
        this.add(logoLabel, java.awt.BorderLayout.EAST);
        this.add(getTextPanel(), java.awt.BorderLayout.CENTER);
    }

    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getTextPanel() {
        if (textPanel == null) {
            String text = "<html><b><font size=+1>"+title+"</font></b><br><div style='margin-left: 30px;'>"+subTitle+"<br></div></html>";
            textLabel = new JLabel(text); 
            textLabel.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));
            textPanel = new JPanel();
            textPanel.setLayout(new BorderLayout());
            textPanel.setOpaque(false);
            textPanel.add(textLabel, java.awt.BorderLayout.NORTH);
        }
        return textPanel;
    }
    

    /**
     * Paints a gradient background on <code>g</code>.
     */
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setPaint(new GradientPaint(0, 0, getStartColor(), getWidth(),
                getHeight(), getEndColor(), true));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        super.paint(g);
    }

    /**
     * Returns the endcolor for the gradient fill.
     * 
     * @return Returns the end color.
     */
    public Color getEndColor() {
        return endColor;
    }

    /**
     * Sets the endcolor for the gradient fill.
     * 
     * @param endColor
     *            The end color of the gradient.
     */
    public void setEndColor(Color endColor) {
        this.endColor = endColor;
    }

    /**
     * Returns the startcolor for the gradient fill.
     * 
     * @return Returns the start color.
     */
    public Color getStartColor() {
        return startColor;
    }

    /**
     * Sets the startcolor for the gradient fill.
     * 
     * @param startColor
     *            The start color of the gradient.
     */
    public void setStartColor(Color startColor) {
        this.startColor = startColor;
    }

}  