package wsdarwin.wadlgenerator.model.xsd;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import wsdarwin.wadlgenerator.model.Representation;
import wsdarwin.wadlgenerator.model.Resource;
import wsdarwin.wadlgenerator.model.WADLElement;

public class XSDFile implements WADLElement {

	//private HashMap<String, XSDIType> types;
	private HashMap<String, XSDElement> elements;
	private XSDElement responseElement;
	private HashMap<XSDElement, HashSet<XSDElement>> changedElements;
	// newly added in order to keep track of value frequency of all xsdelements
	//private TreeMap<XSDElement, Integer> valueFrequencies;

	public XSDFile(HashMap<String, XSDElement> elements) {
		//this.types = types;
		this.elements = elements;
		this.changedElements = new HashMap<XSDElement, HashSet<XSDElement>>();
	}
	
	public XSDFile() {
		//this.types = new HashMap<String, XSDIType>();
		this.elements = new HashMap<String, XSDElement>();
		this.changedElements = new HashMap<XSDElement, HashSet<XSDElement>>();
	}
	
	//public TreeMap<XSDElement, Integer> getValueFrequencies() {
	//	return this.valueFrequencies;
	//}
	
	public void addElement(String name, XSDElement element) {
		//XSDElement thisElement = isNewElement(element);
		//if (thisElement == null) {
			elements.put(name, element);
		//}
		//else {
		//	thisElement.getType().compareToMerge(element.getType());
		//}
	}

	/*public void addType(String name, XSDIType type) {
		types.put(name, type);
	}*/

	/*public String getFilename() {
		return filename;
	}*/

	/*public HashMap<String, XSDIType> getTypes() {
		return types;
	}*/

	public HashMap<String, XSDElement> getElements() {
		return elements;
	}

	public XSDElement getResponseElement() {
		return responseElement;
	}

	public void setResponseElement(XSDElement responseType) {
		this.responseElement = responseType;
	}

	/*public ArrayList<String> sortedElementAndTypeNames() {
		ArrayList<String> names = new ArrayList<String>();
		names.addAll(elements.keySet());
		//names.addAll(types.keySet());
		Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
		return names;
	}*/
	
	// ***************************************** BELOW UNCOMMENTED
	
	// find the xs:element with type 'xs_element_type' and return its name ('variableName' in XSDComplexType)
	public String findXSElement(NodeList typesAndElementsList, String xs_element_type){
		for (int k = 0; k < typesAndElementsList.getLength(); k++){
			Node node = typesAndElementsList.item(k);
			if ( getStringWithoutNamespace(node.getNodeName()).equals("element") ) {
				if ( getStringWithoutNamespace(typesAndElementsList.item(k).getAttributes().getNamedItem("type").getNodeValue()).
						equals(xs_element_type) ) {
					return typesAndElementsList.item(k).getAttributes().getNamedItem("name").getNodeValue();
				}
			}
		}
		return null;
	}
	
