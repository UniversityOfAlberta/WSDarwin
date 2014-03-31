package wsdarwin.model;

import java.util.HashSet;

import wsdarwin.comparison.delta.Delta;

public interface IType extends WSElement {

	
	public void addElement(String name, IType type);
	public String getName();
	public String getVariableName();
	//public String diff(XSDIType input, int level);
	public Delta diff(WSElement input);
	public int getNumberOfTypes();
	public int getNesting();

}
