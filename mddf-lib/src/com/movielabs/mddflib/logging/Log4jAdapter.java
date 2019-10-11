/**
 * Created Sep 9, 2016 
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
package com.movielabs.mddflib.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

import com.movielabs.mddflib.util.xml.MddfTarget;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class Log4jAdapter implements org.apache.logging.log4j.Logger {

	private LogMgmt toolLog;
	private int tag;
	private String moduleID;
	private MddfTarget curTarget;

	public Log4jAdapter(LogMgmt toolLog, int tag, String moduleID) {
		this.toolLog = toolLog;
		this.tag = tag;
		this.moduleID = moduleID;
		this.curTarget = null;
	}

	public void setTargetFile(MddfTarget file) {
		this.curTarget = file;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String)
	 */
	@Override
	public void debug(String msg) {
		sendToLog(msg, LogMgmt.LEV_DEBUG);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String)
	 */
	@Override
	public void warn(String msg) {
		sendToLog(msg, LogMgmt.LEV_WARN);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String)
	 */
	@Override
	public void error(String msg) {
		sendToLog(msg, LogMgmt.LEV_ERR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String)
	 */
	@Override
	public void fatal(String msg) {
		sendToLog(msg, LogMgmt.LEV_FATAL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String)
	 */
	@Override
	public void log(Level arg0, String arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	private void sendToLog(String msg, int level) {
		/*
		 * avails-lib will format messages relating to the excel with row number
		 * first (e.g "Row    5: Invalid LicenseRIghts"). Extract the row number
		 * and use as equivalent to line number in an XML file.
		 */
		if (msg.startsWith("Row ")) {
			String[] parts = msg.split(":", 2);
			msg = parts[1];
			int row;
			try {
				String rowS = parts[0].replaceFirst("Row[\\s]+", "");
				row = Integer.parseInt(rowS);
				toolLog.log(level, tag, msg, curTarget, row, moduleID, null, null);
				return;
			} catch (Exception e) {
				System.out.println(e.getLocalizedMessage() + ", Parsing '" + msg + "'");
			}
		}
		toolLog.log(level, tag, msg, curTarget, null, moduleID, null, null);

	}

	// ###################################################################
	// UNSUPPORTED OPERATIONS...........
	// -------------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#catching(java.lang.Throwable)
	 */
	@Override
	public void catching(Throwable arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#catching(org.apache.logging.log4j.Level,
	 * java.lang.Throwable)
	 */
	@Override
	public void catching(Level arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.message.
	 * Message)
	 */
	@Override
	public void debug(Message arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.util.
	 * MessageSupplier)
	 */
	@Override
	public void debug(MessageSupplier arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.CharSequence)
	 */
	@Override
	public void debug(CharSequence arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.Object)
	 */
	@Override
	public void debug(Object arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.util.
	 * Supplier)
	 */
	@Override
	public void debug(Supplier<?> arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message)
	 */
	@Override
	public void debug(Marker arg0, Message arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier)
	 */
	@Override
	public void debug(Marker arg0, MessageSupplier arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.CharSequence)
	 */
	@Override
	public void debug(Marker arg0, CharSequence arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.Object)
	 */
	@Override
	public void debug(Marker arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String)
	 */
	@Override
	public void debug(Marker arg0, String arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.Supplier)
	 */
	@Override
	public void debug(Marker arg0, Supplier<?> arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.message.
	 * Message, java.lang.Throwable)
	 */
	@Override
	public void debug(Message arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.util.
	 * MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void debug(MessageSupplier arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.CharSequence,
	 * java.lang.Throwable)
	 */
	@Override
	public void debug(CharSequence arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.Object,
	 * java.lang.Throwable)
	 */
	@Override
	public void debug(Object arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void debug(String arg0, Object... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void debug(String arg0, Supplier<?>... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * java.lang.Throwable)
	 */
	@Override
	public void debug(String arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.util.
	 * Supplier, java.lang.Throwable)
	 */
	@Override
	public void debug(Supplier<?> arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void debug(String arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message, java.lang.Throwable)
	 */
	@Override
	public void debug(Marker arg0, Message arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void debug(Marker arg0, MessageSupplier arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.CharSequence, java.lang.Throwable)
	 */
	@Override
	public void debug(Marker arg0, CharSequence arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.Object, java.lang.Throwable)
	 */
	@Override
	public void debug(Marker arg0, Object arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object[])
	 */
	@Override
	public void debug(Marker arg0, String arg1, Object... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void debug(Marker arg0, String arg1, Supplier<?>... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void debug(Marker arg0, String arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.Supplier, java.lang.Throwable)
	 */
	@Override
	public void debug(Marker arg0, Supplier<?> arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object)
	 */
	@Override
	public void debug(Marker arg0, String arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(String arg0, Object arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(Marker arg0, String arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(String arg0, Object arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(String arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void debug(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void debug(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void debug(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void debug(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#debug(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#debug(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#entry()
	 */
	@Override
	public void entry() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#entry(java.lang.Object[])
	 */
	@Override
	public void entry(Object... arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.message.
	 * Message)
	 */
	@Override
	public void error(Message arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.util.
	 * MessageSupplier)
	 */
	@Override
	public void error(MessageSupplier arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.CharSequence)
	 */
	@Override
	public void error(CharSequence arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.Object)
	 */
	@Override
	public void error(Object arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.util.
	 * Supplier)
	 */
	@Override
	public void error(Supplier<?> arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message)
	 */
	@Override
	public void error(Marker arg0, Message arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier)
	 */
	@Override
	public void error(Marker arg0, MessageSupplier arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.CharSequence)
	 */
	@Override
	public void error(Marker arg0, CharSequence arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.Object)
	 */
	@Override
	public void error(Marker arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String)
	 */
	@Override
	public void error(Marker arg0, String arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.Supplier)
	 */
	@Override
	public void error(Marker arg0, Supplier<?> arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.message.
	 * Message, java.lang.Throwable)
	 */
	@Override
	public void error(Message arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.util.
	 * MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void error(MessageSupplier arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.CharSequence,
	 * java.lang.Throwable)
	 */
	@Override
	public void error(CharSequence arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.Object,
	 * java.lang.Throwable)
	 */
	@Override
	public void error(Object arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void error(String arg0, Object... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void error(String arg0, Supplier<?>... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * java.lang.Throwable)
	 */
	@Override
	public void error(String arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.util.
	 * Supplier, java.lang.Throwable)
	 */
	@Override
	public void error(Supplier<?> arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void error(String arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message, java.lang.Throwable)
	 */
	@Override
	public void error(Marker arg0, Message arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void error(Marker arg0, MessageSupplier arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.CharSequence, java.lang.Throwable)
	 */
	@Override
	public void error(Marker arg0, CharSequence arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.Object, java.lang.Throwable)
	 */
	@Override
	public void error(Marker arg0, Object arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object[])
	 */
	@Override
	public void error(Marker arg0, String arg1, Object... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void error(Marker arg0, String arg1, Supplier<?>... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void error(Marker arg0, String arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.Supplier, java.lang.Throwable)
	 */
	@Override
	public void error(Marker arg0, Supplier<?> arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object)
	 */
	@Override
	public void error(Marker arg0, String arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(String arg0, Object arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(Marker arg0, String arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(String arg0, Object arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(String arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void error(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void error(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void error(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void error(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#error(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#error(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void error(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#exit()
	 */
	@Override
	public void exit() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#exit(java.lang.Object)
	 */
	@Override
	public <R> R exit(R arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.message.
	 * Message)
	 */
	@Override
	public void fatal(Message arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.util.
	 * MessageSupplier)
	 */
	@Override
	public void fatal(MessageSupplier arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.CharSequence)
	 */
	@Override
	public void fatal(CharSequence arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.Object)
	 */
	@Override
	public void fatal(Object arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.util.
	 * Supplier)
	 */
	@Override
	public void fatal(Supplier<?> arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message)
	 */
	@Override
	public void fatal(Marker arg0, Message arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier)
	 */
	@Override
	public void fatal(Marker arg0, MessageSupplier arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.CharSequence)
	 */
	@Override
	public void fatal(Marker arg0, CharSequence arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.Object)
	 */
	@Override
	public void fatal(Marker arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String)
	 */
	@Override
	public void fatal(Marker arg0, String arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.Supplier)
	 */
	@Override
	public void fatal(Marker arg0, Supplier<?> arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.message.
	 * Message, java.lang.Throwable)
	 */
	@Override
	public void fatal(Message arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.util.
	 * MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void fatal(MessageSupplier arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.CharSequence,
	 * java.lang.Throwable)
	 */
	@Override
	public void fatal(CharSequence arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.Object,
	 * java.lang.Throwable)
	 */
	@Override
	public void fatal(Object arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void fatal(String arg0, Object... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void fatal(String arg0, Supplier<?>... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * java.lang.Throwable)
	 */
	@Override
	public void fatal(String arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.util.
	 * Supplier, java.lang.Throwable)
	 */
	@Override
	public void fatal(Supplier<?> arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void fatal(String arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message, java.lang.Throwable)
	 */
	@Override
	public void fatal(Marker arg0, Message arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void fatal(Marker arg0, MessageSupplier arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.CharSequence, java.lang.Throwable)
	 */
	@Override
	public void fatal(Marker arg0, CharSequence arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.Object, java.lang.Throwable)
	 */
	@Override
	public void fatal(Marker arg0, Object arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object[])
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Object... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Supplier<?>... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.Supplier, java.lang.Throwable)
	 */
	@Override
	public void fatal(Marker arg0, Supplier<?> arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object)
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(String arg0, Object arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(String arg0, Object arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(String arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void fatal(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void fatal(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#fatal(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#fatal(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fatal(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#getLevel()
	 */
	@Override
	public Level getLevel() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#getMessageFactory()
	 */
	@Override
	public <MF extends MessageFactory> MF getMessageFactory() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#getName()
	 */
	@Override
	public String getName() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.message.
	 * Message)
	 */
	@Override
	public void info(Message arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.util.
	 * MessageSupplier)
	 */
	@Override
	public void info(MessageSupplier arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.CharSequence)
	 */
	@Override
	public void info(CharSequence arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.Object)
	 */
	@Override
	public void info(Object arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String)
	 */
	@Override
	public void info(String arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.util.
	 * Supplier)
	 */
	@Override
	public void info(Supplier<?> arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message)
	 */
	@Override
	public void info(Marker arg0, Message arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier)
	 */
	@Override
	public void info(Marker arg0, MessageSupplier arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.CharSequence)
	 */
	@Override
	public void info(Marker arg0, CharSequence arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.Object)
	 */
	@Override
	public void info(Marker arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String)
	 */
	@Override
	public void info(Marker arg0, String arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.Supplier)
	 */
	@Override
	public void info(Marker arg0, Supplier<?> arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.message.
	 * Message, java.lang.Throwable)
	 */
	@Override
	public void info(Message arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.util.
	 * MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void info(MessageSupplier arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.CharSequence,
	 * java.lang.Throwable)
	 */
	@Override
	public void info(CharSequence arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.Object,
	 * java.lang.Throwable)
	 */
	@Override
	public void info(Object arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void info(String arg0, Object... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void info(String arg0, Supplier<?>... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * java.lang.Throwable)
	 */
	@Override
	public void info(String arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.util.
	 * Supplier, java.lang.Throwable)
	 */
	@Override
	public void info(Supplier<?> arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void info(String arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message, java.lang.Throwable)
	 */
	@Override
	public void info(Marker arg0, Message arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void info(Marker arg0, MessageSupplier arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.CharSequence, java.lang.Throwable)
	 */
	@Override
	public void info(Marker arg0, CharSequence arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.Object, java.lang.Throwable)
	 */
	@Override
	public void info(Marker arg0, Object arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object[])
	 */
	@Override
	public void info(Marker arg0, String arg1, Object... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void info(Marker arg0, String arg1, Supplier<?>... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void info(Marker arg0, String arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.Supplier, java.lang.Throwable)
	 */
	@Override
	public void info(Marker arg0, Supplier<?> arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object)
	 */
	@Override
	public void info(Marker arg0, String arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(String arg0, Object arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(Marker arg0, String arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(String arg0, Object arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(String arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void info(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void info(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void info(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void info(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#info(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#info(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#isDebugEnabled()
	 */
	@Override
	public boolean isDebugEnabled() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#isDebugEnabled(org.apache.logging.log4j.
	 * Marker)
	 */
	@Override
	public boolean isDebugEnabled(Marker arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#isEnabled(org.apache.logging.log4j.Level)
	 */
	@Override
	public boolean isEnabled(Level arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#isEnabled(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker)
	 */
	@Override
	public boolean isEnabled(Level arg0, Marker arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#isErrorEnabled()
	 */
	@Override
	public boolean isErrorEnabled() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#isErrorEnabled(org.apache.logging.log4j.
	 * Marker)
	 */
	@Override
	public boolean isErrorEnabled(Marker arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#isFatalEnabled()
	 */
	@Override
	public boolean isFatalEnabled() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#isFatalEnabled(org.apache.logging.log4j.
	 * Marker)
	 */
	@Override
	public boolean isFatalEnabled(Marker arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#isInfoEnabled()
	 */
	@Override
	public boolean isInfoEnabled() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#isInfoEnabled(org.apache.logging.log4j.
	 * Marker)
	 */
	@Override
	public boolean isInfoEnabled(Marker arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#isTraceEnabled()
	 */
	@Override
	public boolean isTraceEnabled() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#isTraceEnabled(org.apache.logging.log4j.
	 * Marker)
	 */
	@Override
	public boolean isTraceEnabled(Marker arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#isWarnEnabled()
	 */
	@Override
	public boolean isWarnEnabled() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#isWarnEnabled(org.apache.logging.log4j.
	 * Marker)
	 */
	@Override
	public boolean isWarnEnabled(Marker arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.message.Message)
	 */
	@Override
	public void log(Level arg0, Message arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.util.MessageSupplier)
	 */
	@Override
	public void log(Level arg0, MessageSupplier arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.CharSequence)
	 */
	@Override
	public void log(Level arg0, CharSequence arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.Object)
	 */
	@Override
	public void log(Level arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.util.Supplier)
	 */
	@Override
	public void log(Level arg0, Supplier<?> arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message)
	 */
	@Override
	public void log(Level arg0, Marker arg1, Message arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier)
	 */
	@Override
	public void log(Level arg0, Marker arg1, MessageSupplier arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.CharSequence)
	 */
	@Override
	public void log(Level arg0, Marker arg1, CharSequence arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, Marker arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String)
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, org.apache.logging.log4j.util.Supplier)
	 */
	@Override
	public void log(Level arg0, Marker arg1, Supplier<?> arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.message.Message, java.lang.Throwable)
	 */
	@Override
	public void log(Level arg0, Message arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.util.MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void log(Level arg0, MessageSupplier arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.CharSequence, java.lang.Throwable)
	 */
	@Override
	public void log(Level arg0, CharSequence arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.Object, java.lang.Throwable)
	 */
	@Override
	public void log(Level arg0, Object arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Object[])
	 */
	@Override
	public void log(Level arg0, String arg1, Object... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void log(Level arg0, String arg1, Supplier<?>... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void log(Level arg0, String arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.util.Supplier, java.lang.Throwable)
	 */
	@Override
	public void log(Level arg0, Supplier<?> arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, String arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message, java.lang.Throwable)
	 */
	@Override
	public void log(Level arg0, Marker arg1, Message arg2, Throwable arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void log(Level arg0, Marker arg1, MessageSupplier arg2, Throwable arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.CharSequence,
	 * java.lang.Throwable)
	 */
	@Override
	public void log(Level arg0, Marker arg1, CharSequence arg2, Throwable arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.Object, java.lang.Throwable)
	 */
	@Override
	public void log(Level arg0, Marker arg1, Object arg2, Throwable arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Object... arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String,
	 * org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Supplier<?>... arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Throwable arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, org.apache.logging.log4j.util.Supplier,
	 * java.lang.Throwable)
	 */
	@Override
	public void log(Level arg0, Marker arg1, Supplier<?> arg2, Throwable arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, String arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, String arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void log(Level arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void log(Level arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void log(Level arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#log(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void log(Level arg0, Marker arg1, String arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#printf(org.apache.logging.log4j.Level,
	 * java.lang.String, java.lang.Object[])
	 */
	@Override
	public void printf(Level arg0, String arg1, Object... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#printf(org.apache.logging.log4j.Level,
	 * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void printf(Level arg0, Marker arg1, String arg2, Object... arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#throwing(java.lang.Throwable)
	 */
	@Override
	public <T extends Throwable> T throwing(T arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#throwing(org.apache.logging.log4j.Level,
	 * java.lang.Throwable)
	 */
	@Override
	public <T extends Throwable> T throwing(Level arg0, T arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.message.
	 * Message)
	 */
	@Override
	public void trace(Message arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.util.
	 * MessageSupplier)
	 */
	@Override
	public void trace(MessageSupplier arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.CharSequence)
	 */
	@Override
	public void trace(CharSequence arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.Object)
	 */
	@Override
	public void trace(Object arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String)
	 */
	@Override
	public void trace(String arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.util.
	 * Supplier)
	 */
	@Override
	public void trace(Supplier<?> arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message)
	 */
	@Override
	public void trace(Marker arg0, Message arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier)
	 */
	@Override
	public void trace(Marker arg0, MessageSupplier arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.CharSequence)
	 */
	@Override
	public void trace(Marker arg0, CharSequence arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.Object)
	 */
	@Override
	public void trace(Marker arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String)
	 */
	@Override
	public void trace(Marker arg0, String arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.Supplier)
	 */
	@Override
	public void trace(Marker arg0, Supplier<?> arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.message.
	 * Message, java.lang.Throwable)
	 */
	@Override
	public void trace(Message arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.util.
	 * MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void trace(MessageSupplier arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.CharSequence,
	 * java.lang.Throwable)
	 */
	@Override
	public void trace(CharSequence arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.Object,
	 * java.lang.Throwable)
	 */
	@Override
	public void trace(Object arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void trace(String arg0, Object... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void trace(String arg0, Supplier<?>... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * java.lang.Throwable)
	 */
	@Override
	public void trace(String arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.util.
	 * Supplier, java.lang.Throwable)
	 */
	@Override
	public void trace(Supplier<?> arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void trace(String arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message, java.lang.Throwable)
	 */
	@Override
	public void trace(Marker arg0, Message arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void trace(Marker arg0, MessageSupplier arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.CharSequence, java.lang.Throwable)
	 */
	@Override
	public void trace(Marker arg0, CharSequence arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.Object, java.lang.Throwable)
	 */
	@Override
	public void trace(Marker arg0, Object arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object[])
	 */
	@Override
	public void trace(Marker arg0, String arg1, Object... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void trace(Marker arg0, String arg1, Supplier<?>... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void trace(Marker arg0, String arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.Supplier, java.lang.Throwable)
	 */
	@Override
	public void trace(Marker arg0, Supplier<?> arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object)
	 */
	@Override
	public void trace(Marker arg0, String arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(String arg0, Object arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(Marker arg0, String arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(String arg0, Object arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(String arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void trace(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void trace(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void trace(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void trace(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#trace(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#trace(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void trace(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#traceEntry()
	 */
	@Override
	public EntryMessage traceEntry() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#traceEntry(org.apache.logging.log4j.util.
	 * Supplier[])
	 */
	@Override
	public EntryMessage traceEntry(Supplier<?>... arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#traceEntry(org.apache.logging.log4j.
	 * message.Message)
	 */
	@Override
	public EntryMessage traceEntry(Message arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#traceEntry(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public EntryMessage traceEntry(String arg0, Object... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#traceEntry(java.lang.String,
	 * org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public EntryMessage traceEntry(String arg0, Supplier<?>... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#traceExit()
	 */
	@Override
	public void traceExit() {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#traceExit(java.lang.Object)
	 */
	@Override
	public <R> R traceExit(R arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#traceExit(org.apache.logging.log4j.
	 * message.EntryMessage)
	 */
	@Override
	public void traceExit(EntryMessage arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#traceExit(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public <R> R traceExit(String arg0, R arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#traceExit(org.apache.logging.log4j.
	 * message.EntryMessage, java.lang.Object)
	 */
	@Override
	public <R> R traceExit(EntryMessage arg0, R arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#traceExit(org.apache.logging.log4j.
	 * message.Message, java.lang.Object)
	 */
	@Override
	public <R> R traceExit(Message arg0, R arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub
		// return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.message.
	 * Message)
	 */
	@Override
	public void warn(Message arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.util.
	 * MessageSupplier)
	 */
	@Override
	public void warn(MessageSupplier arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.CharSequence)
	 */
	@Override
	public void warn(CharSequence arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.Object)
	 */
	@Override
	public void warn(Object arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.util.
	 * Supplier)
	 */
	@Override
	public void warn(Supplier<?> arg0) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message)
	 */
	@Override
	public void warn(Marker arg0, Message arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier)
	 */
	@Override
	public void warn(Marker arg0, MessageSupplier arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.CharSequence)
	 */
	@Override
	public void warn(Marker arg0, CharSequence arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.Object)
	 */
	@Override
	public void warn(Marker arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String)
	 */
	@Override
	public void warn(Marker arg0, String arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.Supplier)
	 */
	@Override
	public void warn(Marker arg0, Supplier<?> arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.message.
	 * Message, java.lang.Throwable)
	 */
	@Override
	public void warn(Message arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.util.
	 * MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void warn(MessageSupplier arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.CharSequence,
	 * java.lang.Throwable)
	 */
	@Override
	public void warn(CharSequence arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.Object,
	 * java.lang.Throwable)
	 */
	@Override
	public void warn(Object arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void warn(String arg0, Object... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void warn(String arg0, Supplier<?>... arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * java.lang.Throwable)
	 */
	@Override
	public void warn(String arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.util.
	 * Supplier, java.lang.Throwable)
	 */
	@Override
	public void warn(Supplier<?> arg0, Throwable arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void warn(String arg0, Object arg1) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.message.Message, java.lang.Throwable)
	 */
	@Override
	public void warn(Marker arg0, Message arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.MessageSupplier, java.lang.Throwable)
	 */
	@Override
	public void warn(Marker arg0, MessageSupplier arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.CharSequence, java.lang.Throwable)
	 */
	@Override
	public void warn(Marker arg0, CharSequence arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.Object, java.lang.Throwable)
	 */
	@Override
	public void warn(Marker arg0, Object arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object[])
	 */
	@Override
	public void warn(Marker arg0, String arg1, Object... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, org.apache.logging.log4j.util.Supplier[])
	 */
	@Override
	public void warn(Marker arg0, String arg1, Supplier<?>... arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void warn(Marker arg0, String arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * org.apache.logging.log4j.util.Supplier, java.lang.Throwable)
	 */
	@Override
	public void warn(Marker arg0, Supplier<?> arg1, Throwable arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object)
	 */
	@Override
	public void warn(Marker arg0, String arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(String arg0, Object arg1, Object arg2) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(Marker arg0, String arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(String arg0, Object arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(String arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void warn(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void warn(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void warn(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void warn(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.logging.log4j.Logger#warn(java.lang.String,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(String arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.Logger#warn(org.apache.logging.log4j.Marker,
	 * java.lang.String, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object,
	 * java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(Marker arg0, String arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {
		throw new UnsupportedOperationException(); // TODO Auto-generated method
													// stub

	}

}
