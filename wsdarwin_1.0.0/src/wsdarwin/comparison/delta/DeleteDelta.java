package wsdarwin.comparison.delta;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class DeleteDelta extends Delta {
	
	public DeleteDelta(WSElement source, WSElement target) {
		super(source, target);
		deleteChildren();
	}

	private void deleteChildren() {
		for(WSElement child : source.getChildren().values()) {
			DeleteDelta childDelta = new DeleteDelta(child, null);
			childDelta.setParent(this);
			this.deltas.add(childDelta);
		}
	}

	@Override
	public void printDelta(int level) {
		String deltaText = DeltaUtil.indent(level);
		deltaText += "Delete\t"+this.getSource().getClass().getSimpleName()+"\t";
		deltaText += this.getSource().toString()+" -> \t";
		System.out.println(deltaText);
		if(!this.getDeltas().isEmpty()) {
			level++;
			for(Delta child : this.getDeltas()) {
				child.printDelta(level);
			}
		}
	}
	
	public String toString() {
		return "Delete\t"+this.getSource().toString()+" -> \t";
	}

	@Override
	public Element createXMLElement(Document document, Element parent) {
		Element deltaElement = document.createElement("DeleteDelta");
		deltaElement.setAttribute("type", source.getClass().getSimpleName());
		deltaElement.setAttribute("source", source.toString());
		for(Delta delta : deltas) {
			delta.createXMLElement(document, deltaElement);
		}
		if (parent != null) {
			parent.appendChild(deltaElement);
		}
		return deltaElement;
	}

}
