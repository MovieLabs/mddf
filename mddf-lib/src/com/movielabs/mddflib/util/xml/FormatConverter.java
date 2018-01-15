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
package com.movielabs.mddflib.util.xml;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.movielabs.mddflib.logging.LogMgmt;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class FormatConverter {
	private static final String DURATION_REGEX = "P([0-9]+Y)?([0-9]+M)?([0-9]+D)?(T([0-9]+H)?([0-9]+M)?([0-9]+(\\.[0-9]+)?S)?)?";
	private static final String DATETIME_REGEX = "[\\d]{4}-[\\d]{2}-[\\d]{2}T[\\d:\\.]+";

	private static Pattern p_xsDuration;
	private static Pattern p_xsDateTime;
	private static DecimalFormat durFieldFmt = new DecimalFormat("00");

	static { /*
				 * Compile Pattern used to identify an xs:duration value that
				 * requires translation before being added to the spreadsheet.
				 * 
				 */
		p_xsDuration = Pattern.compile(DURATION_REGEX);
		p_xsDateTime = Pattern.compile(DATETIME_REGEX);

	}

	/**
	 * Convert any string formatted in compliance with W3C xs:Duration syntax to
	 * <tt>hh:mm:ss</tt> syntax. Inputs that do not match the xs:duration syntax
	 * will be returned unchanged. After conversion any trailing fields with a
	 * zero value will be dropped (i.e., hh:mm:00s becomes hh:mm). The first
	 * field will always indicate hours, even if it contains a zero value (i.e.,
	 * 00:mm:00s becomes 00:mm).
	 * 
	 * @param input
	 * @return
	 */
	public static String durationFromXml(String input) {
		if(input== null || (input.isEmpty())){
			return input;
		}
		Matcher m = p_xsDuration.matcher(input);
		if (!m.matches()) {
			return input;
		}
		String temp1 = input.replaceFirst("P", "");
		String[] parts = temp1.split("T");
		long totalHrs = 0;
		long totalMin = 0;
		long totalSec = 0;
		if (!parts[0].isEmpty()) {
			/* ignore Y and M, and only allow D fields */
			// if (parts[0].contains("Y") || parts[0].contains("M")) {
			// logger.log(LogMgmt.LEV_WARN, logMsgDefaultTag,
			// "Conversion of duration '" + input + "' will ignore YEAR and
			// MONTH fields", null, logMsgSrcId);
			// }
			Pattern dp = Pattern.compile("[0-9]+D");
			Matcher dm = dp.matcher(parts[0]);
			if (dm.find()) {
				String dayPart = dm.group();
				totalHrs = totalHrs + Integer.parseInt(dayPart.replace("D", ""));
			}
			// covert accumulated days to hours
			totalHrs = totalHrs * 24;

		}
		if (parts.length > 1 && (!parts[1].isEmpty())) {
			// handle H, M, and S fields
			Pattern dp = Pattern.compile("[0-9]+H");
			Matcher dm = dp.matcher(parts[1]);
			if (dm.find()) {
				String hourPart = dm.group();
				totalHrs = totalHrs + Integer.parseInt(hourPart.replace("H", ""));
			}
			dp = Pattern.compile("[0-9]+M");
			dm = dp.matcher(parts[1]);
			if (dm.find()) {
				String mmPart = dm.group();
				totalMin = Integer.parseInt(mmPart.replace("M", ""));
			}
			dp = Pattern.compile("[0-9]+S");
			dm = dp.matcher(parts[1]);
			if (dm.find()) {
				String ssPart = dm.group();
				totalSec = Integer.parseInt(ssPart.replace("S", ""));
			}

		}
		String hh = Integer.toString((int) totalHrs);
		String mm = Integer.toString((int) totalMin);
		String ss = Integer.toString((int) totalSec);
		String output = durFieldFmt.format(totalHrs);
		if ((totalMin + totalSec) > 0) {
			output = output + ":" + durFieldFmt.format(totalMin);
			if (totalSec > 0) {
				output = output + ":" + durFieldFmt.format(totalSec);
			}
		}
		return output;
	}

	/**
	 * Convert any string formatted in compliance with W3C xs:DateTime syntax to
	 * <tt>hh:mm:ss</tt> syntax. Inputs that do not match the syntax will be
	 * returned unchanged.
	 * 
	 * @param input
	 * @return
	 */
	public static String dateFromXml(String input) {
		if(input== null || (input.isEmpty())){
			return input;
		}
		Matcher m = p_xsDateTime.matcher(input);
		if (!m.matches()) {
			return input;
		}
		String[] parts = input.split("T");
		return parts[0];
	}

	/**
	 * Input is one of the following:
	 * <ul>
	 * <li>hh</li>
	 * <li>hh:mm</li>
	 * <li>hh:mm:ss</li>
	 * </ul>
	 * The output format is 'PThhHmmMssS'
	 * 
	 * @param input
	 * @return
	 */
	public static String durationToXml(String input) {
		String parts[] = input.split(":");
		String xmlValue = "PT" + parts[0] + "H";
		if (parts.length > 1) {
			xmlValue = xmlValue + parts[1] + "M";
			if (parts.length > 2) {
				xmlValue = xmlValue + parts[2] + "S";
			}
		}
		return xmlValue;

	}

	public static String dateTimeToXml(String input, boolean roundOff) {
		String output = "";
		if (input.matches("[\\d]{4}-[\\d]{2}-[\\d]{2}")) {
			if (!roundOff) {
				output = input + "T23:59:59";
			} else {
				output = input + "T00:00:00";
			}
		}
		return output;
	}

	/**
	 * @param rawValue
	 * @return
	 */
	public static String booleanToXml(String input) {
		if (input.equals("Yes")) {
			return "true";
		} else if (input.equals("No")) {
			return "false";
		}
		return null;
	}
}
