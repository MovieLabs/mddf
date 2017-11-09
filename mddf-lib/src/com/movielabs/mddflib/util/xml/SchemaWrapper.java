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
					// System.out.println("SchemaWrapper.factory(): Exception
					// for " + xsdRsrc);
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
	 * <i>shallow</i> in the sense that it is only one level deep (i.e., it
	 * includes only the immediate child elements and attributes of an XML
	 * element of the specified type). Deeper descendants must be resolved by
	 * invoking <tt>getContentStructure()</tt> for each immediate child's type.
	 * 
	 * @param type
	 * @return
	 */
	public JSONObject getContentStructure(String type) {
		System.out.println("\n\nSchemaWrapper " + nSpace.getPrefix() + ": Looking for " + type);
		XPathExpression<Element> xpExpression = xpfac.compile("./xs:complexType[@name= '" + type + "']",
				Filters.element(), null, xsNSpace);
		Element target = xpExpression.evaluateFirst(rootEl);
		if (target == null) {
			// not a complex type
			System.out.println("             " + nSpace.getPrefix() + ": " + type + " not a complex type");
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
		return null;
	}

	/**
	 * @param contentEl
	 * @return
	 */
	private JSONObject addExtensionToStructure(Element contentEl) {
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
			baseContent = baseWrapper.getContentStructure(baseType);
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
				// merge
				Iterator keyIt = additionalContent.keys();
				while (keyIt.hasNext()) {
					String childName = (String) keyIt.next();
					JSONObject childDef = additionalContent.getJSONObject(childName);
					baseContent.put(childName, childDef);
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
	 */
	private JSONObject addSequenceToStructure(Element seqEl) {
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
						Element ccEl = ctEl.getChild("complexContent", xsNSpace);
						if (ccEl != null) {
							additionalContent = addExtensionToStructure(ccEl);
							if (additionalContent != null) {
								// merge
								Iterator keyIt = additionalContent.keys();
								while(keyIt.hasNext()){
									Object key = keyIt.next();
									Object value = additionalContent.get(key);
									childObj.put(key, value);
								}
								try {
									parts[0] = additionalContent.getString("nspace");
								} catch (Exception e) {
									System.out.println("DEBUG: Extension of " + subtype + "\n     "
											+ additionalContent.toString(3));
									e.printStackTrace();
								}
								parts[1] = additionalContent.getString("type");
							}
						} else {
							/*
							 * Looking at simpleContent that adds an attribute
							 * to a base (e.g. add the boolean attribute
							 * 'scheduled' to extended the base 'xs:date'
							 */
							Element scEl = ctEl.getChild("simpleContent", xsNSpace);
							additionalContent = addExtensionToStructure(scEl);
							if (additionalContent != null) {
								parts[0] = additionalContent.getString("nspace");
								parts[1] = additionalContent.getString("type");
							}
						}
					} else {
						Element stEl = nextEl.getChild("simpleType", xsNSpace);
						if (stEl != null) {
							additionalContent = addExtensionToStructure(stEl);
							if (additionalContent != null) {
								parts[0] = additionalContent.getString("nspace");
								parts[1] = additionalContent.getString("type");
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
				seqStruct.put(name, childObj);
				break;
			case "choice":
				JSONObject choiceDef = addChoiceToStructure(nextEl);
				seqStruct.put(choiceDef.getString("id"), choiceDef);
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
			parentSeq.put(name, childObj);
		}
	}

	private JSONObject addChoiceToStructure(Element choiceEl) {
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
			JSONObject additionalContent = null;
			if (type == null || type.isEmpty()) {
				Element ctEl = nextEl.getChild("complexType", xsNSpace);
				if (ctEl != null) {
					Element ccEl = ctEl.getChild("complexContent", xsNSpace);
					if (ccEl != null) {
						additionalContent = addExtensionToStructure(ccEl);
						if (additionalContent != null) {
							parts[0] = additionalContent.getString("nspace");
							parts[1] = additionalContent.getString("type");
						}
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
			if (!baseNSpace.equals("xs")) {
				SchemaWrapper baseWrapper;
				if (nSpace.getPrefix().equals(baseNSpace)) {
					baseWrapper = this;
				} else {
					baseWrapper = otherSchemas.get(baseNSpace);
				}
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
		// System.out.println("resolveType::" + type);
		String[] parts = type.split(":");
		/*
		 * IFF the subtype's nspace is NOT 'xs' then check for a simpleType that
		 * is a restriction on a xs type. We treat it (for now) as in effect an
		 * alias for the primitive.
		 */
		String baseNSpace = parts[0];
		String baseType = parts[1];
		if (!baseNSpace.equals("xs")) {
			SchemaWrapper baseWrapper;
			if (nSpace.getPrefix().equals(baseNSpace)) {
				baseWrapper = this;
			} else {
				baseWrapper = otherSchemas.get(baseNSpace);
			}
			if (baseWrapper.isSimpleType(baseType)) {
				String trueType = baseWrapper.resolveTypeRestriction(baseType);
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

	/**
	 * @param baseType
	 * @return
	 */
	private String resolveTypeRestriction(String type) {
		XPathExpression<Element> xpExpression = xpfac.compile("./xs:simpleType[@name= '" + type + "']",
				Filters.element(), null, xsNSpace);
		Element target = xpExpression.evaluateFirst(rootEl);
		Element baseEl = target.getChild("restriction", xsNSpace);
		if (baseEl == null) {
			// must be a union
			System.out.println("    SchemaWrapper " + nSpace.getPrefix()
					+ ": resolveTypeRestriction found UNION(?) for:: " + type);
			return nSpace.getPrefix() + ":" + type;
		}
		String restrictedType = baseEl.getAttributeValue("base");
		if (restrictedType.startsWith("xs:")) {
			return restrictedType;
		} else {
			String[] parts = restrictedType.split(":");
			String baseNSpace = parts[0];
			String baseType = parts[1];
			SchemaWrapper baseWrapper;
			if (nSpace.getPrefix().equals(baseNSpace)) {
				baseWrapper = this;
			} else {
				baseWrapper = otherSchemas.get(baseNSpace);
			}
			if (baseWrapper.isSimpleType(baseType)) {
				String trueType = baseWrapper.resolveTypeRestriction(baseType);
				return trueType;
			}
		}
		System.out.println(
				"    SchemaWrapper " + nSpace.getPrefix() + ": resolveTypeRestriction unable to resolve:: " + type);
		return nSpace.getPrefix() + ":" + type;
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

}
