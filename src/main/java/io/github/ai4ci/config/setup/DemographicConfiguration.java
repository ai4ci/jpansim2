package io.github.ai4ci.config.setup;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.OptBoolean;

import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.functions.EmpiricalDistribution;
import io.github.ai4ci.util.ExoticDistributions;
import io.github.ai4ci.util.Sampler;

/**
 * Supertype of demographic configuration strategies.
 *
 * <p>This interface declares the methods required by model builders to
 * construct persons and to compute relationship strengths between agents.
 * Concrete subtypes provide different levels of demographic detail such as
 * unstratified, age stratified or location aware demographies.
 *
 * <p>Downstream consumers include the setup stage in
 * {@link io.github.ai4ci.flow.builders.DefaultModelBuilder} (which creates
 * person stubs and places agents) and the person baseliner which reads
 * demographic attributes when building per‑person baselines.
 *
 * <p>Extension guidance: add new demographic styles by introducing a new
 * subtype and registering it in the Jackson polymorphic mapping. Keep the
 * public contract stable: builders rely on {@link #createPersonStub} and
 * {@link #getRelationshipStrength}.</p>
 *
 * @author Rob Challen
 */
@JsonTypeInfo(use = Id.NAME, requireTypeIdForSubtypes = OptBoolean.TRUE)
@JsonSubTypes( {
    @Type(value = ImmutableUnstratifiedDemography.class, name = "unstratified"), 
    @Type(value = ImmutableAgeStratifiedDemography.class, name = "age-stratified"),
    @Type(value = ImmutableLocationAwareDemography.class, name = "location-aware") 
} )
public interface DemographicConfiguration extends Serializable {

    /**
     * Create a modifiable person stub attached to the supplied outbreak.
     *
     * @param outbreak owning outbreak used to attach the created person
     * @return a modifiable person stub
     */
    ModifiablePerson createPersonStub(Outbreak outbreak);
    /**
     * Compute a relationship strength between two people used as an odds
     * modifier when composing contact probabilities.
     *
     * @param source the source person
     * @param target the target person
     * @param sampler a random sampler for stochastic choices
     * @return a numeric relationship strength
     */
    double getRelationshipStrength(Person source, Person target, Sampler sampler);
    
    /*
     * Used internally to get the quantile of distance between 2 people in the 
     * network. 
     * @return a distribution of distances between random points in the space
     * 
     */
    @JsonIgnore
    @Value.Lazy default EmpiricalDistribution getEuclidianDistanceCDF() {
        return ExoticDistributions.getEuclidianDistanceDistribution();
    }
    
    /**
     * A distance measure from 0 (distant) to 1 (identity) based on the quantile
     * of the euclidian distance in this persons position versus the distribution 
     * of distances between location of random points in the space.   
     *
     * @param one first person
     * @param two second person
     * @return proximity measure between 0 and 1
     */
    default double getProximity(Person one, Person two) {
        var dist = Math.sqrt(
            Math.pow(one.getDemographic().getLocationX()-two.getDemographic().getLocationX(),2)+
            Math.pow(one.getDemographic().getLocationY()-two.getDemographic().getLocationY(),2)
        ) / Math.sqrt(2.0);
        return 1 - getEuclidianDistanceCDF().getCumulative(dist);
    }
}