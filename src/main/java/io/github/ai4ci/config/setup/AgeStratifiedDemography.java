package io.github.ai4ci.config.setup;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.ImmutablePersonDemographic;
import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.functions.EmpiricalDistribution;
import io.github.ai4ci.functions.EmpiricalFunction;
import io.github.ai4ci.functions.LinkFunction;
import io.github.ai4ci.functions.SimpleFunction;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.functions.ImmutableEmpiricalDistribution;
import io.github.ai4ci.functions.ImmutableEmpiricalFunction;
import io.github.ai4ci.util.Sampler;

/**
 * Age stratified demographic configuration and helpers.
 *
 * <p>This interface describes a simple age stratified demography used during
 * setup and baselining. It supplies an empirical age distribution and a
 * function expressing the odds of contact as a function of age difference.
 * Implementations are used when creating person stubs and when computing
 * relationship strengths between agents.
 *
 * <p>Downstream uses include setup and builder code such as
 * {@link io.github.ai4ci.flow.builders.DefaultModelBuilder#setupOutbreak(io.github.ai4ci.abm.ModifiableOutbreak, io.github.ai4ci.config.setup.SetupConfiguration, io.github.ai4ci.util.Sampler)}
 * and per‑person baselining in
 * {@link io.github.ai4ci.flow.builders.DefaultPersonBaseliner#baselinePerson(io.github.ai4ci.abm.ImmutablePersonBaseline.Builder, io.github.ai4ci.abm.Person, io.github.ai4ci.util.Sampler)}.
 * The {@link #createPersonStub(Outbreak)} helper samples ages for new
 * {@link io.github.ai4ci.abm.Person} instances during setup.
 *
 * @author Rob Challen
 */
@Value.Immutable
@JsonSerialize(as = ImmutableAgeStratifiedDemography.class)
@JsonDeserialize(as = ImmutableAgeStratifiedDemography.class)
public interface AgeStratifiedDemography extends LocationAwareDemography {
 
	/**
	 * Sensible default age stratified demography used in examples and tests.
	 *
	 * <p>The default supplies a simple age distribution and an odds function
	 * that describes relative contact likelihood by age difference. Tests and
	 * examples may rely on this instance as a compact baseline.
	 */
	public static ImmutableAgeStratifiedDemography DEFAULT = 
			ImmutableAgeStratifiedDemography.builder()
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
							.setLink(LinkFunction.LOG)
							.build()
				)
				.setContactProximityBias(2.0)
				.build();
	
	/**
	 * The demographics of the population.
	 *
	 * @return an empirical cumulative distribution describing population ages
	 */
	EmpiricalDistribution getAgeDistribution();
	
	/**
	 * Odds ratio function for making a contact given absolute age difference.
	 *
	 * <p>The function returns a non‑negative odds ratio for a given absolute
	 * age difference; callers typically normalize or combine this function with
	 * other contact modifiers. If the implementation is an
	 * {@link EmpiricalFunction} it will be normalised by
	 * {@link #getNormalisedOddsContactFromAgeDifference()}.
	 *
	 * @return a function mapping age difference to odds ratio
	 */
	SimpleFunction getOddsContactFromAgeDifference();
	
	/**
	 * Normalised odds function for age difference.
	 *
	 * <p>When {@link #getOddsContactFromAgeDifference()} is an
	 * {@link EmpiricalFunction} this method returns a function that has been
	 * normalised against the joint age distribution. The normalisation
	 * produces a baseline odds function suitable for combining with other
	 * contact modifiers.
	 *
	 * @return a normalised odds function for age difference
	 */
	@JsonIgnore
	@Value.Default default SimpleFunction getNormalisedOddsContactFromAgeDifference() {
		if (getOddsContactFromAgeDifference() instanceof EmpiricalFunction) {
			return getAgeDistribution()
				.combine(getAgeDistribution(), (d1,d2) -> Math.abs(d1-d2))
				.getInterpolation()
				.baselineOdds(
					((EmpiricalFunction) getOddsContactFromAgeDifference())
				);
		}
		return getOddsContactFromAgeDifference();
	};
	
	/**
	 * Adjust a base contact probability by the age difference between two
	 * people.
	 *
	 * <p>This helper converts the supplied base probability {@code p} into an
	 * adjusted probability using the normalised age‑difference odds ratio. The
	 * odds ratio is applied using {@link Conversions#scaleProbabilityByOR} to
	 * preserve valid probability semantics.
	 *
	 * @param p base contact probability
	 * @param one source person
	 * @param two target person
	 * @return a probability adjusted for age-based contact odds
	 */
	default double adjustedProbabilityContact(double p, Person one, Person two ) {
		double age1 = one.getDemographic().getAge();
		double age2 = two.getDemographic().getAge();
		double diff = Math.abs(age1-age2);
		return Conversions.scaleProbabilityByOR(
				p,
				getNormalisedOddsContactFromAgeDifference().value(diff));
	}
	
	/**
	 * Create a modifiable person stub and populate the person's age from the
	 * configured age distribution.
	 *
	 * <p>This helper is a convenience used during outbreak setup when the
	 * builder needs to create a population quickly. Downstream callers
	 * include the setup stage in {@link io.github.ai4ci.flow.builders.DefaultModelBuilder}.
	 * The returned {@link ModifiablePerson} has an {@link ImmutablePersonDemographic}
	 * with an age sampled from {@link #getAgeDistribution()}.
	 *
	 * @param outbreak the owning outbreak used to attach the created person
	 * @return a modifiable person with a sampled age demographic
	 */
	default ModifiablePerson createPersonStub(Outbreak outbreak) {
		ModifiablePerson tmp = Person.createPersonStub(outbreak);
		tmp.setDemographic(
			ImmutablePersonDemographic.builder()
				.from(tmp.getDemographic())
				.setAge(this.getAgeDistribution().sample())
				.build()
		);
		return tmp;
	}
	
	/**
	 * Compute relationship strength between two people, incorporating age
	 * effects.
	 *
	 * <p>The default implementation delegates to
	 * {@link LocationAwareDemography#getRelationshipStrength(Person,Person,Sampler)}
	 * then scales the result by the age‑difference adjusted probability via
	 * {@link #adjustedProbabilityContact(double, Person, Person)}. This is
	 * the canonical place to combine location and demographic contact
	 * modifiers.
	 *
	 * @param source the source person
	 * @param target the target person
	 * @param sampler a sampler used for any stochastic elements in the base
	 *        relationship strength
	 * @return a relationship strength used as an odds/probability modifier
	 */
	default public double getRelationshipStrength(Person source, Person target, Sampler sampler) {
		return this.adjustedProbabilityContact(
				LocationAwareDemography.super.getRelationshipStrength(source,target,sampler), source, target
			);
	}
	

}