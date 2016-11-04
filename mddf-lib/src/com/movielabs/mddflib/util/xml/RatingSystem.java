/**
 * Copyright (c) 2016 MovieLabs

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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * Wrapper for an XML representation of a Content Rating System. Complete
 * information on the specification and usage of Content Rating Systems in the
 * context of Motion Picture Laboratories Common Metadata is available at
 * <a href="http://www.movielabs.com/md/ratings/">http://www.movielabs.com/md/
 * ratings/</a>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class RatingSystem {

	public static final String RSRC_PACKAGE = "/com/movielabs/mddf/resources/";
	public static final Namespace mdcrNSpace = Namespace.getNamespace("mdcr",
			"http://www.movielabs.com/schema/mdcr/v1.1");
	public static final Namespace mdNSpace = Namespace.getNamespace("md", "http://www.movielabs.com/schema/md/v2.1/md");
	private static final String idXPath = "./mdcr:RatingSystem/mdcr:RatingSystemID/mdcr:System";
	private static Map<String, RatingSystem> cache = new HashMap<String, RatingSystem>();
	private static String curVer = "v2.2.6";
	private static Element cmrRootEl = null;

	private XPathFactory xpfac = XPathFactory.instance();
	private Element ratingSystemEl;
	private String ratingSysId;

	static {
		String xmlRsrc = "CMR_Ratings_" + curVer + ".xml";
		String rsrcPath = RSRC_PACKAGE + xmlRsrc;
		SAXBuilder builder = new SAXBuilder();
		builder.setJDOMFactory(new LocatedJDOMFactory());
		InputStream inp = RatingSystem.class.getResourceAsStream(rsrcPath);
		if (inp != null) {
			try {
				InputStreamReader isr = new InputStreamReader(inp, "UTF-8");
				Document cmrDoc = builder.build(isr);
				cmrRootEl = cmrDoc.getRootElement();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static RatingSystem factory(String ratingSysId) {
		synchronized (cache) {
			String key = ratingSysId;
			RatingSystem target = cache.get(key);
			if (target == null) {
				try {
					target = new RatingSystem(ratingSysId);
				} catch (IllegalArgumentException e) {
					return null;
				}
				cache.put(key, target);
			}
			return target;
		}
	}

	private RatingSystem(String ratingSysId) throws IllegalArgumentException {
		this.ratingSysId = ratingSysId;
		String queryPath = idXPath + "[text()='" + ratingSysId + "']";
		XPathExpression<Element> xpExpression = xpfac.compile(queryPath, Filters.element(), null, mdcrNSpace);

		Element systemEl = xpExpression.evaluateFirst(cmrRootEl);
		if (systemEl == null) {
			throw new IllegalArgumentException("Unrecognized RatingSystem '" + ratingSysId + "'");
		}
		// need the 'grandfather' element
		ratingSystemEl = systemEl.getParentElement().getParentElement();

	}

	public boolean isValid(String rating) {
		String queryPath = "./mdcr:Rating[@ratingID='" + rating + "']";
		XPathExpression<Element> xpExpression = xpfac.compile(queryPath, Filters.element(), null, mdcrNSpace);
		Element ratingEl = xpExpression.evaluateFirst(ratingSystemEl);
		return ratingEl != null;
	}

	public boolean isDeprecated(String rating) throws IllegalArgumentException {
		String queryPath = "./mdcr:Rating[@ratingID='" + rating + "']";
		XPathExpression<Element> xpExpression = xpfac.compile(queryPath, Filters.element(), null, mdcrNSpace);
		Element ratingEl = xpExpression.evaluateFirst(ratingSystemEl);
		if (ratingEl == null) {
			throw new IllegalArgumentException("Unrecognized Rating '" + rating + "' for RatingSystem " + ratingSysId);
		}
		String depValue = ratingEl.getChildText("Deprecated", mdcrNSpace);
		return ((depValue != null) && depValue.equals("true"));
	}

	/**
	 * @param isoCountryCode ISO 3166-1 Alpha-2 code
	 * @return
	 */
	public boolean isUsedInRegion(String isoCode) {
		String queryPath = "./mdcr:AdoptiveRegion/md:country[text()='" + isoCode + "']";
		XPathExpression<Element> xpExpression = xpfac.compile(queryPath, Filters.element(), null, mdcrNSpace, mdNSpace);
		Element countryEl = xpExpression.evaluateFirst(ratingSystemEl);
		return (countryEl != null);
	}

	/**
	 * @param isoCountryCode ISO 3166-2 code
	 * @return
	 */
	public boolean isUsedInSubRegion(String isoCode) {
		String queryPath = "./mdcr:AdoptiveRegion/md:countryRegion[text()='" + isoCode + "']";
		XPathExpression<Element> xpExpression = xpfac.compile(queryPath, Filters.element(), null, mdcrNSpace, mdNSpace);
		Element countryEl = xpExpression.evaluateFirst(ratingSystemEl);
		return (countryEl != null);
	}
	
	/* FOR TESTING!!! */
	public static void runTest() {
		test("MOC", "E");
		test("MOC", "15");
		test("MPAA", "PG");
		test("MPAA", "X");
		test("MPAA", "PG13");
		test("MPAA", "PG-13");
	}

	/**
	 * @param string
	 * @param string2
	 */
	private static void test(String system, String rating) {
		RatingSystem rs1 = RatingSystem.factory(system);
		if (rs1 != null) {
			System.out.println(system + ":" + rating + "=" + rs1.isValid(rating));
			if (rs1.isValid(rating)) {
				System.out.println("    deprecated =" + rs1.isDeprecated(rating));
			}
			System.out.println("    adopted in US =" + rs1.isUsedInRegion("US"));
		}

	}
}
