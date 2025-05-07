package io.github.ai4ci.config;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.Data.Partial;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.abm.mechanics.Abstraction.Interpolator;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.EmpiricalDistribution;
import io.github.ai4ci.util.EmpiricalFunction;
import io.github.ai4ci.util.EmpiricalFunction.Link;
import io.github.ai4ci.util.ImmutableEmpiricalDistribution;
import io.github.ai4ci.util.ImmutableEmpiricalFunction;

@Value.Immutable
@JsonSerialize(as = ImmutableAgeStratifiedNetworkConfiguration.class)
@JsonDeserialize(as = ImmutableAgeStratifiedNetworkConfiguration.class)
public interface AgeStratifiedNetworkConfiguration extends SetupConfiguration {
 
	@Partial @Value.Immutable
	@JsonSerialize(as = PartialAgeStratifiedNetworkConfiguration.class)
	@JsonDeserialize(as = PartialAgeStratifiedNetworkConfiguration.class)
	public interface _PartialAgeStratifiedNetworkConfiguration extends AgeStratifiedNetworkConfiguration, Abstraction.Modification<AgeStratifiedNetworkConfiguration> {
		default _PartialAgeStratifiedNetworkConfiguration self() {return this;}
	}
	
	public interface Builder extends SetupConfiguration.Builder {}
	
	public static ImmutableAgeStratifiedNetworkConfiguration DEFAULT = 
			ImmutableAgeStratifiedNetworkConfiguration.builder()
				.setInitialImports(5)
				.setName("age-stratified")
				.setNetworkSize(10000)
				.setNetworkRandomness(0.1)
				.setNetworkConnectedness(100)
				.setAgeDistribution(
						ImmutableEmpiricalDistribution.builder()
							.setMinimum(0)
							.setMaximum(120)
							.setX(18,45,65,85)
							.setCumulativeProbability(0.1,0.5,0.75,0.9)
							.build()
						)
				.setOddsContactFromAgeDifference(
						ImmutableEmpiricalFunction.builder()
						.setX(0,10,25,40,60,70)
						.setY(2, 0.5,1.5,0.5,1,0.5)
						.setLink(Link.LOG)
						.build()
				)
				.setOddsMobilityFromAge(
						ImmutableEmpiricalFunction.builder()
						.setX(0,5,15,25,45,75)
						.setY(2,1,1.5,1.25,0.8,1.2)
						.setLink(Link.LOG)
						.build()
				)
				.setOddsTransmissionFromAge(FixedValueFunction.ofOne())
				.setOddsComplianceFromAge(FixedValueFunction.ofOne())
				.setOddsAppUseFromAge(FixedValueFunction.ofOne())
				.setOddsMaximumSocialContactReductionFromAge(FixedValueFunction.ofOne())
				.setScaleIncubationPeriodFromAge(FixedValueFunction.ofOne())
				.setScaleRecoveryPeriodFromAge(
						ImmutableEmpiricalFunction.builder()
						.setX(0,5,15,25,45,65)
						.setY(1,0.5,0.5,0.75,1,2)
						.setLink(Link.LOG)
						.build()
				)
				.setOddsSevereOutcomeFromAge(
						ImmutableEmpiricalFunction.builder()
						.setX(0,5,15,25,45,65,85)
						.setY(1,0.5,0.5,0.75,1,2,4)
						.setLink(Link.LOG)
						.build()
				)
				.build();
	
	EmpiricalDistribution getAgeDistribution();
	int getNetworkConnectedness();
	double getNetworkRandomness();
	
	Abstraction.Interpolator getOddsContactFromAgeDifference();
	Abstraction.Interpolator getOddsMobilityFromAge();
	Abstraction.Interpolator getOddsTransmissionFromAge();
	Abstraction.Interpolator getOddsComplianceFromAge();
	Abstraction.Interpolator getOddsAppUseFromAge();
	Abstraction.Interpolator getOddsMaximumSocialContactReductionFromAge();
	
	Abstraction.Interpolator getScaleIncubationPeriodFromAge();
	Abstraction.Interpolator getScaleRecoveryPeriodFromAge();
	Abstraction.Interpolator getOddsSevereOutcomeFromAge();
	
