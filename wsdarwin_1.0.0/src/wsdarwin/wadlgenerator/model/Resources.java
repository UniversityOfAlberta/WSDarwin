package wsdarwin.wadlgenerator.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import wsdarwin.comparison.delta.*;
import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class Resources implements WADLElement {

	private String base;							// of type xsd:anyURI
	private HashMap<String, Resource> resourceElements;
	private HashMap<Resource, HashSet<WADLElement>> changedResource;
	
	public Resources(String base) {
		this.setIdentifier(base);
		this.resourceElements = new HashMap<String, Resource>();
		this.changedResource = new HashMap<Resource, HashSet<WADLElement>>();
	}	
	
	public String getIdentifier() {
		return base;
	}

	public void setIdentifier(String base) {
		this.base = base;
	}

	public HashMap<String, Resource> getResourceElements() {
		return resourceElements;
	}

	public void addResourceElement(String path, Resource resource) {
		resourceElements.put(path, resource);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base == null) ? 0 : base.hashCode());
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
		Resources other = (Resources) obj;
		if (base == null) {
			if (other.base != null)
				return false;
		} else if (!base.equals(other.base))
			return false;
		return true;
	}
	
	public String toString() {
		return base;
//		return "<resources> BASE="+base+", #resourceElements="+resourceElements.size();
	}
	
	public HashSet<Resource> compareToMerge(Resources resources) {
		HashSet<Resource> resourceAdded = new HashSet<Resource>();
		
		for(String path : resources.getResourceElements().keySet()) {
			if(!this.getResourceElements().containsKey(path)) {
				resourceAdded.add(resources.getResourceElements().get(path));
			} else { // already exists > compare
				changedResource.put(this.getResourceElements().get(path), this.getResourceElements().get(path).compareToMerge(resources.getResourceElements().get(path)));
			}
		}

		return resourceAdded;
	}

	public void mergeResources(HashSet<Resource> addedResourceElements) {
		for(Resource r : addedResourceElements) {
			this.resourceElements.put(r.getIdentifier(), r);
		}
		for(Resource r : changedResource.keySet()) {
			r.mergeResource(changedResource.get(r));
		}
	}

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof Resources) {
			return base.equals(((Resources)o).base);
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
		Resources resources = null;
		if(element instanceof Resources) {
			resources = (Resources)element;
		} else {
			return null;
		}
		
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		
		// check for rename based on identifier (base)
		if(!resources.getIdentifier().equals(this.getIdentifier())) {
			elementDelta = new ChangeDelta(this, resources, "base", this.getIdentifier(), resources.getIdentifier());
		}
		
		HashMap<String, Resource> resourceElementsAdded = new HashMap<String, Resource>();
		HashMap<String, Resource> resourceElementsDeleted = new HashMap<String, Resource>();
		HashMap<String, Resource> resourceElementsIncluded = new HashMap<String , Resource>();

		for(String name : this.resourceElements.keySet()) {
			resourceElementsIncluded.put(name, this.getResourceElements().get(name));
		}
		for(String name : this.resourceElements.keySet()) {
			if(!resources.getResourceElements().containsKey(name)) {
				resourceElementsDeleted.put(name, this.resourceElements.get(name));
				resourceElementsIncluded.remove(name);
			}
		}
		for(String name : resources.resourceElements.keySet()) {
			if(!this.getResourceElements().containsKey(name)) {
				resourceElementsAdded.put(name, resourceElements.get(name));
			}
		}
		
		// mark additions and deletions
		ArrayList<String> resourcesNotAdded = new ArrayList<String>();
		ArrayList<String> resourcesNotDeleted = new ArrayList<String>();
		for(String nameAdded : resourceElementsAdded.keySet()) {
			for(String nameDeleted : resourceElementsDeleted.keySet()) {
				
				if(resourceElementsAdded.get(nameAdded) instanceof Resource
						&& resourceElementsDeleted.get(nameDeleted) instanceof Resource) {
					// check for rename (based on children)
					if(resourceElementsAdded.get(nameAdded).getMethodElements()
							.equals(resourceElementsDeleted.get(nameDeleted).getMethodElements())) {	
						deltas.add(resourceElementsAdded.get(nameAdded).compare(resourceElementsDeleted.get(nameDeleted)));
						resourcesNotAdded.add(nameAdded);
						resourcesNotDeleted.add(nameDeleted);
					}
				}
			}
		}

		// >> delete all of the previously marked elements?
		for(String notAdded : resourcesNotAdded) {
			resourceElementsAdded.remove(notAdded);
		}
		for(String notDeleted : resourcesNotDeleted) {
			resourceElementsDeleted.remove(notDeleted);
		}
		
		// create Deltas for additions & deletions
		for(String name : resourceElementsAdded.keySet()) {
			AddDelta delta = new AddDelta(null, resourceElementsAdded.get(name));
			WADLFile.addChildrenElements(resourceElementsAdded.get(name), delta);
			deltas.add(delta);
		}
		for(String name : resourceElementsDeleted.keySet()) {
			DeleteDelta delta = new DeleteDelta(resourceElementsDeleted.get(name), null);
			WADLFile.deleteChildrenElements(resourceElementsDeleted.get(name), delta);
			deltas.add(delta);
		}
		
		// recursive call of compare() method?
		for(String name : resourceElementsIncluded.keySet()) {
			deltas.add(this.resourceElements.get(name).compare(resources.getResourceElements().get(name)));
		}

		if (elementDelta == null) {
			// create ElementDeltas
			if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
				elementDelta = new MatchDelta(this, resources);
			} else {
				elementDelta = new ChangeDelta(this, resources, "", null, null);
			}
		}
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}*/

	@Override
	public boolean mapElement(WADLElement element) {
		System.out.println("MAP ELEMENT RESOURCES");
		HashMap<String, Resource> mapped = new HashMap<String, Resource>();
		HashMap<String, Resource> added = new HashMap<String, Resource>();
		HashMap<String, Resource> deleted = new HashMap<String, Resource>();
		mapByID(element, mapped, added, deleted);
		//if(added.isEmpty() && deleted.isEmpty())
			mapByStructure(element, mapped, added, deleted);
		//else if(added.isEmpty() && deleted.isEmpty())
			mapByValue(element, mapped, added, deleted);
		return false;
	}

	private void mapByValue(WADLElement element, HashMap<String, Resource> mapped, HashMap<String, Resource> added, HashMap<String, Resource> deleted) {
		
	}
	
	//a Resources Element may have 0-* resources
	private void mapByStructure(WADLElement element, HashMap<String, Resource> mapped, HashMap<String, Resource> added, HashMap<String, Resource> deleted) {
		HashMap<String, Resource> notAdded = new HashMap<String, Resource>();
		HashMap<String, Resource> notDeleted = new HashMap<String, Resource>();
		
		for(String path : added.keySet()){
			for(String path2 : deleted.keySet()){
				if(added.get(path).mapElement(deleted.get(path2))) {
					notAdded.put(path, added.get(path));
					notDeleted.put(path2, deleted.get(path2));
				}
			}
		}
		
		for(String path : notAdded.keySet()) {
			added.remove(path);
			mapped.put(path, notAdded.get(path));
		}
		
		for(String path : notDeleted.keySet()) {
			deleted.remove(path);
			mapped.put(path, notDeleted.get(path));
		}
		
	}

	private void mapByID(WADLElement element, HashMap<String, Resource> mapped, HashMap<String, Resource> added, HashMap<String, Resource> deleted) {
		if(element instanceof Resources){
			Resources resources = (Resources)element;
			for(String path : resources.resourceElements.keySet()){
				if(this.resourceElements.containsKey(path)){
					mapped.put(path,this.resourceElements.get(path));
				}else {
					added.put(path,resources.resourceElements.get(path));
				}
			}
			
			for(String path : this.resourceElements.keySet()){
				if(!mapped.containsKey(path)){
					deleted.put(path,this.resourceElements.get(path));
				}
			}
			
		}
		
	}
}
