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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.movielabs.mddflib.logging.IssueLogger;
import com.movielabs.mddflib.logging.LogMgmt;
import com.movielabs.mddflib.logging.LogReference;

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
 * <h3>Semantics and File Structure:</h3>
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
				  "min": <i>INTEGER</i>,
				  "max": <i>INTEGER</i>,
				  "xpath": <i>(XPATH | ARRAY[XPATH])</i>,
				  "severity": <i>("Fatal" | "Error" | "Warning" | "Notice")</i>,
				  "msg" : <i>STRING</i>,  (optional)
				  "docRef": <i>STRING</i> (optional)
 * 				}
 * 			]
 * 		 }
 * }
 * </pre>
 * 
 * where:
 * <ul>
 * <li><i>USAGE</i> is a string defining the key used by a validator to retrieve
 * the requirements appropriate to a use case.</li>
 * <li>"targetPath" indicates the element(s) that provide the evaluation context
 * for the constraint xpath when invoking the <tt>validateDocStructure()</tt>
 * method. If not specified, a target element must be provided when invoking the
 * <tt>validateConstraint()</tt> method on a target element that has been
 * identified by other means.</li>
 * <li>"xpath" defines one or more xpaths relative to the target element that,
 * when evaluated, the number of matching elements satisfy the min/max
 * cardinality constraints. If multiple xpaths are listed, the total number of
 * elements returned when each is separately evaluated must satisfy the
 * constraint.</li>
 * <li>"severity" must match one of the <tt>LogMgmt.level</tt> values</li>
 * <li>"msg" is the text to use for a log entry if the constraint is not met. If
 * not provided, a generic message is used.</li>
 * <li>"docRef" is a value usable by <tt>LogReference</tt> that will indicate
 * any reference material documenting the requirement.</li>
 * </ul>
 * </p>
 * 
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
 * <h3>Usage:
 * <h3>
 * <p>
 * Validation modules should retrieve a set of requirements using a <i>USAGE</i>
 * value.
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class StructureValidation {

	private IssueLogger logger;
	protected String logMsgSrcId;

	private XPathFactory xpfac = XPathFactory.instance();

	public StructureValidation(IssueLogger logger, String logMsgSrcId) {
		this.logger = logger;
	}

	public boolean validateDocStructure(Element rootEl, JSONObject rqmt) {
		String rootPath = rqmt.getString("targetPath");
		XPathExpression<Element> xpExp = resolveXPath(rootPath);
		List<Element> targetElList = xpExp.evaluate(rootEl);
		JSONArray constraintSet = rqmt.getJSONArray("constraint");
		boolean isOk = true;
		for (Element nextTargetEl : targetElList) {
			for (int i = 0; i < constraintSet.size(); i++) {
				JSONObject constraint = constraintSet.getJSONObject(i);
				isOk = validateConstraint(nextTargetEl, constraint) && isOk;
			}
		}
		return isOk;
	}

	public boolean validateConstraint(Element target, JSONObject constraint) {
		boolean curFileIsValid = true;

		int min = constraint.optInt("min", 0);
		int max = constraint.optInt("max", -1);
		String docRef = constraint.optString("docRef");

		Object xpaths = constraint.opt("xpath");
		List<XPathExpression<Element>> xpeList = new ArrayList<XPathExpression<Element>>();
		String targetList = ""; // for use if error msg is required
		String[] xpParts = null;
		if (xpaths instanceof String) {
			String xpathDef = (String) xpaths;
			xpeList.add(resolveXPath(xpathDef));
			xpParts = xpathDef.split("\\[");
			targetList = xpParts[0];
		} else if (xpaths instanceof JSONArray) {
			JSONArray xpArray = (JSONArray) xpaths;
			for (int i = 0; i < xpArray.size(); i++) {
				String xpathDef = xpArray.getString(i);
				xpeList.add(resolveXPath(xpathDef));
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
		targetList = targetList.replaceAll("\\{\\w+\\}", "");

		List<Element> matchedElList = new ArrayList<Element>();
		for (int i = 0; i < xpeList.size(); i++) {
			XPathExpression<Element> xpExp = xpeList.get(i);
			List<Element> nextElList = xpExp.evaluate(target);
			matchedElList.addAll(nextElList);
		}
		String logMsg = constraint.optString("msg", "");

		// check cardinality
		int count = matchedElList.size();
		if (min > 0 && (count < min)) {
			String elName = target.getName();
			String msg;
			if (logMsg.isEmpty()) {
				msg = "Invalid " + elName + " structure: missing elements";
			} else {
				msg = logMsg;
			}
			String explanation = elName + " requires minimum of " + min + " " + targetList + " elements";
			if (xpParts.length > 1) {
				explanation = explanation + " matching the criteria [" + xpParts[1];
			}
			LogReference srcRef = resolveDocRef(docRef);
			logger.logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, target, msg, explanation, srcRef, logMsgSrcId);
			curFileIsValid = false;
		}
		if (max > -1 && (count > max)) {
			String elName = target.getName();
			String msg;
			if (logMsg.isEmpty()) {
				msg = "Invalid " + elName + " structure: too many child elements";
			} else {
				msg = logMsg;
			}
			String explanation = elName + " permits maximum of " + max + "  " + targetList + " elements";
			LogReference srcRef = resolveDocRef(docRef);
			logger.logIssue(LogMgmt.TAG_AVAIL, LogMgmt.LEV_ERR, target, msg, explanation, srcRef, logMsgSrcId);
			curFileIsValid = false;
		}

		return curFileIsValid;
	}

	private XPathExpression<Element> resolveXPath(String xpathDef) {
		Set<Namespace> nspaceSet = new HashSet<Namespace>();

		/*
		 * replace namespace placeholders with actual prefix being used
		 */
		if (xpathDef.contains("{md}")) {
			xpathDef = xpathDef.replaceAll("\\{md\\}", XmlIngester.mdNSpace.getPrefix() + ":");
			nspaceSet.add(XmlIngester.mdNSpace);
		}

		if (xpathDef.contains("{avail}")) {
			xpathDef = xpathDef.replaceAll("\\{avail\\}", XmlIngester.availsNSpace.getPrefix() + ":");
			nspaceSet.add(XmlIngester.availsNSpace);
		}

		if (xpathDef.contains("{manifest}")) {
			xpathDef = xpathDef.replaceAll("\\{manifest\\}", XmlIngester.manifestNSpace.getPrefix() + ":");
			nspaceSet.add(XmlIngester.manifestNSpace);
		}

		if (xpathDef.contains("{mdmec}")) {
			xpathDef = xpathDef.replaceAll("\\{mdmec\\}", XmlIngester.mdmecNSpace.getPrefix() + ":");
			nspaceSet.add(XmlIngester.mdmecNSpace);
		}
		// Now compile the XPath
		XPathExpression xpExpression;
		/**
		 * The following are examples of xpaths that return an attribute value
		 * and that we therefore need to identity:
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
		if (xpathDef.matches(".*/@[\\w]++(\\[.+\\])?")) {
			// must be an attribute value we're after..
			xpExpression = xpfac.compile(xpathDef, Filters.attribute(), null, nspaceSet);
		} else {
			xpExpression = xpfac.compile(xpathDef, Filters.element(), null, nspaceSet);
		}
		return xpExpression;
	}

	private LogReference resolveDocRef(String docRef) {
		String docStandard = null;
		String docSection = null;
		LogReference srcRef = null;
		if (docRef != null && !docRef.isEmpty()) {
			String[] parts = docRef.split(":");
			docStandard = parts[0];
			docSection = parts[1];
			srcRef = LogReference.getRef(docStandard, docSection);
		}
		return srcRef;
	}
}
