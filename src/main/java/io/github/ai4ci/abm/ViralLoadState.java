package io.github.ai4ci.abm;

import org.immutables.value.Value;

import io.github.ai4ci.util.Sampler;
import static java.lang.Math.*;

import java.io.Serializable;

@Value.Immutable
public interface ViralLoadState extends Serializable {

	Person getPerson();
	int getVirions();
	int getVirionsProduced();
	int getTargetSusceptible();
	int getTargetExposed();
	int getTargetInfected();
	int getImmune();
	int getImmunePriming();
	int getImmuneActive();

	@Value.Derived
	default int getTargets() {
		return getPerson().getBaseline().getTargetCellCount();
	}

	@Value.Derived
	default int getTargetRemoved() {
		return getTargets() - getTargetSusceptible() - getTargetExposed() - getTargetInfected();
	}

	@Value.Derived
	/**
	 * The viral load is the number of newly produced virions, i.e. the total 
	 * virions in the person excluding those innoculating the person. Large 
	 * innoculations can't be excluded completely as they do not immediately 
	 * infect cells.
	 * @return
	 */
	default double getNormalisedViralLoad() {
		return ((double) getVirionsProduced()) /(double) this.model().getBaseline().getVirionsDiseaseCutoff();
	}
	
	@Value.Derived
	default double getNormalisedSeverity() {
		double baselinePercent = this.model().getBaseline().getTargetSymptomsCutoff();
		double baselineRate = -log(1-baselinePercent);
		double currentPercent = 
				((double) this.getTargets()-this.getTargetSusceptible()-this.getTargetExposed()) / 
				(double) this.getTargets();
		double currentRate = -log(1-currentPercent);
		return Math.max(0,currentRate/baselineRate);
	}

	@Value.Derived
	default int getImmuneDormant() {
		return getImmune() - getImmunePriming() - getImmuneActive();
	}

	static ViralLoadState initialise(Person person) {
		int targets = person.getBaseline().getTargetCellCount();
		return ImmutableViralLoadState.builder().setPerson(person)
			.setTargetSusceptible(targets)
			.setTargetExposed(0)
			.setTargetInfected(0)
			.setImmune((int) floor(
				targets * person.getBaseline().getImmuneTargetRatio()
						// When this is used the person is incomplete and the
						// state has not been built yet.
						// * person.getCurrentState().getImmuneModifier()
			))
			.setVirions(0)
			.setVirionsProduced(0)
			.setImmunePriming(0)
			.setImmuneActive(0)
			.build();
	}

	

	private Outbreak model() {
		return getPerson().getOutbreak();
	}

