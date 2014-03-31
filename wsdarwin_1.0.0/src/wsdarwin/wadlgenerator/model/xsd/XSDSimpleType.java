package wsdarwin.wadlgenerator.model.xsd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import wsdarwin.wadlgenerator.model.WADLElement;

public class XSDSimpleType implements XSDIType {
	
	private String name;
	private String restrictionBase;
	private ListElement list;
	private HashSet<String> enumerations;
	
	public XSDSimpleType(String name) {
		this.name = name;
		this.enumerations = new HashSet<String>();
	}
	
	
	
	public String getRestrictionBase() {
		return restrictionBase;
	}



	public void setRestrictionBase(String restrictionBase) {
		this.restrictionBase = restrictionBase;
	}



	public HashSet<String> getEnumerations() {
		return enumerations;
	}



	public ListElement getList() {
		return list;
	}



	public void setList(ListElement list) {
		this.list = list;
	}



	public void addEnumeration(String enumeration) {
		this.enumerations.add(enumeration);
	}

	@Override
	public String getName() {
		return name;
	}
	
	public String toString() {
		return name;
	}
	
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof XSDSimpleType) {
			XSDSimpleType type = (XSDSimpleType)o;
			return type.getName().equals(this.getName()) &&
			type.getEnumerations().equals(this.enumerations);
			
		}
		else {
			return false;
		}
	}

	/*@Override
	public String diff(XSDIType type, int level) {
		level++;
		String diff = indent(level)+this.getName()+"\n";
		if(!((XSDSimpleType) type).getRestrictionBase().equals(this.restrictionBase)) {
			diff += indent(level)+"\tsimpleType changed ("+this.restrictionBase+"->"+((XSDSimpleType) type).getRestrictionBase()+")\n";
		}
		if(!type.getName().equals(this.getName())) {
			diff += indent(level)+"\ttype renamed ("+this.getName()+"->"+type.getName()+")\n";
		}
		for(String name : this.enumerations) {
			if(!((XSDSimpleType) type).getEnumerations().contains(name)) {
				diff += indent(level)+"\ttype deleted("+name+")\n";
			}
		}
		for(String name : ((XSDSimpleType) type).getEnumerations()) {
			if(!this.getEnumerations().contains(name)) {
				diff += indent(level)+"\ttype added("+name+")\n";
			}
		}
		return diff;
	}*/
	
	/*public Delta compare(WSElement element) {
		XSDIType type = null;
		if(element instanceof XSDSimpleType) {
			type = (XSDSimpleType)element;
		}
		else {
			return null;
		}
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		
		if(!type.getName().equals(this.getName())) {
			ChangeDelta delta = new ChangeDelta(this, type, "name", this.getName(), type.getName());
			deltas.add(delta);
		}
		if (this.restrictionBase != null || ((XSDSimpleType)type).restrictionBase != null) {
			if (!((XSDSimpleType) type).getRestrictionBase().equals(
					this.restrictionBase)) {
				ChangeDelta delta = new ChangeDelta(this, type, "type",
						this.restrictionBase,
						((XSDSimpleType) type).getRestrictionBase());
				deltas.add(delta);
			}
			for (String name : this.enumerations) {
				if (!((XSDSimpleType) type).getEnumerations().contains(name)) {
					ChangeDelta delta = new ChangeDelta(this, type,
							"enumeration", name, "");
					deltas.add(delta);
				}
			}
			for (String name : ((XSDSimpleType) type).getEnumerations()) {
				if (!this.getEnumerations().contains(name)) {
					ChangeDelta delta = new ChangeDelta(this, type,
							"enumeration", "", name);
					deltas.add(delta);
				}
			}
		}
		else if(this.list != null || ((XSDSimpleType)type).list != null) {
			if(this.getList().getItemType().getName().equals(((XSDSimpleType)type).getList().getItemType().getName())) {
				deltas.add(this.getList().getItemType().compare(list.getItemType()));
			}
			else {
				AddDelta addDelta = new AddDelta(null, list.getItemType());
				DeleteDelta deleteDelta = new DeleteDelta(this.getList().getItemType(), null);
				deltas.add(addDelta);
				deltas.add(deleteDelta);
			}
			if(DeltaUtil.containsOnlyMatchDeltas(deltas)) {
				elementDelta = new MatchDelta(this, element);
			}
			else {
				elementDelta = new ChangeDelta(this, element, "", null, null);
			}
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
	
	private String indent(int level) {
		String indent = "";
		for(int i=0; i<level; i++) {
			indent += "\t";
		}
		return indent;
	}



	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof XSDSimpleType) {
			return name.equals(((XSDSimpleType)o).name);
		}
		else {
			return false;
		}
	}



	@Override
	public boolean equalsAfterRename(Object o) {
		if(o instanceof XSDSimpleType) {
			return (restrictionBase.equals(((XSDSimpleType)o).restrictionBase) && enumerations.equals(((XSDSimpleType)o).enumerations)) || list.equals(((XSDSimpleType)o).list);
		}
		else {
			return false;
		}
	}



	@Override
	public HashSet<XSDElement> diff(XSDIType xSDIType) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public boolean mapElement(WADLElement element) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public String getIdentifier() {
		// TODO Auto-generated method stub
		return null;
	}

}