	@JsonIgnore
	@Value.Default default Interpolator getNormalisedOddsContactFromAgeDifference() {
		if (getOddsContactFromAgeDifference() instanceof EmpiricalFunction) {
			return ((EmpiricalFunction) getOddsContactFromAgeDifference()).normalise(
					getAgeDistribution().combine(getAgeDistribution(), (d1,d2) -> Math.abs(d1-d2)).getInterpolation() 
			);
		}
		return getOddsContactFromAgeDifference();
	};
	
	private Interpolator normalise(Interpolator fn) {
		if (fn instanceof EmpiricalFunction) {
			return ((EmpiricalFunction) fn).normalise(getAgeDistribution());
		}
		return fn;
	}
	
	@JsonIgnore
	@Value.Default default Interpolator getNormalisedOddsMobilityFromAge() {
		return normalise(getOddsMobilityFromAge());
	};
	
	@JsonIgnore
	@Value.Default default Interpolator getNormalisedOddsTransmissionFromAge() {
		return normalise(getOddsTransmissionFromAge());
	};
	
	@JsonIgnore
	@Value.Default default Interpolator getNormalisedOddsComplianceFromAge() {
		return normalise(getOddsComplianceFromAge());
	};
	
	@JsonIgnore
	@Value.Default default Interpolator getNormalisedOddsAppUseFromAge() {
		return normalise(getOddsAppUseFromAge());
	};
	
	@JsonIgnore
	@Value.Default default Interpolator getNormalisedOddsMaximumSocialContactReductionFromAge() {
		return normalise(getOddsMaximumSocialContactReductionFromAge());
	};
	
	@JsonIgnore
	@Value.Default default Interpolator getNormalisedScaleIncubationPeriodFromAge() {
		return normalise(getScaleIncubationPeriodFromAge());
	};
	
	@JsonIgnore
	@Value.Default default Interpolator getNormalisedScaleRecoveryPeriodFromAge() {
		return normalise(getScaleRecoveryPeriodFromAge());
	};
	
	@JsonIgnore
	@Value.Default default Interpolator getNormalisedOddsSevereOutcomeFromAge() {
		return normalise(getOddsSevereOutcomeFromAge());
	};
	
	/**
	 * Compared to an average relationship strength of 1 how close is the 
	 * relationship between two people, expressed as an odds ratio. I.e.
	 * an OR of 2 means that the odds of the contact is doubled. The underlying
	 * probability of the contact is still random but this will change the 
	 * distribution depending on the contact individual.   
	 * @param p
	 * @param one
	 * @param two
	 * @return
	 */
	default double adjustedProbabilityContact(double p, Person one, Person two ) {
		double age1 = one.getDemographic().getAge();
		double age2 = two.getDemographic().getAge();
		double diff = Math.abs(age1-age2);
		return Conversions.scaleProbabilityByOR(
				p,
				getNormalisedOddsContactFromAgeDifference().interpolate(diff));
	}
	
	private double adjust(double p, Interpolator ageFn, Person person) {
		if (person == null) return p;
		return Conversions.scaleProbabilityByOR(p, 
				ageFn.interpolate(person.getDemographic().getAge())
		);
	}
	
	private double scale(double p, Interpolator ageFn, Person person) {
		if (person == null) return p;
		return p*ageFn.interpolate(person.getDemographic().getAge());
	}
	
	default double adjustedMobilityBaseline(double p, Person person) {
		return adjust(p, getNormalisedOddsMobilityFromAge(), person);
	}
	
	default double adjustedTransmissionFromAge(double p, Person person) {
		return adjust(p, getNormalisedOddsTransmissionFromAge(), person);
	}
	
	default double adjustedComplianceFromAge(double p, Person person) {
		return adjust(p, getNormalisedOddsComplianceFromAge(), person);
	}
	
	default double adjustedAppUseFromAge(double p, Person person) {
		return adjust(p, getNormalisedOddsAppUseFromAge(), person);
	}
	
	default double adjustedMaximumSocialContactReductionFromAge(double p, Person person) {
		return adjust(p, getNormalisedOddsMaximumSocialContactReductionFromAge(), person);
	}
	
	default double adjustIncubationPeriodFromAge(double p, Person person) {
		return scale(p, getNormalisedScaleIncubationPeriodFromAge(), person);
	}
	
	default double adjustRecoveryPeriodFromAge(double p, Person person) {
		return scale(p, getNormalisedScaleRecoveryPeriodFromAge(), person);
	}
	
	default double adjustApproxPeakViralLoadFromAge(double p, Person person) {
		return adjust(p, getNormalisedOddsSevereOutcomeFromAge(), person);
	}
}
