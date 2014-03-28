package wsdarwin.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import wsdarwin.model.*;
import wsdarwin.wadlgenerator.model.*;

public class XMLGenerator {

	public static final String XML_SCHEMA_NAMESPACE = "xs:";
	public static final String TARGET_SCHEMA_NAMESPACE = "tns:";

	/*public void createXSD(XSDFile xsdFile) throws IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl = builder.getDOMImplementation();
		// Document (Xerces implementation only).
		Document xmldoc = domImpl.createDocument("http://www.w3.org/2001/XMLSchema", "xs:schema", null);
		// Root element.
		//Element root = xmldoc.createElementNS(XML_SCHEMA_NAMESPACE, "schema");
		//root.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
		Element root = xmldoc.getDocumentElement();
		root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:tns", new File(xsdFile.getFilename()).toURI().toString());
		root.setAttribute("targetNamespace", new File(xsdFile.getFilename()).toURI().toString());
		ArrayList<String> elementsAndTypes = xsdFile.sortedElementAndTypeNames();
		HashMap<String, XSDElement> elements = xsdFile.getElements();
		HashMap<String, IType> types = xsdFile.getTypes();
		for (String name : elementsAndTypes) {
			if (elements.containsKey(name)) {
				Element element = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"element");
				element.setAttribute("name", elements.get(name).getName());
				if (xsdFile.getTypes().containsKey(elements.get(name).getType().getName())) {
					element.setAttribute("type", TARGET_SCHEMA_NAMESPACE
							+ elements.get(name).getType());
				}
				else {
					element.setAttribute("type", XML_SCHEMA_NAMESPACE
							+ elements.get(name).getType());
				}
				if(elements.get(name).getMaxOccurs() != null) {
					element.setAttribute("minOccurs", ""+elements.get(name).getMinOccurs());
					element.setAttribute("maxOccurs", ""+elements.get(name).getMaxOccurs());
				}
				xmldoc.getDocumentElement().appendChild(element);
			}
			if (types.containsKey(name)) {
				if (types.get(name) instanceof ComplexType) {
					ComplexType type = (ComplexType)types.get(name);
					Element complexType = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"complexType");
					complexType.setAttribute("name", types.get(name).getName());
					Element sequence = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"sequence");
					complexType.appendChild(sequence);
					for (String xsdElementName : type.getElements()
							.keySet()) {
						XSDElement xsdElement = type.getElements()
								.get(xsdElementName);
						Element childElement = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"element");
						childElement.setAttribute("name", xsdElement.getName());
						if (xsdFile.getTypes().containsKey(xsdElement.getType().getName())) {
							childElement.setAttribute(
									"type",
									TARGET_SCHEMA_NAMESPACE
											+ xsdElement.getType());
							
						} else {
							childElement.setAttribute("type", XML_SCHEMA_NAMESPACE+xsdElement
									.getType().getName());
						}
						if(xsdElement.getMaxOccurs() != null) {
							childElement.setAttribute("minOccurs", ""+xsdElement.getMinOccurs());
							childElement.setAttribute("maxOccurs", ""+xsdElement.getMaxOccurs());
						}
						sequence.appendChild(childElement);
					}
					xmldoc.getDocumentElement().appendChild(complexType);
				}
				else if(types.get(name) instanceof SimpleType) {
					SimpleType type = (SimpleType)types.get(name);
					Element simpleType = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"simpleType");
					simpleType.setAttribute("name", types.get(name).getName());
					if(type.getList() != null) {
						Element list = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"list");
						if (xsdFile.getTypes().containsKey(type.getList().getItemType().getName())) {
							list.setAttribute("itemType",
									TARGET_SCHEMA_NAMESPACE
											+ type.getList().getItemType()
													.getName());
						}
						else {
							list.setAttribute("itemType",
									XML_SCHEMA_NAMESPACE
											+ type.getList().getItemType()
													.getName());
						}
						simpleType.appendChild(list);
					}
					else if(type.getRestrictionBase() != null) {
						Element base = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"restriction");
						base.setAttribute("base", TARGET_SCHEMA_NAMESPACE
								+ type.getRestrictionBase());
						simpleType.appendChild(base);
						for(String enume : type.getEnumerations()) {
							Element enumeration = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"enumeration");
							enumeration.setAttribute("value", enume);
							base.appendChild(enumeration);
						}
					}
					xmldoc.getDocumentElement().appendChild(simpleType);
				}
			}
		}
		//xmldoc.appendChild(root);
		
		 writeXML(domImpl, xmldoc, xsdFile.getFilename());
		
		//TransformerFactory transformerFactory = TransformerFactory.newInstance();
		//Transformer transformer = transformerFactory.newTransformer();
		//DOMSource source = new DOMSource(xmldoc);
		//StreamResult result = new StreamResult(new File(filename));
		//transformer.transform(source, result);
		FileOutputStream fos = new FileOutputStream(filename);
		// XERCES 1 or 2 additionnal classes.
		OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
		of.setIndent(1);
		of.setIndenting(true);
		XMLSerializer serializer = new XMLSerializer(fos, of);
		// As a DOM Serializer
		serializer.asDOMSerializer();
		serializer.serialize(xmldoc.getDocumentElement());
		fos.close();
	}*/

