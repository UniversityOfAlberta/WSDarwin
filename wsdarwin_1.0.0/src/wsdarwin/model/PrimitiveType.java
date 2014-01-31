package wsdarwin.model;

import java.util.HashMap;

import wsdarwin.comparison.delta.ChangeDelta;
import wsdarwin.comparison.delta.Delta;
import wsdarwin.comparison.delta.MatchDelta;

public class PrimitiveType implements IType {
	
	/*STRING,
	INT,
	DOUBLE,
	FLOAT,
	LONG,
	DATE,
	BOOLEAN,
	URL;*/
	
	private String name;
	private String variableName;
	private Object value;
	
	
	
	public PrimitiveType(String name, String variableName) {
		super();
		if (name.equalsIgnoreCase("DATETIME")) {
			this.name = "Calendar";
		}
		else if(name.equalsIgnoreCase("integer")) {
			this.name = "BigInteger";
		}
		else {
			this.name = name;
		}
		this.variableName = variableName;
	}
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
		return this.name;
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
			return new ChangeDelta(this, type, "type", this.getName(), type.getName());
		}
		if(!((PrimitiveType)type).getVariableName().equals(this.variableName)) {
			return new ChangeDelta(this, type, "variableName", this.getVariableName(), ((PrimitiveType)type).getVariableName());
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
		return variableName+":"+this.name;
	}
	
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof PrimitiveType) {
			PrimitiveType type = (PrimitiveType)o;
			return type.getName().equals(this.getName()) && type.getVariableName().equals(this.getVariableName());
			
		}
		else {
			return false;
		}
	}
	@Override
	public int getNumberOfTypes() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getNesting() {
		// TODO Auto-generated method stub
		return 0;
	}
}