	public void readXSD(Document xmlDoc) throws ParserConfigurationException, SAXException,
	IOException {
		//DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setNamespaceAware(true);
		//DocumentBuilder builder = factory.newDocumentBuilder();
		//Document xmlDoc = builder.parse(new File(filename));
		
		NodeList grammars = xmlDoc.getElementsByTagName("grammars");
		NodeList schemas = grammars.item(0).getChildNodes();
		
		//System.out.println("<<< READING THE XSD OF THE FILE >>>");
		
		// for all the 'xs:complexType' elements in <xs:schema> with index 0:
		for (int i = 0; i < schemas.item(0).getChildNodes().getLength(); i++){
			Node childNode = schemas.item(0).getChildNodes().item(i);
			if (getStringWithoutNamespace(childNode.getNodeName()).equals("complexType")) {
				
				String variableName = findXSElement(schemas.item(0).getChildNodes(), childNode.getAttributes().getNamedItem("name").getNodeValue());
				//TODO: Find cases when variableName can be null ?
				
				XSDComplexType type = new XSDComplexType(childNode.getAttributes().getNamedItem("name").getNodeValue(), variableName);
				//this.addType(type.getName(), type);
				NodeList typeChildren = childNode.getChildNodes();
				for (int k = 0; k < typeChildren.getLength(); k++) {
					if (typeChildren.item(k).getNodeName().equals("sequence")) {
						NodeList sequenceChildren = typeChildren.item(k)
								.getChildNodes();
						for (int j = 0; j < sequenceChildren.getLength(); j++) {
							if (sequenceChildren.item(j).getNodeName()
									.equals("element")) {
								XSDElement element = new XSDElement(
										sequenceChildren.item(j).getAttributes()
												.getNamedItem("name")
												.getNodeValue(),
										createType(getStringWithoutNamespace(sequenceChildren
												.item(j).getAttributes()
												.getNamedItem("type")
												.getNodeValue())));
								type.addElement(element);
							}
						}
					}
				}
				
			} 
		}
		
		// for all the 'xs:element' elements in <xs:schema> with index 0:
		for (int i = 0; i < schemas.item(0).getChildNodes().getLength(); i++){
			Node childNode = schemas.item(0).getChildNodes().item(i);
			if (getStringWithoutNamespace(childNode.getNodeName()).equals("element")){
				XSDElement element = new XSDElement(childNode.getAttributes().getNamedItem("name").getNodeValue(),
						createType(getStringWithoutNamespace(childNode.getAttributes().getNamedItem("type")
								.getNodeValue())));
				if (!(element.getType() instanceof XSDPrimitiveType)) {
					this.addElement(element.getName(), element);
				}
				
			}
		}
		
		responseElement = findResponseType();
		
		//System.out.println("<<< END OF READING THE XSD OF THE FILE >>>");
	
	}
	
	/*public void readXSD_BACKUP() throws ParserConfigurationException, SAXException,
			IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document xmlDoc = builder.parse(new File(filename));

		NodeList types = xmlDoc.getElementsByTagName("complexType");
		for (int i = 0; i < types.getLength(); i++) {
			NodeList typeChildren = types.item(i).getChildNodes();
			XSDComplexType type = new XSDComplexType(types.item(i).getAttributes()
					.getNamedItem("name").getNodeValue());
			this.addType(type.getName(), type);
			for (int k = 0; k < typeChildren.getLength(); k++) {
				if (typeChildren.item(k).getNodeName().equals("sequence")) {
					NodeList sequenceChildren = typeChildren.item(k)
							.getChildNodes();
					for (int j = 0; j < sequenceChildren.getLength(); j++) {
						if (sequenceChildren.item(j).getNodeName()
								.equals("element")) {
							XSDElement element = new XSDElement(
									sequenceChildren.item(j).getAttributes()
											.getNamedItem("name")
											.getNodeValue(),
									createType(getStringWithoutNamespace(sequenceChildren
											.item(j).getAttributes()
											.getNamedItem("type")
											.getNodeValue())));
							type.addElement(element);
						}
					}
				}
			}
		}

		NodeList elements = xmlDoc.getElementsByTagName("element");
		for (int i = 0; i < elements.getLength(); i++) {
			XSDElement element = new XSDElement(elements.item(i)
					.getAttributes().getNamedItem("name").getNodeValue(),
					createType(getStringWithoutNamespace(elements.item(i)
							.getAttributes().getNamedItem("type")
							.getNodeValue())));
			if (!(element.getType() instanceof XSDPrimitiveType)) {
				this.addElement(element.getName(), element);
			}
		}
		responseElement = findResponseType();
	}
		
	}*/

	private XSDIType createType(String name) {
		if (name.equals("string")) {
			return XSDPrimitiveType.STRING;
		} else if (name.equals("integer")) {
			return XSDPrimitiveType.INT;
		} else if (name.equals("boolean")) {
			return XSDPrimitiveType.BOOLEAN;
		} else if (name.equals("double")) {
			return XSDPrimitiveType.DOUBLE;
		} /*else if (this.getTypes().containsKey(name)) {
			return this.getTypes().get(name);
		}*/ else {								//TODO: when does this occur .. ?
			//return new XSDComplexType(name);	// originally..
			return XSDPrimitiveType.STRING;		// wrong; just a placeholder.
		}
	}
	
	// ***************************************** ABOVE UNCOMMENTED
	
	private String getStringWithoutNamespace(String nodeValue) {
		if (nodeValue.contains(":")) {
			return nodeValue.split(":")[1];
		} else {
			return nodeValue;
		}
	}

