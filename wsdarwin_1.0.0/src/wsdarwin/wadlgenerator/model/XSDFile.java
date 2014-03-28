package wsdarwin.wadlgenerator.model;

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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import wsdarwin.model.ComplexType;
import wsdarwin.model.IType;
import wsdarwin.model.PrimitiveType;
import wsdarwin.model.SimpleType;

public class XSDFile implements WADLElement{

	private String filename;
	private HashMap<String, IType> types;
	private HashMap<String, XSDElement> elements;
	private ComplexType responseType;

	public XSDFile(String filename, HashMap<String, IType> types,
			HashMap<String, XSDElement> elements) {
		this.filename = filename;
		this.types = types;
		this.elements = elements;
	}

	public XSDFile(String filename) {
		this.filename = filename;
		this.types = new HashMap<String, IType>();
		this.elements = new HashMap<String, XSDElement>();
	}

	public void addElement(String name, XSDElement element) {
		elements.put(name, element);
	}

	public void addType(String name, IType type) {
		types.put(name, type);
	}

	public String getFilename() {
		return filename;
	}

	public HashMap<String, IType> getTypes() {
		return types;
	}

	public HashMap<String, XSDElement> getElements() {
		return elements;
	}

	public ComplexType getResponseType() {
		return responseType;
	}

	public void setResponseType(ComplexType responseType) {
		this.responseType = responseType;
	}

	public ArrayList<String> sortedElementAndTypeNames() {
		ArrayList<String> names = new ArrayList<String>();
		names.addAll(elements.keySet());
		names.addAll(types.keySet());
		Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	public void readXSD() throws ParserConfigurationException, SAXException,
			IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document xmlDoc = builder.parse(new File(filename));

		NodeList types = xmlDoc.getElementsByTagName("complexType");
		for (int i = 0; i < types.getLength(); i++) {
			NodeList typeChildren = types.item(i).getChildNodes();
			ComplexType type = new ComplexType(types.item(i).getAttributes()
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
			if (!(element.getType() instanceof PrimitiveType)) {
				this.addElement(element.getName(), element);
			}
		}
		responseType = findResponseType();
	}

	private IType createType(String name) {
		if (name.equals("string")) {
			return PrimitiveType.STRING;
		} else if (name.equals("integer")) {
			return PrimitiveType.INT;
		} else if (name.equals("boolean")) {
			return PrimitiveType.BOOLEAN;
		} else if (name.equals("double")) {
			return PrimitiveType.DOUBLE;
		} else if (this.getTypes().containsKey(name)) {
			return this.getTypes().get(name);
		} else {
			return new ComplexType(name);
		}
	}

	private String getStringWithoutNamespace(String nodeValue) {
		if (nodeValue.contains(":")) {
			return nodeValue.split(":")[1];
		} else {
			return nodeValue;
		}
	}

	public void diffXSD(XSDFile file) {
		if(this.responseType == null && file.responseType != null) {
			this.setResponseType(file.getResponseType());
		}
		HashSet<XSDElement> elementsAdded = new HashSet<XSDElement>();
		HashSet<IType> typesAdded = new HashSet<IType>();
		HashMap<IType, HashSet<XSDElement>> changedTypes = new HashMap<IType, HashSet<XSDElement>>();
		for (String element : file.getElements().keySet()) {
			if (!this.getElements().containsKey(element)) {
				elementsAdded.add(file.getElements().get(element));
			}
		}

		for (String type : file.getTypes().keySet()) {
			if (!this.getTypes().containsKey(type)) {
				typesAdded.add(file.getTypes().get(type));
			} else {
				changedTypes.put(this.getTypes().get(type), this.getTypes()
						.get(type).diff(file.getTypes().get(type)));
			}
		}

		mergeXSDFiles(elementsAdded, typesAdded, changedTypes);
	}

	private void mergeXSDFiles(HashSet<XSDElement> elementsAdded,
			HashSet<IType> typesAdded,
			HashMap<IType, HashSet<XSDElement>> changedTypes) {
		putAllElements(elementsAdded);
		putAllTypes(typesAdded);
		for (IType type : changedTypes.keySet()) {
			if (type instanceof ComplexType) {
				ComplexType complexType = (ComplexType)type;
				for (XSDElement element : changedTypes.get(type)) {
					complexType.addElement(element);
				}
			}
			else if (type instanceof SimpleType) {
				//TODO: I might need to put something here... list frequencies?
			}
		}
	}

	private void putAllTypes(Set<IType> types) {
		for (IType type : types) {
			this.types.put(type.getName(), type);
		}

	}

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
		 

		HashMap<String, IType> typesIncluded = new HashMap<String, IType>();
		HashMap<String, IType> typesDeleted = new HashMap<String, IType>();
		HashMap<String, IType> typesAdded = new HashMap<String, IType>();

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
				if (typesAdded.get(nameAdded) instanceof ComplexType
						&& typesDeleted.get(nameDeleted) instanceof ComplexType) {
					ComplexType addedType = (ComplexType) typesAdded
							.get(nameAdded);
					ComplexType deletedType = (ComplexType) typesDeleted
							.get(nameDeleted);
					if (addedType.getElements().equals(
							deletedType.getElements())) {
						deltas.add(typesAdded.get(nameAdded).compare(
								typesDeleted.get(nameDeleted)));
						typesNotAdded.add(nameAdded);
						typesNotDeleted.add(nameDeleted);
					}
				}
				else if(typesAdded.get(nameAdded) instanceof SimpleType && typesDeleted.get(nameDeleted) instanceof SimpleType) {
					SimpleType addedType = (SimpleType)typesAdded.get(nameAdded);
					SimpleType deletedType = (SimpleType)typesDeleted.get(nameDeleted);
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
		for (XSDElement element : ((ComplexType) type).getElements().values()) {
			DeleteDelta deleteDelta = new DeleteDelta(element, null);
			delta.addDelta(deleteDelta);
			deleteDelta.setParent(delta);
		}
	}

	public void addChildrenElements(WSElement type, AddDelta delta) {
		for (XSDElement element : ((ComplexType) type).getElements().values()) {
			AddDelta addDelta = new AddDelta(null, element);
			delta.addDelta(addDelta);
			addDelta.setParent(delta);
		}
	}*/

	public ComplexType findResponseType() {
		boolean hasParent = false;
		for (IType type1 : this.types.values()) {
			for (IType type2 : this.types.values()) {
				if (type2 instanceof ComplexType) {
					ComplexType complexType = (ComplexType) type2;
					for (XSDElement element : complexType.getElements()
							.values()) {
						if (element.getType().equals(type1)) {
							hasParent = true;
						}
					}
				}
			}
			if (!hasParent) {
				if (type1 instanceof ComplexType) {
					return (ComplexType)type1;
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toString() {
		return filename;
	}

	@Override
	public boolean mapElement(WADLElement element) {
		// TODO Auto-generated method stub
		return false;
	}

}
