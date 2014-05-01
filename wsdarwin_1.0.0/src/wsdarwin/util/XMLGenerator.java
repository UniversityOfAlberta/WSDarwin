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

	private void createXSD(XSDFile xsdFile, Document xmldoc, Element schemaElement) throws IOException, ParserConfigurationException {
		
		ArrayList<String> elementsAndTypes = xsdFile.sortedElementAndTypeNames();
		HashMap<String, XSDElement> elements = xsdFile.getElements();
		HashMap<String, XSDIType> types = xsdFile.getTypes();
		for (String name : elementsAndTypes) {
			//System.out.println("--> name is: " + name);
			if (elements.containsKey(name)) {
				//System.out.println("---> name " + name + " is an xsdElement");
				Element element = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"element");
				element.setAttribute("name", elements.get(name).getName());
				if (xsdFile.getTypes().containsKey(elements.get(name).getType().getName())) {
					element.setAttribute("type", TARGET_SCHEMA_NAMESPACE
							+ elements.get(name).getType());
					//System.out.println("(1) Element: " + element.getAttribute("name") + ", type: " + element.getAttribute("type"));
				}
				else {
					element.setAttribute("type", XML_SCHEMA_NAMESPACE
							+ elements.get(name).getType());
				}
				if(elements.get(name).getMaxOccurs() != null) {
					element.setAttribute("minOccurs", ""+elements.get(name).getMinOccurs());
					element.setAttribute("maxOccurs", ""+elements.get(name).getMaxOccurs());
				}
				schemaElement.appendChild(element);
			}
			if (types.containsKey(name)) {
				if (types.get(name) instanceof XSDComplexType) {
					XSDComplexType type = (XSDComplexType)types.get(name);
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
									TARGET_SCHEMA_NAMESPACE+xsdElement.getType().getName());
							//System.out.println("(2) Element: " + childElement.getAttribute("name") + ", type: " + childElement.getAttribute("type"));
							
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
					schemaElement.appendChild(complexType);
				}
				else if(types.get(name) instanceof XSDSimpleType) {
					XSDSimpleType type = (XSDSimpleType)types.get(name);
					Element simpleType = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"simpleType");
					simpleType.setAttribute("name", types.get(name).getName());
					if(type.getList() != null) {
						Element list = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"list");
						if (xsdFile.getTypes().containsKey(type.getList().getItemType().getName())) {
							list.setAttribute("itemType",
									TARGET_SCHEMA_NAMESPACE
										+type.getList().getItemType()
													.getName());
							System.out.println("(3) list Element: " + list.getAttribute("name") + ", type: " + list.getAttribute("itemType"));
						}
						else {
							list.setAttribute("itemType",
									XML_SCHEMA_NAMESPACE
											+type.getList().getItemType()
													.getName());
						}
						simpleType.appendChild(list);
					}
					else if(type.getRestrictionBase() != null) {
						Element base = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"restriction");
						base.setAttribute("base", TARGET_SCHEMA_NAMESPACE
								+type.getRestrictionBase());
						//System.out.println("(4) base Element: " + base.getAttribute("base"));
						simpleType.appendChild(base);
						for(String enume : type.getEnumerations()) {
							Element enumeration = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"enumeration");
							enumeration.setAttribute("value", enume);
							base.appendChild(enumeration);
						}
					}
					schemaElement.appendChild(simpleType);
				}
			}
		}
	}

	private void writeXML(DOMImplementation domImpl, Document xmldoc,
			String filename) throws FileNotFoundException {
			DOMImplementationLS ls = (DOMImplementationLS) domImpl;
	        LSSerializer lss = ls.createLSSerializer();
	        LSOutput lso = ls.createLSOutput();
	        lso.setByteStream(new FileOutputStream(new File(filename)));
	        
	        lss.write(xmldoc, lso);
	        
	        //Element root = xmldoc.get
	        //System.out.println("root name ? " + getStringFromDocument(xmldoc) );
	        //String jsonRet = gson.toJson( getStringFromDocument(xmldoc) );
	        //System.out.println("THE FINAL XML: " + xmldoc.toString() );
	}
	
	/*
	//method to convert Document to String
	public String getStringFromDocument(Document doc)
	{
	    try
	    {
	       DOMSource domSource = new DOMSource(doc);
	       StringWriter writer = new StringWriter();
	       StreamResult result = new StreamResult(writer);
	       TransformerFactory tf = TransformerFactory.newInstance();
	       Transformer transformer = tf.newTransformer();
	       transformer.transform(domSource, result);
	       return writer.toString();
	    }
	    catch(TransformerException ex)
	    {
	       ex.printStackTrace();
	       return null;
	    }
	} */
	
	public Document createWADL(WADLFile wadlFile) throws IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl = builder.getDOMImplementation();
		
		/* Application = root element */
// TODO	The namespaces are still implemented incorrectly. We will leave it for now and change it later.
		
		/*ArrayList<String> rootArray = new ArrayList<String>();
		rootArray.add(new String("First top"));
		rootArray.add(new String("Second top"));
			
		ArrayList<String> two = new ArrayList<String>();
		two.add(new String("hello"));
		two.add(new String("mellow"));
		
		
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println("json: " + gson.toJson(rootArray) );
		rootArray.add(two);
		
		System.out.println("json: " + gson.toJson(rootArray) );*/
		
		Document xmldoc = domImpl.createDocument("http://wadl.dev.java.net/2009/02", "application", null);
		Element root = xmldoc.getDocumentElement();
		//root.setAttribute("tns:schemaLocation", "http://wadl.dev.java.net/2009/02 "+xsdFile.getFilename());
		//root.setAttribute("xmlns:tns", "http://www.w3.org/2001/XMLSchema");
		root.setAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema");
		
		System.out.println("element attt is " + root + ", v: " + root.getAttribute("xmlns:xs").toString() );
		
		/* Grammars */
		Grammars grammars = wadlFile.getGrammarsElements();
		Element grammarsElement = xmldoc.createElement("grammars");
		Element schemaElement = xmldoc.createElement(XML_SCHEMA_NAMESPACE+"schema");
		for (XSDFile xsdFile : grammars.getIncludedGrammars()) {
			//System.out.println("-> xsd File: " + xsdFile);
			createXSD(xsdFile, xmldoc, schemaElement);
			grammarsElement.appendChild(schemaElement);
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
						paramElement.setAttribute("type", XML_SCHEMA_NAMESPACE+par.getType());
						paramElement.setAttribute("name", par.getIdentifier());
						//System.out.println("(55) list Element: " + paramElement.getAttribute("name") + ", type: " + paramElement.getAttribute("type"));
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
		
		return xmldoc;
	}
}