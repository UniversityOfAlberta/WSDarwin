package wsdarwin.clientadaptation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import wsdarwin.clientadaptation.ast.util.ASTParserUtility;
import wsdarwin.comparison.delta.Delta;
import wsdarwin.model.ComplexType;
import wsdarwin.model.IType;
import wsdarwin.model.Interface;
import wsdarwin.model.Operation;

public class RESTClientAdaptationRefactoring extends
		ClientAdaptationRefactoring {

	private Map<String, CompilationUnit> oldCompilationUnits;
	private Map<String, CompilationUnit> newCompilationUnits;
	private Delta delta;
	private Statement afterStatement;
	
	public RESTClientAdaptationRefactoring(CompilationUnit oldCompilationUnit, CompilationUnit newCompilationUnit,
			Map<String, CompilationUnit> oldCompilationUnits,
			Map<String, CompilationUnit> newCompilationUnits,
			TypeDeclaration oldTypeDeclaration,
			TypeDeclaration newTypeDeclaration,
			Map<MethodDeclaration, Delta> changedMethods, Delta delta) {
		super(oldTypeDeclaration, newTypeDeclaration, changedMethods);
		this.oldCompilationUnit = oldCompilationUnit;
		this.newCompilationUnit = newCompilationUnit;
		this.oldCompilationUnits = oldCompilationUnits;
		this.newCompilationUnits = newCompilationUnits;
		this.oldTypeDeclarations = new TreeMap<String, TypeDeclaration>();
		/*for(TypeDeclaration type : oldTypeDeclaration.getTypes()) {
			oldTypeDeclarations.put(type.getName().getIdentifier(), type);
		}*/
		ArrayList<TypeDeclaration> oldTypes = new ArrayList<TypeDeclaration>();
		for(CompilationUnit unit : oldCompilationUnits.values()) {
			ArrayList<TypeDeclaration> types = new ArrayList<TypeDeclaration>();
			types.addAll(unit.types());
			oldTypes.addAll(types);
			for(TypeDeclaration type : types) {
				getTypeDeclarations(type, oldTypes);
			}
			
		}
		for(TypeDeclaration type : oldTypes) {
			oldTypeDeclarations.put(type.getName().getIdentifier().toLowerCase(), type);
		}
		
		this.newTypeDeclarations = new TreeMap<String, TypeDeclaration>();
		/*for(TypeDeclaration type : newTypeDeclaration.getTypes()) {
			newTypeDeclarations.put(type.getName().getIdentifier(), type);
		}*/
		ArrayList<TypeDeclaration> newTypes = new ArrayList<TypeDeclaration>();
		for(CompilationUnit unit : newCompilationUnits.values()) {
			ArrayList<TypeDeclaration> types = new ArrayList<TypeDeclaration>();
			types.addAll(unit.types());
			newTypes.addAll(types);
			for(TypeDeclaration type : types) {
				getTypeDeclarations(type, newTypes);
			}
			
		}
		for(TypeDeclaration type : newTypes) {
			newTypeDeclarations.put(type.getName().getIdentifier().toLowerCase(), type);
		}
		this.delta = delta;
		MultiTextEdit oldMultiTextEdit = new MultiTextEdit();
		this.compilationUnitChange = new CompilationUnitChange("",
				(ICompilationUnit) oldCompilationUnits.get(
						oldTypeDeclaration.getName().getIdentifier() + ".java")
						.getJavaElement());
		compilationUnitChange.setEdit(oldMultiTextEdit);
		isWSDL = false;
	}
	
	private void getTypeDeclarations(
			TypeDeclaration type, List<TypeDeclaration> types) {
		List<TypeDeclaration> subTypes = Arrays.asList(type.getTypes());
		types.addAll(subTypes);
		for(TypeDeclaration innerType : type.getTypes()) {
			getTypeDeclarations(innerType, types);
		}
	}

	@Override
	protected void apply() {
		String newBaseURI = getNewBaseURI();
		changeBaseURI(newBaseURI);
		for (MethodDeclaration method : changedMethods.keySet()) {
			Delta delta = changedMethods.get(method);
			if (!((Operation) delta.getSource()).getResponseMediaType().equals(
					((Operation) delta.getTarget()).getResponseMediaType())) {
				// changeResponseMediaType();

			}
			AST contextAST = method.getAST();
			ASTRewrite sourceRewriter = ASTRewrite.create(contextAST);
			String typeName = ((Interface)delta.getParent().getSource()).getName();
			oldTypeDeclaration = oldTypeDeclarations.get(typeName.toLowerCase());
			afterStatement = findAfterStatement(method, false);
			prepareInput(method, delta);
			Statement statement = findAfterStatement(method, true);
			refactorMethodInvocation(statement, delta, sourceRewriter, contextAST);
			afterStatement = statement;
			//wrapMethodOutput(method, delta, statement);
			
			prepareOutput(method, delta);
			returnOutput(method);
		}
	}
	
	protected void prepareOutput(MethodDeclaration method, Delta delta) {
		ASTRewrite sourceRewriter = ASTRewrite.create(method
				.getAST());
		AST contextAST = method.getAST();

		wrapMethodOutput(method, delta, sourceRewriter, contextAST);
		
		copyValuesOutput(
				sourceRewriter,
				delta.getDeltas().get(1),
				contextAST,
				method,
				findTypeDeclaration(((ComplexType) delta.getDeltas().get(1)
						.getSource()).getName(), true),
				findTypeDeclaration(((ComplexType) delta.getDeltas().get(1)
						.getTarget()).getName(), false),
				contextAST.newMethodInvocation());

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
	
	

	private void wrapMethodOutput(MethodDeclaration method, Delta delta2, ASTRewrite sourceRewriter, AST contextAST) {
		VariableDeclarationFragment fragment = contextAST.newVariableDeclarationFragment();
		sourceRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, contextAST.newSimpleName(NEW_RESPONSE_NAME), null);
		MethodInvocation methodInvocation = contextAST.newMethodInvocation();
		sourceRewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, contextAST.newSimpleName("response"), null);
		sourceRewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, contextAST.newSimpleName("getEntity"), null);
		
		Delta outputDeltaType = null;
		Operation operation = (Operation)delta2.getTarget();
		for(Delta child : delta2.getDeltas()) {
			if(child.getTarget() instanceof ComplexType) {
				if(((ComplexType)child.getTarget()).equals(operation.getResponse())) {
					outputDeltaType = child;
				}
			}
		}
		ComplexType target = (ComplexType)outputDeltaType.getTarget();
		
		ListRewrite arguments = sourceRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		sourceRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, methodInvocation, null);
		TypeLiteral typeLiteral = contextAST.newTypeLiteral();
		sourceRewriter.set(typeLiteral, TypeLiteral.TYPE_PROPERTY, contextAST.newSimpleType(contextAST.newName(findTypeDeclaration(target.getName(), false).resolveBinding().getQualifiedName())), null);
		arguments.insertLast(typeLiteral, null);
		VariableDeclarationStatement variableDeclarationStatement = contextAST.newVariableDeclarationStatement(fragment);
		sourceRewriter.set(variableDeclarationStatement, VariableDeclarationStatement.TYPE_PROPERTY, contextAST.newSimpleType(contextAST.newName(findTypeDeclaration(target.getName(), false).resolveBinding().getQualifiedName())), null);
		insertStatementInMethod(method, sourceRewriter, variableDeclarationStatement);
		
		
	}

	private void refactorMethodInvocation(Statement statement, Delta delta2, ASTRewrite sourceRewrite, AST contextAST) {
		MethodInvocation methodInvocation = null;
		if (statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement) statement;
			if (expressionStatement.getExpression() instanceof Assignment) {
				Assignment assignment = (Assignment) expressionStatement
						.getExpression();
				if (assignment.getRightHandSide() instanceof MethodInvocation) {
					MethodInvocation invocation = (MethodInvocation) assignment
							.getRightHandSide();
					if (invocation.getName().getIdentifier()
							.equals("method")) {
						methodInvocation = invocation;
					}
				}
			}
		}
		
		Delta inputDeltaType = null;
		Operation operation = (Operation)delta2.getSource();
		for(Delta child : delta2.getDeltas()) {
			if(child.getSource() instanceof ComplexType) {
				if(((ComplexType)child.getSource()).equals(operation.getRequest())) {
					inputDeltaType = child;
				}
			}
		}
		ComplexType source = (ComplexType)inputDeltaType.getSource();
		ComplexType target = (ComplexType)inputDeltaType.getTarget();
		ListRewrite argumentsList = sourceRewrite.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
		List<Expression> arguments = methodInvocation.arguments();
		for(Expression argument : arguments) {
			if(argument instanceof StringLiteral) {
				StringLiteral stringLiteral = (StringLiteral)argument;
				StringLiteral newStringLiteral = contextAST.newStringLiteral();
				newStringLiteral.setLiteralValue(((Operation)delta2.getTarget()).getMethod());
				argumentsList.replace(stringLiteral, newStringLiteral, null);
			}
			else if(argument instanceof ClassInstanceCreation) {
				ClassInstanceCreation instanceCreation = (ClassInstanceCreation)argument;
				ListRewrite creationArgumentsList = sourceRewrite.getListRewrite(instanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
				List<Expression> creationArguments = instanceCreation.arguments();
				for(Expression creationArgument : creationArguments) {
					if(creationArgument instanceof TypeLiteral) {
						TypeLiteral typeLiteral = (TypeLiteral)creationArgument;
						TypeLiteral newTypeLiteral = contextAST.newTypeLiteral();
						newTypeLiteral.setType(contextAST.newSimpleType(contextAST.newName(findTypeDeclaration(target.getName(), false).resolveBinding().getQualifiedName())));
						creationArgumentsList.replace(typeLiteral, newTypeLiteral, null);
					}
					else if(creationArgument instanceof SimpleName) {
						SimpleName simpleName = (SimpleName)creationArgument;
						SimpleName newSimpleName = contextAST.newSimpleName(NEW_REQUEST_NAME);
						creationArgumentsList.replace(simpleName, newSimpleName, null);
						
					}
					else if(creationArgument instanceof ClassInstanceCreation) {
						ClassInstanceCreation classInstance = (ClassInstanceCreation)creationArgument;
						ListRewrite instanceArgumentsList = sourceRewrite.getListRewrite(classInstance, ClassInstanceCreation.ARGUMENTS_PROPERTY);
						List<Expression> instanceArguments = classInstance.arguments();
						for(Expression instanceArgument : instanceArguments) {
							if(instanceArgument instanceof StringLiteral) {
								StringLiteral stringLiteral = (StringLiteral)instanceArgument;
								if(stringLiteral.getLiteralValue().equalsIgnoreCase(source.getVariableName())) {
									StringLiteral newStringLiteral = contextAST.newStringLiteral();
									newStringLiteral.setLiteralValue(target.getVariableName());
									instanceArgumentsList.replace(stringLiteral, newStringLiteral, null);
								}
							}
						}
					}
				}
			}
		}
		
		try {
			TextEdit sourceEdit = sourceRewrite.rewriteAST();
			compilationUnitChange.getEdit().addChild(sourceEdit);
			compilationUnitChange.addTextEditGroup(new TextEditGroup(
					"Refactor method invocation.",
					new TextEdit[] { sourceEdit }));
		} catch (JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}
		
	}

	@Override
	protected void insertStatementInMethod(MethodDeclaration method,
			ASTRewrite sourceRewriter, Statement statement) {
		ListRewrite methodBodyRewrite = sourceRewriter.getListRewrite(
				method.getBody(), Block.STATEMENTS_PROPERTY);
		if (!(statement instanceof ReturnStatement)) {
			
			methodBodyRewrite.insertAfter(statement, afterStatement, null);
			afterStatement = statement;
		}
		else {
			List<Statement> bodyStatements = method.getBody().statements();
			for(Statement bodyStatement : bodyStatements) {
				if(bodyStatement instanceof ReturnStatement) {
					methodBodyRewrite.replace(bodyStatement, statement, null);
				}
			}
		}
	}

	private Statement findAfterStatement(MethodDeclaration method, boolean isOutput) {
		List<Statement> statements = method.getBody().statements();
		if (isOutput) {
			for (Statement statement : statements) {
				if (statement instanceof ExpressionStatement) {
					ExpressionStatement expressionStatement = (ExpressionStatement) statement;
					if (expressionStatement.getExpression() instanceof Assignment) {
						Assignment assignment = (Assignment) expressionStatement
								.getExpression();
						if (assignment.getRightHandSide() instanceof MethodInvocation) {
							MethodInvocation invocation = (MethodInvocation) assignment
									.getRightHandSide();
							if (invocation.getName().getIdentifier()
									.equals("method")) {
								return statement;
							}
						}
					}
				}
			}
		}
		else {
			for (Statement statement : statements) {
				if (statement instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
					if (variableDeclarationStatement.getType().resolveBinding().getName().equals("ClientResponse")) {
						return statement;
					}
				}
			}
		}
		return null;
	}

	private void changeBaseURI(String newBaseURI) {
		ASTRewrite sourceRewriter = ASTRewrite.create(oldTypeDeclaration
				.getAST());
		Initializer initializer = null;
		for (Object declaration : oldTypeDeclaration.bodyDeclarations()) {
			if (declaration instanceof Initializer) {
				initializer = (Initializer) declaration;
			}
		}
		AST contextAST = initializer.getAST();

		for (Object blockStatement : initializer.getBody().statements()) {
			Statement statement = (Statement) blockStatement;
			if (statement instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement) statement;
				if (variableDeclaration.getType().resolveBinding().getName()
						.equals("URI")) {
					for (Object variableFragment : variableDeclaration
							.fragments()) {
						VariableDeclarationFragment fragment = (VariableDeclarationFragment) variableFragment;
						if (fragment.getName().getIdentifier()
								.equals("originalURI")) {
							Expression expression = fragment.getInitializer();
							if (expression instanceof MethodInvocation) {
								MethodInvocation invocation = (MethodInvocation) expression;
								for (Object argumentItem : invocation
										.arguments()) {
									if (argumentItem instanceof StringLiteral) {
										StringLiteral oldLiteral = (StringLiteral) argumentItem;
										StringLiteral newLiteral = contextAST
												.newStringLiteral();
										newLiteral.setLiteralValue(newBaseURI);
										ListRewrite arguments = sourceRewriter
												.getListRewrite(
														invocation,
														MethodInvocation.ARGUMENTS_PROPERTY);
										arguments.replace(oldLiteral,
												newLiteral, null);
									}
								}
							}
						}
					}
				}
			}
		}

		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			compilationUnitChange.getEdit().addChild(sourceEdit);
			compilationUnitChange.addTextEditGroup(new TextEditGroup(
					"Change Base URI in the old proxy",
					new TextEdit[] { sourceEdit }));
		} catch (JavaModelException javaModelException) {
			javaModelException.printStackTrace();
		}

	}

	private String getNewBaseURI() {
		String newBaseURI = "";
		for (Delta childDelta : delta.getDeltas()) {
			if (childDelta.getSource() instanceof Interface) {
				newBaseURI = ((Interface) childDelta.getTarget()).getBase();
				break;
			}
		}
		return newBaseURI;
	}
	
	/*private void prepareInput(MethodDeclaration method, Delta delta) {
		ASTRewrite sourceRewriter = ASTRewrite.create(oldTypeDeclaration
				.getAST());
		AST contextAST = method.getAST();

		copyValuesInput(
				sourceRewriter,
				delta.getDeltas().get(0),
				contextAST,
				method,
				findTypeDeclaration(((ComplexType) delta.getDeltas().get(0)
						.getSource()).getName(), true),
				findTypeDeclaration(((ComplexType) delta.getDeltas().get(0)
						.getTarget()).getName(), false),
				contextAST.newMethodInvocation());
		System.out.println(method.getName().getIdentifier());

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

		copyValuesOutput(
				sourceRewriter,
				delta.getDeltas().get(1),
				contextAST,
				method,
				findTypeDeclaration(((ComplexType) delta.getDeltas().get(1)
						.getSource()).getName(), true),
				findTypeDeclaration(((ComplexType) delta.getDeltas().get(1)
						.getTarget()).getName(), false),
				contextAST.newMethodInvocation());

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

	}*/

	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		try {
			// long start = System.currentTimeMillis();
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
	
	protected void createSetterForParentComplexObject(ASTRewrite sourceRewriter,
			Delta delta, AST contextAST, MethodDeclaration method,
			TypeDeclaration parentTypeDeclaration, IType typeToBeSet,
			Expression setterArgument, boolean isInput) {
		String prefix = "";
		if(!isInput) {
			prefix="_";
		}
		boolean isArray = false;
		String modifiedParentTypeName;
		if (getElementFromDelta(delta.getParent().getParent(),isInput) instanceof ComplexType) {
			modifiedParentTypeName = prefix+parentTypeDeclaration.getName()
					.getIdentifier().substring(0, 1).toLowerCase()
					+ parentTypeDeclaration
							.getName()
							.getIdentifier()
							.substring(
									1,
									parentTypeDeclaration.getName()
											.getIdentifier().length())+"_"+((ComplexType)getElementFromDelta(delta.getParent(),isInput)).getVariableName();
			isArray = isArray(findTypeDeclaration(
					((ComplexType) delta.getParent().getParent().getTarget())
					.getName(),
					getStubTypeDeclaration(true)), (ComplexType) delta.getParent().getTarget());
		/*} else if (getElementFromDelta(delta.getParent(),isInput) instanceof ComplexType
				&& parentTypeDeclaration.resolveBinding().getName().equalsIgnoreCase(((ComplexType) getElementFromDelta(delta.getParent(),isInput)).getName())) {
			modifiedParentTypeName = prefix+parentTypeDeclaration.getName()
					.getIdentifier().substring(0, 1).toLowerCase()
					+ parentTypeDeclaration
							.getName()
							.getIdentifier()
							.substring(
									1,
									parentTypeDeclaration.getName()
											.getIdentifier().length())+"_"+((ComplexType)getElementFromDelta(delta.getParent(),isInput)).getVariableName();*/
			/*isArray = isArray(findTypeDeclaration(
					((XSDComplexType) delta.getParent().getSource())
					.getName(),
			oldTypeDeclaration), (XSDComplexType) delta.getParent().getSource());*/
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
		 * String variableName; if (typeToBeSet instanceof XSDPrimitiveType) {
		 * variableName = ((XSDPrimitiveType) typeToBeSet).getVariableName(); }
		 * else { variableName = typeToBeSet.getName(); }
		 */

		MethodInvocation setterInvocation = contextAST.newMethodInvocation();
		if (!isArray) {
			sourceRewriter.set(setterInvocation,
					MethodInvocation.EXPRESSION_PROPERTY,
					contextAST.newName(modifiedParentTypeName), null);
		}
		else {
			ArrayAccess arrayAccess = contextAST.newArrayAccess();
			sourceRewriter.set(arrayAccess, ArrayAccess.ARRAY_PROPERTY, contextAST.newName(modifiedParentTypeName), null);
			sourceRewriter.set(arrayAccess, ArrayAccess.INDEX_PROPERTY, contextAST.newNumberLiteral("0"), null);
			sourceRewriter.set(setterInvocation, MethodInvocation.EXPRESSION_PROPERTY, arrayAccess, null);
		}
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
						&& (similarTo(setField.getIdentifier(),
								typeToBeSet.getVariableName()) || similarTo(
								setField.getIdentifier(), typeToBeSet.getName()))) {
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
		insertStatementInMethod(method, sourceRewriter, setterStatement);
	}

}
