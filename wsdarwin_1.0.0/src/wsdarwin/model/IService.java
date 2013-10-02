package wsdarwin.model;

import java.util.ArrayList;
import java.util.HashMap;

import wsdarwin.comparison.delta.*;



public class IService implements WSElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = -933940872984097955L;
	
	private String targetNamespace;
	private Schema schema;
	private Interface serviceInterface;
	
		
	public String getTargetNamespace() {
		return targetNamespace;
	}

	public void setTargetNamespace(String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}

	public Schema getSchema() {
		return schema;
	}

	public void setSchema(Schema schema) {
		this.schema = schema;
	}

	public Interface getServiceInterface() {
		return serviceInterface;
	}

	public void setServiceInterface(Interface serviceInterface) {
		this.serviceInterface = serviceInterface;
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
		HashMap<String, Operation> operationsAdded = new HashMap<String, Operation>();
		HashMap<String, Operation> operationsDeleted = new HashMap<String, Operation>();
		HashMap<String, Operation> operationsIncluded = new HashMap<String , Operation>();
		for(String name : this.serviceInterface.getOperations().keySet()) {
			operationsIncluded.put(name, this.serviceInterface.getOperations().get(name));
		}
		for(String name : this.serviceInterface.getOperations().keySet()) {
			if(!service.getServiceInterface().getOperations().containsKey(name)) {
				operationsDeleted.put(name, this.serviceInterface.getOperations().get(name));
				operationsIncluded.remove(name);
				DeleteDelta delta = new DeleteDelta(this.serviceInterface.getOperations().get(name), null);
				deltas.add(delta);
			}
		}
		for(String name : service.getServiceInterface().getOperations().keySet()) {
			if(!this.serviceInterface.getOperations().containsKey(name)) {
				operationsAdded.put(name, service.getServiceInterface().getOperations().get(name));
				operationsIncluded.remove(name);
				AddDelta delta = new AddDelta(null, service.getServiceInterface().getOperations().get(name));
				deltas.add(delta);
			}
		}
		for (String name : operationsIncluded.keySet()) {
			//if (!this.getOperations().get(name).equals(parser.getOperations().get(name))) {
				deltas.add(this.serviceInterface.getOperations().get(name).diff(service.getServiceInterface().getOperations().get(name)));
			//}
		}
		if(deltas.isEmpty()) {
			elementDelta = new MatchDelta(this, service);
			elementDelta.addAllDeltas(deltas);
			elementDelta.adoptOrphanDeltas();
		}
		else {
			elementDelta = new ChangeDelta(this, service, "", null, null);
			elementDelta.addAllDeltas(deltas);
			elementDelta.adoptOrphanDeltas();
		}
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

}
