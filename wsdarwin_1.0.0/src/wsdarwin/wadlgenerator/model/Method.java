package wsdarwin.wadlgenerator.model;

import java.util.HashMap;
import java.util.HashSet;

import wsdarwin.comparison.delta.*;
import wsdarwin.model.ComplexType;
import wsdarwin.model.WSElement;
import wsdarwin.wadlgenerator.model.xsd.XSDComplexType;
import wsdarwin.wadlgenerator.model.xsd.XSDElement;
import wsdarwin.wadlgenerator.model.xsd.XSDFile;

public class Method implements WADLElement {

	private String name;						// TODO Write as an enum? (GET, POST, PUT, DELETE)
	private String id;
	private Request requestElement;
	private HashMap<Integer, Response> responseElements;
	private HashMap<Request, HashSet<Param>> changedRequest;
	private HashMap<Response, HashSet<Representation>> changedResponse;
	private double score;

	public Method(String name, String id) {
		this.name = name;
		this.id= id;
		this.requestElement = new Request();
		this.responseElements = new HashMap<Integer, Response>();
		this.changedRequest = new HashMap<Request, HashSet<Param>>();
		this.changedResponse = new HashMap<Response, HashSet<Representation>>();
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIdentifier() {
		return id;
	}

	public void setIdentifier(String id) {
		this.id = id;
	}

	public Request getRequestElement() {
		return requestElement;
	}

	public void addRequestElement(Request e) {
		this.requestElement = e;
	}
	
	public HashMap<Integer, Response> getResponseElements() {
		return responseElements;
	}

	public void addResponseElement(int status, Response responseElement) {
		this.responseElements.put(status, responseElement);
	}
	
	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		Method other = (Method) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public String toString() {
		return id+" ("+name+")";
//		return "<method> name="+name+", ID="+id+", #requestElements=1(hard-coded)"			//requestElements.size()
//				+", #responseElements="+responseElements.size();
	}
		
	public Request compareToMergeRequest(Method method) {

		changedRequest.put(this.getRequestElement(), this.getRequestElement().compareToMerge(method.getRequestElement()));
		
		return this.getRequestElement();
	}
	
	public HashSet<Response> compareToMergeResponse(Method method) {
		HashSet<Response> responseAdded = new HashSet<Response>();

		for(Integer status : method.getResponseElements().keySet()) {
			if(!this.getResponseElements().containsKey(status)) {
				responseAdded.add(method.getResponseElements().get(status));
			} else { // already exists > compare
				changedResponse.put(this.getResponseElements().get(status), this.getResponseElements().get(status).compareToMerge(method.getResponseElements().get(status)));
			}
		}

		return responseAdded;
	}
	
	public void mergeMethodRequests(Request addedRequestElement) {
		// first loop (addedRequestElements) not needed, since there will only be one Request element
		for(Request r : changedRequest.keySet()) {
			r.mergeParams(changedRequest.get(r));
		}
	}
	
	public void mergeMethodResponses(HashSet<Response> addedResponseElements) {
		for(Response r : addedResponseElements) {
			this.responseElements.put(r.getID(), r);
		}
		for(Response r : changedResponse.keySet()) {
			r.mergeRepresentations(changedResponse.get(r));
		}
	}

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof Method) {
			return id.equals(((Method)o).id);
		}
		else {
			return false;
		}	}

	@Override
	public boolean equalsAfterRename(Object o) {
		if (o instanceof Method) {
			return requestElement.equals(((Method) o).requestElement) && responseElements.equals(((Method)o).responseElements);
		} else {
			return false;
		}
	}

	/*@Override
	public Delta compare(WSElement element) {
		Method method = null;
		if(element instanceof Method) {
			method = (Method)element;
		} else {
			return null;
		}
		
		Delta elementDelta = null;	
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		
		// check for rename based on identifiers (id and name)
		if(!method.getIdentifier().equals(this.getIdentifier())) {
			elementDelta = new ChangeDelta(this, method, "id", this.getIdentifier(), method.getIdentifier());
		}
		if(!method.getName().equals(this.getName())) {
			if (elementDelta == null) {
				elementDelta = new ChangeDelta(this, method, "name", this.getName(),
						method.getName());
			} else {
				((ChangeDelta)elementDelta).addChangedAttribute("name", this.getName(), method.getName());
			}
		}
		
		compareRequest(method, deltas);
			
		compareResponses(method, deltas);
		
		if (elementDelta == null) {
			 create ElementDeltas	
			if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
				elementDelta = new MatchDelta(this, method);
				
			} else {
				elementDelta = new ChangeDelta(this, method, "", null, null);
			}
		}
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}

	private void compareRequest(Method method, ArrayList<Delta> deltas) {
		Request requestAdded = null;
		Request requestDeleted = null;
		
		if(requestElement == null) {
			requestAdded = method.getRequestElement();
			AddDelta delta = new AddDelta(null, requestAdded);
			WADLFile.addChildrenElements(requestAdded, delta);
			deltas.add(delta);
		}
		else if(method.getRequestElement() == null) {
			requestDeleted = this.requestElement;
			DeleteDelta delta = new DeleteDelta(requestDeleted, null);
			WADLFile.deleteChildrenElements(requestDeleted, delta);
			deltas.add(delta);
		}

		// recursive call of compare() method
		if(requestAdded == null && requestDeleted == null) {
			deltas.add(this.requestElement.compare(method.getRequestElement()));
		}
	}

	private void compareResponses(Method method, ArrayList<Delta> deltas) {
		HashMap<Integer, Response> responsesAdded = new HashMap<Integer, Response>();
		HashMap<Integer, Response> responsesDeleted = new HashMap<Integer, Response>();
		HashMap<Integer, Response> responsesIncluded = new HashMap<Integer , Response>();
		
		for(Integer status : this.responseElements.keySet()) {
			responsesIncluded.put(status, this.responseElements.get(status));
		}
		
		for(Integer status : this.responseElements.keySet()) {
			if(!method.getResponseElements().containsKey(status)) {
				responsesDeleted.put(status, this.responseElements.get(status));
				responsesIncluded.remove(status);
			}
		}
		for(Integer status : method.getResponseElements().keySet()) {
			if(!this.getResponseElements().containsKey(status)) {
				responsesAdded.put(status, method.getResponseElements().get(status));
			}
		}
		
		ArrayList<Integer> responsesNotAdded = new ArrayList<Integer>();
		ArrayList<Integer> responsesNotDeleted = new ArrayList<Integer>();
		for(Integer statusAdded : responsesAdded.keySet()) {
			for(Integer statusDeleted : responsesDeleted.keySet()) {
				// check for rename (based on children)
				if(responsesAdded.get(statusAdded).getRepresentationElements()
						.equals(responsesDeleted.get(statusDeleted).getRepresentationElements())) {
					deltas.add(responsesAdded.get(statusAdded).compare(responsesDeleted.get(statusDeleted)));
					responsesNotAdded.add(statusAdded);
					responsesNotDeleted.add(statusDeleted);
				}
			}
		}
		
		// delete all of the previously marked elements
		for(Integer notAdded : responsesNotAdded) {
			responsesAdded.remove(notAdded);
		}
		for(Integer notDeleted : responsesNotDeleted) {
			responsesDeleted.remove(notDeleted);
		}
		
		// create Deltas for additions & deletions
		for(Integer status : responsesAdded.keySet()) {
			AddDelta delta = new AddDelta(null, responsesAdded.get(status));
			WADLFile.addChildrenElements(responsesAdded.get(status), delta);
			deltas.add(delta);
		}
		for(Integer status : responsesDeleted.keySet()) {
			DeleteDelta delta = new DeleteDelta(responsesDeleted.get(status), null);
			WADLFile.deleteChildrenElements(responsesDeleted.get(status), delta);
			deltas.add(delta);
		}
		
		// recursive call of compare() method?
		for(Integer status : responsesIncluded.keySet()) {
			deltas.add(this.responseElements.get(status).compare(method.getResponseElements().get(status)));
		}
	}

	@Override
	public boolean mapElement(WSElement element) {
		System.out.println("MAP ELEMENT RESOURCES");
		HashMap<Integer, Response> mapped = new HashMap<Integer, Response>();
		HashMap<Integer, Response> added = new HashMap<Integer, Response>();
		HashMap<Integer, Response> deleted = new HashMap<Integer, Response>();
		mapByID(element, mapped, added, deleted);
		//if(added.isEmpty() && deleted.isEmpty())
			mapByStructure(element, mapped, added, deleted);
		//else if(added.isEmpty() && deleted.isEmpty())
			mapByValue(element, mapped, added, deleted);
		return false;
	}*/

	private void mapByValue(WSElement element,
			HashMap<Integer, Response> mapped,
			HashMap<Integer, Response> added, HashMap<Integer, Response> deleted) {
		// TODO Auto-generated method stub
		
	}

	//a Method Element may have 0-* requests and responses what about requests?
		private void mapByStructure(WSElement element, HashMap<Integer, Response> mapped, HashMap<Integer, Response> added, HashMap<Integer, Response> deleted) {
			mapByStructureRequest(element, mapped, added, deleted);
			mapByStructureResponse(element, mapped, added, deleted);
		}
		
		private void mapByStructureRequest(WSElement element, HashMap<Integer, Response> mapped, HashMap<Integer, Response> added, HashMap<Integer, Response> deleted) {
			HashMap<Integer, Response> notAdded = new HashMap<Integer, Response>();
			HashMap<Integer, Response> notDeleted = new HashMap<Integer, Response>();
			
			for(Integer status : added.keySet()){
				for(Integer status2 : deleted.keySet()){
					if(added.get(status).mapElement(deleted.get(status2))) {
						notAdded.put(status, added.get(status));
						notDeleted.put(status2, deleted.get(status2));
					}
				}
			}
			
			for(int status : notAdded.keySet()) {
				added.remove(status);
				mapped.put(status, notAdded.get(status));
			}
			
			for(int status : notDeleted.keySet()) {
				deleted.remove(status);
				mapped.put(status, notDeleted.get(status));
			}
			
		}
		
		private void mapByStructureResponse(WSElement element, HashMap<Integer, Response> mapped, HashMap<Integer, Response> added, HashMap<Integer, Response> deleted) {
			HashMap<Integer, Response> notAdded = new HashMap<Integer, Response>();
			HashMap<Integer, Response> notDeleted = new HashMap<Integer, Response>();
			
			for(Integer status : added.keySet()){
				for(Integer status2 : deleted.keySet()){
					if(added.get(status).mapElement(deleted.get(status2))) {
						notAdded.put(status, added.get(status));
						notDeleted.put(status2, deleted.get(status2));
					}
				}
			}
			
			for(int status : notAdded.keySet()) {
				added.remove(status);
				mapped.put(status, notAdded.get(status));
			}
			
			for(int status : notDeleted.keySet()) {
				deleted.remove(status);
				mapped.put(status, notDeleted.get(status));
			}
			
		}


	private void mapByID(WSElement element, HashMap<Integer, Response> mapped, HashMap<Integer, Response> added, HashMap<Integer, Response> deleted) {
		if(element instanceof Method){
			Method method = (Method)element;
			for(int status : method.responseElements.keySet()){
				if(this.responseElements.containsKey(status)){
					mapped.put(status,this.responseElements.get(status));
				}else {
					added.put(status,method.responseElements.get(status));
				}
			}
			
			for(int status : this.responseElements.keySet()){
				if(!mapped.containsKey(status)){
					deleted.put(status,this.responseElements.get(status));
				}
			}
			
		}
	}

	@Override
	public boolean mapElement(WADLElement element) {
		// TODO Auto-generated method stub
		return false;
	}

	public void setVariableID(String id) {
		this.id = id;
		for(Integer status : responseElements.keySet()) {
			for(String representationElement : responseElements.get(status).getRepresentationElements().keySet()) {
				Representation representation = responseElements.get(status).getRepresentationElements().get(representationElement);
				XSDElement element = representation.getElement();
				XSDFile xsdFile = representation.getResponseXSDFile();
				xsdFile.getElements().remove(element.getIdentifier());
				element.setName("response");
				XSDComplexType type = (XSDComplexType)element.getType();
				type.setVariableID("response");
				type.setName("responseType");
				xsdFile.addElement(element.getIdentifier(), element);
				
			}
		}
		
	}
}
