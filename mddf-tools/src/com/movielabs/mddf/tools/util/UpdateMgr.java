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
package com.movielabs.mddf.tools.util;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import com.movielabs.mddf.tools.ToolLauncher;
import com.movielabs.mddf.tools.UpdateDialog;
import com.movielabs.mddf.tools.ValidatorTool;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.groovy.JsonSlurper;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class UpdateMgr {
	private static final String updateUrl = "https://mddf.movielabs.com/updateMgr";
//	private static final String updateUrl = "http://localhost:8080/mddf-svcs/updateMgr"; 

	private static final int maxDaysBtwnChecks = 7;

	public static boolean check(ToolLauncher framework) {
		return check(framework, framework.getFrame(), false);
	}

	public static boolean check(ToolLauncher framework, Component uiFrame, boolean forced) {
		Properties mddfToolProps = ValidatorTool.loadProperties("/com/movielabs/mddf/tools/build.properties");
		if (!forced) {
			// how long since last check??
			String last = framework.getProperty("updateMgr.lastCheck");
			if (last == null) {
				// use the build time instead
				last = mddfToolProps.getProperty("build.timestamp");
			}
			// just need yyyy-MMM-dd and can drop the hh:mm
			String[] parts = last.split(" ");
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd");
			Date dateLastChecked = null;
			try {
				dateLastChecked = df.parse(parts[0]);
			} catch (ParseException e) {
				e.printStackTrace();
				return true;
			}
			LocalDate then = Instant.ofEpochMilli(dateLastChecked.getTime()).atZone(ZoneId.of("UTC+0")).toLocalDate();

			// compare with current date
			LocalDate now = LocalDate.now();
			long daysBetween = ChronoUnit.DAYS.between(then, now);

			if (daysBetween <= maxDaysBtwnChecks) {
				// no need to check
				return true;
			}
		}
		// check with server
		String curVersion = mddfToolProps.getProperty("mddf.tool.version");
		JSONObject statusCheck = getStatus(framework, "1.1.0");
		if (statusCheck == null) {
			/*
			 * In the absence of information to the contrary, assume everything is
			 * up-to-date
			 */
			return true;
		}
		String status = statusCheck.optString("status", "UPDATE");
		if (status.equals("UPDATE")) {
			// Notify user they need to update
			UpdateDialog dialog = new UpdateDialog(statusCheck, curVersion, uiFrame);
			dialog.setVisible(true);
		}
		System.out.println(statusCheck.toString(5));
		return true;
	}

	private static JSONObject getStatus(ToolLauncher framework, String curVersion) {
		securityKludge();
		String uuid = framework.getUUID();
		String fullUrl = updateUrl + "?uuid=" + uuid + "&curVer=" + curVersion;
		try {
			URL netUrl = new URL(fullUrl);
			InputStream is = netUrl.openStream();
			JsonSlurper slurper = new JsonSlurper();
			JSON response = slurper.parse(is);
			if (response instanceof JSONObject) {
				return ((JSONObject) response);
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Handle problem of
	 * 
	 * <pre>
	 * javax.net.ssl.SSLHandshakeException:
	 *     sun.security.validator.ValidatorException: PKIX path building failed:
	 *     sun.security.provider.certpath.SunCertPath
	 * </pre>
	 */
	private static void securityKludge() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			;
		}
	}

}
