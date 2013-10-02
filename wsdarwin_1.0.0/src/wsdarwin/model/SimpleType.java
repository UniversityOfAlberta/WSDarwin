package wsdarwin.model;

import java.util.HashMap;
import java.util.HashSet;

import wsdarwin.comparison.delta.Delta;

public class SimpleType implements IType {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4131625151539868067L;
	
	private String name;
	private String elementName;
	private IType base;
	private HashSet<String> options;
	
	@Override
	public boolean equalsAfterRename(Object o) {
		if(o instanceof SimpleType) {
			return base.equals(((SimpleType)o).base) && options.equals(((SimpleType)o).options);
		}
		else {
			return false;
		}
	}
	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof SimpleType) {
			return name.equals(((SimpleType)o).name);
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
		return name;
	}
	@Override
	public String getVariableName() {
		return elementName;
	}
	@Override
	public Delta diff(WSElement input) {
		// TODO Auto-generated method stub
		return null;
	}

}
