package wsdarwin.clientadaptation;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import wsdarwin.comparison.delta.Delta;


public class ClientAdaptationRefactoringDescriptor extends
		RefactoringDescriptor {

	public static final String REFACTORING_ID = "org.eclipse.client.adaptation";
	private CompilationUnit oldCompilationUnit;
	private CompilationUnit newCompilationUnit;
	private TypeDeclaration oldTypeDeclaration;
	private TypeDeclaration newTypeDeclaration;
	private Map<MethodDeclaration, Delta> changedMethods;

	protected ClientAdaptationRefactoringDescriptor(String project,String description, String comment,
			CompilationUnit oldCompilationUnit, CompilationUnit newCompilationUnit, TypeDeclaration oldTypeDeclaration, TypeDeclaration newTypeDeclaration,
			Map<MethodDeclaration, Delta> changedMethods) {
		super(REFACTORING_ID, project, description, comment, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		this.oldCompilationUnit = oldCompilationUnit;
		this.newCompilationUnit = newCompilationUnit;
		this.oldTypeDeclaration = oldTypeDeclaration;
		this.newTypeDeclaration = newTypeDeclaration;
		this.changedMethods = changedMethods;
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status)
			throws CoreException {
		Refactoring refactoring = new ClientAdaptationRefactoring(oldCompilationUnit, newCompilationUnit, oldTypeDeclaration, newTypeDeclaration, changedMethods);
		RefactoringStatus refStatus = new RefactoringStatus();
		status.merge(refStatus);
		return refactoring;
	}

}
