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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;

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
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
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

	private static HelpFormatter formatter = new HelpFormatter();
	private static InstrumentedLogger iLog;
	private static Options options;

	private static final int DEFAULT_CNT = 5;
	private static final String DEFAULT_DIR = "./resources/stressTests/";
	private static final String TEMP_DIR = "./tmp";
	private static String rsrcPath = DEFAULT_DIR;
	private static String filePrefix = "EMAAvails_v1.7.2_ParamountPictures_Google_";
	private static String testMode;
	private static String MODULE_ID = "Tester";
	private static File tempDir;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
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
		int count = DEFAULT_CNT; // default value
		if ((cmdLine.hasOption("c"))) {
			String cntArg = cmdLine.getOptionValue("c");
			try {
				count = Integer.parseInt(cntArg);
			} catch (NumberFormatException e) {
				printUsage("Invalid Argument: 'cnt'");
				System.exit(0);
			}
		}
		if ((cmdLine.hasOption("d"))) {
			rsrcPath = cmdLine.getOptionValue("d");
		}

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

		if (!cmdLine.hasOption("s")) {
			printUsage("Missing Argument: 'size'");
			System.exit(0);
		}
		String fileSize = cmdLine.getOptionValue("s");

		//intitialize..........
		tempDir = new File(TEMP_DIR);
		if(!tempDir.exists()) {
			tempDir.mkdirs();
		}
		Version version = Version.V1_7_2;
		iLog = new InstrumentedLogger();
		iLog.setPrintToConsole(true);
		iLog.setMinLevel(iLog.LEV_WARN);
		long maxMem = Runtime.getRuntime().maxMemory();
		float maxGB = maxMem / (1024 * 1014 * 1024);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Max Memory = " + maxGB + "GBytes", null, "JUnit");

		try {
			runTest(fileSize, version, count);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void loadOptions() {
		/*
		 * create the Options. Options represents a collection of Option instances,
		 * which describe the **POSSIBLE** options for a command-line
		 */
		options = new Options();
		options.addOption("h", "help", false, "Display this HELP file, then exit");
		options.addOption("s", "size", true, "file size to test with");
		options.addOption("d", "dir", true, "test file directory. \n [OPTIONAL: default=" + DEFAULT_DIR + "]");
		options.addOption("c", "cnt", true,
				"how many times test is repeated \n[OPTIONAL: default=" + DEFAULT_CNT + "]");
		options.addOption("m", "mode", true, "test mode (load | val | xlate) \n[OPTIONAL: defualt=load'");
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
		formatter.printHelp("StressTest", null, options, null, true);
		String example = "StressTest -dir ./testRsrcs/ -s 100k -c 3\n";
		System.out.println("\n" + "Example: \n     " + example);
	}

	/**
	 * @param testFile
	 * @param version
	 * @param count
	 */
	private static void runTest(String testFileSize, Version version, int count) {
		String testFileName = filePrefix + testFileSize + ".xlsx";
		String srcFilePath = rsrcPath + testFileName;
		File srcFile = new File(srcFilePath);

		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "*** Testing with file " + srcFilePath, null, "JUnit");
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Repeat count=" + count, null, "JUnit");
		for (int i = 1; i < (count + 1); i++) {
			String msg = "Run " + i + " of " + count;
			System.out.println(msg);
			iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Run " + i + " of " + count + ":: " + tStamp() + " start time", null,
					"JUnit");
			Map<String, Object> results = AvailsWrkBook.convertSpreadsheet(srcFile, version, null, iLog);
			Document xmlDoc = (Document) results.get("xml");
			collect();
			File outputLoc = new File(tempDir, "TRACE_" + srcFile.getName().replace("xlsx", "xml"));
			XmlIngester.writeXml(outputLoc, xmlDoc);

			switch (testMode) {
			default:
				break;
			case "val":
			case "xlate":
				File xlsxFile = (File) results.get("xlsx");
				Map<Object, Pedigree> pedigreeMap = (Map<Object, Pedigree>) results.get("pedigree");
				FILE_FMT srcMddfFmt = (FILE_FMT) results.get("srcFmt");
				MddfTarget target = new MddfTarget(srcFile, xmlDoc, iLog);
				iLog.log(LogMgmt.LEV_INFO, iLog.TAG_N_A, "Validating file as a " + target.getMddfType().toString(),
						srcFile, MODULE_ID);
				boolean isValid = true;
				AvailValidator tool1 = new AvailValidator(true, iLog);
				isValid = tool1.process(target, pedigreeMap);

				break;
			}
			iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Run " + i + " of " + count + ":: " + tStamp() + " end time", null,
					"JUnit");
			//
			collect();
		}
	}

	/**
	 * 
	 */
	private static void collect() {
		// Get current size of heap in bytes, then convert to GB
		long heapSize = Runtime.getRuntime().totalMemory();
		float heapGB = heapSize / (1024 * 1014 * 1024);
		iLog.log(iLog.LEV_INFO, iLog.TAG_N_A, "Memory usage = " + heapGB + "GBytes", null, "JUnit");
		System.gc();
		// sleep for 2 seconds before next attempt (allows GC (I hope))
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
	}

	/**
	 * Returns the current date and time as String.
	 * 
	 * @return
	 */
	public static String tStamp() {
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		// Remember: SimpleDateFormat is NOT thread-safe so we need per-thread
		// instance
		SimpleDateFormat sdf = new SimpleDateFormat("kk:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return (sdf.format(cal.getTime()));
	}

	private static void dumpLog() {
		System.out.println("\n ===   dumping log ===");
		iLog.printLog();
		System.out.println(" === End log dump  ====");

	}
}
