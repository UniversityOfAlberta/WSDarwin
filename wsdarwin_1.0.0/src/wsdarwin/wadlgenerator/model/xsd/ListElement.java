package wsdarwin.wadlgenerator.model.xsd;

import java.util.ArrayList;
import java.util.HashSet;

import wsdarwin.wadlgenerator.model.WADLElement;

public class ListElement implements XSDIType {
	
	private XSDIType itemType;
	
	

	public ListElement(XSDIType itemType) {
		super();
		this.itemType = itemType;
	}

	public XSDIType getItemType() {
		return itemType;
	}

	public void setItemType(XSDIType itemType) {
		this.itemType = itemType;
	}

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof ListElement) {
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
	public Delta compare(WSElement element) {
		ListElement list = null;
		if(element instanceof ListElement) {
			list = (ListElement)element;
		}
		else {
			return null;
		}
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		if(this.itemType.getName().equals(list.getItemType().getName())) {
			deltas.add(this.itemType.compare(list.getItemType()));
		}
		else {
			AddDelta addDelta = new AddDelta(null, list.getItemType());
			DeleteDelta deleteDelta = new DeleteDelta(this.getItemType(), null);
			deltas.add(addDelta);
			deltas.add(deleteDelta);
		}
		if(DeltaUtil.containsOnlyMatchDeltas(deltas)) {
			elementDelta = new MatchDelta(this, element);
		}
		else {
			elementDelta = new ChangeDelta(this, element, "", null, null);
		}
		
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}*/

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashSet<XSDElement> compareToMerge(XSDIType xSDIType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean mapElement(WADLElement element) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getIdentifier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVariableID(String variableId) {
		// TODO Auto-generated method stub
		
	}

}
