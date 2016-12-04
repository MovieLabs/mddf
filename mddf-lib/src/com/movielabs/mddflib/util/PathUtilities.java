/**
 * Created Nov 30, 2016 
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
package com.movielabs.mddflib.util;

import java.io.File;
import java.io.IOException;

/**
 * A few simple utilities to manipulate file paths.
 * 
 * @author L. Levin, Critical Architectures LLC
 */
public class PathUtilities {

	public static boolean isRelative(String path) {
		if (path.contains(":")) {
			return false;
		}
		File test = new File(path);
		return !test.isAbsolute();
	}

	public static String convertToAbsolute(String base, String target) throws IOException {
		if (!isRelative(target)) {
			return target;
		} else {
			String basePath;
			File baseFile = new File(base);
			if (baseFile.isDirectory()) {
				basePath = baseFile.getCanonicalPath();
			} else {
				basePath = baseFile.getParent();
			}
			File test = new File(basePath, target);
			return test.getCanonicalPath();
		}
	}

	/**
	 * Convert an absolute path into a relative path. If the <tt>target</tt> is
	 * on a different file system than the <tt>base</tt>, no relative path
	 * exists and the original value of <tt>target</tt> is returned. If the
	 * <tt>base</tt> points to a file, then the file's parent directory will be
	 * used as the base directory.
	 * <p>
	 * <b>NOTE</b>: the two input parameters should use the appropriate file
	 * separator for the OS environment in which the software is running.
	 * </p>
	 * 
	 * @param base
	 * @param target
	 * @return
	 * @throws IOException
	 */
	public static String convertToRelative(String base, String target) throws IOException {
		String basePath;
		File baseFile = new File(base);
		if (baseFile.isDirectory()) {
			basePath = baseFile.getCanonicalPath();
		} else {
			basePath = baseFile.getParent();
		}
		String fsep = File.separator;
		/* make sure the file sep is useable in a regexp */
		if (fsep.equalsIgnoreCase("\\")) {
			fsep = "\\\\";
		}
		String[] baseSeg = basePath.split(fsep);
		File targetFile = new File(target);
		String targetPath = targetFile.getCanonicalPath();
		String[] targetSeg = targetPath.split(fsep);
		int check1 = Math.min(baseSeg.length, targetSeg.length);
		boolean common = true;
		int i = 0;
		while ((i < check1 && common)) {
			if (!baseSeg[i].equals(targetSeg[i++])) {
				common = false;
			}
		}
		String relPath = "";
		/*
		 * need to distinguish btwn end of comparison due to non-match vs end
		 * due to all matching;
		 */
		if (!common) {
			i = i - 1;
		} else {
			relPath = "." + File.separator;
		}
		// is there any commonality at all??
		if (i == 0) {
			// nope, so just rtn the target's canonical path
			return targetPath;
		}
		/*
		 * how many 'ups' rqd in the base path depends on how many remaining
		 * levels
		 */
		if (i < baseSeg.length) {
			for (int j = i; j < baseSeg.length; j++) {
				relPath = relPath + ".." + File.separator;
			}
		}
		for (int j = i; j < targetSeg.length; j++) {
			relPath = relPath + targetSeg[j];
			if (j < targetSeg.length) {
				relPath = relPath + File.separator;
			}
		}
		return relPath;
	}
}
