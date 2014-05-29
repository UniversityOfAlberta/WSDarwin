package wsdarwin.model;

import java.io.Serializable;
import java.util.HashMap;

import wsdarwin.comparison.delta.Delta;

public interface WSElement extends Serializable {

	public boolean equalsAfterRename(Object o);

	public boolean equalsByName(Object o);
	
	public Delta diff(WSElement input);

	public HashMap<String, WSElement> getChildren();
	
	public String getName();
}
