/** 
 * Copyright Motion Picture Laboratories, Inc. 2017
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
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JLabel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddf.tools.util.FileChooserDialog;
import com.movielabs.mddf.tools.util.UpdateMgr;
import com.movielabs.mddflib.logging.DefaultLogging;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.util.Translator;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * Implements a framework for accessing the various MDDF <i>tools</i>. Depending
 * on the <tt>args</tt> passed to either <tt>ToolLauncher.main()</tt> or
 * <tt>ToolLauncher.execute()</tt>, usage may be either via a GUI or via the
 * command line arguments.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class ToolLauncher {

	private JFrame frame;
	private Properties mddfPreferences;
	private File mddfPrefFile;
	private static Options options = null;
	public static final String TOOL_RSRC_PATH = "/com/movielabs/mddf/tools/resources/";
	private static String prefPath;
	private static HelpFormatter formatter = new HelpFormatter();
	private static ToolLauncher singleton;

	/**
	 * Main entry point for executable jar.
	 */
	public static void main(String[] args) {
		DefaultLogging logger = new DefaultLogging();
		execute(args, logger);

	}

	private static void loadOptions() {
		/*
		 * create the Options. Options represents a collection of Option instances,
		 * which describe the **POSSIBLE** options for a command-line
		 */
		options = new Options();
		options.addOption("h", "help", false, "Display this HELP file, then exit");
		options.addOption("i", "interactive", false,
				"Launch in interactive mode with full UI. When used, this argument will result in all other arguments being ignored.");
		options.addOption("f", "file", true, "Process a single MDDF file.");
		options.addOption("d", "dir", true, "Process all MDDF files in a directory.");
		options.addOption("s", "script", true, "Run a script file.");
		options.addOption("l", "logFile", true, "Output file for logging.");
		options.addOption("logLevel", true,
				"Filter for logging; valid values are: " + "\n'verbose'\n 'warn' (DEFAULT)\n 'error'\n 'info'");
		options.addOption("r", "recursive", true,
				"[T/F] processing of a directory will be recursive (Default is 'T').");
		options.addOption("v", "verbose", false, "Display log-file entries in terminal window during execution.");
		options.addOption("V", "version", false, "Display software version and build date.");

		/* Translation-related arguments: */
		options.addOption("X", "exportAll", false, "export valid files in all applicable formats.");
		Option xOption = new Option("x",
				"export valid files in specified format(s). Multiple formats may be specified separated by commas"
						+ " and requests to export in a format matching the source format are ignored. "
						+ Translator.getHelp());
		// Set option 'x' to take 1 to n arguments
		xOption.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(xOption);
		options.addOption("xDir", "exportDir", true, "Directory for exported files (Default is '.')");
	}

	public static void execute(String[] args, LogMgmt logger) {
		loadOptions();
		CommandLine cmdLine = null;
		if (args.length == 0) {
			printUsage("");
			System.exit(0);
		}
		try {
			cmdLine = parseOptions(args);
		} catch (ParseException e) {
			String hdrMsg = e.getLocalizedMessage();
			printUsage(hdrMsg);
			System.exit(0);
		}
		boolean showVersion = cmdLine.hasOption("V");
		if (showVersion) {
			printVersion();
		}
		if ((cmdLine.hasOption("h"))) {
			printHelp();
			System.exit(0);
		}
		if (cmdLine.hasOption("i")) {
			// Launch in interactive mode
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						ToolLauncher window = new ToolLauncher();
						window.frame.setVisible(true);
						/*
						 * Immediately after making the Launcher visible, invoke UpdateMgr. If an update
						 * is available, it will display a pop-up notifying the user.
						 */
						UpdateMgr.check(singleton);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} else {
			runNonInteractive(cmdLine, logger);
		}
	}

	private static void runNonInteractive(CommandLine cmdLine, LogMgmt logger) {
		configureLogOptions(cmdLine, logger);
		if (cmdLine.hasOption("s")) {
			throw new UnsupportedOperationException();
		} else {
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			/*
			 * Pre-Validation set-up and prep.........
			 */
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			ValidationController vCtrl = new ValidationController(logger);
			EnumSet<FILE_FMT> selections = EnumSet.noneOf(FILE_FMT.class);
			String[] xlatFmts = cmdLine.getOptionValues("x");
			if (xlatFmts == null || (xlatFmts.length == 0)) {
				vCtrl.setTranslations(null, null);
			} else {
				Set<String> allowed = new HashSet<String>();
				allowed.addAll(Arrays.asList(MddfContext.getSupportedVersions("AVAIL")));
				allowed.addAll(Arrays.asList(MddfContext.getSupportedVersions("AVAIL-E")));
				for (String fmt : xlatFmts) {
					fmt = fmt.replaceAll(",", "");
					if (allowed.contains(fmt)) {
						selections.add(MddfContext.identifyMddfFormat("avails", fmt));
					} else {
						String hdrMsg = "Unrecognized Translation format '" + fmt;
						printUsage(hdrMsg);
						System.exit(0);
					}
				}
				String exportDir = cmdLine.getOptionValue("xDir", ".");
				vCtrl.setTranslations(selections, new File(exportDir));
			}
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// ~~~~~~~~~~~~~~~~~~ Validation ~~~~~~~~~~~~~~~~~~~
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			String dir = cmdLine.getOptionValue("d");
			if (dir != null) {
				try {
					String recursive = cmdLine.getOptionValue("r", "T");
					if (recursive.equalsIgnoreCase("F")) {
						vCtrl.setRecursive(false);
					}
					vCtrl.validate(dir, null, null);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			String filePath = cmdLine.getOptionValue("f");
			if (filePath != null) {
				File srcFile = new File(filePath);
				if (srcFile.isDirectory()) {
					String hdrMsg = "Use the '-d' argument to specify directories";
					printUsage(hdrMsg);
					System.exit(0);
				}
				try {
					vCtrl.validate(filePath, null, null);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			/*
			 * POST-Validation actions.........
			 */
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			String logFile = cmdLine.getOptionValue("l");
			if (logFile != null) {
				File logOutput = new File(logFile);
				try {
					logger.saveAs(logOutput, "csv");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Parse and return command-line arguments.
	 * 
	 * @param args
	 * @return
	 * @throws ParseException
	 */
	private static CommandLine parseOptions(String[] args) throws ParseException {
		// create the command line parser
		CommandLineParser parser = new DefaultParser();
		// parse the command line arguments
		CommandLine line = parser.parse(options, args);
		return line;
	}

	private static void printUsage(String headerMsg) {
		System.out.println("\n" + headerMsg + "\n");
		formatter.printHelp("ToolLauncher", null, options, null, true);
	}

	/**
	 * 
	 */
	private static void printHelp() {
		String header = "\nLaunch one of the MDDF tools used to validate and/or translate MovieLabs Digital Distribution Framework (MDDF) files.\n\n\n";
		String footer = getHelpBody();
		formatter.printHelp("ToolLauncher", header, options, footer, true);

	}

	private static String getHelpBody() {
		String header = "";
		String rsrcPath = TOOL_RSRC_PATH + "LauncherHelp.txt";
		InputStream in = ToolLauncher.class.getResourceAsStream(rsrcPath);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = null;
		String ls = System.getProperty("line.separator");
		try {
			while ((line = reader.readLine()) != null) {
				header = header + ls + line;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return header;
	}

	private static void configureLogOptions(CommandLine cmdLine, LogMgmt logger) {
		if (cmdLine.hasOption("logLevel")) {
			String llValue = cmdLine.getOptionValue("logLevel", "warn");
			switch (llValue) {
			case "debug":
				logger.setMinLevel(LogMgmt.LEV_DEBUG);
				break;
			case "verbose":
				logger.setMinLevel(LogMgmt.LEV_NOTICE);
				break;
			case "warn":
				logger.setMinLevel(LogMgmt.LEV_WARN);
				break;
			case "error":
				logger.setMinLevel(LogMgmt.LEV_ERR);
				break;
			case "info":
				logger.setMinLevel(LogMgmt.LEV_INFO);
				break;
			}
		}
		((DefaultLogging) logger).setPrintToConsole(cmdLine.hasOption("v"));
	}

	private static void printVersion() {
		Properties mddfToolProps = ValidatorTool.loadProperties("/com/movielabs/mddflib/build.properties");
		String libVersion = mddfToolProps.getProperty("version", "Not Specified");
		String buildDate = mddfToolProps.getProperty("buildDate") + "; " + mddfToolProps.getProperty("buildTime");
		System.out.println("mddf-lib version " + libVersion);
		System.out.println("mddf-lib build date " + buildDate);
	}

	public static ToolLauncher getSingleton() {
		return singleton;
	}

	/**
	 * Create the application.
	 */
	public ToolLauncher() {
		ToolLauncher.singleton = this;
		loadPreferences();
		initialize();
	}

	/**
	 * Load user preferences for activities relating to all MDDF activities and
	 * tools. Additional preferences that are applicable only to a given tool may be
	 * loaded later by specific tools.
	 */
	private void loadPreferences() {
		String NAME_SETTINGSFILE = ".movielab.mddf.ini";
		String PATH_USERSETTINGS = null;
		try {
			PATH_USERSETTINGS = System.getProperty("user.home") + File.separator + NAME_SETTINGSFILE;
		} catch (SecurityException e) {
			// ignore
		}
		mddfPreferences = new Properties();
		mddfPrefFile = new File(PATH_USERSETTINGS);
		if (mddfPrefFile.canRead()) {
			try {
				mddfPreferences.load(new FileReader(mddfPrefFile));
				FileChooserDialog.setDefaultDirMap(mddfPreferences);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		getUUID();
	}

	public String getUUID() {
		String uuidString = mddfPreferences.getProperty("mddf.tool.uuid");
		if (uuidString == null || uuidString.isEmpty()) {
			// create a UUID
			UUID uuid = UUID.randomUUID();
			uuidString = uuid.toString();
			mddfPreferences.setProperty("mddf.tool.uuid", uuidString);
		}
		return uuidString;
	}

	public void saveState() {
		try {
			mddfPreferences.store(new FileWriter(mddfPrefFile), "MovieLabs MDDF Tool-Kit");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String getProperty(String key) {
		return mddfPreferences.getProperty(key);
	}

	public void setProperty(String key, String value) {
		mddfPreferences.setProperty(key, value);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 162);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 46, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 34, 34, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		frame.getContentPane().setLayout(gridBagLayout);

		JLabel lblNewLabel = new JLabel("MDDF tool launcher");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.gridwidth = 4;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 1;
		gbc_lblNewLabel.gridy = 1;
		frame.getContentPane().add(lblNewLabel, gbc_lblNewLabel);

		JButton btnAvails = new JButton("Avails");
		btnAvails.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				frame.setVisible(false);
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							ValidatorTool.tool = new AvailsTool();
							ValidatorTool.tool.frame.setVisible(true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});

			}
		});

		btnAvails.setToolTipText("launch Avail Validator");
		GridBagConstraints gbc_btnAvails = new GridBagConstraints();
		gbc_btnAvails.fill = GridBagConstraints.BOTH;
		gbc_btnAvails.insets = new Insets(0, 0, 0, 5);
		gbc_btnAvails.gridx = 1;
		gbc_btnAvails.gridy = 3;
		frame.getContentPane().add(btnAvails, gbc_btnAvails);

		JButton btnManifest = new JButton("Manifest");
		btnManifest.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				frame.setVisible(false);
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							ValidatorTool.tool = new ManifestTool();
							ValidatorTool.tool.frame.setVisible(true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		});
		GridBagConstraints gbc_btnManifest = new GridBagConstraints();
		gbc_btnManifest.insets = new Insets(0, 0, 0, 5);
		gbc_btnManifest.fill = GridBagConstraints.BOTH;
		gbc_btnManifest.gridx = 4;
		gbc_btnManifest.gridy = 3;
		frame.getContentPane().add(btnManifest, gbc_btnManifest);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.exit(0);
			}
		});
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.gridx = 7;
		gbc_btnCancel.gridy = 3;
		frame.getContentPane().add(btnCancel, gbc_btnCancel);
	}

	public Component getFrame() { 
		return frame;
	}

}
