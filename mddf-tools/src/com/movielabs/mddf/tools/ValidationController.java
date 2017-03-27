/** 
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
package com.movielabs.mddf.tools;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.xml.sax.SAXParseException;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddf.MddfContext.MDDF_TYPE;
import com.movielabs.mddf.tools.ValidatorTool.Context;
import com.movielabs.mddf.tools.resources.Foo;
import com.movielabs.mddf.tools.util.logging.AdvLogPanel;
import com.movielabs.mddf.tools.util.logging.LogNavPanel;
import com.movielabs.mddflib.Obfuscator;
import com.movielabs.mddflib.Obfuscator.Target;
import com.movielabs.mddflib.avails.validation.AvailValidator;
import com.movielabs.mddflib.avails.xlsx.AvailsWrkBook;
import com.movielabs.mddflib.avails.xlsx.AvailsSheet;
import com.movielabs.mddflib.avails.xlsx.AvailsSheet.Version;
import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.avails.xml.XmlBuilder;
import com.movielabs.mddflib.logging.DefaultLogging;
import com.movielabs.mddflib.logging.Log4jAdapter;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.manifest.validation.ManifestValidator;
import com.movielabs.mddflib.manifest.validation.MecValidator;
import com.movielabs.mddflib.manifest.validation.profiles.CpeIP1Validator;
import com.movielabs.mddflib.manifest.validation.profiles.CpeValidator;
import com.movielabs.mddflib.manifest.validation.profiles.MMCoreValidator;
import com.movielabs.mddflib.manifest.validation.profiles.ProfileValidator;
import com.movielabs.mddflib.util.xml.XmlIngester;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Configure and control a Validation <i>tool chain</i>. The <tt>Validator</tt>
 * class implements the logic aspects of a validation workflow. Inputs may be
 * provided either via an interactive GUI, the command line interface, or by
 * reading a script.
 * 
 * <h4>DESIGN NOTE:</h4>Profile-specific validation modules need to be made
 * known to the <tt>Validator</tt>. Current design has these classes hard-coded
 * but future implementations need to use a more flexible plug-n-play approach.
 * 
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class ValidationController {

	public static final String MODULE_ID = "Validator";
	private static File tempDir = new File("./tmp");
	private static String[] supportedProfiles = { "none", "IP-0", "IP-1", "MMC-1"};
	private static HashSet<String> supportedProfileKeys;
	private static Options options = null;

	private boolean validateS = true;
	private boolean validateC = true;
	private boolean validateBP = false;
	private Context context;
	private LogMgmt logMgr;
	private LogNavPanel logNav = null;

	static {
		supportedProfileKeys = new HashSet<String>();
		supportedProfileKeys.addAll(Arrays.asList(supportedProfiles));
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
	}

	/**
	 * [Implementation DEFERED a/o 2016-04-11] Run preprocesssing functions via
	 * CLI and/or script.
	 *
	 * @param args
	 * @throws ParseException
	 */
	public static void main(String[] args) {
		CommandLine cmdLine = null;
		try {
			cmdLine = loadOptions(args);
		} catch (ParseException e) {
			e.printStackTrace();
			printHelp();
			System.exit(0);
		}
		DefaultLogging logger = new DefaultLogging();
		configureOptions(cmdLine, logger);
	}

	/**
	 * @param cmdLine
	 */
	private static void configureOptions(CommandLine cmdLine, LogMgmt logger) {
		if (cmdLine.hasOption("h")) {
			printHelp();
			System.exit(0);
		}
		if (cmdLine.hasOption("p")) {
			System.out.println("Supported profiles:\n");
			for (int i = 0; i < supportedProfiles.length; i++) {
				System.out.println("     " + supportedProfiles[i]);
			}
		}
		if (cmdLine.hasOption("logLevel")) {
			String llValue = cmdLine.getOptionValue("logLevel", "warn");
			switch (llValue) {
			case "verbose":
				logger.setMinLevel(LogMgmt.LEV_DEBUG);
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
	}

	/**
	 * Parse and return command-line arguments.
	 * 
	 * @param args
	 * @return
	 * @throws ParseException
	 */
	private static CommandLine loadOptions(String[] args) throws ParseException {
		// create the command line parser
		CommandLineParser parser = new DefaultParser();

		/*
		 * create the Options. Options represents a collection of Option
		 * instances, which describe the **POSSIBLE** options for a command-line
		 */
		options = new Options();
		options.addOption("h", "help", false, "Display this HELP file, then exit");
		options.addOption("v", "version", false, "Display software version and build date");
		options.addOption("s", "script", true, "Run a script file");
		options.addOption("l", "logFile", true,
				"Output file for logging. Default is './validatorLog.xxx' where 'xxx' denotes format");
		options.addOption("logFormat", true, "Format for log file; valid values are: " + "\n'csv' (DEFAULT)\n 'xml'");
		options.addOption("logLevel", true,
				"Filter for logging; valid values are: " + "\n'verbose'\n 'warn' (DEFAULT)\n 'error'\n 'info'");
		options.addOption("p", "profiles", false, "List supported profiles");
		options.addOption("useCases", false, "List supported use-case grouped by profile");

		if (args.length == 0) {
			printHelp();
			System.exit(0);
		}
		// parse the command line arguments
		CommandLine line = parser.parse(options, args);
		return line;
	}

	/**
	 * 
	 */
	private static void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		String header = "\nValidates one or more MovieLabs Digital Distribution Framework (MDDF) files.\n"
				+ getHelpHeader() + "\n\n";
		String footer = "\nPlease report issues at http://www.movielabs.com/ or info@movielabs.com";
		formatter.printHelp("Validator", header, options, footer, true);

	}

	private static String getHelpHeader() {
		String header = "";
		Object foo = new Foo();
		String rsrcPath = "./ValidatorHelp.txt";
		InputStream in = foo.getClass().getResourceAsStream(rsrcPath);
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

	public static String[] getSupportedProfiles() { 
		return supportedProfiles;
	}

	public static String[] getSupportedUseCases(String profile) {
//		ProfileValidator referenceInstance = profileMap.get(profile);
//		if (referenceInstance != null) {
//			List<String> pucList = referenceInstance.getSupporteUseCases(profile);
//			return pucList.toArray(new String[pucList.size()]);
//		} else {
			return null;
//		}
	}

	/**
	 * Construct new <tt>PreProcessor</tt> for use in the indicated
	 * <i>context</i>.
	 * 
	 * @param context
	 */
	public ValidationController(Context context, LogMgmt logMgr) {
		this.context = context;
		this.logMgr = logMgr;
		/*
		 * Determine if we are running in an interactive mode via a GUI. If so,
		 * there is a need at various stages to provide the logging UI with
		 * additional status updates.
		 */
		if (logMgr instanceof AdvLogPanel) {
			logNav = ((AdvLogPanel) logMgr).getLogNavPanel();
		} else {
			logNav = null;
		}
		logMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_ACTION, "Initializing Validator", null, MODULE_ID);
	}

	public void setValidation(boolean schema, boolean constraints, boolean bestPrac) {
		validateS = schema;
		validateC = constraints;
		validateBP = bestPrac;
	}

	public void runScript(File scriptFile) throws IOException {
		if (!scriptFile.isFile()) {
			return;
		}
		JSONObject script = XmlIngester.getAsJson(scriptFile);
		JSONObject validationTasks = script.optJSONObject("validate");
		if (validationTasks == null) {
			return;
		}
		String pathPrefix = validationTasks.optString("pathPrefix", "");
		if (pathPrefix.startsWith("./")) {
			// path is relative to script file
			pathPrefix = scriptFile.getParent() + "/" + pathPrefix;
		}

		List<String> useCaseList = new ArrayList<String>();

		JSONObject logging = validationTasks.optJSONObject("logging");
		String logLevel;
		String logFile;
		if (logging == null) {
			logLevel = "Warning";
			logFile = null;
		} else {
			logLevel = logging.optString("level", "Warning");
			logFile = logging.optString("output"); // default is NULL
		}
		if ((logFile != null) && (logFile.startsWith("./"))) {
			// path is relative to script file
			logFile = scriptFile.getParent() + "/" + logFile;
		}
		if (logMgr != null) {
			switch (logLevel) {
			case "Verbose":
				logMgr.setMinLevel(LogMgmt.LEV_DEBUG);
				break;
			case "Notice":
				logMgr.setMinLevel(LogMgmt.LEV_NOTICE);
				break;
			case "Warning":
				logMgr.setMinLevel(LogMgmt.LEV_WARN);
				break;
			case "Error":
				logMgr.setMinLevel(LogMgmt.LEV_ERR);
				break;
			case "Fatal":
				logMgr.setMinLevel(LogMgmt.LEV_FATAL);
				break;
			}
		}

		// .................
		JSONArray taskList = null;
		switch (context) {
		case AVAILS:
			setValidation(true, true, false);
			taskList = validationTasks.optJSONArray("avails");
			break;
		case MANIFEST:
			JSONObject checks = validationTasks.optJSONObject("checks");
			boolean chk_s = true;
			boolean check_c = (checks.optString("constraints", "Y").equalsIgnoreCase("Y"));
			boolean check_bp;
			if (check_c) {
				check_bp = (checks.optString("bestPrac", "Y").equalsIgnoreCase("Y"));
			} else {
				check_bp = false;
			}
			setValidation(chk_s, check_c, check_bp);
			taskList = validationTasks.optJSONArray("manifests");
		}
		// .................
		if (taskList == null || (taskList.isEmpty())) {
			return;
		} else {
			for (int i = 0; i < taskList.size(); i++) {
				JSONObject next = taskList.getJSONObject(i);
				String path = pathPrefix + next.getString("file");
				// String schema = next.optString("schema", "1.4");
				String profile = next.optString("profile", "none");
				// try {
				validate(path, profile, useCaseList);
				// } catch (JDOMException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// String errMsg = "EXCEPTION processing file " + path + "; " +
				// e.getLocalizedMessage();
				// System.out.println(errMsg);
				// logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_ACTION, errMsg, new
				// File(path), MODULE_ID);
				// }
			}
		}
		if ((logMgr != null) && (logFile != null)) {
			File logOutput = new File(logFile);
			logMgr.saveAs(logOutput, "csv");
		}
	}

	/**
	 * Validate one or more files. The <tt>srcPath</tt> argument indicates a
	 * either a single Common Media Manifest (CMM) or Avails file or a directory
	 * containing CMM and/or Avails files. If the later, any file found in the
	 * directory will be validated. The <tt>uxProfile</tt> argument specifies
	 * which profile to use for the validation process. A value of <tt>none</tt>
	 * indicates validation is to be performed only in the context of the
	 * generic CPE Information Model.
	 * 
	 * @param srcPath
	 *            location of a file or a directory containing CMM and/or Avails
	 *            files.
	 * @param uxProfile
	 *            the name of a valid profile or <tt>none</tt>
	 * @param useCases
	 * @throws IOException
	 * @throws JDOMException
	 */
	public void validate(String srcPath, String uxProfile, List<String> useCases) throws IOException {
		File srcFile = new File(srcPath);
		if (srcFile.isDirectory()) {
			boolean isRecursive = true;
			File[] inputFiles = srcFile.listFiles();
			int fileCount = inputFiles.length;
			for (int i = 0; i < fileCount; i++) {
				File aFile = (File) inputFiles[i];
				String message = aFile.getName();
				if (aFile.isFile()) {
					try {
						validateFile(aFile, uxProfile, useCases);
					} catch (Exception e) {
						String msg = e.getMessage();
						if (msg == null) {
							e.printStackTrace();
							msg = e.toString();
						}
						String details = "Exception while validating; file processing terminated.";
						logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_MANIFEST, msg, aFile, -1, MODULE_ID, details, null);
					}
				} else {
					boolean isDir = aFile.isDirectory();
					if (isDir && isRecursive) {
						// recursively descend directory tree...
						validate(aFile.getCanonicalPath(), uxProfile, useCases);
					}
				}
			}
		} else {
			// Process a single file
			try {
				validateFile(srcFile, uxProfile, useCases);
			} catch (Exception e) {
				e.printStackTrace();
				String msg = e.getMessage();
				if (msg == null) {
					msg = "Unspecified Exception while validating";
				}
				String loc = e.getStackTrace()[0].toString();
				String details = "Exception while validating; " + loc;
				logMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_MANIFEST, msg, srcFile, -1, MODULE_ID, details, null);
			}
		}
	}

	protected void validateFile(File srcFile, String uxProfile, List<String> useCases)
			throws IOException, JDOMException {
		String fileType = extractFileType(srcFile.getAbsolutePath());
		if (!(fileType.equals("xml") || fileType.equals("xlsx"))) {
			// Skipping file: Unsupported file type
			return;
		}
		logMgr.setCurrentFile(srcFile);
		Map<Object, Pedigree> pedigreeMap = null;
		Document xmlDoc = null;
		if (fileType.equals("xlsx")) {
			/* The XLSX format is only supported with AVAILS files */
			Map<String, Object> results = convertSpreadsheet(srcFile);
			if (results == null) {
				String msg = "Unable to convert Excel to XML";
				logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_AVAIL, msg, srcFile, -1, MODULE_ID, null, null);
				return;
			} else {
				srcFile = (File) results.get("xlsx");
				pedigreeMap = (Map<Object, Pedigree>) results.get("pedigree");
				xmlDoc = (Document) results.get("xml");
			}
		} else if (fileType.equals("xml")) {
			try {
				xmlDoc = XmlIngester.getAsXml(srcFile);
			} catch (SAXParseException e) {
				int ln = e.getLineNumber();
				String errMsg = "Invalid XML on or before line " + e.getLineNumber();
				String supplemental = e.getMessage();
				logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, srcFile, ln, MODULE_ID, supplemental, null);
				return;
			}
			if (logNav != null) {
				FILE_FMT fileFmt = MddfContext.identifyMddfFormat(xmlDoc.getRootElement());
				logNav.setMddfFormat(srcFile, fileFmt);
			}
		} else {
			/*
			 * Because of the file-type check at the start of this method, this
			 * code block should never be executed. However, just to be super
			 * cautious and safe, I've added this trap...
			 */
			String errMsg = "Skipping file: Unsupported file type";
			String supplemental = null;
			logMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_N_A, errMsg, srcFile, -1, MODULE_ID, supplemental, null);
			return;
		}
		XmlIngester.setSourceDirPath(srcFile.getAbsolutePath());
		/*
		 * Identify type of XML file (i.e., Manifest, Avail, etc)
		 */
		Element docRootEl = xmlDoc.getRootElement();
		String nSpaceUri = docRootEl.getNamespaceURI();
		String schemaType = null;
		MDDF_TYPE mddfType = null;
		int logTag = -1;
		if (nSpaceUri.contains("manifest")) {
			schemaType = "manifest";
			logTag = LogMgmt.TAG_MANIFEST;
			mddfType = MDDF_TYPE.MANIFEST;
		} else if (nSpaceUri.contains("avails")) {
			schemaType = "avails";
			logTag = LogMgmt.TAG_AVAIL;
			mddfType = MDDF_TYPE.AVAILS;
		} else if (nSpaceUri.contains("mdmec")) {
			schemaType = "mdmec";
			logTag = LogMgmt.TAG_MEC;
			mddfType = MDDF_TYPE.MEC;
		} else {
			String errMsg = "Validation terminated: Unable to identify file type.";
			String supplemental = "Root has unrecognized namespace " + nSpaceUri;
			logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, srcFile, -1, MODULE_ID, supplemental, null);
			return;
		}
		if (logNav != null) {
			logNav.setFileMddfType(srcFile, mddfType);
			logNav.setXml(srcFile, xmlDoc);
		}
		logMgr.log(LogMgmt.LEV_INFO, logTag, "Validating file as a " + schemaType, srcFile, MODULE_ID);
		switch (mddfType) {
		case MANIFEST:
			validateManifest(docRootEl, srcFile, uxProfile, useCases);
			break;
		case AVAILS:
			boolean isValid = validateAvail(docRootEl, pedigreeMap, srcFile);
			if (!isValid && (fileType.equals("xlsx"))) {
				File outputLoc = new File(tempDir, "TRACE_" + srcFile.getName().replace("xlsx", "xml"));
				XmlIngester.writeXml(outputLoc, xmlDoc);
			}
			break;
		case MEC:
			validateMEC(docRootEl, srcFile);
			break;
		}
	}

	private String extractFileType(String name) {
		int cutPt = name.lastIndexOf(".");
		String extension;
		if (cutPt < 0) {
			extension = "";
		} else {
			extension = name.substring(cutPt + 1, name.length());
		}
		return extension.toLowerCase();
	}

	// private String changeFileType(String inFileName, String fType) {
	// int ptr = inFileName.lastIndexOf(".xlsx");
	// String changed = inFileName.substring(0, ptr) + "." + fType;
	// return changed;
	// }

	/**
	 * Convert an AVAIL file in spreadsheet (i.e., xlsx) format to an XML file.
	 * The result <tt>Map</tt> that is returned will contain:
	 * <ul>
	 * <li>
	 * <tt>xlsx<tt>: the xlsx File that was passed as the input argument</li>
	 * <li><tt>xml<tt>: the JDOM2 Document that was created from the xlsx</li>
	 * <li><tt>pedigree<tt>: the <tt>Pedigree</tt> map that was created by the
	 * <tt>XmlBuilder</tt> during the conversion process.</li>
	 * </ul>
	 * 
	 * @param xslxFile
	 * @return
	 */
	private Map<String, Object> convertSpreadsheet(File xslxFile) {
		org.apache.logging.log4j.Logger log4j = new Log4jAdapter(logMgr, LogMgmt.TAG_AVAIL, "xlsx Converter");
		((Log4jAdapter) log4j).setFile(xslxFile);
		boolean autoCorrect = false;
		boolean exitOnError = false;
		AvailsWrkBook ss;
		try {
			ss = new AvailsWrkBook(xslxFile, log4j, exitOnError, autoCorrect);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return null;
		}
		int sheetNum = 0; // KLUDGE for now
		AvailsSheet as;
		try {
			as = ss.addSheet(sheetNum);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		Version templateVersion = as.getVersion();
		switch (templateVersion) {
		case V1_7:
			if (logNav != null) {
				logNav.setMddfFormat(xslxFile, FILE_FMT.AVAILS_1_7);
			}
			break;
		case V1_6:
			if (logNav != null) {
				logNav.setMddfFormat(xslxFile, FILE_FMT.AVAILS_1_6);
			}
			break;
		case UNK:
			logMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, "Unable to identify XLSX format ", xslxFile, MODULE_ID);
			break;
		default:
			logMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_AVAIL, "Unsupported template version " + templateVersion,
					xslxFile, MODULE_ID);
			return null;
		}
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, "Ingesting XLSX in " + templateVersion + " format", xslxFile,
				MODULE_ID);
		String inFileName = xslxFile.getName();
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
		String shortDesc = String.format("generated XML from %s:Sheet_%s on %s", inFileName, sheetNum, timeStamp);
		XmlBuilder xBuilder = new XmlBuilder(logMgr, templateVersion);
		xBuilder.setVersion("2.2");
		try {
			Document xmlJDomDoc = xBuilder.makeXmlAsJDom(as, shortDesc);
			Map<Object, Pedigree> pedigreeMap = xBuilder.getPedigreeMap();
			Map<String, Object> results = new HashMap<String, Object>();
			results.put("xlsx", xslxFile);
			results.put("xml", xmlJDomDoc);
			results.put("pedigree", pedigreeMap);

			return results;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Perform requested pre-processing on a single Avail file. The extent of
	 * the pre-processing may depend on the arguments passed when invoking this
	 * method as well as any a-priori set-up and configuration (e.g., use of the
	 * <tt>setValidation()</tt> function).
	 * 
	 * @param docRootEl
	 *            root of the Avail
	 * @param pedigreeMap
	 *            links XML Elements to their original source (used for logging
	 *            only)
	 * @param srcFile
	 *            is source from which XML was obtained (used for logging only)
	 * @throws IOException
	 * @throws JDOMException
	 */
	protected boolean validateAvail(Element docRootEl, Map<Object, Pedigree> pedigreeMap, File srcFile)
			throws IOException, JDOMException {
		boolean isValid = true;
		AvailValidator tool1 = new AvailValidator(validateC, logMgr);
		isValid = tool1.process(docRootEl, pedigreeMap, srcFile);
		if (!isValid) {
			String msg = "Validation FAILED; Terminating processing of file";
			logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, srcFile, -1, MODULE_ID, null, null);

			return isValid;
		}
		// ----------------------------------------------------------------
		String msg = "AVAIL Validation completed";
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, srcFile, -1, MODULE_ID, null, null);
		return isValid;
	}

	public void obfuscateAvail(File inFile, File outFile, Map<Target, String> replacementMap) {
		System.out.println("Obfuscate " + inFile.getName());
		System.out.println("Output to " + outFile.getName());
		Document xmlDoc;
		String fileType = extractFileType(inFile.getAbsolutePath());
		if (fileType.equals("xlsx")) {
			Map<String, Object> results = convertSpreadsheet(inFile);
			if (results == null) {
				String msg = "Obfuscation failed: Unable to convert Excel to XML";
				logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_AVAIL, msg, inFile, -1, MODULE_ID, null, null);
				return;
			}
			xmlDoc = (Document) results.get("xml");
		} else if (fileType.equals("xml")) {
			try {
				xmlDoc = XmlIngester.getAsXml(inFile);
			} catch (SAXParseException e) {
				int ln = e.getLineNumber();
				String errMsg = "Processing terminate due to invalid XML on or before line " + e.getLineNumber();
				String supplemental = e.getMessage();
				logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, inFile, ln, MODULE_ID, supplemental, null);
				return;
			} catch (IOException e) {
				e.printStackTrace();
				String errMsg = "Processing terminate due to invalid XML file";
				String supplemental = e.getMessage();
				logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, inFile, -1, MODULE_ID, supplemental, null);
				return;
			}
		} else {
			String errMsg = "File type '" + fileType + "' is not supported";
			logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, inFile, -1, MODULE_ID, null, null);
			return;
		}
		Document changedDoc = Obfuscator.process(xmlDoc, replacementMap, logMgr);
		XmlIngester.writeXml(outFile, changedDoc);

		String msg = "Obfuscated output in " + outFile.getAbsolutePath();
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, inFile, -1, MODULE_ID, null, null);

	}

	protected void validateMEC(Element docRootEl, File srcFile) throws IOException, JDOMException {
		boolean isValid = true;
		MecValidator tool1 = new MecValidator(validateC, logMgr);
		isValid = tool1.process(srcFile, docRootEl);
		if (!isValid) {
			String msg = "Validation FAILED; Terminating processing of file";
			logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MEC, msg, srcFile, -1, MODULE_ID, null, null);

			return;
		}
		// ----------------------------------------------------------------
		String msg = "MEC Validation completed";
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MEC, msg, srcFile, -1, MODULE_ID, null, null);
	}

	/**
	 * Perform requested pre-processing on a single Manifest file. The extent of
	 * the pre-processing will depend on the arguments passed when invoking this
	 * method as well as any a-priori set-up and configuration (e.g., use of the
	 * <tt>setValidation()</tt> function).
	 * 
	 * @param srcFile
	 * @param uxProfile
	 * @throws IOException
	 * @throws JDOMException
	 */
	protected void validateManifest(Element docRootEl, File srcFile, String uxProfile, List<String> useCases)
			throws IOException, JDOMException {
		boolean isValid = true;

		String schemaVer = ManifestValidator.identifyXsdVersion(docRootEl);
		ManifestValidator.setManifestVersion(schemaVer);

		List<String> profileNameList = identifyProfiles(docRootEl, srcFile, uxProfile);
		if (profileNameList.isEmpty() || profileNameList.contains("none")) {
			ManifestValidator tool1 = new ManifestValidator(validateC, logMgr);
			isValid = tool1.process(docRootEl, srcFile);
			return;
		}
		for (int i = 0; i < profileNameList.size(); i++) {
			String profile = profileNameList.get(i);
			// ProfileValidator referenceInstance = profileMap.get(profile);
			if (!supportedProfileKeys.contains(profile)) {
				String msg = "Unrecognized Profile: " + profile;
				logMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_PROFILE, msg, srcFile, -1, MODULE_ID, null, null);
			} else {
				String msg = "Validating compatibility with Profile '" + profile + "'";
				logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_PROFILE, msg, srcFile, -1, MODULE_ID, null, null);
				// String pvClass =
				// referenceInstance.getClass().getSimpleName();
				ProfileValidator pValidator = null;
				switch (profile) {
				case "IP-0":
				case "IP-01":
				case "IP-1":
					pValidator = new CpeValidator(logMgr);
					break;
				case "MMC-1":
					pValidator = new MMCoreValidator(logMgr);
					break;
				}
				pValidator.process(docRootEl, srcFile, profile, useCases);
			}
		}
		// ----------------------------------------------------------------
		String msg = "MANIFEST Validation completed";
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MANIFEST, msg, srcFile, -1, MODULE_ID, null, null);
	}

	/**
	 * Validate compatibility with any identified profiles.
	 * Compatibility/Profile was added in v1.5. Prior to that Profile can only
	 * be identified to Validator via external input (e.g., GUI or script). This
	 * code will allow the use of either or both modes. It also anticipates
	 * future changes to the schema to allow a single Manifest to be compatible
	 * with multiple Profiles.
	 * 
	 * @param docRootEl
	 * @param srcFile
	 * @param uxProfile
	 * @return
	 */
	private List<String> identifyProfiles(Element docRootEl, File srcFile, String uxProfile) {
		// make sure data structures got initialized..
		List<String> profileNameList = new ArrayList<String>();
		if (XmlIngester.MAN_VER.endsWith("1.4")) {
			if (!uxProfile.equals("none")) {
				profileNameList.add(uxProfile);
			}
			return profileNameList;
		}
		Element compEl = docRootEl.getChild("Compatibility", XmlIngester.manifestNSpace);
		List<Element> profileElList = compEl.getChildren("Profile", XmlIngester.manifestNSpace);
		if (!profileElList.isEmpty()) {
			for (int i = 0; i < profileElList.size(); i++) {
				Element nextProfile = profileElList.get(i);
				String nextName = nextProfile.getTextNormalize();
				if (!supportedProfileKeys.contains(nextName)) {
					String msg = "Compatibility/Profile specifies unrecognized Profile: " + nextName;
					logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_PROFILE, msg, srcFile, -1, MODULE_ID, null, null);
				} else if (!profileNameList.contains(nextName)) {
					profileNameList.add(nextName);
				}
			}
		}
		return profileNameList;
	}
}
