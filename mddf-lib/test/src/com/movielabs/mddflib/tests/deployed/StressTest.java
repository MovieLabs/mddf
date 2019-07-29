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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jdom2.Document;

import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.avails.validation.AvailValidator;
import com.movielabs.mddflib.avails.xml.AvailsWrkBook;
import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.avails.xml.AvailsSheet;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.logging.LogEntryNode;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.testsupport.InstrumentedLogger;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * Run diagnostic stress tests
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
	public static final String STAT_COL_NAMES[] = { "mode", "context", "Run", "Heap", "Time", "size", "forced GC" };
	private static final String stats_colSep = ",";

	private static String MODULE_ID = "Tester";
	private static File tempDir;
	private static final int DEFAULT_CNT = 5;
	private static String filePrefix = "TestCase_";
	
	private String testMode;
	private  String rsrcPath = DEFAULT_DIR;
	private File logDir;
	private boolean interactive;
	private StopWatch timer = new StopWatch();
	private PrintWriter statsWriter;
	private boolean gcOn;
	private String fileSize;
	private int count;
	private File workDir; 

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
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Force GC=" + gcOn, null, "JUnit");
		try {
			runTest(version, count);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (interactive) {
			System.out.println("Hit <Enter> to exit");
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

			// Reading data using readLine
			try {
				String name = reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		statsWriter.flush();
		statsWriter.close();
		System.out.println("Good-bye");

	}

	/**
	 * @throws IOException
	 * 
	 */
	private void initialize(String[] args) throws IOException {
		iLog = new InstrumentedLogger();
		iLog.setPrintToConsole(true);
		iLog.setMinLevel(iLog.LEV_WARN);
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

		// intitialize alll directory paths....
		String wrkDirPath;
		if ((cmdLine.hasOption("d"))) {
			wrkDirPath = cmdLine.getOptionValue("d");
		}else {
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
		rsrcPath = wrkDirPath +"/resources/stressTests/";

		if ((cmdLine.hasOption("m"))) {
			testMode = cmdLine.getOptionValue("m");
			switch (testMode) {
			case "load":
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

		gcOn = (cmdLine.hasOption("g"));
		interactive = (cmdLine.hasOption("i"));

		if (!cmdLine.hasOption("s")) {
			printUsage("Missing Argument: 'size'");
			System.exit(0);
		}
		fileSize = cmdLine.getOptionValue("s");


		String statsOutFile = logDir.getPath() + "/StressTest_" + testMode + "_" + fileSize + "_" + tStamp() + ".csv";
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
	 * @param testFile
	 * @param version
	 * @param count
	 * @param force    if <tt>true</tt> request garbage collection between major
	 *                 stages
	 */
	private void runTest(Version version, int count) {
		String testFileName = filePrefix + fileSize + ".xlsx"; 
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
			String msg = "\nRun " + i + " of " + count;
			System.out.println(msg);
			iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Run " + i + " of " + count + ":: " + tStamp() + " start time", null,
					"JUnit");
			// reset the stopwatch
			timer.reset();
			reportStats(" initial", i);
			AvailsSheet sheet = AvailsWrkBook.loadWorkBook(srcFile, iLog, null, 0);
			if (testMode.equals("load")) {
				reportStats("LOAD completed", i);
				continue;
			}
			reportStats("LOAD completed", i);
			Map<String, Object> results = AvailsWrkBook.mapToXml(sheet, srcFile, version, null, iLog);
			if (testMode.equals("cvrt")) {
				reportStats("XML CONVERSION completed", i);
				continue;
			}
			reportStats("XML CONVERSION completed", i);

			Document xmlDoc = (Document) results.get("xml");
			File outputLoc = new File(tempDir, "TRACE_" + srcFile.getName().replace("xlsx", "xml"));
			XmlIngester.writeXml(outputLoc, xmlDoc);

			results = AvailsWrkBook.convertSpreadsheet(srcFile, version, null, iLog);
			xmlDoc = (Document) results.get("xml");
			outputLoc = new File(tempDir, "TRACE_" + srcFile.getName().replace("xlsx", "xml"));
			XmlIngester.writeXml(outputLoc, xmlDoc);

			File xlsxFile = (File) results.get("xlsx");
			Map<Object, Pedigree> pedigreeMap = (Map<Object, Pedigree>) results.get("pedigree");
			FILE_FMT srcMddfFmt = (FILE_FMT) results.get("srcFmt");
			MddfTarget target = new MddfTarget(srcFile, xmlDoc, iLog);
			iLog.log(LogMgmt.LEV_INFO, iLog.TAG_N_A, "Validating file as a " + target.getMddfType().toString(), srcFile,
					MODULE_ID);
			boolean isValid = true;
			AvailValidator tool1 = new AvailValidator(true, iLog);
			isValid = tool1.process(target, pedigreeMap);
			reportStats("VALIDATION completed", i);

			iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Run " + i + " of " + count + ":: " + tStamp() + " end time", null,
					"JUnit");
			//
		}
	}

	/**
	 * 
	 */
	private void reportStats(String contextMsg, int runNum) {
		// Get current size of heap in bytes, then convert to GB
		long heapSize = Runtime.getRuntime().totalMemory();
		float heapGB = heapSize / (1024 * 1014);// * 1024);
		String time = timer.elapsedTime();
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A,
				contextMsg + ": elapsed time = " + time + ", Memory usage = " + heapGB + "MBytes", null, "JUnit");
		if (statsWriter != null) {
			// { "mode", "context", "heap", "time", "size", "forced GC" };
			String stats = testMode + stats_colSep + contextMsg + stats_colSep + runNum + stats_colSep + heapGB
					+ stats_colSep + time + stats_colSep + fileSize + stats_colSep + gcOn + "\n";
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
		System.out.println("\n ===   dumping log ===");
		iLog.printLog();
		System.out.println(" === End log dump  ====");

	}
}
