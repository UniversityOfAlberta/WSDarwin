package wsdarwin.model;

import java.util.HashMap;

import wsdarwin.comparison.delta.Delta;

public enum PrimitiveType implements IType {
	
	STRING,
	INT,
	DOUBLE,
	FLOAT,
	LONG,
	DATE,
	URL;
	
	private String variableName;
	private Object value;
	
	@Override
	public boolean equalsAfterRename(Object o) {
		if(o instanceof PrimitiveType && value != null && ((PrimitiveType)o).value != null) {
			return value.equals(((PrimitiveType)o).value);
		}
		else {
			return false;
		}
	}
	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof PrimitiveType) {
			return variableName.equals(((PrimitiveType)o).variableName);
		}
		else {
			return false;
		}
	}
	@Override
	public void addElement(String name, IType type) {
		
	}
	@Override
	public HashMap<String, IType> getElements() {
		return new HashMap<String, IType>();
	}
	@Override
	public String getName() {
		return this.name();
	}
	@Override
	public String getVariableName() {
		return variableName;
	}
	@Override
	public Delta diff(WSElement input) {
		// TODO Auto-generated method stub
		return null;
	}

}
