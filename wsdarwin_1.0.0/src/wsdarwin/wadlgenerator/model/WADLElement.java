package wsdarwin.wadlgenerator.model;

import wsdarwin.comparison.delta.Delta;

public interface WADLElement {
	public String getIdentifier();
	public boolean mapElement(WADLElement element);
	//public Delta compare(WADLElement element);
	public boolean equalsByName(Object o);
	public boolean equalsAfterRename(Object o);
}
