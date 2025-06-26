package io.github.ai4ci.config.setup;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.ImmutablePersonDemographic;
import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.mechanics.Abstraction.SimpleFunction;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.EmpiricalDistribution;
import io.github.ai4ci.util.EmpiricalFunction;
import io.github.ai4ci.util.ImmutableEmpiricalDistribution;
import io.github.ai4ci.util.ImmutableEmpiricalFunction;
import io.github.ai4ci.util.LinkFunction;
import io.github.ai4ci.util.Sampler;

@Value.Immutable
@JsonSerialize(as = ImmutableAgeStratifiedDemography.class)
@JsonDeserialize(as = ImmutableAgeStratifiedDemography.class)
public interface AgeStratifiedDemography extends LocationAwareDemography {
 
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
	
	EmpiricalDistribution getAgeDistribution();
	SimpleFunction getOddsContactFromAgeDifference();
	
	@JsonIgnore
	@Value.Default default SimpleFunction getNormalisedOddsContactFromAgeDifference() {
		if (getOddsContactFromAgeDifference() instanceof EmpiricalFunction) {
			return ((EmpiricalFunction) getOddsContactFromAgeDifference()).normalise(
					getAgeDistribution().combine(getAgeDistribution(), (d1,d2) -> Math.abs(d1-d2)).getInterpolation() 
			);
		}
		return getOddsContactFromAgeDifference();
	};
	
	
	/**
	 * Compared to an average relationship strength of 1 how close is the 
	 * relationship between two people, expressed as an odds ratio. I.e.
	 * an OR of 2 means that the odds of the contact is doubled. The underlying
	 * probability of the contact is still random but this will change the 
	 * distribution depending on the contact individual.   
	 */
	default double adjustedProbabilityContact(double p, Person one, Person two ) {
		double age1 = one.getDemographic().getAge();
		double age2 = two.getDemographic().getAge();
		double diff = Math.abs(age1-age2);
		return Conversions.scaleProbabilityByOR(
				p,
				getNormalisedOddsContactFromAgeDifference().value(diff));
	}
	
	
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
	
	default public double getRelationshipStrength(Person source, Person target, Sampler sampler) {
		return this.adjustedProbabilityContact(
				LocationAwareDemography.super.getRelationshipStrength(source,target,sampler), source, target
			);
	}
	
	

}
