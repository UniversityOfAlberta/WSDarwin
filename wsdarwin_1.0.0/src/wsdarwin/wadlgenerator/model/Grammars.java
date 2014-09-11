package wsdarwin.wadlgenerator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import wsdarwin.comparison.delta.Delta;
import wsdarwin.model.WSElement;
import wsdarwin.wadlgenerator.model.xsd.XSDFile;

public class Grammars implements WADLElement {

	//	enum includeType {INLINE, REFERENCE};
	/* Grammars can be included inline or by reference. In our case 
	 * we will only use them by reference, therefore the includeType 
	 * is not needed. */
	private HashMap<String, XSDFile> includedGrammars;		// Set of "href"-values of XSD-files


	public Grammars() {
		includedGrammars = new HashMap<String, XSDFile>();
	}

	public String getIdentifier() {
		return includedGrammars.toString();
	}

	public HashMap<String, XSDFile> getIncludedGrammars() {
		return includedGrammars;
	}

	public void addIncludedGrammar(String responseElement, XSDFile schema) {
		includedGrammars.put(responseElement, schema);
	}	

	public void addAllIncludedGrammars(Map<String, XSDFile> includes) {
		includedGrammars.putAll(includes);
	}

	/*public Grammars diff(Grammars grammars) {
		Grammars grammarsAdded = new Grammars();

		for(String str : grammars.getIncludedGrammars()) {
			if(!includedGrammars.contains(str)) {
				grammarsAdded.addIncludedGrammar(str);
			}	
		}

		return grammarsAdded;
	}*/

	public String toString() {
		//		return "Grammars";
		return "[Grammars] "+includedGrammars.toString();
	}

	public boolean equals(Object o) {
		return ((Grammars)o).includedGrammars.equals(this.includedGrammars);
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


	/*@Override
	public Delta compare(WADLElement element) {
		Grammars grammars = null;
		if(element instanceof Grammars) {
			grammars = (Grammars)element;
		} else {
			return null;
		}

		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();

		// leave it out for the moment (MF, 06/29/2012)

		return elementDelta;
	}*/

	@Override
	public boolean mapElement(WADLElement element) {
		System.out.println("MAP ELEMENT GRAMMARS");
		HashSet<XSDFile> mapped = new HashSet<XSDFile>();
		HashSet<XSDFile> added = new HashSet<XSDFile>();
		HashSet<XSDFile> deleted = new HashSet<XSDFile>();
		mapByID(element, mapped, added, deleted);
		mapByStructure(element, mapped, added, deleted);

		return false;
	}
	
	//Does not need to be implemented as include do not have child nodes and thus no structure
	private void mapByStructure(WADLElement element, HashSet<XSDFile> mapped,
			HashSet<XSDFile> added, HashSet<XSDFile> deleted) {
		
	}

	private void mapByID(WADLElement element, HashSet<XSDFile> mapped, HashSet<XSDFile> added, HashSet<XSDFile> deleted) {
		if(element instanceof Grammars){
			Grammars grammars2 = (Grammars)element;
			for(XSDFile schema : grammars2.includedGrammars.values()) {
				if(this.getIncludedGrammars().containsValue(schema)){
					mapped.add(schema);
				}else{
					added.add(schema);
				}
			}

			for(XSDFile schema : this.includedGrammars.values()) {
				if(!mapped.contains(schema)){
					deleted.add(schema);
				}
			}
		}
		
	}



}
