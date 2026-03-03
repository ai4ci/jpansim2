package io.github.ai4ci.functions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.statistics.distribution.BetaDistribution;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.LogNormalDistribution;
import org.apache.commons.statistics.distribution.NormalDistribution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.ai4ci.util.Sampler;

public class TestEmpiricalDistribution {

	static Stream<Arguments> linkFunctionTestCases() {
		return Stream.of(
			// Test Case 1: Identity link on Normal(50, 15) truncated to [0,120]
			Arguments.of(
				LinkFunction.NONE,
				DoubleStream.iterate(20, d -> d < 80, d -> d + 5)
					.toArray(),
				NormalDistribution.of(50, 5)
			),
			// Test Case 2: Log link on LogNormal(3, 0.5) — income-like
			Arguments.of(
				LinkFunction.LOG,
				DoubleStream.iterate(10, d -> d < 160, d -> d + 1)
					.toArray(),
				LogNormalDistribution.of(3, 0.25)
			),
			// Test Case 3: Logit link on Beta(2,5) scaled to [0,1]
			Arguments.of(
				LinkFunction.LOGIT,
				DoubleStream.iterate(0, d -> d < 1, d -> d + 0.01)
					.toArray(),
				BetaDistribution.of(2.0, 5.0)
			)
		);
	}

	@ParameterizedTest @MethodSource("linkFunctionTestCases")
	void testBuilderLinkFunctions(
			LinkFunction link, double[] x, ContinuousDistribution ref
	) {
		// Build distribution
		var Fx = Arrays.stream(x)
			.map(d -> ref.cumulativeProbability(d))
			.toArray();

		var min = ref.inverseCumulativeProbability(0.0001);
		var max = ref.inverseCumulativeProbability(0.9999);

		var dist = ImmutableEmpiricalDistribution
			.builder()
			.setLink(link)
			.setX(x)
			.setCumulativeProbability(Fx)
			.setMinimum(min)
			.setMaximum(max)
			.build();

		doTests(dist, ref, 1);

		var data = ref.createSampler(Sampler.getSampler())
			.samples()
			.limit(10000)
			.toArray();
		var dist2 = EmpiricalDistribution
			.fromData(link, data);
		doTests(dist2, ref, 2);
	}

	static void doTests(
			EmpiricalDistribution dist, ContinuousDistribution ref, double relTol
	) {
		System.out.println(ref);
		var expectedMedian = ref.inverseCumulativeProbability(0.5);
		var expectedMean = ref.getMean();
		var tol = ref.inverseCumulativeProbability(0.52)
				- ref.inverseCumulativeProbability(0.48);
		tol = tol * relTol;
		var ptol = 0.05 * relTol;
		var min = dist.getMinimum();
		var max = dist.getMaximum();
		// Test: CDF at median ≈ 0.5
		assertThat(dist.getCumulative(expectedMedian))
			.isCloseTo(0.5, byLessThan(ptol));

		// Test: getMedian() ≈ expected median
		assertThat(dist.getMedian()).isCloseTo(expectedMedian, byLessThan(tol));

		// Test: Monotonicity of CDF
		var step = (max - min) / 1000;
		var testX = DoubleStream
			.iterate(min + step, d -> d < max - step, d -> d + step)
			.toArray();

		var prev = 0.0;
		for (double xi : testX) {
			var cdf = dist.getCumulative(xi);
			assertThat(cdf).isGreaterThanOrEqualTo(prev);
			var refCdf = ref.cumulativeProbability(xi);
			assertThat(cdf).isCloseTo(refCdf, byLessThan(ptol));
			prev = cdf;
		}

		// Test: Quantiles

		var testP = DoubleStream
			.of(0.025, 0.05, 0.10, 0.25, 0.5, 0.75, 0.90, 0.95, 0.975)
			.toArray();
		for (double yi : testP) {
			var q = dist.getQuantile(yi);
			var refq = ref.inverseCumulativeProbability(yi);
			var qtol1 = ref.inverseCumulativeProbability(yi - 0.025);
			var qtol2 = ref.inverseCumulativeProbability(yi + 0.025);
			var diff = qtol2 - qtol1;
			assertThat(q).describedAs("yi: " + yi + " true: " + refq)
				.isCloseTo(refq, byLessThan(diff));
			// .isLessThan(qtol2).isGreaterThan(qtol1);
		}

		var testX2 = DoubleStream.of(0.05, 0.10, 0.25, 0.5, 0.75, 0.90, 0.95)
			.map(d -> ref.inverseCumulativeProbability(d))
			.toArray();

		for (double xi : testX2) {
			var pdf = dist.getDensity(xi);
			var refPdf = ref.density(xi);
			System.out.println(refPdf + " " + pdf);
			// assertThat(pdf).describedAs("xi: "+xi).isCloseTo(refPdf,
			// byLessThan(ptol));
		}

		// Test density integrates to 1
		var tmp = new RombergIntegrator(
				0.0001, 0.0001, RombergIntegrator.DEFAULT_MIN_ITERATIONS_COUNT,
				RombergIntegrator.ROMBERG_MAX_ITERATIONS_COUNT
		);
		var integral = tmp
			.integrate(100000, x -> dist.getDensity(x), min, max);
		assertThat(integral).isCloseTo(1, byLessThan(0.001));

		// Test: Central tendency near expected mean
		assertThat(dist.getMean()).isCloseTo(expectedMean, byLessThan(tol));
	}

}