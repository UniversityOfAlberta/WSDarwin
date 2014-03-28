package wsdarwin.wadlgenerator.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import wsdarwin.comparison.delta.*;
import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class Request implements WADLElement {

	private HashMap<String, Param> paramElements;
	private HashSet<Param> changedParam;

	public Request() {
		this.paramElements = new HashMap<String, Param>();
		this.changedParam = new HashSet<Param>();
	}
	
	@Override
	public String getIdentifier() {
		return null;
	}
	
	public HashMap<String, Param> getParamElements() {
		return paramElements;
	}

	public void addParamElement(String name, Param param) {
		paramElements.put(name, param);
	}
	
	public void addAllParamElements(HashMap<String, Param> elements) {
		paramElements.putAll(elements);		
	}

/* 	NOTE: hashCode() and equals() were hand- and hard-coded. 
 *  Since all WADL Request objects don't have any attributes, they will always 
 *  be the same. equals() will alaways return true and the hashCode() will always
 *  be the same. */
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return true;		// all WADL Request objects will always be the same
	}
	
	public String toString() {
		return "Request";
//		return "#param="+paramElements.size();
//		return "<request> #paramElements="+paramElements.size();
	}
	
	public HashSet<Param> compareToMerge(Request request) {
		HashSet<Param> paramAdded = new HashSet<Param>();

		for(String name : request.getParamElements().keySet()) {
			Param passedParam = request.getParamElements().get(name);
			Param retrievedElement = this.getParamElements().get(name);

			if(!this.getParamElements().containsKey(name)) {
				paramAdded.add(request.getParamElements().get(name));
				// + set frequency to 1 		| 		NOTE LZ: I am not sure if we really need this part
				passedParam.addTypeFrequency(passedParam.getType(), 1);
				passedParam.addValueFrequency(passedParam.getValue(), 1);
				passedParam.addType2Value(passedParam.getType(), passedParam.getValue());
				
				//if(MockClientLukas.DEBUG) System.out.println("(!) type="+passedParam.getType()+", value="+passedParam.getValue());
				
			} else {
				paramAdded.add(this.getParamElements().get(name));
				// increase frequency by 1
				retrievedElement.addTypeFrequency(passedParam.getType(), 1);
				retrievedElement.addValueFrequency(passedParam.getValue(), 1);
				retrievedElement.addType2Value(passedParam.getType(), passedParam.getValue());
				
				//if(MockClientLukas.DEBUG) System.out.println("(=) type="+passedParam.getType()+", value="+passedParam.getValue());
			}
		}
		return paramAdded;
	}
	
	public void mergeParams(HashSet<Param> addedParamElements) {
		for(Param p : addedParamElements) {
			this.paramElements.put(p.getIdentifier(), p);
		}
	}

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof Request) {
			return true;
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
	public Delta compare(WADLElement element) {
		Request request = null;
		if(element instanceof Request) {
			request = (Request)element;
		} else {
			return null;
		}
		
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		// no need to check for rename (no attributes)
		HashMap<String, Param> paramsAdded = new HashMap<String, Param>();
		HashMap<String, Param> paramsDeleted = new HashMap<String, Param>();
		HashMap<String, Param> paramsIncluded = new HashMap<String , Param>();
		
		// fill paramIncluded
		for(String name : this.paramElements.keySet()) {
			paramsIncluded.put(name, this.getParamElements().get(name));
		}
		
		// fill paramDeleted (& for match also remove from paramIncluded)  		(iterate over File1)
		for(String name : this.paramElements.keySet()) {
			if(!request.getParamElements().containsKey(name)) {
				paramsDeleted.put(name, this.paramElements.get(name));
				paramsIncluded.remove(name);
			}
		}
		
		// fill paramAdded			(iterate over File2)
		for(String name : request.paramElements.keySet()) {
			if(!this.getParamElements().containsKey(name)) {
				paramsAdded.put(name, paramElements.get(name));
			}
		}
		
		// iterate over included + call compare() method
		for(String name : paramsIncluded.keySet()) {
			deltas.add(this.paramElements.get(name).compare(request.getParamElements().get(name)));
		}

		// create elementDeltas
		if(DeltaUtil.containsOnlyMatchDeltas(deltas)) {
			elementDelta = new MatchDelta(this, request);
		}
		else {
			elementDelta = new ChangeDelta(this, request, "", null, null);
			
		}	
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		
		return elementDelta;
	}*/

	@Override
	public boolean mapElement(WADLElement element) {
		System.out.println("MAP ELEMENT GRAMMARS");
		HashMap<String, Param> mapped = new HashMap<String, Param>();
		HashMap<String, Param> added = new HashMap<String, Param>();
		HashMap<String, Param> deleted = new HashMap<String, Param>();
		mapByID(element, mapped, added, deleted);
		mapByStructure(element, mapped, added, deleted);
		return false;
	}

	private void mapByStructure(WADLElement element,
			HashMap<String, Param> mapped, HashMap<String, Param> added,
			HashMap<String, Param> deleted) {
		// TODO Auto-generated method stub
		
	}

	private void mapByID(WADLElement element, HashMap<String, Param> mapped, HashMap<String, Param> added, HashMap<String, Param> deleted) {
		if(element instanceof Param){
			Request request = (Request)element;
			for(String name : request.paramElements.keySet()) {
				if(this.getParamElements().containsKey(name)){
					mapped.put(name,this.paramElements.get(name));
				}else{
					added.put(name,this.paramElements.get(name));
				}
			}

			for(String name : this.paramElements.keySet()) {
				if(!mapped.containsKey(name)){
					deleted.put(name,this.paramElements.get(name));
				}
			}
		}
	}
}