package wsdarwin.util;

import java.util.ArrayList;
import java.util.HashMap;

import wsdarwin.comparison.delta.*;
import wsdarwin.model.*;


public class DeltaUtil {

	public static boolean containsOnlyMatchDeltas(ArrayList<Delta> deltas) {
		for (Delta delta : deltas) {
			if (!(delta instanceof MatchDelta)) {
				return false;
			}
		}
		return true;
	}

	public static String indent(int level) {
		String indent = "";
		for (int i = 0; i < level; i++) {
			indent += "\t";
		}
		return indent;
	}

	public static void findMoveDeltas(Delta delta) {
		ArrayList<AddDelta> addDeltas = delta.getAddDeltas();
		ArrayList<DeleteDelta> deleteDeltas = delta.getDeleteDeltas();
		HashMap<DeleteDelta, AddDelta> moveDeltaMap = new HashMap<DeleteDelta, AddDelta>();
		HashMap<DeleteDelta, AddDelta> moveAndRenameDeltaMap = new HashMap<DeleteDelta, AddDelta>();
		HashMap<DeleteDelta, AddDelta> moveAndChangeDeltaMap = new HashMap<DeleteDelta, AddDelta>();
		ArrayList<Integer> indices = new ArrayList<Integer>();
		for (DeleteDelta deleteDelta : deleteDeltas) {
			for (AddDelta addDelta : addDeltas) {
				WSElement commonAncestor = hasCommonAncestor(
						deleteDelta.getParent(), addDelta.getParent());
				if (deleteDelta.getSource().equals(addDelta.getTarget())
						&& commonAncestor != null) {
					if (!indices.contains(addDeltas.indexOf(addDelta))) {
						moveDeltaMap.put(deleteDelta, addDelta);
						indices.add(addDeltas.indexOf(addDelta));
						break;
					}
				}
			}
		}
		deleteDeltas.removeAll(moveDeltaMap.keySet());
		addDeltas.removeAll(moveDeltaMap.values());
		indices = new ArrayList<Integer>();
		for (DeleteDelta deleteDelta : deleteDeltas) {
			for (AddDelta addDelta : addDeltas) {
				WSElement commonAncestor = hasCommonAncestor(
						deleteDelta.getParent(), addDelta.getParent());
				if (deleteDelta.getSource().equalsByName(addDelta.getTarget())
						&& commonAncestor != null) {
					if (!indices.contains(addDeltas.indexOf(addDelta))) {
						moveAndChangeDeltaMap.put(deleteDelta, addDelta);
						indices.add(addDeltas.indexOf(addDelta));
						break;
					}
				}
			}
		}
		deleteDeltas.removeAll(moveAndChangeDeltaMap.keySet());
		addDeltas.removeAll(moveAndChangeDeltaMap.values());
		indices = new ArrayList<Integer>();
		for (DeleteDelta deleteDelta : deleteDeltas) {
			for (AddDelta addDelta : addDeltas) {
				WSElement commonAncestor = hasCommonAncestor(
						deleteDelta.getParent(), addDelta.getParent());
				if (deleteDelta.getSource().equalsAfterRename(
						addDelta.getTarget())
						&& commonAncestor != null) {
					if (!indices.contains(addDeltas.indexOf(addDelta))) {
						moveAndRenameDeltaMap.put(deleteDelta, addDelta);
						indices.add(addDeltas.indexOf(addDelta));
						break;
					}
				}
			}
		}
		if (!moveDeltaMap.isEmpty()) {
			for (DeleteDelta deleteDelta : moveDeltaMap.keySet()) {
				Delta deleteParent = deleteDelta.getParent();
				Delta addParent = moveDeltaMap.get(deleteDelta).getParent();
				MoveDelta moveDelta = new MoveDelta(deleteDelta.getSource(),
						moveDeltaMap.get(deleteDelta).getTarget(),
						deleteParent.getSource(), addParent.getTarget());
				deleteParent.getDeltas().set(
						deleteParent.getDeltas().indexOf(deleteDelta),
						moveDelta);
				addParent.getDeltas().remove(addDeltas);
			}
		}
		if (!moveAndChangeDeltaMap.isEmpty()) {
			for (DeleteDelta deleteDelta : moveAndChangeDeltaMap.keySet()) {
				Delta deleteParent = deleteDelta.getParent();
				Delta addParent = moveAndChangeDeltaMap.get(deleteDelta)
						.getParent();
				Delta aDelta = deleteDelta.getSource().diff(
						moveAndChangeDeltaMap.get(deleteDelta).getTarget());
				ChangeDelta change = null;
				if (aDelta instanceof ChangeDelta) {
					change = (ChangeDelta) aDelta;
				}
				MoveAndChangeDelta moveAndChangeDelta = new MoveAndChangeDelta(
						deleteDelta.getSource(), moveAndChangeDeltaMap.get(
								deleteDelta).getTarget(),
						deleteParent.getSource(), addParent.getTarget(),
						change.getChangedAttribute(), change.getOldValue(),
						change.getNewValue());
				deleteParent.getDeltas().set(
						deleteParent.getDeltas().indexOf(deleteDelta),
						moveAndChangeDelta);
				addParent.getDeltas().remove(addDeltas);
			}
		}
		if (!moveAndRenameDeltaMap.isEmpty()) {
			for (DeleteDelta deleteDelta : moveAndRenameDeltaMap.keySet()) {
				Delta deleteParent = deleteDelta.getParent();
				Delta addParent = moveAndRenameDeltaMap.get(deleteDelta)
						.getParent();
				Delta aDelta = deleteDelta.getSource().diff(
						moveAndRenameDeltaMap.get(deleteDelta).getTarget());
				ChangeDelta change = null;
				if (aDelta instanceof ChangeDelta) {
					change = (ChangeDelta) aDelta;
				}
				MoveAndChangeDelta moveAndChangeDelta = new MoveAndChangeDelta(
						deleteDelta.getSource(), moveAndRenameDeltaMap.get(
								deleteDelta).getTarget(),
						deleteParent.getSource(), addParent.getTarget(),
						change.getChangedAttribute(), change.getOldValue(),
						change.getNewValue());
				deleteParent.getDeltas().set(
						deleteParent.getDeltas().indexOf(deleteDelta),
						moveAndChangeDelta);
				addParent.getDeltas().remove(addDeltas);
			}
		}
		if (!moveAndChangeDeltaMap.isEmpty()) {
			System.out.println("MoveAndChange Found!");
		}
		/*
		 * else { System.out.println("No moves found!"); }
		 */
	}

	private static WSElement hasCommonAncestor(Delta delta1, Delta delta2) {
		WSElement commonAncestor = null;
		if (delta1.getSource() != null && delta2.getSource() != null) {
			if (delta1.getSource().equals(delta2.getSource())) {
				commonAncestor = delta1.getSource();
			} else {
				if (delta2.getParent() != null
						&& !(delta2.getParent().getSource() instanceof IService)) {
					commonAncestor = hasCommonAncestor(delta1,
							delta2.getParent());
				}
			}
		}
		if (commonAncestor == null) {
			if (delta1.getParent() != null
					&& !(delta1.getParent().getSource() instanceof IService)) {
				commonAncestor = hasCommonAncestor(delta1.getParent(), delta2);
			}
		}
		return commonAncestor;
	}

}
