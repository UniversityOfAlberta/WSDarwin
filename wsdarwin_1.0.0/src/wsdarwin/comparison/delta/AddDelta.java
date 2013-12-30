package wsdarwin.comparison.delta;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class AddDelta extends Delta {
	
	public AddDelta(WSElement source, WSElement target) {
		super(source, target);
		addChildren();
	}

	private void addChildren() {
		for(WSElement child : target.getChildren().values()) {
			AddDelta childDelta = new AddDelta(null, child);
			childDelta.setParent(this);
			this.deltas.add(childDelta);
		}
		
	}

	@Override
	public void printDelta(int level) {
		String deltaText = DeltaUtil.indent(level);
		deltaText += "Add\t"+this.getTarget().getClass().getSimpleName()+"\t";
		deltaText += "\t -> "+this.getTarget().toString();
		System.out.println(deltaText);
		if(!this.getDeltas().isEmpty()) {
			level++;
			for(Delta child : this.getDeltas()) {
				child.printDelta(level);
			}
		}
	}
	
	public String toString() {
		return "Add\t\t -> "+this.getTarget().toString();
	}

	@Override
	public Element createXMLElement(Document document, Element parent) {
		Element deltaElement = document.createElement("AddDelta");
		deltaElement.setAttribute("type", target.getClass().getSimpleName());
		deltaElement.setAttribute("target", target.toString());
		for(Delta delta : deltas) {
			delta.createXMLElement(document, deltaElement);
		}
		if (parent != null) {
			parent.appendChild(deltaElement);
		}
		return deltaElement;
	}
	
	

}
