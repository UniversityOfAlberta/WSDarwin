package wsdarwin.wadlgenerator.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import wsdarwin.comparison.delta.*;
import wsdarwin.model.ComplexType;
import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class Resource implements WADLElement {

//  TODO Implement more of the specification @ http://www.w3.org/Submission/wadl/#x3-120002.6 (see id, type, queryType)
//	private String id;
//	private String type;
//	private String queryType;
	
	public static final String VARIABLE_ID = "{id}";
	private String path;
	private boolean variableID;
	private HashMap<String, Method> methodElements;
	private HashMap<String, Resource> resourceElements;
	private HashMap<String, Param> paramElements;
	private HashMap<Method, Request> changedMethodRequest;
	private HashMap<Method, HashSet<Response>> changedMethodResponse;
	private HashMap<Resource, HashSet<WADLElement>> changedResources;

	public Resource(String path) {
		this.path = path;
		this.variableID = false;
		this.methodElements = new HashMap<String, Method>();
		this.resourceElements = new HashMap<String, Resource>();
		this.paramElements = new HashMap<String, Param>();
		this.changedMethodRequest = new HashMap<Method, Request>();
		this.changedMethodResponse = new HashMap<Method, HashSet<Response>>();
		this.changedResources = new HashMap<Resource, HashSet<WADLElement>>();
		if(path.startsWith("{")) {
			String paramID = path.replace("{", "");
			paramID = paramID.replace("}", "");
			Param param = new Param(paramID, "template");
			paramElements.put(paramID, param);
		}
	}
	
	public String getIdentifier() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean hasVariableID() {
		return variableID;
	}

	public void setVariableID(boolean variableID) {
		if (methodElements.size()==1 && methodElements.containsKey(path)) {
			Method method = methodElements.remove(path);
			method.setVariableID(Resource.VARIABLE_ID);
			methodElements.put(method.getIdentifier(), method);
		}
		path = Resource.VARIABLE_ID;
		this.variableID = variableID;
	}

	public HashMap<String, Method> getMethodElements() {
		return methodElements;
	}

	public void addMethodElement(String id, Method element) {
		methodElements.put(id, element);
	}
	
	public HashMap<String, Resource> getResourceElements() {
		return resourceElements;
	}

	public void addResourceElement(String id, Resource element) {
		resourceElements.put(id, element);
	}
	
	public HashMap<String, Param> getParamElements() {
		return paramElements;
	}
	
	public void addParamElement(String id, Param element) {
		paramElements.put(id, element);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
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
		Resource other = (Resource) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	public String toString() {
		return path;
//		return "<resource> PATH="+path+", #methodElements="+methodElements.size();
	}
	
	public HashSet<WADLElement> compareToMerge(Resource resource) {
		HashSet<WADLElement> elementsAdded = new HashSet<WADLElement>();
		
		for(String id : resource.getParamElements().keySet()) {
			if(!this.getParamElements().containsKey(id)) {
				elementsAdded.add(resource.getMethodElements().get(id));
			}
		}

		for(String id : resource.getMethodElements().keySet()) {
			if(!this.getMethodElements().containsKey(id)) {
				elementsAdded.add(resource.getMethodElements().get(id));
			} else {
				changedMethodRequest.put(this.getMethodElements().get(id), this.getMethodElements().get(id).compareToMergeRequest(resource.getMethodElements().get(id)));
				changedMethodResponse.put(this.getMethodElements().get(id), this.getMethodElements().get(id).compareToMergeResponse(resource.getMethodElements().get(id)));
			}
		}
		for(String id : resource.getResourceElements().keySet()) {
			if(!this.getResourceElements().containsKey(id)) {
				elementsAdded.add(resource.getResourceElements().get(id));
			} else {
				changedResources.put(this.getResourceElements().get(id), this.getResourceElements().get(id).compareToMerge(resource.getResourceElements().get(id)));
			}
		}
		return elementsAdded;
	}

	public void mergeResource(HashSet<WADLElement> addedElements) {
		for(WADLElement e : addedElements) {
			if (e instanceof Method) {
				Method method = (Method)e;
				this.methodElements.put(method.getIdentifier(), method);
			}
			else if(e instanceof Resource) {
				Resource resource = (Resource)e;
				this.resourceElements.put(resource.getIdentifier(), resource);
			}
			else if(e instanceof Param) {
				Param param = (Param)e;
				this.paramElements.put(param.getIdentifier(), param);
			}
		}	
		// Request
		for(Method m : changedMethodRequest.keySet()) {
			m.mergeMethodRequests(changedMethodRequest.get(m));
		}
		// Response
		for(Method m : changedMethodResponse.keySet()) {
			m.mergeMethodResponses(changedMethodResponse.get(m));
		}
		
		for(Resource r : changedResources.keySet()) {
			r.mergeResource(changedResources.get(r));
		}
	}

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof Resource) {
			return path.equals(((Resource)o).path);
		}
		else {
			return false;
		}
	}

	@Override
	public boolean equalsAfterRename(Object o) {
		if (o instanceof Resource) {
			return resourceElements.equals(((Resource) o).resourceElements) && (methodElements.equals(((Resource)o ).methodElements) || methodElementsEqualAfterRename(((Resource)o).methodElements));
		} else {
			return false;
		}
	}
	
	public boolean methodElementsEqualAfterRename(HashMap<String, Method> methodElements) {
		for(Method thisMethod : this.methodElements.values()) {
			for(Method method : methodElements.values()) {
				if(!thisMethod.equals(method) && !thisMethod.equalsAfterRename(method)) {
					return false;
				}
			}
		}
		return true;
	}

	/*@Override
	public Delta compare(WSElement element) {
		Resource resource = null;
		if(element instanceof Resource) {
			resource = (Resource)element;
		} else {
			return null;
		}
		
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		
		// check for rename based on identifier (path)
		if(!resource.getIdentifier().equals(this.getIdentifier())) {
			elementDelta = new ChangeDelta(this, resource, "path", this.getIdentifier(), resource.getIdentifier());
		}
		
		HashMap<String, Method> methodsAdded = new HashMap<String, Method>();
		HashMap<String, Method> methodsDeleted = new HashMap<String, Method>();
		HashMap<String, Method> methodsIncluded = new HashMap<String , Method>();

		for(String name : this.methodElements.keySet()) {
			methodsIncluded.put(name, this.getMethodElements().get(name));
		}
		for(String name : this.methodElements.keySet()) {
			if(!resource.getMethodElements().containsKey(name)) {
				methodsDeleted.put(name, this.methodElements.get(name));
				methodsIncluded.remove(name);
			}
		}
		for(String name : resource.methodElements.keySet()) {
			if(!this.getMethodElements().containsKey(name)) {
				methodsAdded.put(name, methodElements.get(name));
			}
		}
		
		// mark additions and deletions
		ArrayList<String> resourcesNotAdded = new ArrayList<String>();
		ArrayList<String> resourcesNotDeleted = new ArrayList<String>();
		// loop over all Param objects (added & deleted)
		for(String nameAdded : methodsAdded.keySet()) {
			for(String nameDeleted : methodsDeleted.keySet()) {
				
				if(methodsAdded.get(nameAdded) instanceof Method
						&& methodsDeleted.get(nameDeleted) instanceof Method) {
					// check for rename (based on children) / comparing requestElement and responseElements
					if(methodsAdded.get(nameAdded).getRequestElement()
							.equals(methodsDeleted.get(nameDeleted).getRequestElement())
							&& methodsAdded.get(nameAdded).getResponseElements()
							.equals(methodsDeleted.get(nameDeleted).getResponseElements())) {
						deltas.add(methodsAdded.get(nameAdded).compare(methodsDeleted.get(nameDeleted)));
						resourcesNotAdded.add(nameAdded);
						resourcesNotDeleted.add(nameDeleted);
					}
				}
			}
		}

		// delete all of the previously marked elements
		for(String notAdded : resourcesNotAdded) {
			methodsAdded.remove(notAdded);
		}
		for(String notDeleted : resourcesNotDeleted) {
			methodsDeleted.remove(notDeleted);
		}
		
		// create Deltas for additions & deletions
		for(String name : methodsAdded.keySet()) {
			AddDelta delta = new AddDelta(null, methodsAdded.get(name));
			WADLFile.addChildrenElements(methodsAdded.get(name), delta);
			deltas.add(delta);
		}
		for(String name : methodsDeleted.keySet()) {
			DeleteDelta delta = new DeleteDelta(methodsDeleted.get(name), null);
			WADLFile.deleteChildrenElements(methodsDeleted.get(name), delta);
			deltas.add(delta);
		}
		
		// recursive call of compare() method?
		for(String name : methodsIncluded.keySet()) {
			deltas.add(this.methodElements.get(name).compare(resource.getMethodElements().get(name)));
		}

		if (elementDelta == null) {
			// create ElementDeltas
			if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
				elementDelta = new MatchDelta(this, resource);
			} else {
				elementDelta = new ChangeDelta(this, resource, "", null, null);
			}
		}
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}*/

	@Override
	public boolean mapElement(WADLElement element) {
		System.out.println("MAP ELEMENT RESOURCE");
		HashMap<String, Method> mapped = new HashMap<String, Method>();
		HashMap<String, Method> added = new HashMap<String, Method>();
		HashMap<String, Method> deleted = new HashMap<String, Method>();
		mapByID(element, mapped, added, deleted);
		//if(added.isEmpty() && deleted.isEmpty())
			mapByStructure(element, mapped, added, deleted);
		//else if(added.isEmpty() && deleted.isEmpty())
			mapByValue(element, mapped, added, deleted);
		return false;
	}

	private void mapByValue(WADLElement element, HashMap<String, Method> mapped,
			HashMap<String, Method> added, HashMap<String, Method> deleted) {
		// TODO Auto-generated method stub
		
	}

	//a Resource Element may have 0-* Method Elements
	private void mapByStructure(WADLElement element, HashMap<String, Method> mapped, HashMap<String, Method> added, HashMap<String, Method> deleted) {
		HashMap<String, Method> notAdded = new HashMap<String, Method>();
		HashMap<String, Method> notDeleted = new HashMap<String, Method>();
		
		for(String methodID : added.keySet()){
			for(String methodID2 : deleted.keySet()){
				if(added.get(methodID).mapElement(deleted.get(methodID2))) {
					notAdded.put(methodID, added.get(methodID));
					notDeleted.put(methodID2, deleted.get(methodID2));
				}
			}
		}
		
		for(String methodID : notAdded.keySet()) {
			added.remove(methodID);
			mapped.put(methodID, notAdded.get(methodID));
		}
		
		for(String methodID : notDeleted.keySet()) {
			deleted.remove(methodID);
			mapped.put(methodID, notDeleted.get(methodID));
		}
		
	}

	private void mapByID(WADLElement element, HashMap<String, Method> mapped, HashMap<String, Method> added, HashMap<String, Method> deleted) {
		if(element instanceof Resource){
			Resource resource = (Resource)element;
			for(String methodID : resource.methodElements.keySet()){
				if(this.methodElements.containsKey(path)){
					mapped.put(methodID,this.methodElements.get(methodID));
				}else {
					added.put(methodID,resource.methodElements.get(methodID));
				}
			}
			
			for(String methodID : this.methodElements.keySet()){
				if(!mapped.containsKey(methodID)){
					deleted.put(methodID, this.methodElements.get(methodID));
				}
			}
			System.out.println("METHOD MAPPING:");
			System.out.println(mapped.toString());
			System.out.println(added.toString());
			System.out.println(deleted.toString());
			
		}
	}
}
