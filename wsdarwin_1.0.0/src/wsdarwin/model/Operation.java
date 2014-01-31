package wsdarwin.model;

import java.util.ArrayList;
import java.util.HashMap;

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

	public Operation(String name, String method, String pattern, IType request,
			IType response) {
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

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Operation other = (Operation) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String toString() {
		return name;
	}

	/*
	 * public String diff(Operation operation) { String difference = "";
	 * if(!this.name.equals(operation.name)) { difference += "rename\n"; }
	 * if(!this.input.equals(operation.input)) { difference +=
	 * "input changed\n"+this.input.diff(operation.input, 0); }
	 * if(!this.output.equals(operation.output)) { difference +=
	 * "output changed\n"+this.output.diff(operation.output, 0); } return
	 * difference; }
	 */

	public Delta diff(WSElement element) {
		Operation operation = null;
		if (element instanceof Operation) {
			operation = (Operation) element;
		} else {
			return null;
		}
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		if (!this.name.equals(operation.name)) {
			elementDelta = new ChangeDelta(this, operation, "name",
					this.getName(), operation.getName());
		}
		if (!this.method.equals(operation.method)) {
			if (elementDelta == null) {
				elementDelta = new ChangeDelta(this, operation, "method",
						this.getMethod(), operation.getMethod());
			} else {
				((ChangeDelta) elementDelta).addChangedAttribute("method",
						this.getMethod(), operation.getMethod());
			}
		}
		if (!this.pattern.equals(operation.pattern)) {
			if (elementDelta == null) {
				elementDelta = new ChangeDelta(this, operation, "pattern",
						this.getPattern(), operation.getPattern());
			} else {
				((ChangeDelta) elementDelta).addChangedAttribute("pattern",
						this.getPattern(), operation.getPattern());
			}
		}

		deltas.add(this.request.diff(operation.request));
		deltas.add(this.response.diff(operation.response));

		if (elementDelta == null) {
			if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
				elementDelta = new MatchDelta(this, operation);
			} else {
				elementDelta = new ChangeDelta(this, operation, "", null, null);
			}
		}
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}

	@Override
	public boolean equalsByName(Object o) {
		if (o instanceof Operation) {
			return name.equals(((Operation) o).name);
		} else {
			return false;
		}
	}

	@Override
	public boolean equalsAfterRename(Object o) {
		if (o instanceof Operation) {
			return request.equals(((Operation) o).request)
					&& response.equals(((Operation) o).response);
		} else {
			return false;
		}
	}

	@Override
	public HashMap<String, WSElement> getChildren() {
		HashMap<String, WSElement> children = new HashMap<String, WSElement>();
		children.put(request.getName(), request);
		children.put(response.getName(), response);
		return children;
	}

	public int getNumberOfTypes() {
		int typeCount = 0;
		typeCount += request.getNumberOfTypes();
		typeCount += response.getNumberOfTypes();
		return typeCount;
	}

	public double getNesting() {
		double nesting1 = request.getNesting();
		double nesting2 = request.getNesting();
		return Math.max(nesting1, nesting2);
	}

	
}
