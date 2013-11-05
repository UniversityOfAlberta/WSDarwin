package wsdarwin.clientadaptation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import wsdarwin.clientadaptation.ast.util.ASTParserUtility;
import wsdarwin.clientadaptation.ast.util.TypeVisitor;
import wsdarwin.comparison.delta.Delta;
import wsdarwin.model.ComplexType;
import wsdarwin.model.IType;
import wsdarwin.model.Operation;
import wsdarwin.model.PrimitiveType;
import wsdarwin.model.WSElement;
import wsdarwin.util.HumaniseCamelCase;

public class ClientAdaptationRefactoring extends Refactoring {

	public static final String NEW_STUB_REFERENCE_NAME = "newStub";
	public static final String NEW_REQUEST_NAME = "newRequest";
	public static final String OLD_RESPONSE_NAME = "oldResponse";
	public static final String NEW_RESPONSE_NAME = "newResponse";

	private CompilationUnit oldCompilationUnit;
	private CompilationUnit newCompilationUnit;
	private TypeDeclaration oldTypeDeclaration;
	private TypeDeclaration newTypeDeclaration;
	private Map<MethodDeclaration, Delta> changedMethods;
	private CompilationUnitChange compilationUnitChange;
	private Set<ITypeBinding> requiredImportDeclarationsInExtractedClass;

