package wsdarwin.parsers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import wsdarwin.comparison.delta.Delta;
import wsdarwin.model.ComplexType;
import wsdarwin.model.IService;
import wsdarwin.model.IType;
import wsdarwin.model.Interface;
import wsdarwin.model.Operation;
import wsdarwin.model.PrimitiveType;
import wsdarwin.model.SimpleType;
import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class WADLParser {

	private Document document;
	private IService service;

	public WADLParser(File file) {
		parseFile(file);
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

			service = new IService("");

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
		NodeList resourcesList = document.getElementsByTagNameNS("*",
				"resources");
		for (int i = 0; i < resourcesList.getLength(); i++) {
			Node resourcesNode = resourcesList.item(i);
			String base = resourcesNode.getAttributes()
					.getNamedItem("base").getNodeValue().substring(0,resourcesNode.getAttributes()
					.getNamedItem("base").getNodeValue().length()-1);
			NodeList resourceList = document.getElementsByTagNameNS("*",
					"resource");
			HashMap<String, WSElement> operations = new HashMap<String, WSElement>();
			parseResources(serviceInterfaces, resourcesNode, base,
					resourceList, operations);
		}
		return serviceInterfaces;
	}

	private void parseResources(HashMap<String, WSElement> serviceInterfaces,
			Node resourcesNode, String base, NodeList resourceList,
			HashMap<String, WSElement> operations) {
		for (int j = 0; j < resourceList.getLength(); j++) {
			Node resourceNode = resourceList.item(j);
			if (resourceNode.getParentNode()
					.equals(resourcesNode)) {
				if (containsMethods(resourceNode)) {
					String id = "";
					if (resourceNode.getAttributes().getNamedItem("id") != null) {
						id = resourceNode.getAttributes()
								.getNamedItem("id").getNodeValue();
					}
					Interface serviceInterface = new Interface(id, base);
					operations.putAll(getInterfaceOperations(resourceNode));
					serviceInterface.getChildren().putAll(operations);
					serviceInterfaces.put(serviceInterface.getAddress(),
							serviceInterface);
				}
				else if(resourceNode.getNodeName().equals("resource")) {
					resourceList = resourceNode.getChildNodes();
					base+="/"+resourceNode.getAttributes().getNamedItem("path").getNodeValue();
					parseResources(serviceInterfaces, resourceNode, base, resourceList, operations);
				}
			}
		}
	}

	private boolean containsMethods(Node item) {
		NodeList list = item.getChildNodes();
		for(int i=0; i<list.getLength(); i++) {
			if(list.item(i).getNodeName().equals("method")) {
				return true;
			}
		}
		return false;
	}

	private HashMap<String, WSElement> getInterfaceOperations(Node resourceNode) {
		HashMap<String, WSElement> operations = new HashMap<String, WSElement>();
		NodeList methodList = document.getElementsByTagNameNS("*", "method");
		for (int i = 0; i < methodList.getLength(); i++) {
			if (methodList.item(i).getParentNode().equals(resourceNode)) {
				Operation operation = new Operation(methodList.item(i)
						.getAttributes().getNamedItem("id").getNodeValue(),
						methodList.item(i).getAttributes().getNamedItem("name")
								.getNodeValue(), "",
						getInputTypeOfOperation(methodList.item(i)),
						getOutputTypeOfOperation(methodList.item(i)));
				operations.put(operation.getName(), operation);
			}
		}
		return operations;
	}
	
	private IType getOutputTypeOfOperation(Node method) {
		Node response = getMethodChild(method, "response");
		IType iType = null;
		if (true) {
			// TODO if request has parameters, else request has representation
			NodeList responseChildren = response.getChildNodes();
			for (int i = 0; i < responseChildren.getLength(); i++) {
				if (equalsIgnoreNamespace(responseChildren.item(i)
						.getNodeName(), "representation")) {
					Node representation = responseChildren.item(i);
					String representationElementName = representation
							.getAttributes().getNamedItem("element")
							.getNodeValue();
					Node element = getElementFromRepresentation(representationElementName);
					Node type = null;
					String elementName = "";
					if (element != null) {
						String elementType = getElementType(element);
						elementName = getElementName(element);
						type = getTypeFromElement(elementType);
					} else {
						// type = getTypeFromElement(messageElement);
					}
					if (type != null) {
						iType = getIType(type, elementName);
					} else {
						iType = buildITypeFromElement(element, elementName);
					}
				}
			}
		}
		return iType;
	}

	private Node getElementFromRepresentation(String message) {
		NodeList elements = document.getElementsByTagNameNS("*", "element");
		for (int i = 0; i < elements.getLength(); i++) {
			if (equalsIgnoreNamespace(elements.item(i).getParentNode()
					.getNodeName(), "schema")) {
				if (equalsIgnoreNamespace(elements.item(i).getAttributes()
						.getNamedItem("type").getNodeValue(), message)) {
					return elements.item(i);
				}
			}
		}
		return null;
	}

	private String getElementName(Node element) {
		if (element.getAttributes().getNamedItem("name") != null) {
			return element.getAttributes().getNamedItem("name").getNodeValue();
		} else {
			return "";
		}
	}

	private IType getInputTypeOfOperation(Node method) {
		Node request = getMethodChild(method, "request");
		IType iType = null;
		if (true) {
			// TODO if request has parameters, else request has representation
			iType = new ComplexType(method.getAttributes().getNamedItem("id")
					.getNodeValue()
					+ "RequestType", method.getAttributes().getNamedItem("id")
					.getNodeValue()
					+ "Request");
			NodeList requestChildren = request.getChildNodes();
			for (int i = 0; i < requestChildren.getLength(); i++) {
				// TODO check if param has options
				if (equalsIgnoreNamespace(
						requestChildren.item(i).getNodeName(), "param")) {
					String elementName = requestChildren.item(i)
							.getAttributes().getNamedItem("name")
							.getNodeValue();
					String elementType = requestChildren.item(i)
							.getAttributes().getNamedItem("type")
							.getNodeValue();
					Node element = getElementNodeFromOperationElement(elementType);
					Node type;
					if (element != null && !element.hasChildNodes()) {
						type = getTypeFromElement(elementType);
					} else {
						type = getTypeFromElement(elementName);
					}
					IType nestedType;
					if (type != null) {
						nestedType = getIType(type, elementName);
					} else {
						nestedType = getITypeFromElement(elementType, elementName);
					}
					iType.addElement(requestChildren.item(i).getAttributes()
							.getNamedItem("name").getNodeValue(), nestedType);
				}
			}
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

	private IType getITypeFromElement(String elementType, String name) {
		IType iType = null;
		if (elementType.startsWith("xs:") || elementType.startsWith("xsd:")) {
			iType = new PrimitiveType(elementType.substring(
					elementType.indexOf(':') + 1).toUpperCase(), name);
		} else if (elementType.startsWith("tns:") || !elementType.contains(":")
				|| elementType.startsWith("ns:")
				|| elementType.startsWith("ax21:")) {
			iType = getIType(getTypeFromElement(elementType), name);
		} else {
			iType = new PrimitiveType(elementType.toUpperCase(), name);
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
		NodeList elements = document.getElementsByTagNameNS("*", "element");
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

	private Node getMethodChild(Node method, String childName) {
		NodeList methodChildren = method.getChildNodes();
		for (int i = 0; i < methodChildren.getLength(); i++) {
			if (equalsIgnoreNamespace(methodChildren.item(i).getNodeName(),
					childName)) {
				return methodChildren.item(i);
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

	public static void main(String[] args) {
		System.out.println("Diff started");
		WADLParser parser1 = new WADLParser(new File(
				"files/imdbMerged.wadl"));
		WADLParser parser2 = new WADLParser(new File(
				"files/imdbMerged.wadl"));

		Delta delta = parser1.getService().diff(parser2.getService());
		

		DeltaUtil.findMoveDeltas(delta);
		delta.printDelta(0);
		System.out.println("Diff finished");
		System.out.println("Finished!!");
	}

}