	public void compareToMerge(XSDFile file) {
		/*if(this.responseElement == null && file.responseElement != null) {
			this.setResponseElement(file.getResponseElement());
		}*/
		HashSet<XSDElement> elementsAdded = new HashSet<XSDElement>();
		for (String element : file.getElements().keySet()) {

			XSDElement fileElement = file.getElements().get(element);
			if (!this.getElements().containsKey(element)) {
				XSDElement thisElement = isNewElement(fileElement);
				if (thisElement == null) {
					elementsAdded.add(fileElement);
				}
				else {
					changedElements.put(thisElement, thisElement.getType().compareToMerge(fileElement.getType()));
				}
			}
			else {
				changedElements.put(this.elements.get(element), this.elements.get(element).getType().compareToMerge(fileElement.getType()));
			}
		}

		/*for (String type : file.getTypes().keySet()) {
			if (!this.getTypes().containsKey(type)) {
				typesAdded.add(file.getTypes().get(type));
			} else {
				changedElements.put(this.getTypes().get(type), this.getTypes()
						.get(type).compareToMerge(file.getTypes().get(type)));
			}
		}*/

		mergeXSDFiles(elementsAdded);
	}
	
	private XSDElement isNewElement(XSDElement element) {
		for(XSDElement thisElement : this.elements.values()) {
			if(thisElement.equalsAfterRename(element)) {
				element.setName(thisElement.getName());
				element.setType(thisElement.getType());
				return thisElement;
			}
		}
		return null;
	}

	private void mergeXSDFiles(HashSet<XSDElement> elementsAdded) {
		putAllElements(elementsAdded);
		//putAllTypes(typesAdded);
		for (XSDElement element : changedElements.keySet()) {
			XSDIType type = element.getType();
			if (type instanceof XSDComplexType) {
				XSDComplexType xSDComplexType = (XSDComplexType)type;
				for (XSDElement addedElement : changedElements.get(element)) {
					xSDComplexType.addElement(addedElement);
				}
			}
			else if (type instanceof XSDSimpleType) {
				//TODO: I might need to put something here... list frequencies?
			}
		}
	}

	/*private void putAllTypes(Set<XSDIType> types) {
		for (XSDIType type : types) {
			this.types.put(type.getName(), type);
		}
	}*/

	private void putAllElements(Set<XSDElement> elements) {
		for (XSDElement element : elements) {
			this.elements.put(element.getName(), element);
		}
	}

