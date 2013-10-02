package wsdarwin.model;

import java.io.Serializable;

import wsdarwin.comparison.delta.Delta;

public interface WSElement extends Serializable {

	public boolean equalsAfterRename(Object o);

	public boolean equalsByName(Object o);
	
	public Delta diff(WSElement input);

}
