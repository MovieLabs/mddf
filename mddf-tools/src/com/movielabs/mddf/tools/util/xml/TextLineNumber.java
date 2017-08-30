/**
 * Created Jul 19, 2016
 *  
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.beans.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.movielabs.mddf.tools.util.logging.AdvLogPanel;
import com.movielabs.mddflib.logging.LogEntryNode;
import com.movielabs.mddflib.logging.LogMgmt;

/**
 * This class will display line numbers for a related text component. The text
 * component must use the same line height for each line. TextLineNumber
 * supports wrapped lines and will highlight the line number of the current line
 * in the text component.
 *
 * This class was designed to be used as a component added to the row header of
 * a JScrollPane.
 */
public class TextLineNumber extends JPanel
		implements CaretListener, DocumentListener, PropertyChangeListener, ImageObserver {
	/**
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	public class LineListener implements MouseListener {

		private double minY;
		private double maxY;
		private LogEntryNode logEntry;
		private boolean active;

		/**
		 * @param r
		 * @param textLineNumber
		 */
		public LineListener(Rectangle region, LogEntryNode logEntry, JPanel lnPanel) {
			this.logEntry = logEntry;
			minY = region.getY();
			maxY = minY + region.getHeight();
			lnPanel.addMouseListener(this);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseClicked(MouseEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		@Override
		public void mousePressed(MouseEvent e) {
			if (interects(e) && e.isPopupTrigger()) {
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseReleased(MouseEvent e) {
			if (interects(e)) {
				String msg = "Line " + logEntry.getLine() + ": " + logEntry.getSummary();
				String tooltip = logEntry.getTooltip();
				if (tooltip != null && !tooltip.isEmpty()) {
					msg = "<html>" + msg + "<br/>" + tooltip + "</html>";
				}
				// editor.getMsgField().setToolTipText(tooltip);
				editor.getMsgField().setText(msg);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		private boolean interects(MouseEvent e) {
			return ((minY <= e.getY()) && (e.getY() <= maxY));
		}

	}

	public final static float LEFT = 0.0f;
	public final static float CENTER = 0.5f;
	public final static float RIGHT = 1.0f;

	private final static Border OUTER = new MatteBorder(0, 0, 0, 2, Color.GRAY);

	private final static int HEIGHT = Integer.MAX_VALUE - 1000000;
	private static final int DEFAULT_BORDER_GAP = 35;

	private JTextComponent component;

	// Properties that can be changed

	private boolean updateFont;
	private int borderGap;
	private Color currentLineForeground;
	private float digitAlignment;
	private int minimumDisplayDigits;

	/*
	 * Keep history information to reduce the number of times the component
	 * needs to be repainted
	 */

	private int lastDigits;
	private int lastHeight;
	private int lastLine;

	private HashMap<String, FontMetrics> fonts;
	private int currentLine = -1;
	// private ArrayList<LogEntryNode> markerList;
	// private LogEntryNode[] markerArray;
	// private int[] markerIndexArray;

	private Map<String, List> markerHash = new HashMap();

	private BufferedImage[] markerImage = new BufferedImage[LogMgmt.logLevels.length];
	private SimpleXmlEditor editor;

	/**
	 * Create a line number component for a text component. This minimum display
	 * width will default to 3 digits.
	 *
	 * @param editor
	 *            the related SimpleXmlEditor
	 */
	public TextLineNumber(SimpleXmlEditor editor) {
		this(editor, 3);
	}

	/**
	 * Create a line number component for a text component. This minimum display
	 * width will be determined by the <tt>minimumDisplayDigits</tt> parameter.
	 *
	 * @param editor
	 *            the related SimpleXmlEditor
	 * @param minimumDisplayDigits
	 *            the number of digits used to calculate the minimum width of
	 *            the component
	 */
	public TextLineNumber(SimpleXmlEditor editor, int minimumDisplayDigits) {
		this.editor = editor;
		this.component = editor.getXmlEditorPane();

		setFont(component.getFont());

		setBorderGap(DEFAULT_BORDER_GAP);
		setCurrentLineForeground(Color.RED);
		setDigitAlignment(RIGHT);
		setMinimumDisplayDigits(minimumDisplayDigits);

		component.getDocument().addDocumentListener(this);
		component.addCaretListener(this);
		component.addPropertyChangeListener("font", this);

		markerImage[LogMgmt.LEV_DEBUG] = null;
		markerImage[LogMgmt.LEV_WARN] = createIcon("images/warning-icon.png");
		markerImage[LogMgmt.LEV_ERR] = createIcon("images/error-icon.png");
		markerImage[LogMgmt.LEV_INFO] = createIcon("images/info-icon.png");
	}

	/**
	 * Gets the update font property
	 *
	 * @return the update font property
	 */
	public boolean getUpdateFont() {
		return updateFont;
	}

	/**
	 * Set the update font property. Indicates whether this Font should be
	 * updated automatically when the Font of the related text component is
	 * changed.
	 *
	 * @param updateFont
	 *            when true update the Font and repaint the line numbers,
	 *            otherwise just repaint the line numbers.
	 */
	public void setUpdateFont(boolean updateFont) {
		this.updateFont = updateFont;
	}

	/**
	 * Gets the border gap
	 *
	 * @return the border gap in pixels
	 */
	public int getBorderGap() {
		return borderGap;
	}

	/**
	 * The border gap is used in calculating the left and right insets of the
	 * border.
	 *
	 * @param borderGap
	 *            the gap in pixels
	 */
	public void setBorderGap(int borderGap) {
		this.borderGap = borderGap;
		Border inner = new EmptyBorder(0, borderGap, 0, borderGap);
		setBorder(new CompoundBorder(OUTER, inner));
		lastDigits = 0;
		setPreferredWidth();
	}

	/**
	 * Gets the current line rendering Color
	 *
	 * @return the Color used to render the current line number
	 */
	public Color getCurrentLineForeground() {
		return currentLineForeground == null ? getForeground() : currentLineForeground;
	}

	/**
	 * The Color used to render the current line digits. Default is Coolor.RED.
	 *
	 * @param currentLineForeground
	 *            the Color used to render the current line
	 */
	public void setCurrentLineForeground(Color currentLineForeground) {
		this.currentLineForeground = currentLineForeground;
	}

	/**
	 * Gets the digit alignment
	 *
	 * @return the alignment of the painted digits
	 */
	public float getDigitAlignment() {
		return digitAlignment;
	}

	/**
	 * Specify the horizontal alignment of the digits within the component.
	 * Common values would be:
	 * <ul>
	 * <li>TextLineNumber.LEFT
	 * <li>TextLineNumber.CENTER
	 * <li>TextLineNumber.RIGHT (default)
	 * </ul>
	 * 
	 * @param currentLineForeground
	 *            the Color used to render the current line
	 */
	public void setDigitAlignment(float digitAlignment) {
		this.digitAlignment = digitAlignment > 1.0f ? 1.0f : digitAlignment < 0.0f ? -1.0f : digitAlignment;
	}

	/**
	 * Gets the minimum display digits
	 *
	 * @return the minimum display digits
	 */
	public int getMinimumDisplayDigits() {
		return minimumDisplayDigits;
	}

	/**
	 * Specify the minimum number of digits used to calculate the preferred
	 * width of the component.
	 *
	 * @param minimumDisplayDigits
	 *            the number digits used in the preferred width calculation
	 */
	public void setMinimumDisplayDigits(int minimumDisplayDigits) {
		this.minimumDisplayDigits = minimumDisplayDigits;
		setPreferredWidth();
	}

	/**
	 * Calculate the width needed to display the maximum line number
	 */
	private void setPreferredWidth() {
		Element root = component.getDocument().getDefaultRootElement();
		int lines = root.getElementCount();
		int digits = Math.max(String.valueOf(lines).length(), minimumDisplayDigits);

		// Update sizes when number of digits in the line number changes

		if (lastDigits != digits) {
			lastDigits = digits;
			FontMetrics fontMetrics = getFontMetrics(getFont());
			int width = fontMetrics.charWidth('0') * digits;
			Insets insets = getInsets();
			int preferredWidth = insets.left + insets.right + width;

			Dimension d = getPreferredSize();
			d.setSize(preferredWidth, HEIGHT);
			setPreferredSize(d);
			setSize(d);
		}
	}

	/**
	 * Draw the line numbers and markers.
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		// Determine the width of the space available to draw the line number:
		FontMetrics fontMetrics = component.getFontMetrics(component.getFont());
		Insets insets = getInsets();
		int availableWidth = getSize().width - insets.left - insets.right;

		// Determine the rows to draw within the clipped bounds.

		Rectangle clip = g.getClipBounds();
		int rowStartOffset = component.viewToModel(new Point(0, clip.y));
		int endOffset = component.viewToModel(new Point(0, clip.y + clip.height));
		int markerCnt = 0;
		while (rowStartOffset <= endOffset) {
			boolean curLine = isCurrentLine(rowStartOffset);
			try {
				if (curLine) {
					g.setColor(getCurrentLineForeground());
					Font boldFont = g.getFont().deriveFont(Font.BOLD);
					g.setFont(boldFont);
				} else {
					g.setColor(getForeground());
					Font plainFont = g.getFont().deriveFont(Font.PLAIN);
					g.setFont(plainFont);
				}

				// Get the line number as a string and then determine the
				// "X" and "Y" offsets for drawing the string.

				String lineLabel = getTextLineNumber(rowStartOffset);
				int stringWidth = fontMetrics.stringWidth(lineLabel);
				int x = getOffsetX(availableWidth, stringWidth) + insets.left;
				int y = getOffsetY(rowStartOffset, fontMetrics);
				g.drawString(lineLabel, x, y);

				int lineNum = getLineNumber(rowStartOffset);
				// are there any markers for this line?
				String key = Integer.toString(lineNum);
				List<LogEntryNode> nextList = markerHash.get(key);
				if (nextList != null && !nextList.isEmpty()) {
					/*
					 * if there are multiple log entries we need the one with
					 * the highest severity
					 */
					LogEntryNode lineMarker = null;
					for (LogEntryNode nextMarker : nextList){
						if((lineMarker == null) || (nextMarker.getLevel() > lineMarker.getLevel())){
							lineMarker = nextMarker;
						}
					}
					// now set the graphic based on most sever entry found
					BufferedImage myImage = markerImage[lineMarker.getLevel()];
					markerCnt++;
					if (myImage != null) {
						int imgX = 0;
						Rectangle r = component.modelToView(rowStartOffset);
						int imgY = r.y;
						Graphics2D g2 = (Graphics2D) g;
						g2.drawImage(myImage, imgX, imgY, null);
						new LineListener(r, lineMarker, this);
					}
				} 
				// Move to the next row
				rowStartOffset = Utilities.getRowEnd(component, rowStartOffset) + 1;
			} catch (Exception e) {
				break;
			}
		}
	}

	/**
	 * Returns an Image, or null if the path was invalid.
	 * 
	 * @param path
	 * @return a <tt>BufferedImage</tt>
	 */
	private BufferedImage createIcon(String path) {
		java.net.URL imgURL = AdvLogPanel.class.getResource(path);
		if (imgURL != null) {
			try {
				return ImageIO.read(imgURL);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String pkgName = getClass().getPackage().getName();
		System.err.println("Couldn't find image resource: " + path + ", pkgName=" + pkgName);
		return null;

	}

	/**
	 * @param markerList
	 */
	public void setLineMarkers(ArrayList<LogEntryNode> markerList) {
		// this.markerList = markerList;
		// // need array that can be used in a binary search ....
		// markerIndexArray = new int[markerList.size()];
		// for (int i = 0; i < markerList.size(); i++) {
		// LogEntryNode next = markerList.get(i);
		// markerIndexArray[i] = next.getLine();
		// }
		markerHash = new HashMap();
		for (LogEntryNode nextEntry : markerList) {
			String key = Integer.toString(nextEntry.getLine());
			List<LogEntryNode> nextList = markerHash.get(key);
			if (nextList == null) {
				nextList = new ArrayList<LogEntryNode>();
			}
			nextList.add(nextEntry);
			markerHash.put(key, nextList);
		}
	}

	private boolean isCurrentLine(int rowStartOffset) {
		/*
		 * Is the caret positioned on the line about to be painted?
		 */
		int caretPosition = component.getCaretPosition();
		Element root = component.getDocument().getDefaultRootElement();

		if (root.getElementIndex(rowStartOffset) == root.getElementIndex(caretPosition))
			return true;
		else
			return false;
	}

	/**
	 * Get the line number to be drawn. The empty string will be returned when a
	 * line of text has wrapped.
	 * 
	 * @param rowStartOffset
	 * @return
	 */
	protected String getTextLineNumber(int rowStartOffset) {
		Element root = component.getDocument().getDefaultRootElement();
		int index = root.getElementIndex(rowStartOffset);
		Element line = root.getElement(index);

		if (line.getStartOffset() == rowStartOffset)
			return String.valueOf(index + 1);
		else
			return "";
	}

	protected int getLineNumber(int rowStartOffset) {
		Element root = component.getDocument().getDefaultRootElement();
		int index = root.getElementIndex(rowStartOffset);
		return index + 1;
	}

	/**
	 * @return the currentLine
	 */
	public int getCurrentLine() {
		return currentLine;
	}

	/**
	 * Determine the X offset to properly align the line number when drawn
	 * 
	 * @param availableWidth
	 * @param stringWidth
	 * @return
	 */
	private int getOffsetX(int availableWidth, int stringWidth) {
		return (int) ((availableWidth - stringWidth) * digitAlignment);
	}

	/**
	 * Determine the Y offset for the current row
	 * 
	 * @param rowStartOffset
	 * @param fontMetrics
	 * @return
	 * @throws BadLocationException
	 */
	private int getOffsetY(int rowStartOffset, FontMetrics fontMetrics) throws BadLocationException {
		// Get the bounding rectangle of the row

		Rectangle r = component.modelToView(rowStartOffset);
		int lineHeight = fontMetrics.getHeight();
		int y = r.y + r.height;
		int descent = 0;

		/*
		 * The text needs to be positioned above the bottom of the bounding
		 * rectangle based on the descent of the font(s) contained on the row.
		 */

		if (r.height == lineHeight) // default font is being used
		{
			descent = fontMetrics.getDescent();
		} else // We need to check all the attributes for font changes
		{
			if (fonts == null)
				fonts = new HashMap<String, FontMetrics>();

			Element root = component.getDocument().getDefaultRootElement();
			int index = root.getElementIndex(rowStartOffset);
			Element line = root.getElement(index);

			for (int i = 0; i < line.getElementCount(); i++) {
				Element child = line.getElement(i);
				AttributeSet as = child.getAttributes();
				String fontFamily = (String) as.getAttribute(StyleConstants.FontFamily);
				Integer fontSize = (Integer) as.getAttribute(StyleConstants.FontSize);
				String key = fontFamily + fontSize;

				FontMetrics fm = fonts.get(key);

				if (fm == null) {
					Font font = new Font(fontFamily, Font.PLAIN, fontSize);
					fm = component.getFontMetrics(font);
					fonts.put(key, fm);
				}

				descent = Math.max(descent, fm.getDescent());
			}
		}

		return y - descent;
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		int caretPosition = component.getCaretPosition();
		Element root = component.getDocument().getDefaultRootElement();
		currentLine = root.getElementIndex(caretPosition);
		// repaint so the correct line number can be highlighted

		if (lastLine != currentLine) {
			repaint();
			lastLine = currentLine;
		}
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		documentChanged();
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		documentChanged();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		documentChanged();
	}

	/*
	 * A document change may affect the number of displayed lines of text.
	 * Therefore the lines numbers will also change.
	 */
	private void documentChanged() {
		// View of the component has not been updated at the time
		// the DocumentEvent is fired

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					int endPos = component.getDocument().getLength();
					Rectangle rect = component.modelToView(endPos);

					if (rect != null && rect.y != lastHeight) {
						setPreferredWidth();
						repaint();
						lastHeight = rect.y;
					}
				} catch (BadLocationException ex) {
					/* nothing to do */ }
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
	 * PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getNewValue() instanceof Font) {
			if (updateFont) {
				Font newFont = (Font) evt.getNewValue();
				setFont(newFont);
				lastDigits = 0;
				setPreferredWidth();
			} else {
				repaint();
			}
		}
	}
}
