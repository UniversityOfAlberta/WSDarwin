package wsdarwin.wadlgenerator.model.xsd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import wsdarwin.wadlgenerator.model.WADLElement;

public class XSDComplexType implements Comparable<XSDComplexType>, XSDIType{
	
	private String name;
	private String variableName;
	private HashMap<String, XSDElement> elements;

	public XSDComplexType(String name, String variableName) {
		this.name = name;
		this.variableName = variableName;
		this.elements = new HashMap<String, XSDElement>();
	}

	public XSDComplexType(String name, HashMap<String, XSDElement> elements) {
		this.name = name;
		this.elements = elements;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public HashMap<String, XSDElement> getElements() {
		return elements;
	}
	
	public void addElement(XSDElement element) {
		if (element != null) {
			elements.put(element.getName(), element);
		}
	}
	
	public String getNameWithoutType() {
		return name.replace("Type", "");
	}

	public String toString() {
		return name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof XSDComplexType)) {
			return false;
		}
		XSDComplexType other = (XSDComplexType) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(XSDComplexType o) {
		return o.name.compareTo(name);
	}

	public HashSet<XSDElement> diff(XSDIType type) {
		
		HashSet<XSDElement> elementsAdded = new HashSet<XSDElement>();
		XSDComplexType xSDComplexType = null;
		if(type instanceof XSDComplexType) {
			xSDComplexType = (XSDComplexType)type;
		}
		else {
			return elementsAdded;
		}
		for(String element : xSDComplexType.getElements().keySet()) {
			if(!this.getElements().containsKey(element)) {
				
				if (xSDComplexType.getElements().get(element).getType() instanceof XSDPrimitiveType) {
					/*complexType.getElements().get(element).addTypeFrequency(complexType.getElements().get(element).getType().getName(), 1);
					complexType.getElements().get(element).addValueFrequency(complexType.getElements().get(element).getValue(), 1);
					complexType.getElements().get(element).addType2Value(complexType.getElements().get(element).getType().getName(), complexType.getElements().get(element).getValue());*/
				}
				elementsAdded.add(xSDComplexType.getElements().get(element));
			}
			else {
				XSDElement retrievedElement = this.elements.get(element);
				if (retrievedElement.getType() instanceof XSDPrimitiveType) {
					/*retrievedElement.addTypeFrequency(
							retrievedElement.getType().getName(), 1);
					retrievedElement.addValueFrequency(
							retrievedElement.getValue(), 1);
					retrievedElement.addType2Value(retrievedElement.getType().getName(), retrievedElement.getValue());*/
				}
			}
		}
		return elementsAdded;
	}

	/*@Override
	public Delta compare(WSElement element) {
		XSDComplexType type = null;
		if(element instanceof XSDComplexType) {
			type = (XSDComplexType)element;
		}
		else {
			return null;
		}
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		HashMap<String, XSDElement> elementsAdded = new HashMap<String, XSDElement>();
		HashMap<String, XSDElement> elementsDeleted = new HashMap<String, XSDElement>();
		HashMap<String, XSDElement> elementsIncluded = new HashMap<String , XSDElement>();
		for(String name : this.elements.keySet()) {
			elementsIncluded.put(name, this.elements.get(name));
		}
		if(!type.getName().equals(this.getName())) {
			ChangeDelta delta = new ChangeDelta(this, type, "name", this.getName(), type.getName());
			deltas.add(delta);
		}
		for(String name : this.elements.keySet()) {
			if(!type.getElements().containsKey(name)) {
				elementsDeleted.put(name, this.elements.get(name));
				elementsIncluded.remove(name);
			}
		}
		for(String name : type.getElements().keySet()) {
			if(!this.getElements().containsKey(name)) {
				elementsAdded.put(name, type.getElements().get(name));
			}
		}
		ArrayList<String> elementsNotAdded = new ArrayList<String>();
		ArrayList<String> elementsNotDeleted = new ArrayList<String>();
		for(String nameAdded : elementsAdded.keySet()) {
			for(String nameDeleted : elementsDeleted.keySet()) {
				if (elementsAdded.get(nameAdded).getType() instanceof XSDComplexType && elementsDeleted.get(nameDeleted).getType() instanceof XSDComplexType) {
					if (((XSDComplexType)elementsAdded
							.get(nameAdded).getType())
							.getElements()
							.equals(((XSDComplexType)elementsDeleted.get(nameDeleted).getType()).getElements())) {
						deltas.add(elementsAdded.get(nameAdded).compare(elementsDeleted.get(nameDeleted)));
						elementsNotAdded.add(nameAdded);
						elementsNotDeleted.add(nameDeleted);
					}
				}
				else if(elementsAdded.get(nameAdded).getType() instanceof XSDPrimitiveType && elementsDeleted.get(nameDeleted).getType() instanceof XSDPrimitiveType) {
					if(elementsAdded.get(nameAdded).getName().equals(elementsDeleted.get(nameDeleted).getName())) {
						deltas.add(elementsAdded.get(nameAdded).compare(elementsDeleted.get(nameDeleted)));
						elementsNotAdded.add(nameAdded);
						elementsNotDeleted.add(nameDeleted);
					}
				}
			}
		}
		for(String notAdded : elementsNotAdded) {
			elementsAdded.remove(notAdded);
		}
		for(String notDeleted : elementsNotDeleted) {
			elementsDeleted.remove(notDeleted);
		}
		for(String name : elementsAdded.keySet()) {
			AddDelta delta = new AddDelta(null, elementsAdded.get(name).getType());
			addChildrenElements(elementsAdded.get(name).getType(), delta, elementsAdded.get(name));
			deltas.add(delta);
		}
		for(String name : elementsDeleted.keySet()) {
			DeleteDelta delta = new DeleteDelta(elementsDeleted.get(name).getType(), null);
			deleteChildrenElements(elementsDeleted.get(name).getType(), delta, elementsDeleted.get(name));
			deltas.add(delta);
		}
		for(String name : elementsIncluded.keySet()) {
			//if(!this.elements.get(name).equals(type.getElements().get(name))) {
				deltas.add(this.elements.get(name).compare(type.getElements().get(name)));
			//}
		}
		if(DeltaUtil.containsOnlyMatchDeltas(deltas)) {
			elementDelta = new MatchDelta(this, type);
			elementDelta.addAllDeltas(deltas);
			elementDelta.adoptOrphanDeltas();
		}
		else {
			elementDelta = new ChangeDelta(this, type, "", null, null);
			elementDelta.addAllDeltas(deltas);
			elementDelta.adoptOrphanDeltas();
		}
		return elementDelta;
	}*/
	
