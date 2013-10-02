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
	private HashMap<String, IType> elements;

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

	public HashMap<String, IType> getElements() {
		return elements;
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
		HashMap<String, IType> typesAdded = new HashMap<String, IType>();
		HashMap<String, IType> typesDeleted = new HashMap<String, IType>();
		HashMap<String, IType> typesIncluded = new HashMap<String, IType>();
		for (String name : this.elements.keySet()) {
			typesIncluded.put(name, this.elements.get(name));
		}
		if (!type.getName().equals(this.getName())) {
			ChangeDelta delta = new ChangeDelta(this, type, "name",
					this.getName(), type.getName());
			deltas.add(delta);
		}
		for (String name : this.elements.keySet()) {
			if (!type.getElements().containsKey(name)) {
				typesDeleted.put(name, this.elements.get(name));
				typesIncluded.remove(name);
			}
		}
		for (String name : type.getElements().keySet()) {
			if (!this.getElements().containsKey(name)) {
				typesAdded.put(name, type.getElements().get(name));
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
							.getElements()
							.equals(typesDeleted.get(nameDeleted).getElements())) {
						deltas.add(typesAdded.get(nameAdded).diff(
								typesDeleted.get(nameDeleted)));
						typesNotAdded.add(nameAdded);
						typesNotDeleted.add(nameDeleted);
					}
				} else if (typesAdded.get(nameAdded) instanceof PrimitiveType
						&& typesDeleted.get(nameDeleted) instanceof PrimitiveType) {
					if (typesAdded.get(nameAdded).getName()
							.equals(typesDeleted.get(nameDeleted).getName())) {
						deltas.add(typesAdded.get(nameAdded).diff(
								typesDeleted.get(nameDeleted)));
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
			addTypeElements(typesAdded.get(name), delta);
			deltas.add(delta);
		}
		for (String name : typesDeleted.keySet()) {
			DeleteDelta delta = new DeleteDelta(typesDeleted.get(name), null);
			deleteTypeElements(typesDeleted.get(name), delta);
			deltas.add(delta);
		}
		for (String name : typesIncluded.keySet()) {
			// if(!this.elements.get(name).equals(type.getElements().get(name)))
			// {
			deltas.add(this.elements.get(name).diff(
					type.getElements().get(name)));
			// }
		}
		if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
			elementDelta = new MatchDelta(this, type);
			elementDelta.addAllDeltas(deltas);
			elementDelta.adoptOrphanDeltas();
		} else {
			elementDelta = new ChangeDelta(this, type, "", null, null);
			elementDelta.addAllDeltas(deltas);
			elementDelta.adoptOrphanDeltas();
		}
		return elementDelta;
	}

	private void deleteTypeElements(IType type, DeleteDelta delta) {
		if (type instanceof ComplexType) {
			for (IType element : type.getElements().values()) {
				deleteTypeElements(element, delta);
			}
		} else {
			if (!delta.getSource().equals(type)) {
				DeleteDelta deleteDelta = new DeleteDelta(type, null);
				delta.addDelta(deleteDelta);
				deleteDelta.setParent(delta);
			}
		}

	}

	private void addTypeElements(IType type, AddDelta delta) {
		if (type instanceof ComplexType) {
			AddDelta addDelta = null;
			if (!delta.getTarget().equals(type)) {
				addDelta = new AddDelta(null, type);
				delta.addDelta(addDelta);
				addDelta.setParent(delta);
			}
			for (IType element : type.getElements().values()) {
				if (addDelta == null) {
					addTypeElements(element, delta);
				} else {
					addTypeElements(element, addDelta);
				}
			}
		} else {
			if (!delta.getTarget().equals(type)) {
				AddDelta addDelta = new AddDelta(null, type);
				delta.addDelta(addDelta);
				addDelta.setParent(delta);
			}
		}

	}
}
