package io.github.ai4ci.abm.mechanics;

import io.github.ai4ci.abm.ImmutableOutbreakBaseline;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.util.Sampler;
import io.github.ai4ci.util.ThreadSafeArray;

public abstract class AbstractModelBuilder {

	public ModifiableOutbreak doSetupOutbreak(ModifiableOutbreak outbreak, SetupConfiguration config, Sampler sampler) {
		if (config == null) throw new RuntimeException("Outbreak is not configured correctly for setup.");
		outbreak.setPeople(new ThreadSafeArray<Person>(Person.class, config.getNetworkSize()));
		outbreak.setSetupConfiguration( config );
		return setupOutbreak(outbreak,config,sampler);
	}
	
	public ImmutableOutbreakBaseline doBaselineOutbreak(ImmutableOutbreakBaseline.Builder builder, Outbreak o , Sampler sampler) {
		if (o instanceof ModifiableOutbreak) {
			ModifiableOutbreak m = (ModifiableOutbreak) o;
			if(
					!m.getPeople().isEmpty() &&
					m.initialisedSetupConfiguration() &&
					m.initialisedExecutionConfiguration() &&
					m.initialisedSocialNetwork() &&
					isBaselined(m.getPeople())
				) {
				return baselineOutbreak(builder,o,sampler);
			}
		} 
		throw new RuntimeException("Outbreak is not configured correctly for baselining.");
	} 
	
	private boolean isBaselined(ThreadSafeArray<Person> people) {
		if (people.isEmpty()) return false;
		return ((ModifiablePerson) people.get(0)).initialisedBaseline();
	};

	public ImmutablePersonBaseline doBaselinePerson(ImmutablePersonBaseline.Builder builder, Person p, Sampler sampler) {
		// Filter only to people that have been fully initialised
		if (p.getOutbreak() instanceof ModifiableOutbreak) {
			ModifiableOutbreak m = (ModifiableOutbreak) p.getOutbreak();
			if (m.initialisedSetupConfiguration() &&
					m.initialisedExecutionConfiguration() &&
					m.initialisedSocialNetwork()
			) {
				return baselinePerson(builder,p,sampler);
			}
		} 
		throw new RuntimeException("Person is not configured correctly for baselining.");
	}
	
	public ImmutableOutbreakState doInitialiseOutbreak(ImmutableOutbreakState.Builder builder, Outbreak o , Sampler sampler) {
		if (o instanceof ModifiableOutbreak) {
			ModifiableOutbreak m = (ModifiableOutbreak) o;
			if ( 
					m.initialisedBaseline() &&
					m.initialisedSetupConfiguration() &&
					m.initialisedExecutionConfiguration() &&
					m.initialisedSocialNetwork()
			) {
				return initialiseOutbreak(builder,o,sampler);
			}
		} 
		throw new RuntimeException("Outbreak is not configured correctly for initialisation."); 
	}
	
	public ImmutablePersonState doInitialisePerson(ImmutablePersonState.Builder builder, Person p, Sampler sampler) {
					if (p instanceof ModifiablePerson) {
						ModifiablePerson mp = (ModifiablePerson) p;
						if (p.getOutbreak() instanceof ModifiableOutbreak) {
							ModifiableOutbreak m = (ModifiableOutbreak) p.getOutbreak();
							if (
									mp.initialisedBaseline() &&
									m.initialisedSetupConfiguration() &&
									m.initialisedExecutionConfiguration() &&
									m.initialisedSocialNetwork()
							) {
								return initialisePerson(builder,p,sampler);
							}
						} 
					}
					throw new RuntimeException("Person is not configured correctly for initialisation.");
				}
	
	

	public abstract ModifiableOutbreak setupOutbreak(ModifiableOutbreak outbreak, SetupConfiguration config, Sampler sampler);
	
	public abstract ImmutableOutbreakBaseline baselineOutbreak(ImmutableOutbreakBaseline.Builder builder, Outbreak outbreak, Sampler sampler);
	
	public abstract ImmutablePersonBaseline baselinePerson(ImmutablePersonBaseline.Builder builder, Person person, Sampler rng);

	public abstract ImmutablePersonState initialisePerson(ImmutablePersonState.Builder builder, Person person, Sampler rng);

	public abstract ImmutableOutbreakState initialiseOutbreak(ImmutableOutbreakState.Builder builder, Outbreak outbreak, Sampler sampler);

	
}
