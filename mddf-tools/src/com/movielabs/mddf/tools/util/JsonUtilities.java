/**
 * Created Sep 19, 2015 
 *
 * Copyright Motion Picture Laboratories, Inc. 2015
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
package com.movielabs.mddf.tools.util;

import java.text.DecimalFormat;

import net.sf.json.*;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class JsonUtilities {

	/**
	 * Return the child node that has the specified property value. If no child
	 * matching the criteria is found a <tt>null</tt> value is returned.
	 * 
	 * @param parent
	 * @param childName
	 * @param propertyName
	 * @param propertyVal
	 * @return
	 */
	public static JSONObject findByAttribute(JSONObject parent,
			String childName, String propertyName, String propertyVal) {
		JSONArray target = asArray(parent, childName);
		for (int i = 0; i < target.size(); i++) {
			JSONObject entry = target.optJSONObject(i);
			String testVal = entry.optString(propertyName);
			if (testVal != null && testVal.equals(propertyVal)) {
				return entry;
			}
		}
		return null;

	}

	/**
	 * Returns an JSONArray regardless of the cardinality of matching children.
	 * If no child is found then a <tt>null</tt> value is returned.
	 * 
	 * <p>
	 * This function addresses an issue resulting from the conversion of XML
	 * into JSON. If only one child of an element is defined the conversion
	 * results in a JSON parent this is an associative array. Use of this
	 * function allows a consistent interface regardless of cardinality.
	 * </p>
	 */
	public static JSONArray asArray(JSONObject parent, String childName) {
		Object target = parent.opt(childName);
		if (target instanceof JSONArray) {
			return (JSONArray) target;
		} else {
			JSONArray results = new JSONArray();
			if (target != null) {
				results.add(target);
				return results;
			} else {
				return null;
			}
		}
	}

	public static String getContainerLoc(JSONObject target) {
		try {
			JSONObject outter = target.getJSONObject("ContainerReference");
			String cLocUrl = outter.getString("ContainerLocation");
			return cLocUrl;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Parse an <xs:duration> string and return the duration in two formats:
	 * 
	 * <pre>
	 *     duration = {
	 *         seconds : number of seconds
	 *         hmm : hours and minutes w/o seconds (i.e., 'h:mm')
	 *     }
	 * </pre>
	 * 
	 * <xs:duration> specifies a duration as PThhHmmMss.sS with any sub-field
	 * optional. The following are all valid specifications of 71 minutes in the
	 * Media manifest XSD:
	 * <ul>
	 * <li>PT01H11M00.0S</li>
	 * <li>PT00H71M00.0S</li>
	 * <li>PT71M00.0S</li>
	 * <li>PT70M60.0S</li>
	 * </ul>
	 * <p>
	 * ' This software, however, requires time to be specified by more stringent
	 * rules. Duration will be specified with the "M", and "S" fields only. The
	 * "S" field is <b>required</b>, even when it contains a zero value. The "M"
	 * field is <b>optional</b>
	 * </p>
	 * 
	 * @param runTime
	 * @return
	 */
	public static JSONObject durationAsSeconds(String dur) {
		JSONObject duration = new JSONObject();
		String temp1 = dur.replace("PT", "");
		String[] parts = temp1.split("[MS]");
		int mVal;
		float seconds;
		if (parts.length == 2) {
			// Now convert to total number of seconds
			mVal = Integer.parseInt(parts[0]);
			seconds = Float.parseFloat(parts[1]);
		} else {
			mVal = 0;
			seconds = Float.parseFloat(parts[0]);
		}
		int sVal = (mVal * 60) + Math.round(seconds);
		/*
		 * Now convert to the h:mm format. Start with total duration as number
		 * of hours including fractional part.
		 */ 
		float hoursF = sVal / (60.0f * 60.0f);
		// always round down..
		int hours = (int) Math.floor(hoursF);
		float minutesF = (float) (60.0 * (hoursF - hours));
		// round up or down (i.e., to nearest Int value)..
		int minutes = Math.round(minutesF);
		String hPattern = "#0";
		DecimalFormat hFormat = new DecimalFormat(hPattern);
		String mPattern = "00";
		DecimalFormat mFormat = new DecimalFormat(mPattern);
		String hmm = hFormat.format(hours) + ":" + mFormat.format(minutes);

		try {
			duration.put("seconds", sVal);
			duration.put("hmm", hmm);
		} catch (JSONException e) {
		}

		return duration;
	}
}
