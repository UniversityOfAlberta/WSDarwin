package wsdarwin.wadlgenerator.model;

import java.util.ArrayList;
import java.util.HashMap;

import wsdarwin.comparison.delta.*;
import wsdarwin.model.ComplexType;
import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;
import wsdarwin.wadlgenerator.model.xsd.XSDElement;


public class Representation implements WADLElement {

	private String mediaType;
	private XSDElement element;					// TODO will be changed to an XSD element

	public Representation(String mediaType, XSDElement element) {
		setMediaType(mediaType);
		setIdentifier(element);
		this.element = element;
	}
	
	

	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public XSDElement getElement() {
		return element;
	}

	public void setElement(XSDElement element) {
		this.element = element;
	}

	public String getIdentifier() {
		return element.getName();
	}
	/**
	 * Main identifier is the element-attribute. For comparison both mediaType and element get compared.
	 * @param element
	 */
	public void setIdentifier(XSDElement element) {
		this.element = element;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((element == null) ? 0 : element.hashCode());
		result = prime * result
				+ ((mediaType == null) ? 0 : mediaType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Representation other = (Representation) obj;
		if (element == null) {
			if (other.element != null)
				return false;
		} else if (!element.equals(other.element))
			return false;
		if (mediaType == null) {
			if (other.mediaType != null)
				return false;
		} else if (!mediaType.equals(other.mediaType))
			return false;
		return true;
	}

	public String toString() {
		return element.getName();
		//		return "<representation> MEDIATYPE="+mediaType+" , ELEMENT="+element;
	}

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof Representation) {
			return (mediaType.equals(((Representation)o).mediaType) 
					&& element.equals(((Representation)o).element));
		}
		else {
			return false;
		}
	}

	@Override
	public boolean equalsAfterRename(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	/*@Override
	public Delta compare(WSElement element) {
		Representation representation = null;
		if(element instanceof Representation) {
			representation = (Representation)element;
		} else {
			return null;
		}

		ArrayList<Delta> deltas = new ArrayList<Delta>();

		Delta elementDelta = null;

		// compare ELEMENT
		if(!representation.getIdentifier().equals(this.getIdentifier())) {
			elementDelta = new ChangeDelta(this, representation, "element", this.getIdentifier(), representation.getIdentifier());
		}
		// compare mediaType
		if(!representation.getMediaType().equals(this.getMediaType())) {
			if (elementDelta != null) {
				((ChangeDelta)elementDelta).addChangedAttribute("mediaType", this.getMediaType(), representation.getMediaType());
			} else {
				elementDelta = new ChangeDelta(this, representation, "mediaType", this.getMediaType(), representation.getMediaType());
			}
		}
		if(representation.getIdentifier().equals(this.getIdentifier())) {
			deltas.add(this.getElement().compare(representation.getElement()));
		}



		// TODO
		// @Marios will implement the compare method
		// connection to the XSD files needed (for element-attribute)



		if (elementDelta == null) {
			if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
				elementDelta = new MatchDelta(this, representation);
			} else {
				elementDelta = new ChangeDelta(this, representation, "", null,
						null);
			}
		}
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}*/

	@Override
	public boolean mapElement(WADLElement element) {
		HashMap<String, XSDElement> mapped = new HashMap<String, XSDElement>();
		HashMap<String, XSDElement> added = new HashMap<String, XSDElement>();
		HashMap<String, XSDElement> deleted = new HashMap<String, XSDElement>();
		mapByID(element, mapped, added, deleted);
		mapByStructure(element, mapped, added, deleted);
		return false;
	}

	private void mapByStructure(WADLElement element, HashMap<String, XSDElement> mapped, HashMap<String, XSDElement> added, HashMap<String, XSDElement> deleted) {
		HashMap<String, XSDElement> notAdded = new HashMap<String, XSDElement>();
		HashMap<String, XSDElement> notDeleted = new HashMap<String, XSDElement>();
		
		for(String complexTypeName : added.keySet()){
			for(String complexTypeName2 : deleted.keySet()){
				/*if(added.get(complexTypeName).mapElement(deleted.get(complexTypeName2))) {
					notAdded.put(complexTypeName, added.get(complexTypeName));
					notDeleted.put(complexTypeName2, deleted.get(complexTypeName2));
				}*/
			}
		}
		
		for(String complexTypeName : notAdded.keySet()) {
			added.remove(complexTypeName);
			mapped.put(complexTypeName, notAdded.get(complexTypeName));
		}
		
		for(String complexTypeName : notDeleted.keySet()) {
			deleted.remove(complexTypeName);
			mapped.put(complexTypeName, notDeleted.get(complexTypeName));
		}

	}

	private void mapByID(WADLElement element, HashMap<String, XSDElement> mapped, HashMap<String, XSDElement> added, HashMap<String, XSDElement> deleted) {
		if(element instanceof Representation){
			Representation representation = (Representation)element;
			String complexTypeName = this.element.getName();
			if(complexTypeName.equals(representation.getElement().getName())){
				mapped.put(complexTypeName, this.element);
			}else {
				added.put(complexTypeName, representation.getElement());
			}

			if(!mapped.containsKey(complexTypeName)){
				deleted.put(complexTypeName,this.getElement());
			}	
		}
	}
}
