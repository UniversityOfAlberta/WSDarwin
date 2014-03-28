package wsdarwin.wadlgenerator.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Response2XSD {

	private XSDFile xsdFile;

	public XSDFile getXSDFile() {
		return xsdFile;
	}

	private Document getDocumentFromXMLFile(String filename) {
		Document document = null;
		try {
			File responseXMLFile = new File(filename);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder;

			dBuilder = dbFactory.newDocumentBuilder();
			document = dBuilder.parse(responseXMLFile);
			document.getDocumentElement().normalize();

		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return document;

	}

	public void buildXSDFromXML(String targetXSDFilename,
			String sourceXMLFilename) {
		Document document = getDocumentFromXMLFile(sourceXMLFilename);
		HashMap<String, IType> types = new HashMap<String, IType>();
		HashMap<String, XSDElement> elements = new HashMap<String, XSDElement>();
		xsdFile = new XSDFile(targetXSDFilename, types, elements);
		ComplexType type = getTypeFromNode(document.getFirstChild());
		xsdFile.setResponseType(type);
		xsdFile.addType(type.getName(), type);
	}

	public void buildXSDFromJSON(String targetXSDFilename,
			String sourceJSONFilename, String methodID) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		File source = new File(sourceJSONFilename);
		HashMap<String, IType> types = new HashMap<String, IType>();
		HashMap<String, XSDElement> elements = new HashMap<String, XSDElement>();
		xsdFile = new XSDFile(targetXSDFilename, types, elements);
		BufferedReader in = new BufferedReader(new FileReader(source));
		String firstLine = in.readLine();
		if (firstLine.startsWith("[")) {
			List<Map<String, Object>> jsonList = mapper.readValue(source,
					new TypeReference<List<Map<String, Object>>>() {
					});
			if (!jsonList.isEmpty()) {
				ComplexType type = getTypeFromJSONNode(jsonList.get(0), "",
						methodID);
				for (Map<String, Object> map : jsonList) {
					type.diff(getTypeFromJSONNode(map, "", methodID));
				}
				xsdFile.setResponseType(type);
				xsdFile.addType(type.getName(), type);
			}
		} else {
			Map<String, Object> jsonMap = mapper.readValue(source,
					new TypeReference<Map<String, Object>>() {
					});
			ComplexType type = getTypeFromJSONNode(jsonMap, "", methodID);
			xsdFile.setResponseType(type);
			xsdFile.addType(type.getName(), type);
		}
		in.close();
	}

	private ComplexType getTypeFromJSONNode(Map<String, Object> map, String typeName, String methodID) {
		ComplexType type = null;
		XSDElement element = null;
		if (typeName.equals("")) {
			type = new ComplexType(methodID+"ResponseType");
			element = new XSDElement("response", type);
		}
		else {
			type = new ComplexType(typeName);
			element = new XSDElement(lowerFirstLetter(type.getNameWithoutType()), type);
		}
		xsdFile.addElement(element.getName(), element);
		xsdFile.addType(type.getName(), type);
		for(String s : map.keySet()) {
			type.addElement(processJSONChildren(s, map.get(s)));
		}
		return type;
	}

	private XSDElement processJSONChildren(String s, Object object) {
		XSDElement element = null;
		if(object instanceof Map) {
			ComplexType type = new ComplexType(s + "Type");
			element = new XSDElement(
					s, type);
			Map<String,Object> map = (Map<String,Object>)object;
			for(String key : map.keySet()) {
				type.addElement(processJSONChildren(key, map.get(key)));
			}
			xsdFile.addElement(element.getName(), element);
			xsdFile.addType(type.getName(), type);
		}
		else if(object instanceof List) {
			ComplexType listType = new ComplexType(s+"ListType");
			List<Object> list = (List<Object>)object;
			if (!list.isEmpty()) {
				if (list.get(0) instanceof String) {
					String value = (String) list.get(0);
					if (Pattern.matches("^[-+]?\\d*$", value)
							&& value.length() <= 10) {
						XSDElement listElement = new XSDElement("item",
								PrimitiveType.INT);
						listElement.setMinOccurs(0);
						listElement.setMaxOccurs("unbounded");
						listType.addElement(listElement);
					} else if (Pattern.matches("^[-+]?\\d*$", value)
							&& value.length() <= 19) {
						XSDElement listElement = new XSDElement("item",
								PrimitiveType.LONG);
						listElement.setMinOccurs(0);
						listElement.setMaxOccurs("unbounded");
						listType.addElement(listElement);
					} else if (Pattern.matches(
							"^[-+]?[0-9]+[.]?[0-9]*([eE][-+]?[0-9]+)?$", value)) {
						XSDElement listElement = new XSDElement("item",
								PrimitiveType.DOUBLE);
						listElement.setMinOccurs(0);
						listElement.setMaxOccurs("unbounded");
						listType.addElement(listElement);
					} else if (Pattern.matches("true|false", value)) {
						XSDElement listElement = new XSDElement("item",
								PrimitiveType.BOOLEAN);
						listElement.setMinOccurs(0);
						listElement.setMaxOccurs("unbounded");
						listType.addElement(listElement);
					} else {
						XSDElement listElement = new XSDElement("item",
								PrimitiveType.STRING);
						listElement.setMinOccurs(0);
						listElement.setMaxOccurs("unbounded");
						listType.addElement(listElement);
					}
				} else if (list.get(0) instanceof Map) {
					Map<String, Object> map = (Map<String, Object>) list.get(0);
					ComplexType type = getTypeFromJSONNode(map, s + "ItemType", "");
					XSDElement xsdElement = new XSDElement(
							lowerFirstLetter(type.getNameWithoutType()), type);
					xsdFile.addElement(xsdElement.getName(), xsdElement);
					xsdFile.addType(type.getName(), type);
					XSDElement listElement = new XSDElement("item", type);
					listElement.setMinOccurs(0);
					listElement.setMaxOccurs("unbounded");
					listType.addElement(listElement);
				}
				element = new XSDElement(s, listType);
				xsdFile.addElement(element.getName(), element);
				xsdFile.addType(listType.getName(), listType);
			}
		}
		else {
			String value = ""+object;
			if(value.equals("")) {
				element = new XSDElement(s,PrimitiveType.STRING,value);
			}
			else if (Pattern.matches("^[-+]?\\d*$", value) && value.length()<=10) {
				element = new XSDElement(s,
						PrimitiveType.INT, Integer.parseInt(value));
			} else if (Pattern.matches("^[-+]?\\d*$", value) && value.length()<=19) {
				element = new XSDElement(s,
						PrimitiveType.LONG, Long.parseLong(value));
			} else if (Pattern.matches(
					"^[-+]?[0-9]+[.]?[0-9]*([eE][-+]?[0-9]+)?$",
					value)) {
				element = new XSDElement(s,
						PrimitiveType.DOUBLE, Double.parseDouble(value));
			} else if (Pattern.matches("true|false", value)) {
				element = new XSDElement(s,
						PrimitiveType.BOOLEAN, Boolean.parseBoolean(value));
			} else {
				element = new XSDElement(s,
						PrimitiveType.STRING, value);
			}
		}
		return element;
	}

	private boolean isComplexType(Map<String, Object> map) {
		int counter = 0;
		for (String key : map.keySet()) {
			if (isWrapperType(map.get(key).getClass())) {
				counter++;
			}
		}
		if(counter == map.size()) {
			return false;
		}
		return true;
	}

	private boolean isWrapperType(Class<? extends Object> class1) {
		// TODO Auto-generated method stub
		return class1.equals(Boolean.class) || class1.equals(Character.class)
				|| class1.equals(Byte.class) || class1.equals(Short.class)
				|| class1.equals(Integer.class) || class1.equals(Long.class)
				|| class1.equals(Float.class) || class1.equals(Double.class);
	}

	private ComplexType getTypeFromNode(Node node) {
		ComplexType type = null;
		if (node.hasChildNodes()) {
			type = new ComplexType(node.getNodeName() + "Type");
			XSDElement element = new XSDElement(
					lowerFirstLetter(type.getNameWithoutType()), type);
			xsdFile.addElement(element.getName(), element);
			xsdFile.addType(type.getName(), type);
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				if (!isText(node.getChildNodes().item(i))) {
					type.addElement(processChildren(node.getChildNodes()
							.item(i)));
				}
			}
		}
		xsdFile.addType(type.getName(), type);
		return type;
	}

	private XSDElement processChildren(Node node) {
		XSDElement element = null;
		if (hasNonTextChildNodes(node)) {
			ComplexType type = new ComplexType(node.getNodeName() + "Type");
			element = new XSDElement(
					lowerFirstLetter(type.getNameWithoutType()), type);
			xsdFile.addElement(element.getName(), element);
			xsdFile.addType(type.getName(), type);
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				if (!isText(node.getChildNodes().item(i))) {
					type.addElement(processChildren(node.getChildNodes()
							.item(i)));
				}
			}
		} else {
			if (Pattern.matches("^[-+]?\\d*$", node.getTextContent()) && node.getTextContent().length()<=10) {
				element = new XSDElement(lowerFirstLetter(node.getNodeName()),
						PrimitiveType.INT, Integer.parseInt(node
								.getTextContent()));
			} else if (Pattern.matches("^[-+]?\\d*$", node.getTextContent()) && node.getTextContent().length()<=19) {
				element = new XSDElement(lowerFirstLetter(node.getNodeName()),
						PrimitiveType.LONG, Long.parseLong(node
								.getTextContent()));
			} else if (Pattern.matches(
					"^[-+]?[0-9]+[.]?[0-9]*([eE][-+]?[0-9]+)?$",
					node.getTextContent())) {
				element = new XSDElement(lowerFirstLetter(node.getNodeName()),
						PrimitiveType.DOUBLE, Double.parseDouble(node
								.getTextContent()));
			} else if (Pattern.matches("true|false", node.getTextContent())) {
				element = new XSDElement(lowerFirstLetter(node.getNodeName()),
						PrimitiveType.BOOLEAN, Boolean.parseBoolean(node
								.getTextContent()));
			} else {
				element = new XSDElement(lowerFirstLetter(node.getNodeName()),
						PrimitiveType.STRING, node.getTextContent());
			}
		}
		return element;
	}

	private boolean hasNonTextChildNodes(Node node) {
		if (node.hasChildNodes()) {
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				if (!isText(node.getChildNodes().item(i))) {
					return true;
				}
			}
		}
		return false;
	}

	private String lowerFirstLetter(String text) {
		return text.substring(0, 1).toLowerCase()
				+ text.substring(1, text.length());
	}

	private boolean isText(Node node) {
		return (node instanceof Text);
	}
}
