package io.github.ai4ci.functions;
import java.io.Serializable;

import org.immutables.value.Value;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.mXparser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableMathematicalFunction.class)
@JsonDeserialize(as = ImmutableMathematicalFunction.class)
public interface MathematicalFunction extends Serializable, SimpleFunction {
	
	/**
	 * A univariate mathematical function as a String containing `x` as the 
	 * parameter. Supported expressions are described here:
	 * https://mathparser.org/mxparser-math-collection
	 * @return the function expression.
	 */
	String getFXExpression();
	
	
	/** The following methods are derived from the expression and are not serialized.
	 * They provide a convenient way to evaluate the function at runtime.
	 * @return the argument representing `x`.
	 */
	@JsonIgnore
	@Value.Derived default Argument getXArgument() {
		return new Argument("x");
	}
	
	/** The parsed expression ready for evaluation. This is derived from the `fxExpression` and is not serialized.
	 * It is configured to disable rounding to ensure accurate calculations.
	 * @return the parsed expression.
	 */
	@JsonIgnore
	@Value.Derived default Expression getParsed() {
		mXparser.disableUlpRounding();
		mXparser.disableCanonicalRounding();
		mXparser.disableAlmostIntRounding();
		Argument x = getXArgument(); 
		return new Expression(getFXExpression(), x);
	}
	
	/** Evaluates the function at a given value of `x`.
	 * @param x the value to evaluate the function at.
	 * @return the result of the function evaluation.
	 */
	default double value(double x) {
		Argument xArg = getXArgument();
		xArg.setArgumentValue(x);
		double result = getParsed().calculate();
		return result;
	}
	
}
