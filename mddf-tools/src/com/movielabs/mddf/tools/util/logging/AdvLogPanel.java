/**
 * Created Feb 4, 2016 
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
package com.movielabs.mddf.tools.util.logging;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.apache.poi.ss.usermodel.Cell;
import org.jdom2.located.Located;

import com.movielabs.mddf.tools.util.xml.EditorMgr;
import com.movielabs.mddf.tools.util.xml.SimpleXmlEditor;
import com.movielabs.mddflib.logging.LogEntryFolder;
import com.movielabs.mddflib.logging.LogEntryNode;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class AdvLogPanel extends JPanel implements LoggerWidget, TreeSelectionListener {

	static final int leftWidth = 130;
	private LogNavPanel treeView;
	private LogPanel tableView;
	private JSplitPane splitPane;
	private JMenu saveLogMenu;
	private File curInputFile;
	private int minLevel = LogMgmt.LEV_WARN;

	public AdvLogPanel() {
		treeView = new LogNavPanel();
		treeView.addListener(this);
		tableView = new LogPanel( );
		tableView.addMouseListener(new PopClickListener(this));
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeView, tableView);
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(leftWidth);
		splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {

				System.out.println("AdvLogPanel: JSplitPane PropertyChangeEvent" );
				String propertyName = pce.getPropertyName();
				JSplitPane sourceSplitPane = (JSplitPane) pce.getSource();
				if (propertyName.equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
					setSize(getWidth(), getHeight());
				}
			}
		});
		tableView.addPropertyChangeListener( new PropertyChangeListener(){

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				// TODO Auto-generated method stub

				System.out.println("AdvLogPanel: tableView PropertyChangeEvent" );
			}
			
		});
		// Add GUI components
		this.setLayout(new BorderLayout());
		this.add("Center", splitPane);
		this.addComponentListener(new ComponentListener() {

			@Override
			public void componentResized(ComponentEvent e) {
				setSize(getWidth(), getHeight());
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void componentShown(ComponentEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void componentHidden(ComponentEvent e) {
				// TODO Auto-generated method stub

			}

		});
	}

	public void setSize( ){ 
		setSize(getWidth(), getHeight());
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddf.util.UiLogger#setWidth(int)
	 */
	@Override
	public void setSize(int width, int height) {
		int tableWidth = width - treeView.getWidth() - splitPane.getDividerSize();
		tableView.setSize(tableWidth, height);
	}

	/**
	 * @return
	 */
	JMenu getSaveLogMenu() {
		if (saveLogMenu == null) {
			saveLogMenu = new JMenu("Save as...");
			JMenuItem saveCsvMItem = new JMenuItem("CSV");
			saveLogMenu.add(saveCsvMItem);
			JMenuItem saveXmlMItem = new JMenuItem("XML");
			saveXmlMItem.setEnabled(false);
			saveXmlMItem.setToolTipText("not yet implemented");
			saveLogMenu.add(saveXmlMItem);
			saveCsvMItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						tableView.saveAs("csv");
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			});
		}
		return saveLogMenu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddf.util.UiLogger#clearLog()
	 */
	@Override
	public void clearLog() {
		treeView.clearLog();
		tableView.clearLog();
		this.invalidate();
		this.repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event
	 * .TreeSelectionEvent)
	 */
	@Override
	public void valueChanged(TreeSelectionEvent e) {
		List<LogEntryNode> msgList = treeView.getSelectedMsgSet();
		if (msgList == null) {
			// nothing selected
			return;
		}
		tableView.clearLog();
		tableView.append(msgList);

	}

	/**
	 * @param logEntry
	 */
	protected void showEditor(LogEntryNode logEntry) {
		// Key is the ABSOLUTE path to XML file
		String key = logEntry.getManifestPath();
		if (key == null || (key.isEmpty())) {
			JOptionPane.showMessageDialog(this, "The selected logEntry is not linked to a file.", "Unsupported Request",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (key.endsWith(".xml")) {
			SimpleXmlEditor editor = EditorMgr.getSingleton().getEditor(logEntry, this);
		} else {
			JOptionPane.showMessageDialog(this,
					"File type is not supported by the internal Editor. Please use an external editor of the appropriate type.",
					"Unsupported Request", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}

	public class MsgLogPopup extends JPopupMenu {
		JMenuItem clrLogMItem;
		private LoggerWidget logger;
		private LogEntryNode logEntry;

		public MsgLogPopup(LoggerWidget logPanel, LogEntryNode logEntry) {
			this.logger = logPanel;
			this.logEntry = logEntry;

			JMenuItem editXmlMItem = new JMenuItem("Show in Editor");
			add(editXmlMItem);
			editXmlMItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					showEditor(logEntry);
				}
			});
			add(new JSeparator());

			clrLogMItem = new JMenuItem("Clear Log");
			clrLogMItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					logger.clearLog();
				}
			});
			add(clrLogMItem);
			add(getSaveLogMenu());
		}

	}

	/**
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	class PopClickListener extends MouseAdapter {
		private LoggerWidget logPanel;

		/**
		 * @param logPanel
		 */
		public PopClickListener(LoggerWidget logPanel) {
			this.logPanel = logPanel;
		}

		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger())
				doPop(e);
		}

		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger())
				doPop(e);
		}

		private void doPop(MouseEvent e) {
			LogEntryNode logEntry = tableView.getLogEntryAt(e);
			// System.out.println("Entry is at line " + logEntry.locLine);
			MsgLogPopup menu = new MsgLogPopup(logPanel, logEntry);
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	@Override
	public void expand() {
		treeView.expand();
	}

	@Override
	public void collapse() {
		treeView.collapse();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.movielabs.mddf.util.logging.Logger#getFileFolder(java.lang.String)
	 */
	@Override
	public LogEntryFolder getFileFolder(String fileName) {
		return treeView.getFileFolder(fileName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddf.util.UiLogger#setCurrentFile(java.lang.String)
	 */
	@Override
	public void setCurrentFile(File targetFile) {
		this.curInputFile = targetFile;
		treeView.setCurrentFileId(targetFile.getName(), true);
		valueChanged(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddf.util.UiLogger#saveAs(java.io.File,
	 * java.lang.String)
	 */
	@Override
	public void saveAs(File outFile, String format) throws IOException {
		tableView.saveAs(outFile, format);
	}

	// ===============================================

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#logXmlIssue(int, int,
	 * org.jdom2.Element, java.lang.String, java.lang.String,
	 * com.movielabs.mddflib.logging.LogReference)
	 */
	@Override
	public void logIssue(int tag, int level, Object target, String msg, String explanation, LogReference srcRef,
			String moduleId) {
		int lineNum = -1;
		if (target != null) {
			if (target instanceof Located) {
				lineNum = ((Located) target).getLine();
			} else if (target instanceof Cell) {
				/*
				 * Add 1 to line number for display purposes. Code is zero-based
				 * index but Excel spreadsheet displays using 1 as the 1st row.
				 */
				lineNum = ((Cell) target).getRowIndex() + 1;
				/* Prefix an 'explanation' with column ID (e.g., 'X', 'AA') */
				int colNum = ((Cell) target).getColumnIndex();
				String prefix = "Column " + mapColNum(colNum);
				if ((explanation == null) || (explanation.isEmpty())) {
					explanation = prefix;
				} else {
					explanation = prefix + ": " + explanation;
				}
			}
		}
		log(level, tag, msg, curInputFile, lineNum, moduleId, explanation, srcRef);
	}

	/**
	 * @param colNum
	 * @return
	 */
	private String mapColNum(int colNum) {
		if (colNum >= 0 && colNum < 26)
			return String.valueOf((char) ('A' + colNum));
		else if (colNum > 25)
			return mapColNum((colNum / 26) - 1) + mapColNum(colNum % 26);
		else
			return "#";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#log(int, int,
	 * java.lang.String, java.io.File, java.lang.String)
	 */
	@Override
	public void log(int level, int tag, String msg, File file, String moduleId) {
		append(level, tag, msg, file, -1, moduleId, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#log(int, int,
	 * java.lang.String, java.io.File, int, java.lang.String, java.lang.String,
	 * com.movielabs.mddflib.logging.LogReference)
	 */
	@Override
	public void log(int level, int tag, String msg, File file, int lineNumber, String moduleId, String details,
			LogReference srcRef) {
		append(level, tag, msg, file, lineNumber, moduleId, details, srcRef);
	}

	protected void append(int level, int tag, String msg, File xmlFile, int line, String moduleID, String tooltip,
			LogReference srcRef) {
		if (level < minLevel) {
			return;
		}
		List<LogEntryNode> entryList = new ArrayList<LogEntryNode>();
		LogEntryNode entry = treeView.append(level, tag, msg, xmlFile, line, moduleID, tooltip, srcRef);
		entryList.add(entry);
		tableView.append(entryList);
		setSize(getWidth(), getHeight());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#setMinLevel(int)
	 */
	@Override
	public void setMinLevel(int level) {
		minLevel = level;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#getMinLevel()
	 */
	@Override
	public int getMinLevel() {
		return minLevel;
	}
}
