package io.github.ai4ci.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mariuszgromada.math.mxparser.License;

/**
 * Tests for {@link MathematicalFunction} behaviour: expression parsing, link
 * functions (NONE, LOG, LOGIT), bounds and out-of-range evaluation.
 *
 * Author: Rob Challen
 */
public class TestMathematicalFunction {

	static {
		License.iConfirmNonCommercialUse("rob.challen@bristol.ac.uk");
	}

	@Test
	public void testLinkFunctionLog() {
		MathematicalFunction mf = ImmutableMathematicalFunction.builder()
			.setFXExpression("0")
			.setMinimum(0.0)
			.setMaximum(1.0)
			.setLink(LinkFunction.LOG)
			.build();

		// fx == 0 on the link scale -> invFn(0) == exp(0) == 1
		assertEquals(
			Math.exp(0.0),
			mf.value(0.5),
			1e-9,
			"LOG link should invert 0 -> 1"
		);
	}

	@Test
	public void testLinkFunctionLogit() {
		MathematicalFunction mf = ImmutableMathematicalFunction.builder()
			.setFXExpression("0")
			.setMinimum(0.0)
			.setMaximum(1.0)
			.setLink(LinkFunction.LOGIT)
			.build();

		// fx == 0 on the logit scale -> invFn(0) == 0.5
		assertEquals(
			0.5,
			mf.value(0.25),
			1e-9,
			"LOGIT link should invert 0 -> 0.5"
		);
	}

	@Test
	public void testOutOfBoundsInterpolationDoesNotThrow() {
		MathematicalFunction mf = ImmutableMathematicalFunction.builder()
			.setFXExpression("x")
			.setMinimum(0.0)
			.setMaximum(2.0)
			.build();

		var below = mf.value(mf.getMinimum() - 1.0);
		var above = mf.value(mf.getMaximum() + 1.0);

		assertEquals(
			mf.value(mf.getMinimum()),
			below,
			"Out of range evaluation should return a finite value"
		);
		assertEquals(
			mf.value(mf.getMaximum()),
			above,
			"Out of range evaluation should return a finite value"
		);
	}

	@Test
	public void testSimpleExpressionParsing() {
		MathematicalFunction mf = ImmutableMathematicalFunction.builder()
			.setFXExpression("2*x+1")
			.setMinimum(0.0)
			.setMaximum(10.0)
			.build();

		assertEquals(
			5.0,
			mf.value(2.0),
			1e-9,
			"Simple linear expression should parse and evaluate correctly"
		);

		var xs = mf.getX();
		assertEquals(
			mf.getMinimum(),
			xs[0],
			1e-9,
			"getX first element == minimum"
		);
		assertEquals(
			mf.getMaximum(),
			xs[xs.length - 1],
			1e-9,
			"getX last element == maximum"
		);
	}
}