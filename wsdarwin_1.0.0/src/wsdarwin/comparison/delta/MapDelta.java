package wsdarwin.comparison.delta;

import java.util.ArrayList;
import java.util.HashSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;
import wsdarwin.wadlgenerator.model.WADLElement;


public class MapDelta extends Delta {
	
	private HashSet<WADLElement> sources;
	private HashSet<WADLElement> targets;
	private double mapScore;
	

	public MapDelta(WADLElement source, WADLElement target) {
		this.sources = new HashSet<WADLElement>();
		this.targets = new HashSet<WADLElement>();
		this.sources.add(source);
		this.targets.add(target);
		this.mapScore = 0.0;
		// TODO Auto-generated constructor stub
	}
	
	

	public double getMapScore() {
		return mapScore;
	}



	public void addMapScore(double mapScore) {
		this.mapScore += mapScore;
	}



	public HashSet<WADLElement> getSources() {
		return sources;
	}
	
	public HashSet<WADLElement> getTargets() {
		return targets;
	}
	
	@Override
	public void printDelta(int level) {
		String deltaText = DeltaUtil.indent(level);
		deltaText += "Map\t"+sources.iterator().next().getClass().getSimpleName()+"\t";
		deltaText += sources.iterator().next().toString()+" -> "+targets.iterator().next().toString()+"\n";
		System.out.println(deltaText);
		if(!this.getDeltas().isEmpty()) {
			level++;
			for(Delta child : this.getDeltas()) {
				child.printDelta(level);
			}
		}
	}
	
	public String toString() {
		return sources.getClass().getSimpleName()+": "+sources.toString()+"->"+targets.toString()+"{"+mapScore+"}";
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapDelta other = (MapDelta) obj;
		if (sources == null) {
			if (other.sources != null)
				return false;
		} else if (targets == null) {
			if(other.targets != null)
				return false;
		} else if (!sources.equals(other.sources) || !targets.equals(other.targets))
			return false;
		return true;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sources == null) ? 0 : sources.hashCode()) + ((targets == null) ? 0 : targets.hashCode());
		return result;
	}



	@Override
	public Element createXMLElement(Document document, Element parent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
