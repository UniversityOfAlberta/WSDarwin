package wsdarwin.wadlgenerator.testMains;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.test.AddressBook;

public class AddressBookApplication extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(TestMainForWADLGeneration.class);
		return classes;
	}

}
