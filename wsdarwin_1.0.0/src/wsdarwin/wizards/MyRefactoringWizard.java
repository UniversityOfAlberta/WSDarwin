package wsdarwin.wizards;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class MyRefactoringWizard extends RefactoringWizard {
	
	

	public MyRefactoringWizard(Refactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE | NO_BACK_BUTTON_ON_STATUS_DIALOG);
		setDefaultPageTitle(refactoring.getName());
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void addUserInputPages() {
		// TODO Auto-generated method stub

	}

}
