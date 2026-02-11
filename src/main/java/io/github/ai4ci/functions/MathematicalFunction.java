package io.github.ai4ci.functions;

import java.io.Serializable;

import org.immutables.value.Value;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.mXparser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A mathematical function that can be evaluated at runtime. This is used to
 * represent functions that are defined by the user as part of the configuration
 * of the model, such as the viral load to transmission probability mapping.
 *
 * <p>
 * The function is defined by a String expression containing `x` as the
 * parameter, which is parsed and evaluated using the mXparser library. The
 * parsed expression is derived from the original String and is not serialized,
 * ensuring that only the user-defined expression is stored in the
 * configuration.
 *
 * @see io.github.ai4ci.abm.OutbreakBaseline#transmissibilityFromViralLoad(double,
 *      double)
 */
@Value.Immutable
@JsonSerialize(as = ImmutableMathematicalFunction.class)
@JsonDeserialize(as = ImmutableMathematicalFunction.class)
public interface MathematicalFunction extends Serializable, SimpleFunction {

	/**
	 * A univariate mathematical function as a String containing `x` as the
	 * parameter. Supported expressions are described here:
	 * https://mathparser.org/mxparser-math-collection
	 *
	 * @return the function expression.
	 */
	String getFXExpression();

	/**
	 * The parsed expression ready for evaluation. This is derived from the
	 * `fxExpression` and is not serialized. It is configured to disable rounding
	 * to ensure accurate calculations.
	 *
	 * @return the parsed expression.
	 */
	@JsonIgnore @Value.Derived
	default Expression getParsed() {
		mXparser.disableUlpRounding();
		mXparser.disableCanonicalRounding();
		mXparser.disableAlmostIntRounding();
		var x = this.getXArgument();
		return new Expression(this.getFXExpression(), x);
	}

	/**
	 * The following methods are derived from the expression and are not
	 * serialized. They provide a convenient way to evaluate the function at
	 * runtime.
	 *
	 * @return the argument representing `x`.
	 */
	@JsonIgnore @Value.Derived
	default Argument getXArgument() { return new Argument("x"); }

	/**
	 * Evaluates the function at a given value of `x`.
	 *
	 * @param x the value to evaluate the function at.
	 * @return the result of the function evaluation.
	 */
	@Override
	default double value(double x) {
		var xArg = this.getXArgument();
		xArg.setArgumentValue(x);
		var result = this.getParsed().calculate();
		return result;
	}

}
