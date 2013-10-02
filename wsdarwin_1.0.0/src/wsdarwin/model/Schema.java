package wsdarwin.model;

import java.util.HashMap;

import wsdarwin.comparison.delta.Delta;

public class Schema implements WSElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1032753507622949532L;
	
	private String location;
	private HashMap<String, ComplexType> types;

	@Override
	public boolean equalsAfterRename(Object o) {
		if(o instanceof Schema) {
			return types.equals(((Schema)o).types);
		}
		else {
			return false;
		}
	}

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof Schema) {
			return location.equals(((Schema)o).location);
		}
		else {
			return false;
		}
	}

	@Override
	public Delta diff(WSElement input) {
		// TODO Auto-generated method stub
		return null;
	}

}
