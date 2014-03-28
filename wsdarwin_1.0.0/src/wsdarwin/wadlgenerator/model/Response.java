package wsdarwin.wadlgenerator.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import wsdarwin.comparison.delta.*;
import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class Response implements WADLElement {

	private int status;
	private HashMap<String, Representation> representationElements;
	private HashSet<Representation> changedRepresentation;

	public Response(int status) {
		super();
		this.status = status;
		this.representationElements = new HashMap<String, Representation>();
		this.changedRepresentation = new HashSet<Representation>();
	}

	public String getIdentifier() {
		return Integer.toString(status);
	}
	
	public int getID() {
		return status;
	}

	public void setID(int status) {
		this.status = status;
	}

	public HashMap<String, Representation> getRepresentationElements() {
		return representationElements;
	}

	public void addRepresentationElement(String element, Representation e) {
		representationElements.put(element, e);
	}
	
	public void addAllRepresentationElement(HashMap<String, Representation> elements) {
		representationElements.putAll(elements);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + status;
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
		Response other = (Response) obj;
		if (status != other.status)
			return false;
		return true;
	}

	public String toString() {
		return Integer.toString(status);
//		return "<response> STATUS="+status+", #representationElements="+representationElements.size();
	}
	
	/*	old diff-method(HashSet<Response> responses) {
//	There are three cases:
//	1) status isn't equal							> add a new Response item
//	2) status is equal, representation not			> add a new Response item
//	3) status is equal, representation as well 		> do nothing
	return methodAdded; */	
	
	
	public HashSet<Representation> compareToMerge(Response response) {
		HashSet<Representation> representationAdded = new HashSet<Representation>();

		for(String element : response.getRepresentationElements().keySet()) {
			if(!this.getRepresentationElements().containsKey(element)) {
				representationAdded.add(response.getRepresentationElements().get(element));
				// + set frequency to 1
			} else {
					// + increase frequency
				changedRepresentation.add(this.getRepresentationElements().get(element));
			}
		}
		return representationAdded;
	}
	
	public void mergeRepresentations(HashSet<Representation> addedRepresentationElements) {
		for(Representation r : addedRepresentationElements) {
			this.representationElements.put(r.getMediaType(), r);
		}
	}

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof Response) {
			return status == (((Response)o).status);
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
		Response response = null;
		if(element instanceof Response) {
			response = (Response)element;
		} else {
			return null;
		}
	
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();

		// check for rename based on identifier (status)
		if(!response.getIdentifier().equals(this.getIdentifier())) {
			elementDelta = new ChangeDelta(this, response, "status", this.getIdentifier(), response.getIdentifier());
		}

		HashMap<String, Representation> representationsAdded = new HashMap<String, Representation>();
		HashMap<String, Representation> representationsDeleted = new HashMap<String, Representation>();
		HashMap<String, Representation> representationsIncluded = new HashMap<String , Representation>();
		
		// fill representationIncluded
		for(String name : this.representationElements.keySet()) {
			representationsIncluded.put(name, this.getRepresentationElements().get(name));
		}
		
		// fill representationDeleted (& for match also remove from paramIncluded)  		(iterate over File1)
		for(String mediaType : this.representationElements.keySet()) {
			if(!response.getRepresentationElements().containsKey(mediaType)) {
				representationsDeleted.put(mediaType, this.representationElements.get(mediaType));
				representationsIncluded.remove(mediaType);
			}
		}
		
		// fill representationAdded			(iterate over File2)
		for(String name : response.representationElements.keySet()) {
			if(!this.getRepresentationElements().containsKey(name)) {
				representationsAdded.put(name, representationElements.get(name));
			}
		}
		
		// iterate over included + call compare() method
		for(String mediaType : representationsIncluded.keySet()) {
			deltas.add(this.representationElements.get(mediaType).compare(response.getRepresentationElements().get(mediaType)));
		}

		if (elementDelta == null) {
			// create elementDeltas
			if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
				elementDelta = new MatchDelta(this, response);
			} else {
				elementDelta = new ChangeDelta(this, response, "", null, null);
			}
		}
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}*/

	@Override
	public boolean mapElement(WADLElement element) {
		System.out.println("MAP ELEMENT GRAMMARS");
		HashMap<String, Representation> mapped = new HashMap<String, Representation>();
		HashMap<String, Representation> added = new HashMap<String, Representation>();
		HashMap<String, Representation> deleted = new HashMap<String, Representation>();
		mapByID(element, mapped, added, deleted);
		mapByStructure(element, mapped, added, deleted);
		return false;
	}

	private void mapByStructure(WADLElement element,
			HashMap<String, Representation> mapped, HashMap<String, Representation> added,
			HashMap<String, Representation> deleted) {
		
		HashMap<String, Representation> notAdded = new HashMap<String, Representation>();
		HashMap<String, Representation> notDeleted = new HashMap<String, Representation>();
		
		for(String elementName : added.keySet()) {
			for(String elementName2 : deleted.keySet()) {
				if(added.get(elementName).mapElement(deleted.get(elementName2))) {
					notAdded.put(elementName, added.get(elementName));
					notDeleted.put(elementName2, deleted.get(elementName2));
				}
			}
		}
		
		for(String elementName : notAdded.keySet()) {
			added.remove(elementName);
			mapped.put(elementName, notAdded.get(elementName));
		}
		
		for(String elementName : notDeleted.keySet()) {
			deleted.remove(elementName);
			mapped.put(elementName, notDeleted.get(elementName));
		}
		
	}

	//We haven't take into consideration MediaType at all here
	private void mapByID(WADLElement element, HashMap<String, Representation> mapped, HashMap<String, Representation> added, HashMap<String, Representation> deleted) {
		if(element instanceof Response){
			Response response = (Response)element;
			for(String elementName : response.representationElements.keySet()) {
				if(this.getRepresentationElements().containsKey(elementName)){
					mapped.put(elementName ,this.representationElements.get(elementName));
				}else{
					added.put(elementName ,this.representationElements.get(elementName ));
				}
			}

			for(String elementName : this.representationElements.keySet()) {
				if(!mapped.containsKey(elementName)){
					deleted.put(elementName,this.representationElements.get(elementName));
				}
			}
		}
		
	}
}
