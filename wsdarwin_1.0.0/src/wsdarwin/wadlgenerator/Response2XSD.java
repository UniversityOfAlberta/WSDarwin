package wsdarwin.wadlgenerator;

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

import wsdarwin.model.ComplexType;
import wsdarwin.model.PrimitiveType;
import wsdarwin.model.SimpleType;
import wsdarwin.wadlgenerator.model.xsd.*;

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

	public void buildXSDFromXML(String sourceXMLFilename) {
		Document document = getDocumentFromXMLFile(sourceXMLFilename);
		HashMap<String, XSDElement> elements = new HashMap<String, XSDElement>();
		xsdFile = new XSDFile(elements);
		XSDElement element = getElementFromNode(document.getFirstChild());
		xsdFile.setResponseElement(element);
	}

	public void buildXSDFromJSON(File source, String methodID) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, XSDElement> elements = new HashMap<String, XSDElement>();
		xsdFile = new XSDFile(elements);
		BufferedReader in = new BufferedReader(new FileReader(source));
		String firstLine = in.readLine();
			if (firstLine.startsWith("[")) {
				List<Map<String, Object>> jsonList = mapper.readValue(source,
						new TypeReference<List<Map<String, Object>>>() {
						});
				if (!jsonList.isEmpty()) {
					XSDElement element = getElementFromJSONNode(
							jsonList.get(0), "", methodID);
					for (Map<String, Object> map : jsonList) {
						element.getType().compareToMerge(
								getElementFromJSONNode(map, "", methodID)
										.getType());
					}
					xsdFile.setResponseElement(element);
				}
			} else {
				Map<String, Object> jsonMap = mapper.readValue(source,
						new TypeReference<Map<String, Object>>() {
						});
				XSDElement element = getElementFromJSONNode(jsonMap, "",
						methodID);
				xsdFile.setResponseElement(element);
			}
		in.close();
	}

	private XSDComplexType getTypeFromJSONNode(Map<String, Object> map, String typeName, String methodID) {
		XSDComplexType type = null;
		XSDElement element = null;
		if (typeName.equals("")) {
			String name = "";
			if(methodID.equals("")) {
				name = "overallResponse";
			}
			else {
				name = methodID+"Response";
			}
			type = new XSDComplexType(name+"Type", name);
			element = new XSDElement(name, type);
		}
		else {
			type = new XSDComplexType(typeName, lowerFirstLetter(typeName.replace("Type", "")));
			element = new XSDElement(lowerFirstLetter(type.getNameWithoutType()), type);
		}
		xsdFile.addElement(element.getName(), element);
		for(String s : map.keySet()) {
			type.addElement(processJSONChildren(s, map.get(s)));
		}
		return type;
	}
	
	private XSDElement getElementFromJSONNode(Map<String, Object> map, String typeName, String methodID) {
		XSDComplexType type = null;
		XSDElement element = null;
		if (typeName.equals("")) {
			String name = "";
			if(methodID.equals("")) {
				name = "overallResponse";
			}
			else {
				name = methodID+"Response";
			}
			type = new XSDComplexType(name+"Type", name);
			element = new XSDElement(name, type);
		}
		else {
			type = new XSDComplexType(typeName, lowerFirstLetter(typeName.replace("Type", "")));
			element = new XSDElement(lowerFirstLetter(type.getNameWithoutType()), type);
		}
		xsdFile.addElement(element.getName(), element);
		for(String s : map.keySet()) {
			type.addElement(processJSONChildren(s, map.get(s)));
		}
		return element;
	}

	private XSDElement processJSONChildren(String s, Object object) {
		XSDElement element = null;
		//System.out.println("processJSONChildren: [String:] " + s + " [object:] " + object);
		if(object instanceof Map) {
			//System.out.println(object + " [is a Map]");
			XSDComplexType type = null;
			if(xsdFile.getElements().containsKey(s)) {
				type = (XSDComplexType)xsdFile.getElements().get(s).getType();
			}
			else {
				type = new XSDComplexType(s + "Type", s);
			}
			element = new XSDElement(
					s, type);
			Map<String,Object> map = (Map<String,Object>)object;
			for(String key : map.keySet()) {
				type.addElement(processJSONChildren(key, map.get(key)));
			}
			xsdFile.addElement(element.getName(), element);
		}
		else if(object instanceof List) {
			//System.out.println(object + " [is a List]");
			XSDComplexType listType = null;
			if(xsdFile.getElements().containsKey(s)) {
				listType = (XSDComplexType)xsdFile.getElements().get(s).getType();
			}
			else {
				listType = new XSDComplexType(s + "ListType", s);
			}
			List<Object> list = (List<Object>)object;
			for(Object listObject : list) {
				if (listObject instanceof String) {
					String value = (String) listObject;
					if (Pattern.matches("^[-+]?\\d*$", value)
							&& value.length() <= 10) {
						XSDElement listElement = new XSDElement(listType.getName()+"-item",
								XSDPrimitiveType.INT, Integer.parseInt(value));
						listElement.setMinOccurs(0);
						listElement.setMaxOccurs("unbounded");
						listType.addElement(listElement);
					} else if (Pattern.matches("^[-+]?\\d*$", value)
							&& value.length() <= 19) {
						XSDElement listElement = new XSDElement(listType.getName()+"-item",
								XSDPrimitiveType.LONG, Long.parseLong(value));
						listElement.setMinOccurs(0);
						listElement.setMaxOccurs("unbounded");
						listType.addElement(listElement);
					} else if (Pattern.matches(
							"^[-+]?[0-9]+[.]?[0-9]*([eE][-+]?[0-9]+)?$", value)) {
						XSDElement listElement = new XSDElement(listType.getName()+"-item",
								XSDPrimitiveType.DOUBLE, Double.parseDouble(value));
						listElement.setMinOccurs(0);
						listElement.setMaxOccurs("unbounded");
						listType.addElement(listElement);
					} else if (Pattern.matches("true|false", value)) {
						XSDElement listElement = new XSDElement(listType.getName()+"-item",
								XSDPrimitiveType.BOOLEAN, Boolean.parseBoolean(value));
						listElement.setMinOccurs(0);
						listElement.setMaxOccurs("unbounded");
						listType.addElement(listElement);
					} else {
						XSDElement listElement = new XSDElement(listType.getName()+"-item",
								XSDPrimitiveType.STRING, value);
						listElement.setMinOccurs(0);
						listElement.setMaxOccurs("unbounded");
						listType.addElement(listElement);
					}
				}
				else if(listObject instanceof Double) {
					XSDElement listElement = new XSDElement(listType.getName()+"-item",
							XSDPrimitiveType.DOUBLE, listObject);
					listElement.setMinOccurs(0);
					listElement.setMaxOccurs("unbounded");
					listType.addElement(listElement);
				}
				else if (listObject instanceof Map) {
					Map<String, Object> map = (Map<String, Object>) listObject;
					XSDComplexType type = getTypeFromJSONNode(map, s + "ItemType", "");
					XSDElement xsdElement = new XSDElement(
							lowerFirstLetter(type.getNameWithoutType()), type);
					xsdFile.addElement(xsdElement.getName(), xsdElement);
					XSDElement listElement = new XSDElement(s + "ItemType-item", type);
					listElement.setMinOccurs(0);
					listElement.setMaxOccurs("unbounded");
					listType.addElement(listElement);
				}
				element = new XSDElement(s, listType);
				xsdFile.addElement(element.getName(), element);
			}
		}
		else {
			//System.out.println(object + " [is ELSE]");
			String value = ""+object;
			if(value.equals("")) {
				element = new XSDElement(s,XSDPrimitiveType.STRING,value);
			}
			//else if (Pattern.matches("^[-+]?\\d*$", value) && value.length()<=10) {
			else if (Pattern.matches("^[-+]?\\d*$", value) && isInteger(value)) {
				element = new XSDElement(s,
						XSDPrimitiveType.INT, Integer.parseInt(value));				
			//} else if (Pattern.matches("^[-+]?\\d*$", value) && value.length()<=19) {
			} else if (Pattern.matches("^[-+]?\\d*$", value) && isLong(value)) {
				element = new XSDElement(s,
						XSDPrimitiveType.LONG, Long.parseLong(value));
			//} else if (Pattern.matches("^[-+]?[0-9]+[.]?[0-9]*([eE][-+]?[0-9]+)?$", value)) {
			} else if (Pattern.matches("^[-+]?[0-9]+[.]?[0-9]*([eE][-+]?[0-9]+)?$", value) && isDouble(value) ) {
				element = new XSDElement(s,
						XSDPrimitiveType.DOUBLE, Double.parseDouble(value));
			} else if (Pattern.matches("true|false", value)) {
				element = new XSDElement(s,
						XSDPrimitiveType.BOOLEAN, Boolean.parseBoolean(value));
			} else if (Pattern.matches("^[-+]?\\d*$", value) && isShort(value)) {
				element = new XSDElement(s,
						XSDPrimitiveType.SHORT, value);
			} else if (Pattern.matches("^[-+]?\\d*$", value) && isByte(value)) {
				element = new XSDElement(s,
						XSDPrimitiveType.BYTE, value);
			} else if (Pattern.matches("^(\\d{4})-(\\d{2})-(\\d{2})[T]?(\\d{2}):(\\d{2}):(\\d{2})[Z]?$", value)) {
				element = new XSDElement(s,
						XSDPrimitiveType.DATETIME, value);
			} else if (matchesURI(value)) {
				element = new XSDElement(s,
						XSDPrimitiveType.ANYURI, value);
			} else if (matchesEmail(value)) {
				element = new XSDElement(s,
						XSDPrimitiveType.EMAIL, value);
			} else if (Pattern.matches("^(\\d{4})-(\\d{2})-(\\d{2})$", value)) {
				element = new XSDElement(s,
						XSDPrimitiveType.DATE, value);
			}
			else {
				element = new XSDElement(s,
						XSDPrimitiveType.STRING, value);
			}
		}
		
		return element;
	}
	
	public static boolean matchesEmail(String value){
		String regex = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		if (Pattern.matches(regex, value)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean matchesURI(String value) {
		String regex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
		if (Pattern.matches(regex, value)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean isInteger(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
	}
	
	public static boolean isShort(String s) {
	    try { 
	        Short.parseShort(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
	}
	
	public static boolean isByte(String s) {
	    try { 
	        Byte.parseByte(s);
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
	}

	public static boolean isLong(String s) {
	    try { 
	        Long.parseLong(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
	}
	
	public static boolean isDouble(String s) {
	    try { 
	        Double.parseDouble(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
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

	private XSDComplexType getTypeFromNode(Node node) {
		XSDComplexType type = null;
		if (node.hasChildNodes()) {
			type = new XSDComplexType(node.getNodeName() + "Type", lowerFirstLetter(node.getNodeName()));
			XSDElement element = new XSDElement(
					lowerFirstLetter(type.getNameWithoutType()), type);
			xsdFile.addElement(element.getName(), element);
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				if (!isText(node.getChildNodes().item(i))) {
					type.addElement(processChildren(node.getChildNodes()
							.item(i)));
				}
			}
		}
		return type;
	}
	
	private XSDElement getElementFromNode(Node node) {
		XSDComplexType type = null;
		XSDElement element = null;
		if (node.hasChildNodes()) {
			type = new XSDComplexType(node.getNodeName() + "Type", lowerFirstLetter(node.getNodeName()));
			element = new XSDElement(
					lowerFirstLetter(type.getNameWithoutType()), type);
			xsdFile.addElement(element.getName(), element);
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				if (!isText(node.getChildNodes().item(i))) {
					type.addElement(processChildren(node.getChildNodes()
							.item(i)));
				}
			}
		}
		return element;
	}

	private XSDElement processChildren(Node node) {
		XSDElement element = null;
		if (hasNonTextChildNodes(node)) {
			XSDComplexType type = new XSDComplexType(node.getNodeName() + "Type", lowerFirstLetter(node.getNodeName()));
			element = new XSDElement(
					lowerFirstLetter(type.getNameWithoutType()), type);
			xsdFile.addElement(element.getName(), element);
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				if (!isText(node.getChildNodes().item(i))) {
					type.addElement(processChildren(node.getChildNodes()
							.item(i)));
				}
			}
		} else {
			if (Pattern.matches("^[-+]?\\d*$", node.getTextContent()) && node.getTextContent().length()<=10) {
				element = new XSDElement(lowerFirstLetter(node.getNodeName()),
						XSDPrimitiveType.INT, Integer.parseInt(node
								.getTextContent()));
			} else if (Pattern.matches("^[-+]?\\d*$", node.getTextContent()) && node.getTextContent().length()<=19) {
				element = new XSDElement(lowerFirstLetter(node.getNodeName()),
						XSDPrimitiveType.LONG, Long.parseLong(node
								.getTextContent()));
			} else if (Pattern.matches(
					"^[-+]?[0-9]+[.]?[0-9]*([eE][-+]?[0-9]+)?$",
					node.getTextContent())) {
				element = new XSDElement(lowerFirstLetter(node.getNodeName()),
						XSDPrimitiveType.DOUBLE, Double.parseDouble(node
								.getTextContent()));
			} else if (Pattern.matches("true|false", node.getTextContent())) {
				element = new XSDElement(lowerFirstLetter(node.getNodeName()),
						XSDPrimitiveType.BOOLEAN, Boolean.parseBoolean(node
								.getTextContent()));
			} else {
				element = new XSDElement(lowerFirstLetter(node.getNodeName()),
						XSDPrimitiveType.STRING, node.getTextContent());
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
	
	public ComplexType convertFromXSD(XSDComplexType xsdType) {
		ComplexType type = new ComplexType(xsdType.getName(), xsdType.getVariableName());
		for(XSDElement childType : xsdType.getElements().values()) {
			if(childType.getType() instanceof XSDPrimitiveType) {
				type.addElement(childType.getName(), new PrimitiveType(childType.getType().getName(), childType.getName()));
			}
			else if(childType.getType() instanceof XSDSimpleType) {
				type.addElement(childType.getName(), new SimpleType(childType.getType().getName(), childType.getName()));
			}
			else if(childType.getType() instanceof XSDComplexType) {
				type.addElement(childType.getName(), convertFromXSD((XSDComplexType)childType.getType()));
			}
		}
		return type;
	}
}
