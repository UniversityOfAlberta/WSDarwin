package wsdarwin.wadlgenerator.model.xsd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import wsdarwin.wadlgenerator.model.WADLElement;

public class XSDElement implements Comparable<XSDElement>, WADLElement{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7524658800708759113L;
	private String name;
	private XSDIType type;
	private HashSet<Object> value;
	private int minOccurs;
	private String maxOccurs;
	private TreeMap<String, Integer> typeFrequencies;
	private TreeMap<Object, Integer> valueFrequencies;
	private TreeMap<String, Object> type2valueMap;
	private HashMap<XSDElement, Double> elementDistanceMap;
	
	public XSDElement(String name, XSDIType type, Object value) {
		this.name = name;
		this.type = type;
		this.value = new HashSet<Object>();
		this.value.add(value);
		this.typeFrequencies = new TreeMap<String, Integer>();
		this.valueFrequencies = new TreeMap<Object, Integer>();
		this.type2valueMap = new TreeMap<String, Object>();
		this.elementDistanceMap = new HashMap<XSDElement, Double>();
		/*this.addType2Value(type, value);
		this.addTypeFrequency(type, 1);
		this.addValueFrequency(value, 1);*/
	}
	
	public XSDElement(String name, XSDIType type) {
		this.name = name;
		this.type = type;
		this.value = new HashSet<Object>();
		this.typeFrequencies = new TreeMap<String, Integer>();
		this.valueFrequencies = new TreeMap<Object, Integer>();
		this.type2valueMap = new TreeMap<String, Object>();
	}

	public int getMinOccurs() {
		return minOccurs;
	}

	public void setMinOccurs(int minOccurs) {
		this.minOccurs = minOccurs;
	}

	public String getMaxOccurs() {
		return maxOccurs;
	}

	public void setMaxOccurs(String maxOccurs) {
		this.maxOccurs = maxOccurs;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public XSDIType getType() {
		return type;
	}

	public void setType(XSDIType type) {
		this.type = type;
	}
	
	public Object getValue() {
		if (value.size()==1) {
			return value.iterator().next();
		}
		else {
			return value;
		}
	}

	public void addValue(Object value) {
		if(value instanceof List) {
			this.value.addAll((List<Object>)value);
		}
		else {
			this.value.add(value);
		}
	}

	public TreeMap<String, Integer> getTypeFrequencies() {
		return typeFrequencies;
	}

	public TreeMap<Object, Integer> getValueFrequencies() {
		return valueFrequencies;
	}

	public TreeMap<String, Object> getType2valueMap() {
		return type2valueMap;
	}

	public void addTypeFrequency(String type, int frequency) {
		if(this.typeFrequencies.containsKey(type)) {
			this.typeFrequencies.put(type, this.typeFrequencies.get(type)+frequency);
		}
		else {
			this.typeFrequencies.put(type, frequency);
		}
	}
	
	public void addValueFrequency(Object value, int frequency) {
		if(this.valueFrequencies.containsKey(value)) {
			this.valueFrequencies.put(value, this.valueFrequencies.get(value)+frequency);
		}
		else {
			this.valueFrequencies.put(value, frequency);
		}
	}
	
	public void addType2Value(String type, Object value) {
		this.type2valueMap.put(type, value);
	}
	
	public HashMap<XSDElement, Double> getElementDistanceMap() {
		return elementDistanceMap;
	}

	public void addElementDistanceMap(XSDElement element, Double diff) {
		this.elementDistanceMap.put(element, diff);
	}
	
	public String toString() {
		return name+":="+type;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		//result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof XSDElement)) {
			return false;
		}
		XSDElement other = (XSDElement) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		/*if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}*/
		return true;
	}

	@Override
	public int compareTo(XSDElement o) {
		return o.name.compareTo(name);
	}
	
	/*public Delta compare(WSElement element) {
		XSDElement xsdElement = null;
		if(element instanceof XSDElement) {
			xsdElement  = (XSDElement)element;
		}
		else {
			return null;
		}
		if(this.type instanceof XSDComplexType) {
			return this.type.compare(xsdElement.getType());
		}
		else {
			Delta typeDelta = this.type.compare(xsdElement.getType());
			if(typeDelta instanceof ChangeDelta) {
				return new ChangeDelta(this, element, ((ChangeDelta)typeDelta).getChangedAttribute(), ((ChangeDelta) typeDelta).getNewValue(), ((ChangeDelta) typeDelta).getOldValue());
			}
			else {
				return new MatchDelta(this, element);
			}
		}
		
	}*/

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof XSDElement) {
			return name.equals(((XSDElement)o).name);
		}
		else {
			return false;
		}
	}

	@Override
	public boolean equalsAfterRename(Object o) {
		if(o instanceof XSDElement) {
			return type.getName().equals(((XSDElement)o).type.getName());
		}
		else {
			return false;
		}
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


}
