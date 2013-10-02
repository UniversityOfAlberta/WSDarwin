package wsdarwin.comparison.delta;

import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;


public class ChangeDelta extends Delta{
	
	private String changedAttribute;
	private String oldValue;
	private String newValue;
	
	public ChangeDelta(WSElement source, WSElement target, String changedAttribute, String oldValue, String newValue) {
		super(source, target);
		this.changedAttribute = changedAttribute;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public String getChangedAttribute() {
		return changedAttribute;
	}

	public String getOldValue() {
		return oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	@Override
	public void printDelta(int level) {
		String deltaText = DeltaUtil.indent(level);
		deltaText += "Change\t";
		deltaText += this.getSource().toString()+" -> "+this.getTarget().toString();
		if(!this.getChangedAttribute().equals("")) {
			deltaText  += "\t@" +this.getChangedAttribute()+"\t"+this.getOldValue()+" -> "+this.getNewValue();
		}
		System.out.println(deltaText);
		if(!this.getDeltas().isEmpty()) {
			level++;
			for(Delta child : this.getDeltas()) {
				child.printDelta(level);
			}
		}
		
	}
	
	public String toString() {
		String deltaText =  "Change\t"+this.getSource().toString()+" -> "+this.getTarget().toString();
		if(!this.getChangedAttribute().equals("")) {
			deltaText  += "\t@" +this.getChangedAttribute()+"\t"+this.getOldValue()+" -> "+this.getNewValue();
		}
		return deltaText;
	}

}
