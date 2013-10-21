package wsdarwin.parsers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

import wsdarwin.comparison.delta.Delta;
import wsdarwin.model.ComplexType;
import wsdarwin.model.PrimitiveType;
import wsdarwin.model.IService;
import wsdarwin.model.IType;
import wsdarwin.model.Interface;
import wsdarwin.model.Operation;
import wsdarwin.model.SimpleType;
import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class WSDLParser {

	private Document document;
	private IService service;

	public WSDLParser(File file) {
		parseFile(file);
	}

	public IService getService() {
		return service;
	}

	public void parseFile(File file) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			document = dbf.newDocumentBuilder().parse(file);

			NodeList definitionsList = document
					.getElementsByTagName("description");
			Node definitions = definitionsList.item(0);

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
		NodeList interfaceList = document.getElementsByTagName("interface");
		for (int i = 0; i < interfaceList.getLength(); i++) {
			Interface serviceInterface = new Interface(interfaceList.item(i).getAttributes()
					.getNamedItem("name").getNodeValue(),
					getServiceEndpointForInterface(
							interfaceList.item(i).getAttributes()
									.getNamedItem("name").getNodeValue())
							.getAttributes().getNamedItem("address")
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
		NodeList interfaceChildren = interfaceNode.getChildNodes();
		for (int i = 0; i < interfaceChildren.getLength(); i++) {
			if (equalsIgnoreNamespace(interfaceChildren.item(i).getNodeName(),
					"operation")) {
				Node operationNode = interfaceChildren.item(i);
				Operation operation = new Operation(operationNode
						.getAttributes().getNamedItem("name").getNodeValue(),"",
						operationNode.getAttributes().getNamedItem("pattern")
								.getNodeValue(),
						getInputTypeOfOperation(operationNode),
						getOutputTypeOfOperation(operationNode));
				operations.put(operation.getName(), operation);
			}
		}
		return operations;
	}

	private Node getServiceEndpointForInterface(String interfaceName) {
		NodeList serviceList = document.getElementsByTagName("service");
		for (int i = 0; i < serviceList.getLength(); i++) {
			if (equalsIgnoreNamespace(serviceList.item(i).getAttributes()
					.getNamedItem("interface").getNodeValue(), interfaceName)) {
				Node serviceNode = serviceList.item(i);
				NodeList serviceChildren = serviceNode.getChildNodes();
				for (int j = 0; j < serviceChildren.getLength(); j++) {
					if (equalsIgnoreNamespace(serviceChildren.item(j)
							.getNodeName(), "endpoint")) {
						return serviceChildren.item(j);
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
		NodeList children = operation.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (equalsIgnoreNamespace(child.getNodeName(), "input")) {
				return child;
			}
		}
		return null;
	}

	private Node getOutputOfOperation(Node operation) {
		NodeList children = operation.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (equalsIgnoreNamespace(child.getNodeName(), "output")) {
				return child;
			}
		}
		return null;
	}

	private IType getInputTypeOfOperation(Node operation) {
		Node input = getInputOfOperation(operation);
		String elementName = input.getAttributes().getNamedItem("element")
				.getNodeValue();
		Node element = getElementNodeFromOperationElement(elementName);
		Node type;
		if (element != null && !element.hasChildNodes()) {
			String elementType = getElementType(element);
			type = getTypeFromElement(elementType);
		} else {
			type = getTypeFromElement(elementName);
		}
		IType iType;
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
		Node output = getOutputOfOperation(operation);
		String elementName = output.getAttributes().getNamedItem("element")
				.getNodeValue();
		Node element = getElementNodeFromOperationElement(elementName);
		Node type;
		if (element != null) {
			String elementType = getElementType(element);
			type = getTypeFromElement(elementType);
		} else {
			type = getTypeFromElement(elementName);
		}
		IType iType;
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
			iType = new ComplexType(type.getAttributes().getNamedItem("name")
					.getNodeValue(), variableName);
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
			iType = new SimpleType(type.getAttributes().getNamedItem("name")
					.getNodeValue(), variableName);
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
			iType = PrimitiveType.valueOf(elementType.substring(elementType
					.indexOf(':') + 1).toUpperCase());
			((PrimitiveType)iType).setVariableName(name);
		} else if (elementType.startsWith("tns:") || !elementType.contains(":")
				|| elementType.startsWith("ns:")
				|| elementType.startsWith("ax21:")) {
			iType = getIType(getTypeFromElement(elementType), name);
		} else {
			iType = PrimitiveType.valueOf(elementType);
			((PrimitiveType)iType).setVariableName(name);
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
				Node elementRef = getElementNodeFromOperationElement(element
						.getAttributes().getNamedItem("ref").getNodeValue());
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

	private Node getElementNodeFromOperationElement(String elementName) {
		NodeList elements = document.getElementsByTagNameNS("*","element");
		for (int i = 0; i < elements.getLength(); i++) {
			if (equalsIgnoreNamespace(elements.item(i).getParentNode()
					.getNodeName(), "schema")) {
				if (equalsIgnoreNamespace(elements.item(i).getAttributes()
						.getNamedItem("name").getNodeValue(), elementName)) {
					return elements.item(i);
				}
			}
		}
		return null;
	}

	public void createXML(String filename) throws IOException {
		// Document (Xerces implementation only).
		Document xmldoc = new DocumentImpl();
		// Root element.
		Element root = xmldoc.createElement("service");
		root.setAttributeNS(null, "targetNamespace", service.getTargetNamespace());
		for (WSElement child : service.getChildren().values()) {
			Interface serviceInterface = (Interface)child;
			Element interfaceElement = xmldoc.createElementNS(null, "interface");
			interfaceElement.setAttributeNS(null, "name", serviceInterface.getName());
			interfaceElement.setAttributeNS(null, "address", serviceInterface.getAddress());
			root.appendChild(interfaceElement);
			for (WSElement interfaceChild : serviceInterface.getChildren().values()) {
				Operation operation = (Operation)interfaceChild;
				// Child i.
				Element operationElement = xmldoc.createElementNS(null,
						"operation");
				operationElement.setAttributeNS(null, "name",
						operation.getName());
				Element input = xmldoc.createElementNS(null, "inputType");
				Element type = getTypeElement(operation.getRequest().getName(),
						operation.getRequest(), xmldoc);
				input.appendChild(type);
				Element output = xmldoc.createElementNS(null, "outputType");
				// output.setAttributeNS(null, "name",
				// operation.getOutput().getName());
				type = getTypeElement(operation.getResponse().getName(),
						operation.getResponse(), xmldoc);
				output.appendChild(type);
				operationElement.appendChild(input);
				operationElement.appendChild(output);
				interfaceElement.appendChild(operationElement);
			}
		}
		xmldoc.appendChild(root);
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
	}

	private Element getTypeElement(String name, IType type, Document xmldoc) {
		if (type instanceof ComplexType) {
			Element complexType = xmldoc.createElementNS(null, "complexType");
			complexType.setAttributeNS(null, "name", name);
			if (!name.equals(type.getName())) {
				complexType.setAttributeNS(null, "type", type.getName());
			}
			for (String nestedType : type.getChildren().keySet()) {
				Element nestedTypeElement = getTypeElement(nestedType, (IType)type
						.getChildren().get(nestedType), xmldoc);
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
			restriction.setAttributeNS(null, "base",
					((SimpleType) type).getBase().getName());
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
		WSDLParser parser1 = new WSDLParser(new File("files/amazon2.wsdl2"));
		WSDLParser parser2 = new WSDLParser(new File("files/amazon3.wsdl2"));
		Delta delta = parser1.getService().diff(parser2.getService());
		DeltaUtil.findMoveDeltas(delta);
		delta.printDelta(0);
		try {
			parser1.createXML("files/amazon2.xml");
			parser2.createXML("files/amazon3.xml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
