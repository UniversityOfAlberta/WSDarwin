package wsdarwin.wadlgenerator.model;

import wsdarwin.comparison.delta.Delta;
import wsdarwin.model.WSElement;


public class Option implements WADLElement{
	
	private String value;

	public Option(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public boolean equalsByName(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean equalsAfterRename(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		if (!(obj instanceof Option)) {
			return false;
		}
		Option other = (Option) obj;
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
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
