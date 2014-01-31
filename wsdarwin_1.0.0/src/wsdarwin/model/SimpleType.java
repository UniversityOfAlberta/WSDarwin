package wsdarwin.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import wsdarwin.comparison.delta.ChangeDelta;
import wsdarwin.comparison.delta.Delta;
import wsdarwin.comparison.delta.MatchDelta;
import wsdarwin.util.DeltaUtil;

public class SimpleType implements IType {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4131625151539868067L;
	
	private String name;
	private String elementName;
	private IType base;
	private HashSet<String> options;
	
	
	
	public SimpleType(String name, String elementName) {
		super();
		this.name = name;
		this.elementName = elementName;
	}
	public String getElementName() {
		return elementName;
	}
	public void setElementName(String elementName) {
		this.elementName = elementName;
	}
	public IType getBase() {
		return base;
	}
	public void setBase(IType base) {
		this.base = base;
	}
	public HashSet<String> getOptions() {
		return options;
	}
	public void setOptions(HashSet<String> options) {
		this.options = options;
	}
	
	public void addOption(String option) {
		this.options.add(option);
	}
	
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public boolean equalsAfterRename(Object o) {
		if(o instanceof SimpleType) {
			return base.equals(((SimpleType)o).base) && options.equals(((SimpleType)o).options);
		}
		else {
			return false;
		}
	}
	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof SimpleType) {
			return name.equals(((SimpleType)o).name);
		}
		else {
			return false;
		}
	}
	@Override
	public void addElement(String name, IType type) {
		
	}
	@Override
	public String getName() {
		return name;
	}
	@Override
	public String getVariableName() {
		return elementName;
	}
	@Override
	public Delta diff(WSElement input) {
		SimpleType type = null;
		if(input instanceof SimpleType) {
			type = (SimpleType)input;
		}
		else {
			return null;
		}
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		
		if(!type.getName().equals(this.getName())) {
			ChangeDelta delta = new ChangeDelta(this, type, "name", this.getName(), type.getName());
			deltas.add(delta);
		}
		
		if(!type.getElementName().equals(this.getElementName())) {
			ChangeDelta delta = new ChangeDelta(this, type, "elementName", this.getElementName(), type.getElementName());
			deltas.add(delta);
		}
		
		deltas.add(this.base.diff(type.getBase()));
		for (String name : this.options) {
			if (!type.getOptions().contains(name)) {
				ChangeDelta delta = new ChangeDelta(this, type,
						"option", name, "");
				deltas.add(delta);
			}
		}
		for (String name : type.getOptions()) {
			if (!this.getOptions().contains(name)) {
				ChangeDelta delta = new ChangeDelta(this, type,
						"option", "", name);
				deltas.add(delta);
			}
		}
		
		if(DeltaUtil.containsOnlyMatchDeltas(deltas)) {
			elementDelta = new MatchDelta(this, type);
			elementDelta.addAllDeltas(deltas);
			elementDelta.adoptOrphanDeltas();
		}
		else {
			elementDelta = new ChangeDelta(this, type, "", null, null);
			elementDelta.addAllDeltas(deltas);
			elementDelta.adoptOrphanDeltas();
		}
		return elementDelta;
	}
	@Override
	public HashMap<String, WSElement> getChildren() {
		return new HashMap<String, WSElement>();
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleType other = (SimpleType) obj;
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
		return elementName+":"+base.getName();
	}
	@Override
	public int getNumberOfTypes() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getNesting() {
		// TODO Auto-generated method stub
		return 0;
	}

	
}
