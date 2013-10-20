package wsdarwin.comparison.delta;

import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class MatchDelta extends Delta {

	public MatchDelta(WSElement source, WSElement target) {
		super(source, target);
	}

	@Override
	public void printDelta(int level) {
		String deltaText = DeltaUtil.indent(level);
		deltaText += "Match\t"+this.getSource().getClass().getSimpleName()+"\t";
		deltaText += this.getSource().toString()+" -> "+this.getTarget().toString();
		System.out.println(deltaText);
		if(!this.getDeltas().isEmpty()) {
			level++;
			for(Delta child : this.getDeltas()) {
				child.printDelta(level);
			}
		}
	}
	
	public String toString() {
		return "Match\t"+this.getSource().toString()+" -> "+this.getTarget().toString();
	}
}
