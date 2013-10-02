package wsdarwin.model;

import java.util.ArrayList;

import wsdarwin.comparison.delta.*;
import wsdarwin.util.DeltaUtil;


public class Operation implements WSElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5993682650342253465L;
	
	private String name;
	private String method;
	private String pattern;
	
	private IType request;
	private IType response;
	
	public Operation(String name, String method, String pattern,
			IType request, IType response) {
		super();
		this.name = name;
		this.method = method;
		this.pattern = pattern;
		this.request = request;
		this.response = response;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public IType getRequest() {
		return request;
	}

	public void setRequest(IType request) {
		this.request = request;
	}

	public IType getResponse() {
		return response;
	}

	public void setResponse(IType response) {
		this.response = response;
	}

	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof Operation) {
			Operation op = (Operation)o;
			return this.name.equals(op.name) && this.request.equals(op.request) && this.response.equals(op.response);
		}
		else {
			return false;
		}
	}
	
	public String toString() {
		return name;
	}
	
	/*public String diff(Operation operation) {
		String difference = "";
		if(!this.name.equals(operation.name)) {
			difference += "rename\n";
		}
		if(!this.input.equals(operation.input)) {
			difference += "input changed\n"+this.input.diff(operation.input, 0);
		}
		if(!this.output.equals(operation.output)) {
			difference += "output changed\n"+this.output.diff(operation.output, 0);
		}
		return difference;
	}*/
	
	public Delta diff(WSElement element) {
		Operation operation = null;
		if(element instanceof Operation) {
			operation = (Operation)element;
		}
		else {
			return null;
		}
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		if(!this.name.equals(operation.name)) {
			ChangeDelta delta = new ChangeDelta(this, operation, "name", this.getName(), operation.getName());
			deltas.add(delta);
		}
		else if(!this.method.equals(operation.method)) {
			ChangeDelta delta = new ChangeDelta(this, operation, "method", this.getMethod(), operation.getMethod());
			deltas.add(delta);
		}
		else if(!this.pattern.equals(operation.pattern)) {
			ChangeDelta delta = new ChangeDelta(this, operation, "pattern", this.getPattern(), operation.getPattern());
			deltas.add(delta);
		}
		//if(!this.input.equals(operation.input)) {
			deltas.add(this.request.diff(operation.request));
		//}
		//if(!this.output.equals(operation.output)) {
			deltas.add(this.response.diff(operation.response));
		//}
		if(DeltaUtil.containsOnlyMatchDeltas(deltas)) {
			elementDelta = new MatchDelta(this, operation);
			elementDelta.addAllDeltas(deltas);
			elementDelta.adoptOrphanDeltas();
		}
		else {
			elementDelta = new ChangeDelta(this, operation, "", null, null);
			elementDelta.addAllDeltas(deltas);
			elementDelta.adoptOrphanDeltas();
		}
		return elementDelta;
	}



	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof Operation) {
			return name.equals(((Operation)o).name);
		}
		else {
			return false;
		}
	}



	@Override
	public boolean equalsAfterRename(Object o) {
		if(o instanceof Operation) {
			return request.equals(((Operation)o).request) && response.equals(((Operation)o).response);
		}
		else {
			return false;
		}
	}

}
