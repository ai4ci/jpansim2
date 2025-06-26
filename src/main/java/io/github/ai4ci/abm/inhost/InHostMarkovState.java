package io.github.ai4ci.abm.inhost;

import java.io.Serializable;

import org.apache.commons.lang3.tuple.Pair;
import org.immutables.value.Value;

import io.github.ai4ci.abm.inhost.ImmutableInHostMarkovState.Builder;
import io.github.ai4ci.config.inhost.MarkovStateModel;
import io.github.ai4ci.util.Sampler;

@Value.Immutable
public interface InHostMarkovState extends InHostModelState<MarkovStateModel> {

	// MarkovStateModel getConfig();
	int getTime();
	
	// these next 3 are here because there is no easy way to access them within
	// the top level experiment configuration outside of the config stage so
	// we have to copy them. 
	double getInfectionCaseRate();
	double getInfectionHospitalisationRate();
	double getInfectionFatalityRate();
	
	DiseaseState getDiseaseState();
	SymptomState getSymptomState();
	@Value.Redacted InHostMarkovStateMachine getMachine();
	
	public static enum DiseaseState {
		SUSCEPTIBLE, EXPOSED, INFECTIOUS, IMMUNE
	}
	
	public static enum SymptomState {
		ASYMPTOMATIC, SYMPTOMATIC, HOSPITALISED, DEAD
	}
	
	@Value.Immutable
	public static interface InHostMarkovStateMachine extends Serializable {
		
		double getPExposedInfectious();
		// double getPExposedSusceptible();
		double getPInfectiousImmune();
		double getPImmuneSusceptible();
		
		/**
		 * this is the per day probability of becoming symptomatic given that
		 * you are infected. it is 1-(1-infection_case_rate)^duration_of_infection 
		 * @return
		 */
		double getPAsymptomaticSymptomatic();
		
		
		double getPSymptomaticAsymptomatic();
		double getPSymptomaticHospitalised();
		double getPSymptomaticDead();
		double getPHospitalisedDead();
		double getPHospitalisedAsymptomatic();
		
		default DiseaseState updateDiseaseState(DiseaseState current, Sampler rng) {
			switch (current) {
				case EXPOSED:
					return rng.bern(
						this.getPExposedInfectious(), DiseaseState.INFECTIOUS
					// This is disabled because partial transmission is handles before an
					// individual gets exposed. It is a fundamental part of the contact process
					// and in this model immunity is binary and a disease state so 
					// we don't get a path from exposed to susceptible because of that.
					// return rng.multinom(
					//		Pair.of(this.getPExposedInfectious(), DiseaseState.INFECTIOUS) ,
					//		Pair.of(this.getPExposedSusceptible(), DiseaseState.SUSCEPTIBLE)
					).orElse(DiseaseState.EXPOSED);
				
				case INFECTIOUS:
					return rng.bern(this.getPInfectiousImmune(), DiseaseState.IMMUNE)
							.orElse(DiseaseState.INFECTIOUS);
				case IMMUNE:
					return rng.bern(this.getPImmuneSusceptible(), DiseaseState.SUSCEPTIBLE)
							.orElse(DiseaseState.IMMUNE);
				case SUSCEPTIBLE:
					return DiseaseState.SUSCEPTIBLE;
				default:
					throw new RuntimeException();
			}
		}
		
		default SymptomState updateSymptomState(SymptomState current, DiseaseState dx, Sampler rng) {
			switch (current) {
			case ASYMPTOMATIC:
				if (dx.equals(DiseaseState.INFECTIOUS)) {
					return rng.bern(this.getPAsymptomaticSymptomatic(), SymptomState.SYMPTOMATIC)
							.orElse(SymptomState.ASYMPTOMATIC);
				}
				return SymptomState.ASYMPTOMATIC;
			case SYMPTOMATIC:
				return rng.multinom(
						Pair.of(this.getPSymptomaticAsymptomatic(), SymptomState.ASYMPTOMATIC),
						Pair.of(this.getPSymptomaticHospitalised(), SymptomState.HOSPITALISED),
						Pair.of(this.getPSymptomaticDead(), SymptomState.DEAD)
				).orElse(SymptomState.SYMPTOMATIC);
			case HOSPITALISED:
				return rng.multinom(
						Pair.of(this.getPHospitalisedAsymptomatic(), SymptomState.ASYMPTOMATIC),
						Pair.of(this.getPHospitalisedDead(), SymptomState.DEAD)
				).orElse(SymptomState.HOSPITALISED);
			case DEAD:
				return SymptomState.DEAD;
			default:
				throw new RuntimeException();
			}
		}
	}
	
	@Override
	default double getNormalisedViralLoad() {
		if (this.getDiseaseState().equals(DiseaseState.INFECTIOUS)) return 1.5D;
		if (this.getDiseaseState().equals(DiseaseState.EXPOSED)) return 0.5D;
		return 0D;
	}

	@Override
	@Value.Derived
	default double getNormalisedSeverity() {
		Sampler rng = Sampler.getSampler();
		switch (this.getSymptomState()) {
		case ASYMPTOMATIC:
			if (!this.getDiseaseState().equals(DiseaseState.INFECTIOUS)) return 0;
			return rng.uniform(0,1-this.getInfectionCaseRate());
		case SYMPTOMATIC:
			return rng.uniform(
				1-this.getInfectionCaseRate(),
				1-this.getInfectionHospitalisationRate()
			);
		case HOSPITALISED:
			return rng.uniform(
				1-this.getInfectionHospitalisationRate(),
				1-this.getInfectionFatalityRate()
			);
		case DEAD:
			return rng.uniform(
				1-this.getInfectionFatalityRate(),
				1
			);
		default:
			throw new RuntimeException();
		}
	}

	@Override
	default double getImmuneActivity() {
		return this.getDiseaseState().equals(DiseaseState.IMMUNE) ? 1D : 0D;
	}
	
	@Override
	default boolean isInfected() {
		return this.getDiseaseState().equals(DiseaseState.INFECTIOUS);
	}

	@Override
	default InHostMarkovState update(Sampler sampler, double virionExposure,
			double immunisationDose) {
		
		DiseaseState next;
		if (this.getDiseaseState().equals(DiseaseState.SUSCEPTIBLE)) {
			if (virionExposure > 0) next = DiseaseState.EXPOSED;
			else if (immunisationDose > 0) next = DiseaseState.IMMUNE;
			else next = this.getMachine().updateDiseaseState(getDiseaseState(), sampler);
		} else {
			next = this.getMachine().updateDiseaseState(getDiseaseState(), sampler);
		}
		
		Builder builder = ImmutableInHostMarkovState.builder().from(this);
		return builder
			.setDiseaseState(next)
			.setSymptomState(
				this.getMachine().updateSymptomState(getSymptomState(), getDiseaseState(), sampler)
			)
			.setTime(getTime()+1)
			.build();
			
		
	}

}
