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
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.movielabs.mddflib.logging.LogEntryFolder;
import com.movielabs.mddflib.logging.LogEntryNode;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class LogNavPanel extends JPanel {

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
		}

	}

	/**
	 * Customized <tt>TreeCellRenderer</tt> that sets the icon based on the
	 * highest level of severity for all messages found in any sub-folder.
	 * 
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	public class LogTreeRenderer extends DefaultTreeCellRenderer {
		private Icon defaultOpen = UIManager.getIcon("Tree.openIcon");
		private Icon defaultClosed = UIManager.getIcon("Tree.closedIcon");

		/*
		 * Note that for now the same icon is used for both OPEN and CLOSED
		 * states but that isn't required.
		 */
		private Icon errorFolderOpen = createIcon("images/error-icon.png");
		// private Icon errorFolderClosed = errorFolderOpen;

		private Icon warningFolderOpen = createIcon("images/warning-icon.png");
		// private Icon warningFolderClosed = warningFolderOpen;

		private Icon debugFolderOpen = createIcon("images/debug-icon.png");
		// private Icon debugFolderClosed = debugFolderOpen;

		private Icon infoFolderOpen = createIcon("images/info-icon.png");
		// private Icon infoFolderClosed = infoFolderOpen;

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
				switch (maxLevelFound) {
				case LogMgmt.LEV_ERR:
					setIcon(errorFolderOpen);
					break;
				case LogMgmt.LEV_WARN:
					setIcon(warningFolderOpen);
					break;
				case LogMgmt.LEV_DEBUG:
					setIcon(debugFolderOpen);
					break;
				case LogMgmt.LEV_INFO:
					setIcon(infoFolderOpen);
					break;
				default:
					setOpenIcon(defaultOpen);
					setClosedIcon(defaultClosed);
					break;
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

	static final Color errorColor = new Color(255, 100, 100); // lighter red
	static final Color warningColor = new Color(255, 250, 87);

	public static final Color backgroundPanel = new Color(205, 205, 205);
	public static final Color backgroundPanelLabel = new Color(95, 158, 160);

	// public static final Color backgroundHdrPanel = new Color(20, 52, 103);
	// public static final Color disabledTextColor = new Color(200, 10, 10);

	private JTree tree;
	private DefaultTreeModel treeModel;
	private LogEntryFolder rootLogNode = new LogEntryFolder("", -1);
	private int masterSeqNum = 0;
	private String currentManifestId = "Default";

	private LogEntryFolder previousSelectedNode;

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
	public Icon createIcon(String path) { 
		java.net.URL imgURL = imgURL = AdvLogPanel.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, "foobar");
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	LogNavPanel() {
		initializeGui();
	}

	LogEntryFolder getFileFolder(String fileName) {
		LogEntryFolder fileFolder = (LogEntryFolder) rootLogNode.getChild(fileName);
		if (fileFolder == null) {
			fileFolder = new LogEntryFolder(fileName, -1);
			rootLogNode.add(fileFolder);
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

		// renderer.setBackgroundNonSelectionColor(backgroundPanel);
		// renderer.setBackgroundSelectionColor(backgroundPanelLabel);
	}

	/**
	 * Set the file ID to associate with any new log entries. This results in
	 * all new entries being added to the folder with that identifier. If the
	 * <tt>clear</tt> flag is <tt>true</tt> the current contents of the folder
	 * will be deleted.
	 * 
	 * @param id
	 */
	public void setCurrentFileId(String id, boolean clear) {
		currentManifestId = id;
		LogEntryFolder fileFolder = getFileFolder(id);
		if (clear) {
			fileFolder.deleteMsgs();
			treeModel.nodeChanged(fileFolder);
			treeModel.reload();
		}
	}

	/**
	 * 
	 */
	public void clearLog() {
		rootLogNode.deleteMsgs();
		rootLogNode.removeAllChildren();
		masterSeqNum = 0;
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
	public LogEntryNode append(int level, int tag, String msg, File xmlFile, int line, String moduleID,
			String tooltip, LogReference srcRef) {
		String tagAsText = LogMgmt.logTags[tag];
		// First get correct 'folder'
		LogEntryFolder byManifestFile = getFileFolder(currentManifestId);
		LogEntryFolder byLevel = (LogEntryFolder) byManifestFile.getChild(LogMgmt.logLevels[level]);
		LogEntryFolder tagNode = (LogEntryFolder) byLevel.getChild(tagAsText);
		if (tagNode == null) {
			/*
			 * Add tag-specific node for previously unused folder (i.e., we use
			 * lazy-constructor design pattern)
			 */
			tagNode = new LogEntryFolder(tagAsText, level);
			/*
			 * need to keep order consistent so figure where to insert. Allow
			 * for situation where it is the first node or the child set is
			 * empty
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
		LogEntryNode entryNode = new LogEntryNode(level, tagNode, msg, xmlFile, line, moduleID, masterSeqNum++,
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
