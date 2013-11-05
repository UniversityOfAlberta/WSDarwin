package wsdarwin.model;

import java.util.ArrayList;
import java.util.HashMap;

import wsdarwin.comparison.delta.AddDelta;
import wsdarwin.comparison.delta.ChangeDelta;
import wsdarwin.comparison.delta.DeleteDelta;
import wsdarwin.comparison.delta.Delta;
import wsdarwin.comparison.delta.MatchDelta;
import wsdarwin.util.DeltaUtil;

public class ComplexType implements IType {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7518917502287303139L;

	private String name;
	private String elementName;
	private HashMap<String, WSElement> elements;

	
	
	public ComplexType(String name, String elementName) {
		this.name = name;
		this.elementName = elementName;
		this.elements = new HashMap<String, WSElement>();
	}

	@Override
	public boolean equalsAfterRename(Object o) {
		if (o instanceof ComplexType) {
			return elements.equals(((ComplexType) o).elements);
		} else {
			return false;
		}
	}

	@Override
	public boolean equalsByName(Object o) {
		if (o instanceof ComplexType) {
			return name.equals(((ComplexType) o).name);
		} else {
			return false;
		}
	}

	public void addElement(String name, IType type) {
		elements.put(name, type);

	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getVariableName() {
		// TODO Auto-generated method stub
		return elementName;
	}

	@Override
	public Delta diff(WSElement input) {
		IType type = null;
		if (input instanceof ComplexType) {
			type = (ComplexType) input;
		} else {
			return null;
		}
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		HashMap<String, WSElement> typesAdded = new HashMap<String, WSElement>();
		HashMap<String, WSElement> typesDeleted = new HashMap<String, WSElement>();
		HashMap<String, WSElement> typesIncluded = new HashMap<String, WSElement>();
		for (String name : this.elements.keySet()) {
			typesIncluded.put(name, this.elements.get(name));
		}
		if (!type.getName().equals(this.getName())) {
			elementDelta = new ChangeDelta(this, type, "name",
					this.getName(), type.getName());
		}
		if (!type.getVariableName().equals(this.getVariableName())) {
			if (elementDelta == null) {
				elementDelta = new ChangeDelta(this, type, "elementName",
						this.getVariableName(), type.getVariableName());
			}
			else {
				((ChangeDelta)elementDelta).addChangedAttribute("elementName", this.getVariableName(), type.getVariableName());
			}
		}
		for (String name : this.elements.keySet()) {
			if (!type.getChildren().containsKey(name)) {
				typesDeleted.put(name, this.elements.get(name));
				typesIncluded.remove(name);
			}
		}
		for (String name : type.getChildren().keySet()) {
			if (!this.getChildren().containsKey(name)) {
				typesAdded.put(name, type.getChildren().get(name));
			}
		}
		ArrayList<String> typesNotAdded = new ArrayList<String>();
		ArrayList<String> typesNotDeleted = new ArrayList<String>();
		for (String nameAdded : typesAdded.keySet()) {
			for (String nameDeleted : typesDeleted.keySet()) {
				if (typesAdded.get(nameAdded) instanceof ComplexType
						&& typesDeleted.get(nameDeleted) instanceof ComplexType) {
					if (typesAdded
							.get(nameAdded)
							.getChildren()
							.equals(typesDeleted.get(nameDeleted).getChildren())) {
						deltas.add(typesAdded.get(nameAdded).diff(
								typesDeleted.get(nameDeleted)));
						typesNotAdded.add(nameAdded);
						typesNotDeleted.add(nameDeleted);
					}
				} else if (typesAdded.get(nameAdded) instanceof PrimitiveType
						&& typesDeleted.get(nameDeleted) instanceof PrimitiveType) {
					if (((IType)typesAdded.get(nameAdded)).getName()
							.equals(((IType)typesDeleted.get(nameDeleted)).getName())) {
						deltas.add(typesDeleted.get(nameDeleted).diff(
								typesAdded.get(nameAdded)));
						typesNotAdded.add(nameAdded);
						typesNotDeleted.add(nameDeleted);
					}
				}
			}
		}
		for (String notAdded : typesNotAdded) {
			typesAdded.remove(notAdded);
		}
		for (String notDeleted : typesNotDeleted) {
			typesDeleted.remove(notDeleted);
		}
		for (String name : typesAdded.keySet()) {
			AddDelta delta = new AddDelta(null, typesAdded.get(name));
			deltas.add(delta);
		}
		for (String name : typesDeleted.keySet()) {
			DeleteDelta delta = new DeleteDelta(typesDeleted.get(name), null);
			deltas.add(delta);
		}
		for (String name : typesIncluded.keySet()) {
			// if(!this.elements.get(name).equals(type.getElements().get(name)))
			// {
			deltas.add(this.elements.get(name).diff(
					type.getChildren().get(name)));
			// }
		}
		if (elementDelta == null) {
			if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
				elementDelta = new MatchDelta(this, type);
			} else {
				elementDelta = new ChangeDelta(this, type, "", null, null);
			}
		}
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}

	@Override
	public HashMap<String, WSElement> getChildren() {
		// TODO Auto-generated method stub
		return elements;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ComplexType other = (ComplexType) obj;
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
		return name+":"+elementName;
	}
	
	
}
