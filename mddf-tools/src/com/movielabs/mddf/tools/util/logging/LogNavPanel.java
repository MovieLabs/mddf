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
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jdom2.Document;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddf.MddfContext.MDDF_TYPE;
import com.movielabs.mddf.tools.MaskerDialog;
import com.movielabs.mddf.tools.TranslatorDialog;
import com.movielabs.mddf.tools.ValidatorTool;
import com.movielabs.mddf.tools.util.FileChooserDialog;
import com.movielabs.mddf.tools.util.xml.EditorMgr;
import com.movielabs.mddf.tools.util.xml.SimpleXmlEditor;
import com.movielabs.mddflib.logging.LogEntryFolder;
import com.movielabs.mddflib.logging.LogEntryNode;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.xml.MddfTarget;

/**
 * A <tt>JPanel</tt> that displays a <tt>JTree</tt> containing all log entries
 * hierarchically grouped first by the file, then by severity, and finally by
 * the category of problem (i.e., best practices, XML, CM, etc.). A
 * context-sensitive pop-up menu that is displayed when user right-clicks on a
 * tree node provides a mechanism for performing various operations on a file
 * (e.g., re-validation, editing, translation).
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class LogNavPanel extends JPanel {

	/**
	 * Implements context-sensitive pop-up menu displayed when user right-clicks on
	 * a tree node.
	 * 
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	public class TreePopupMenu extends JPopupMenu {

		private MddfTarget target;
		private boolean isXml;
		private LogEntryFolder node;

		public TreePopupMenu(TreePath selPath) {
			/*
			 * Step 1: get the context (i.e., what was selected and what is is the state &
			 * status of whatever is associated with the selected node).
			 */
			int depth = selPath.getPathCount();
			node = (LogEntryFolder) selPath.getLastPathComponent();
			LogEntryFolder fileFolder = (LogEntryFolder) selPath.getPathComponent(1);
			this.target = fileFolder.getMddfTarget();
			if (target == null) {
				return;
			}
			// TODO: should really use FILE_FMT here....
			isXml = target.getSrcFile().getAbsolutePath().endsWith(".xml");

			int maxErrLevelFound = fileFolder.getHighestLevel();
			boolean fileSelected = (node == fileFolder);
			MDDF_TYPE mddfType = target.getMddfType();
			/*
			 * Step 2: Now we can construct the pop-up and enable/disable specific menu
			 * items based on the context.
			 * 
			 */
			JMenuItem runMItem = new JMenuItem("Re-Validate");
			runMItem.setToolTipText("Re-run validation checks on selection");
			runMItem.setEnabled(fileSelected);
			add(runMItem);
			runMItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ValidatorTool.getTool().runTool();
					// ================================
					/*
					 * Moved the code handling updates to an XmlEditor to the SwingWorker in the
					 * Validation tool due to possible thread synchronization issue
					 */
					// ================================
				}
			});

			JMenuItem editXmlMItem = new JMenuItem("Show in Editor");
			add(editXmlMItem);
			editXmlMItem.setEnabled(isXml);
			editXmlMItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					EditorMgr edMgr = EditorMgr.getSingleton();
					MddfTarget target = node.getMddfTarget();
					edMgr.getEditorFor(target, parentLogger, null);
				}
			});

			add(new JSeparator());
			String encoding = fileFolder.getMddfFormat().getEncoding();
			if (encoding.equalsIgnoreCase("xlsx")) {
				JMenuItem xlateLogMItem = new JMenuItem("Translate");
				add(xlateLogMItem);
				xlateLogMItem.setEnabled(maxErrLevelFound < LogMgmt.LEV_ERR);
				xlateLogMItem.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						TranslatorDialog xlateDialog = TranslatorDialog.getDialog();
						Document doc = fileFolder.getXml();
						File srcFile = fileFolder.getFile();
						FILE_FMT curFmt = fileFolder.getMddfFormat();
						xlateDialog.setContext(curFmt, srcFile);
						Point p = tree.getLocationOnScreen();
						xlateDialog.setLocation((int) p.getX(), (int) p.getY());
						xlateDialog.setVisible(true);
						EnumSet<FILE_FMT> selections = xlateDialog.getSelections();
						if (!selections.isEmpty()) {
							MddfTarget target = new MddfTarget(doc, srcFile, parentLogger);
							ValidatorTool.getTool().runTranslation(target, selections, xlateDialog.getOutputDir(),
									xlateDialog.getOutputFilePrefix(), xlateDialog.addVersion());
						}
					}
				});

				JMenuItem compressAvailsMItem = new JMenuItem("Compress ");
				compressAvailsMItem.setToolTipText("Hide empty Excel columns");
				add(compressAvailsMItem);
				FILE_FMT curFmt = fileFolder.getMddfFormat();
				compressAvailsMItem.setEnabled(curFmt.getEncoding().equals("xlsx"));
				compressAvailsMItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						File srcFile = fileFolder.getFile();
						File saveToFile = FileChooserDialog.getFilePath("Save file as...", srcFile.getAbsolutePath(),
								null, "AVAIL", parentLogger);
						String dirPath;
						String fileName;
						if (saveToFile.exists() && saveToFile.isDirectory()) {
							dirPath = saveToFile.getPath();
							fileName = fileFolder.getFile().getName();
						} else {
							dirPath = saveToFile.getParent();
							fileName = saveToFile.getName();
						}
						ValidatorTool.getTool().compress(srcFile, dirPath, fileName);
					}
				});

				JMenuItem reformatAvailsMItem = new JMenuItem("Reformat");
				reformatAvailsMItem.setToolTipText("Reorder columns; Hide empty columns");
				add(reformatAvailsMItem);
				curFmt = fileFolder.getMddfFormat();
				reformatAvailsMItem.setEnabled(curFmt.getEncoding().equals("xlsx"));
				reformatAvailsMItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						File srcFile = fileFolder.getFile();
						File saveToFile = FileChooserDialog.getFilePath("Save file as...", srcFile.getAbsolutePath(),
								null, "AVAIL", parentLogger);
						String dirPath;
						String fileName;
						if (saveToFile.exists() && saveToFile.isDirectory()) {
							dirPath = saveToFile.getPath();
							fileName = fileFolder.getFile().getName();
						} else {
							dirPath = saveToFile.getParent();
							fileName = saveToFile.getName();
						}
						ValidatorTool.getTool().cleanup(srcFile, dirPath, fileName);
					}
				});

				JMenuItem maskAvailsMItem = new JMenuItem("Export Obfuscated");
				maskAvailsMItem.setToolTipText("Work-in-progress");
				maskAvailsMItem.setEnabled(false);
				add(maskAvailsMItem);
				maskAvailsMItem.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						MaskerDialog xlateDialog = MaskerDialog.getDialog();
						Document doc = fileFolder.getXml();
						FILE_FMT curFmt = fileFolder.getMddfFormat();
						File srcFile = fileFolder.getFile();
						xlateDialog.setContext(srcFile);
						xlateDialog.setVisible(true);
					}
				});

				add(new JSeparator());
			}

			JMenuItem deleteLogMItem = new JMenuItem("Delete from Log");
			add(deleteLogMItem);
			deleteLogMItem.setEnabled(false); // not yet implemented
			deleteLogMItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
				}
			});
			add(parentLogger.createSaveLogMenu());
		}

	}

	public class LogTree extends JTree {

		/**
		 * @param treeModel
		 */
		public LogTree(DefaultTreeModel treeModel) {
			super(treeModel);
			LogTreeRenderer renderer = new LogTreeRenderer();
			renderer.setBackgroundNonSelectionColor(backgroundPanel);
			renderer.setBackgroundSelectionColor(backgroundPanelLabel);
			setCellRenderer(renderer);
			/* add listener to trigger a pop-up menu */
			this.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (ValidatorTool.getTool().isRunning()) {
						return;
					}
					if (SwingUtilities.isRightMouseButton(e)) {
						TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
						int row = tree.getClosestRowForLocation(e.getX(), e.getY());
						if ((selPath == null) || (row < 0)) {
							return;
						}
						tree.setSelectionPath(selPath);
						TreePopupMenu contextMenu = new TreePopupMenu(selPath);
						contextMenu.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			});
		}

	}

	/**
	 * Customized <tt>TreeCellRenderer</tt> that sets the icon based on the highest
	 * level of severity for all messages found in any sub-folder.
	 * 
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	public class LogTreeRenderer extends DefaultTreeCellRenderer {
		private Icon defaultOpen = UIManager.getIcon("Tree.openIcon");
		private Icon defaultClosed = UIManager.getIcon("Tree.closedIcon");

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean exp, boolean leaf,
				int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, hasFocus);
			if (sel) {
				setBorder(BorderFactory.createLoweredBevelBorder());
			} else {
				setBorder(BorderFactory.createEmptyBorder());
			}
			if (value instanceof LogEntryFolder) {
				LogEntryFolder folder = (LogEntryFolder) value;
				int maxLevelFound = folder.getHighestLevel();
				// --- use icon to indicate level...
				Icon folderIcon = null;
				try {
					String key = LogMgmt.logLevels[maxLevelFound].toLowerCase();
					folderIcon = iconSet.get(key);
				} catch (Exception e) {
				}
				if (folderIcon != null) {
					setIcon(folderIcon);
				} else {
					setOpenIcon(defaultOpen);
					setClosedIcon(defaultClosed);
				}
			} else {
				setOpenIcon(defaultOpen);
				setClosedIcon(defaultClosed);
			}
			setOpaque(true);

			return this;
		}
	}

	// :::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static Map<String, Icon> iconSet = new HashMap<String, Icon>();

	static {
		/*
		 * Note that for now the same icon is used for both OPEN and CLOSED states but
		 * that isn't required.
		 */
		String[] levels = LogMgmt.logLevels;
		for (int i = 0; i < levels.length; i++) {
			String iconKey = levels[i].toLowerCase();
			String path = "images/" + iconKey + "-icon.png";
			Icon folderIcon = createIcon(path);
			iconSet.put(iconKey, folderIcon);
		}
	}

	static final Color errorColor = new Color(255, 100, 100); // lighter red
	static final Color warningColor = new Color(255, 250, 87);

	public static final Color backgroundPanel = new Color(205, 205, 205);
	public static final Color backgroundPanelLabel = new Color(95, 158, 160);

	private JTree tree;
	private DefaultTreeModel treeModel;
	private LogEntryFolder rootLogNode = new LogEntryFolder("", -1, LogMgmt.DEFAULT_TOOL_FOLDER_KEY);
	private Map<String, LogEntryFolder> fileFolderMap = new HashMap<String, LogEntryFolder>();
	private int masterSeqNum = 0;
	private String currentManifestId = "Default";

	private LogEntryFolder previousSelectedNode;

	private AdvLogPanel parentLogger;

	static Color resolveColor(int i, TreeNode target, Color defaultColor) {
		Color assignedColor = defaultColor;
		if (target instanceof LogEntryFolder) {
			LogEntryFolder folder = (LogEntryFolder) target;
			int maxLevelFound = folder.getHighestLevel();
			switch (maxLevelFound) {
			case LogMgmt.LEV_ERR:
				assignedColor = errorColor;
				break;
			case LogMgmt.LEV_WARN:
				assignedColor = warningColor;
				break;
			default:
			}
		}
		return assignedColor;
	}

	/**
	 * Returns an ImageIcon, or null if the path was invalid.
	 * 
	 * @param path
	 * @return
	 */
	static Icon createIcon(String path) {
		java.net.URL imgURL = imgURL = AdvLogPanel.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, "foobar");
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	LogNavPanel(AdvLogPanel parent) {
		parentLogger = parent;
		initializeGui();
	}

	/**
	 * 
	 */
	private void initializeGui() {
		setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBackground(backgroundPanel);
		add(scrollPane, BorderLayout.CENTER);
		// ...
		treeModel = new DefaultTreeModel(rootLogNode);
		tree = new LogTree(treeModel);
		tree.setEditable(false);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setShowsRootHandles(true);
		tree.setBackground(backgroundPanel);
		TreePath path1 = new TreePath(rootLogNode.getPath());
		tree.setSelectionPath(path1);
		scrollPane.getViewport().setView(tree);

		tree.setBackground(backgroundPanel);
		scrollPane.getViewport().setBackground(backgroundPanel);
		scrollPane.setBackground(backgroundPanel);
		this.setBackground(backgroundPanel);

	}

	public LogEntryFolder getFileFolder(MddfTarget target) {
		String folderKey = LogMgmt.genFolderKey(target);
		LogEntryFolder fileFolder = fileFolderMap.get(folderKey);
		return fileFolder;
	}

	/**
	 * @param target
	 * @return
	 */
	public LogEntryFolder assignFileFolder(MddfTarget target) {
		String folderKey = LogMgmt.genFolderKey(target);
		LogEntryFolder fileFolder = fileFolderMap.get(folderKey);
		if (fileFolder == null) {
			/*
			 * Folder has not yet been created.
			 */
			int suffix = 1;
			String label = "";
			MddfTarget parent = null;
			if (target == null) {
				label = LogMgmt.DEFAULT_TOOL_FOLDER_LABEL;
			} else {
				File targetFile = target.getSrcFile();
				if (targetFile != null) {
					label = targetFile.getName();
				} else {
					label = LogMgmt.DEFAULT_TOOL_FOLDER_LABEL;
				}
				  parent = target.getParentTarget();
			}
			String qualifiedName = label;
			fileFolder = (LogEntryFolder) rootLogNode.getChild(label);
			while (fileFolder != null) {
				suffix++;
				qualifiedName = label + " (" + suffix + ")";
				fileFolder = (LogEntryFolder) rootLogNode.getChild(qualifiedName);
			}
			fileFolder = new LogEntryFolder(qualifiedName, -1, folderKey);
			fileFolder.setFile(target);
			fileFolderMap.put(folderKey, fileFolder);
			/*
			 * now add to the tree. This requires knowing its parent w/in the context.
			 */ 
			if (parent == null) {
				// must be the 'VALIDATOR' folder
				rootLogNode.add(fileFolder);
			} else {
				// add as child to the folder of the 'parent'
				String parentKey = LogMgmt.genFolderKey(parent);
				LogEntryFolder parentFolder = fileFolderMap.get(parentKey);
				if (parentFolder != null) {
					parentFolder.add(fileFolder);
				} else {
					rootLogNode.add(fileFolder);
				}
			}
			// add the folders for each TAG type...
			for (int i = 0; i < LogMgmt.logLevels.length; i++) {
				LogEntryFolder levelTNode = new LogEntryFolder(LogMgmt.logLevels[i], i);
				fileFolder.add(levelTNode);
			}
			treeModel.nodeChanged(rootLogNode);
			treeModel.reload();

		}
		return fileFolder;
	}

	/**
	 * Set the file ID to associate with any new log entries. This results in all
	 * new entries being added to the folder with that identifier. If the
	 * <tt>clear</tt> flag is <tt>true</tt> the current contents of the folder will
	 * be deleted.
	 * 
	 * @param id
	 */
	LogEntryFolder setCurrentFileId(String folderKey, boolean clear) {
		if (folderKey == null) {
			LogEntryFolder fileFolder = fileFolderMap.get(LogMgmt.DEFAULT_TOOL_FOLDER_KEY);
			return fileFolder;
		}
		LogEntryFolder fileFolder = fileFolderMap.get(folderKey);
		if (clear) {
			fileFolder.deleteMsgs();
			treeModel.nodeChanged(fileFolder);
			treeModel.reload();
		}
		return fileFolder;
	}

	public void setMddfFormat(MddfTarget target, FILE_FMT format) {
		String folderKey = LogMgmt.genFolderKey(target);
		LogEntryFolder fileFolder = fileFolderMap.get(folderKey);
		if (fileFolder != null) {
			fileFolder.setMddfFormat(format);
		}
	}

	/**
	 * 
	 */
	public void clearLog() {
		rootLogNode.deleteMsgs();
		rootLogNode.removeAllChildren();
		fileFolderMap = new HashMap<String, LogEntryFolder>();
		masterSeqNum = 0;
		treeModel.reload();
	}

	public void clearLog(MddfTarget target) {
		LogEntryFolder targetFolder = getFileFolder(target);
		targetFolder.deleteMsgs();
		treeModel.reload();
	}

	/**
	 * @param level
	 * @param tag
	 * @param details
	 * @param path
	 * @param moduleID
	 * @param tooltip
	 * @param srcRef
	 * @return
	 */
	public LogEntryNode append(int level, int tag, String msg, LogEntryFolder logFolder, int line, String moduleID,
			String tooltip, LogReference srcRef) {
		String tagAsText = LogMgmt.logTags[tag];
		// First get correct 'folder'
		if (logFolder == null) {
			logFolder = rootLogNode; // getFileFolder(null, true);
		}
		LogEntryFolder byLevel = (LogEntryFolder) logFolder.getChild(LogMgmt.logLevels[level]);
		LogEntryFolder tagNode = (LogEntryFolder) byLevel.getChild(tagAsText);
		if (tagNode == null) {
			/*
			 * Add tag-specific node for previously unused folder (i.e., we use
			 * lazy-constructor design pattern)
			 */
			tagNode = new LogEntryFolder(tagAsText, level);
			/*
			 * need to keep order consistent so figure where to insert. Allow for situation
			 * where it is the first node or the child set is empty
			 */
			if (tag == 0) {
				treeModel.insertNodeInto(tagNode, byLevel, 0);
			} else {
				int j = -1;
				for (int i = tag - 1; i >= 0; i--) {
					DefaultMutableTreeNode priorNode = byLevel.getChild(LogMgmt.logTags[i]);
					if (priorNode != null) {
						j = byLevel.getIndex(priorNode);
					}
				}
				treeModel.insertNodeInto(tagNode, byLevel, j + 1);
			}
			byLevel.add(tagNode);
		}
		/* now create a new LogEntryNode and add it to folder. */
		LogEntryNode entryNode = new LogEntryNode(level, tagNode, msg, logFolder, line, moduleID, masterSeqNum++,
				tooltip, srcRef);
		tagNode.addMsg(entryNode);
		treeModel.nodeChanged(tagNode);
		return entryNode;
	}

	/**
	 * @param advLogPanel
	 */
	public void addListener(TreeSelectionListener listener) {
		tree.addTreeSelectionListener(listener);
	}

	/**
	 * @return
	 * 
	 */
	public List<LogEntryNode> getSelectedMsgSet() {
		LogEntryFolder node = (LogEntryFolder) tree.getLastSelectedPathComponent();
		if (node != null) {
			previousSelectedNode = node;
		} else if (previousSelectedNode == null) {
			node = rootLogNode;
		} else {
			node = previousSelectedNode;
		}
		if (node != null) {
			List<LogEntryNode> msgList = node.getMsgList();
			return msgList;
		} else {
			return null;
		}
	}

	public void collapse() {
		int row = tree.getRowCount() - 1;
		while (row >= 0) {
			tree.collapseRow(row);
			row--;
		}
	}

	public void expand() {
		TreeNode rootNode = (TreeNode) treeModel.getRoot();
		expand(rootNode);
	}

	private void expand(TreeNode node) {
		int count = treeModel.getChildCount(node);
		if (count == 0)
			return;
		for (int i = 0; i < count; i++) {
			TreeNode childNode = (TreeNode) treeModel.getChild(node, i);
			TreeNode[] pNodes = treeModel.getPathToRoot(childNode);
			TreePath path = new TreePath(pNodes);
			tree.scrollPathToVisible(path);
		}

	}

}