	/**
	 * Update the viral load for a person. 
	 * This depends on the PersonHistory being up to date which it should be based
	 * on the fact this is updated during the next update phase.
	 * @param rng (thread local RNG)
	 * @return
	 */
	default ViralLoadState update(Sampler rng) {
		
		int virionsDx = this.model().getBaseline().getVirionsDiseaseCutoff();
		
		int time = this.getPerson().getCurrentState().getTime();
		// This update function is called when the history has been updated, 
		// as part of the update to the patientState so
		// exposure will be as determined by current contact network.
		int virionExposure = (int) floor(
				(
					this.getPerson().getHistoryEntry(time)
						.map(ph -> ph.getVirionExposure())
						.orElse(0D)
					+
					this.getPerson().getCurrentState().getImportationExposure()
				) * virionsDx);
		
		int immunePriming = (int) floor(this.getPerson().getCurrentState().getImmunisationDose() * this.getImmuneDormant());
		
		// Overall modifiers are a number around 1:
		Double hostImmunity = getPerson().getCurrentState().getImmuneModifier();
		Double viralActivity = model().getCurrentState().getViralActivityModifier();
		
		// Viral factors are shared across the model
		Double rate_infection = model().getBaseline().getBaselineViralInfectionRate() * viralActivity;
		Double rate_virion_replication = pow(model().getBaseline().getBaselineViralReplicationRate(),viralActivity); // TODO: update
		Double rate_infected_given_exposed = rate_virion_replication;
		// Host factors
		
		Double ratio_immune_target = getPerson().getBaseline().getImmuneTargetRatio()*hostImmunity;
		Double rate_priming_given_infected = getPerson().getBaseline().getImmuneActivationRate()*hostImmunity; //user(1) 
		Double rate_active_given_priming = rate_priming_given_infected; 
		
		Double rate_neutralization = rate_virion_replication;
		Double rate_cellular_removal = rate_neutralization;
		
		Double rate_target_recovery = getPerson().getBaseline().getTargetRecoveryRate()*hostImmunity; //user(1/7); 
		
		Double rate_senescence_given_active = getPerson().getBaseline().getImmuneWaningRate()*(1.0/hostImmunity); //user(1/150)
		
		Double p_propensity_chronic = getPerson().getBaseline().getInfectionCarrierProbability(); //user(0)

		
		// Derived 
		Double p_neutralization = 1 - exp(-rate_neutralization * getImmuneActive() / (double) getImmune());
		Double p_infection = 1 - exp(-rate_infection * getTargetSusceptible() / (double) getTargets());

		Integer virions_added = rng.poisson(rate_virion_replication * getTargetInfected());
		Integer virions_neutralized = rng.binom(getVirions(), p_neutralization);
		Integer virions_infecting = rng.binom(getVirions() - virions_neutralized, p_infection);
		

//		  # virions_infecting is a sample size and targets_susceptible is a pool size #
//		  coverage of the pool by repeated sampling with replacement is given by: #
//		  p_target_infected = 1-(1-1/target_susceptible)^virions_infecting # this is
//		  not a standard way of doing this # because multiple viral particles can
//		  infect a single host but not vice versa # so a standard collision model is
//		  not applicable. #
//		  https://math.stackexchange.com/questions/32800/probability-distribution-of-
//		  coverage-of-a-set-after-x-independently-randomly/32816#32816
		
		// Targets - exposed
		Integer target_interacted = 
				(int) floor(
//					virions_infecting.doubleValue() * 
//					(1-
//						pow(
//							virions_infecting.doubleValue()/(1+virions_infecting.doubleValue()),
//							(double) getTargetSusceptible()
//						)
//					)
//					(double) getTargetSusceptible() * 
//					(1-
//						pow(
//							((double) getTargetSusceptible()-1)/((double) getTargetSusceptible()),
//							virions_infecting.doubleValue()
//						)
//					)
					(double) getTargetSusceptible() * 
					(1-
						exp( - virions_infecting.doubleValue() / ((double) getTargetSusceptible())	)
					)
				);
		Integer target_newly_exposed = rng.binom(target_interacted, p_infection);
		Double p_target_recovery = pFromRate(rate_target_recovery);
		Integer target_recovered = rng.binom(getTargetRemoved(), p_target_recovery);
		// Targets - infected
		Double p_infected_given_exposed = pFromRate(rate_infected_given_exposed);
		Double p_target_cellular_removal = pFromRate( rate_cellular_removal * getImmuneActive() / ((double) getTargets()));
		Integer target_exposed_removal = rng.binom(getTargetExposed(), (1-p_propensity_chronic) * p_target_cellular_removal); 
		Integer target_start_infected = rng.binom(getTargetExposed()-target_exposed_removal, p_infected_given_exposed);
		Integer target_infected_removed = rng.binom(getTargetInfected(), p_target_cellular_removal);

		// Immunity - priming
		Double p_priming_given_infected = pFromRate(rate_priming_given_infected * (getTargetExposed()+getTargetInfected()) / (double) getTargets());
		Integer immune_start_priming = rng.binom(getImmuneDormant(), p_priming_given_infected);
		Double p_active_given_priming = pFromRate(rate_active_given_priming);
		Integer immune_start_active = rng.binom(getImmunePriming(), p_active_given_priming);
		// Immunity - active
		Double p_senescence_given_active = pFromRate(rate_senescence_given_active);
		Integer immune_senescence = rng.binom(getImmuneActive(), p_senescence_given_active);
		
		if (immunePriming > this.getImmune()) immunePriming = this.getImmune();
		
		return ImmutableViralLoadState.builder().from(this)
				// Virions
				.setVirions(this.getVirions() - virions_neutralized - virions_infecting + virions_added + virionExposure)
				.setVirionsProduced(this.getVirions() - virions_neutralized - virions_infecting + virions_added)
				// Targets
				.setTargetSusceptible(getTargetSusceptible() - target_newly_exposed + target_recovered)
				.setTargetExposed(getTargetExposed() + target_newly_exposed - target_exposed_removal - target_start_infected)
				.setTargetInfected(getTargetInfected() + target_start_infected - target_infected_removed)
				// Immune
				.setImmune((int) floor(getTargets()*ratio_immune_target) - immunePriming)
				.setImmunePriming(getImmunePriming() + immune_start_priming - immune_start_active + immunePriming)
				.setImmuneActive(getImmuneActive()+ immune_start_active - immune_senescence)
				.build();
	}
	
	private double pFromRate(double rate) {
		if (rate < 0) rate = 0;
		return 1-exp(-rate);
	}
	
}
