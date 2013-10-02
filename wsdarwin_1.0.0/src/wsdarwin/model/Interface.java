package wsdarwin.model;

import java.util.HashMap;

import wsdarwin.comparison.delta.Delta;

public class Interface implements WSElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7070601985272103499L;
	
	private String address;
	private HashMap<String,Operation> operations;
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public HashMap<String, Operation> getOperations() {
		return operations;
	}
	public void setOperations(HashMap<String, Operation> operations) {
		this.operations = operations;
	}
	@Override
	public boolean equalsAfterRename(Object o) {
		if (o instanceof Interface) {
			return operations.equals(((Interface) o).operations);
		} else {
			return false;
		}
	}
	@Override
	public boolean equalsByName(Object o) {
		if (o instanceof Interface) {
			return address.equals(((Interface) o).address);
		} else {
			return false;
		}
	}
	@Override
	public Delta diff(WSElement input) {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
