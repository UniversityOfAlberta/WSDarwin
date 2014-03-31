package wsdarwin.wadlgenerator.model.xsd;

import java.util.HashSet;

import wsdarwin.wadlgenerator.model.WADLElement;

public interface XSDIType extends WADLElement {
	
	public String getName();

	public HashSet<XSDElement> diff(XSDIType xSDIType);

}
