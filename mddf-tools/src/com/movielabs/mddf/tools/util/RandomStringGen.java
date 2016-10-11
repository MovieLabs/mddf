/**
 * Created on Feb 01, 2016
 * Copyright Motion Picture Laboratories, Inc. (2015)
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

/**
 * Generates random character strings of a specified length. These strings may
 * then be used as part of a security token.
 * 
 * @author L. J. Levin
 * 
 */
public class RandomStringGen {

	/** arrary of all Latin charcters that may be safely used in a URL */
	private static final char[] urlSafeChar = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
			'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
			'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
			'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
			'w', 'x', 'y', 'z', '$', '(', ')', '*', ',', '-', '.', '_' };
	private static final int uscCnt = urlSafeChar.length;
	// for strictly alphanumeric strings....
	private static final int ancCnt = uscCnt - 8;

	/**
	 * generate a random string of a specified length from the set of printable
	 * Basic Latin Unicode characters that may safely be passed as parameters in
	 * a URL.
	 */
	public static String genUrlSafeString(int charCnt) {
		char[] chars = new char[charCnt];
		int idx;
		for (int i = 0; i < charCnt; i++) {
			idx = (int) ((uscCnt) * Math.random());
			chars[i] = urlSafeChar[idx];
		}
		String result = new String(chars);
		return result;
	}

	/**
	 * generate a random string of a specified length from the set of
	 * alphanumeric characters.
	 */
	public static String alphanumericString(int charCnt) {
		char[] chars = new char[charCnt];
		int idx;
		for (int i = 0; i < charCnt; i++) {
			idx = (int) ((ancCnt) * Math.random());
			chars[i] = urlSafeChar[idx];
		}
		String result = new String(chars);
		return result;
	}

	/**
	 * generate a random string of a specified length from the set of all
	 * printable Basic Latin Unicode characters.
	 */
	public static String genString(int charCnt) {
		char[] chars = new char[charCnt];
		/*
		 * gen random characters between '!' and '~', which pretty much covers
		 * the entire range of the "Basic Latin" set of printable Unicode
		 * characters
		 */
		for (int i = 0; i < charCnt; i++) {
			chars[i] = getRandomChar('!', '~');
		}
		String result = new String(chars);
		return result;
	}

	/** Generate a random character between fromChar and toChar */
	private static char getRandomChar(char fromChar, char toChar) {
		// Get the Unicode of the character
		int unicode = fromChar
				+ (int) ((toChar - fromChar + 1) * Math.random());

		// Return the character
		return (char) unicode;
	}

}
