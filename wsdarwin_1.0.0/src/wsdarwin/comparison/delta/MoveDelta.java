package wsdarwin.comparison.delta;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class MoveDelta extends Delta {
	
	private WSElement oldParent;
	private WSElement newParent;

	public MoveDelta(WSElement source, WSElement target,
			WSElement oldParent, WSElement newParent) {
		super(source, target);
		this.oldParent = oldParent;
		this.newParent = newParent;
	}

	public WSElement getOldParent() {
		return oldParent;
	}

	public WSElement getNewParent() {
		return newParent;
	}

	@Override
	public void printDelta(int level) {
		String deltaText = DeltaUtil.indent(level);
		deltaText += "Move\t"+this.getSource().getClass().getSimpleName()+"\t";
		deltaText += this.getSource().toString() + "\t" + oldParent.toString() + " ->" + newParent.toString();
		System.out.println(deltaText);
		if (!this.getDeltas().isEmpty()) {
			level++;
			for (Delta child : this.getDeltas()) {
				child.printDelta(level);
			}
		}

	}
	
	public String toString() {
		return "Move\t"+this.getSource().toString() + "\t" + oldParent.toString() + " ->" + newParent.toString();
	}

	@Override
	public Element createXMLElement(Document document, Element parent) {
		Element deltaElement = document.createElement("MoveDelta");
		deltaElement.setAttribute("type", source.getClass().getSimpleName());
		deltaElement.setAttribute("source", source.toString());
		deltaElement.setAttribute("target", target.toString());
		deltaElement.setAttribute("oldParent", oldParent.toString());
		deltaElement.setAttribute("newParent", newParent.toString());
		for(Delta delta : deltas) {
			delta.createXMLElement(document, deltaElement);
		}
		if (parent != null) {
			parent.appendChild(deltaElement);
		}
		return deltaElement;
	}

}
