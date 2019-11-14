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
import java.util.Stack;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.apache.poi.ss.usermodel.Cell;
import com.movielabs.mddf.tools.util.xml.EditorMgr;
import com.movielabs.mddf.tools.util.xml.SimpleXmlEditor;
import com.movielabs.mddflib.logging.LogEntryFolder;
import com.movielabs.mddflib.logging.LogEntryNode;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.xml.MddfTarget;

/**
 * A composite UI component that provides the user with the ability to filter,
 * sort, and navigate thru a hierarchically structured log file. An
 * <tt>AdvLogPanel</tt> contains two sub-components:
 * <ul>
 * <li>a table-based display of the log messages that includes support for
 * sorting, and</li>
 * <li>a tree-based <i>navigator</i> that provides the ability to filter the set
 * of messages being displayed.</li>
 * </ul>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class AdvLogPanel extends JPanel implements LoggerWidget, TreeSelectionListener {

	static final int leftWidth = 250;
	private LogNavPanel treeView;
	private LogPanel tableView;
	private JSplitPane splitPane;
	private JMenu saveLogMenu; 
	private Stack<String> contextStack = new Stack<String>();
	private String previousContext = null;
	private int minLevel = LogMgmt.LEV_WARN;
	private boolean infoIncluded = true;
	private JTextField statusTextField;
	private LogEntryFolder curDefaultFolder; 

	public AdvLogPanel() {
		treeView = new LogNavPanel(this);
		treeView.addListener(this);
		tableView = new LogPanel();
		tableView.addMouseListener(new PopClickListener(this));
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeView, tableView);
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(leftWidth);
		splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {
				String propertyName = pce.getPropertyName();
				JSplitPane sourceSplitPane = (JSplitPane) pce.getSource();
				if (propertyName.equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
					setSize(getWidth(), getHeight());
				}
			}
		});
		tableView.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				// TODO Auto-generated method stub

				System.out.println("AdvLogPanel: tableView PropertyChangeEvent");
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
		// this needs to come last ....
		curDefaultFolder = treeView.assignFileFolder(null);
	}

	public void setSize() {
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
			saveLogMenu = createSaveLogMenu(tableView);
		}
		return saveLogMenu;
	}

	public JMenuItem createSaveLogMenu() {
		return createSaveLogMenu(tableView);
	}

	private static JMenu createSaveLogMenu(LogPanel targetView) {
		JMenu menu = new JMenu("Save as...");
		JMenuItem saveCsvMItem = new JMenuItem("CSV");
		menu.add(saveCsvMItem);
		JMenuItem saveXmlMItem = new JMenuItem("XML");
		saveXmlMItem.setEnabled(false);
		saveXmlMItem.setToolTipText("not yet implemented");
		menu.add(saveXmlMItem);
		saveCsvMItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					targetView.saveAs("csv");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		return menu;
	}

	@Override
	public void clearLog(MddfTarget target) {
		treeView.clearLog(target);
		this.invalidate();
		this.repaint();
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
		System.gc();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event
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

		setSize(getWidth(), getHeight());
	}

	/**
	 * @param logEntry
	 */
	protected void showEditor(LogEntryNode logEntry) {
		// Key is the ABSOLUTE path to XML file
		String key = logEntry.getSrcFilePath();
		if (key == null || (key.isEmpty())) {
			JOptionPane.showMessageDialog(this, "The selected logEntry is not linked to a file.", "Unsupported Request",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		// BUG FIX: Handle both '.xml' and '.XML' endings...
		key = key.toLowerCase();
		if (key.endsWith(".xml")) {
			MddfTarget target = logEntry.getSrcFileNode().getMddfTarget();
			SimpleXmlEditor editor = EditorMgr.getSingleton().getEditorFor(target, this, logEntry);
			if (editor != null) {
				editor.setVisible(true);
			}
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

	public LogEntryFolder assignFileFolder(MddfTarget target) {
		return treeView.assignFileFolder(target);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#setCurrentFile(java.io.File)
	 */
	public void setCurrentFile(File targetFile, boolean clear) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#pushFileContext(java.io.File,
	 * boolean)
	 */
	@Override
	public LogEntryFolder pushFileContext(MddfTarget target) {
		/*
		 * first eliminate redundant pushes
		 */
		if ((!contextStack.isEmpty()) && (target.getKey() == contextStack.peek())) {
			return curDefaultFolder;
		}
		curDefaultFolder = target.getLogFolder();
		treeView.setCurrentFileId(target.getKey(), false);
		contextStack.push(target.getKey());
		valueChanged(null);
		return curDefaultFolder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#popFileContext()
	 */
	@Override
	public void popFileContext(MddfTarget assumedTopTarget) {
		if (contextStack.isEmpty()) {
			// S/W error
			return;
		}
		String curContextKey = contextStack.peek();
		String assumedKey = assumedTopTarget.getKey();
		if (!assumedKey.equals(curContextKey)) {
			/*
			 * check for redundant 'pop'. These get ignored same way we ignore redundant
			 * push.
			 */
			if (previousContext != null) {
				if (previousContext.equals(assumedKey)) {
					return;
				}
			}

			// something is out of wack
			throw new IllegalStateException("ContextStack not popped.... file mis-match (current top: "
					+ curDefaultFolder.getLabel() + ", assumed top: " + assumedTopTarget.getSrcFile().getName());
		}
		previousContext = contextStack.pop();
		/*
		 * are we at top of stack (i.e., done with a file and back to 'tool-level
		 * logging')?
		 */
		MddfTarget parentTarget = assumedTopTarget.getParentTarget();
		if (parentTarget == null) {
			curDefaultFolder = treeView.setCurrentFileId(DEFAULT_TOOL_FOLDER_KEY, false);
		} else {
			curDefaultFolder = treeView.setCurrentFileId(parentTarget.getKey(), false);
		}
		valueChanged(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddf.util.UiLogger#saveAs(java.io.File, java.lang.String)
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
		logIssue(tag, level, target, null, msg, explanation, srcRef, moduleId);
	}

	public void logIssue(int tag, int level, Object targetData, LogEntryFolder folder, String msg, String explanation,
			LogReference srcRef, String moduleId) {
		int lineNum = LogMgmt.resolveLineNumber(targetData);
		if (targetData != null) {
			if (targetData instanceof Cell) {
				/* Prefix an 'explanation' with column ID (e.g., 'X', 'AA') */
				int colNum = ((Cell) targetData).getColumnIndex();
				String prefix = "Column " + LogMgmt.mapColNum(colNum);
				if ((explanation == null) || (explanation.isEmpty())) {
					explanation = prefix;
				} else {
					explanation = prefix + ": " + explanation;
				}
			}
		}
		append(level, tag, msg, folder, lineNum, moduleId, explanation, srcRef);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#log(int, int, java.lang.String,
	 * java.io.File, java.lang.String)
	 */
	@Override
	public void log(int level, int tag, String msg, MddfTarget file, String moduleId) {
		append(level, tag, msg, file, -1, moduleId, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddflib.logging.LogMgmt#log(int, int, java.lang.String,
	 * java.io.File, int, java.lang.String, java.lang.String,
	 * com.movielabs.mddflib.logging.LogReference)
	 */
	@Override
	public void log(int level, int tag, String msg, MddfTarget targetFile, Object targetData, String moduleId,
			String explanation, LogReference srcRef) {

		int lineNum = LogMgmt.resolveLineNumber(targetData);
		if (targetData != null) {
			if (targetData instanceof Cell) {
				/* Prefix an 'explanation' with column ID (e.g., 'X', 'AA') */
				int colNum = ((Cell) targetData).getColumnIndex();
				String prefix = "Column " + LogMgmt.mapColNum(colNum);
				if ((explanation == null) || (explanation.isEmpty())) {
					explanation = prefix;
				} else {
					explanation = prefix + ": " + explanation;
				}
			}
		}
		append(level, tag, msg, targetFile, lineNum, moduleId, explanation, srcRef);
	}

	/**
	 * @param level
	 * @param tag
	 * @param msg
	 * @param target
	 * @param line
	 * @param moduleID
	 * @param tooltip
	 * @param srcRef
	 */
	protected void append(int level, int tag, String msg, MddfTarget target, int line, String moduleID, String tooltip,
			LogReference srcRef) {
		LogEntryFolder logFolder = null;
		if (target == null) {
			logFolder = curDefaultFolder;
		} else {
			logFolder = target.getLogFolder();
		}
		append(level, tag, msg, logFolder, line, moduleID, tooltip, srcRef);
	}

	protected void append(int level, int tag, String msg, LogEntryFolder logFolder, int line, String moduleID,
			String tooltip, LogReference srcRef) {
		if (level < minLevel) {
			return;
		}
		if (level == LogMgmt.LEV_INFO) {
			if (statusTextField != null) {
				statusTextField.setText(msg);
			}
			if (!infoIncluded) {
				return;
			}
		}
		if (logFolder == null) {
			logFolder = curDefaultFolder;
		}
		List<LogEntryNode> entryList = new ArrayList<LogEntryNode>();
		LogEntryNode entry = treeView.append(level, tag, msg, logFolder, line, moduleID, tooltip, srcRef);
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

	/**
	 * @return the infoIncluded
	 */
	public boolean isInfoIncluded() {
		return infoIncluded;
	}

	/**
	 * @param infoIncluded the infoIncluded to set
	 */
	public void setInfoIncluded(boolean infoIncluded) {
		this.infoIncluded = infoIncluded;
	}

	/**
	 * @return the LogNavPanel
	 */
	public LogNavPanel getLogNavPanel() {
		return treeView;
	}

	public void setStatusDisplay(JTextField txtStatus) {
		statusTextField = txtStatus;

	}
}
