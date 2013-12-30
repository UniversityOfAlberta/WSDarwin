package wsdarwin.parsers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.*;
import org.eclipse.core.internal.resources.Folder;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import wsdarwin.comparison.delta.AddDelta;
import wsdarwin.comparison.delta.ChangeDelta;
import wsdarwin.comparison.delta.DeleteDelta;
import wsdarwin.comparison.delta.Delta;
import wsdarwin.comparison.delta.MatchDelta;
import wsdarwin.model.ComplexType;
import wsdarwin.model.IService;
import wsdarwin.model.IType;
import wsdarwin.model.Interface;
import wsdarwin.model.Operation;
import wsdarwin.model.SimpleType;
import wsdarwin.model.PrimitiveType;
import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class WSDLParser {

	private Document document;
	private IService service;
	private String filename;

	// private HashMap<String, IType> types;

	public WSDLParser(String filename) {
		// types = new HashMap<String, IType>();
		this.filename = filename;
		parseFile(new File(filename));
	}

	public IService getService() {
		return service;
	}

	public void setService(IService service) {
		this.service = service;
	}

	public void parseFile(File file) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			document = dbf.newDocumentBuilder().parse(file);

			NodeList definitionsList = document.getElementsByTagNameNS("*",
					"definitions");
			Node definitions = definitionsList.item(0);
			
			int typeCount = 0;
			typeCount += document.getElementsByTagNameNS("*","complexType").getLength();
			typeCount += document.getElementsByTagNameNS("*","simpleType").getLength();
			
			System.out.print("Types: "+ typeCount+"\t");

			service = new IService(definitions.getAttributes()
					.getNamedItem("targetNamespace").getNodeValue());

			service.setInterfaces(getServiceInterfaces());
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	private HashMap<String, WSElement> getServiceInterfaces() {
		HashMap<String, WSElement> serviceInterfaces = new HashMap<String, WSElement>();
		NodeList interfaceList = document.getElementsByTagNameNS("*",
				"portType");
		for (int i = 0; i < interfaceList.getLength(); i++) {
			Interface serviceInterface = new Interface(interfaceList.item(i)
					.getAttributes().getNamedItem("name").getNodeValue(),
					getServicePortForPortType(
							interfaceList.item(i).getAttributes()
									.getNamedItem("name").getNodeValue())
							.getAttributes().getNamedItem("location")
							.getNodeValue());
			HashMap<String, WSElement> operations = new HashMap<String, WSElement>();
			operations = getInterfaceOperations(interfaceList.item(i));
			serviceInterface.setOperations(operations);
			serviceInterfaces.put(serviceInterface.getAddress(),
					serviceInterface);
		}
		return serviceInterfaces;
	}

	private HashMap<String, WSElement> getInterfaceOperations(Node interfaceNode) {
		HashMap<String, WSElement> operations = new HashMap<String, WSElement>();
		NodeList operationList = document.getElementsByTagNameNS("*",
				"operation");
		for (int i = 0; i < operationList.getLength(); i++) {
			if (equalsIgnoreNamespace(operationList.item(i).getParentNode()
					.getNodeName(), "portType")) {
				Node operationNode = operationList.item(i);
				Operation operation = new Operation(operationNode
						.getAttributes().getNamedItem("name").getNodeValue(),
						"", "", getInputTypeOfOperation(operationNode),
						getOutputTypeOfOperation(operationNode));
				operations.put(operation.getName(), operation);
			}
		}
		return operations;
	}

	private Node getServicePortForPortType(String interfaceName) {
		NodeList bindingList = document.getElementsByTagNameNS("*", "binding");
		String bindingName = "";
		for (int i = 0; i < bindingList.getLength(); i++) {
			if (equalsIgnoreNamespace(bindingList.item(i).getParentNode()
					.getNodeName(), "definitions")
					&& equalsIgnoreNamespace(bindingList.item(i)
							.getAttributes().getNamedItem("type")
							.getNodeValue(), interfaceName)) {
				bindingName = bindingList.item(i).getAttributes()
						.getNamedItem("name").getNodeValue();
			}
		}
		NodeList serviceList = document.getElementsByTagNameNS("*", "service");
		for (int i = 0; i < serviceList.getLength(); i++) {
			NodeList portList = document.getElementsByTagNameNS("*", "port");

			for (int k = 0; k < portList.getLength(); k++) {
				if (equalsIgnoreNamespace(portList.item(k).getParentNode()
						.getNodeName(), "service")
						&& equalsIgnoreNamespace(portList.item(k)
								.getAttributes().getNamedItem("binding")
								.getNodeValue(), bindingName)) {
					Node portNode = portList.item(k);
					NodeList addressList = document.getElementsByTagNameNS("*",
							"address");
					for (int j = 0; j < addressList.getLength(); j++) {
						if (equalsIgnoreNamespace(addressList.item(j)
								.getParentNode().getNodeName(), "port")) {
							return addressList.item(j);
						}
					}
				}
			}
		}
		return null;
	}

	private boolean equalsIgnoreNamespace(String s1, String s2) {
		if (s1.contains(":")) {
			s1 = s1.substring(s1.indexOf(':') + 1);
		}
		if (s2.contains(":")) {
			s2 = s2.substring(s2.indexOf(':') + 1);
		}
		return s1.equals(s2);
	}

	private Node getInputOfOperation(Node operation) {
		NodeList inputList = document.getElementsByTagNameNS("*", "input");
		for (int i = 0; i < inputList.getLength(); i++) {
			Node child = inputList.item(i);
			if (equalsIgnoreNamespace(child.getParentNode().getNodeName(),
					"operation") && child.getParentNode().equals(operation)) {
				return child;
			}
		}
		return null;
	}

	private Node getOutputOfOperation(Node operation) {
		NodeList outputList = document.getElementsByTagNameNS("*", "output");
		for (int i = 0; i < outputList.getLength(); i++) {
			Node child = outputList.item(i);
			if (equalsIgnoreNamespace(child.getParentNode().getNodeName(),
					"operation") && child.getParentNode().equals(operation)) {
				return child;
			}
		}
		return null;
	}

	private IType getInputTypeOfOperation(Node operation) {
		Node input = getInputOfOperation(operation);
		Node messageAttribute = input.getAttributes().getNamedItem("message");
		Node message = getMessageFromAttribute(messageAttribute.getNodeValue());
		String messageElement = getMessageElement(message);
		Node element = getElementFromMessage(messageElement);
		Node elementTypeElement = null;
		if (element != null) {
			elementTypeElement = getElementTypeFromElement(element);
		}
		Node type;
		String elementName = "";
		if (element != null) {
			if (elementTypeElement == null) {
				String elementType = getElementType(element);
				elementName = getElementName(element);
				type = getTypeFromElement(elementType);
			} else {
				String elementType = getElementType(elementTypeElement);
				elementName = getElementName(element);
				type = getTypeFromElement(elementType);
			}
		} else {
			type = getTypeFromElement(messageElement);
		}
		IType iType = null;
		if (type != null) {
			iType = getIType(type, elementName);
		} else {
			iType = buildITypeFromElement(element, elementName);
		}
		return iType;
	}

	private IType buildITypeFromElement(Node element, String variableName) {
		IType iType = new ComplexType(element.getAttributes()
				.getNamedItem("name").getNodeValue(), variableName);
		NodeList elementChildren = element.getChildNodes();
		for (int i = 0; i < elementChildren.getLength(); i++) {
			if (equalsIgnoreNamespace(elementChildren.item(i).getNodeName(),
					"complexType")) {
				NodeList typeChildren = elementChildren.item(i).getChildNodes();
				for (int j = 0; j < typeChildren.getLength(); j++) {
					if (equalsIgnoreNamespace(typeChildren.item(j)
							.getNodeName(), "sequence")) {
						NodeList sequenceChildren = typeChildren.item(j)
								.getChildNodes();
						for (int k = 0; k < sequenceChildren.getLength(); k++) {
							if (equalsIgnoreNamespace(sequenceChildren.item(k)
									.getNodeName(), "element")) {
								iType.addElement(
										sequenceChildren.item(k)
												.getAttributes()
												.getNamedItem("name")
												.getNodeValue(),
										getITypeFromElement(sequenceChildren
												.item(k).getAttributes()
												.getNamedItem("type")
												.getNodeValue(),
												sequenceChildren.item(k)
														.getAttributes()
														.getNamedItem("name")
														.getNodeValue()));
							}
						}
					}
				}
			}
		}
		return iType;
	}

	private IType getOutputTypeOfOperation(Node operation) {
		Node input = getOutputOfOperation(operation);
		Node messageAttribute = input.getAttributes().getNamedItem("message");
		Node message = getMessageFromAttribute(messageAttribute.getNodeValue());
		String messageElement = getMessageElement(message);
		Node element = getElementFromMessage(messageElement);
		Node elementTypeElement = null;
		if (element != null) {
			elementTypeElement = getElementTypeFromElement(element);
		}
		Node type;
		String elementName = "";
		if (element != null) {
			if (elementTypeElement == null) {
				String elementType = getElementType(element);
				elementName = getElementName(element);
				type = getTypeFromElement(elementType);
			} else {
				String elementType = getElementType(elementTypeElement);
				elementName = getElementName(element);
				type = getTypeFromElement(elementType);
			}
		} else {
			type = getTypeFromElement(messageElement);
		}
		IType iType = null;
		if (type != null) {
			iType = getIType(type, elementName);
		} else {
			iType = buildITypeFromElement(element, elementName);
		}
		return iType;
	}

	private IType getIType(Node type, String variableName) {
		IType iType = null;
		if (equalsIgnoreNamespace(type.getNodeName(), "complexType")) {
			// if (!types.containsKey(type.getAttributes().getNamedItem("name")
			// .getNodeValue())) {
			iType = new ComplexType(type.getAttributes().getNamedItem("name")
					.getNodeValue(), variableName);
			// } else {
			// iType = types.get(type.getAttributes().getNamedItem("name")
			// .getNodeValue());
			// }
			// types.put(iType.getName(), iType);
			NodeList children = type.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				if (equalsIgnoreNamespace(children.item(i).getNodeName(),
						"sequence")) {
					NodeList sequenceChildren = children.item(i)
							.getChildNodes();
					for (int j = 0; j < sequenceChildren.getLength(); j++) {
						if (equalsIgnoreNamespace(sequenceChildren.item(j)
								.getNodeName(), "element")) {
							iType.addElement(
									sequenceChildren.item(j).getAttributes()
											.getNamedItem("name")
											.getNodeValue(),
									getITypeFromElement(
											getElementType(sequenceChildren
													.item(j)), sequenceChildren
													.item(j).getAttributes()
													.getNamedItem("name")
													.getNodeValue()));
						}
					}
				} else if (equalsIgnoreNamespace(
						children.item(i).getNodeName(), "complexContent")) {
					NodeList contentChildren = children.item(i).getChildNodes();
					for (int k = 0; k < contentChildren.getLength(); k++) {
						if (equalsIgnoreNamespace(contentChildren.item(k)
								.getNodeName(), "extension")) {
							NodeList extensionChildren = contentChildren
									.item(k).getChildNodes();
							for (int m = 0; m < extensionChildren.getLength(); m++) {
								if (equalsIgnoreNamespace(extensionChildren
										.item(m).getNodeName(), "sequence")) {
									NodeList sequenceChildren = extensionChildren
											.item(m).getChildNodes();
									for (int j = 0; j < sequenceChildren
											.getLength(); j++) {
										if (equalsIgnoreNamespace(
												sequenceChildren.item(j)
														.getNodeName(),
												"element")) {
											if (sequenceChildren.item(j)
													.getAttributes()
													.getNamedItem("name") != null) {
												iType.addElement(
														sequenceChildren
																.item(j)
																.getAttributes()
																.getNamedItem(
																		"name")
																.getNodeValue(),
														getITypeFromElement(
																sequenceChildren
																		.item(j)
																		.getAttributes()
																		.getNamedItem(
																				"type")
																		.getNodeValue(),
																sequenceChildren
																		.item(j)
																		.getAttributes()
																		.getNamedItem(
																				"name")
																		.getNodeValue()));
											} else if (sequenceChildren.item(j)
													.getAttributes()
													.getNamedItem("ref") != null) {
												iType.addElement(
														sequenceChildren
																.item(j)
																.getAttributes()
																.getNamedItem(
																		"ref")
																.getNodeValue(),
														getITypeFromElement(
																sequenceChildren
																		.item(j)
																		.getAttributes()
																		.getNamedItem(
																				"ref")
																		.getNodeValue(),
																sequenceChildren
																		.item(j)
																		.getAttributes()
																		.getNamedItem(
																				"name")
																		.getNodeValue()));
											}
										}
									}
								}
							}
						}
					}
				}
			}
		} else if (equalsIgnoreNamespace(type.getNodeName(), "simpleType")) {
			// if (!types.containsKey(type.getAttributes().getNamedItem("name")
			// .getNodeValue())) {
			iType = new SimpleType(type.getAttributes().getNamedItem("name")
					.getNodeValue(), variableName);
			// } else {
			// iType = types.get(type.getAttributes().getNamedItem("name")
			// .getNodeValue());
			// }
			// types.put(iType.getName(), iType);
			NodeList children = type.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				if (equalsIgnoreNamespace(children.item(i).getNodeName(),
						"restriction")) {
					((SimpleType) iType).setBase(getITypeFromElement(children
							.item(i).getAttributes().getNamedItem("base")
							.getNodeValue(), ""));
					NodeList restrictionChildren = children.item(i)
							.getChildNodes();
					for (int j = 0; j < restrictionChildren.getLength(); j++) {
						if (equalsIgnoreNamespace(restrictionChildren.item(j)
								.getNodeName(), "enumeration")) {
							((SimpleType) iType).addOption(restrictionChildren
									.item(j).getAttributes()
									.getNamedItem("value").getNodeValue());
						}
					}
				}
			}
		}
		return iType;
	}

	private IType getITypeFromElement(String elementType, String name) {
		IType iType = null;
		if (elementType.startsWith("xs:") || elementType.startsWith("xsd:")) {
			// if (!types.containsKey(elementType.substring(elementType
			// .indexOf(':') + 1))) {
			iType = new PrimitiveType(elementType.substring(
					elementType.indexOf(':') + 1).toUpperCase(), name);
			// types.put(iType.toString(), iType);
			// } else {
			// iType = types.get(elementType.substring(elementType
			// .indexOf(':') + 1));
			// }
		} else if (elementType.startsWith("tns:") || !elementType.contains(":")
				|| elementType.startsWith("ns:")
				|| elementType.startsWith("ax21:")) {
			iType = getIType(getTypeFromElement(elementType), name);
		} else {
			iType = new PrimitiveType(elementType.toUpperCase(), name);
		}
		return iType;
	}

	private Node getTypeFromElement(String elementType) {
		NodeList types = document.getElementsByTagNameNS("*", "complexType");
		for (int i = 0; i < types.getLength(); i++) {
			if (equalsIgnoreNamespace(types.item(i).getParentNode()
					.getNodeName(), "schema")
					&& equalsIgnoreNamespace(types.item(i).getAttributes()
							.getNamedItem("name").getNodeValue(), elementType)) {
				return types.item(i);
			}
		}
		types = document.getElementsByTagNameNS("*", "simpleType");
		for (int i = 0; i < types.getLength(); i++) {
			if (equalsIgnoreNamespace(types.item(i).getParentNode()
					.getNodeName(), "schema")
					&& equalsIgnoreNamespace(types.item(i).getAttributes()
							.getNamedItem("name").getNodeValue(), elementType)) {
				return types.item(i);
			}
		}
		return null;
	}

	private String getElementType(Node element) {
		if (element.getAttributes().getNamedItem("type") != null) {
			return element.getAttributes().getNamedItem("type").getNodeValue();
		} else {
			return getNestedElementType(element);
		}
	}

	private String getElementName(Node element) {
		if (element.getAttributes().getNamedItem("name") != null) {
			return element.getAttributes().getNamedItem("name").getNodeValue();
		} else {
			return "";
		}
	}

	private String getNestedElementType(Node element) {
		String elementType = "";
		if ((equalsIgnoreNamespace(element.getNodeName(), "element") && ((element
				.getAttributes().getNamedItem("type") != null) || (element
				.getAttributes().getNamedItem("ref") != null)))
				|| equalsIgnoreNamespace(element.getNodeName(), "simpleType")) {
			if (element.getAttributes().getNamedItem("type") != null) {
				elementType = element.getAttributes().getNamedItem("type")
						.getNodeValue();
			} else if (element.getAttributes().getNamedItem("ref") != null) {
				Node elementRef = getElementFromMessage(element.getAttributes()
						.getNamedItem("ref").getNodeValue());
				elementType = getNestedElementType(elementRef);
			} else if (equalsIgnoreNamespace(element.getNodeName(),
					"simpleType")) {
				NodeList children = element.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					if (equalsIgnoreNamespace(children.item(i).getNodeName(),
							"restriction")) {
						elementType = children.item(i).getAttributes()
								.getNamedItem("base").getNodeValue();
					}
				}
			}
		} else {
			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				elementType = getNestedElementType(children.item(i));
				if (elementType != "") {
					return elementType;
				}
			}
		}
		return elementType;
	}

	private String getMessageElement(Node message) {
		NodeList children = message.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (equalsIgnoreNamespace(children.item(i).getNodeName(), "part")) {
				return children.item(i).getAttributes().getNamedItem("element")
						.getNodeValue();
			}
		}
		return null;
	}

	private Node getElementTypeFromElement(Node element) {
		if (element.hasChildNodes()) {
			NodeList elementChildren = element.getChildNodes();
			for (int i = 0; i < elementChildren.getLength(); i++) {
				if (equalsIgnoreNamespace(
						elementChildren.item(i).getNodeName(), "complexType")) {
					NodeList typeChildren = elementChildren.item(i)
							.getChildNodes();
					for (int j = 0; j < typeChildren.getLength(); j++) {
						if (equalsIgnoreNamespace(typeChildren.item(j)
								.getNodeName(), "sequence")) {
							NodeList sequenceChildren = typeChildren.item(j)
									.getChildNodes();
							for (int k = 0; k < sequenceChildren.getLength(); k++) {
								if (equalsIgnoreNamespace(sequenceChildren
										.item(k).getNodeName(), "element")) {
									return sequenceChildren.item(k);
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	private Node getElementFromMessage(String message) {
		NodeList elements = document.getElementsByTagNameNS("*", "element");
		for (int i = 0; i < elements.getLength(); i++) {
			if (equalsIgnoreNamespace(elements.item(i).getParentNode()
					.getNodeName(), "schema")) {
				if (equalsIgnoreNamespace(elements.item(i).getAttributes()
						.getNamedItem("name").getNodeValue(), message)) {
					return elements.item(i);
				}
			}
		}
		return null;
	}

	private Node getMessageFromAttribute(String nodeValue) {
		NodeList messages = document.getElementsByTagNameNS("*", "message");
		for (int i = 0; i < messages.getLength(); i++) {
			if (equalsIgnoreNamespace(messages.item(i).getAttributes()
					.getNamedItem("name").getNodeValue(), nodeValue)) {
				return messages.item(i);
			}
		}
		return null;
	}

	public static void createXML(String filename, Delta delta) throws IOException {
		// Document (Xerces implementation only).
		Document xmldoc = new DocumentImpl();
		// Root element.
		Element root = delta.createXMLElement(xmldoc, null);
		xmldoc.appendChild(root);
		FileOutputStream fos = new FileOutputStream(filename); // XERCES 1 or 2
																// additionnal
																// classes.
		OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
		of.setIndent(1);
		of.setIndenting(true);
		XMLSerializer serializer = new XMLSerializer(fos, of); // As a DOM
																// Serializer
		serializer.asDOMSerializer();
		serializer.serialize(xmldoc.getDocumentElement());
		fos.close();
	}

	private Element getTypeElement(String name, IType type, Document xmldoc) {
		if (type instanceof ComplexType) {
			Element complexType = xmldoc.createElementNS(null, "complexType");
			complexType.setAttributeNS(null, "name", name);
			if (!name.equals(type.getName())) {
				complexType.setAttributeNS(null, "type", type.getName());
			}
			for (String nestedType : type.getChildren().keySet()) {
				Element nestedTypeElement = getTypeElement(nestedType,
						(IType) type.getChildren().get(nestedType), xmldoc);
				complexType.appendChild(nestedTypeElement);
			}
			return complexType;
		} else if (type instanceof PrimitiveType) {
			Element primitiveType = xmldoc.createElementNS(null,
					"primitiveType");
			primitiveType.setAttributeNS(null, "name", name);
			primitiveType.setAttributeNS(null, "type", type.getName());
			return primitiveType;
		} else if (type instanceof SimpleType) {
			Element simpleType = xmldoc.createElementNS(null, "simpleType");
			simpleType.setAttributeNS(null, "name", name);
			Element restriction = xmldoc.createElementNS(null, "restriction");
			restriction.setAttributeNS(null, "base", ((SimpleType) type)
					.getBase().getName());
			simpleType.appendChild(restriction);
			for (String enumeration : ((SimpleType) type).getOptions()) {
				Element nestedTypeElement = xmldoc.createElementNS(null,
						"enumeration");
				nestedTypeElement.setAttributeNS(null, "value", enumeration);
				restriction.appendChild(nestedTypeElement);
			}
			return simpleType;
		}
		return null;
	}

	/*
	 * public String diff(WSDLParser parser) { String diff = ""; HashMap<String,
	 * Operation> operationsAdded = new HashMap<String, Operation>();
	 * HashMap<String, Operation> operationsDeleted = new HashMap<String,
	 * Operation>(); HashMap<String, Operation> operationsIncluded = new
	 * HashMap<String , Operation>(); for(String name :
	 * this.operations.keySet()) { operationsIncluded.put(name,
	 * this.operations.get(name)); } for(String name : this.operations.keySet())
	 * { if(!parser.getOperations().containsKey(name)) {
	 * operationsDeleted.put(name, this.operations.get(name));
	 * operationsIncluded.remove(name); diff += "operation deleted(" +
	 * this.operations.get(name) + ")\n"; } } for(String name :
	 * parser.getOperations().keySet()) { if(!this.operations.containsKey(name))
	 * { operationsAdded.put(name, parser.getOperations().get(name));
	 * operationsIncluded.remove(name); diff += "operation added(" +
	 * parser.operations.get(name) + ")\n"; } } for (String name :
	 * operationsIncluded.keySet()) { if (!this.getOperations().get(name)
	 * .equals(parser.getOperations().get(name))) { diff += "(t1):" +
	 * this.getOperations().get(name) + " = (t2):" +
	 * parser.getOperations().get(name) + "\n(" + this.getOperations().get(name)
	 * .diff(parser.getOperations().get(name)) + ")\n"; } } for (String name :
	 * this.operations.keySet()) { if
	 * (!parser.getOperations().containsKey(name)) { diff +=
	 * "operation deleted(" + this.operations.get(name) + ")\n"; } } for (String
	 * name : parser.operations.keySet()) { if
	 * (!this.getOperations().containsKey(name)) { diff += "operation added(" +
	 * parser.operations.get(name) + ")\n"; } } return diff; }
	 */

	public static void main(String[] args) {
		File folder = new File("files/amazonEC2");
		File[] files = folder.listFiles();
		ArrayList<IService> services = new ArrayList<IService>();
		//for(int i=0; i<files.length; i++) {
			long currentTime = System.currentTimeMillis();
			System.out.print("Parsing 2010 \t");
			IService service1 = new WSDLParser("files/amazonEC2/2009-11-30.ec2.wsdl").getService();
			services.add(service1);
			IService service2 = new WSDLParser("files/amazonEC2/2013-02-01.ec2.wsdl").getService();
			services.add(service2);
			/*int operationCount = 0;
			for(WSElement serviceInterface : service.getChildren().values()) {
				
				operationCount += ((Interface)serviceInterface).getChildren().size();
			}*/
			
			//System.out.println("Operations: "+operationCount);
			/*ObjectMapper mapper = new ObjectMapper();
			try {
				mapper.writeValue(new File("files/json/service"+i+".json"), service);
			} catch (JsonGenerationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			//System.out.println(System.currentTimeMillis()-currentTime);
		//}
		for(int i=0; i<services.size()-1; i++) {
				currentTime = System.currentTimeMillis();
				System.out.print("Diff 2010\t");
				//ObjectMapper mapper = new ObjectMapper();
				try {
					//IService service1 = mapper.readValue(new File("files/json/service"+i+".json"), IService.class);
					//IService service2 = mapper.readValue(new File("files/json/service"+(i+1)+".json"), IService.class);
					Delta delta = services.get(i).diff(services.get(i+1));
					WSDLParser.createXML("files/output/diffALL.xml", delta);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println(System.currentTimeMillis()-currentTime);
		}
		
	}

}
