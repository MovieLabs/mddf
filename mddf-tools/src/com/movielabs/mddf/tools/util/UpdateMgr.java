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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import com.movielabs.mddf.tools.ToolLauncher;
import com.movielabs.mddf.tools.UpdateDialog;
import com.movielabs.mddf.tools.ValidatorTool;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.groovy.JsonSlurper;

/**
 * Handles checking to see if a more recent version of the toolkit is available
 * for download. The <tt>UpdateMgr</tt> can be invoked at any time but is
 * typically used on startup. A query is sent to the <tt>UPDATE_SERVER</tt>
 * which will respond with a message indicating if an update is available. If
 * one is, a pop-up notifying the user is displayed.
 * <p>
 * All communications with the server will take place on a background thread
 * using a <tt>SwingWorker</tt>
 * </p>
 * 
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class UpdateMgr {
	public static final String UPDATE_SERVER = "https://mddf.movielabs.com/updateMgr";
//	private static final String UPDATE_SERVER = "http://localhost:8080/mddf-svcs/updateMgr";

	public static final int maxDaysBtwnChecks = 7;
	private static UpdateMgr singleton = new UpdateMgr();

	public class UpdateWorker extends SwingWorker<Void, Void> {

		private ToolLauncher framework;
		private Component uiFrame;
		private boolean forced;
		private JSONObject statusCheck;
		private String curVersion;

		/**
		 * @param framework
		 * @param uiFrame
		 * @param forced
		 */
		public UpdateWorker(ToolLauncher framework, Component uiFrame, boolean forced) {
			super();
			this.framework = framework;
			this.uiFrame = uiFrame;
			this.forced = forced;
		}

		@Override
		protected Void doInBackground() throws Exception {
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
					return null;
				}
				LocalDate then = Instant.ofEpochMilli(dateLastChecked.getTime()).atZone(ZoneId.of("UTC+0"))
						.toLocalDate();

				// compare with current date
				LocalDate now = LocalDate.now();
				long daysBetween = ChronoUnit.DAYS.between(then, now);
				if (daysBetween <= maxDaysBtwnChecks) {
					// no need to check
					return null;
				}
			}
			// check with server
			curVersion = mddfToolProps.getProperty("mddf.tool.version");
			statusCheck = getStatus(framework, curVersion);
			return null;
		}

		@Override
		protected void done() {
			if (statusCheck == null) {
				if (forced) {
					JOptionPane.showMessageDialog(uiFrame,
							"<html>Unable to connect with Update Server.<br/>Try latter</html>", "Server Unreachable",
							JOptionPane.WARNING_MESSAGE);
				}
			} else {
				System.out.println(statusCheck);
				/* Server's response is in the form of a JSON structure */
				String status = statusCheck.optString("status", "UPDATE");
				if (status.equals("UPDATE")) {
					/*
					 * A detailed msg may *OPTIONALLY* be provided by the server. If absent, use a
					 * simple default
					 */
					String defaultMsg = "A more recent version is available. See ChangeLog for details";
					String msg2user = statusCheck.optString("userMsg", defaultMsg);
					UpdateDialog dialog = new UpdateDialog(statusCheck, curVersion, uiFrame, msg2user);
					dialog.setVisible(true);
				} else {
					if (forced) {
						JOptionPane.showMessageDialog(uiFrame, "Software is up-to-date.");
					}
				}
				LocalDate now = LocalDate.now();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd");
				framework.setProperty("updateMgr.lastCheck", now.format(formatter));
			}
		}
	}

	/**
	 * Check with <tt>UPDATE_SERVER</tt> to see if an update is available. Check is
	 * only made if the elapsed number of days since last check exceeds
	 * <tt>maxDaysBtwnChecks</tt>.
	 * 
	 * @param framework ToolLauncher
	 * @return
	 */
	public static void check(ToolLauncher framework) {
		check(framework, framework.getFrame(), false);
	}

	/**
	 * Check with <tt>UPDATE_SERVER</tt> to see if an update is available.
	 * 
	 * @param framework ToolLauncher
	 * @param uiFrame   parent component used to position dialog
	 * @param forced    if <tt>true</tt>, check will be made regardless of time
	 *                  elapsed since previous check.
	 * @return
	 */
	public static void check(ToolLauncher framework, Component uiFrame, boolean forced) {
		singleton.launch(framework, uiFrame, forced);
	}

	private void launch(ToolLauncher framework, Component uiFrame, boolean forced) {
		SwingWorker<Void, Void> worker = new UpdateMgr.UpdateWorker(framework, uiFrame, forced);
		worker.execute();

	}

	private static JSONObject getStatus(ToolLauncher framework, String curVersion) {
		securityKludge();
		String uuid = framework.getUUID();
		String fullUrl = UPDATE_SERVER + "?uuid=" + uuid + "&curVer=" + curVersion;
		System.out.println(fullUrl);
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
