package wsdarwin.wadlgenerator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import wsdarwin.comparison.delta.*;
import wsdarwin.model.ComplexType;
import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;
import wsdarwin.wadlgenerator.model.xsd.XSDComplexType;
import wsdarwin.wadlgenerator.model.xsd.XSDElement;
import wsdarwin.wadlgenerator.model.xsd.XSDFile;


public class Representation implements WADLElement {

	private String mediaType;
	private XSDFile responseXSDFile;					// TODO will be changed to an XSD responseXSDFile

	public Representation(String mediaType, XSDFile element) {
		setMediaType(mediaType);
		//setIdentifier(responseXSDFile.getResponseElement());
		this.responseXSDFile = element;
	}
	
	

	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public XSDElement getElement() {
		return responseXSDFile.getResponseElement();
	}
	
	public XSDFile getResponseXSDFile() {
		return this.responseXSDFile;
	}

	public void setResponseXSDFile(XSDFile xsdFile) {
		this.responseXSDFile = xsdFile;
	}

	public String getIdentifier() {
		return responseXSDFile.getResponseElement().getName();
	}
	/**
	 * Main identifier is the responseXSDFile-attribute. For comparison both mediaType and responseXSDFile get compared.
	 * @param responseXSDFile
	 *//*
	public void setIdentifier(XSDFile xsdFile) {
		this.element = responseXSDFile;
	}*/

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((responseXSDFile == null) ? 0 : responseXSDFile.hashCode());
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
		if (responseXSDFile == null) {
			if (other.responseXSDFile != null)
				return false;
		} else if (!responseXSDFile.equals(other.responseXSDFile))
			return false;
		if (mediaType == null) {
			if (other.mediaType != null)
				return false;
		} else if (!mediaType.equals(other.mediaType))
			return false;
		return true;
	}

	public String toString() {
		return responseXSDFile.getResponseElement().getName();
		//		return "<representation> MEDIATYPE="+mediaType+" , ELEMENT="+responseXSDFile;
	}

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof Representation) {
			return (mediaType.equals(((Representation)o).mediaType) 
					&& responseXSDFile.equals(((Representation)o).responseXSDFile));
		}
		else {
			return false;
		}
	}

	@Override
	public boolean equalsAfterRename(Object o) {
		if(o instanceof Representation) {
			return responseXSDFile.equalsAfterRename(((Representation)o).responseXSDFile);
		}
		else {
			return false;
		}
	}

	/*@Override
	public Delta compare(WSElement responseXSDFile) {
		Representation representation = null;
		if(responseXSDFile instanceof Representation) {
			representation = (Representation)responseXSDFile;
		} else {
			return null;
		}

		ArrayList<Delta> deltas = new ArrayList<Delta>();

		Delta elementDelta = null;

		// compare ELEMENT
		if(!representation.getIdentifier().equals(this.getIdentifier())) {
			elementDelta = new ChangeDelta(this, representation, "responseXSDFile", this.getIdentifier(), representation.getIdentifier());
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
		// connection to the XSD files needed (for responseXSDFile-attribute)



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
			String complexTypeName = this.responseXSDFile.getResponseElement().getName();
			if(complexTypeName.equals(representation.getElement().getName())){
				mapped.put(complexTypeName, this.responseXSDFile.getResponseElement());
			}else {
				added.put(complexTypeName, representation.getElement());
			}

			if(!mapped.containsKey(complexTypeName)){
				deleted.put(complexTypeName,this.getElement());
			}	
		}
	}



	public void compareToMerge(Representation representation) {
		this.responseXSDFile.compareToMerge(representation.responseXSDFile);
	}
}
