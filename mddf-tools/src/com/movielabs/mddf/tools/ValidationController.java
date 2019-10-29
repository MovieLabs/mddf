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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.xml.sax.SAXParseException;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddf.MddfContext.MDDF_TYPE;
import com.movielabs.mddf.tools.util.VersionChooserDialog;
import com.movielabs.mddf.tools.util.logging.AdvLogPanel;
import com.movielabs.mddf.tools.util.logging.LogNavPanel;
import com.movielabs.mddflib.Obfuscator;
import com.movielabs.mddflib.Obfuscator.DataTarget;
import com.movielabs.mddflib.avails.validation.AvailValidator;
import com.movielabs.mddflib.avails.xml.AvailsSheet.Version;
import com.movielabs.mddflib.avails.xml.AvailsWrkBook;
import com.movielabs.mddflib.avails.xml.AvailsWrkBook.RESULT_STATUS;
import com.movielabs.mddflib.avails.xml.streaming.StreamingXmlBuilder;
import com.movielabs.mddflib.avails.xml.Pedigree;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.manifest.validation.CpeValidator;
import com.movielabs.mddflib.manifest.validation.ManifestValidator;
import com.movielabs.mddflib.manifest.validation.MecValidator;
import com.movielabs.mddflib.manifest.validation.profiles.MMCoreValidator;
import com.movielabs.mddflib.manifest.validation.profiles.ProfileValidator;
import com.movielabs.mddflib.util.StringUtils;
import com.movielabs.mddflib.util.Translator;
import com.movielabs.mddflib.util.xml.InterimMddfTarget;
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
	private static final boolean DBG_XLSX = true;
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
	 * Construct new <tt>PreProcessor</tt> for use in the indicated <i>context</i>.
	 * 
	 * @param context
	 */
	public ValidationController(LogMgmt logMgr) {
		this.logMgr = logMgr;
		/*
		 * Determine if we are running in an interactive mode via a GUI. If so, there is
		 * a need at various stages to provide the logging UI with additional status
		 * updates.
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
		if ((scriptFile == null) || !scriptFile.isFile()) {
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
	 * Validate one or more files. The <tt>srcPath</tt> argument indicates a either
	 * a single Common Media Manifest (CMM) or Avails file or a directory containing
	 * CMM and/or Avails files. If the later, any file found in the directory will
	 * be validated. The <tt>uxProfile</tt> argument specifies which profile to use
	 * for the validation process. A value of <tt>none</tt> indicates validation is
	 * to be performed only in the context of the generic CPE Information Model.
	 * 
	 * @param srcPath   location of a file or a directory containing CMM and/or
	 *                  Avails files.
	 * @param uxProfile the name of a valid profile or <tt>none</tt>
	 * @throws IOException
	 * @throws JDOMException
	 */
	public void validate(String srcPath, String uxProfile) throws IOException {
		File srcFile = new File(srcPath);
		if (srcFile.isDirectory()) {
			File[] inputFiles = srcFile.listFiles(new MddfFileFilter());
			int fileCount = inputFiles.length;
			for (int i = 0; i < fileCount; i++) {
				File aFile = (File) inputFiles[i];
				String message = aFile.getName();
				if (aFile.isFile()) {
					/*
					 * this is a initial target for use in early logging. If all goes well it get
					 * over-ridden very quickly.
					 */
					MddfTarget target = new InterimMddfTarget(aFile, logMgr);
					logMgr.pushFileContext(target);
					logMgr.clearLog(target);
					try {
						validateFile(target, uxProfile);
					} catch (Exception e) {
						String msg = e.getMessage();
						if (msg == null) {
							e.printStackTrace();
							msg = e.toString();
						}
						String details = "Exception while validating; file processing terminated.";
						logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_MANIFEST, msg, target, null, MODULE_ID, details, null);
					} finally {
						/* Some memory mgmt??? */
						System.gc();
					}
//					logMgr.popFileContext(aFile);
				} else {
					boolean isDir = aFile.isDirectory();
					if (isDir && isRecursive) {
						// recursively descend directory tree...
						validate(aFile.getCanonicalPath(), uxProfile);
					}
				}
			}
		} else {
			// Process a single file
			/*
			 * this is a initial target for use in early logging. If all goes well it get
			 * over-ridden very quickly.
			 */
			MddfTarget target = new InterimMddfTarget(srcFile, logMgr);
			try {
				logMgr.pushFileContext(target);
				logMgr.clearLog(target);
				validateFile(target, uxProfile);
			} catch (Exception e) {
				e.printStackTrace();
				String msg = e.getLocalizedMessage();
				if (msg == null) {
					msg = "Unspecified Exception while validating";
				}
				String loc = e.getStackTrace()[0].toString();
				String details = "Exception while validating; " + loc;
				logMgr.log(LogMgmt.LEV_FATAL, LogMgmt.TAG_MANIFEST, msg, target, null, MODULE_ID, details, null);
			} finally {
				/* Some memory mgmt??? */
				System.gc();
			}
		}
	}

	protected void validateFile(MddfTarget target, String uxProfile) throws IOException, JDOMException {
		File srcFile = target.getSrcFile();
		String fileType = StringUtils.extractFileType(srcFile.getAbsolutePath());
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
			logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, errMsg, target, -1, MODULE_ID, supplemental, null);
			return;
		}
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, "Validating " + srcFile.getPath(), target, MODULE_ID);
		Map<Object, Pedigree> pedigreeMap = null;
		FILE_FMT srcMddfFmt = null;
		Document xmlDoc = null;
		if (fileType.equals("xlsx")) {
			/* The XLSX format is only supported with AVAILS files */
			Map<String, Object> results = convertSpreadsheet_v2(srcFile);
			if (results == null) {
				String msg = "Unable to convert Excel to XML";
				logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_AVAIL, msg, target, -1, MODULE_ID, null, null);
				return;
			} else {
				RESULT_STATUS completionStatus = (RESULT_STATUS) results.get("status");
				if (completionStatus == RESULT_STATUS.CANCELLED) {
					return;
				}
				srcFile = (File) results.get("xlsx");
				pedigreeMap = (Map<Object, Pedigree>) results.get("pedigree");
				xmlDoc = (Document) results.get("xml");
				srcMddfFmt = (FILE_FMT) results.get("srcFmt");
				target = new MddfTarget( xmlDoc, srcFile,logMgr);
				/* Some memory mgmt??? */
				results = null;
				System.gc();
			}
		} else if (fileType.equals("xml")) {
			target = new MddfTarget(srcFile, logMgr);
			xmlDoc = target.getXmlDoc();// XmlIngester.getAsXml(srcFile);
			if (target == null) {
				return;
			}
			srcMddfFmt = MddfContext.identifyMddfFormat(xmlDoc.getRootElement());
			if (logNav != null) {
				logNav.setMddfFormat(target, srcMddfFmt);
			}
		} else {
			/*
			 * Because of the file-type check at the start of this method, this code block
			 * should never be executed. However, just to be super cautious and safe, I've
			 * added this trap...
			 */
			String errMsg = "Skipping file: Unsupported file type";
			String supplemental = null;
			logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_N_A, errMsg, null, -1, MODULE_ID, supplemental, null);
			return;
		}

		MDDF_TYPE mddfType = target.getMddfType();
		int logTag = target.getLogTag();
		if (mddfType == null) {
			String errMsg = "Validation terminated: Unable to identify file type.";
			logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, target, -1, MODULE_ID, null, null);
			return;
		}

		logMgr.log(LogMgmt.LEV_INFO, logTag, "Validating file as a " + mddfType.toString(), target, MODULE_ID);
		boolean isValid;
		switch (mddfType) {
		case MANIFEST:
			isValid = validateManifest(target);
			break;
		case AVAILS:
			isValid = validateAvail(target, pedigreeMap);
			if ((!isValid || DBG_XLSX) && (fileType.equals("xlsx"))) {
				File outputLoc = new File(tempDir, "TRACE_" + srcFile.getName().replace("xlsx", "xml"));
				XmlIngester.writeXml(outputLoc, xmlDoc);
			}
			if (isValid) {
				// Export translated versions??
				if ((exportDir != null) && (xportFmts != null)) {
					String baseFileName = trimFileName(srcFile.getName());
					xportFmts.remove(srcMddfFmt);
					int cnt = Translator.translateAvails(target, xportFmts, exportDir, baseFileName, true, logMgr);
					logMgr.log(LogMgmt.LEV_INFO, logTag, "Exported in " + cnt + " format(s)", target, MODULE_ID);
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

	/**
	 * Convert an AVAIL file in spreadsheet (i.e., xlsx) format to an XML file. If
	 * <tt>inStream</tt> is <tt>null</tt> the <tt>xlsxFile</tt> parameter is used to
	 * read a file accessible via the local system. Otherwise the contents of the
	 * <tt>inStream</tt> is used.
	 * <p>
	 * The result <tt>Map</tt> that is returned will contain:
	 * </p>
	 * <ul>
	 * <li><tt>xlsx</tt>: the xlsx File that was passed as the input argument</li>
	 * <li><tt>xml</tt>: the JDOM2 Document that was created from the xlsx</li>
	 * <li><tt>pedigree</tt>: the <tt>Pedigree</tt> map that was created by the
	 * <tt>XmlBuilder</tt> during the conversion process.</li>
	 * <li><tt>srcFmt</tt>: the <tt>MddfContect.FILE_FMT</tt> of the ingested file.
	 * </ul>
	 * 
	 * @param xslxFile
	 * @param xlsxVersion xlsx template version
	 * @param inStream
	 * @param logMgr
	 * @return a Map&lt;String, Object&gt;
	 */
	private Map<String, Object> convertSpreadsheet_v2(File xslxFile) {
		VersionChooserDialog vcd = new VersionChooserDialog();
		vcd.setLocationRelativeTo(logNav);
		vcd.setVisible(true);
		if (vcd.isCancelled()) {
			Map<String, Object> results = new HashMap<String, Object>();
			results.put("status", RESULT_STATUS.CANCELLED);
			return results;
		}
		Version xlsxVersion = vcd.getSelected();

		StreamingXmlBuilder bldr = new StreamingXmlBuilder(logMgr, xlsxVersion);
		String shortDesc = "Converted using STREAMING XML builder";
		Map<String, Object> results = bldr.convert(xslxFile, null, 0, shortDesc);
		return results;
	}

	/**
	 * Convert an AVAIL file in spreadsheet (i.e., xlsx) format to an XML file. The
	 * result <tt>Map</tt> that is returned will contain:
	 * <ul>
	 * <li><tt>xlsx<tt>: the xlsx File that was passed as the input argument</li>
	 * <li><tt>xml<tt>: the JDOM2 Document that was created from the xlsx</li>
	 * <li><tt>pedigree<tt>: the <tt>Pedigree</tt> map that was created by the
	 * <tt>XmlBuilder</tt> during the conversion process.</li>
	 * </ul>
	 * 
	 * @param xslxFile
	 * @return
	 * @deprecated
	 */
	private Map<String, Object> convertSpreadsheet_v1(File xslxFile) {

		VersionChooserDialog vcd = new VersionChooserDialog();
		vcd.setLocationRelativeTo(logNav);
		vcd.setVisible(true);
		if (vcd.isCancelled()) {
			Map<String, Object> results = new HashMap<String, Object>();
			results.put("status", RESULT_STATUS.CANCELLED);
			return results;
		}
		Version version = vcd.getSelected();
		Map<String, Object> results = AvailsWrkBook.convertSpreadsheet(xslxFile, version, null, logMgr);
		if (results != null) {
			FILE_FMT srcMddfFmt = (FILE_FMT) results.get("srcFmt");
			if (logNav != null) {
				// comented out so code compiles.....
//				logNav.setMddfFormat(xslxFile, srcMddfFmt);
			}
		}
		return results;
	}

	/**
	 * Validate a single Avail file. The extent of the pre-processing may depend on
	 * a-priori set-up and configuration (e.g., use of the <tt>setValidation()</tt>
	 * function).
	 * 
	 * @param target
	 * @param pedigreeMap
	 * @return
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
			logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, target, -1, MODULE_ID, null, null);

			return isValid;
		}
		// ----------------------------------------------------------------
		String msg = "AVAIL Validation completed";
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, target, -1, MODULE_ID, null, null);
		return isValid;
	}

	public void obfuscateAvail(File inFile, File outFile, Map<DataTarget, String> replacementMap) {

		MddfTarget availsTarget = new MddfTarget(inFile, logMgr);
		if (!inFile.canRead()) { 
			String msg = "Obfuscation failed: File not found or is not readable";
			logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_AVAIL, msg, availsTarget, null, MODULE_ID, null, null);
			return;
		}
		Document xmlDoc;
		String fileType = StringUtils.extractFileType(inFile.getAbsolutePath());
		if (fileType.equals("xlsx")) {
			Map<String, Object> results = convertSpreadsheet_v2(inFile);
			if (results == null) {
				String msg = "Obfuscation failed: Unable to convert Excel to XML";
				logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_AVAIL, msg, availsTarget, null, MODULE_ID, null, null);
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
				logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, availsTarget, ln, MODULE_ID, supplemental, null);
				return;
			} catch (IOException e) {
				e.printStackTrace();
				String errMsg = "Processing terminate due to invalid XML file";
				String supplemental = e.getMessage();
				logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, availsTarget, null, MODULE_ID, supplemental, null);
				return;
			}
		} else {
			String errMsg = "File type '" + fileType + "' is not supported";
			logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, availsTarget, null, MODULE_ID, null, null);
			return;
		}
		Document changedDoc = Obfuscator.process(xmlDoc, replacementMap, logMgr);
		XmlIngester.writeXml(outFile, changedDoc);

		String msg = "Obfuscated output in " + outFile.getAbsolutePath();
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_AVAIL, msg, availsTarget, null, MODULE_ID, null, null);

	}

	protected boolean validateMEC(MddfTarget target) throws IOException, JDOMException {
		boolean isValid = true;
		MecValidator tool1 = new MecValidator(validateC, logMgr);
		isValid = tool1.process(target);
		if (!isValid) {
			String msg = "Validation FAILED; Terminating processing of file";
			logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MEC, msg, target, -1, MODULE_ID, null, null);
			return false;
		}
		// ----------------------------------------------------------------
		String msg = "MEC Validation completed";
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MEC, msg, target, -1, MODULE_ID, null, null);
		return isValid;
	}

	/**
	 * Perform requested pre-processing on a single Manifest file. The extent of the
	 * pre-processing will depend on any a-priori set-up and configuration (e.g.,
	 * use of the <tt>setValidation()</tt> function).
	 * 
	 * @param target
	 * @return
	 * @throws IOException
	 * @throws JDOMException
	 */
	protected boolean validateManifest(MddfTarget target) throws IOException, JDOMException {
		boolean isValid = true;

		List<String> profileNameList = identifyProfiles(target);
		if (profileNameList.isEmpty() || profileNameList.contains("none")) {
			ManifestValidator tool1 = new ManifestValidator(validateC, logMgr);
			isValid = tool1.process(target);
			Map<String, List<Element>> supportingFiles = ((ManifestValidator) tool1).getSupportingRsrcLocations();
			if (supportingFiles != null) {
				Set<String> foobar = supportingFiles.keySet();
				for (String path : foobar) {
					if (path.endsWith("xml") && !path.contains(":")) {
						validateReferencedMddf(target, path, supportingFiles.get(path));
					}
				}
			}

		} else {
			for (int i = 0; i < profileNameList.size(); i++) {
				String profile = profileNameList.get(i);
				// ProfileValidator referenceInstance = profileMap.get(profile);
				if (!supportedProfileKeys.contains(profile)) {
					String msg = "Unrecognized Profile: " + profile;
					logMgr.log(LogMgmt.LEV_DEBUG, LogMgmt.TAG_PROFILE, msg, target, -1, MODULE_ID, null, null);
				} else {
					String msg = "Validating compatibility with Profile '" + profile + "'";
					logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_PROFILE, msg, target, -1, MODULE_ID, null, null);

					ProfileValidator pValidator = null;
					switch (profile) {
					case "IP-0":
					case "IP-01":
					case "IP-1":
						pValidator = new CpeValidator(logMgr);
						isValid = pValidator.process(target, profile) && isValid;
						break;
					case "MMC-1":
						pValidator = new MMCoreValidator(logMgr);
						isValid = pValidator.process(target, profile) && isValid;
						Map<String, List<Element>> supportingFiles = ((ManifestValidator) pValidator)
								.getSupportingRsrcLocations();
						Set<String> foobar = supportingFiles.keySet();
						for (String path : foobar) {
							if (path.endsWith("xml") && !path.contains(":")) {
								validateReferencedMddf(target, path, supportingFiles.get(path));
							}
						}

						break;
					}
				}
			}
		}
		// ----------------------------------------------------------------
		String msg = "MANIFEST Validation completed";
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MANIFEST, msg, target, -1, MODULE_ID, null, null);
		return isValid;
	}

	/**
	 * @param parentTarget
	 * @param path
	 * @param list
	 * @throws IOException
	 * @throws JDOMException
	 */
	private void validateReferencedMddf(MddfTarget parentTarget, String path, List<Element> list)
			throws IOException, JDOMException {
		File mddfFile = new File(path);
		String fileType = StringUtils.extractFileType(path);
		fileType = fileType.toLowerCase();
		if (!fileType.equals("xml")) {
			// ERROR
			String errMsg = "Referenced location does not contain MDDF file.";
			String supplemental = "Invalid file extension";
			/*
			 * Since a given file may have been referenced by more than 1 <ContainerLoc>
			 * element, we may need to gen multiple error msgs.
			 */
			for (Element containerLocEl : list) {
				logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_N_A, errMsg, parentTarget, containerLocEl, MODULE_ID,
						supplemental, null);
			}
			return;
		}
		MddfTarget curTarget  = new MddfTarget(parentTarget, mddfFile, logMgr);
		if(!mddfFile.exists()) {
			String logMsg = "Referenced container not found";
			for (Element clocEl : list) {
				logMgr.log(LogMgmt.LEV_WARN, LogMgmt.TAG_MANIFEST, logMsg, parentTarget, clocEl, MODULE_ID, null, null);
			}
			return;
		}
		MDDF_TYPE type = curTarget.getMddfType();
		switch (type) {
		case MANIFEST:
			String logMsg = "Skipping validation of ExternalManifest (not yet supported)";
			for (Element clocEl : list) {
				logMgr.log(LogMgmt.LEV_WARN, LogMgmt.TAG_MANIFEST, logMsg, parentTarget, clocEl, MODULE_ID, null, null);
			}
			break;
		case MEC:
			logMsg = "Validating referenced MEC file";
			logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_MANIFEST, logMsg, parentTarget, null, MODULE_ID, null, null);
			validateMEC(curTarget);
			break;
		default:
			logMsg = "Referenced file has unrecognized MDDF type " + type.toString();
			int tag = curTarget.getLogTag();
			for (Element containerLocEl : list) {
				logMgr.log(LogMgmt.LEV_ERR, tag, logMsg, parentTarget, containerLocEl, MODULE_ID, null, null);
			}

		}

	}

	/**
	 * Identify profiles the target Manifest is intended to be compatible with.
	 * Compatibility/Profile was added in v1.5. Prior to that Profile can only be
	 * identified to Validator via external input (e.g., GUI or script). This code
	 * will allow the use of either or both modes. It also anticipates future
	 * changes to the schema to allow a single Manifest to be compatible with
	 * multiple Profiles.
	 * 
	 * @param target
	 * @return
	 */
	private List<String> identifyProfiles(MddfTarget target) {

		String schemaVer = ManifestValidator.identifyXsdVersion(target);
		Namespace manifestNSpace = Namespace.getNamespace("manifest",
				MddfContext.NSPACE_MANIFEST_PREFIX + schemaVer + MddfContext.NSPACE_MANIFEST_SUFFIX);
		// make sure data structures got initialized..
		List<String> profileNameList = new ArrayList<String>();
		Element docRootEl = target.getXmlDoc().getRootElement();
		Element compEl = docRootEl.getChild("Compatibility", manifestNSpace);
		List<Element> profileElList = compEl.getChildren("Profile", manifestNSpace);
		if (!profileElList.isEmpty()) {
			for (int i = 0; i < profileElList.size(); i++) {
				Element nextProfile = profileElList.get(i);
				String nextName = nextProfile.getTextNormalize();
				if (!supportedProfileKeys.contains(nextName)) {
					String msg = "Compatibility/Profile specifies unrecognized Profile: " + nextName;
					logMgr.log(LogMgmt.LEV_ERR, LogMgmt.TAG_PROFILE, msg, target, -1, MODULE_ID, null, null);
				} else if (!profileNameList.contains(nextName)) {
					profileNameList.add(nextName);
				}
			}
		}
		String msg = "Applicable profiles: " + profileNameList.toString();
		logMgr.log(LogMgmt.LEV_INFO, LogMgmt.TAG_PROFILE, msg, target, -1, MODULE_ID, null, null);
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
	 * @param isRecursive the isRecursive to set
	 */
	public void setRecursive(boolean isRecursive) {
		this.isRecursive = isRecursive;
	}

}
