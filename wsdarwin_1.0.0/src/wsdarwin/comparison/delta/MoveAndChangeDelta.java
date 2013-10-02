package wsdarwin.comparison.delta;

import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;

public class MoveAndChangeDelta extends Delta {
	
	private ChangeDelta change;
	private MoveDelta move;

	public MoveAndChangeDelta(WSElement source, WSElement target, WSElement oldParent, WSElement newParent, String changedAttribute, String oldValue, String newValue) {
		super(source, target);
		change = new ChangeDelta(source, target, changedAttribute, oldValue, newValue);
		move = new MoveDelta(source, target, oldParent, newParent);
	}

	@Override
	public void printDelta(int level) {
		String deltaText = DeltaUtil.indent(level);
		deltaText += "MoveAndChange\t";
		deltaText += this.getSource().toString() + "\t" + move.getOldParent().toString() + " ->" + move.getNewParent().toString()+"\t@"+change.getChangedAttribute()+"\t"+change.getOldValue()+" -> "+change.getNewValue();
		System.out.println(deltaText);
		if (!this.getDeltas().isEmpty()) {
			level++;
			for (Delta child : this.getDeltas()) {
				child.printDelta(level);
			}
		}

	}
	
	public String toString() {
		return "MoveAndChange\t"+this.getSource().toString() + "\t" + move.getOldParent().toString() + " ->" + move.getNewParent().toString()+"\t@"+change.getChangedAttribute()+"\t"+change.getOldValue()+" -> "+change.getNewValue();
	}

}
