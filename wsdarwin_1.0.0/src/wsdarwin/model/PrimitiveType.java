package wsdarwin.model;

import java.util.HashMap;

import wsdarwin.comparison.delta.ChangeDelta;
import wsdarwin.comparison.delta.Delta;
import wsdarwin.comparison.delta.MatchDelta;

public enum PrimitiveType implements IType {
	
	STRING,
	INT,
	DOUBLE,
	FLOAT,
	LONG,
	DATE,
	BOOLEAN,
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
	public String getName() {
		return this.name();
	}
	@Override
	public String getVariableName() {
		return variableName;
	}
	
	
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	@Override
	public Delta diff(WSElement input) {
		PrimitiveType type = null;
		if(input instanceof PrimitiveType) {
			type = (PrimitiveType)input;
		}
		else {
			return null;
		}
		if(!type.getName().equals(this.getName())) {
			return new ChangeDelta(this, type, "name", this.getName(), type.getName());
		}
		return new MatchDelta(this, type);
	}
	@Override
	public HashMap<String, WSElement> getChildren() {
		return new HashMap<String, WSElement>();
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return variableName+":"+this.name();
	}

	
}
