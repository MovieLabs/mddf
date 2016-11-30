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

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.apache.batik.util.gui.xmleditor.XMLDocument;
import org.apache.batik.util.gui.xmleditor.XMLTextEditor;

import com.movielabs.mddf.tools.GenericTool;
import com.movielabs.mddf.tools.util.FileChooserDialog;
import com.movielabs.mddf.tools.util.logging.LoggerWidget;
import com.movielabs.mddflib.logging.LogEntry;
import com.movielabs.mddflib.logging.LogEntryComparator;
import com.movielabs.mddflib.logging.LogEntryFolder;
import com.movielabs.mddflib.logging.LogEntryNode;
import com.movielabs.mddflib.logging.LogEntryNode.Field;

import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import java.awt.Color;
import java.awt.Component;

import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.ScrollPaneConstants;
import java.awt.Dimension;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class SimpleXmlEditor {

	private static final Color SELECTION_BACKGROUND_COLOR = new Color(255, 255, 180);
	private JFrame frame;
	private XMLTextEditor xmlEditorPane;
	private JScrollPane scrollPane;
	private JPanel footerPanel;
	private TextLineNumber tln;
	protected SimpleXmlEditor spawnedEditor;
	private File curFile;
	private JMenuBar menuBar;
	private JLabel headerLabel;
	private JMenu editMenu;
	private JMenuItem undoMItem;
	private JMenuItem cutMItem;
	private JMenuItem copyMItem;
	private JMenuItem pasteMItem;
	private JMenuItem findReplaceMItem;
	private JMenuItem findNextMItem;
	private JMenuItem findPrevMItem;
	private JMenu fileMenu;
	private JMenuItem saveMItem;
	private JMenuItem saveAsMItem;
	private FindReplaceDialog findReplDialog;
	private EditActionListener editActionHandler;
	private EditorMonitor owner = null;
	private ArrayList<LogEntryNode> logMarkerList;
	private JLabel msgField;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SimpleXmlEditor window = new SimpleXmlEditor();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * @param logEntry
	 * @return
	 */
	public static SimpleXmlEditor spawn(LogEntry logEntry) {
		String filePath = logEntry.getFile().getAbsolutePath();
		SimpleXmlEditor editor = spawn(filePath);
		if (editor != null) {
			if (logEntry instanceof LogEntryNode) {
				editor.goTo((LogEntryNode) logEntry);
			}
		}
		return editor;
	}

	public static SimpleXmlEditor spawn(String filePath) {
		File file = new File(filePath);
		if (!file.exists() || !file.isFile()) {
			return null;
		}
		SimpleXmlEditor spawnedEditor = new SimpleXmlEditor();
		JFrame spawnedFrame = spawnedEditor.getFrame();
		spawnedFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		spawnedFrame.setVisible(true);
		if (filePath != null && !filePath.isEmpty()) {
			spawnedEditor.load(filePath);
		}
		spawnedEditor.goTo(0);
		return spawnedEditor;
	}

	/**
	 * Create the application.
	 */
	public SimpleXmlEditor() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setIconImage(Toolkit.getDefaultToolkit()
				.getImage(FindReplaceDialog.class.getResource(GenericTool.imageRsrcPath + "icon_movielabs.jpg")));

		frame.setBounds(100, 100, 1024, 535);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BorderLayout(0, 0));
		headerPanel.add(getMenuBar(), BorderLayout.SOUTH);
		frame.getContentPane().add(headerPanel, BorderLayout.NORTH);

		headerLabel = new JLabel("Simple XML Editor");
		headerLabel.setHorizontalAlignment(JLabel.CENTER);
		headerPanel.add(headerLabel, BorderLayout.CENTER);

		JPanel panel_1 = new JPanel();
		frame.getContentPane().add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));

		panel_1.add(getScrollPane(), BorderLayout.CENTER);

		frame.getContentPane().add(getFooterPanel(), BorderLayout.SOUTH);
		UIListener wl = new UIListener(this);
		frame.addWindowListener(wl);
	}

	boolean load(String filePath) {
		File file = new File(filePath);
		if (!file.exists() || !file.isFile()) {
			return false;
		}
		try {
			xmlEditorPane.read(new FileReader(file), file);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		curFile = file;
		headerLabel.setText(curFile.getName());
		// xmlEditorPane.goToLine(line - 1);
		return true;
	}

	public void setVisible(boolean show) {
		if (show) {
			// Refresh log markers
			LoggerWidget logger = GenericTool.consoleLogger;
			LogEntryFolder logFolder = logger.getFileFolder(curFile);
			List<LogEntryNode> msgList = logFolder.getMsgList();
			this.showLogMarkers(msgList);
		}
		getFrame().setVisible(show);
		getFrame().repaint();
	}

	/**
	 * @param logEntry
	 */
	public void goTo(LogEntryNode logEntry) {
		int line = logEntry.getLine();
		goTo(line);
	}

	/**
	 * @param line
	 */
	public void goTo(int line) {
		xmlEditorPane.goToLine(line - 1);

		if (frame.getState() == Frame.ICONIFIED) {
			frame.setState(Frame.NORMAL);
		}
	}

	private JMenuBar getMenuBar() {
		if (menuBar == null) {
			menuBar = new JMenuBar();

			menuBar.add(getFileMenu());
			menuBar.add(getEditMenu());

		}
		return menuBar;
	}

	private JMenu getFileMenu() {
		if (fileMenu == null) {
			fileMenu = new JMenu("File");

			saveMItem = new JMenuItem("Save");
			saveMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
			saveMItem.setActionCommand(EditActionListener.AC_SAVE);
			saveMItem.addActionListener(getEditActionListener());
			fileMenu.add(saveMItem);

			saveAsMItem = new JMenuItem("Save As");
			saveAsMItem.setActionCommand(EditActionListener.AC_SAVE_AS);
			saveAsMItem.addActionListener(getEditActionListener());
			fileMenu.add(saveAsMItem);
		}
		return fileMenu;
	}

	private JMenu getEditMenu() {
		if (editMenu == null) {
			editMenu = new JMenu("Edit");

			undoMItem = new JMenuItem("Undo");
			editMenu.add(undoMItem);
			editMenu.add(new JSeparator());

			cutMItem = new JMenuItem("Cut");
			cutMItem.setMnemonic(KeyEvent.VK_T);
			cutMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK));
			cutMItem.setActionCommand(EditActionListener.AC_CUT);
			cutMItem.addActionListener(getEditActionListener());
			editMenu.add(cutMItem);

			copyMItem = new JMenuItem("Copy");
			copyMItem.setMnemonic(KeyEvent.VK_C);
			copyMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
			copyMItem.setActionCommand(EditActionListener.AC_COPY);
			copyMItem.addActionListener(getEditActionListener());
			editMenu.add(copyMItem);

			pasteMItem = new JMenuItem("Paste");
			pasteMItem.setMnemonic(KeyEvent.VK_P);
			pasteMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
			pasteMItem.setActionCommand(EditActionListener.AC_PASTE);
			pasteMItem.addActionListener(getEditActionListener());
			editMenu.add(pasteMItem);

			editMenu.add(new JSeparator());

			findReplaceMItem = new JMenuItem("Find/Replace");
			findReplaceMItem.setMnemonic(KeyEvent.VK_F);
			findReplaceMItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
			findReplaceMItem.setActionCommand(EditActionListener.AC_FR);
			findReplaceMItem.addActionListener(getEditActionListener());
			editMenu.add(findReplaceMItem);

			findNextMItem = new JMenuItem("Find Next");
			findNextMItem.setActionCommand(EditActionListener.AC_FN);
			findNextMItem.addActionListener(getEditActionListener());
			editMenu.add(findNextMItem);

			findPrevMItem = new JMenuItem("Find Previous");
			findPrevMItem.setActionCommand(EditActionListener.AC_FP);
			findPrevMItem.addActionListener(getEditActionListener());
			editMenu.add(findPrevMItem);

		}
		return editMenu;
	}

	/**
	 * @return
	 */
	protected FindReplaceDialog getFindReplDialog() {
		if (findReplDialog == null) {
			findReplDialog = new FindReplaceDialog(this);
		}
		return findReplDialog;
	}

	private JPanel getFooterPanel() {
		if (footerPanel == null) {
			footerPanel = new JPanel();
			footerPanel.setBackground(new Color(200, 200, 200));
			getMsgField();
			footerPanel.add(msgField);

		}
		return footerPanel;
	}

	/**
	 * 
	 */
	JLabel getMsgField() {
		if (msgField == null) {
			msgField = new JLabel();
			msgField.setText(" ");
			msgField.setBackground(new Color(220, 220, 220));
		}
		return msgField;
	}

	private Component getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane();
			scrollPane.setPreferredSize(new Dimension(300, 300));
			scrollPane.setMinimumSize(new Dimension(220, 220));
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			scrollPane.setViewportBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));

			scrollPane.getViewport().setView(getXmlEditorPane());
			tln = new TextLineNumber(this);
			scrollPane.setRowHeaderView(tln);

			Color backgroundColor = new Color(180, 255, 180);
			scrollPane.getViewport().setBackground(backgroundColor);
			scrollPane.setBackground(backgroundColor);
		}
		return scrollPane;

	}

	/**
	 * @return
	 */
	XMLTextEditor getXmlEditorPane() {
		if (xmlEditorPane == null) {
			xmlEditorPane = new XMLTextEditor();
			xmlEditorPane.setSelectionColor(SELECTION_BACKGROUND_COLOR);
		}
		return xmlEditorPane;
	}

	/**
	 * @return the frame
	 */
	JFrame getFrame() {
		return frame;
	}

	/**
	 * @return the curFile
	 */
	public File getCurFile() {
		return curFile;
	}

	/**
	 * @return
	 */
	EditActionListener getEditActionListener() {
		if (editActionHandler == null) {
			editActionHandler = new EditActionListener();
		}
		return editActionHandler;
	}

	/**
	 * Return the absolute path of the XML file being edited.
	 * 
	 * @return path
	 */
	public String getFilePath() {
		return curFile.getAbsolutePath();
	}

	/**
	 * Sets the location of the <tt>Frame</tt> relative to another
	 * <tt>Component</tt> and de-iconifies the <tt>Frame</tt> if it is currently
	 * in a minimized state.
	 * 
	 * @param parent
	 * 
	 * @see java.awt.Windowr#setLocationRelativeTo(java.awt.Component)
	 */
	public void setLocation(Point pointOnScreen) {
		frame.setLocation(pointOnScreen);
		if (frame.getState() == Frame.ICONIFIED) {
			frame.setState(Frame.NORMAL);
		}
	}

	public boolean isShowing() {
		boolean status = (frame.getState() == Frame.NORMAL) && frame.isShowing();
		return status;
	}

	/**
	 * Set the monitoring entity. This will be notified of significant
	 * life-cycle events such as the user closing the editor.
	 * 
	 * @param monitor
	 */
	public void setMonitor(EditorMonitor monitor) {
		owner = monitor;
	}

	/**
	 * Show markers (i.e., icons) on lines corresponding to the provided list of
	 * <tt>LogEntryNode</tt> instances. If the <tt>List</tt> is empty or null,
	 * no markers will be shown.
	 * 
	 * @param msgList
	 */
	public void showLogMarkers(List<LogEntryNode> msgList) {
		logMarkerList = new ArrayList<LogEntryNode>();
		logMarkerList.addAll(msgList);
		Collections.sort(logMarkerList, new LogEntryComparator(Field.Line));
		tln.setLineMarkers(logMarkerList);
		frame.repaint();
	}

	public String getSelected() {
		return xmlEditorPane.getSelectedText();
	}

	// =================================================
	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	// ==================================================
	/**
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	public class EditActionListener implements ActionListener {

		/* Action Command strings... */
		static final String AC_CUT = "cut";
		static final String AC_COPY = "copy";
		static final String AC_PASTE = "paste";
		static final String AC_FR = "f_repl";
		static final String AC_FN = "f_next";
		static final String AC_FP = "f_prev";
		static final String AC_REP = "r";
		static final String AC_REP_F = "r_f";
		static final String AC_REP_ALL = "r_all";
		static final String AC_SAVE = "s";
		static final String AC_SAVE_AS = "s_all";

		EditActionListener() {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.
		 * ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			execute(cmd);
		}

		void execute(String cmd) {
			FindReplaceDialog frDialog = getFindReplDialog();
			findReplDialog.setStatusMsg("");
			String text;
			switch (cmd) {
			case AC_CUT:
				xmlEditorPane.cut();
				break;
			case AC_COPY:
				xmlEditorPane.copy();
				break;
			case AC_PASTE:
				xmlEditorPane.paste();
				break;
			case AC_FR:
				/*
				 * Instead of directly accessing 'findReplDialog', use the
				 * getter. That way a new dialog will be instantiated if
				 * necessary.
				 */
				frDialog.setVisible(true);
				frDialog.setLocationRelativeTo(frame);
				if (frDialog.getState() == Frame.ICONIFIED) {
					frDialog.setState(Frame.NORMAL);
				}
				break;
			case AC_FN:
				doFindNext();
				break;
			case AC_FP:
				// Find Previous match
				doFindPrevious();
				break;
			case AC_REP:
				xmlEditorPane.replaceSelection(findReplDialog.getReplaceText());
				break;
			case AC_REP_F:
				// Replace currently selected text, then find next match
				xmlEditorPane.replaceSelection(findReplDialog.getReplaceText());
				doFindNext();
				break;
			case AC_REP_ALL:
				doReplaceAll();
				break;
			case AC_SAVE:
				doSave();
				break;
			case AC_SAVE_AS:
				doSaveAs();
				break;
			default:
			}

		}

		/**
		 * 
		 */
		private void doReplaceAll() {
			String targetText = findReplDialog.getFindText();
			if (targetText.isEmpty()) {
				findReplDialog.setStatusMsg("Text to be replaced has not be entered.");
				return;
			}
			/* Replace all matching text */
			boolean working = true;
			int count = 0;
			/*
			 * There may or may not be currently selected text. If there is, it
			 * may or may not match the search criteria.
			 */
			String curSelText = xmlEditorPane.getSelectedText();
			boolean selectionMatches;
			if (curSelText != null && !curSelText.isEmpty()) {
				if (findReplDialog.getCaseChkBox().isSelected()) {
					selectionMatches = curSelText.equals(targetText);
				} else {
					selectionMatches = curSelText.equalsIgnoreCase(targetText);
				}
			} else {
				selectionMatches = false;
			}
			if (!selectionMatches) {
				working = doFindNext();
			}
			while (working) {
				xmlEditorPane.replaceSelection(findReplDialog.getReplaceText());
				count++;
				working = doFindNext();
			}
			findReplDialog.setStatusMsg("Replaced " + count + " occurances.");
		}

		private boolean doFindNext() {
			String targetText = findReplDialog.getFindText();
			if (targetText.isEmpty()) {
				targetText = xmlEditorPane.getSelectedText();
				findReplDialog.setFindText(targetText);
			}
			int fromIndex = xmlEditorPane.getSelectionEnd();
			// Find Next match
			XMLDocument xmlDoc = (XMLDocument) xmlEditorPane.getDocument();
			boolean caseSensitive = findReplDialog.getCaseChkBox().isSelected();
			int nextStart;
			try {
				nextStart = xmlDoc.find(targetText, fromIndex, caseSensitive);
			} catch (BadLocationException e1) {
				findReplDialog.setStatusMsg("BadLocationException");
				return false;
			}
			if (nextStart < 0) {
				findReplDialog.setStatusMsg("String not found");
				return false;
			} else {
				xmlEditorPane.setSelectionStart(nextStart);
				xmlEditorPane.setSelectionEnd(nextStart + targetText.length());
			}
			return true;
		}

		private boolean doFindPrevious() {
			String targetText = findReplDialog.getFindText();
			if (targetText.isEmpty()) {
				targetText = xmlEditorPane.getSelectedText();
				findReplDialog.setFindText(targetText);
			}
			int fromIndex = 0;
			int cutOffIndex = xmlEditorPane.getSelectionStart();

			XMLDocument xmlDoc = (XMLDocument) xmlEditorPane.getDocument();
			boolean caseSensitive = findReplDialog.getCaseChkBox().isSelected();
			int nextStart = 0;
			int previousStart = -1;
			while (nextStart != -1) {
				try {
					nextStart = xmlDoc.find(targetText, fromIndex, caseSensitive);
				} catch (BadLocationException e1) {
					findReplDialog.setStatusMsg("BadLocationException");
					return false;
				}
				if (nextStart < 0) {
					findReplDialog.setStatusMsg("String not found");
					return false;
				} else if ((nextStart >= cutOffIndex) && (previousStart != -1)) {
					xmlEditorPane.setSelectionStart(previousStart);
					xmlEditorPane.setSelectionEnd(previousStart + targetText.length());
					return true;
				} else if (nextStart >= cutOffIndex) {
					findReplDialog.setStatusMsg("String not found");
					return false;
				} else {
					fromIndex = nextStart + 1;
					previousStart = nextStart;
				}
			}
			return false;
		}

		/**
		 * 
		 */
		protected void doSave() {
			try {
				xmlEditorPane.write(new FileWriter(curFile));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		/**
		 * 
		 */
		private void doSaveAs() {
			FileFilter filter = new FileNameExtensionFilter("Manifest File", "xml");
			File outFile = FileChooserDialog.getFilePath("Save as file", null, filter, "CMM", frame);
			if (outFile == null) {
				return;
			}
			try {
				outFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				String errMsg = "<p>Unable to create new file :<br/> " + e.getMessage() + "</p>";
				JOptionPane.showMessageDialog(frame,
						"<html><div style='text-align: center;'>" + errMsg + "</style></html>", "Unable to Save",
						JOptionPane.ERROR_MESSAGE);
			}
			if (outFile.canWrite()) {
				curFile = outFile;
				doSave();
			}
		}

	}

	/**
	 * A <tt>WindowListener</tt> implementation that monitors the
	 * <tt>JFrame</tt> and, where appropriate, passes event notifications to the
	 * parent component that spawned it (e.g., the <tt>ValidatorTool</tt>).
	 * 
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	public class UIListener implements WindowListener {

		private SimpleXmlEditor myEditor;

		/**
		 * @param simpleXmlEditor
		 */
		public UIListener(SimpleXmlEditor xmlEditor) {
			this.myEditor = xmlEditor;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.WindowListener#windowOpened(java.awt.event.
		 * WindowEvent)
		 */
		@Override
		public void windowOpened(WindowEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.WindowListener#windowClosing(java.awt.event.
		 * WindowEvent)
		 */
		@Override
		public void windowClosing(WindowEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.WindowListener#windowClosed(java.awt.event.
		 * WindowEvent)
		 */
		@Override
		public void windowClosed(WindowEvent e) {
			if (owner != null) {
				owner.editorHasClosed(myEditor);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.WindowListener#windowIconified(java.awt.event.
		 * WindowEvent)
		 */
		@Override
		public void windowIconified(WindowEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.
		 * WindowEvent)
		 */
		@Override
		public void windowDeiconified(WindowEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.WindowListener#windowActivated(java.awt.event.
		 * WindowEvent)
		 */
		@Override
		public void windowActivated(WindowEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.
		 * WindowEvent)
		 */
		@Override
		public void windowDeactivated(WindowEvent e) {
		}

	}

}
