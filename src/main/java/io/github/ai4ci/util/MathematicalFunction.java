package io.github.ai4ci.util;
import org.mariuszgromada.math.mxparser.*;
import java.io.Serializable;
import org.immutables.value.Value;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.mechanics.Abstraction;

@Value.Immutable
@JsonSerialize(as = ImmutableMathematicalFunction.class)
@JsonDeserialize(as = ImmutableMathematicalFunction.class)
public interface MathematicalFunction extends Serializable, Abstraction.SimpleFunction {
	
	/**
	 * A univariate mathematical function as a String containing `x` as the 
	 * parameter. Supported expressions are described here:
	 * https://mathparser.org/mxparser-math-collection
	 * @return the function expression.
	 */
	String getFXExpression();
	
	static class LicenseHolder { 
		static {
			License.iConfirmNonCommercialUse("rob.challen@bristol.ac.uk");
		}
	}
	
	@JsonIgnore
	@Value.Derived default Argument getXArgument() {
		return new Argument("x");
	}
	
	@JsonIgnore
	@Value.Derived default Expression getParsed() {
		mXparser.disableUlpRounding();
		mXparser.disableCanonicalRounding();
		mXparser.disableAlmostIntRounding();
		Argument x = getXArgument(); 
		return new Expression(getFXExpression(), x);
	}
	
	default double value(double x) {
		Argument xArg = getXArgument();
		xArg.setArgumentValue(x);
		double result = getParsed().calculate();
		return result;
	}
	
}
