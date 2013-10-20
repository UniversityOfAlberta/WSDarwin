package wsdarwin.comparison.delta;

import java.util.ArrayList;
import java.util.List;

import wsdarwin.model.WSElement;
import wsdarwin.util.DeltaUtil;


public class ChangeDelta extends Delta{
	
	private List<String> changedAttribute;
	private List<String> oldValue;
	private List<String> newValue;
	
	public ChangeDelta(WSElement source, WSElement target, String changedAttribute, String oldValue, String newValue) {
		super(source, target);
		this.changedAttribute = new ArrayList<String>();
		this.changedAttribute.add(changedAttribute);
		
		this.oldValue = new ArrayList<String>();
		this.oldValue.add(oldValue);
		this.newValue = new ArrayList<String>();
		this.newValue.add(newValue);
	}

	public ChangeDelta(WSElement source, WSElement target,
			List<String> changedAttribute, List<String> oldValue,
			List<String> newValue) {
		super(source, target);
		this.changedAttribute = changedAttribute;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}



	public List<String> getChangedAttribute() {
		return changedAttribute;
	}

	public List<String> getOldValue() {
		return oldValue;
	}

	public List<String> getNewValue() {
		return newValue;
	}
	
	public void addChangedAttribute(String changedAttribute, String oldValue, String newValue) {
		this.changedAttribute.add(changedAttribute);
		this.oldValue.add(oldValue);
		this.newValue.add(newValue);
	}

	@Override
	public void printDelta(int level) {
		String deltaText = DeltaUtil.indent(level);
		deltaText += "Change\t"+this.getSource().getClass().getSimpleName()+"\t";
		deltaText += this.getSource().toString()+" -> "+this.getTarget().toString();
		if(hasChangedAttribute()) {
			for (int i = 0; i < this.getChangedAttribute().size(); i++) {
				deltaText += "\t@" + this.getChangedAttribute().get(i) + "\t"
						+ this.getOldValue().get(i) + " -> " + this.getNewValue().get(i);
			}
		}
		System.out.println(deltaText);
		if(!this.getDeltas().isEmpty()) {
			level++;
			for(Delta child : this.getDeltas()) {
				child.printDelta(level);
			}
		}
		
	}
	
	private boolean hasChangedAttribute() {
		for(String attribute : changedAttribute) {
			if(!attribute.equals("")) {
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		String deltaText =  "Change\t"+this.getSource().toString()+" -> "+this.getTarget().toString();
		if(!this.getChangedAttribute().equals("")) {
			deltaText  += "\t@" +this.getChangedAttribute()+"\t"+this.getOldValue()+" -> "+this.getNewValue();
		}
		return deltaText;
	}

}
