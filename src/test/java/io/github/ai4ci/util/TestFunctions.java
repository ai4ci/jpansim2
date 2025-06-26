package io.github.ai4ci.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class TestFunctions {

	@Test
	void testMathematical() {
		var tmp = ImmutableMathematicalFunction.builder()
			.setFXExpression("exp((x-45)/10*lg(2))")
			.build();
		
		IntStream.range(0,100).mapToDouble(i -> tmp.value((double) i))
			.forEach(System.out::println);;
	}

}
