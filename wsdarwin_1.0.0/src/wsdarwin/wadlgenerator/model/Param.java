package wsdarwin.wadlgenerator.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import wsdarwin.comparison.delta.*;
import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class Param implements WADLElement {

	private String name;
	private String type;
	private HashSet<Option> options;
	private Object value;				// will contain the final value for the diffMerged WADL file (e.g. enum{DRIVING, WALKING,...})
	private HashMap<String, Integer> typeFrequencies;
	private HashMap<Object, Integer> valueFrequencies;	// contains all of the individual values String:DRIVING, ...
	private HashMap<String, Object>  type2valueMap;

	private String style;
	private boolean required;			// default should be 'false', if nothing is specified 
	
	
	public Param(String name, String type, Object value, String style, boolean required) {
		super();
		
		System.out.println("--------> " + type + ", name: " + name + ", style: " + style);
		this.name = name;
		this.type = type;
		this.value = value;
		this.style = style;
		this.required = required;
		
		this.typeFrequencies = new HashMap<String, Integer>();
		this.valueFrequencies = new HashMap<Object, Integer>();
		this.type2valueMap = new HashMap<String, Object>();
		this.addTypeFrequency(type, 1);
		this.addValueFrequency(value, 1);
		this.addType2Value(type, value);
		this.options = new HashSet<Option>();
	}
	
	public Param(String name, String type, String style, boolean required) {
		super();
		this.name = name;
		this.type = type;
		this.style = style;
		this.required = required;
		
		this.options = new HashSet<Option>();
	}

	public String getIdentifier() {
		return name;
	}

	public void setIdentifier(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		System.out.println("-----------------> SETTINGS PARAM TYPE: " + type);
		this.type = type;
	}

	public HashSet<Option> getOptions() {
		return options;
	}
	
	public void addOption(Option option) {
		this.options.add(option);
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}
	
	public Object getValue() {
		return value;
	}

	public void setValue(Object defaultVal) {
		this.value = defaultVal;
	}
	
	public HashMap<String, Integer> getTypeFrequencies() {
		return typeFrequencies;
	}

	public HashMap<Object, Integer> getValueFrequencies() {
		return valueFrequencies;
	}

	public HashMap<String, Object> getType2valueMap() {
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
	
	
	/* Gets called by diff().
	 * Check during diff&merge process if the values in valueFrequencies are 
	 * enumerations. >> will be the <param><options>... part in the final WADL.
	 *  OR call it figureOutOptions() */
	private Object findEnumerations() {
		return null;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
//		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Param other = (Param) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
//		if (type == null) {
//			if (other.type != null)
//				return false;
//		} else if (!type.equals(other.type))
//			return false;
		return true;
	}
	
	public String toString() {
		return name;
//		return "<param> NAME="+name+", type="+type+", value="+value+", style="+style+", required="+required+", "
//				+"FREQUENCIES: type="+typeFrequencies+", value="+valueFrequencies;
	}

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof Param) {
			return name.equals(((Param)o).name);
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
		Param param = null;
		if(element instanceof Param) {
			param = (Param)element;
		} else {
			return null;
		}
		
		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();
		
		// compare NAME
		if(!param.getIdentifier().equals(this.getIdentifier())) {
			elementDelta = new ChangeDelta(this, param, "name", this.getIdentifier(), param.getIdentifier());
		}
		// compare type
		if(!param.getType().equals(this.getType())) {
			if (elementDelta != null) {
				((ChangeDelta)elementDelta).addChangedAttribute("type", this.getType(), param.getType());
			} else {
				elementDelta = new ChangeDelta(this, param, "type", this.getType(), param.getType());
			}
		}
		// compare style
		if(!param.getStyle().equals(this.getStyle())) {
			if (elementDelta != null) {
				((ChangeDelta)elementDelta).addChangedAttribute("style", this.getStyle(), param.getStyle());
			} else {
				elementDelta = new ChangeDelta(this, param, "style", this.getStyle(), param.getStyle());
			}
		}
		// required
		if(param.isRequired() != this.isRequired()) {
			if (elementDelta != null) {
				((ChangeDelta)elementDelta).addChangedAttribute("required", Boolean.toString(this.isRequired()), Boolean.toString(param.isRequired()));
			} else {
				elementDelta = new ChangeDelta(this, param, "required", Boolean.toString(this.isRequired()), Boolean.toString(param.isRequired()));
			}
		}
		
		for(Option option : this.options) {
			if(!param.getOptions().contains(option)) {
				deltas.add(new DeleteDelta(option, null));
			}
		}
		
		for(Option option : param.getOptions()) {
			if(!this.getOptions().contains(option)) {
				deltas.add(new AddDelta(null, option));
			}
		}
		
		// TODO add default-attribute when Enum got detected
		
		if (elementDelta == null) {
			if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
				elementDelta = new MatchDelta(this, param);
			} else {
				elementDelta = new ChangeDelta(this, param, "", null, null);
			}
		}
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}*/

	@Override
	public boolean mapElement(WADLElement element) {
		// TODO Auto-generated method stub
		return false;
	}
}
