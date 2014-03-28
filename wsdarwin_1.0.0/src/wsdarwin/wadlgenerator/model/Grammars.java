package wsdarwin.wadlgenerator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import wsdarwin.comparison.delta.Delta;
import wsdarwin.model.WSElement;

public class Grammars implements WADLElement {

	//	enum includeType {INLINE, REFERENCE};
	/* Grammars can be included inline or by reference. In our case 
	 * we will only use them by reference, therefore the includeType 
	 * is not needed. */
	private HashSet<String> includedGrammars;		// Set of "href"-values of XSD-files


	public Grammars() {
		includedGrammars = new HashSet<String>();
	}

	public String getIdentifier() {
		return includedGrammars.toString();
	}

	public HashSet<String> getIncludedGrammars() {
		return includedGrammars;
	}

	public void addIncludedGrammar(String include) {
		includedGrammars.add(include);
	}	

	public void addAllIncludedGrammars(Set<String> includes) {
		includedGrammars.addAll(includes);
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
		HashSet<String> mapped = new HashSet<String>();
		HashSet<String> added = new HashSet<String>();
		HashSet<String> deleted = new HashSet<String>();
		mapByID(element, mapped, added, deleted);
		mapByStructure(element, mapped, added, deleted);

		return false;
	}
	
	//Does not need to be implemented as include do not have child nodes and thus no structure
	private void mapByStructure(WADLElement element, HashSet<String> mapped,
			HashSet<String> added, HashSet<String> deleted) {
		
	}

	private void mapByID(WADLElement element, HashSet<String> mapped, HashSet<String> added, HashSet<String> deleted) {
		if(element instanceof Grammars){
			Grammars grammars2 = (Grammars)element;
			for(String href2 : grammars2.includedGrammars) {
				if(this.getIncludedGrammars().contains(href2)){
					mapped.add(href2);
				}else{
					added.add(href2);
				}
			}

			for(String href2 : this.includedGrammars) {
				if(!mapped.contains(href2)){
					deleted.add(href2);
				}
			}
		}
		
	}



}
