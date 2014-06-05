package wsdarwin.wadlgenerator.model;

import java.io.Serializable;

import wsdarwin.comparison.delta.Delta;

public interface WADLElement extends Serializable{
	public String getIdentifier();
	public boolean mapElement(WADLElement element);
	//public Delta compare(WADLElement element);
	public boolean equalsByName(Object o);
	public boolean equalsAfterRename(Object o);
}
