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
package com.movielabs.mddflib.util.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddf.MddfContext.FILE_FMT;
import com.movielabs.mddflib.logging.IssueLogger;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;
import com.movielabs.mddflib.util.CMValidator;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * A 'helper' class that supports the validation of an XML file against a set of
 * structural requirements not specified via an XSD.
 * <p>
 * The MDDF specifications define many requirements for specific use cases as
 * well as recommended 'best practices'. These requirements and recommendations
 * specify relationships between XML elements that are not defined via the XSD.
 * In order to support validation they are instead formally specified via a
 * JSON-formatted <i>structure definition</i> file. The
 * <tt>StructureValidation</tt> class provides the functions that can interpret
 * the requirements and test an XML file for compliance.
 * </p>
 * <h3><u>Semantics and Syntax:</u></h3>
 * <p>
 * The semantics of JSON a structure definition is as follows:
 * 
 * <pre>
 * {
 * 	  <i>USAGE</i>:
 * 		 {
 * 			"targetPath" : <i>XPATH</i> (optional)
 * 			"constraint" :
 * 			[
 * 				{
 				  "<i>VARIABLE ID</i>": <i>XPATH</i>, (optional)
				  "min": <i>INTEGER</i>, (optional)
				  "max": <i>INTEGER</i>, (optional)
				  "xpath": <i>(XPATH | ARRAY[XPATH])</i>,
				  "filter" : (optional)
				  	{
				  		 "values":  ARRAY[ <i>STRING</i>],
				  		 "negated" : <i>("true" | "false")</i> (optional)
				  	}
				  "severity": <i>("Fatal" | "Error" | "Warning" | "Notice")</i>,
				  "msg" : <i>STRING</i>,  (optional)
				  "docRef": <i>STRING</i> (optional)
 * 				}
 * 			]
 * 			"children": {.... } (optional)
 * 		 }
 * }
 * </pre>
 * 
 * where:
 * <ul>
 * <li><i>USAGE</i> is a string defining the key used by a validator to retrieve
 * the requirements appropriate to a use case.</li>
 * <li><tt>targetPath</tt>: indicates the element(s) that provide the evaluation
 * context for the constraint xpath when invoking the
 * <tt>validateDocStructure()</tt> method. If not specified, a target element
 * must be provided when invoking the <tt>validateConstraint()</tt> method on a
 * target element that has been identified by other means.</li>
 * <li><tt>constraint</tt>: one or more structural requirements associated with
 * the targeted element.
 * <ul>
 * <li><tt><i>VARIABLE-ID</i></tt>: Variables are denoted by a "$" followed by
 * an ID (e.g., $FOO) and are assigned a value via a XPath.</li>
 * <li><tt>xpath</tt>: defines one or more xpaths relative to the target element
 * that, when evaluated, the number of matching elements satisfy the min/max
 * cardinality constraints. If multiple xpaths are listed, the total number of
 * elements (or attributes) returned when each is separately evaluated must
 * satisfy the constraint.</li>
 * <li><tt>filter</tt>: supplemental condition applied to results returned when
 * XPath is evaluated.</li>
 * <li><tt>min</tt>: minimum number of matching objects that should be found
 * when evaluating the xpath(s). [OPTIONAL, default is 0]</li>
 * <li><tt>max</tt>: maximum number of matching objects that should be found
 * when evaluating the xpath(s). [OPTIONAL, default is unlimited]</li>
 * <li><tt>severity</tt>: must match one of the <tt>LogMgmt.level</tt> values
 * </li>
 * <li><tt>msg</tt>: text to use for a log entry if the constraint is not met.
 * If not provided, a generic message is used.</li>
 * <li><tt>docRef</tt>: is a value usable by <tt>LogReference</tt> that will
 * indicate any reference material documenting the requirement.</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 * 
 * <h3><u>XPaths:</u></h3>
 * <p>
 * An <tt>XPATH</tt> is defined using the standard XPath syntax with two
 * modifications.
 * </p>
 * <h4>Indicating Namespaces:</h4>
 * <p>
 * Namespaces are indicated using a variable with the name of an MDDF schema.
 * The appropriate namespace prefixes will be inserted by the software.
 * Supported namespaces are:
 * </p>
 * <ul>
 * <li>{avail}</li>
 * <li>{mdmec}</li>
 * <li>{manifest}</li>
 * <li>{md}</li>
 * </ul>
 * For example:
 * 
 * <pre>
 * <tt>
		"POEST": 
		{
			"targetPath": ".//{avail}LicenseType[.='POEST']",
			"constraint": 
			[
				{ 
					"min": "1",
					"max": "1",
					"xpath": "../{avail}Term[@termName='SuppressionLiftDate']",
					"severity": "Error",
					"msg": "One SuppressionLiftDate is required for LicenseType 'POEST'"
				}
			]
		},
		
		"WorkType-Episode": 
		{
			"constraint": 
			[
				{
					"docRef": "AVAIL:avail00n",
					"min": "1",
					"max": "2",
					"xpath": 
					[
						"{avail}EpisodeMetadata/{avail}AltIdentifier",
						"{avail}EpisodeMetadata/{avail}EditEIDR-URN"
					]
				}
			]
		},
 *</tt>
 * </pre>
 * 
 * <h4>Support for MEC File Usage:</h4>
 * <p>
 * A Manifest file may provide metadata in one of two ways: either
 * <i>internally</i> via a <tt>&lt;BasicMetadata&gt;</tt> element or
 * <i>externally</i> via a <tt>&lt;ContainerReference&gt;</tt> pointing to a MEC
 * file. Validating a constraint on Metadata may, therefore, require specifying
 * two different XPaths to cover both possible situations.
 * </p>
 * <p>
 * To handle this type of situation, a JSONArray with both paths is used. The
 * xpath that is to be applied when a MEC file is used will be prefixed with the
 * keyword <tt>{$$MEC}</tt>. For example:
 * 
 * <pre>
 * <tt> 
