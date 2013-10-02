package wsdarwin.model;

import java.util.HashMap;

import wsdarwin.comparison.delta.Delta;

public interface IType extends WSElement {

	
	public void addElement(String name, IType type);
	public HashMap<String, IType> getElements();
	public String getName();
	public String getVariableName();
	//public String diff(IType input, int level);
	public Delta diff(WSElement input);

}
