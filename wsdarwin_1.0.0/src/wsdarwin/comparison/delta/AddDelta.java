package wsdarwin.comparison.delta;

import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class AddDelta extends Delta {
	
	public AddDelta(WSElement source, WSElement target) {
		super(source, target);
	}

	@Override
	public void printDelta(int level) {
		String deltaText = DeltaUtil.indent(level);
		deltaText += "Add\t";
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

}
