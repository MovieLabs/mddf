/**
 * Created Oct 23, 2015 
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
package com.movielabs.mddf.tools;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Frame;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import java.awt.Color;

import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.TreePath;

import javax.swing.JButton;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;

import com.movielabs.mddf.tools.ValidationController; 
import com.movielabs.mddf.tools.util.AboutDialog;
import com.movielabs.mddf.tools.util.logging.AdvLogPanel;
import com.movielabs.mddf.tools.util.logging.LogNavPanel;
import com.movielabs.mddf.tools.util.logging.LoggerWidget;
import com.movielabs.mddf.tools.util.xml.EditorMgr;
import com.movielabs.mddf.tools.util.xml.SimpleXmlEditor;
import com.movielabs.mddflib.logging.LogEntryFolder;
import com.movielabs.mddflib.logging.LogEntryNode;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.xml.XmlIngester;
import com.movielabs.mddf.tools.util.FileChooserDialog;
import com.movielabs.mddf.tools.util.HeaderPanel;

import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public abstract class ValidatorTool extends GenericTool implements TreeSelectionListener {

	public static enum Context {
		MANIFEST, AVAILS
	}

	public boolean running = false;

	public class ValidationWorker extends SwingWorker<Void, StatusMsg> {

		private ValidationController controller;
		private String srcPath;
		private String uxProfile;
		private List<String> useCases;

		public ValidationWorker(ValidationController controller, String srcPath, String uxProfile,
				List<String> useCases) {
			this.controller = controller;
			this.srcPath = srcPath;
			this.uxProfile = uxProfile;
			this.useCases = useCases;
		}

		@Override
		protected Void doInBackground() throws Exception {
			setRunningState(true);
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			getTxtStatus().setText("Starting.....");
			consoleLogger.collapse(); 
			try {
				controller.validate(srcPath, uxProfile, useCases);
				refreshEditor(srcPath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				consoleLogger.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, e.getMessage(), null, "UI");
				getTxtStatus().setText( e.getMessage());
			}
			consoleLogger.expand();
			((Component) consoleLogger).invalidate();
			((Component) consoleLogger).repaint();
			frame.setCursor(null); // turn off the wait cursor
			setRunningState(false);
			return null;
		}

	}

	public static class StatusMsg {
		private final String msg;

		StatusMsg(String msg) {
			this.msg = msg;
		}
	}

	protected static final int MAX_RECENT = 8;
	private static final String PROP_KEY_includeInfo = "pref.log.includeInfo";
	private static final String PROP_KEY_minLogLevel = "pref.log.minLevel";
	protected static ValidatorTool tool;
	protected String htmlDocUrl;
	protected String appVersion = "t.b.d.";
	protected Context context = null;
	protected String contextId; // version suitable for constructing paths,
								// URLs, or property names
	protected String contextName; // version suitable for use in UI and log
									// messages
	protected JFrame frame;
	protected JTextField inputSrcTField;
	protected Properties userPreferences;
	protected File userPropFile;
	protected List<String> recentFileList = new ArrayList<String>();
	protected JComboBox<String> profileCB;
	protected File fileInputDir = null;
	protected File fileOutDir = new File("./temp"); // null;
	protected ValidationController controller;
	protected JMenuBar menuBar;
	protected HeaderPanel headerPanel;
	protected JPanel optionsPanel;
	protected JMenu processingMenu;
	protected JMenu loggingMenu;
	protected JCheckBoxMenuItem validateConstraintsCBox;
	protected JCheckBoxMenuItem validateBestPracCBox;
	protected JCheckBoxMenuItem compressCBoxMenuItem;
	protected JPanel mainPanel;
	protected JButton runValidatorBtn;
	protected JLabel useCaseLabel;
	protected UseCaseDialog useCaseDialog;
	protected List<String> useCases = new ArrayList<String>();
	protected JToolBar validatorToolBar;
	protected JButton editFileBtn;
	protected Map<String, File> selectedFiles = new HashMap<String, File>();
	private JMenu fileRecentMenu;
	protected boolean inputSrcTFieldLocked = false;
	protected FileFilter inputFileFilter = new FileNameExtensionFilter("XML file", "xml");
	private JPanel statusPanel;
	private JTextField txtStatus;
	private JMenuItem openFileMI;
	private JMenuItem runScriptMI;

	/**
	 * Returns the singleton instance of the currently executing tool.
	 * 
	 * @return
	 */
	public static ValidatorTool getTool() {
		return tool;
	}

	/**
	 * Create the application.
	 */
	public ValidatorTool(Context context) {
		this.context = context;
		switch (context) {
		case MANIFEST:
			contextId = context.toString();
			break;
		case AVAILS:
			contextId = context.toString();
			break;
		default:
			contextId = "";
		}
		contextName = contextId.substring(0, 1).toUpperCase() + contextId.substring(1).toLowerCase();
		String NAME_SETTINGSFILE = ".movielab.validate." + contextId.toLowerCase() + ".ini";
		String PATH_USERSETTINGS = null;
		try {
			PATH_USERSETTINGS = System.getProperty("user.home") + File.separator + NAME_SETTINGSFILE;
		} catch (SecurityException e) {
			// ignore
		}
		userPreferences = new Properties();
		userPropFile = new File(PATH_USERSETTINGS);
		if (userPropFile.canRead()) {
			try {
				userPreferences.load(new FileReader(userPropFile));
				FileChooserDialog.setDefaultDirMap(userPreferences);
				/* also re-load the list of recently accessed files */

				for (int i = 0; i < MAX_RECENT; i++) {
					String key = "recentFile." + i;
					String value = userPreferences.getProperty(key);
					if (value != null) {
						recentFileList.add(value);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		initialize();
	}

	/**
	 * Initialize the contents of the frame. This method is intended to be
	 * generic (i.e., independent of a specific validation context) but may be
	 * overridden by subclasses if necessary.
	 */
	protected void initialize() {
		frame = new JFrame();
		frame.setIconImage(Toolkit.getDefaultToolkit()
				.getImage(ValidatorTool.class.getResource(GenericTool.imageRsrcPath + "icon_movielabs.jpg")));
		frame.setTitle(contextName + " Validator");
		frame.setBounds(100, 100, 1150, 600);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				shutDown();
			}
		});
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		consoleLogger = getConsoleLogPanel();

		frame.getContentPane().add(getStatusPanel(), BorderLayout.SOUTH);

		mainPanel = new JPanel();
		frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
		mainPanel.setLayout(new BorderLayout(0, 0));

		mainPanel.add(getValidationTools(), BorderLayout.NORTH);

		mainPanel.add((Component) getConsoleLogPanel(), BorderLayout.CENTER);
		frame.getContentPane().add(getHeaderPanel(), BorderLayout.NORTH);

		setFileRecentMenuItems(recentFileList);
	}

	protected static boolean isMaximized(int state) {
		return (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
	}

	protected JToolBar getValidationTools() {
		if (validatorToolBar == null) {
			validatorToolBar = new JToolBar();

			validatorToolBar.add(getEditFileBtn());
			validatorToolBar.add(getRunValidationBtn());

			JLabel fileLabel = new JLabel("File");
			fileLabel.setMaximumSize(new Dimension(71, 15));
			fileLabel.setPreferredSize(new Dimension(71, 15));
			fileLabel.setMinimumSize(new Dimension(71, 15));
			fileLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			fileLabel.setForeground(Color.BLUE);
			fileLabel.setToolTipText("Select a single file or a directory");
			validatorToolBar.add(fileLabel);
			validatorToolBar.add(getInputSrcTextField());
		}
		return validatorToolBar;
	}

	void setRunningState(boolean state) {
		this.running = state;
		boolean enableUI = !running;
		openFileMI.setEnabled(enableUI);
		openFileMI.setEnabled(enableUI);
		runScriptMI.setEnabled(enableUI);
		editFileBtn.setEnabled(enableUI);
		inputSrcTField.setEnabled(enableUI);
	}

	/**
	 * @return
	 */
	protected JButton getEditFileBtn() {
		if (editFileBtn == null) {
			editFileBtn = new JButton("Edit");
			editFileBtn.setIcon(new ImageIcon(ValidatorTool.class.getResource(GenericTool.imageRsrcPath + "edit.png")));
			editFileBtn.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if(running){
						editFileBtn.setEnabled(false);
						return;
					}
					editFile();
				}
			});
			editFileBtn.setEnabled(false);
		}
		return editFileBtn;
	}

	/**
	 * 
	 */
	protected void editFile() {
		String manifestPath = fileInputDir.getAbsolutePath();
		/* Is it an XML file? */
		if (manifestPath.endsWith(".xml")) {
			SimpleXmlEditor editor = EditorMgr.getSingleton().getEditor(manifestPath, frame);

			if (editor != null) {
				editor.setVisible(true);
			}
		}

	}

	/**
	 * @return
	 */
	protected JButton getRunValidationBtn() {
		if (runValidatorBtn == null) {
			runValidatorBtn = new JButton("RUN");
			runValidatorBtn.setIcon(
					new ImageIcon(ValidatorTool.class.getResource(GenericTool.imageRsrcPath + "validate.png")));
			runValidatorBtn.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if(running){
						runValidatorBtn.setEnabled(false);
						return;
					}
					runTool();
				}
			});
			runValidatorBtn.setEnabled(false);
		}
		return runValidatorBtn;
	}

	/**
	 * @return
	 */
	protected Component getHeaderPanel() {
		if (headerPanel == null) {
			headerPanel = new HeaderPanel(getMenuBar(), context);
		}
		return headerPanel;
	}

	/**
	 * @return
	 */
	protected Component getStatusPanel() {
		if (statusPanel == null) {
			statusPanel = new JPanel();
			statusPanel.add(getTxtStatus());
		}
		return statusPanel;
	}

	protected JMenuBar getMenuBar() {
		if (menuBar == null) {
			menuBar = new JMenuBar();

			JMenu fileMenu = new JMenu("File");
			menuBar.add(fileMenu);

			openFileMI = new JMenuItem("Open File...");
			openFileMI.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					openFile();
				}

			});
			fileMenu.add(openFileMI);

			fileMenu.add(getFileRecentMenu());

			runScriptMI = new JMenuItem("Run Script..");
			runScriptMI.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					runScript();
				}

			});
			fileMenu.add(runScriptMI);

			JMenuItem menuItem2 = new JMenuItem("Exit");
			menuItem2.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					shutDown();
				}

			});
			fileMenu.add(menuItem2);

			getProcessingMenu();
			processingMenu.add(getLoggingMenu());
			menuBar.add(processingMenu);

			JMenu helpMenu = new JMenu("Help");
			menuBar.add(helpMenu);

			JMenuItem welcomeMenuItem = new JMenuItem("Welcome");
			helpMenu.add(welcomeMenuItem);
			welcomeMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					String location = htmlDocUrl + "../";
					URI uri = null;
					try {
						uri = new URI(location);
						Desktop.getDesktop().browse(uri);
					} catch (Exception e1) {
						showDocAlert(location);
					}
				}

			});

			JMenuItem helpContentsMenuItem = new JMenuItem("Help contents");
			helpMenu.add(helpContentsMenuItem);
			helpContentsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					String location = htmlDocUrl + "UsersGuide.html";
					URI uri;
					try {
						uri = new URI(location);
						Desktop.getDesktop().browse(uri);
					} catch (Exception e1) {
						showDocAlert(location);
					}
				}

			});

			JMenuItem relNotesMenuItem = new JMenuItem("Release Notes");
			helpMenu.add(relNotesMenuItem);
			relNotesMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					String location = htmlDocUrl;
					URI uri;
					try {
						uri = new URI(location);
						Desktop.getDesktop().browse(uri);
					} catch (Exception e1) {
						showDocAlert(location);
					}
				}

			});
			JMenuItem aboutMenuItem = new JMenuItem("About");
			helpMenu.add(aboutMenuItem);
			aboutMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					/*
					 * get build.properties if possible. These may not be
					 * present when running from an IDE as opposed to the jar
					 * files.
					 * 
					 */
					Properties mddfLibProps = loadProperties("/com/movielabs/mddflib/build.properties");
					String libVersion;
					String libBuildDate;
					if (mddfLibProps == null) {
						libVersion = "n.a.";
						libBuildDate = "n.a.";
					} else {
						libVersion = mddfLibProps.getProperty("version");
						libBuildDate = mddfLibProps.getProperty("buildDate") + "; "
								+ mddfLibProps.getProperty("buildTime");
					}
					Properties mddfToolProps = loadProperties("/com/movielabs/mddf/tools/build.properties");
					String toolVersion;
					String toolBuildDate;
					if (mddfToolProps == null) {
						toolVersion = "n.a.";
						toolBuildDate = "n.a.";
					} else {
						switch (context) {
						case MANIFEST:
							toolVersion = mddfToolProps.getProperty("versionCMM", "Not Specified");
							break;
						case AVAILS:
							toolVersion = mddfToolProps.getProperty("versionAvail", "Not Specified");
							break;
						default:
							toolVersion = mddfToolProps.getProperty("version", "Not Specified");
						}
						toolBuildDate = mddfToolProps.getProperty("buildDate") + "; "
								+ mddfToolProps.getProperty("buildTime");
					}

					AboutDialog dialog = new AboutDialog("CMM Preprocessing:<br/><b>" + contextName + " Validator</b>",
							"");
					dialog.addTab("Build");
					dialog.addEntry("Build", "<b>App Version:</b> <tt>" + toolVersion + "</tt>");
					dialog.addEntry("Build", "<b>App Build Date:</b> <tt>" + toolBuildDate + "</tt><hr/>");
					dialog.addEntry("Build", "<b>mddflib S/W Version:</b> <tt>" + libVersion + "</tt>");
					dialog.addEntry("Build", "<b>mddflib Build Date:</b> <tt>" + libBuildDate + "</tt><hr/>");
					dialog.addTab("License");
					dialog.addEntry("License", "Copyright Motion Picture Laboratories, Inc. 2016<br/>");
					dialog.addEntry("License", license);
					dialog.setLocationRelativeTo(frame);
					dialog.setVisible(true);
				}

			});

		}
		return menuBar;
	}

	/**
	 * @return
	 */
	public JMenu getFileRecentMenu() {
		if (fileRecentMenu == null) {
			fileRecentMenu = new JMenu("Recent..");
		}
		return fileRecentMenu;
	}

	/**
	 * Resets the Menu Items specifying recently accessed files.
	 * 
	 * @param recentFileList
	 */
	public void setFileRecentMenuItems(List<String> recentFileList) {
		// make sure the Menu has been created
		getFileRecentMenu();
		fileRecentMenu.removeAll();
		for (int i = 0; i < recentFileList.size(); i++) {
			/* remember the list contains the full path */
			final String path2file = recentFileList.get(i);
			File temp = new File(path2file);
			String name = temp.getName();
			JMenuItem nextMI = new JMenuItem(name);
			nextMI.setToolTipText(recentFileList.get(i));
			fileRecentMenu.add(nextMI);
			nextMI.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					setFileInputDir(new File(path2file));
				}
			});
		}
	}

	protected static Properties loadProperties(String rsrcPath) {
		Properties props = new Properties();
		InputStream inStream = XmlIngester.class.getResourceAsStream(rsrcPath);
		if (inStream == null) {
			return null;
		}
		try {
			props.load(inStream);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return props;
	}

	/**
	 * Override in subclasses to add support for context-specific processing.
	 * 
	 * @return
	 */
	protected JMenu getProcessingMenu() {
		if (processingMenu == null) {
			processingMenu = new JMenu("Processing");
		}
		return processingMenu;
	}

	protected void showDocAlert(String location) {
		String errMsg = "Unable to directly access the browser from this platform.";
		errMsg = errMsg + "<p>The requested documentation is available at:<br/> " + location + "</p>";
		JOptionPane.showMessageDialog(frame, "<html><div style='text-align: center;'>" + errMsg + "</style></html>",
				"Unsupported Feature", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * @return
	 */
	protected JMenuItem getLoggingMenu() {
		if (loggingMenu == null) {
			loggingMenu = new JMenu("Logging");
			JMenu minLevelMenu = new JMenu("Set filter level..");
			minLevelMenu.setToolTipText("Set minimum severity level for filtering of messages to be displayed");
			loggingMenu.add(minLevelMenu);
			ButtonGroup group = new ButtonGroup();
			int minLevel = getConsoleLogPanel().getMinLevel();
			for (int i = 0; i < LogMgmt.logLevels.length; i++) {
				int logLevel = i;
				if (logLevel != LogMgmt.LEV_INFO) {
					String label = LogMgmt.logLevels[i];
					final JRadioButtonMenuItem logMenuItem = new JRadioButtonMenuItem(label);
					if (minLevel == logLevel) {
						logMenuItem.setSelected(true);
					}
					group.add(logMenuItem);
					minLevelMenu.add(logMenuItem);
					logMenuItem.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							if (logMenuItem.isSelected()) {
								consoleLogger.setMinLevel(logLevel);
							}
						}
					});
				} else {
					String label = "Info Messages ON/OFF";
					final JCheckBoxMenuItem logMenuItem = new JCheckBoxMenuItem(label);
					logMenuItem.setToolTipText("Include/exclude status and summary messages in log file");
					logMenuItem.setSelected(consoleLogger.isInfoIncluded());
					logMenuItem.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							consoleLogger.setInfoIncluded(logMenuItem.isSelected());
						}
					});
					// user preference??
					userPreferences.getProperty(PROP_KEY_includeInfo, "true");
					loggingMenu.add(logMenuItem);
				}
			}
			loggingMenu.add(new JSeparator());

			JMenuItem clearLogMI = new JMenuItem("Clear Log");
			clearLogMI.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					consoleLogger.clearLog();
				}

			});
			loggingMenu.add(clearLogMI);

			loggingMenu.add(((AdvLogPanel) consoleLogger).createSaveLogMenu());
		}
		return loggingMenu;
	}

	protected LoggerWidget getConsoleLogPanel() {
		if (consoleLogger == null) {
			consoleLogger = new AdvLogPanel();
			((AdvLogPanel) consoleLogger).setStatusDisplay(getTxtStatus());
			LogNavPanel logNav = ((AdvLogPanel) consoleLogger).getLogNavPanel();
			// listen for user selections..
			logNav.addListener(this);
			/*
			 * configure based on last saved user-specific settings..
			 */
			boolean includeInfo = Boolean.valueOf(userPreferences.getProperty(PROP_KEY_includeInfo, "true"));
			consoleLogger.setInfoIncluded(includeInfo);
			String minLevelProp = userPreferences.getProperty(PROP_KEY_minLogLevel);
			if (minLevelProp != null) {
				int minLevel = Integer.parseInt(minLevelProp);
				consoleLogger.setMinLevel(minLevel);
			}
		}
		return consoleLogger;
	}

	/**
	 * @return
	 */
	protected JTextField getInputSrcTextField() {
		if (inputSrcTField == null) {
			inputSrcTField = new JTextField();
			inputSrcTField.setText(" ");
			inputSrcTField.setToolTipText("Select single file or a directory");
			inputSrcTField.setEditable(false);
			inputSrcTField.setColumns(20);
			inputSrcTField.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					if(running){
						inputSrcTField.setEnabled(false);
						return;
					}
					openFile();
				}
			});
		}
		return inputSrcTField;
	}

	protected void openFile() {
		if(running){
			return;
		}
		// trigger the FileChooser dialog
		File targetFile = FileChooserDialog.getPath("Source Folder", null, inputFileFilter, "srcManifest", tool.frame,
				JFileChooser.FILES_AND_DIRECTORIES);
		if (targetFile != null) {
			fileInputDir = targetFile;
			setFileInputDir(fileInputDir);
		}
		runValidatorBtn.setEnabled(fileInputDir != null);
	}

	/**
	 * Set the file or directory that will be the focus of any actions and
	 * synchronize all other UI widgets accordingly.
	 * 
	 * @param target
	 *            the fileInputDir to set
	 */
	protected void setFileInputDir(File target) {
		if (inputSrcTFieldLocked || running) {
			/*
			 * runTool() locks and unlocks this field to deal with a minor but
			 * annoying bug resulting from it's collapsing the log tree.
			 */
			return;
		}
		String name = target.getName();
		this.fileInputDir = target;
		inputSrcTField.setText(name);
		inputSrcTField.setToolTipText(target.getAbsolutePath());
		getRunValidationBtn().setEnabled(true);
		getEditFileBtn().setEnabled(target.isFile());
		/*
		 * Store for possible recall be user via menu selection
		 */
		selectedFiles.put(name, target);
		String path;
		if (fileInputDir.isDirectory()) {
			path = fileInputDir.getAbsolutePath();
		} else {
			path = fileInputDir.getParent();
		}
		FileChooserDialog.setDirMapping("srcManifest", path);
	}

	/**
	 * Respond to user selection in the Log Panel's navigation (i.e., tree)
	 * component.
	 * 
	 * @param e
	 */
	public void valueChanged(TreeSelectionEvent e) {
		TreePath tp = e.getPath();
		if (tp == null || (tp.getPathCount() != 2)) {
			return;
		}
		LogEntryFolder fileFolder = (LogEntryFolder) tp.getPathComponent(1);
		File selectedFile = fileFolder.getFile();
		if (selectedFile == null) {
			return;
		}
		setFileInputDir(selectedFile);
	}

	/**
	 * @return
	 */
	protected JPanel getProfilingPanel() {
		JPanel profilingChoicePanel = new JPanel();
		return profilingChoicePanel;
	}

	protected JComboBox<String> getProfileComboBox() {
		if (profileCB == null) {
			String[] profiles = ValidationController.getSupportedProfiles();
			DefaultComboBoxModel<String> cbModel = new DefaultComboBoxModel<String>(profiles);
			profileCB = new JComboBox<String>(cbModel);
			profileCB.setBorder(null);
			profileCB.setToolTipText("Set to 'none' to limit validation to schema-only requirements");

			profileCB.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent event) {
					if (event.getStateChange() == ItemEvent.SELECTED) {
						String selectedProfile = (String) event.getItem();
						setAvailableUseCase(selectedProfile);
					}
				}
			});
		}
		return profileCB;
	}

	/**
	 * @param selectedProfile
	 */
	protected void setAvailableUseCase(String selectedProfile) {
		getUseCaseDialog();
		controller = getController();
		String[] useCases = controller.getSupportedUseCases(selectedProfile);
		useCaseDialog.setAvailableUseCases(useCases);
	}

	protected UseCaseDialog getUseCaseDialog() {
		if (useCaseDialog == null) {
			useCaseDialog = new UseCaseDialog();
			useCaseDialog.addComponentListener(new ComponentAdapter() {
				public void componentShown(ComponentEvent e) {
					// Not really interested in this
				}

				public void componentHidden(ComponentEvent e) {
					// need to get current selections
					useCases = useCaseDialog.getSelectedUseCases();
				}
			});
		}
		return useCaseDialog;
	}

	/**
	 * @return
	 */
	protected Component getUseCasePanel() {
		if (useCaseLabel == null) {
			useCaseLabel = new JLabel("--------");
			useCaseLabel.setPreferredSize(new Dimension(150, 35));
			useCaseLabel.setBorder(
					new TitledBorder(null, "Use Cases", TitledBorder.LEADING, TitledBorder.TOP, null, Color.BLUE));
			useCaseLabel.setToolTipText("Click here to set or view Use-Cases for the selected Profile");
			useCaseLabel.addMouseListener(new MouseListener() {

				@Override
				public void mouseClicked(MouseEvent e) {
					getUseCaseDialog();
					useCaseDialog.setVisible(true);
					useCaseDialog.setLocationRelativeTo(useCaseLabel);

				}

				@Override
				public void mousePressed(MouseEvent e) {
					// TODO Auto-generated method stub

				}

				@Override
				public void mouseReleased(MouseEvent e) {
					// TODO Auto-generated method stub

				}

				@Override
				public void mouseEntered(MouseEvent e) {
					// TODO Auto-generated method stub

				}

				@Override
				public void mouseExited(MouseEvent e) {
					// TODO Auto-generated method stub

				}

			});
		}
		return useCaseLabel;
	}

	protected void runScript() {
		// trigger the FileChooser dialog
		String format = "json";
		FileFilter filter = new FileNameExtensionFilter(format.toUpperCase() + " File", format);
		File scriptPath = FileChooserDialog.getPath("Script File", null, filter, "scriptIn", tool.frame,
				JFileChooser.FILES_ONLY);

		frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		// Converter.outputJson = false;
		controller = getController();
		try {
			controller.runScript(scriptPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		frame.setCursor(null); // turn off the wait cursor
		consoleLogger.expand();
	}

	/**
	 * 
	 */
	public void runTool() {
		inputSrcTFieldLocked = true;
		frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		// Converter.compress = compressCBoxMenuItem.isSelected();
		// Converter.outputJson = false;
		consoleLogger.collapse();
		String uxProfile = (String) getProfileComboBox().getSelectedItem();
		String srcPath = fileInputDir.getAbsolutePath();
		updateUsageHistory(srcPath);
		fileOutDir.getAbsolutePath();
		controller = getController();
		controller.setValidation(true, validateConstraintsCBox.isSelected(), validateBestPracCBox.isSelected());
		// ....................................................
		runInBackground(srcPath,uxProfile, useCases);
		// .................................................
		frame.setCursor(null); // turn off the wait cursor
		consoleLogger.expand();
		inputSrcTFieldLocked = false;
	}

	protected void runInBackground(String srcPath, String uxProfile, List<String> useCases) {
		SwingWorker<Void, StatusMsg> worker = new ValidationWorker(controller, srcPath, uxProfile, useCases);
		worker.execute();
	}

	/**
	 * If there is already an Editor for the specified file, update the
	 * log-entries markers
	 * 
	 * @param srcPath
	 */
	protected void refreshEditor(String srcPath) {
		SimpleXmlEditor xmlEditor = EditorMgr.getSingleton().getEditorFor(srcPath);
		if (xmlEditor != null) {
			LogEntryFolder logFolder = consoleLogger.getFileFolder(xmlEditor.getCurFile());
			List<LogEntryNode> msgList = logFolder.getMsgList();
			xmlEditor.showLogMarkers(msgList);
		}
	}

	/**
	 * keep track of recently accessed files
	 * 
	 * @param filePath
	 */
	protected void updateUsageHistory(String filePath) {
		if (recentFileList.isEmpty()) {
			recentFileList.add(filePath);
		} else {
			// if already there, remove it
			recentFileList.remove(filePath);
			// set as most recent
			recentFileList.add(0, filePath);
			// trim to max# remembered
			int last = recentFileList.size();
			if (last > MAX_RECENT) {
				for (int j = last - 1; j >= MAX_RECENT; j--) {
					recentFileList.remove(last - 1);
				}
			}
		}
		setFileRecentMenuItems(recentFileList);
	}

	/**
	 * @return
	 */
	public ValidationController getController() {
		if (controller == null) {
			controller = new ValidationController(context, consoleLogger);
		}
		return controller;
	}

	/**
	 * Save properties and open files, then shut down and exit.
	 */
	public void shutDown() {
		// save user preferences
		String flag = Boolean.toString(consoleLogger.isInfoIncluded());
		userPreferences.setProperty(PROP_KEY_includeInfo, flag);
		String minLev = Integer.toString(consoleLogger.getMinLevel());
		userPreferences.setProperty(PROP_KEY_minLogLevel, minLev);
		// save what user has been accessing
		for (int i = 0; i < recentFileList.size(); i++) {
			/* remember the list contains the full path */
			String path2xml = recentFileList.get(i);
			userPreferences.setProperty("recentFile." + i, path2xml);
		}
		try {
			userPreferences.store(new FileWriter(userPropFile), "MovieLabs CPE Pre-processing");
		} catch (Exception e) {
			e.printStackTrace();
			consoleLogger.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, e.getMessage(), null, "UI");
		}
		System.exit(0);
	}

	public JTextField getTxtStatus() {
		if (txtStatus == null) {
			txtStatus = new JTextField();
			txtStatus.setText("Status:");
			txtStatus.setBackground(UIManager.getColor("OptionPane.questionDialog.titlePane.background"));
			txtStatus.setEditable(false);
			txtStatus.setColumns(90);
		}
		return txtStatus;
	}
}
