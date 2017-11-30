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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedElement;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.movielabs.mddf.MddfContext;
import com.movielabs.mddflib.util.StringUtils;

import net.sf.json.JSONObject;

/**
 * Wrapper for an XSD specification. This class provides functions supporting
 * queries and comparisons that facilitate checking an XML file for
 * compatibility with a given schema. It is not intended to replace the classes
 * in the <tt>javax.xml.validation</tt> package but it is rather a supplement
 * intended to make some types of checks easier. 
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class SchemaWrapper {
	public static final Namespace xsNSpace = Namespace.getNamespace("xs", "http://www.w3.org/2001/XMLSchema");
	public static final Namespace xsiNSpace = Namespace.getNamespace("xsi",
			"http://www.w3.org/2001/XMLSchema-instance");

	public static final String RSRC_PACKAGE = "/com/movielabs/mddf/resources/";
	public static final String JSON_KEY_PREFIX = "@__";
	private static Map<String, SchemaWrapper> cache = new HashMap<String, SchemaWrapper>();

	private Document schemaXSD;

	private Element rootEl;

	private XPathFactory xpfac = XPathFactory.instance();
	private Namespace nSpace;
	private ArrayList<XPathExpression<?>> reqElXpList;
	private HashMap<String, SchemaWrapper> otherSchemas = new HashMap<String, SchemaWrapper>();
	private int anonSeqNum = 0;

	public static SchemaWrapper factory(String xsdRsrc) {
		synchronized (cache) {
			SchemaWrapper target = cache.get(xsdRsrc);
			if (target == null) {
				try {
					target = new SchemaWrapper(xsdRsrc);
					cache.put(xsdRsrc, target);
				} catch (Exception e) {
					/*
					 * This happens if request is for a schema we don't provide
					 * XSD for
					 */
					System.out.println("SchemaWrapper.factory(): Exception for " + xsdRsrc);
				}
			}
			return target;
		}
	}

	/**
	 * Return the XSD resource with the specified schema. If the requested
	 * version is not supported a <tt>null</tt> value is returned.
	 * 
	 * @param xsdRsrc
	 * @return
	 */
	private static Document getSchemaXSD(String xsdRsrc) {
		String rsrcPath = RSRC_PACKAGE + xsdRsrc + ".xsd";
		SAXBuilder builder = new SAXBuilder();
		builder.setJDOMFactory(new LocatedJDOMFactory());
		InputStream inp = SchemaWrapper.class.getResourceAsStream(rsrcPath);
		if (inp == null) {
			// Unsupported version of an MDDF Schema
			return null;
		}
		try {
			InputStreamReader isr = new InputStreamReader(inp, "UTF-8");
			Document schemaDoc = builder.build(isr);
			return schemaDoc;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private SchemaWrapper(String xsdRsrc) {
		schemaXSD = getSchemaXSD(xsdRsrc);
		if (schemaXSD == null) {
			throw new IllegalArgumentException("XSD for " + xsdRsrc + " is not an available resource");
		}
		rootEl = schemaXSD.getRootElement();
		String targetNamespace = rootEl.getAttributeValue("targetNamespace");
		String[] parts = xsdRsrc.split("-v");
		String prefix = parts[0];
		nSpace = Namespace.getNamespace(prefix, targetNamespace);

		/* get the associated wrapper for the md namespace */
		List<Namespace> nSpaceList = rootEl.getAdditionalNamespaces();
		for (Namespace subSpace : nSpaceList) {
			String subPrefix = subSpace.getPrefix();
			if (!subPrefix.equals(prefix)) {
				String subURI = subSpace.getURI();
				String schemaPrefix = MddfContext.SCHEMA_PREFIX + subPrefix + "/v";
				String schemaVer = subURI.replace(schemaPrefix, "");
				schemaVer = schemaVer.replace("/" + subPrefix, "");
				String xsdForSub = subPrefix + "-v" + schemaVer;
				SchemaWrapper subWrapper = SchemaWrapper.factory(xsdForSub);
				if (subWrapper != null) {
					otherSchemas.put(subPrefix, subWrapper);
				}
			}
		}
		// can now process
		buildReqElList();
	}

	/**
	 * Return a JSON-encoded description of a type. The description is
	 * <i>normailzed</i> in that while there are often multiple ways in
	 * <tt>XSD</tt> to specify a given semantic structure, the JSON returned
	 * will always may equivalent XSD to the same JSON semantics.
	 * </p>
	 * <p>
	 * For example, an <tt>&lt;xs:sequence&gt;</tt> may incorporate a
	 * <tt>&lt;xs:complexType&gt;</tt> in one of two ways:
	 * <ul>
	 * <li>as a referenced to a <i>named</i>; e.g.,
	 * 
	 * <tt><pre>
	 &lt;xs:complexType name="Foo-type"&gt;
		 &lt;xs:sequence&gt;
			&lt;xs:element name="FooBarType" <b>type="md:string-FooBarType"/&gt;</b>
			           :
	 * </pre></tt></li>
	 * 
	 * <li>as an in-line <i>anonymous</i> definition; e.g.,
	 * 
	 * <tt><pre>
	&lt;xs:complexType name="Foo-type"&gt;
		&lt;xs:sequence&gt;
			&lt;xs:element name="FooBarType"&gt;
				&lt;xs:complexType&gt;
					&lt;xs:simpleContent&gt;
						&lt;xs:extension base="xs:string"/&gt;
					&lt;/xs:simpleContent&gt;
				&lt;/xs:complexType&gt;
			           :
	 * </pre></tt></li>
	 * </ul>
	 * Both representations will return identical JSON: <tt><pre>
	"FooBarType":    {
	  "prefix": "avails",
	  "name": "FooBarType",
	  "type": "string",
	  "nspace": "xs"
	},</pre></tt>
	 * </p>
	 * 
	 * <p>
	 * The description is also <i>shallow</i> in the sense that it is only one
	 * level deep (i.e., it includes only the immediate child elements and
	 * attributes of an XML element of the specified type). Deeper descendants
	 * must be resolved by invoking <tt>getContentStructure()</tt> for each
	 * immediate child's type.
	 * </p>
	 * 
	 * @param type
	 * @return
	 */
	public JSONObject getContentStructure(String type) throws SchemaException {
		Element target = getXmlTarget(type);
		if (target == null) {
			// not a complex type
			return null;
		}
		Element seqEl = target.getChild("sequence", xsNSpace);
		if (seqEl != null) {
			return addSequenceToStructure(seqEl);
		}
		Element ccEl = target.getChild("complexContent", xsNSpace);
		if (ccEl != null) {
			return addExtensionToStructure(ccEl);
		}
		Element ctEl = target.getChild("complexType", xsNSpace);
		if (ctEl != null) {
			ccEl = ctEl.getChild("complexContent", xsNSpace);
			if (ccEl != null) {
				return addExtensionToStructure(ccEl);
			}
		}
		Element choiceEl = target.getChild("choice", xsNSpace);
		if (choiceEl != null) {
			return addChoiceToStructure(choiceEl);
		}
		Element scEl = target.getChild("simpleContent", xsNSpace);
		if (scEl != null) {
			return addExtensionToStructure(scEl);
		}
		return null;
	}

	private Element getXmlTarget(String type) {
		XPathExpression<Element> xpExp1 = xpfac.compile("./xs:complexType[@name= '" + type + "']", Filters.element(),
				null, xsNSpace);
		Element target = xpExp1.evaluateFirst(rootEl);
		if (target == null) {
			// check for an anonymous in-line definition
			XPathExpression<Element> xpExp2 = xpfac.compile("//xs:element[@name='" + type + "']/xs:complexType",
					Filters.element(), null, xsNSpace);
			target = xpExp2.evaluateFirst(rootEl);
		}
		return target;
	}

	/**
	 * @param contentEl
	 * @return
	 * @throws SchemaException 
	 */
	private JSONObject addExtensionToStructure(Element contentEl) throws SchemaException {
		Element extEl = contentEl.getChild("extension", xsNSpace);
		String base = extEl.getAttributeValue("base");
		String[] parts = base.split(":");
		JSONObject baseContent = new JSONObject();
		String baseNSpace = parts[0];
		String baseType = parts[1];
		if (!baseNSpace.equals("xs")) {
			SchemaWrapper baseWrapper;
			if (nSpace.getPrefix().equals(baseNSpace)) {
				baseWrapper = this;
			} else {
				baseWrapper = otherSchemas.get(baseNSpace);
			}
			JSONObject extContent = baseWrapper.getContentStructure(baseType);
			if (extContent != null) {
				baseContent = extContent;
			} else {
				boolean isST = baseWrapper.isSimpleType(baseType);
				String[] tType = baseWrapper.resolveType(base);
				baseContent.put("prefix", nSpace.getPrefix());
				baseContent.put("name", base.replaceAll("-type", ""));
				baseContent.put("isAtt", false);
				baseContent.put("type", tType[1]);
				baseContent.put("nspace", tType[0]);
				baseContent.put("min", 1);
				baseContent.put("max", 1);
			}
		} else {
			String name = "base (t.b.d)";
			baseContent.put("prefix", nSpace.getPrefix());
			baseContent.put("name", name);
			baseContent.put("isAtt", false);
			baseContent.put("type", baseType);
			baseContent.put("nspace", baseNSpace);
			baseContent.put("min", 1);
			baseContent.put("max", 1);
		}

		// now look for any extension in the form of a <xs:sequence>
		Element seqEl = extEl.getChild("sequence", xsNSpace);
		if (seqEl != null) {
			JSONObject additionalContent = addSequenceToStructure(seqEl);
			if (additionalContent != null) {
				/*
				 * Merge. Note that 'attributes' are a special case
				 */
				Iterator keyIt = additionalContent.keys();
				while (keyIt.hasNext()) {
					String childName = (String) keyIt.next();
					if (childName.equals("attributes") && baseContent.containsKey("attributes")) {
						JSONObject attSet1 = additionalContent.getJSONObject("attributes");
						JSONObject attSet2 = baseContent.getJSONObject("attributes");
						Iterator attIt = attSet1.keys();
						while (attIt.hasNext()) {
							String nextAtt = (String) attIt.next();
							// check for and filter out any duplicates..
							if (!attSet2.containsKey(nextAtt)) {
								JSONObject attDef = attSet1.getJSONObject(nextAtt);
								attSet2.put(nextAtt, attDef);
							}
						}
					} else {
						JSONObject childDef = additionalContent.getJSONObject(childName);
						baseContent.put(childName, childDef);
					}
				}
			}
		}
		// now look for any extension in the form of a <xs:attribute>
		addAttributesToStructure(extEl, baseContent);

		return baseContent;
	}

	/**
	 * Resolve structure of a <tt>&lt;xs:complexType&gt;</tt> that is comprised
	 * of a <tt>&lt;xs:sequence&gt;</tt>. The only assumption is that the
	 * <tt>seqEl</tt> is the direct child of the <tt>&lt;xs:complexType&gt;</tt>
	 * element.
	 * 
	 * @param seqEl
	 * @return
	 * @throws SchemaException 
	 */
	private JSONObject addSequenceToStructure(Element seqEl) throws SchemaException {
		JSONObject seqStruct = new JSONObject();

		// First add any ATTRIBUTES
		Element parentEl = seqEl.getParentElement();
		addAttributesToStructure(parentEl, seqStruct);

		List<Element> childList = seqEl.getContent(new ElementFilter());
		kinderLoop: for (Element nextEl : childList) {
			String childType = nextEl.getName();
			String name = nextEl.getAttributeValue("name");
			switch (childType) {
			case "element":
				String min = nextEl.getAttributeValue("minOccurs", "1");
				int minVal = Integer.parseInt(min);
				String max = nextEl.getAttributeValue("maxOccurs", "1");
				if (max.equals("unbounded")) {
					max = "-1";
				}
				int maxVal = Integer.parseInt(max);
				JSONObject childObj = new JSONObject();
				/*
				 * NOTE: is subType comes back null it means we are dealing with
				 * a complexType or a simpleType which is a union or extension.
				 */
				String subtype = nextEl.getAttributeValue("type");
				String[] parts = new String[2];
				JSONObject additionalContent = null;
				if (subtype == null || subtype.isEmpty()) {
					Element ctEl = nextEl.getChild("complexType", xsNSpace);
					if (ctEl != null) {
						Element innerContent = ctEl.getChild("complexContent", xsNSpace);
						if (innerContent == null) {
							innerContent = ctEl.getChild("simpleContent", xsNSpace);
						}
						if (innerContent != null) {
							additionalContent = addExtensionToStructure(innerContent);
							if (additionalContent != null) {
								// merge
								Iterator keyIt = additionalContent.keys();
								while (keyIt.hasNext()) {
									Object key = keyIt.next();
									Object value = additionalContent.get(key);
									childObj.put(key, value);
								}
								if (additionalContent.containsKey("nspace")) {
									parts[0] = additionalContent.getString("nspace");
									parts[1] = additionalContent.getString("type");
								} else {
									String[] extType = resolveExtendedType(innerContent);
									if (extType != null) {
										parts = extType;
									}
								}
							}
						} else {
							/*
							 * Looking at simpleContent that adds an attribute
							 * to a base (e.g. add the boolean attribute
							 * 'scheduled' to extended the base 'xs:date'
							 */
							Element scEl = ctEl.getChild("simpleContent", xsNSpace);
							String[] extType = resolveExtendedType(scEl);
							if (extType != null) {
								parts = extType;
							}
						}
					} else {
						Element stEl = nextEl.getChild("simpleType", xsNSpace);
						if (stEl != null) {
							String[] extType = resolveExtendedType(stEl);
							if (extType != null) {
								parts = extType;
							}
						}
					}
					// continue kinderLoop;
				} else {
					parts = resolveType(subtype);
				}
				String baseNSpace = parts[0];
				String baseType = parts[1];
				childObj.put("prefix", nSpace.getPrefix());
				childObj.put("name", name);
				childObj.put("isAtt", false);
				childObj.put("type", baseType);
				childObj.put("nspace", baseNSpace);
				childObj.put("min", minVal);
				childObj.put("max", maxVal);

				SchemaWrapper baseWrapper = getReferencedSchema(baseNSpace);
				if (baseWrapper != null) {
					boolean isChoice = baseWrapper.isChoice(baseType);
					if (isChoice) {
						childObj.put("isChoice", isChoice);
						JSONObject baseStruct = baseWrapper.getContentStructure(baseType);
						childObj.put("baseChoice", baseStruct);
					}
				}

				seqStruct.put(JSON_KEY_PREFIX + name, childObj);
				break;
			case "choice":
				JSONObject choiceDef = addChoiceToStructure(nextEl);
				seqStruct.put(JSON_KEY_PREFIX + choiceDef.getString("id"), choiceDef);
				break;
			default:
			}
		}
		return seqStruct;
	}

	public String getType(String elementName) {
		Element target = getElement(elementName);
		if (target == null) {
			throw new IllegalArgumentException(
					"Schema '" + schemaXSD + "' does not define element '" + elementName + "'");
		}
		String type = target.getAttributeValue("type", "xs:string");
		/*
		 * WHAT ABOUT:::::> <xs:element name="Event"> <xs:simpleType> <xs:union
		 * memberTypes="xs:dateTime xs:date"/> </xs:simpleType> </xs:element>
		 */
		return type;
	}

	private void addAttributesToStructure(Element parentEl, JSONObject parentSeq) {
		List<Element> attElList = parentEl.getChildren("attribute", xsNSpace);
		JSONObject attSet;
		if (parentSeq.containsKey("attributes")) {
			attSet = parentSeq.getJSONObject("attributes");
		} else {
			attSet = new JSONObject();
		}
		for (Element nextAt : attElList) {
			String name = nextAt.getAttributeValue("name");
			int minVal = 0;
			String use = nextAt.getAttributeValue("required");
			if (use != null && use.equals("required")) {
				minVal = 1;
			}
			int maxVal = 1; // max is always 1 for an attribute
			String subtype = nextAt.getAttributeValue("type");
			// must be either an primitive (i.e. XS) type or a simpleType

			String[] parts = resolveType(subtype);
			String baseNSpace = parts[0];
			String baseType = parts[1];
			JSONObject childObj = new JSONObject();
			childObj.put("prefix", nSpace.getPrefix());
			childObj.put("name", name);
			childObj.put("isAtt", true);
			childObj.put("type", baseType);
			childObj.put("nspace", baseNSpace);
			childObj.put("min", minVal);
			childObj.put("max", maxVal);
			attSet.put(JSON_KEY_PREFIX + name, childObj);
		}
		parentSeq.put("attributes", attSet);
	}

	private JSONObject addChoiceToStructure(Element choiceEl) throws SchemaException {
		boolean allSimple = true;
		JSONObject choiceStruct = new JSONObject();
		List<Element> choiceList = choiceEl.getChildren("element", xsNSpace);
		for (Element nextEl : choiceList) {
			String name = nextEl.getAttributeValue("name");
			String type = nextEl.getAttributeValue("type");
			/*
			 * NOTE: is type comes back null it means we are dealing with a
			 * complexType or simpleType which is a union or extension.
			 */
			String[] parts = new String[2];
			if (type == null || type.isEmpty()) {
				Element ctEl = nextEl.getChild("complexType", xsNSpace);
				if (ctEl != null) {
					/*
					 * looking at complexContent or simpleContent that defines
					 * an extension.
					 */
					Element targetEl = ctEl.getChild("complexContent", xsNSpace);
					if (targetEl == null) {
						targetEl = ctEl.getChild("simpleContent", xsNSpace);
					}
					if(targetEl == null){
						String msg = "Unsupportable complexType; nSpace="+nSpace.getPrefix()+", line="+((LocatedElement) ctEl).getLine();
						throw new SchemaException(msg);
					}
					String[] extType = resolveExtendedType(targetEl);
					if (extType != null) {
						parts = extType;
					}

				} else {
					// sinpleType???
					Element stEl = nextEl.getChild("simpleType", xsNSpace);
					if (stEl != null) {
						// KLUDGE!!!
						parts[0] = "xs";
						parts[1] = "string";
						// TODO: complete code to handle union or restriction
					}
				}
			} else {
				parts = resolveType(type);
			}

			String baseNSpace = parts[0];
			String baseType = parts[1];
			SchemaWrapper baseWrapper =  getReferencedSchema(baseNSpace); 
			if (baseWrapper != null) {
				if (!baseWrapper.isSimpleType(baseType)) {
					allSimple = false;
				}
			}
			JSONObject childObj = new JSONObject();
			childObj.put("prefix", nSpace.getPrefix());
			childObj.put("name", name);
			childObj.put("type", baseType);
			childObj.put("nspace", baseNSpace);
			choiceStruct.put(name, childObj);
		}
		/*
		 * Is this an 'anonymous' (i.e. inline) definition or does it have a
		 * name of it's own?
		 */
		Element parentEl = choiceEl.getParentElement();
		String baseType = parentEl.getAttributeValue("name");
		if (baseType == null) {
			// generate a name using the names of the choices...
			List<String> optNames = new ArrayList<String>();
			optNames.addAll(choiceStruct.keySet());
			String bestName = StringUtils.longestSubstring(optNames, false);
			baseType = "_ANON_";
			choiceStruct.put("id", bestName + " Option");
		} else {
			choiceStruct.put("id", baseType);
		}
		choiceStruct.put("type", baseType);
		choiceStruct.put("nspace", nSpace.getPrefix());
		choiceStruct.put("isChoice", true);
		choiceStruct.put("allSimple", allSimple);
		return choiceStruct;
	}

	/**
	 * @param type
	 * @return
	 */
	private String[] resolveType(String type) {
		String[] parts = type.split(":");
		/*
		 * IFF the subtype's nspace is NOT 'xs' then check for a simpleType that
		 * is a restriction on a xs type. We treat it (for now) as in effect an
		 * alias for the primitive.
		 */
		String baseNSpace = parts[0];
		String baseType = parts[1];

		SchemaWrapper baseWrapper = getReferencedSchema(baseNSpace);
		if (baseWrapper != null) {
			if (baseWrapper.isSimpleType(baseType)) {
				String trueType = baseWrapper.resolveRestrictionBase(baseType);
				parts = trueType.split(":");
				baseNSpace = parts[0];
				baseType = parts[1];
			}
		}
		String[] actual = new String[2];
		actual[0] = baseNSpace;
		actual[1] = baseType;
		return actual;
	}

	private String[] resolveExtendedType(Element contentEl) throws SchemaException {
		Element extEl = contentEl.getChild("extension", xsNSpace);
		if (extEl != null) {
			String base = extEl.getAttributeValue("base");
			String[] parts = base.split(":");
			String baseNSpace = parts[0];

			SchemaWrapper baseWrapper = getReferencedSchema(baseNSpace);
			if (baseWrapper != null) {
				String baseType = parts[1];
				JSONObject extContent = baseWrapper.getContentStructure(baseType);
				if (extContent != null && extContent.containsKey("nspace")) {
					parts[0] = extContent.getString("nspace");
					parts[1] = extContent.getString("type");
				} else {
					parts = baseWrapper.resolveType(base);
				}
			}
			return parts;
		}
		// Argh! must by a xs:union, e.g.:
		// <xs:simpleType>
		// .....<xs:union memberTypes="xs:gYear xs:gYearMonth xs:date"/>
		// </xs:simpleType>
		/*
		 * default for now is to treat as a string..
		 */

		String[] parts = new String[2];
		parts[0] = "xs";
		parts[1] = "string";
		return parts;
	}

	/**
	 * Identify the <i>base type</i> that is used to define a
	 * <tt>&lt;xs:simpleType&gt;</tt>.
	 * <p>
	 * A <tt>simpleType</tt> is a restriction on a base type. The restriction
	 * may be in the form of a <i>pattern</i>, <i>enumeration</i>, or
	 * <i>union</i>. It may also be non-existent (i.e, missing).
	 * </p>
	 * <p>
	 * In the case of a <tt>union</tt> no explicit base is identified. The type
	 * returned is therefore 'xs:string' on the assumption that the member-types
	 * are all defining a specific pattern-based restriction on a xs:string
	 * (e.g.,
	 * <tt> &lt;xs:union memberTypes="xs:gYear xs:date xs:dateTime"/&gt;</tt>)
	 * </p>
	 * 
	 * @param referencedType
	 * @return
	 */
	private String resolveRestrictionBase(String referencedType) {
		XPathExpression<Element> xpExpression = xpfac.compile("./xs:simpleType[@name= '" + referencedType + "']",
				Filters.element(), null, xsNSpace);
		Element target = xpExpression.evaluateFirst(rootEl);
		Element baseEl = target.getChild("restriction", xsNSpace);
		if (baseEl == null) {
			/*
			 * must be a union.
			 */
			System.out.println("    SchemaWrapper " + nSpace.getPrefix()
					+ ": resolveTypeRestriction (found UNION ?) for:: " + referencedType);
			return "xs:string";
		}
		String restrictedType = baseEl.getAttributeValue("base");
		if (restrictedType.startsWith("xs:")) {
			return restrictedType;
		} else {
			String[] parts = restrictedType.split(":");
			String baseNSpace = parts[0];
			String baseType = parts[1];

			SchemaWrapper baseWrapper = getReferencedSchema(baseNSpace);
			if (baseWrapper.isSimpleType(baseType)) {
				String trueType = baseWrapper.resolveRestrictionBase(baseType);
				return trueType;
			}
		}
		return nSpace.getPrefix() + ":" + referencedType;
	}

	public boolean isChoice(String type) {
		Element target = getXmlTarget(type);
		if (target == null) {
			return false;
		}
		Element choiceEl = target.getChild("choice", xsNSpace);
		return (choiceEl != null);
	}

	/**
	 * @param type
	 * @return
	 */
	private boolean isSimpleType(String type) {
		XPathExpression<Element> xpExpression = xpfac.compile("./xs:simpleType[@name= '" + type + "']",
				Filters.element(), null, xsNSpace);
		Element target = xpExpression.evaluateFirst(rootEl);
		return (target != null);
	}

	/**
	 * @param elementName
	 * @return
	 * @throws IllegalArgumentException
	 *             if <tt>schema</tt> is unrecognized or <tt>elementName</tt> is
	 *             not defined by the <tt>schema</tt>
	 */
	public boolean isRequired(String elementName) throws IllegalStateException, IllegalArgumentException {
		Element target = getElement(elementName);
		if (target == null) {
			// TODO: Maybe its an attribute?
			throw new IllegalArgumentException(
					"Schema '" + schemaXSD + "' does not define element '" + elementName + "'");
		}
		String minVal = target.getAttributeValue("minOccurs", "1");
		return (!minVal.equals("0"));
	}

	private Element getElement(String elementName) {
		XPathExpression<Element> xpExpression = xpfac.compile(".//xs:element[@name='" + elementName + "']",
				Filters.element(), null, xsNSpace);
		Element target = xpExpression.evaluateFirst(rootEl);
		return target;
	}

	/**
	 * Return the <tt>SchemaWrapper</tt> instance associated with a namespace
	 * prefix. This is NOT the same as using <tt>SchemaWrapper.factory()</tt> to
	 * retrieve a <tt>SchemaWrapper</tt> as the factory method takes a full
	 * resource identifier as it's input, thus identifying explicitly a specific
	 * version of a mddf schema (e.g., "mdmec-v2.4"). This method, however,
	 * takes only a prefix (e.g., "mdmec") and then maps that to a specific
	 * version using the namespace declarations of the current
	 * <tt>SchemaWrapper's</tt> XSD (e.g.,
	 * <tt>xmlns:mdmec="http://www.movielabs.com/schema/mdmec/v2.4"</tt>).
	 * 
	 * @param nspacePrefix
	 * @return
	 */
	private SchemaWrapper getReferencedSchema(String nspacePrefix) {
		SchemaWrapper baseWrapper = null;
		if (!nspacePrefix.equals("xs")) {
			if (nSpace.getPrefix().equals(nspacePrefix)) {
				baseWrapper = this;
			} else {
				baseWrapper = otherSchemas.get(nspacePrefix);
			}
		}
		return baseWrapper;
	}

	private void buildReqElList() {
		reqElXpList = new ArrayList<XPathExpression<?>>();
		XPathExpression<Element> xpExpression = xpfac.compile(".//xs:element", Filters.element(), null, xsNSpace);
		List<Element> elementList = xpExpression.evaluate(rootEl);
		for (int i = 0; i < elementList.size(); i++) {
			String targetXPath = null;
			Element target = (Element) elementList.get(i);
			String name = target.getAttributeValue("name", "FOOBAR");
			if (name.equalsIgnoreCase("ALID")) {
				// DEBUG trap
				int xyz = 0;
			}
			String minVal = target.getAttributeValue("minOccurs", "1");
			if (minVal.equals("0")) {
				// ignore optional elements
				continue;
			}
			/*
			 * Note an Element that is a complexType will not have the type
			 * attribute so will return a null.
			 */
			String type = target.getAttributeValue("type");
			boolean process = false;
			if (type == null) {
				/* only <xs:simpleContent> gets processed */
				Element el1 = target.getChild("complexType", xsNSpace);
				if (el1 != null) {
					Element el2 = el1.getChild("simpleContent", xsNSpace);
					process = (el2 != null);
				}
			} else if (type.startsWith("xs:")) {
				process = true;
			} else {
				// is it simpleType in a referenced schema?
				String typePrefix = type.split(":")[0];
				SchemaWrapper other = otherSchemas.get(typePrefix);
				if (other != null) {
					String typeName = type.replaceFirst(typePrefix + ":", "");
					process = other.isSimpleType(typeName);
				}
			}
			if (process) {
				/*
				 * parent may be null it target is not part of a sequence w/in a
				 * complexType
				 */
				Element parent = getNamedAncestor(target);
				if (parent == null) {
					targetXPath = ".//" + getPrefix() + ":" + name;
				} else {
					/* find all element declarations with the parent type */
					String parentType = parent.getAttributeValue("name");
					String xp = ".//xs:element[@type='" + getPrefix() + ":" + parentType + "']";
					XPathExpression<Element> xpe2 = xpfac.compile(xp, Filters.element(), null, xsNSpace);
					List<Element> referencingList = xpe2.evaluate(rootEl);
					for (Element refEl : referencingList) {
						String parentName = refEl.getAttributeValue("name");
						targetXPath = ".//" + getPrefix() + ":" + parentName + "/" + getPrefix() + ":" + name;
					}
				}
				if (targetXPath != null) {
					XPathExpression<Element> targetXpE = xpfac.compile(targetXPath, Filters.element(), null, nSpace);
					reqElXpList.add(targetXpE);
				}
			}
		}
		// add required attributes...
		xpExpression = xpfac.compile(".//xs:attribute[@use='required']", Filters.element(), null, xsNSpace);
		elementList = xpExpression.evaluate(rootEl);
		for (int i = 0; i < elementList.size(); i++) {
			String targetXPath = null;
			Element target = (Element) elementList.get(i);
			String attName = target.getAttributeValue("name");
			/*
			 * need the parent Element's name which can be complicated since the
			 * XSD can have the attribute (a) in a referenced complex type, (b)
			 * in an xs:extension, or (c) nested in other intermediate stuff.
			 * 
			 */
			Element parent = getNamedAncestor(target);
			if (parent.getName().contains("element")) {
				String elName = parent.getAttributeValue("name");
				targetXPath = ".//" + getPrefix() + ":" + elName + "/@" + attName;
			} else {
				// dealing with a complex-type so its more indirect
				String typeName = parent.getAttributeValue("name");
				xpExpression = xpfac.compile(".//xs:element[contains(@type,'" + typeName + "')]", Filters.element(),
						null, xsNSpace);
				List<Element> innerList = xpExpression.evaluate(rootEl);
				for (int j = 0; j < innerList.size(); j++) {
					Element ownerEl = (Element) innerList.get(j);
					String elName = ownerEl.getAttributeValue("name");
					targetXPath = ".//" + getPrefix() + ":" + elName + "/@" + attName;
				}
			}
			if (targetXPath != null) {
				XPathExpression<Attribute> targetXpE = xpfac.compile(targetXPath, Filters.attribute(), null, nSpace);
				reqElXpList.add(targetXpE);
			}
		}
	}

	/**
	 * @param target
	 * @return
	 */
	private Element getNamedAncestor(Element target) {
		Element next = target;
		Element parent = next.getParentElement();
		while (parent != null) {
			if (parent.getAttribute("name") != null) {
				return parent;
			}
			next = parent;
			parent = next.getParentElement();
		}
		return null;
	}

	/**
	 * @return the reqElList
	 */
	public ArrayList<XPathExpression<?>> getReqElList() {
		return reqElXpList;
	}

	/**
	 * @return the targetNamespace
	 */
	public Namespace getTargetNamespace() {
		return nSpace;
	}

	/**
	 * @return the prefix
	 */
	public String getPrefix() {
		return nSpace.getPrefix();
	}

	public static void test(String schema, String type) {
		SchemaWrapper sw = factory(schema);
		JSONObject def=null;
		try {
			def = sw.getContentStructure(type);
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (def != null) {
			System.out.println("\n~~~~~~~~~~~\n" + def.toString(4) + "\n================\n");
		}
	}
}
