package wsdarwin.model;

import java.util.ArrayList;
import java.util.HashMap;

import wsdarwin.comparison.delta.AddDelta;
import wsdarwin.comparison.delta.ChangeDelta;
import wsdarwin.comparison.delta.DeleteDelta;
import wsdarwin.comparison.delta.Delta;
import wsdarwin.comparison.delta.MatchDelta;
import wsdarwin.util.DeltaUtil;

public class Interface implements WSElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7070601985272103499L;
	
	private String name;
	private String address;
	private HashMap<String,WSElement> operations;
	
	
	
	public Interface(String name, String address) {
		super();
		this.name = name;
		this.address = address;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public void setOperations(HashMap<String, WSElement> operations) {
		this.operations = operations;
	}
	@Override
	public boolean equalsAfterRename(Object o) {
		if (o instanceof Interface) {
			return operations.equals(((Interface) o).operations);
		} else {
			return false;
		}
	}
	@Override
	public boolean equalsByName(Object o) {
		if (o instanceof Interface) {
			return address.equals(((Interface) o).address);
		} else {
			return false;
		}
	}
	@Override
	public Delta diff(WSElement input) {
		
		Interface serviceInterface = null;
		if(input instanceof Interface) {
			serviceInterface = (Interface)input;
		}
		else {
			return null;
		}
		
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();	
		
		if(!this.address.equals(serviceInterface.getAddress())) {
			elementDelta = new ChangeDelta(this, serviceInterface, "address", this.address, serviceInterface.getAddress());
		}
		
		HashMap<String, WSElement> operationsAdded = new HashMap<String, WSElement>();
		HashMap<String, WSElement> operationsDeleted = new HashMap<String, WSElement>();
		HashMap<String, WSElement> operationsIncluded = new HashMap<String , WSElement>();
		for(String name : this.operations.keySet()) {
			operationsIncluded.put(name, this.operations.get(name));
		}
		for(String name : this.operations.keySet()) {
			if(!serviceInterface.getChildren().containsKey(name)) {
				operationsDeleted.put(name, this.operations.get(name));
				operationsIncluded.remove(name);
			}
		}
		for(String name : serviceInterface.getChildren().keySet()) {
			if(!this.operations.containsKey(name)) {
				operationsAdded.put(name, serviceInterface.getChildren().get(name));
				operationsIncluded.remove(name);

			}
		}
		
		ArrayList<String> operationsNotAdded = new ArrayList<String>();
		ArrayList<String> operationsNotDeleted = new ArrayList<String>();
		
		for(String operationAdded : operationsAdded.keySet()) {
			for(String operationDeleted : operationsDeleted.keySet()) {
				if(((Operation)operationsAdded.get(operationAdded)).getChildren().equals(((Operation)operationsDeleted.get(operationDeleted)).getChildren())) {
					deltas.add(operationsAdded.get(operationAdded).diff(operationsDeleted.get(operationDeleted)));
					operationsNotAdded.add(operationAdded);
					operationsNotDeleted.add(operationDeleted);
				}
			}
		}
		
		for(String operationNotAdded : operationsNotAdded) {
			operationsAdded.remove(operationNotAdded);
		}
		
		for(String operationNotDeleted : operationsNotDeleted) {
			operationsDeleted.remove(operationNotDeleted);
		}
		
		for(String operationAdded : operationsAdded.keySet()) {
			AddDelta addDelta = new AddDelta(null, operationsAdded.get(operationAdded));
			deltas.add(addDelta);
		}
		
		for(String operationDeleted : operationsDeleted.keySet()) {
			DeleteDelta deleteDelta = new DeleteDelta(operationsDeleted.get(operationDeleted), null);
			deltas.add(deleteDelta);
		}
		for (String name : operationsIncluded.keySet()) {
			//if (!this.getOperations().get(name).equals(parser.getOperations().get(name))) {
				deltas.add(this.operations.get(name).diff(serviceInterface.getChildren().get(name)));
			//}
		}
		if (elementDelta == null) {
			if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
				elementDelta = new MatchDelta(this, serviceInterface);
			} else {
				elementDelta = new ChangeDelta(this, serviceInterface, "",
						null, null);
			}
		}
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}

	@Override
	public HashMap<String, WSElement> getChildren() {
		// TODO Auto-generated method stub
		return operations;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Interface other = (Interface) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return name;
	}
	
	

}