	private void writeXML(DOMImplementation domImpl, Document xmldoc,
			String filename) throws FileNotFoundException {
			DOMImplementationLS ls = (DOMImplementationLS) domImpl;
	        LSSerializer lss = ls.createLSSerializer();
	        LSOutput lso = ls.createLSOutput();
	        lso.setByteStream(new FileOutputStream(new File(filename)));
	        lss.write(xmldoc, lso);
	}

	public void createWADL(WADLFile wadlFile, String xsdFilename) throws IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl = builder.getDOMImplementation();
		
		/* Application = root element */
// TODO	The namespaces are still implemented incorrectly. We will leave it for now and change it later.
		
		Document xmldoc = domImpl.createDocument("http://wadl.dev.java.net/2009/02", "application", null);
		Element root = xmldoc.getDocumentElement();
		root.setAttribute("tns:schemaLocation", "http://wadl.dev.java.net/2009/02 "+xsdFilename);
		root.setAttribute("xmlns:tns", "http://www.w3.org/2001/XMLSchema");
		root.setAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema");
				
		/* Grammars */
		Grammars grammars = wadlFile.getGrammarsElements();
		Element grammarsElement = xmldoc.createElement("grammars");
		for(String hrefValue : grammars.getIncludedGrammars()) {
			Element includeElement = xmldoc.createElement("include");
			includeElement.setAttribute("href", hrefValue);
			grammarsElement.appendChild(includeElement);
		}
		root.appendChild(grammarsElement);
				
		/* Resources */
		HashMap<String, Resources> resources = wadlFile.getResourcesElements();
		for(Resources r : resources.values()) {
			Element resourcesElement = xmldoc.createElement("resources");
			resourcesElement.setAttribute("base", r.getIdentifier());
			root.appendChild(resourcesElement);
		
			HashSet<Resource> resource = new HashSet<Resource>();
			resource.addAll(r.getResourceElements().values());
			
			for(Resource rr : resource) {
				Element resourceElement = xmldoc.createElement("resource");
				resourceElement.setAttribute("path", rr.getIdentifier());
				resourcesElement.appendChild(resourceElement);
			
				HashSet<Method> methods = new HashSet<Method>();
				methods.addAll(rr.getMethodElements().values());
				for(Method m : methods) {
					Element methodElement = xmldoc.createElement("method");
					methodElement.setAttribute("id", m.getIdentifier());
					methodElement.setAttribute("name", m.getName());
					resourceElement.appendChild(methodElement);
					
					/* Request (limited to 1) */
					Request request = m.getRequestElement();
					Element requestElement = xmldoc.createElement("request");
					methodElement.appendChild(requestElement);

					HashSet<Param> param = new HashSet<Param>(); 
					param.addAll(request.getParamElements().values());
					for(Param par : param) {
						Element paramElement = xmldoc.createElement("param");
//							paramElement.setIdAttribute("required", par.isRequired());
						paramElement.setAttribute("style", par.getStyle());
						paramElement.setAttribute("type", TARGET_SCHEMA_NAMESPACE+par.getType());
						paramElement.setAttribute("name", par.getIdentifier());
						requestElement.appendChild(paramElement);
					}
																	
					/* Response (multiple possible) */
					HashSet<Response> responses = new HashSet<Response>();
					responses.addAll(m.getResponseElements().values());
					for(Response resp : responses) {
						Element responseElement = xmldoc.createElement("response");		
						responseElement.setAttribute("status", Integer.toString(resp.getID()));
						methodElement.appendChild(responseElement);
					
						HashSet<Representation> representation = new HashSet<Representation>();
						representation.addAll(resp.getRepresentationElements().values());
						for(Representation rep : representation) {
							Element representationElement = xmldoc.createElement("representation");
							representationElement.setAttribute("element", rep.getIdentifier());
							representationElement.setAttribute("mediaType", rep.getMediaType());
							responseElement.appendChild(representationElement);
						}
					}
				}
			}
		}
		
		writeXML(domImpl, xmldoc, wadlFile.getIdentifier());
	}
}