	/*public Delta compare(WSElement element) {
		XSDFile file = null;
		if (element instanceof XSDFile) {
			file = (XSDFile) element;
		} else {
			return null;
		}
		ArrayList<Delta> deltas = new ArrayList<Delta>();

		
		 * HashMap<String, XSDElement> elementsIncluded = new HashMap<String,
		 * XSDElement>(); for(String name : this.elements.keySet()) {
		 * elementsIncluded.put(name, this.elements.get(name)); } for(String
		 * name : this.elements.keySet()) {
		 * if(!file.getElements().containsKey(name)) {
		 * elementsIncluded.remove(name); DeleteDelta delta = new
		 * DeleteDelta(this.elements.get(name), null); deltas.add(delta); } }
		 * for(String name : file.getElements().keySet()) {
		 * if(!this.getElements().containsKey(name)) { AddDelta delta = new
		 * AddDelta(null, file.getElements().get(name)); deltas.add(delta); } }
		 * for(String name : elementsIncluded.keySet()) {
		 * deltas.add(this.getElements
		 * ().get(name).compare(file.getElements().get(name))); }
		 

		HashMap<String, XSDIType> typesIncluded = new HashMap<String, XSDIType>();
		HashMap<String, XSDIType> typesDeleted = new HashMap<String, XSDIType>();
		HashMap<String, XSDIType> typesAdded = new HashMap<String, XSDIType>();

		for (String name : this.types.keySet()) {
			typesIncluded.put(name, this.types.get(name));
		}
		for (String name : this.types.keySet()) {
			if (!file.getTypes().containsKey(name)) {
				typesDeleted.put(name, this.types.get(name));
				typesIncluded.remove(name);
			}
		}
		for (String name : file.getTypes().keySet()) {
			if (!this.types.containsKey(name)) {
				typesAdded.put(name, file.getTypes().get(name));
			}
		}
		ArrayList<String> typesNotAdded = new ArrayList<String>();
		ArrayList<String> typesNotDeleted = new ArrayList<String>();
		for (String nameAdded : typesAdded.keySet()) {
			for (String nameDeleted : typesDeleted.keySet()) {
				if (typesAdded.get(nameAdded) instanceof XSDComplexType
						&& typesDeleted.get(nameDeleted) instanceof XSDComplexType) {
					XSDComplexType addedType = (XSDComplexType) typesAdded
							.get(nameAdded);
					XSDComplexType deletedType = (XSDComplexType) typesDeleted
							.get(nameDeleted);
					if (addedType.getElements().equals(
							deletedType.getElements())) {
						deltas.add(typesAdded.get(nameAdded).compare(
								typesDeleted.get(nameDeleted)));
						typesNotAdded.add(nameAdded);
						typesNotDeleted.add(nameDeleted);
					}
				}
				else if(typesAdded.get(nameAdded) instanceof XSDSimpleType && typesDeleted.get(nameDeleted) instanceof XSDSimpleType) {
					XSDSimpleType addedType = (XSDSimpleType)typesAdded.get(nameAdded);
					XSDSimpleType deletedType = (XSDSimpleType)typesDeleted.get(nameDeleted);
					if(addedType.getList().equals(deletedType.getList())) {
						deltas.add(addedType.compare(deletedType));
						typesNotAdded.add(nameAdded);
						typesNotDeleted.add(nameDeleted);
					}
				}
			}
		}

		for (String notAdded : typesNotAdded) {
			typesAdded.remove(notAdded);
		}
		for (String notDeleted : typesNotDeleted) {
			typesDeleted.remove(notDeleted);
		}
		for (String name : typesAdded.keySet()) {
			AddDelta delta = new AddDelta(null, typesAdded.get(name));
			addChildrenElements(typesAdded.get(name), delta);
			deltas.add(delta);
		}
		for (String name : typesDeleted.keySet()) {
			DeleteDelta delta = new DeleteDelta(typesDeleted.get(name), null);
			deleteChildrenElements(typesDeleted.get(name), delta);
			deltas.add(delta);
		}
		for (String name : typesIncluded.keySet()) {
			// if(!this.elements.get(name).equals(type.getElements().get(name)))
			// {
			deltas.add(this.types.get(name).compare(file.getTypes().get(name)));
			// }
		}

		Delta elementDelta = null;
		if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
			elementDelta = new MatchDelta(this, file);
		} else {
			elementDelta = new ChangeDelta(this, file, "", null, null);
		}

		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}

	public void deleteChildrenElements(WSElement type, DeleteDelta delta) {
		for (XSDElement element : ((XSDComplexType) type).getElements().values()) {
			DeleteDelta deleteDelta = new DeleteDelta(element, null);
			delta.addDelta(deleteDelta);
			deleteDelta.setParent(delta);
		}
	}

	public void addChildrenElements(WSElement type, AddDelta delta) {
		for (XSDElement element : ((XSDComplexType) type).getElements().values()) {
			AddDelta addDelta = new AddDelta(null, element);
			delta.addDelta(addDelta);
			addDelta.setParent(delta);
		}
	}*/

	public XSDElement findResponseType() {
		boolean hasParent = false;
		for (XSDElement element1 : this.elements.values()) {
			for (XSDElement element2 : this.elements.values()) {
				if (element2.getType() instanceof XSDComplexType) {
					XSDComplexType xSDComplexType = (XSDComplexType) element2.getType();
					for (XSDElement element : xSDComplexType.getElements()
							.values()) {
						if (element.getType().equals(element1.getType())) {
							hasParent = true;
						}
					}
				}
			}
			if (!hasParent) {
				if (element1.getType() instanceof XSDComplexType) {
					return element1;
				}
			} else {
				hasParent = false;
			}
		}
		return null;
	}

	@Override
	public boolean equalsByName(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean equalsAfterRename(Object o) {
		if(o instanceof XSDFile) {
			return responseElement.equalsAfterRename(((XSDFile)o).responseElement);
		}
		else {
			return false;
		}
	}

	@Override
	public boolean mapElement(WADLElement element) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getIdentifier() {
		// TODO Auto-generated method stub
		return this.responseElement.getIdentifier();
	}

	@Override
	public String toString() {
		return responseElement.getIdentifier();
	}

	
}
