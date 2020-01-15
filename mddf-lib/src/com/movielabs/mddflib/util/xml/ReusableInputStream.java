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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class ReusableInputStream extends BufferedInputStream {

	public ReusableInputStream(InputStream in) {
		super(in);
		mark(Integer.MAX_VALUE);
	}

	/**
	 * Over-ride parent class implementation to always set the <tt>readlimit</tt> t
	 * <tt>Integer.MAX_VALUE</tt>. This insures the mark position will not be
	 * invalidated.
	 * <p>
	 * <b>Note:</b>: This fixes a bug relating to the behavior of the
	 * <tt>OPCPackage</tt> when it opens the stream.</o>
	 * 
	 * @see java.io.BufferedInputStream#mark(int)
	 */
	public void mark(int readlimit) {
//		System.out.println(" [MARK]" + this.toString() + ", limit=" + readlimit);
		super.mark(Integer.MAX_VALUE);

	}

//	public void reset() throws IOException {
//		System.out.println(" [RESET]" + this.toString());
//		super.reset();
//	}

	@Override
	public void close() throws IOException {
		reset();
	}
}
