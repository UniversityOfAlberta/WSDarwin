package wsdarwin.clientadaptation.ast.util;

import java.util.List;

import org.eclipse.jdt.core.dom.*;

public class ASTParserUtility {

	public static SimpleName isGetter(MethodDeclaration methodDeclaration) {
		Block methodBody = methodDeclaration.getBody();
		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		if(methodBody != null) {
			List<Statement> statements = methodBody.statements();
			if(statements.size() == 1 && parameters.size() == 0) {
				Statement statement = statements.get(0);
	    		if(statement instanceof ReturnStatement) {
	    			ReturnStatement returnStatement = (ReturnStatement)statement;
	    			Expression returnStatementExpression = returnStatement.getExpression();
	    			if(returnStatementExpression instanceof SimpleName) {
	    				return (SimpleName)returnStatementExpression;
	    			}
	    			else if(returnStatementExpression instanceof FieldAccess) {
	    				FieldAccess fieldAccess = (FieldAccess)returnStatementExpression;
	    				return fieldAccess.getName();
	    			}
	    		}
			}
		}
		return null;
	}

	public static SimpleName isSetter(MethodDeclaration methodDeclaration) {
		Block methodBody = methodDeclaration.getBody();
		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		if(methodBody != null) {
			List<Statement> statements = methodBody.statements();
			if(statements.size() == 1 && parameters.size() == 1) {
				Statement statement = statements.get(0);
	    		if(statement instanceof ExpressionStatement) {
	    			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
	    			Expression expressionStatementExpression = expressionStatement.getExpression();
	    			if(expressionStatementExpression instanceof Assignment) {
	    				Assignment assignment = (Assignment)expressionStatementExpression;
	    				Expression rightHandSide = assignment.getRightHandSide();
	    				if(rightHandSide instanceof SimpleName) {
	    					SimpleName rightHandSideSimpleName = (SimpleName)rightHandSide;
	    					if(rightHandSideSimpleName.getIdentifier().equals(parameters.get(0).getName().getIdentifier())) {
	    						Expression leftHandSide = assignment.getLeftHandSide();
	    						if(leftHandSide instanceof SimpleName) {
	    		    				return (SimpleName)leftHandSide;
	    		    			}
	    		    			else if(leftHandSide instanceof FieldAccess) {
	    		    				FieldAccess fieldAccess = (FieldAccess)leftHandSide;
	    		    				return fieldAccess.getName();
	    		    			}
	    					}
	    				}
	    			}
	    		}
			}
		}
		return null;
	}
	
}