	/*public void deleteChildrenElements(WSElement type, DeleteDelta delta, XSDElement xsdElement) {
		if(type instanceof XSDComplexType) {
			DeleteDelta deleteDelta = null;
			if (!delta.getTarget().equals(type)) {
				deleteDelta = new DeleteDelta(type, null);
				delta.addDelta(deleteDelta);
				deleteDelta.setParent(delta);
			}
			for(XSDElement element : ((XSDComplexType)type).getElements().values()) {
				if(deleteDelta == null) {
					deleteChildrenElements(element.getType(), delta, element);
				}
				else {
					deleteChildrenElements(element.getType(), deleteDelta, element);
				}
			}
		}
		else {
			if (!delta.getSource().equals(type)) {
				DeleteDelta deleteDelta = new DeleteDelta(xsdElement, null);
				delta.addDelta(deleteDelta);
				deleteDelta.setParent(delta);
			}
		}
	}
	
	public void addChildrenElements(WSElement type, AddDelta delta, XSDElement xsdElement) {
		if(type instanceof XSDComplexType) {
			AddDelta addDelta = null;
			if (!delta.getTarget().equals(type)) {
				addDelta = new AddDelta(null, type);
				delta.addDelta(addDelta);
				addDelta.setParent(delta);
			}
			for(XSDElement element : ((XSDComplexType)type).getElements().values()) {
				if (addDelta == null) {
					addChildrenElements(element.getType(), delta, element);
				}
				else {
					addChildrenElements(element.getType(), addDelta, element);
				}
			}
		}
		else {
			if (!delta.getTarget().equals(type)) {
				AddDelta addDelta = new AddDelta(null, xsdElement);
				delta.addDelta(addDelta);
				addDelta.setParent(delta);
			}
		}
	}*/

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof XSDComplexType) {
			return name.equals(((XSDComplexType)o).name);
		}
		else {
			return false;
		}
	}

	@Override
	public boolean equalsAfterRename(Object o) {
		if(o instanceof XSDComplexType) {
			return elements.equals(((XSDComplexType)o).elements);
		}
		else {
			return false;
		}
	}

	@Override
	public boolean mapElement(WADLElement element) {
		HashMap<String, XSDIType> mapped = new HashMap<String, XSDIType>();
		HashMap<String, XSDIType> added = new HashMap<String, XSDIType>();
		HashMap<String, XSDIType> deleted = new HashMap<String, XSDIType>();
		mapByID(element, mapped, added, deleted);
		mapByStructure(element, mapped, added, deleted);		
		return false;
	}

	private void mapByStructure(WADLElement element,
			HashMap<String, XSDIType> mapped, HashMap<String, XSDIType> added,
			HashMap<String, XSDIType> deleted) {
		// TODO Auto-generated method stub
		
	}

	private void mapByID(WADLElement element, HashMap<String, XSDIType> mapped,
			HashMap<String, XSDIType> added, HashMap<String, XSDIType> deleted) {
				
	}

	@Override
	public String getIdentifier() {
		// TODO Auto-generated method stub
		return null;
	}
}