	public ClientAdaptationRefactoring(CompilationUnit oldCompilationUnit,
			CompilationUnit newCompilationUnit,
			TypeDeclaration oldTypeDeclaration,
			TypeDeclaration newTypeDeclaration,
			Map<MethodDeclaration, Delta> changedMethods) {
		this.oldCompilationUnit = oldCompilationUnit;
		this.newCompilationUnit = newCompilationUnit;
		this.oldTypeDeclaration = oldTypeDeclaration;
		this.newTypeDeclaration = newTypeDeclaration;
		this.changedMethods = changedMethods;
		MultiTextEdit oldMultiTextEdit = new MultiTextEdit();
		this.compilationUnitChange = new CompilationUnitChange("",
				(ICompilationUnit) oldCompilationUnit.getJavaElement());
		compilationUnitChange.setEdit(oldMultiTextEdit);
		this.requiredImportDeclarationsInExtractedClass = new LinkedHashSet<ITypeBinding>();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		final RefactoringStatus status = new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 2);
			apply();
		} finally {
			pm.done();
		}
		return status;
	}

	private void apply() {
		Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
		TypeVisitor typeVisitor = new TypeVisitor();
		for (MethodDeclaration method : changedMethods.keySet()) {
			method.accept(typeVisitor);
			for (ITypeBinding typeBinding : typeVisitor.getTypeBindings()) {
				typeBindings.add(typeBinding);
			}
		}
		getSimpleTypeBindings(typeBindings,
				requiredImportDeclarationsInExtractedClass);

		addNewStubReferenceToOldStub();
		for (int i = 0; i < oldTypeDeclaration.getMethods().length; i++) {
			if (oldTypeDeclaration.getMethods()[i].isConstructor()) {
				instantiateNewStub(oldTypeDeclaration.getMethods()[i]);
			}
		}
		for (MethodDeclaration method : changedMethods.keySet()) {
			deleteChangedMethodBody(method);

			prepareInput(method, changedMethods.get(method));
			invokeNewMethod(method, changedMethods.get(method));
			prepareOutput(method, changedMethods.get(method));
			returnOuput(method);
		}

	}

	private void returnOuput(MethodDeclaration method) {
		ASTRewrite sourceRewriter = ASTRewrite.create(oldTypeDeclaration
				.getAST());
		AST contextAST = method.getAST();

		ReturnStatement returnStatement = contextAST.newReturnStatement();
		sourceRewriter.set(returnStatement,
				ReturnStatement.EXPRESSION_PROPERTY,
				contextAST.newSimpleName(OLD_RESPONSE_NAME), null);

		ListRewrite methodBody = sourceRewriter.getListRewrite(
				method.getBody(), Block.STATEMENTS_PROPERTY);
		methodBody.insertLast(returnStatement, null);

		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			compilationUnitChange.getEdit().addChild(sourceEdit);
			compilationUnitChange.addTextEditGroup(new TextEditGroup(
					"Return output for method "
							+ method.getName().getIdentifier(),
					new TextEdit[] { sourceEdit }));
		} catch (JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}
	}

	private void invokeNewMethod(MethodDeclaration method, Delta delta) {
		ASTRewrite sourceRewriter = ASTRewrite.create(oldTypeDeclaration
				.getAST());
		AST contextAST = method.getAST();
		Operation targetOperation = (Operation) delta.getTarget();

		MethodDeclaration newMethod = null;
		for (int i = 0; i < newTypeDeclaration.getMethods().length; i++) {
			if (newTypeDeclaration.getMethods()[i].getName().getIdentifier()
					.equalsIgnoreCase(targetOperation.getName())) {
				newMethod = newTypeDeclaration.getMethods()[i];
			}
		}

		String qualifiedNameOfNewReturnType = newMethod.getReturnType2()
				.resolveBinding().getQualifiedName();

		String qualifiedNameOfOldReturnType = method.getReturnType2()
				.resolveBinding().getQualifiedName();

		Delta responseDelta = delta.getDeltaByType(method.getReturnType2()
				.resolveBinding().getName());
		IType responseType = null;
		if (responseDelta.getSource() != null) {
			responseType = (IType) responseDelta.getSource();
		} else {
			responseType = (IType) responseDelta.getTarget();
		}

		MethodInvocation newMethodInvocation = contextAST.newMethodInvocation();
		sourceRewriter.set(newMethodInvocation,
				MethodInvocation.EXPRESSION_PROPERTY,
				contextAST.newSimpleName(NEW_STUB_REFERENCE_NAME), null);
		sourceRewriter.set(newMethodInvocation, MethodInvocation.NAME_PROPERTY,
				newMethod.getName(), null);
		ListRewrite arguments = sourceRewriter.getListRewrite(
				newMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		arguments.insertLast(contextAST.newSimpleName(NEW_REQUEST_NAME), null);

		VariableDeclarationFragment fragment = contextAST
				.newVariableDeclarationFragment();
		sourceRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY,
				contextAST.newSimpleName(NEW_RESPONSE_NAME), null);
		sourceRewriter.set(fragment,
				VariableDeclarationFragment.INITIALIZER_PROPERTY,
				newMethodInvocation, null);

		VariableDeclarationStatement declaration = contextAST
				.newVariableDeclarationStatement(fragment);
		sourceRewriter.set(declaration,
				VariableDeclarationStatement.TYPE_PROPERTY,
				contextAST.newName(qualifiedNameOfNewReturnType), null);

		// copyValues(sourceRewriter, responseDelta, contextAST, method,
		// findTypeDeclaration((ComplexType)responseType, newTypeDeclaration),
		// contextAST.newMethodInvocation());

		ListRewrite methodBody = sourceRewriter.getListRewrite(
				method.getBody(), Block.STATEMENTS_PROPERTY);
		methodBody.insertLast(declaration, null);

		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			compilationUnitChange.getEdit().addChild(sourceEdit);
			compilationUnitChange.addTextEditGroup(new TextEditGroup(
					"Invoke new method " + newMethod.getName().getIdentifier(),
					new TextEdit[] { sourceEdit }));
		} catch (JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}

	}

	private SimpleName findTargetMethodName(TypeDeclaration typeDeclaration,
			String name) {
		for (MethodDeclaration method : typeDeclaration.getMethods()) {
			if (method.getName().getIdentifier().equalsIgnoreCase(name)) {
				return method.getName();
			}
		}
		return null;
	}

	private void createInstance(MethodDeclaration method,
			ASTRewrite sourceRewriter, AST contextAST,
			String typeQualifiedName, String variableName, boolean isArray) {

		VariableDeclarationFragment variableDeclaration = contextAST
				.newVariableDeclarationFragment();
		sourceRewriter.set(variableDeclaration,
				VariableDeclarationFragment.NAME_PROPERTY,
				contextAST.newSimpleName(variableName), null);
		VariableDeclarationStatement variableDeclarationStatement = contextAST
				.newVariableDeclarationStatement(variableDeclaration);

		if (!isArray) {
			// TODO: Check if the typeQualifiedName is an array
			ClassInstanceCreation classInstanceCreation = contextAST
					.newClassInstanceCreation();
			sourceRewriter.set(classInstanceCreation,
					ClassInstanceCreation.TYPE_PROPERTY,
					contextAST.newName(typeQualifiedName), null);

			sourceRewriter.set(variableDeclaration,
					VariableDeclarationFragment.INITIALIZER_PROPERTY,
					classInstanceCreation, null);

			sourceRewriter.set(variableDeclarationStatement,
					VariableDeclarationStatement.TYPE_PROPERTY,
					contextAST.newName(typeQualifiedName), null);

		} else {
			ArrayCreation arrayCreation = contextAST.newArrayCreation();
			ListRewrite dimensions = sourceRewriter.getListRewrite(
					arrayCreation, ArrayCreation.DIMENSIONS_PROPERTY);
			dimensions.insertLast(contextAST.newNumberLiteral("1"), null);

			Type componentType = contextAST.newSimpleType(contextAST
					.newName(typeQualifiedName));

			sourceRewriter.set(arrayCreation, ArrayCreation.TYPE_PROPERTY,
					contextAST.newArrayType(componentType, 1), null);

			sourceRewriter.set(variableDeclaration,
					VariableDeclarationFragment.INITIALIZER_PROPERTY,
					arrayCreation, null);

			sourceRewriter.set(variableDeclarationStatement,
					VariableDeclarationStatement.TYPE_PROPERTY,
					contextAST.newArrayType(componentType), null);
		}
		ListRewrite methodBody = sourceRewriter.getListRewrite(
				method.getBody(), Block.STATEMENTS_PROPERTY);
		methodBody.insertLast(variableDeclarationStatement, null);
	}

	private void prepareInput(MethodDeclaration method, Delta delta) {
		ASTRewrite sourceRewriter = ASTRewrite.create(oldTypeDeclaration
				.getAST());
		AST contextAST = method.getAST();

		copyValues(
				sourceRewriter,
				delta.getDeltas().get(0),
				contextAST,
				method,
				findTypeDeclaration(((ComplexType) delta.getDeltas().get(0)
						.getSource()).getName(), oldTypeDeclaration),
				findTypeDeclaration(((ComplexType) delta.getDeltas().get(0)
						.getTarget()).getName(), newTypeDeclaration),
				contextAST.newMethodInvocation(), true);

		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			compilationUnitChange.getEdit().addChild(sourceEdit);
			compilationUnitChange.addTextEditGroup(new TextEditGroup(
					"Prepare input for method "
							+ method.getName().getIdentifier(),
					new TextEdit[] { sourceEdit }));
		} catch (JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}

	}

	private void prepareOutput(MethodDeclaration method, Delta delta) {
		ASTRewrite sourceRewriter = ASTRewrite.create(oldTypeDeclaration
				.getAST());
		AST contextAST = method.getAST();

		copyValues(
				sourceRewriter,
				delta.getDeltas().get(1),
				contextAST,
				method,
				findTypeDeclaration(((ComplexType) delta.getDeltas().get(1)
						.getSource()).getName(), oldTypeDeclaration),
				findTypeDeclaration(((ComplexType) delta.getDeltas().get(1)
						.getTarget()).getName(), newTypeDeclaration),
				contextAST.newMethodInvocation(), false);

		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			compilationUnitChange.getEdit().addChild(sourceEdit);
			compilationUnitChange.addTextEditGroup(new TextEditGroup(
					"Prepare output for method "
							+ method.getName().getIdentifier(),
					new TextEdit[] { sourceEdit }));
		} catch (JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}

	}

	private void copyValues(ASTRewrite sourceRewriter, Delta delta,
			AST contextAST, MethodDeclaration method,
			TypeDeclaration oldTypeDec, TypeDeclaration newTypeDec,
			Expression getterMethodInvocation, boolean isInput) {
		WSElement source = delta.getSource();
		WSElement target = delta.getTarget();
		String oldInputName = "";
		if (isInput) {
			oldInputName = ((SingleVariableDeclaration) method.parameters()
					.get(0)).getName().getIdentifier();
		} else {
			oldInputName = NEW_RESPONSE_NAME;
		}
		if (source != null) {
			if (target != null) {
				if (source instanceof PrimitiveType) {
					PrimitiveType type = (PrimitiveType) source;
					PrimitiveType newType = (PrimitiveType) target;

					MethodInvocation getterInvocation = contextAST
							.newMethodInvocation();
					sourceRewriter.set(getterInvocation,
							MethodInvocation.EXPRESSION_PROPERTY,
							getterMethodInvocation, null);

					SimpleName getterMethodName = null;
					TypeDeclaration myTypeDeclaration = null;
					if (isInput) {
						myTypeDeclaration = oldTypeDec;
					} else {
						myTypeDeclaration = newTypeDec;
					}
					for (MethodDeclaration aMethod : myTypeDeclaration
							.getMethods()) {
						SimpleName field = ASTParserUtility.isGetter(aMethod);
						if (field != null) {
							if ((field.resolveTypeBinding().getName()
									.equalsIgnoreCase(newType.getName())
									|| field.resolveTypeBinding()
											.getName()
											.equalsIgnoreCase(
													"local" + newType.getName()) || field
									.resolveTypeBinding().getName()
									.startsWith(newType.getName()))
									|| similarTo(field.getIdentifier(),
											newType.getVariableName())) {
								getterMethodName = aMethod.getName();
								break;
							}
						}
					}

					/*
					 * String modifiedVariableName =
					 * newType.getVariableName().substring(0, 1) .toUpperCase()
					 * + newType.getVariableName().substring(1,
					 * newType.getVariableName().length());
					 */
					sourceRewriter.set(getterInvocation,
							MethodInvocation.NAME_PROPERTY, getterMethodName,
							null);

					if (isInput) {
						createSetterForParentComplexObject(sourceRewriter,
								delta, contextAST, method, oldTypeDec, newType,
								getterInvocation, isInput);
					} else {
						createSetterForParentComplexObject(sourceRewriter,
								delta, contextAST, method, oldTypeDec, type,
								getterInvocation, isInput);
					}

				} else if (source instanceof ComplexType) {
					// TODO: determine if newType is an array
					ComplexType type = (ComplexType) source;
					ComplexType newType = (ComplexType) target;
					String newTypeName;
					boolean setterNeeded = false;
					boolean isArray = false;
					if (delta.getParent().getSource() instanceof ComplexType) {
						newTypeName = newType.getName();
						setterNeeded = true;
						TypeDeclaration parentTypeDeclaration = findTypeDeclaration(
								((ComplexType) delta.getParent().getSource())
										.getName(),
								oldTypeDeclaration);
						for (FieldDeclaration field : parentTypeDeclaration
								.getFields()) {
							if ((field.getType().resolveBinding().getName()
									.equalsIgnoreCase(newType.getName()) || field
									.getType().resolveBinding().getName()
									.startsWith(newType.getName()))) {
								for (Object fragment : field.fragments()) {
									VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragment;
									if (similarTo(variableDeclarationFragment
											.getName().getIdentifier(),
											newType.getVariableName())) {
										if (variableDeclarationFragment
												.resolveBinding().getType()
												.isArray()) {
											isArray = true;
										}
									}
									break;
								}
							}
							break;
						}

					} else {
						if (isInput) {
							newTypeName = NEW_REQUEST_NAME;
						} else {
							newTypeName = OLD_RESPONSE_NAME;
						}
					}
					String qualifiedTypeName = null;
					String modifiedNewTypeName = null;
					if (isInput) {
						this.createInstance(
								method,
								sourceRewriter,
								contextAST,
								this.findTypeDeclaration(
										newType.getVariableName(),
										newTypeDeclaration).resolveBinding()
										.getQualifiedName(), newTypeName,
								isArray);
						modifiedNewTypeName = newType.getName().substring(0, 1)
								.toLowerCase()
								+ newType.getName().substring(1,
										newType.getName().length());
						qualifiedTypeName = findTypeDeclaration(
								newType.getName(), newTypeDeclaration)
								.resolveBinding().getQualifiedName();
					} else {
						this.createInstance(
								method,
								sourceRewriter,
								contextAST,
								this.findTypeDeclaration(
										type.getVariableName(),
										oldTypeDeclaration).resolveBinding()
										.getQualifiedName(), newTypeName,
								isArray);
						modifiedNewTypeName = type.getName().substring(0, 1)
								.toLowerCase()
								+ type.getName().substring(1,
										type.getName().length());
						qualifiedTypeName = findTypeDeclaration(type.getName(),
								oldTypeDeclaration).resolveBinding()
								.getQualifiedName();
					}

					createInstance(method, sourceRewriter, contextAST,
							qualifiedTypeName, modifiedNewTypeName, isArray);

					if (setterNeeded) {

						this.createSetterForParentComplexObject(sourceRewriter,
								delta, contextAST, method, oldTypeDec, newType,
								contextAST.newSimpleName(modifiedNewTypeName),
								isInput);
					}

					Expression newExpression = null;
					if (delta.getParent().getSource() instanceof ComplexType) {
						newExpression = contextAST.newMethodInvocation();
						sourceRewriter.set(newExpression,
								MethodInvocation.EXPRESSION_PROPERTY,
								getterMethodInvocation, null);

						SimpleName getterMethodName = null;
						for (MethodDeclaration aMethod : oldTypeDec
								.getMethods()) {
							SimpleName field = ASTParserUtility
									.isGetter(aMethod);
							if (field != null) {
								if ((field.resolveTypeBinding().getName()
										.equalsIgnoreCase(newType.getName()) || field
										.resolveTypeBinding().getName()
										.startsWith(newType.getName()))
										&& similarTo(field.getIdentifier(),
												newType.getVariableName())) {
									getterMethodName = aMethod.getName();
									break;
								}
							}
						}

						sourceRewriter.set(newExpression,
								MethodInvocation.NAME_PROPERTY,
								getterMethodName, null);

					} else {
						newExpression = contextAST.newMethodInvocation();
						sourceRewriter.set(newExpression,
								MethodInvocation.EXPRESSION_PROPERTY,
								contextAST.newSimpleName(oldInputName), null);

						SimpleName getterMethodName = null;
						TypeDeclaration typeDec = findTypeDeclaration(
								type.getVariableName(), oldTypeDeclaration);
						for (MethodDeclaration aMethod : typeDec.getMethods()) {
							SimpleName field = ASTParserUtility
									.isGetter(aMethod);
							if (field != null) {
								if ((field.resolveTypeBinding().getName()
										.equalsIgnoreCase(newType.getName()) || field
										.resolveTypeBinding().getName()
										.startsWith(newType.getName()))
										&& similarTo(field.getIdentifier(),
												newType.getVariableName())) {
									getterMethodName = aMethod.getName();
									break;
								}
							}
						}

						sourceRewriter.set(newExpression,
								MethodInvocation.NAME_PROPERTY,
								getterMethodName, null);

					}

					for (Delta child : delta.getDeltas()) {
						copyValues(
								sourceRewriter,
								child,
								contextAST,
								method,
								findTypeDeclaration(type.getName(),
										oldTypeDeclaration),
								findTypeDeclaration(newType.getName(),
										newTypeDeclaration), newExpression,
								isInput);
					}
					this.createSetterForParentComplexObject(
							sourceRewriter,
							delta,
							contextAST,
							method,
							this.findTypeDeclaration(newType.getVariableName(),
									newTypeDeclaration),
							new ComplexType(newType.getName(), newType
									.getVariableName()), contextAST
									.newSimpleName(modifiedNewTypeName),
							isInput);
				}
			}
		} else {
			if (isInput) {
				if (target instanceof PrimitiveType) {

					PrimitiveType newType = (PrimitiveType) target;
					TypeDeclaration parentType = null;
					if (delta.getParent().getTarget() instanceof ComplexType) {
						parentType = findTypeDeclaration(((ComplexType) delta
								.getParent().getTarget()).getName(),
								newTypeDeclaration);
					}
					Expression defaultValue = getDefaultValueForArgument(
							method, newType, parentType, contextAST);
					this.createSetterForParentComplexObject(sourceRewriter,
							delta, contextAST, method, oldTypeDec, newType,
							defaultValue, isInput);

				} else if (target instanceof ComplexType) {
					// find parent until parent = typeDeclaration and invoke
					// getters
					// in chain
					// MethodInvocation getterChain = createGetterChain();
					ComplexType type = (ComplexType) source;
					ComplexType newType = (ComplexType) target;
					String newTypeName;
					if (delta.getParent().getTarget() instanceof ComplexType) {
						newTypeName = newType.getName();
					} else {
						newTypeName = NEW_REQUEST_NAME;
					}
					String modifiedNewTypeName = newTypeName.substring(0, 1)
							.toLowerCase()
							+ newTypeName.substring(1, newTypeName.length());
					String qualifiedTypeName = findTypeDeclaration(
							newType.getName(), newTypeDeclaration)
							.resolveBinding().getQualifiedName();

					createInstance(method, sourceRewriter, contextAST,
							qualifiedTypeName, modifiedNewTypeName, false);

					Expression newExpression = contextAST.newMethodInvocation();
					if (delta.getParent().getSource() instanceof ComplexType) {
						sourceRewriter.set(newExpression,
								MethodInvocation.EXPRESSION_PROPERTY,
								getterMethodInvocation, null);
						SimpleName getterMethodName = null;
						for (MethodDeclaration aMethod : oldTypeDec
								.getMethods()) {
							SimpleName field = ASTParserUtility
									.isGetter(aMethod);
							if (field != null) {
								if (field.resolveTypeBinding().getName()
										.equalsIgnoreCase(newType.getName())) {
									getterMethodName = aMethod.getName();
									break;
								}
							}
						}

						sourceRewriter.set(newExpression,
								MethodInvocation.NAME_PROPERTY,
								getterMethodName, null);
					} else {
						newExpression = contextAST.newSimpleName(oldInputName);
					}

					for (Delta child : delta.getDeltas()) {
						copyValues(
								sourceRewriter,
								child,
								contextAST,
								method,
								findTypeDeclaration(type.getName(),
										oldTypeDeclaration),
								findTypeDeclaration(newType.getName(),
										newTypeDeclaration), newExpression,
								isInput);
					}
					/*
					 * if (!newType.getName().equalsIgnoreCase(
					 * typeDeclaration.getName().getIdentifier())) {
					 * TypeDeclaration[] types = newTypeDeclaration.getTypes();
					 * for (int i = 0; i < types.length; i++) { if (types[i]
					 * .getName() .getIdentifier() .equalsIgnoreCase(
					 * typeDeclaration.getName() .getIdentifier())) {
					 * 
					 * } } }
					 */
				}
			}
		}
	}

	private void createSetterForParentComplexObject(ASTRewrite sourceRewriter,
			Delta delta, AST contextAST, MethodDeclaration method,
			TypeDeclaration parentTypeDeclaration, IType typeToBeSet,
			Expression setterArgument, boolean isInput) {
		String modifiedParentTypeName;
		if (delta.getParent().getParent().getTarget() instanceof ComplexType) {
			modifiedParentTypeName = parentTypeDeclaration.getName()
					.getIdentifier().substring(0, 1).toLowerCase()
					+ parentTypeDeclaration
							.getName()
							.getIdentifier()
							.substring(
									1,
									parentTypeDeclaration.getName()
											.getIdentifier().length());
		} else if (delta.getParent().getTarget() instanceof ComplexType
				&& parentTypeDeclaration
						.resolveBinding()
						.getName()
						.equalsIgnoreCase(
								((ComplexType) delta.getParent().getTarget())
										.getName())) {
			modifiedParentTypeName = parentTypeDeclaration.getName()
					.getIdentifier().substring(0, 1).toLowerCase()
					+ parentTypeDeclaration
							.getName()
							.getIdentifier()
							.substring(
									1,
									parentTypeDeclaration.getName()
											.getIdentifier().length());
		} else {
			if (isInput) {
				modifiedParentTypeName = NEW_REQUEST_NAME.substring(0, 1)
						.toLowerCase()
						+ NEW_REQUEST_NAME.substring(1,
								NEW_REQUEST_NAME.length());
			} else {
				modifiedParentTypeName = OLD_RESPONSE_NAME.substring(0, 1)
						.toLowerCase()
						+ OLD_RESPONSE_NAME.substring(1,
								OLD_RESPONSE_NAME.length());
			}
		}
		/*
		 * String variableName; if (typeToBeSet instanceof PrimitiveType) {
		 * variableName = ((PrimitiveType) typeToBeSet).getVariableName(); }
		 * else { variableName = typeToBeSet.getName(); }
		 */

		MethodInvocation setterInvocation = contextAST.newMethodInvocation();
		sourceRewriter.set(setterInvocation,
				MethodInvocation.EXPRESSION_PROPERTY,
				contextAST.newName(modifiedParentTypeName), null);

		SimpleName setterMethodName = null;
		for (MethodDeclaration aMethod : parentTypeDeclaration.getMethods()) {
			SimpleName field = ASTParserUtility.isSetter(aMethod);
			// in case the setter is not a normal setter
			SimpleName setField = isSetterMethod(aMethod);
			if (field != null) {
				if (field.resolveTypeBinding().getName()
						.equalsIgnoreCase(typeToBeSet.getName())
						&& similarTo(field.getIdentifier(),
								typeToBeSet.getVariableName())) {
					setterMethodName = aMethod.getName();
					break;
				}
			} else if (setField != null) {
				if ((setField.resolveTypeBinding().getName()
						.equalsIgnoreCase(typeToBeSet.getName()) || setField
						.resolveTypeBinding().getName()
						.startsWith(typeToBeSet.getName()))
						&& similarTo(setField.getIdentifier(),
								typeToBeSet.getVariableName())) {
					setterMethodName = aMethod.getName();
					break;
				}
			}
		}

		sourceRewriter.set(setterInvocation, MethodInvocation.NAME_PROPERTY,
				setterMethodName, null);

		ListRewrite arguments = sourceRewriter.getListRewrite(setterInvocation,
				MethodInvocation.ARGUMENTS_PROPERTY);
		arguments.insertLast(setterArgument, null);

		ExpressionStatement setterStatement = contextAST
				.newExpressionStatement(setterInvocation);
		ListRewrite methodBody = sourceRewriter.getListRewrite(
				method.getBody(), Block.STATEMENTS_PROPERTY);
		methodBody.insertLast(setterStatement, null);
	}

	private boolean similarTo(String localVariableName, String variableName) {
		String[] localVariableNameHumanized = HumaniseCamelCase.humanise(
				localVariableName).split(" ");
		String[] variableNameHumanized = HumaniseCamelCase.humanise(
				variableName).split(" ");
		int counter = 0;
		for (String word : variableNameHumanized) {
			if (contains(localVariableNameHumanized, word)) {
				counter++;
			}
		}
		if (counter == variableNameHumanized.length) {
			return true;
		} else {
			return false;
		}
	}

	private boolean contains(String[] wordList, String word) {
		for (String aWord : wordList) {
			if (aWord.equalsIgnoreCase(word)) {
				return true;
			}
		}
		return false;
	}

	private SimpleName isSetterMethod(MethodDeclaration aMethod) {
		Block methodBody = aMethod.getBody();
		List<SingleVariableDeclaration> parameters = aMethod.parameters();
		if (methodBody != null) {
			List<Statement> statements = methodBody.statements();
			if (aMethod.getName().getIdentifier().startsWith("set")
					&& parameters.size() == 1) {
				for (Statement statement : statements) {
					if (statement instanceof ExpressionStatement) {
						ExpressionStatement expressionStatement = (ExpressionStatement) statement;
						Expression expressionStatementExpression = expressionStatement
								.getExpression();
						if (expressionStatementExpression instanceof Assignment) {
							Assignment assignment = (Assignment) expressionStatementExpression;
							Expression rightHandSide = assignment
									.getRightHandSide();
							if (rightHandSide instanceof SimpleName) {
								SimpleName rightHandSideSimpleName = (SimpleName) rightHandSide;
								if (rightHandSideSimpleName.getIdentifier()
										.equals(parameters.get(0).getName()
												.getIdentifier())) {
									Expression leftHandSide = assignment
											.getLeftHandSide();
									if (leftHandSide instanceof SimpleName) {
										return (SimpleName) leftHandSide;
									} else if (leftHandSide instanceof FieldAccess) {
										FieldAccess fieldAccess = (FieldAccess) leftHandSide;
										return fieldAccess.getName();
									}
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	private Expression getDefaultValueForArgument(MethodDeclaration method,
			PrimitiveType newType, TypeDeclaration parentType, AST contextAST) {
		FieldDeclaration[] fields = parentType.getFields();
		for (int i = 0; i < fields.length; i++) {
			List<VariableDeclarationFragment> fragments = fields[i].fragments();
			for (VariableDeclarationFragment fragment : fragments) {
				if (similarTo(fragment.getName().getIdentifier(),
						newType.getVariableName())) {
					String typeName = fragment.resolveBinding().getType()
							.getName();
					if (typeName.equals("String")) {
						StringLiteral stringLiteral = contextAST
								.newStringLiteral();
						stringLiteral.setLiteralValue("");
						return stringLiteral;
					} else if (typeName.equals("int")) {
						NumberLiteral numberLiteral = contextAST
								.newNumberLiteral();
						numberLiteral.setToken("0");
						return numberLiteral;
					} else {
						NullLiteral nullLiteral = contextAST.newNullLiteral();
						return nullLiteral;
					}
				}
			}
		}

		return null;
	}

	private TypeDeclaration findTypeDeclaration(String newTypeName,
			TypeDeclaration typeDeclaration) {
		for (int i = 0; i < typeDeclaration.getTypes().length; i++) {
			if (typeDeclaration.getTypes()[i].getName().getIdentifier()
					.equalsIgnoreCase(newTypeName)) {
				return typeDeclaration.getTypes()[i];
			}
		}
		return null;
	}

	private void instantiateNewStub(MethodDeclaration constructorDeclaration) {
		ASTRewrite sourceRewriter = ASTRewrite.create(oldTypeDeclaration
				.getAST());
		AST contextAST = constructorDeclaration.getAST();
		ListRewrite constructorBodyRewrite = sourceRewriter.getListRewrite(
				constructorDeclaration.getBody(), Block.STATEMENTS_PROPERTY);

		FieldAccess fieldAccess = contextAST.newFieldAccess();
		ThisExpression thisExpression = contextAST.newThisExpression();
		sourceRewriter.set(fieldAccess, FieldAccess.EXPRESSION_PROPERTY,
				thisExpression, null);
		SimpleName simpleName = contextAST
				.newSimpleName(ClientAdaptationRefactoring.NEW_STUB_REFERENCE_NAME);
		sourceRewriter.set(fieldAccess, FieldAccess.NAME_PROPERTY, simpleName,
				null);

		ClassInstanceCreation classInstanceCreation = contextAST
				.newClassInstanceCreation();
		SimpleType simpleType = contextAST
				.newSimpleType(contextAST.newName(newTypeDeclaration
						.resolveBinding().getQualifiedName()));
		sourceRewriter.set(classInstanceCreation,
				ClassInstanceCreation.TYPE_PROPERTY, simpleType, null);
		List<SingleVariableDeclaration> parameters = constructorDeclaration
				.parameters();
		ListRewrite arguments = sourceRewriter
				.getListRewrite(classInstanceCreation,
						ClassInstanceCreation.ARGUMENTS_PROPERTY);
		for (SingleVariableDeclaration parameter : parameters) {
			arguments.insertLast(contextAST.newSimpleName(parameter.getName()
					.getIdentifier()), null);
		}

		Assignment assignment = contextAST.newAssignment();
		sourceRewriter.set(assignment, Assignment.LEFT_HAND_SIDE_PROPERTY,
				fieldAccess, null);
		sourceRewriter.set(assignment, Assignment.OPERATOR_PROPERTY,
				Assignment.Operator.ASSIGN, null);
		sourceRewriter.set(assignment, Assignment.RIGHT_HAND_SIDE_PROPERTY,
				classInstanceCreation, null);

		ExpressionStatement assignmentStatement = contextAST
				.newExpressionStatement(assignment);

		constructorBodyRewrite.insertLast(assignmentStatement, null);

		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			compilationUnitChange.getEdit().addChild(sourceEdit);
			compilationUnitChange.addTextEditGroup(new TextEditGroup(
					"Instantiate new stub reference in constructor ",
					new TextEdit[] { sourceEdit }));
		} catch (JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}

	}

	private void deleteChangedMethodBody(MethodDeclaration method) {
		ASTRewrite sourceRewriter = ASTRewrite.create(oldTypeDeclaration
				.getAST());
		ListRewrite methodBodyRewrite = sourceRewriter.getListRewrite(
				method.getBody(), Block.STATEMENTS_PROPERTY);
		List<Statement> sourceMethodStatements = method.getBody().statements();
		for (Statement statement : sourceMethodStatements) {
			methodBodyRewrite.remove(statement, null);
		}
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			compilationUnitChange.getEdit().addChild(sourceEdit);
			compilationUnitChange.addTextEditGroup(new TextEditGroup(
					"Delete body of change method "
							+ method.getName().getIdentifier(),
					new TextEdit[] { sourceEdit }));
		} catch (JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}
	}

	private void addNewStubReferenceToOldStub() {
		ASTRewrite sourceRewriter = ASTRewrite.create(oldTypeDeclaration
				.getAST());
		AST contextAST = oldTypeDeclaration.getAST();
		ListRewrite contextBodyRewrite = sourceRewriter.getListRewrite(
				oldTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		VariableDeclarationFragment newStubFragment = contextAST
				.newVariableDeclarationFragment();
		sourceRewriter
				.set(newStubFragment,
						VariableDeclarationFragment.NAME_PROPERTY,
						contextAST
								.newSimpleName(ClientAdaptationRefactoring.NEW_STUB_REFERENCE_NAME),
						null);

		FieldDeclaration newStubReference = contextAST
				.newFieldDeclaration(newStubFragment);
		ListRewrite newStubReferenceModifiers = sourceRewriter.getListRewrite(
				newStubReference, FieldDeclaration.MODIFIERS2_PROPERTY);
		newStubReferenceModifiers.insertLast(contextAST
				.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD), null);

		// SimpleType simpleType =
		// contextAST.newSimpleType(contextAST.newName(newTypeDeclaration.getName().getIdentifier()));
		sourceRewriter.set(newStubReference, FieldDeclaration.TYPE_PROPERTY,
				contextAST.newName(newTypeDeclaration.resolveBinding()
						.getQualifiedName()), null);

		contextBodyRewrite.insertFirst(newStubReference, null);
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			compilationUnitChange.getEdit().addChild(sourceEdit);
			compilationUnitChange.addTextEditGroup(new TextEditGroup(
					"Add new stub reference to old stub.",
					new TextEdit[] { sourceEdit }));
		} catch (JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}

	}

	private void getSimpleTypeBindings(Set<ITypeBinding> typeBindings,
			Set<ITypeBinding> finalTypeBindings) {
		for (ITypeBinding typeBinding : typeBindings) {
			if (typeBinding.isPrimitive()) {

			} else if (typeBinding.isArray()) {
				ITypeBinding elementTypeBinding = typeBinding.getElementType();
				Set<ITypeBinding> typeBindingList = new LinkedHashSet<ITypeBinding>();
				typeBindingList.add(elementTypeBinding);
				getSimpleTypeBindings(typeBindingList, finalTypeBindings);
			} else if (typeBinding.isParameterizedType()) {
				Set<ITypeBinding> typeBindingList = new LinkedHashSet<ITypeBinding>();
				typeBindingList.add(typeBinding.getTypeDeclaration());
				ITypeBinding[] typeArgumentBindings = typeBinding
						.getTypeArguments();
				for (ITypeBinding typeArgumentBinding : typeArgumentBindings)
					typeBindingList.add(typeArgumentBinding);
				getSimpleTypeBindings(typeBindingList, finalTypeBindings);
			} else if (typeBinding.isWildcardType()) {
				Set<ITypeBinding> typeBindingList = new LinkedHashSet<ITypeBinding>();
				typeBindingList.add(typeBinding.getBound());
				getSimpleTypeBindings(typeBindingList, finalTypeBindings);
			} else {
				if (typeBinding.isNested()) {
					finalTypeBindings.add(typeBinding.getDeclaringClass());
				}
				finalTypeBindings.add(typeBinding);
			}
		}
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 1);
		} finally {
			pm.done();
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		try {
			pm.beginTask("Creating change...", 1);
			final Collection<Change> changes = new ArrayList<Change>();
			changes.add(compilationUnitChange);
			CompositeChange change = new CompositeChange(getName(),
					changes.toArray(new Change[changes.size()])) {
				@Override
				public ChangeDescriptor getDescriptor() {
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit) oldCompilationUnit
							.getJavaElement();
					String project = sourceICompilationUnit.getJavaProject()
							.getElementName();
					String description = MessageFormat.format(
							"Extracting class from ''{0}''",
							new Object[] { oldTypeDeclaration.getName()
									.getIdentifier() });
					String comment = null;
					return new RefactoringChangeDescriptor(
							new ClientAdaptationRefactoringDescriptor(project,
									description, comment, oldCompilationUnit,
									newCompilationUnit, oldTypeDeclaration,
									newTypeDeclaration, changedMethods));
				}
			};
			return change;
		} finally {
			pm.done();
		}
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Client Adaptation";
	}

}