"$CID": "./{manifest}ContentID",
"max": "0",
"severity": "Error",
"xpath": 
[
		"//{manifest}BasicMetadata[(descendant::{md}ArtReference) and (@ContentID = {$CID}) ]",						
		"{$$MEC}//{mdmec}Basic[(descendant::{md}ArtReference) and (@ContentID = {$CID}) ]"
],
 * </tt>
 * </pre>
 * </p>
 * 
 * <h3><u>Filters:</u></h3>
 * <p>
 * A 'Filter' may be used to supplement the matching criteria specified by the
 * XPaths. This is used when XPath criteria are insufficient, or too unwieldy,
 * to fully implement a constraint. Filters are (for now) defined as a set of
 * values and an optional 'negated' flag set to <tt>true</tt> or <tt>false</tt>.
 * </p>
 * <p>
 * For example, the following filter would identify <tt>Audio</tt> assets that
 * have a <tt>ChannelMapping</tt> that is inconsistent with the number of actual
 * channels:
 * </p>
 * 
 * <pre>
		"MultiChannel": 
		{
			"targetPath": ".//{md}Channels[. &lt; 1]",
			"constraint": 
			[
				{
					"xpath": 
					[
						"../{md}Encoding/{md}ChannelMapping"
					],

					"filter": 
					{
						"values": 
						[
							"Mono",
							"Left",
							"Center",
							"Right"
						],

						"negated": "false"
					},

					"max": "0",
					"severity": "Error",
					"msg": "ChannelMapping is not valid for multi-channel Audio"
				}
			]
		}
 * </pre>
 * 
 * <h3><u>Variables:</u></h3>
 * 
 * Variables are denoted by a "$" followed by an ID (e.g., <tt>$FOO</tt>) and
 * are assigned a value via a XPath. For example: <tt><pre>
 *   "$CID": "./@ContentID"
 * </pre></tt> They may be included in the constraint's XPath by using enclosing
 * the variable name in curly brackets. For example: <tt><pre>
 *   "xpath": 
 *      [
 *         "../..//{manifest}Experience[@ExperienceID={$CID}]/{manifest}PictureGroupID"
 *      ],
 * </pre></tt>
 * <p>
 * It is possible that the XPath used to determine a variable's value will
 * evaluate to a <tt>null</tt>. Any constraint XPath that references a null
 * variable will be skipped when evaluating the constraint criteria.
 * </p>
 * 
 * 
 * <h3><u>Nested Requirements:</u></h3>
 * <p>
 * Requirements can be defined in a way that reflects the nested structure of
 * the underlying XML. For example, assume the XSD specifies the following
 * syntax:
 * </p>
 * <ul>
 * <li>Foo:
 * <ul>
 * <li>Bar:
 * <ul>
 * <li>Flavor</li>
 * <li>Weight</li>
 * <li>Calories</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * We wish to specify that <i>if</i> Flavor is present then Calories must also
 * be specified. There are two options:
 * <ul>
 * <li>use a <tt>targetPath</tt> pointing to <tt>Foo</tt> and constraints with
 * <tt>xpath</tt> that will resolve when <tt>Foo</tt> is the root for xpath
 * evaluation (e.g., "xpath": "./Foo/Calories")</li>
 * <li>the <tt>targetPath</tt> points to <tt>Foo</tt>, followed by a
 * <i>child</i> constraint with the <tt>targetPath</tt> pointing to <tt>Bar</tt>
 * and an <tt>xpath</tt> that will resolve when <tt>Bar</tt> is the root (e.g.,
 * "xpath": "./Calories")</li>
 * </ul>
 * <p>
 * Either option will work but in some situations one or the other may be more
 * efficient to evaluate or easier to write.
 * </p>
 * <h3><u>Usage</u></h3>
 * <p>
 * Validation modules should determine the appropriate JSON resource file based
 * on the type and version of the MDDF file. Requirements may then be retrieved
 * and individually checked using the USAGE key or the entire collection may be
 * iterated thru.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class StructureValidation {

	private static final String KEY_MEC_REF = "{$$MEC}";
	protected IssueLogger logger;
	protected String logMsgSrcId;

	/**
	 * @param logger
	 * @param logMsgSrcId
	 */
	public StructureValidation(IssueLogger logger, String logMsgSrcId) {
		this.logger = logger;
	}

	/**
	 * Check to see if the XML satisfies the specified requirement. The
	 * <tt>rootEL</tt> may either be the root of an entire document or the root of a
	 * DOM tree forming a sub-tree within the overall document.
	 * <p>
	 * Any errors or warnings detected will be reported via the logger. The return
	 * status will indicate to caller if an error was detected.
	 * </p>
	 * 
	 * @param rootEl
	 * @param rqmt
	 * @return
	 */
	public boolean validateDocStructure(Element rootEl, JSONObject rqmt) {
		return validateDocStructure(rootEl, rqmt, null);
	}

	/**
	 * Check to see if the XML satisfies the specified requirement. The
	 * <tt>rootEL</tt> may either be the root of an entire document or the root of a
	 * DOM tree forming a sub-tree within the overall document. A requirement's
	 * <tt>constraint</tt> may include an <tt>XPath</tt> that is to be applied to
	 * the supporting MEC files.
	 * <p>
	 * Any errors or warnings detected will be reported via the logger. The return
	 * status will indicate to caller if an error was detected.
	 * </p>
	 * 
	 * @param rootEl
	 * @param rqmt
	 * @param supportingMECs
	 * @return
	 */
	public boolean validateDocStructure(Element rootEl, JSONObject rqmt, List<MddfTarget> supportingMECs) {
		String rootPath = rqmt.getString("targetPath");
		FILE_FMT mddfFmt = MddfContext.identifyMddfFormat(rootEl);
		XPathExpression<?> xpExp = resolveXPath(rootPath, null, mddfFmt);
		List<Element> targetElList = (List<Element>) xpExp.evaluate(rootEl);
		boolean isOk = true;
		if (rqmt.containsKey("constraint")) {
			JSONArray constraintSet = rqmt.getJSONArray("constraint");
			for (Element nextTargetEl : targetElList) {
				for (int i = 0; i < constraintSet.size(); i++) {
					JSONObject constraint = constraintSet.getJSONObject(i);
					isOk = evaluateConstraint(nextTargetEl, constraint, supportingMECs) && isOk;
				}
			}
		}
		/* are there nested constraints?? */
		if (rqmt.containsKey("children")) {
			JSONObject embeddedReqmts = rqmt.getJSONObject("children");
			for (Element nextTargetEl : targetElList) {
				Iterator<String> embeddedKeys = embeddedReqmts.keys();
				while (embeddedKeys.hasNext()) {
					String key = embeddedKeys.next();
					JSONObject childRqmtSpec = embeddedReqmts.getJSONObject(key);
					// NOTE: This block of code requires a 'targetPath' be defined
					if (childRqmtSpec.has("targetPath")) {
						// Recursive descent...
						isOk = validateDocStructure(nextTargetEl, childRqmtSpec) && isOk;
					}
				}
			}
		}
		return isOk;
	}

	/**
	 * Evaluate a constraint in the context of a specific Element. This means
	 * descendant Elements are not considered.
	 * <p>
	 * <b>Note:</b> This method is exposed as <tt>public</tt> to support unit
	 * testing. Use of <tt>validateDocStructure()</tt> is preferred.
	 * 
	 * @param target
	 * @param constraint
	 * @param supportingMECs
	 * @return
	 */
	public boolean evaluateConstraint(Element target, JSONObject constraint, List<MddfTarget> supportingMECs) {
		boolean passes = true;
		Map<String, String> varMap = resolveVariables(target, constraint);
		int min = constraint.optInt("min", 0);
		int max = constraint.optInt("max", -1);
		String severity = constraint.optString("severity", "Error");
		int logLevel = LogMgmt.text2Level(severity);

		String docRef = constraint.optString("docRef");

		Object xpaths = constraint.opt("xpath");
		List<XPathExpression<?>> xpeList = new ArrayList<XPathExpression<?>>();
		List<String> externalPathList = new ArrayList<String>();
		String targetList = ""; // for use if error msg is required
		String[] xpParts = null;
		if (xpaths instanceof String) {
			String xpathDef = (String) xpaths;
			if (xpathDef.startsWith(KEY_MEC_REF)) {
				xpathDef = xpathDef.replace(KEY_MEC_REF, "");
				externalPathList.add(xpathDef);
			} else {
				xpeList.add(resolveXPath(xpathDef, varMap, target));
			}
			xpParts = xpathDef.split("\\[");
			targetList = xpParts[0];
		} else if (xpaths instanceof JSONArray) {
			JSONArray xpArray = (JSONArray) xpaths;
			for (int i = 0; i < xpArray.size(); i++) {
				String xpathDef = xpArray.getString(i);
				if (xpathDef.startsWith(KEY_MEC_REF)) {
					xpathDef = xpathDef.replace(KEY_MEC_REF, "");
					externalPathList.add(xpathDef);
				} else {
					xpeList.add(resolveXPath(xpathDef, varMap, target));
				}
				xpParts = xpathDef.split("\\[");
				if (i < 1) {
					targetList = xpParts[0];
				} else if (i == (xpArray.size() - 1)) {
					targetList = targetList + ", or " + xpParts[0];
				} else {
					targetList = targetList + ", " + xpParts[0];
				}
			}
		}
		// reformat targetList for use in log msgs....
		targetList = targetList.replaceAll("\\{\\w+\\}", "");

		/* Has an OPTIONAL filter been included with the constraint? */
		JSONObject filterDef = constraint.optJSONObject("filter");

		List<Object> matchedElList = new ArrayList<Object>();
		/*
		 * The xpeLlist contains constraints that are based on an XPath that should be
		 * applied to the SAME file that contains the 'target' element (i.e., the
		 * primary MDDF file being evaluated).
		 */
		for (int i = 0; i < xpeList.size(); i++) {
			XPathExpression<Element> xpExp = (XPathExpression<Element>) xpeList.get(i);
			List<?> nextElList = xpExp.evaluate(target);
			if (filterDef != null) {
				nextElList = applyFilter(nextElList, filterDef);
			}
			matchedElList.addAll(nextElList);
		}
		/*
		 * The externalPathList contains constraints that are based on an XPath that
		 * should be applied to a MEC file providing Metadata in support of the primary
		 * MDDF file being evaluated). This list contains Strings rather than resolved
		 * xpath expressions (a.k.a 'xpe'). The reason for this is that the xpath can
		 * not be resolved to an xpe without knowing the namespaces used by the MEC file
		 * it is being applied to.
		 */
		if ((supportingMECs != null) && (!supportingMECs.isEmpty())) {
			for (int i = 0; i < externalPathList.size(); i++) {
				String xpath = externalPathList.get(i);
				for (MddfTarget targetSrc : supportingMECs) {
					Element rootEl = targetSrc.getXmlDoc().getRootElement();
					XPathExpression<?> xpExp = resolveXPath(xpath, varMap, rootEl);
					List<?> nextElList = xpExp.evaluate(rootEl);
					if (filterDef != null) {
						nextElList = applyFilter(nextElList, filterDef);
					}
					matchedElList.addAll(nextElList);
				}
			}
		}

		String logMsg = constraint.optString("msg", "");
		String details = constraint.optString("details", "");

		// check cardinality
		int count = matchedElList.size();
		if (min > 0 && (count < min)) {
			String elName = target.getName();
			String msg;
			if (logMsg.isEmpty()) {
				msg = "Invalid " + elName + " structure: missing " + targetList + " elements";
			} else {
				msg = logMsg;
			}
			if (details.isEmpty()) {
				details = elName + " requires minimum of " + min + " " + targetList + " elements";
				if (xpParts.length > 1) {
					details = details + " matching the criteria [" + xpParts[1];
				}
			}
			LogReference srcRef = resolveDocRef(docRef);
			logger.logIssue(LogMgmt.TAG_MD, logLevel, target, msg, details, srcRef, logMsgSrcId);
			if (logLevel > LogMgmt.LEV_WARN) {
				passes = false;
			}
		}
		if (max > -1 && (count > max)) {
			String elName = target.getName();
			String msg;
			if (logMsg.isEmpty()) {
				msg = "Invalid " + elName + " structure: too many child " + targetList + "elements";
			} else {
				msg = logMsg;
			}
			if (details.isEmpty()) {
				details = elName + " permits maximum of " + max + "  " + targetList + " elements";
			}
			LogReference srcRef = resolveDocRef(docRef);
			logger.logIssue(LogMgmt.TAG_MD, logLevel, target, msg, details, srcRef, logMsgSrcId);

			if (logLevel > LogMgmt.LEV_WARN) {
				passes = false;
			}
		}

		return passes;
	}

	/**
	 * Assign values to any <i>variables</i> used by a <tt>constraint</tt>.
	 * Variables are denoted by a "$" followed by an ID (e.g., <tt>$FOO</tt>) and
	 * are assigned a value via a XPath. For example: <tt><pre>
	 *   "$CID": "./@ContentID"
	 * </pre></tt> They may be included in the constraint's XPath by using enclosing
	 * the variable name in curly brackets. For example: <tt><pre>
	 *   "xpath": 
	 *      [
	 *         "../..//{manifest}Experience[@ExperienceID={$CID}]/{manifest}PictureGroupID"
	 *      ],
	 * </pre></tt>
	 * <p>
	 * It is possible that the XPath used to determine a variable's value will
	 * evaluate to a <tt>null</tt>. Any constraint XPath that references a null
	 * variable will be skipped when evaluating the constraint criteria.
	 * </p>
	 * 
	 * @param target
	 * @param constraint
	 * @return variable assignments as key/value entries in a Map
	 */
	private Map<String, String> resolveVariables(Element target, JSONObject constraint) {
		Map<String, String> varMap = new HashMap<String, String>();
		Set<String> keySet = constraint.keySet();
		for (String key : keySet) {
			if (key.startsWith("$")) {
				String xpath = constraint.getString(key);
				XPathExpression<?> xpe = resolveXPath(xpath, null, target);
				String value = null;
				if (resolvesToAttribute(xpath)) {
					Attribute varSrc = (Attribute) xpe.evaluateFirst(target);
					if (varSrc != null) {
						value = varSrc.getValue();
					}
				} else {
					Element varSrc = (Element) xpe.evaluateFirst(target);
					if (varSrc != null) {
						value = varSrc.getTextNormalize();
					}
				}
				varMap.put(key, value);
				String msg = "resolveVariables(): Var " + key + "=" + value;
				logger.logIssue(LogMgmt.TAG_MD, LogMgmt.LEV_DEBUG, target, msg, xpath, null, logMsgSrcId);

			}
		}
		return varMap;
	}

	/**
	 * Apply a filter to a list of Elements and/ro Attributes. This is used when
	 * XPath criteria are insufficient, or too unwieldy, to fully implement a
	 * constraint.
	 * <p>
	 * Filters are (for now) defined as a set of values and an optional 'negated'
	 * flag set to <tt>true</tt> or <tt>false</tt>
	 * </p>
	 * <b>NOTE:</b> future implementations may include support for REGEX pattern
	 * matching.
	 * 
	 * @param elList    of <tt>Elements</tt> and/or <tt>Attributes</tt>
	 * @param filterDef
	 * @return
	 */
	private List<Object> applyFilter(List<?> inList, JSONObject filterDef) {
		List<Object> outList = new ArrayList();
		JSONArray valueSet = filterDef.optJSONArray("values");
		String negated = filterDef.optString("negated", "false");
		boolean isNegated = negated.equals("true");
		for (Object nextEl : inList) {
			String value;
			if (nextEl instanceof Element) {
				value = ((Element) nextEl).getValue();
			} else {
				Attribute nextAt = (Attribute) nextEl;
				value = nextAt.getValue();
			}
			if (valueSet.contains(value)) {
				if (!isNegated) {
					outList.add(nextEl);
				}
			} else {
				if (isNegated) {
					outList.add(nextEl);
				}
			}
		}
		return outList;
	}

	/**
	 * Create a <tt>XPathExpression</tt> from a string representation. An
	 * <tt>xpathDef</tt> is defined using the standard XPath syntax with one
	 * modification. Namespaces are indicated using a variable indicating the name
	 * of an MDDF schema. The appropriate namespace prefixes will be inserted by the
	 * software. Supported namespaces are:
	 * <ul>
	 * <li>{avail}</li>
	 * <li>{mdmec}</li>
	 * <li>{manifest}</li>
	 * <li>{md}</li>
	 * </ul>
	 * <p>
	 * The specific version-dependent <tt>Namespace</tt> instances used when
	 * compiling the <tt>XPathExpression</tt> will be determined based on the
	 * <tt>Namespaces</tt> used by the <tt>Document</tt> containing the
	 * <tt>target</tt> Element argument. The returned XPathExpression may not,
	 * therefore, evaluate properly when applied to a different Document.
	 * </p>
	 * 
	 * @param xpath
	 * @param varMap
	 * @param target
	 * @return
	 */
	public XPathExpression<?> resolveXPath(String xpath, Map<String, String> varMap, Element target) {
		Element rootEl = target.getDocument().getRootElement();
		FILE_FMT mddfFmt = MddfContext.identifyMddfFormat(rootEl);
		return resolveXPath(xpath, varMap, mddfFmt);
	}

	/**
	 * Create a <tt>XPathExpression</tt> from a string representation. An
	 * <tt>xpathDef</tt> is defined using the standard XPath syntax with one
	 * modification. Namespaces are indicated using a variable indicating the name
	 * of an MDDF schema. The appropriate namespace prefixes will be inserted by the
	 * software. Supported namespaces are:
	 * <ul>
	 * <li>{avail}</li>
	 * <li>{mdmec}</li>
	 * <li>{manifest}</li>
	 * <li>{md}</li>
	 * </ul>
	 * <p>
	 * The specific version-dependent <tt>Namespace</tt> instances used when
	 * compiling the <tt>XPathExpression</tt> will be determined based on the value
	 * of the <tt>targetMddfFmt</tt> argument. The returned XPathExpression may not,
	 * therefore, evaluate properly when applied to a different Document with a
	 * <tt>FILE_FMT</tt>
	 * </p>
	 * 
	 * @param xpathDef
	 * @param varMap
	 * @param targetMddfFmt
	 * @return
	 */
	private XPathExpression<?> resolveXPath(String xpathDef, Map<String, String> varMap, FILE_FMT targetMddfFmt) {
		if (varMap != null) {
			Set<String> keySet = varMap.keySet();
			for (String varID : keySet) {
				String varValue = varMap.get(varID);
				String regExp = "\\{\\" + varID + "\\}";
				xpathDef = xpathDef.replaceAll(regExp, "'" + varValue + "'");
			}
		}
		Map<String, Namespace> uses = MddfContext.getRequiredNamespaces(targetMddfFmt);

		if (xpathDef.contains("{md}")) {
			xpathDef = xpathDef.replaceAll("\\{md\\}", uses.get("MD").getPrefix() + ":");
		}
		if (xpathDef.contains("{avail}")) {
			xpathDef = xpathDef.replaceAll("\\{avail\\}", uses.get("AVAILS").getPrefix() + ":");
		}
		if (xpathDef.contains("{manifest}")) {
			xpathDef = xpathDef.replaceAll("\\{manifest\\}", uses.get("MANIFEST").getPrefix() + ":");
		}
		if (xpathDef.contains("{mdmec}")) {
			xpathDef = xpathDef.replaceAll("\\{mdmec\\}", uses.get("MDMEC").getPrefix() + ":");
		}
		// Now compile the XPath
		XPathExpression<?> xpExpression;
		/**
		 * The following are examples of xpaths that return an attribute value and that
		 * we therefore need to identity:
		 * <ul>
		 * <li>avail:Term/@termName</li>
		 * <li>avail:Term[@termName[.='Tier' or .='WSP' or .='DMRP']</li>
		 * <li>@contentID</li>
		 * </ul>
		 * whereas the following should NOT match:
		 * <ul>
		 * <li>avail:Term/avail:Event[../@termName='AnnounceDate']</li>
		 * </ul>
		 */
		XPathFactory xpfac = XPathFactory.instance();
		Set<Namespace> nspaceSet = new HashSet<Namespace>();
		nspaceSet.addAll(uses.values());
		if (resolvesToAttribute(xpathDef)) {
			// must be an attribute value we're after..
			xpExpression = xpfac.compile(xpathDef, Filters.attribute(), null, nspaceSet);
		} else {
			xpExpression = xpfac.compile(xpathDef, Filters.element(), null, nspaceSet);
		}
		return xpExpression;
	}

	private static boolean resolvesToAttribute(String xpathDef) {
		return xpathDef.matches(".*/@[\\w]++(\\[.+\\])?");
	}

	private LogReference resolveDocRef(String docRef) {
		String docStandard = null;
		String docSection = null;
		LogReference srcRef = null;
		if (docRef != null && !docRef.isEmpty()) {
			String[] parts = docRef.split(":");
			if (parts.length >= 2) {
				docStandard = parts[0];
				docSection = parts[1];
				srcRef = LogReference.getRef(docStandard, docSection);
			}
		}
		return srcRef;
	}
}
