package wsdarwin.wadlgenerator.model.xsd;

import java.util.HashSet;

import wsdarwin.wadlgenerator.model.WADLElement;

public enum XSDPrimitiveType implements XSDIType{
	
	INT ("int"),
	STRING ("string"),
	DOUBLE ("double"),
	BOOLEAN ("boolean"),
	LONG ("long");
	
	private String type;
	
	XSDPrimitiveType(String type) {
		this.type = type;
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

	/*@Override
	public Delta compare(WSElement element) {
		XSDPrimitiveType type = null;
		if(element instanceof XSDPrimitiveType) {
			type = (XSDPrimitiveType)element;
		}
		else {
			return null;
		}
		if(!type.getName().equals(this.getName())) {
			return new ChangeDelta(this, type, "type", this.getName(), type.getName());
		}
		return new MatchDelta(this, type);
	}*/

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return type;
	}

	@Override
	public boolean mapElement(WADLElement element) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public HashSet<XSDElement> diff(XSDIType xSDIType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIdentifier() {
		// TODO Auto-generated method stub
		return null;
	}

}
