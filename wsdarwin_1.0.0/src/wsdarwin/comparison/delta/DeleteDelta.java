package wsdarwin.comparison.delta;

import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class DeleteDelta extends Delta {
	
	public DeleteDelta(WSElement source, WSElement target) {
		super(source, target);
	}

	@Override
	public void printDelta(int level) {
		String deltaText = DeltaUtil.indent(level);
		deltaText += "Delete\t";
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

}
