package wsdarwin.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import wsdarwin.model.*;
import wsdarwin.wadlgenerator.model.*;
import wsdarwin.wadlgenerator.model.xsd.*;

import java.io.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class XMLGenerator {

	public static final String XML_SCHEMA_NAMESPACE = "xs:";
	public static final String TARGET_SCHEMA_NAMESPACE = "tns:";

	private void createXSD(XSDFile xsdFile, Document xmldoc,
			Element schemaElement) throws IOException,
			ParserConfigurationException {

		HashMap<String, XSDElement> elements = xsdFile.getElements();
		HashMap<String, XSDIType> types = new HashMap<String, XSDIType>();
		for(XSDElement element : elements.values()) {
			types.put(element.getType().getIdentifier(), element.getType());
		}
		for (String name : elements.keySet()) {
			Element element = xmldoc.createElement(XML_SCHEMA_NAMESPACE
					+ "element");
			element.setAttribute("name", elements.get(name).getName());
			if (types.containsKey(
					elements.get(name).getType().getName())) {
				element.setAttribute("type", TARGET_SCHEMA_NAMESPACE
						+ elements.get(name).getType());
				// System.out.println("(1) Element: " +
				// element.getAttribute("name") + ", type: " +
				// element.getAttribute("type"));
			} else {
				element.setAttribute("type", XML_SCHEMA_NAMESPACE
						+ elements.get(name).getType());
			}
			if (elements.get(name).getMaxOccurs() != null) {
				element.setAttribute("minOccurs", ""
						+ elements.get(name).getMinOccurs());
				element.setAttribute("maxOccurs", ""
						+ elements.get(name).getMaxOccurs());
			}
			schemaElement.appendChild(element);
			if (types.get(elements.get(name).getType().getName()) instanceof XSDComplexType) {
				XSDComplexType type = (XSDComplexType) types.get(elements.get(name).getType().getName());
				Element complexType = xmldoc.createElement(XML_SCHEMA_NAMESPACE
						+ "complexType");
				complexType.setAttribute("name", types.get(elements.get(name).getType().getName()).getName());
				Element sequence = xmldoc.createElement(XML_SCHEMA_NAMESPACE
						+ "sequence");
				complexType.appendChild(sequence);
				for (String xsdElementName : type.getElements().keySet()) {
					XSDElement xsdElement = type.getElements().get(
							xsdElementName);
					Element childElement = xmldoc
							.createElement(XML_SCHEMA_NAMESPACE + "element");
					childElement.setAttribute("name", xsdElement.getName());
					if (types.containsKey(
							xsdElement.getType().getName())) {
						childElement.setAttribute("type",
								TARGET_SCHEMA_NAMESPACE
										+ xsdElement.getType().getName());
						// System.out.println("(2) Element: " +
						// childElement.getAttribute("name") + ", type: " +
						// childElement.getAttribute("type"));

					} else {
						childElement.setAttribute("type", XML_SCHEMA_NAMESPACE
								+ xsdElement.getType().getName());
					}
					if (xsdElement.getMaxOccurs() != null) {
						childElement.setAttribute("minOccurs",
								"" + xsdElement.getMinOccurs());
						childElement.setAttribute("maxOccurs",
								"" + xsdElement.getMaxOccurs());
					}
					if (xsdElement.getType() instanceof XSDPrimitiveType) {
						Map<Object, Integer> valueFrequencies = xsdElement
								.getValueFrequencies();
						Map<String, Integer> typeFrequencies = xsdElement
								.getTypeFrequencies();
						//appendValueAndTypeFrequencies(xmldoc, childElement,valueFrequencies, typeFrequencies);
					}
					sequence.appendChild(childElement);
				}
				schemaElement.appendChild(complexType);
			} else if (types.get(elements.get(name).getType().getName()) instanceof XSDSimpleType) {
				XSDSimpleType type = (XSDSimpleType) types.get(elements.get(name).getType().getName());
				Element simpleType = xmldoc.createElement(XML_SCHEMA_NAMESPACE
						+ "simpleType");
				simpleType.setAttribute("name", types.get(elements.get(name).getType().getName()).getName());
				if (type.getList() != null) {
					Element list = xmldoc.createElement(XML_SCHEMA_NAMESPACE
							+ "list");
					if (types.containsKey(
							type.getList().getItemType().getName())) {
						list.setAttribute("itemType", TARGET_SCHEMA_NAMESPACE
								+ type.getList().getItemType().getName());
						System.out.println("(3) list Element: "
								+ list.getAttribute("name") + ", type: "
								+ list.getAttribute("itemType"));
					} else {
						list.setAttribute("itemType", XML_SCHEMA_NAMESPACE
								+ type.getList().getItemType().getName());
					}
					simpleType.appendChild(list);
				} else if (type.getRestrictionBase() != null) {
					Element base = xmldoc.createElement(XML_SCHEMA_NAMESPACE
							+ "restriction");
					base.setAttribute("base",
							TARGET_SCHEMA_NAMESPACE + type.getRestrictionBase());
					// System.out.println("(4) base Element: " +
					// base.getAttribute("base"));
					simpleType.appendChild(base);
					for (String enume : type.getEnumerations()) {
						Element enumeration = xmldoc
								.createElement(XML_SCHEMA_NAMESPACE
										+ "enumeration");
						enumeration.setAttribute("value", enume);
						base.appendChild(enumeration);
					}
				}
				schemaElement.appendChild(simpleType);
			}
		}
	}

	public static void writeXML(DOMImplementation domImpl, Document xmldoc,
			String filename) throws FileNotFoundException {
		DOMImplementationLS ls = (DOMImplementationLS) domImpl;
		LSSerializer lss = ls.createLSSerializer();
		LSOutput lso = ls.createLSOutput();
		lso.setByteStream(new FileOutputStream(new File(filename)));

		lss.write(xmldoc, lso);

		// Element root = xmldoc.get
		// System.out.println("root name ? " + getStringFromDocument(xmldoc) );
		// String jsonRet = gson.toJson( getStringFromDocument(xmldoc) );
		// System.out.println("THE FINAL XML: " + xmldoc.toString() );
	}

	/*
	 * //method to convert Document to String public String
	 * getStringFromDocument(Document doc) { try { DOMSource domSource = new
	 * DOMSource(doc); StringWriter writer = new StringWriter(); StreamResult
	 * result = new StreamResult(writer); TransformerFactory tf =
	 * TransformerFactory.newInstance(); Transformer transformer =
	 * tf.newTransformer(); transformer.transform(domSource, result); return
	 * writer.toString(); } catch(TransformerException ex) {
	 * ex.printStackTrace(); return null; } }
	 */

	public Document createWADL(WADLFile wadlFile, String base) throws IOException,
			ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl = builder.getDOMImplementation();

		/* Application = root element */
		// TODO The namespaces are still implemented incorrectly. We will leave
		// it for now and change it later.

		/*
		 * ArrayList<String> rootArray = new ArrayList<String>();
		 * rootArray.add(new String("First top")); rootArray.add(new
		 * String("Second top"));
		 * 
		 * ArrayList<String> two = new ArrayList<String>(); two.add(new
		 * String("hello")); two.add(new String("mellow"));
		 * 
		 * 
		 * 
		 * Gson gson = new GsonBuilder().setPrettyPrinting().create();
		 * System.out.println("json: " + gson.toJson(rootArray) );
		 * rootArray.add(two);
		 * 
		 * System.out.println("json: " + gson.toJson(rootArray) );
		 */

		Document xmldoc = domImpl.createDocument(
				"http://wadl.dev.java.net/2009/02", "application", null);
		Element root = xmldoc.getDocumentElement();
		// root.setAttribute("tns:schemaLocation",
		// "http://wadl.dev.java.net/2009/02 "+xsdFile.getFilename());
		
		root.setAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema");

		System.out.println("element attt is " + root + ", v: "
				+ root.getAttribute("xmlns:xs").toString());

		/* Grammars */
		//Grammars grammars = wadlFile.getGrammarsElements();
		Element grammarsElement = xmldoc.createElement("grammars");
		Element schemaElement = xmldoc.createElement(XML_SCHEMA_NAMESPACE
				+ "schema");
		if(base.startsWith("{")) {
			base = base.replace("{", "");
			base = base.replace("}", "");
		}
		schemaElement.setAttribute("targetNamespace", base);
		schemaElement.setAttribute("xmlns:tns", base);
		XSDFile xsdFile = wadlFile.getSchema();
			// System.out.println("-> xsd File: " + xsdFile);
		createXSD(xsdFile, xmldoc, schemaElement);
		grammarsElement.appendChild(schemaElement);
		root.appendChild(grammarsElement);

		/* Resources */
		HashMap<String, Resources> resources = wadlFile.getResourcesElements();
		for (Resources r : resources.values()) {
			Element resourcesElement = xmldoc.createElement("resources");
			resourcesElement.setAttribute("base", r.getIdentifier());
			root.appendChild(resourcesElement);

			HashSet<Resource> resource = new HashSet<Resource>();
			resource.addAll(r.getResourceElements().values());

			addResourceElements(resource, resourcesElement, xmldoc);
		}

		writeXML(domImpl, xmldoc, wadlFile.getIdentifier());

		return xmldoc;
	}

	private void addResourceElements(HashSet<Resource> resource,
			Element resourcesElement, Document xmldoc) {
		for (Resource rr : resource) {
			Element resourceElement = xmldoc.createElement("resource");
			resourceElement.setAttribute("path", rr.getIdentifier());
			//resourceElement.setAttribute("hasVariableID","" + rr.hasVariableID());
			resourcesElement.appendChild(resourceElement);
			
			HashSet<Param> params = new HashSet<Param>();
			params.addAll(rr.getParamElements().values());
			for(Param p : params) {
				Element paramElement = xmldoc.createElement("param");
				paramElement.setAttribute("id", p.getIdentifier());
				paramElement.setAttribute("style", p.getStyle());
				resourceElement.appendChild(paramElement);
			}

			HashSet<Resource> resourceResources = new HashSet<Resource>();
			resourceResources.addAll(rr.getResourceElements().values());
			addResourceElements(resourceResources, resourceElement, xmldoc);

			HashSet<Method> methods = new HashSet<Method>();
			methods.addAll(rr.getMethodElements().values());
			for (Method m : methods) {
				Element methodElement = xmldoc.createElement("method");
				if (!m.getIdentifier().equals("")) {
					methodElement.setAttribute("id", m.getIdentifier());
				}
				methodElement.setAttribute("name", m.getName());
				resourceElement.appendChild(methodElement);

				/* Request (limited to 1) */
				Request request = m.getRequestElement();
				Element requestElement = xmldoc.createElement("request");
				methodElement.appendChild(requestElement);

				HashSet<Param> parameters = new HashSet<Param>();
				parameters.addAll(request.getParamElements().values());
				for (Param par : parameters) {
					Element paramElement = xmldoc.createElement("param");
					paramElement.setAttribute("style", par.getStyle());
					paramElement.setAttribute("type", XML_SCHEMA_NAMESPACE
							+ par.getType());
					paramElement.setAttribute("name", par.getIdentifier());
					HashMap<Object, Integer> valueFrequencies = par
							.getValueFrequencies();
					HashMap<String, Integer> typeFrequencies = par
							.getTypeFrequencies();
					//appendValueAndTypeFrequencies(xmldoc, paramElement,valueFrequencies, typeFrequencies);
					requestElement.appendChild(paramElement);
				}

				/* Response (multiple possible) */
				HashSet<Response> responses = new HashSet<Response>();
				responses.addAll(m.getResponseElements().values());
				for (Response resp : responses) {
					Element responseElement = xmldoc.createElement("response");
					responseElement.setAttribute("status",
							Integer.toString(resp.getID()));
					methodElement.appendChild(responseElement);

					HashSet<Representation> representation = new HashSet<Representation>();
					representation.addAll(resp.getRepresentationElements()
							.values());
					for (Representation rep : representation) {
						Element representationElement = xmldoc
								.createElement("representation");
						representationElement.setAttribute("element",
								rep.getIdentifier());
						representationElement.setAttribute("mediaType",
								rep.getMediaType());
						responseElement.appendChild(representationElement);
					}
				}
			}
		}

	}

	private void appendValueAndTypeFrequencies(Document xmldoc,
			Element parentElement, Map<Object, Integer> valueFrequencies,
			Map<String, Integer> typeFrequencies) {
		Element valueFrequenciesElement = xmldoc
				.createElement("valueFrequencies");
		for (Object value : valueFrequencies.keySet()) {
			Element valueElement = xmldoc.createElement("vf");
			valueElement.setAttribute("value", value.toString());
			valueElement.setAttribute("frequency",
					"" + valueFrequencies.get(value));
			valueFrequenciesElement.appendChild(valueElement);
		}
		parentElement.appendChild(valueFrequenciesElement);
		Element typeFrequenciesElement = xmldoc
				.createElement("typeFrequencies");
		for (String type : typeFrequencies.keySet()) {
			Element typeElement = xmldoc.createElement("tf");
			typeElement.setAttribute("type", type);
			typeElement.setAttribute("frequency",
					"" + typeFrequencies.get(type));
			typeFrequenciesElement.appendChild(typeElement);
		}
		parentElement.appendChild(typeFrequenciesElement);
	}
}