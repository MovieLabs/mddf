/**
 * Copyright (c) 2018 MovieLabs

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
package com.movielabs.mddflib.manifest.validation.profiles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;

import com.movielabs.mddflib.logging.IssueLogger;
import com.movielabs.mddflib.util.CMValidator;
import com.movielabs.mddflib.util.xml.StructureValidation;
import com.movielabs.mddflib.util.xml.XmlIngester;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * EXPERIMENTAL CODE: The <tt>Profiler</tt> is used to try and identify the
 * use-case an MMC file is supposed to address. It does NOT validate an MDDF
 * file.
 * 
 * <pre>
 *  
	"<i>key</i>": 
	{
	    "ucid" : "<i>use-case-id</i>",
		"start": "<i>test-id</i>",
		"tests": 
		{
			"T00": 
			{
				"merge": ("AND" | "OR"),
				"constraint": 
				[
					
				],
					"result": 
				{
					"pass": "RTN:(TRUE | FALSE)",
					"fail": "GOTO:<i>id</i>"
				}
			}
		}
	}
 * </pre>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class Profiler {

	public static final String PROFILE_DIR = "profiles/";
	protected IssueLogger logger;
	protected String logMsgSrcId;
	private JSONObject usecaseSet;
	private StructureValidation structHelper;

	/**
	 * @param logger
	 * @param logMsgSrcId
	 * @param profilingRules
	 */
	public Profiler(CMValidator validator, IssueLogger logger, String logMsgSrcId, String profilingRules) {
		super();
		this.logger = logger;
		this.logMsgSrcId = logMsgSrcId;
		JSONObject ruleFile = XmlIngester.getMddfResource(PROFILE_DIR + profilingRules);
		usecaseSet = ruleFile.getJSONObject("Profiles");
		structHelper = new StructureValidation(validator, logger, logMsgSrcId);
	}

	/**
	 * Identify the <i>use cases</i> an Manifest seems to be addressing.
	 * 
	 * @param rootEl
	 * @return
	 */
	public List<String> evaluate(Element rootEl) {
		List<String> matches = new ArrayList<String>();
		Iterator<String> keys = usecaseSet.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject useCaseDef = usecaseSet.getJSONObject(key);
			if (matches(rootEl, useCaseDef)) {
				matches.add(useCaseDef.getString("ucid"));
			}
		}
		return matches;
	}

	/**
	 * @param targetEl
	 * @param useCaseDef
	 * @return
	 */
	private boolean matches(Element targetEl, JSONObject useCaseDef) {
		String firstTestId = useCaseDef.getString("start");
		JSONObject testSet = useCaseDef.getJSONObject("tests");
		JSONObject test = testSet.getJSONObject(firstTestId);
		Set<String> loopDetector = new HashSet<String>();
		loopDetector.add(firstTestId);
		boolean done = false;
		String testSeq = firstTestId;
		while (!done) {
			String mergeOp = test.optString("merge", "AND");
			boolean mergeAnd = mergeOp.equals("AND");
			/*
			 * if AND'ing then start condition should be TRUE. If OR'ing then it should be
			 * FALSE.
			 * 
			 */
			boolean passesAll = mergeAnd;
			JSONArray constraintSet = test.getJSONArray("constraint");
			evalBlock: for (int i = 0; i < constraintSet.size(); i++) {
				JSONObject constraint = constraintSet.getJSONObject(i);
				boolean passes = structHelper.evaluateConstraint(targetEl, constraint);
				if (mergeAnd) {
					passesAll = passesAll && passes;
					if (!passesAll) {
						break evalBlock;
					}
				} else {
					passesAll = passesAll || passes;
				}
			}
			JSONObject resultOptions = test.getJSONObject("result");
			/* is there a next test to perform or are we done? */
			String result = null;
			if (passesAll) {
				result = resultOptions.getString("pass");
			} else {
				result = resultOptions.getString("fail");
			}
			if (result.startsWith("RTN")) {
				return (result.endsWith(":true"));
			}
			// must be a GOTO
			String nextTestId = result.split(":")[1];
			test = testSet.getJSONObject(nextTestId);
			testSeq = testSeq + "; " + nextTestId;
			/*
			 * The decision tree defined for evaluation must be DAG. Terminate the
			 * processing if the logic results in a loop back to a previously performed
			 * test.
			 */
			if (loopDetector.contains(nextTestId)) {
				throw new RuntimeException("Loop in test sequence: " + testSeq);
			}
			loopDetector.add(nextTestId);
		}

		return false;
	}

}
