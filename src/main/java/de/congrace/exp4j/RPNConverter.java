package de.congrace.exp4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

abstract class RPNConverter {

	private static String substituteUnaryOperators(String expr, Map<String, CustomOperator> operators) {
		final StringBuilder exprBuilder = new StringBuilder(expr.length());
		final char[] data = expr.toCharArray();
		char lastChar = ' ';
		StringBuilder lastOperation = new StringBuilder();
		for (int i = 0; i < expr.length(); i++) {
			if (exprBuilder.length() > 0) {
				lastChar = exprBuilder.charAt(exprBuilder.length() - 1);
			}
			final char c = data[i];
			if (i > 0 && isOperatorCharacter(expr.charAt(i - 1), operators)) {
				if (!operators.containsKey(lastOperation.toString() + expr.charAt(i - 1))) {
					lastOperation = new StringBuilder();
				}
				lastOperation.append(expr.charAt(i - 1));
			} else if (i > 0 && !Character.isWhitespace(expr.charAt(i - 1))) {
				lastOperation = new StringBuilder();
			}
			switch (c) {
			case '+':
				if (i > 0 && lastChar != '(' && getOperator(lastOperation.toString(),operators) == null) {
					exprBuilder.append(c);
				}
				break;
			case '-':
				if (i > 0 && lastChar != '(' && getOperator(lastOperation.toString(),operators) == null) {
					exprBuilder.append(c);
				} else {
					exprBuilder.append('\'');
				}
				break;
			default:
				exprBuilder.append(c);
			}
		}
		return exprBuilder.toString();
	}

	private static CustomOperator getOperator(String symbol, Map<String, CustomOperator> operators) {
		CustomOperator op = symbol.length() == 1 ? BuiltinOperators.getOperator(symbol.charAt(0)) : null;
		if (op == null){
			op = operators.get(symbol);
		}
		return op;
	}

	static RPNExpression toRPNExpression(String infix, Map<String, Double> variables,
			Map<String, CustomFunction> customFunctions, Map<String, CustomOperator> operators)
			throws UnknownFunctionException, UnparsableExpressionException {
		final Stack<Token> operatorStack = new Stack<Token>();
		final StringBuilder output = new StringBuilder(infix.length());
		final Tokenizer tokenizer = new Tokenizer(variables.keySet(), customFunctions, operators);
		long start = System.currentTimeMillis();
		String tmp = substituteUnaryOperators(infix, operators);
		System.out.println(":: tokenizer took " + (System.currentTimeMillis() - start) + " ms WHAT?");
		List<Token> tokens = tokenizer.getTokens(tmp);
		validateRPNExpression(tokens,operators);
		for (final Token token : tokens) {
			token.mutateStackForInfixTranslation(operatorStack, output);
		}
		// all tokens read, put the rest of the operations on the output;
		while (operatorStack.size() > 0) {
			output.append(operatorStack.pop().getValue()).append(" ");
		}
		String postfix = output.toString().trim();
		tokens = tokenizer.getTokens(postfix);
		RPNExpression rpn = new RPNExpression(tokens, postfix, variables); 
		return rpn;
	}

	private static void validateRPNExpression(List<Token> tokens,Map<String,CustomOperator> operators) throws UnparsableExpressionException{
		for (int i = 1; i < tokens.size(); i++) {
			Token t = tokens.get(i);
			if (tokens.get(i - 1) instanceof NumberToken){
				if (t instanceof VariableToken || 
					(t instanceof ParenthesesToken && ((ParenthesesToken)t).isOpen()) ||
					t instanceof FunctionToken) {
					throw new UnparsableExpressionException("Implicit multiplication is not supported. E.g. always use '2*x' instead of '2x'");
				}
			}
		}
	}

	private static boolean isOperatorCharacter(char c, Map<String, CustomOperator> operators) {
		if (BuiltinOperators.isOperatorCharacter(c)){
			return true;
		}
		for (String symbol : operators.keySet()) {
			if (symbol.indexOf(c) != -1) {
				return true;
			}
		}
		return false;
	}

}
