package wsdarwin.model;

import java.util.ArrayList;
import java.util.HashMap;

import wsdarwin.comparison.delta.*;
import wsdarwin.util.DeltaUtil;



public class IService implements WSElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = -933940872984097955L;
	
	private String targetNamespace;
	private HashMap<String, WSElement> interfaces;
	
	public IService() {
		
	}
	
	@Override
	public String getName() {
		return targetNamespace;
	}
		
	public IService(String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}

	public String getTargetNamespace() {
		return targetNamespace;
	}

	public void setTargetNamespace(String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}

	public HashMap<String, WSElement> getInterfaces() {
		return interfaces;
	}

	public void setInterfaces(HashMap<String, WSElement> interfaces) {
		this.interfaces = interfaces;
	}

	public Delta diff(WSElement element) {
		IService service = null;
		if(element instanceof IService) {
			service = (IService)element;
		}
		else {
			return null;
		}
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		
		if(!this.targetNamespace.equals(service.getTargetNamespace())) {
			elementDelta = new ChangeDelta(this, service, "targetNamespace", this.targetNamespace, service.targetNamespace);
		}
		
		HashMap<String, WSElement> interfacesAdded = new HashMap<String, WSElement>();
		HashMap<String, WSElement> interfacesDeleted = new HashMap<String, WSElement>();
		HashMap<String, WSElement> interfacesIncluded = new HashMap<String , WSElement>();
		for(String location : this.interfaces.keySet()) {
			interfacesIncluded.put(location, this.interfaces.get(location));
		}
		for(String address : this.interfaces.keySet()) {
			if(!service.getChildren().containsKey(address)) {
				interfacesDeleted.put(address, this.interfaces.get(address));
				interfacesIncluded.remove(address);
			}
		}
		for(String address : service.getChildren().keySet()) {
			if(!this.interfaces.containsKey(address)) {
				interfacesAdded.put(address, service.getChildren().get(address));
				interfacesIncluded.remove(address);
			}
		}
		
		ArrayList<String> interfacesNotAdded = new ArrayList<String>();
		ArrayList<String> interfacesNotDeleted = new ArrayList<String>();
		
		for(String nameAdded : interfacesAdded.keySet()) {
			for(String nameDeleted : interfacesDeleted.keySet()) {
				if(((Interface)interfacesAdded.get(nameAdded)).getChildren().equals(((Interface)interfacesDeleted.get(nameDeleted)).getChildren())) {
					deltas.add(interfacesAdded.get(nameAdded).diff(interfacesDeleted.get(nameDeleted)));
					interfacesNotAdded.add(nameAdded);
					interfacesNotDeleted.add(nameDeleted);
				}
			}
		}
		
		for(String nameNotAdded : interfacesNotAdded) {
			interfacesAdded.remove(nameNotAdded);
		}
		
		for(String nameNotDeleted : interfacesNotDeleted) {
			interfacesDeleted.remove(nameNotDeleted);
		}
		
		for(String added : interfacesAdded.keySet()) {
			AddDelta addDelta = new AddDelta(null, interfacesAdded.get(added));
			deltas.add(addDelta);
		}
		
		for(String deleted : interfacesDeleted.keySet()) {
			DeleteDelta deleteDelta = new DeleteDelta(interfacesDeleted.get(deleted), null);
			deltas.add(deleteDelta);
		}
		
		
		for (String address : interfacesIncluded.keySet()) {
			//if (!this.getOperations().get(name).equals(parser.getOperations().get(name))) {
				deltas.add(this.interfaces.get(address).diff(service.getChildren().get(address)));
			//}
		}
		
		if (elementDelta == null) {
			if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
				elementDelta = new MatchDelta(this, service);
			} else {
				elementDelta = new ChangeDelta(this, service, "", null, null);
			}
		}
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}

	@Override
	public boolean equalsAfterRename(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean equalsByName(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public HashMap<String, WSElement> getChildren() {
		return interfaces;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IService other = (IService) obj;
		if (targetNamespace == null) {
			if (other.targetNamespace != null)
				return false;
		} else if (!targetNamespace.equals(other.targetNamespace))
			return false;
		return true;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.targetNamespace;
	}
	
	public int getNumberOfTypes() {
		int typeCount = 0;
		for(WSElement serviceInterface : interfaces.values()) {
			typeCount += ((Interface)serviceInterface).getNumberOfTypes();
		}
		return typeCount;
 	}
	
	public double getAverageNestingPerOperation() {
		double averageNesting = 0;
		for(WSElement serviceInterface : interfaces.values()) {
			averageNesting += ((Interface)serviceInterface).getAverageNestingPerOperation();
		}
		return averageNesting/(double)interfaces.size();
	}



}
