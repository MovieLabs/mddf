/**
 * Copyright (c) 2017 MovieLabs

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
package com.movielabs.mddf;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jdom2.Element;
import org.jdom2.Namespace;

import com.movielabs.mddflib.util.xml.XmlIngester;

/**
 * Defines constants and Enum types used to indicate the context of various MDDF
 * operations and requests. A primary function of the <tt>MddfContext</tt> class
 * is providing a mechanism for identifying the type, version, and format of an
 * MDDF file (e.g. an Avails file that conforms to v2.2 of the standard and is
 * encoded as XML). <tt>MddfContext</tt> also provides constants that are used
 * to identify and locate resources required for processing MDDF files (e.g.,
 * the XSD specs) .
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class MddfContext {

	public static final String SCHEMA_PREFIX = "http://www.movielabs.com/schema/";

	public static final String NSPACE_CMD_PREFIX = SCHEMA_PREFIX + "md/v";
	public static final String NSPACE_CMD_SUFFIX = "/md";

	public static final String NSPACE_MANIFEST_PREFIX = SCHEMA_PREFIX + "manifest/v";
	public static final String NSPACE_MANIFEST_SUFFIX = "/manifest";

	public static final String NSPACE_MDMEC_PREFIX = SCHEMA_PREFIX + "mdmec/v";
	public static final String NSPACE_MDMEC_SUFFIX = "/mdmec";

	public static final String NSPACE_AVAILS_PREFIX = SCHEMA_PREFIX + "avails/v";
	public static final String NSPACE_AVAILS_SUFFIX = "/avails";

	public static final String NSPACE_AOD_PREFIX = SCHEMA_PREFIX + "md/delivery/v";
	public static final String NSPACE_AOD_SUFFIX = "/delivery";

	/**
	 * Path indicating location of XSD files for all supported MDDF schemas.
	 */
	public static final String RSRC_PATH = "/com/movielabs/mddf/resources/";

	public static final String PROP_PATH = "/com/movielabs/mddflib/build.properties";

	public static final String CUR_RATINGS_VER = "v2.4.8";

	private static Map<String, String[]> stdsVersions;

	private static Map<String, FILE_FMT> map2FmtEnums = new HashMap<String, FILE_FMT>();

	private static Properties buildProps;

	static {
		// --- Supported versions of standards (in order) ---
		//fake version cm here
		String[] CM_VER = {"2.11","2.10","2.9",  "2.8", "2.7.1", "2.7", "2.6", "2.5", "2.4" };
		// fake version manifest here
		String[] MANIFEST_VER = { "1.12","1.11","1.10", "1.9", "1.8.1", "1.8", "1.7", "1.6.1", "1.6", "1.5" };
		String[] MEC_VER = { "2.10", "2.9", "2.8", "2.7.1", "2.7", "2.6", "2.5", "2.4" };
		String[] AVAILS_X_VER = {"2.6", "2.5.2",  "2.5", "2.4", "2.3", "2.2.2", "2.2.1", "2.2", "2.1" };
		String[] AVAILS_E_VER = { "1.9", "1.8", "1.7.3", "1.7.2", "1.7", "1.6" };
		String[] AOD_VER = { "1.0", "1.1", "1.2" };
		String[] MMM_BP = { "1.0" };

		stdsVersions = new HashMap<String, String[]>();
		stdsVersions.put("CM", CM_VER);
		stdsVersions.put("MANIFEST", MANIFEST_VER);
		stdsVersions.put("MEC", MEC_VER);
		stdsVersions.put("AVAIL", AVAILS_X_VER);
		stdsVersions.put("AVAIL_E", AVAILS_E_VER);
		stdsVersions.put("AOD", AOD_VER);
		/*
		 * Special case: for documentation we need version of 'Best Practices'
		 */
		stdsVersions.put("MMM-BP", MMM_BP);

	}

	public enum MDDF_TYPE {
		MEC, AVAILS, MANIFEST, OSTATUS, AOD, UNK
	};

	public enum FILE_FMT {
		AVAILS_1_6("Avails", "1.6", "xlsx"), AVAILS_1_7("Avails", "1.7", "xlsx"),
		AVAILS_1_7_3("Avails", "1.7.3", "xlsx"), AVAILS_1_7_2("Avails", "1.7.2", "xlsx"),
		AVAILS_1_8("Avails", "1.8", "xlsx"), AVAILS_1_9("Avails", "1.9", "xlsx"), AVAILS_2_1("Avails", "2.1", "xml"),
		AVAILS_2_2("Avails", "2.2", "xml"), AVAILS_2_2_1("Avails", "2.2.1", "xml"),
		AVAILS_2_2_2("Avails", "2.2.2", "xml"), AVAILS_2_3("Avails", "2.3", "xml"), AVAILS_2_4("Avails", "2.4", "xml"),
		AVAILS_2_5("Avails", "2.5", "xml"),AVAILS_2_5_2("Avails", "2.5.2", "xml"), AVAILS_2_6("Avails", "2.6", "xml"), 
		MANIFEST_1_4("Manifest", "1.4", "xml"),
		MANIFEST_1_5("Manifest", "1.5", "xml"), MANIFEST_1_6("Manifest", "1.6", "xml"),
		MANIFEST_1_6_1("Manifest", "1.6.1", "xml"), MANIFEST_1_7("Manifest", "1.7", "xml"),
		MANIFEST_1_8("Manifest", "1.8", "xml"), MANIFEST_1_8_1("Manifest", "1.8.1", "xml"),
		MANIFEST_1_9("Manifest", "1.9", "xml"), MANIFEST_1_10("Manifest", "1.10", "xml"),
		// fake version 1.12 here
		MANIFEST_1_11("Manifest", "1.11", "xml"), MANIFEST_1_12("Manifest", "1.12", "xml"),
		MDMEC_2_4("MEC", "2.4", "xml"), MDMEC_2_5("MEC", "2.5", "xml"), MDMEC_2_6("MEC", "2.6", "xml"),
		MDMEC_2_7("MEC", "2.7", "xml"), MDMEC_2_7_1("MEC", "2.7.1", "xml"), MDMEC_2_9("MEC", "2.9", "xml"),
		MDMEC_2_10("MEC", "2.10", "xml"), MDMEC_2_8("MEC", "2.8", "xml"), AOD_1_0("AOD", "1.0", "xml"), 
		AOD_1_1("AOD", "1.1", "xml"), AOD_1_2("AOD", "1.2", "xml");

		private String standard;
		private String ver;

		private String encoding;
		private String label;

		/**
		 * @param standard
		 * @param ver
		 * @param encoding
		 */
		private FILE_FMT(String standard, String ver, String encoding) {
			this.standard = standard;
			this.ver = ver;
			this.encoding = encoding;
			label = standard + " v" + ver + " (" + encoding + ")";
			map2FmtEnums.put(label, this);
		}

		@Override
		public String toString() {
			return label;
		}

		public String getEncoding() {
			return encoding;
		}

		/**
		 * @return the version
		 */
		public String getVersion() {
			return ver;
		}

		/**
		 * @return the standard
		 */
		public String getStandard() {
			return standard;
		}

	}

	public static FILE_FMT identifyMddfFormat(Element targetEl) {
		Element docRootEl = targetEl.getDocument().getRootElement();
		String nSpaceUri = docRootEl.getNamespaceURI();
		String schemaType = null;
		if (nSpaceUri.contains("manifest")) {
			schemaType = "manifest";
		} else if (nSpaceUri.contains("avails")) {
			schemaType = "avails";
		} else if (nSpaceUri.contains("mdmec")) {
			schemaType = "mdmec";
		} else if (nSpaceUri.contains("delivery")) {
			schemaType = "delivery";
		} else {
			return null;
		}
		String[] parts = nSpaceUri.split(schemaType + "/v");
		String schemaVer = parts[1].replace("/" + schemaType, "");
		return identifyMddfFormat(schemaType, schemaVer);
	}

	public static FILE_FMT getMddfFormat(String label) {
		return map2FmtEnums.get(label);
	}

	public static FILE_FMT identifyMddfFormat(String standard, String schemaVer) {
		switch (standard.toLowerCase()) {
		case "manifest":
			switch (schemaVer) {
			case "1.4":
				return FILE_FMT.MANIFEST_1_4;
			case "1.5":
				return FILE_FMT.MANIFEST_1_5;
			case "1.6":
				return FILE_FMT.MANIFEST_1_6;
			case "1.6.1":
				return FILE_FMT.MANIFEST_1_6_1;
			case "1.7":
				return FILE_FMT.MANIFEST_1_7;
			case "1.8":
				return FILE_FMT.MANIFEST_1_8;
			case "1.8.1":
				return FILE_FMT.MANIFEST_1_8_1;
			case "1.9":
				return FILE_FMT.MANIFEST_1_9;
			case "1.10":
				return FILE_FMT.MANIFEST_1_10;
			case "1.11":
				return FILE_FMT.MANIFEST_1_11;
			// fake version
			case "1.12":
				return FILE_FMT.MANIFEST_1_12;
			}
			break;
		case "avails":
			switch (schemaVer) {
			case "1.6":
				return FILE_FMT.AVAILS_1_6;
			case "1.7":
				return FILE_FMT.AVAILS_1_7;
			case "1.7.2":
				return FILE_FMT.AVAILS_1_7_2;
			case "1.7.3":
				return FILE_FMT.AVAILS_1_7_3;
			case "1.8":
				return FILE_FMT.AVAILS_1_8;
			case "1.9":
				return FILE_FMT.AVAILS_1_9;
			case "2.6":
				return FILE_FMT.AVAILS_2_1;
			case "2.5.2":
				return FILE_FMT.AVAILS_2_6;
			case "2.5":
				return FILE_FMT.AVAILS_2_5;
			case "2.4":
				return FILE_FMT.AVAILS_2_4;
			case "2.3":
				return FILE_FMT.AVAILS_2_3;
			case "2.2.2":
				return FILE_FMT.AVAILS_2_2_2;
			case "2.2.1":
				return FILE_FMT.AVAILS_2_2_1;
			case "2.2":
				return FILE_FMT.AVAILS_2_2;
			case "2.1":
				return FILE_FMT.AVAILS_2_1;
			}
			break;
		case "mdmec":
			switch (schemaVer) {
			case "2.4":
				return FILE_FMT.MDMEC_2_4;
			case "2.5":
				return FILE_FMT.MDMEC_2_5;
			case "2.6":
				return FILE_FMT.MDMEC_2_6;
			case "2.7":
				return FILE_FMT.MDMEC_2_7;
			case "2.7.1":
				return FILE_FMT.MDMEC_2_7_1;
			case "2.8":
				return FILE_FMT.MDMEC_2_8;
			case "2.9":
				return FILE_FMT.MDMEC_2_9;
			case "2.10":
				return FILE_FMT.MDMEC_2_10;
			}
			break;
		case "delivery":
			switch (schemaVer) {
			case "1.0":
				return FILE_FMT.AOD_1_0;
			case "1.1":
				return FILE_FMT.AOD_1_1;
			case "1.2":
				return FILE_FMT.AOD_1_2;
			}
			break;
		}
		return null;
	}

	/**
	 * Return a list of all know versions of the specified MDDF standard. The valid
	 * standards are:
	 * <ul>
	 * <li>CM</li>
	 * <li>MANIFEST</li>
	 * <li>MEC</li>
	 * <li>AVAIL (for XML-only)</li>
	 * <li>AVAIL-E (for XLSX only)</li>
	 * <li>AOD (a.k.a. Delivery)</li>
	 * </ul>
	 * 
	 * @param standard identifier for an MDDF statndard
	 * @return an array with all know versions of the specified MDDF standard
	 */
	public static String[] getSupportedVersions(String standard) {
		return stdsVersions.get(standard);
	}

	/**
	 * Return the appropriate versions of supporting schemas that are to be used
	 * with the specified MDDF standard. Supporting schemas are
	 * <ul>
	 * <li>Common Metadata ("MD") and</li>
	 * <li>Media Entertainment Code ("MDMEC")</li>
	 * </ul>
	 * If a supporting schema is not used by the main schema then the map will
	 * contain a <tt>null</tt> value for that key.
	 * 
	 * @param standard an MDDF XML standard for Avails, Manifest, or MEC.
	 * @return map identifying versions of supporting schemas that are to be used
	 *         with the specified MDDF standard
	 */
	public static Map<String, String> getReferencedXsdVersions(FILE_FMT standard) {
		Map<String, String> uses = new HashMap<String, String>();
		switch (standard) {
		case AVAILS_2_6:
			// fake version  here
			uses.put("MD", "2.11");
			uses.put("MDMEC", "2.10");
			break;
		case AVAILS_2_5_2:
			uses.put("MD", "2.9");
			uses.put("MDMEC", "2.9");
			break;
		case AVAILS_2_5:
			uses.put("MD", "2.8");
			uses.put("MDMEC", "2.8");
			break;
		case AVAILS_2_4:
			uses.put("MD", "2.7");
			uses.put("MDMEC", "2.7");
			break;
		case AVAILS_2_3:
			uses.put("MD", "2.6");
			uses.put("MDMEC", "2.6");
			break;
		case AVAILS_2_2_2:
		case AVAILS_2_2_1:
			uses.put("MD", "2.5");
			uses.put("MDMEC", "2.5");
			break;
		case AVAILS_2_2:
		case AVAILS_2_1:
			uses.put("MD", "2.4");
			uses.put("MDMEC", "2.4");
			break;
		case AVAILS_1_7:
			uses.put("MD", "2.3");
			uses.put("MDMEC", "2.3");
			break;
		// ===========================
		case AVAILS_1_8:
			uses.put("MD", "2.3");
			uses.put("MDMEC", "2.3");
			break;
		// .......................
		case MANIFEST_1_4:
			uses.put("MD", "2.3");
			break;
		case MANIFEST_1_5:
			uses.put("MD", "2.4");
			break;
		case MANIFEST_1_6:
			uses.put("MD", "2.5");
			break;
		case MANIFEST_1_6_1:
			uses.put("MD", "2.6");
			break;
		case MANIFEST_1_7:
			uses.put("MD", "2.6");
			break;
		case MANIFEST_1_8:
			uses.put("MD", "2.7");
			break;
		case MANIFEST_1_8_1:
			uses.put("MD", "2.7.1");
			break;
		case MANIFEST_1_9:
			uses.put("MD", "2.8");
			break;
		case MANIFEST_1_10:
			uses.put("MD", "2.9");
			break;
		case MANIFEST_1_11:
			uses.put("MD", "2.10");
			break;
		// fake version
		case MANIFEST_1_12:
			uses.put("MD", "2.11");
			break;
		case MDMEC_2_10:
			// fake version here
			uses.put("MD", "2.11");
			break;
		case MDMEC_2_9:
			uses.put("MD", "2.9");
			break;
		case MDMEC_2_8:
			uses.put("MD", "2.8");
			break;
		case MDMEC_2_7_1:
			uses.put("MD", "2.7.1");
			break;
		case MDMEC_2_7:
			uses.put("MD", "2.7");
			break;
		case MDMEC_2_6:
			uses.put("MD", "2.6");
			break;
		case MDMEC_2_5:
			uses.put("MD", "2.5");
			break;
		case MDMEC_2_4:
			uses.put("MD", "2.4");
			break;
		case AOD_1_0:
			uses.put("MD", "2.8");
			uses.put("MANIFEST", "1.9");
			break;
		case AOD_1_1:
			uses.put("MD", "2.9");
			uses.put("MANIFEST", "1.10");
			break;
		case AOD_1_2:
			// fake version here
			uses.put("MD", "2.11");
			uses.put("MANIFEST", "1.11");
			break;
		default:
			System.out.println("Bugger!!! Who is " + standard.label + "?");
			break;
		}
		uses.put(standard.standard.toUpperCase(), standard.ver);
		return uses;
	}

	/**
	 * Return all <tt>Namespaces</tt> required to process an XML Document based on
	 * the specified MDDF <tt>FILE_FMT</tt>. The returned <tt>Map</tt> uses the
	 * following <i>keys</i>:
	 * <ul>
	 * <li>AVAILS</li>
	 * <li>MANIFEST</li>
	 * <li>MDMEC</li>
	 * <li>MD</li>
	 * <li>AOD</li>
	 * </ul>
	 * <p>
	 * If the specified <tt>FILE_FMT</tt> identifies an XLSX representation for an
	 * Avails (e.g., <tt>AVAILS_1_8</tt>), the returned <tt>Namespaces</tt> will be
	 * those used for the corresponding XML representation (e.g.,
	 * <tt>AVAILS_1_8</tt> is converted to a <tt>AVAILS_1_8</tt> representation).
	 * </p>
	 * 
	 * @param standard
	 * @return <tt>Map&lt;String, Namespace&gt;</tt> or <tt>null</tt> if the
	 *         <tt>FILE_FMT</tt> is not supported.
	 */
	public static Map<String, Namespace> getRequiredNamespaces(FILE_FMT standard) {
		Map<String, Namespace> uses = new HashMap<String, Namespace>();
		switch (standard) {
		case AVAILS_1_9:
		case AVAILS_2_6:
			uses.put("AVAILS", Namespace.getNamespace("avails", "http://www.movielabs.com/schema/avails/v2.6/avails"));
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.10"));
			//uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.10/md"));
			//adding fake version for testing
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.11/md"));
			break;
		case AVAILS_2_5_2:
			uses.put("AVAILS", Namespace.getNamespace("avails", "http://www.movielabs.com/schema/avails/v2.5.2/avails"));
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.9"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.9/md"));
			break;
		case AVAILS_2_5:
			uses.put("AVAILS", Namespace.getNamespace("avails", "http://www.movielabs.com/schema/avails/v2.5/avails"));
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.8"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.8/md"));
			break;
		case AVAILS_1_8:
		case AVAILS_2_4:
			uses.put("AVAILS", Namespace.getNamespace("avails", "http://www.movielabs.com/schema/avails/v2.4/avails"));
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.7"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.7/md"));
			break;
		case AVAILS_1_7_3:
		case AVAILS_2_3:
			uses.put("AVAILS", Namespace.getNamespace("avails", "http://www.movielabs.com/schema/avails/v2.3/avails"));
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.6"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.6/md"));
			break;
		case AVAILS_1_7_2:
		case AVAILS_2_2_2:
			uses.put("AVAILS",
					Namespace.getNamespace("avails", "http://www.movielabs.com/schema/avails/v2.2.2/avails"));
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.5"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.5/md"));
			break;
		case AVAILS_2_2_1:
			uses.put("AVAILS", Namespace.getNamespace("avails", "http://www.movielabs.com/schema/avails/v2.2/avails"));
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.5"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.5/md"));
			break;
		case AVAILS_1_7:
		case AVAILS_2_2:
			uses.put("AVAILS", Namespace.getNamespace("avails", "http://www.movielabs.com/schema/avails/v2.2/avails"));
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.4"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.4/md"));
			break;
		case AVAILS_2_1:
			uses.put("AVAILS", Namespace.getNamespace("avails", "http://www.movielabs.com/schema/avails/v2.1/avails"));
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.4"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.4/md"));
			break;
		// .......................
		case MANIFEST_1_4:
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.4" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.3/md"));
			break;
		case MANIFEST_1_5:
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.5" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.4/md"));
			break;
		case MANIFEST_1_6:
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.6" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.5/md"));
			break;
		case MANIFEST_1_6_1:
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.6.1" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.6/md"));
			break;
		case MANIFEST_1_7:
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.7" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.6/md"));
			break;
		case MANIFEST_1_8:
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.8" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.7/md"));
			break;
		case MANIFEST_1_8_1:
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.8.1" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.7.1/md"));
			break;
		case MANIFEST_1_9:
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.9" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.8/md"));
			break;
		case MANIFEST_1_10:
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.10" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.9/md"));
			break;
		case MANIFEST_1_11:
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.11" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.10/md"));
			break;
		case MANIFEST_1_12:
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.12" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			//uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.10/md"));
			//adding fake version for testing
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.11/md"));
			break;
		case MDMEC_2_10:
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.10"));
			//uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.10/md"));
			//adding fake version for testing
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.11/md"));
			break;
		case MDMEC_2_9:
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.9"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.9/md"));
			break;
		case MDMEC_2_8:
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.8"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.8/md"));
			break;
		case MDMEC_2_7_1:
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.7.1"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.7.1/md"));
			break;
		case MDMEC_2_7:
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.7"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.7/md"));
			break;
		case MDMEC_2_6:
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.6"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.6/md"));
			break;
		case MDMEC_2_5:
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.5"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.5/md"));
			break;
		case MDMEC_2_4:
			uses.put("MDMEC", Namespace.getNamespace("mdmec", "http://www.movielabs.com/schema/mdmec/v2.4"));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.4/md"));
			break;
//			*********************************** 
		case AOD_1_2:
			uses.put("AOD", Namespace.getNamespace("delivery", "http://www.movielabs.com/schema/md/delivery/v1.2"));
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.11" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			//uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.10/md"));
			//adding fake version for testing
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.11/md"));
			break;
		case AOD_1_1:
			uses.put("AOD", Namespace.getNamespace("delivery", "http://www.movielabs.com/schema/md/delivery/v1.1"));
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.10" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.9/md"));
			break;
		case AOD_1_0:
			uses.put("AOD", Namespace.getNamespace("delivery", "http://www.movielabs.com/schema/md/delivery/v1.0"));
			uses.put("MANIFEST", Namespace.getNamespace("manifest",
					MddfContext.NSPACE_MANIFEST_PREFIX + "1.9" + MddfContext.NSPACE_MANIFEST_SUFFIX));
			uses.put("MD", Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.8/md"));
			break;
		default:
			return null;
		}
		return uses;
	}

	public static Properties getProperties() {
		if (buildProps == null) {
			buildProps = loadProperties();
		}
		return buildProps;
	}

	protected static Properties loadProperties() {
		Properties props = new Properties();
		InputStream inStream = XmlIngester.class.getResourceAsStream(PROP_PATH);
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
}
