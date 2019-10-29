/**
 * Copyright (c) 2019 MovieLabs

 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.movielabs.mddflib.tests.deployed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jdom2.Document;

import com.movielabs.mddflib.avails.validation.AvailValidator;
import com.movielabs.mddflib.avails.xml.AvailsWrkBook;
import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.avails.xml.AvailsSheet;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * Run diagnostic stress tests. Statistics on execution time and heap usage are
 * collected.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class StressTest {

	/**
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	class StopWatch {
		private long timer_start;
		private long elapsed = 0;
		private boolean paused = false;

		/**
		 * Resets elapsed time. If timer is paused, it is re-started.
		 */
		void reset() {
			timer_start = System.nanoTime();
			elapsed = 0;
			paused = false;
		}

		void pause() {
			if (!paused) {
				paused = true;
				long difference = System.nanoTime() - timer_start;
				elapsed = elapsed + difference;
			}
		}

		void restart() {
			if (paused) {
				timer_start = System.nanoTime();
				paused = false;
			}
		}

		String elapsedTime() {
			long timer_cur = System.nanoTime();
			long difference = System.nanoTime() - timer_start;
			elapsed = elapsed + difference;
			/* since 'elapsed' as been incremented, we need to also bump the start time */
			timer_start = timer_cur;
			long e_min = TimeUnit.NANOSECONDS.toMinutes(elapsed);
			long min2nano = TimeUnit.MINUTES.toNanos(e_min);
			long e_sec = TimeUnit.NANOSECONDS.toSeconds(elapsed - min2nano);
			String et = String.format("%d min: %d sec", e_min, e_sec);
			return et;
		}
	}

	private static HelpFormatter formatter = new HelpFormatter();
	private InstrumentedLogger iLog;
	private Options options;

	private static final String DEFAULT_DIR = ".";
	public static final String STAT_COL_NAMES[] = { "mode", "context", "Run", "Heap (MB)", "Used Mem (MB)", "Time",
			"size", "forced GC" };
	private static final String stats_colSep = ",";

	private static String MODULE_ID = "Tester";
	private static File tempDir;
	private static final int DEFAULT_CNT = 5;
	private static String filePrefix = "TestCase_";

	private String testMode;
	private String rsrcPath = DEFAULT_DIR;
	private File logDir;
	private boolean interactive;
	private StopWatch timer = new StopWatch();
	private PrintWriter statsWriter;
	private boolean gcOn;
	private String fileSize;
	private String fileFmt = "xlsx"; // default value
	private int count;
	private File workDir;
	private String startTime;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			StressTest tester = new StressTest(args);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public StressTest(String[] args) throws IOException {
		initialize(args);
		// ..................................
		Version version = Version.V1_7_2;
		long maxMem = Runtime.getRuntime().maxMemory();
		float maxGB = maxMem / (1024 * 1014 * 1024);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Max Memory = " + maxGB + "GBytes", null, "JUnit");
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Mode=" + testMode, null, "JUnit");
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Format=" + fileFmt, null, "JUnit");
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Force GC=" + gcOn, null, "JUnit");
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Interactive=" + interactive, null, "JUnit");
		try {
			runTest(version, count);
		} catch (Exception e) {
			e.printStackTrace();
			iLog.log(iLog.LEV_FATAL, iLog.TAG_N_A, e.getMessage(), null, "JUnit");
		}
		pauseForInput("Hit <Enter> to exit");
		statsWriter.flush();
		statsWriter.close();
		iLog.closeLog();
		System.out.println("Good-bye");

	}

	private void pauseForInput(String msg) {
		if (interactive) {
			System.out.println(msg);
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			// Reading data using readLine
			try {
				String name = reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param testFile
	 * @param version
	 * @param count
	 * @param force    if <tt>true</tt> request garbage collection between major
	 *                 stages
	 */
	private void runTest(Version version, int count) {
		String testFileName = filePrefix + fileSize + "." + fileFmt;
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with rsrc file " + srcFile.getAbsolutePath(), null, "JUnit");

		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Repeat count=" + count, null, "JUnit");
		for (int i = 1; i < (count + 1); i++) {
			if (gcOn) {
				/* do garbage collection btwn runs */
				System.gc();
				// sleep for 2 seconds before next attempt (allows GC (I hope))
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			}
			pauseForInput("Hit <Enter> to continue");
			String msg = "\nRun " + i + " of " + count;
			System.out.println(msg);
			iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Run " + i + " of " + count + ":: " + tStamp() + " start time", null,
					"JUnit");
			// reset the stopwatch
			timer.reset();
			reportStats(" initial", i);
			switch (fileFmt) {
			case "xlsx":
				runXlsxTest(srcFile, version, i);
				break;
			case "xml":
				runXmlTest(srcFile, version, i);
				break;
			}
			reportStats("Processing completed", i);
			iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Run " + i + " of " + count + ":: " + tStamp() + " end time", null,
					"JUnit");
		}
	}

	/**
	 * @param srcFile
	 * @param version
	 * @param i
	 */
	private void runXmlTest(File srcFile, Version version, int i) {

		if (!srcFile.exists()) {
			throw new MissingResourceException("Missing test artifact " + srcFile.getAbsolutePath(), "File",
					srcFile.getName());
		}
		MddfTarget target = new MddfTarget(srcFile, iLog);
		Document xmlDoc = target.getXmlDoc();
		if (testMode.equals("load")) {
			return;
		}
		boolean isValid = true;
		if (testMode.equals("cvrt")) {
			iLog.log(LogMgmt.LEV_INFO, iLog.TAG_N_A, "Conversion of XML is a NO-OP.", target, MODULE_ID);
			return;
		}
		AvailValidator tool1 = new AvailValidator(true, iLog);
		isValid = tool1.process(target, null);
		if (testMode.equals("val")) {
			return;
		}
	}

	private void runXlsxTest(File srcFile, Version version, int runNum) {

		AvailsSheet sheet = AvailsWrkBook.loadWorkBook(srcFile, iLog, null, 0);
		reportStats("LOAD completed", runNum);
		if (testMode.equals("load")) {
			return;
		}

		Map<String, Object> results = AvailsWrkBook.mapToXml(sheet, srcFile, version, null, iLog);
		reportStats("XLSX CONVERSION completed", runNum);
		Document xmlDoc = (Document) results.get("xml");
		File outputLoc = new File(tempDir, "TRACE_" + srcFile.getName().replace("xlsx", "xml"));
		XmlIngester.writeXml(outputLoc, xmlDoc);
		if (testMode.equals("cvrt")) {
			return;
		}

		pauseForInput("Ready to validate. Hit <Enter> when ready to continue");
		Map<Object, Pedigree> pedigreeMap = (Map<Object, Pedigree>) results.get("pedigree");
		MddfTarget target = new MddfTarget(xmlDoc, srcFile, iLog);
		String msg = "Validating file as a " + target.getMddfType().toString();
		iLog.log(LogMgmt.LEV_INFO, iLog.TAG_N_A, msg, target, MODULE_ID);
		boolean isValid = true;
		AvailValidator tool1 = new AvailValidator(true, iLog);
		isValid = tool1.process(target, pedigreeMap);
		reportStats("XLSX VALIDATION completed", runNum);
	}

	/**
	 * @throws IOException
	 * 
	 */
	private void initialize(String[] args) throws IOException {
		startTime = tStamp();
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
		if ((cmdLine.hasOption("h"))) {
			printUsage("");
			System.exit(0);
		}
		count = DEFAULT_CNT; // default value
		if ((cmdLine.hasOption("c"))) {
			String cntArg = cmdLine.getOptionValue("c");
			try {
				count = Integer.parseInt(cntArg);
			} catch (NumberFormatException e) {
				printUsage("Invalid Argument: 'cnt'");
				System.exit(0);
			}
		}

		// intitialize all directory paths....
		String wrkDirPath;
		if ((cmdLine.hasOption("d"))) {
			wrkDirPath = cmdLine.getOptionValue("d");
		} else {
			wrkDirPath = DEFAULT_DIR;
		}
		workDir = new File(wrkDirPath);
		tempDir = new File(workDir, "./tmp");
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
		logDir = new File(workDir, "./logs");
		if (!logDir.exists()) {
			logDir.mkdirs();
		}
		rsrcPath = wrkDirPath + "/resources/stressTests/";

		if ((cmdLine.hasOption("m"))) {
			testMode = cmdLine.getOptionValue("m");
			switch (testMode) {
			case "load":
			case "cvrt":
			case "val":
			case "xlate":
				break;
			default:
				printUsage("Invalid Argument: 'mode'");
				System.exit(0);
			}
		} else {
			testMode = "load";
		}

		if ((cmdLine.hasOption("f"))) {
			fileFmt = cmdLine.getOptionValue("f");
			switch (fileFmt) {
			case "xlsx":
			case "xml":
				break;
			default:
				printUsage("Invalid Argument: 'format'");
				System.exit(0);
			}
		}
		gcOn = (cmdLine.hasOption("g"));
		interactive = (cmdLine.hasOption("i"));

		if (!cmdLine.hasOption("s")) {
			printUsage("Missing Argument: 'size'");
			System.exit(0);
		}
		fileSize = cmdLine.getOptionValue("s");

		String logFileName = "/" + testMode + "_" + fileFmt + "_" + fileSize + "_" + startTime + ".log";
		File logFile = new File(logDir, logFileName);
		iLog = new InstrumentedLogger(logFile);
		iLog.setPrintToConsole(true);
		iLog.setMinLevel(iLog.LEV_WARN);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Start time = " + startTime, null, "JUnit");

		String statsOutFile = logDir.getPath() + "/" + testMode + "_" + fileFmt + "_" + fileSize + "_" + startTime
				+ ".csv";
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Stats file=" + statsOutFile, null, "JUnit");
		statsWriter = new PrintWriter(new BufferedWriter(new FileWriter(statsOutFile)));
		/* first row has column names */
		int cCnt = STAT_COL_NAMES.length;
		String headerRow = STAT_COL_NAMES[0];
		for (int i = 1; i < cCnt; i++) {
			headerRow = headerRow + stats_colSep + STAT_COL_NAMES[i];
		}
		statsWriter.write(headerRow + "\n");

	}

	private void loadOptions() {
		/*
		 * create the Options. Options represents a collection of Option instances,
		 * which describe the **POSSIBLE** options for a command-line
		 */
		options = new Options();
		options.addOption("h", "help", false, "Display this HELP file, then exit");
		options.addOption("s", "size", true, "file size to test with");
		options.addOption("d", "dir", true, "working directory. \n [OPTIONAL: default=" + DEFAULT_DIR + "]");
		options.addOption("c", "cnt", true,
				"how many times test is repeated \n[OPTIONAL: default=" + DEFAULT_CNT + "]");
		options.addOption("f", "format", true, "input file format ( xlsx | xml)\n[OPTIONAL: default='xlsx']");
		options.addOption("m", "mode", true, "test mode (load | cvrt | val | xlate) \n[OPTIONAL: defualt=load'");
		options.addOption("g", "garbage", false, "Request garbage collection between major stages");
		options.addOption("i", "interactive", false, "Run in interactive mode");
	}

	/**
	 * Parse and return command-line arguments.
	 * 
	 * @param args
	 * @return
	 * @throws ParseException
	 */
	private CommandLine parseOptions(String[] args) throws ParseException {
		// create the command line parser
		CommandLineParser parser = new DefaultParser();
		// parse the command line arguments
		CommandLine line = parser.parse(options, args);
		return line;
	}

	private void printUsage(String headerMsg) {
		System.out.println("\n" + headerMsg + "\n");
		formatter.printHelp("StressTest", null, options, null, true);
		String example = "StressTest -dir ./testRsrcs/ -s 100k -c 3\n";
		System.out.println("\n" + "Example: \n     " + example);
	}

	/**
	 * 
	 */
	private void reportStats(String contextMsg, int runNum) {
		// Get current size of heap in bytes, then convert to GB
		long heapSize = Runtime.getRuntime().totalMemory();
		float heapGB = heapSize / (1024 * 1014);// * 1024);
		long freeMem = Runtime.getRuntime().freeMemory();
		float freeMB = freeMem / (1024 * 1014);// * 1024);
		float usedMB = (heapSize - freeMem) / (1024 * 1014);
		String time = timer.elapsedTime();
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A,
				contextMsg + ": elapsed time = " + time + ", Memory used = " + usedMB + "MBytes", null, "JUnit");
		if (statsWriter != null) {
			// { "mode", "context", "heap", "usedMB, "time", "size", "forced GC" };
			String stats = testMode + stats_colSep + contextMsg + stats_colSep + runNum + stats_colSep + heapGB
					+ stats_colSep + usedMB + stats_colSep + time + stats_colSep + fileSize + stats_colSep + gcOn
					+ "\n";
			statsWriter.write(stats);
			statsWriter.flush();
		}
	}

	/**
	 * Returns the current date and time as String.
	 * 
	 * @return
	 */
	private static String tStamp() {
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		// Remember: SimpleDateFormat is NOT thread-safe so we need per-thread
		// instance
		SimpleDateFormat sdf = new SimpleDateFormat("kk:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return (sdf.format(cal.getTime()));
	}

	private void dumpLog() {
		String logFileName = "/" + testMode + "_" + fileSize + "_" + startTime + ".log";
		File logFile = new File(logDir, logFileName);
		System.out.println("\n ===   dumping log ===> " + logFile.getAbsolutePath());
		iLog.saveLog(logFile);

	}
}
