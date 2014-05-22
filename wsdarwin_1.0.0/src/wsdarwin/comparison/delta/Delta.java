package wsdarwin.comparison.delta;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import wsdarwin.model.IType;
import wsdarwin.model.WSElement;

public abstract class Delta {
	
	protected WSElement source;
	protected WSElement target;
	protected Delta parent;
	protected ArrayList<Delta> deltas;
	
	protected Delta(WSElement source, WSElement target) {
		this.source = source;
		this.target = target;
		this.deltas = new ArrayList<Delta>();
	}
	
	public void addDelta(Delta delta) {
		this.deltas.add(delta);
	}
	
	public void addAllDeltas(ArrayList<Delta> deltas) {
		this.deltas.addAll(deltas);
	}

	public WSElement getSource() {
		return source;
	}

	public WSElement getTarget() {
		return target;
	}

	public ArrayList<Delta> getDeltas() {
		return deltas;
	}
	
	public Delta getParent() {
		return parent;
	}

	public void setParent(Delta parent) {
		this.parent = parent;
	}

	public ArrayList<AddDelta> getAddDeltas() {
		ArrayList<AddDelta> addDeltas = new ArrayList<AddDelta>();
		if(this instanceof AddDelta) {
			addDeltas.add((AddDelta)this);
		}
		for(Delta delta : deltas) {
			addDeltas.addAll(delta.getAddDeltas());
		}
		return addDeltas;
	}
	
	public ArrayList<DeleteDelta> getDeleteDeltas() {
		ArrayList<DeleteDelta> deleteDeltas = new ArrayList<DeleteDelta>();
		if(this instanceof DeleteDelta) {
			deleteDeltas.add((DeleteDelta)this);
		}
		for(Delta delta : deltas) {
			deleteDeltas.addAll(delta.getDeleteDeltas());
		}
		return deleteDeltas;
	}
	
	public void adoptOrphanDeltas() {
		for(Delta delta : deltas) {
			delta.setParent(this);
		}
	}
	
	public Delta findParentOfDeltaBySource(WSElement source) {
		Delta parent = null;
		for(Delta delta : deltas) {
			if (!(delta instanceof AddDelta)) {
				if (delta.getSource().equals(source)) {
					parent = this;
				}
				if (!delta.getDeltas().isEmpty()) {
					for (Delta child : delta.getDeltas()) {
						parent = child.findParentOfDeltaBySource(source);
					}
				}
			}
		}
		return parent;
	}
	
	public Delta findParentOfDeltaByTarget(WSElement target) {
		for(Delta delta : deltas) {
			if (!(delta instanceof DeleteDelta)) {
				if (delta.getTarget().equals(target)) {
					return this;
				}
				if (!delta.getDeltas().isEmpty()) {
					for (Delta child : delta.getDeltas()) {
						return child.findParentOfDeltaByTarget(target);
					}
				}
			}
		}
		return null;
	}
	
	public Delta getDeltaByType(String type) {
		Delta typeDelta = null;
		for(Delta delta : deltas) {
			if(delta.getSource() != null) {
				if(delta.getSource() instanceof IType) {
					if(((IType)delta.getSource()).getName().equalsIgnoreCase(type) || ((IType)delta.getSource()).getVariableName().equalsIgnoreCase(type)) {
						typeDelta = delta;
					}
				}
			}
			else {
				if(delta.getTarget() instanceof IType) {
					if(((IType)delta.getTarget()).getName().equalsIgnoreCase(type) || ((IType)delta.getTarget()).getVariableName().equalsIgnoreCase(type)) {
						typeDelta =  delta;
					}
				}
			}
			if(typeDelta == null) {
				typeDelta = delta.getDeltaByType(type);
			}
		}
		return typeDelta;
	}
	
	public WSElement getElement() {
		if(source != null) {
			return source;
		}
		else {
			return target;
		}
	}
	
	public abstract void printDelta(int level);
	
	public abstract Element createXMLElement(Document document, Element parent);

}
