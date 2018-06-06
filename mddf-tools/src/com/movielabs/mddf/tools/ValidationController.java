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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.xml.sax.SAXParseException;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddf.MddfContext.MDDF_TYPE;
import com.movielabs.mddf.tools.util.logging.AdvLogPanel;
import com.movielabs.mddf.tools.util.logging.LogNavPanel;
import com.movielabs.mddflib.Obfuscator;
import com.movielabs.mddflib.Obfuscator.Target;
import com.movielabs.mddflib.avails.validation.AvailValidator;
import com.movielabs.mddflib.avails.xml.AvailsWrkBook;
import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.manifest.validation.CpeValidator;
import com.movielabs.mddflib.manifest.validation.ManifestValidator;
import com.movielabs.mddflib.manifest.validation.MecValidator;
import com.movielabs.mddflib.manifest.validation.profiles.MMCoreValidator;
import com.movielabs.mddflib.manifest.validation.profiles.ProfileValidator;
import com.movielabs.mddflib.util.Translator;
import com.movielabs.mddflib.util.xml.MddfTarget;
import com.movielabs.mddflib.util.xml.XmlIngester;

import net.sf.json.JSONObject;

/**
 * Configure and control a Validation <i>tool chain</i> when using
 * <tt>mddf-lib</tt> in a stand-alone desktop environment. The
 * <tt>Validator</tt> class implements the logic aspects of a validation
 * workflow. Inputs may be provided either via an interactive GUI, the command
 * line interface, or by reading a script.
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

	public class MddfFileFilter implements FileFilter {

		private final String[] okFileExtensions = new String[] { "xml", "xlsx" };

		public boolean accept(File file) {
			for (String extension : okFileExtensions) {
				if (file.isDirectory()) {
					return true;
				}
				if (file.getName().toLowerCase().endsWith(extension)) {
					return true;
				}
			}
			System.out.println("Rejecting " + file.getName());
			return false;
		}

	}

	public static final String MODULE_ID = "Validator";
	private static File tempDir = new File("./tmp");
	private static String[] supportedProfiles = { "none", "IP-0", "IP-1", "MMC-1" };
	private static HashSet<String> supportedProfileKeys;

	private boolean validateS = true;
	private boolean validateC = true;
	private boolean validateBP = false;
	private boolean isRecursive = true;
	private LogMgmt logMgr;
	private LogNavPanel logNav = null;
	private EnumSet<FILE_FMT> xportFmts = null;
	private File exportDir = null;

	static {
		supportedProfileKeys = new HashSet<String>();
		supportedProfileKeys.addAll(Arrays.asList(supportedProfiles));
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
	}

	public static String[] getSupportedProfiles() {
		return supportedProfiles;
	}

	public static String[] getSupportedUseCases(String profile) {
		// ProfileValidator referenceInstance = profileMap.get(profile);
		// if (referenceInstance != null) {
		// List<String> pucList =
		// referenceInstance.getSupporteUseCases(profile);
		// return pucList.toArray(new String[pucList.size()]);
		// } else {
		return null;
		// }
	}

	/**
	 * Construct new <tt>PreProcessor</tt> for use in the indicated
	 * <i>context</i>.
	 * 
	 * @param context
	 */
	public ValidationController(LogMgmt logMgr) {
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
		// JSONArray taskList = null;
		// switch (context) {
		// case AVAILS:
		// setValidation(true, true, false);
		// taskList = validationTasks.optJSONArray("avails");
		// break;
		// case MANIFEST:
		// JSONObject checks = validationTasks.optJSONObject("checks");
		// boolean chk_s = true;
		// boolean check_c = (checks.optString("constraints",
		// "Y").equalsIgnoreCase("Y"));
		// boolean check_bp;
		// if (check_c) {
		// check_bp = (checks.optString("bestPrac", "Y").equalsIgnoreCase("Y"));
		// } else {
		// check_bp = false;
		// }
		// setValidation(chk_s, check_c, check_bp);
		// taskList = validationTasks.optJSONArray("manifests");
		// }
		// // .................
		// if (taskList == null || (taskList.isEmpty())) {
		// return;
		// } else {
		// for (int i = 0; i < taskList.size(); i++) {
		// JSONObject next = taskList.getJSONObject(i);
		// String path = pathPrefix + next.getString("file");
		// // String schema = next.optString("schema", "1.4");
		// String profile = next.optString("profile", "none");
		// // try {
		// validate(path, profile, useCaseList);
		// // } catch (JDOMException e) {
		// // // TODO Auto-generated catch block
		// // e.printStackTrace();
		// // String errMsg = "EXCEPTION processing file " + path + "; " +
		// // e.getLocalizedMessage();
		// // System.out.println(errMsg);
		// // logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_ACTION, errMsg, new
		// // File(path), MODULE_ID);
		// // }
		// }
		// }
		if ((logMgr != null) && (logFile != null)) {
			File logOutput = new File(logFile);
			logMgr.saveAs(logOutput, "csv");
		}
	}

	public void setTranslations(EnumSet<FILE_FMT> xportFmts, File exportDir) {
		this.xportFmts = xportFmts;
		this.exportDir = exportDir;

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
			File[] inputFiles = srcFile.listFiles(new MddfFileFilter());
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
				String msg = e.getCause().getMessage();
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
		fileType = fileType.toLowerCase();
		if (!(fileType.equals("xml") || fileType.equals("xlsx"))) {
			// Skipping file: Unsupported file type
			String errMsg = "Skipping file " + srcFile.getName() + ": Unsupported file type";
			String supplemental = "File must be of type '.xml' or, if Avails, '.xlsx'";
			switch (fileType) {
			case "xls":
			case "xlt":
			case "xlm":
			case "xlsm":
			case "xltx":
			case "xltm":
			case "xlsb":
			case "xla":
			case "xlam":
			case "xll":
			case "xlw":
				supplemental = "Avails Excel support restricted to 'xlsx'. All other formats are potential security risk";
				break;
			}
			logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, errMsg, srcFile, -1, MODULE_ID, supplemental, null);
			return;
		}
		logMgr.setCurrentFile(srcFile);
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, "Validating " + srcFile.getPath(), srcFile, MODULE_ID);

		Map<Object, Pedigree> pedigreeMap = null;
		FILE_FMT srcMddfFmt = null;
		Document xmlDoc = null;
		MddfTarget target = null;
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
				srcMddfFmt = (FILE_FMT) results.get("srcFmt");
				target = new MddfTarget(srcFile, xmlDoc, logMgr);
			}
		} else if (fileType.equals("xml")) {
			target = new MddfTarget(srcFile, logMgr);
			xmlDoc = target.getXmlDoc();// XmlIngester.getAsXml(srcFile);
			if (target == null) {
				return;
			}
			srcMddfFmt = MddfContext.identifyMddfFormat(xmlDoc.getRootElement());
			if (logNav != null) {
				logNav.setMddfFormat(srcFile, srcMddfFmt);
			}
		} else {
			/*
			 * Because of the file-type check at the start of this method, this
			 * code block should never be executed. However, just to be super
			 * cautious and safe, I've added this trap...
			 */
			String errMsg = "Skipping file: Unsupported file type";
			String supplemental = null;
			logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, errMsg, srcFile, -1, MODULE_ID, supplemental, null);
			return;
		}
		XmlIngester.setSourceDirPath(srcFile.getAbsolutePath());
		MDDF_TYPE mddfType = target.getMddfType();
		int logTag = target.getLogTag();
		if (mddfType == null) {
			String errMsg = "Validation terminated: Unable to identify file type.";
			logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, srcFile, -1, MODULE_ID, null, null);
			return;
		}
		if (logNav != null) {
			logNav.setFileMddfType(srcFile, mddfType);
			logNav.setXml(srcFile, xmlDoc);
		}
		logMgr.log(LogMgmt.LEV_INFO, logTag, "Validating file as a " + mddfType.toString(), srcFile, MODULE_ID);
		boolean isValid;
		switch (mddfType) {
		case MANIFEST:
			isValid = validateManifest(target, uxProfile, useCases);
			break;
		case AVAILS:
			isValid = validateAvail(target, pedigreeMap);
			if (!isValid && (fileType.equals("xlsx"))) {
				File outputLoc = new File(tempDir, "TRACE_" + srcFile.getName().replace("xlsx", "xml"));
				XmlIngester.writeXml(outputLoc, xmlDoc);
			}
			if (isValid) {
				// Export translated versions??
				if ((exportDir != null) && (xportFmts != null)) {
					String baseFileName = trimFileName(srcFile.getName());
					xportFmts.remove(srcMddfFmt);
					int cnt = Translator.translateAvails(target, xportFmts, exportDir, baseFileName, true, logMgr);
					logMgr.log(LogMgmt.LEV_INFO, logTag, "Exported in " + cnt + " format(s)", srcFile, MODULE_ID);
				}
			}
			break;
		case MEC:
			validateMEC(target);
			break;
		}
	}

	/**
	 * removes the file-type extension from a file name.
	 * 
	 * @param name
	 * @return
	 */
	private String trimFileName(String name) {
		int trimAt = name.lastIndexOf(".");
		if (trimAt > 0) {
			name = name.substring(0, trimAt);
		}
		return name;
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
		Map<String, Object> results = AvailsWrkBook.convertSpreadsheet(xslxFile, null, logMgr);
		if (results != null) {
			FILE_FMT srcMddfFmt = (FILE_FMT) results.get("srcFmt");
			if (logNav != null) {
				logNav.setMddfFormat(xslxFile, srcMddfFmt);
			}
		}
		return results;
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
	protected boolean validateAvail(MddfTarget target, Map<Object, Pedigree> pedigreeMap)
			throws IOException, JDOMException {
		boolean isValid = true;
		AvailValidator tool1 = new AvailValidator(validateC, logMgr);
		isValid = tool1.process(target, pedigreeMap);
		if (!isValid) {
			String msg = "Validation FAILED; Terminating processing of file";
			logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, target.getSrcFile(), -1, MODULE_ID, null, null);

			return isValid;
		}
		// ----------------------------------------------------------------
		String msg = "AVAIL Validation completed";
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, target.getSrcFile(), -1, MODULE_ID, null, null);
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

	protected boolean validateMEC(MddfTarget target) throws IOException, JDOMException {
		boolean isValid = true;
		MecValidator tool1 = new MecValidator(validateC, logMgr);
		isValid = tool1.process(target);
		if (!isValid) {
			String msg = "Validation FAILED; Terminating processing of file";
			logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MEC, msg, target.getSrcFile(), -1, MODULE_ID, null, null);
			return false;
		}
		// ----------------------------------------------------------------
		String msg = "MEC Validation completed";
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MEC, msg, target.getSrcFile(), -1, MODULE_ID, null, null);
		return isValid;
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
	protected boolean validateManifest(MddfTarget target, String uxProfile, List<String> useCases)
			throws IOException, JDOMException {
		boolean isValid = true;

		String schemaVer = ManifestValidator.identifyXsdVersion(target);
		ManifestValidator.setManifestVersion(schemaVer);

		List<String> profileNameList = identifyProfiles(target, uxProfile);
		if (profileNameList.isEmpty() || profileNameList.contains("none")) {
			ManifestValidator tool1 = new ManifestValidator(validateC, logMgr);
			isValid = tool1.process(target);
			List<File> supportingMecFiles = tool1.getSupportingMecFiles();
			for (File nextMec : supportingMecFiles) {
				try {
					validateReferencedMec(nextMec);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			}
		} else {
			for (int i = 0; i < profileNameList.size(); i++) {
				String profile = profileNameList.get(i);
				// ProfileValidator referenceInstance = profileMap.get(profile);
				if (!supportedProfileKeys.contains(profile)) {
					String msg = "Unrecognized Profile: " + profile;
					logMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_PROFILE, msg, target.getSrcFile(), -1, MODULE_ID, null,
							null);
				} else {
					String msg = "Validating compatibility with Profile '" + profile + "'";
					logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_PROFILE, msg, target.getSrcFile(), -1, MODULE_ID, null,
							null);
					// String pvClass =
					// referenceInstance.getClass().getSimpleName();
					ProfileValidator pValidator = null;
					switch (profile) {
					case "IP-0":
					case "IP-01":
					case "IP-1":
						pValidator = new CpeValidator(logMgr);
						isValid = pValidator.process(target, profile, useCases) && isValid;
						break;
					case "MMC-1":
						pValidator = new MMCoreValidator(logMgr);
						isValid = pValidator.process(target, profile, useCases) && isValid;
						List<File> supportingMecFiles = ((ManifestValidator) pValidator).getSupportingMecFiles();
						for (File nextMec : supportingMecFiles) {
							try {
								validateReferencedMec(nextMec);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								// e.printStackTrace();
							}
						}
						break;
					}
				}
			}
		}
		// ----------------------------------------------------------------
		String msg = "MANIFEST Validation completed";
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MANIFEST, msg, target.getSrcFile(), -1, MODULE_ID, null, null);
		return isValid;
	}

	private void validateReferencedMec(File mecFile) throws IOException, JDOMException {
		if (!mecFile.exists()) {
			return;
		}
		String fileType = extractFileType(mecFile.getAbsolutePath());
		fileType = fileType.toLowerCase();
		if (!fileType.equals("xml")) {
			// ERROR
			String errMsg = "Referenced location does not contain MEC file.";
			String supplemental = "Invalid file extension";
			logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, mecFile, -1, MODULE_ID, supplemental, null);
			return;
		}
		FILE_FMT srcMddfFmt = null;
		Document xmlDoc = null;
		try {
			xmlDoc = XmlIngester.getAsXml(mecFile);
		} catch (SAXParseException e) {
			int ln = e.getLineNumber();
			String errMsg = "Invalid XML on or before line " + e.getLineNumber();
			String supplemental = e.getMessage();
			logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, mecFile, ln, MODULE_ID, supplemental, null);
			return;
		}
		srcMddfFmt = MddfContext.identifyMddfFormat(xmlDoc.getRootElement());
		if (logNav != null) {
			logNav.setMddfFormat(mecFile, srcMddfFmt);
		}

		XmlIngester.setSourceDirPath(mecFile.getAbsolutePath());
		/*
		 * Identify type of XML file (i.e., Manifest, Avail, etc)
		 */
		Element docRootEl = xmlDoc.getRootElement();
		String nSpaceUri = docRootEl.getNamespaceURI();
		String schemaType = null;
		MDDF_TYPE mddfType = null;
		int logTag = -1;
		if (nSpaceUri.contains("mdmec")) {
			schemaType = "mdmec";
			logTag = LogMgmt.TAG_MEC;
			mddfType = MDDF_TYPE.MEC;
		} else {
			String errMsg = "Referenced location does not contain MEC file.";
			String supplemental = "Root has unrecognized namespace " + nSpaceUri;
			logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, mecFile, -1, MODULE_ID, supplemental, null);
			return;
		}
		if (logNav != null) {
			logNav.setFileMddfType(mecFile, mddfType);
			logNav.setXml(mecFile, xmlDoc);
		}
		logMgr.log(LogMgmt.LEV_INFO, logTag, "Validating file as a " + schemaType, mecFile, MODULE_ID);
		MddfTarget target = new MddfTarget(mecFile, logMgr);
		validateMEC(target);
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
	private List<String> identifyProfiles(MddfTarget target, String uxProfile) {
		// make sure data structures got initialized..
		List<String> profileNameList = new ArrayList<String>();
		if (XmlIngester.MAN_VER.endsWith("1.4")) {
			if (!uxProfile.equals("none")) {
				profileNameList.add(uxProfile);
			}
			return profileNameList;
		}
		Element docRootEl = target.getXmlDoc().getRootElement();
		Element compEl = docRootEl.getChild("Compatibility", XmlIngester.manifestNSpace);
		List<Element> profileElList = compEl.getChildren("Profile", XmlIngester.manifestNSpace);
		if (!profileElList.isEmpty()) {
			for (int i = 0; i < profileElList.size(); i++) {
				Element nextProfile = profileElList.get(i);
				String nextName = nextProfile.getTextNormalize();
				if (!supportedProfileKeys.contains(nextName)) {
					String msg = "Compatibility/Profile specifies unrecognized Profile: " + nextName;
					logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_PROFILE, msg, target.getSrcFile(), -1, MODULE_ID, null,
							null);
				} else if (!profileNameList.contains(nextName)) {
					profileNameList.add(nextName);
				}
			}
		}
		return profileNameList;
	}

	/**
	 * @return the isRecursive
	 */
	public boolean isRecursive() {
		return isRecursive;
	}

	/**
	 * If <tt>true</tt> processing of a directory will be recursive.
	 * 
	 * @param isRecursive
	 *            the isRecursive to set
	 */
	public void setRecursive(boolean isRecursive) {
		this.isRecursive = isRecursive;
	}
}
