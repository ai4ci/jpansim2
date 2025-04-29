package io.github.ai4ci.abm.builders;

import io.github.ai4ci.abm.ImmutableOutbreakBaseline;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.config.AgeStratifiedNetworkConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.config.WattsStrogatzConfiguration;
import io.github.ai4ci.util.Sampler;
import io.github.ai4ci.util.ThreadSafeArray;

public abstract class AbstractModelBuilder {

	public void doSetupOutbreak(ModifiableOutbreak outbreak, SetupConfiguration config, Sampler sampler) {
		if (config == null) throw new RuntimeException("Outbreak is not configured correctly for setup.");
		outbreak.setPeople(new ThreadSafeArray<Person>(Person.class, config.getNetworkSize()));
		outbreak.setSetupConfiguration( config );
		setupOutbreak(outbreak,config,sampler);
	}
	
	public void doBaselineOutbreak(ImmutableOutbreakBaseline.Builder builder, Outbreak o , Sampler sampler) {
		if (o instanceof ModifiableOutbreak) {
			ModifiableOutbreak m = (ModifiableOutbreak) o;
			if(
					!m.getPeople().isEmpty() &&
					m.initialisedSetupConfiguration() &&
					m.initialisedExecutionConfiguration() &&
					m.initialisedSocialNetwork() &&
					isBaselined(m.getPeople())
				) {
				baselineOutbreak(builder,o,sampler);
				return;
			}
		} 
		throw new RuntimeException("Outbreak is not configured correctly for baselining.");
	} 
	
	private boolean isBaselined(ThreadSafeArray<Person> people) {
		if (people.isEmpty()) return false;
		return ((ModifiablePerson) people.get(0)).initialisedBaseline();
	};

	public void doBaselinePerson(ImmutablePersonBaseline.Builder builder, Person p, Sampler sampler) {
		// Filter only to people that have been fully initialised
		// TODO: realistically this should throw exceptions
		// rather that skip the setup if it is not ready
		if (p.getOutbreak() instanceof ModifiableOutbreak) {
			ModifiableOutbreak m = (ModifiableOutbreak) p.getOutbreak();
			if (m.initialisedSetupConfiguration() &&
					m.initialisedExecutionConfiguration() &&
					m.initialisedSocialNetwork()
			) {
				baselinePerson(builder,p,sampler);
				return;
			}
		} 
		throw new RuntimeException("Person is not configured correctly for baselining.");
	}
	
	public void doInitialiseOutbreak(ImmutableOutbreakState.Builder builder, Outbreak o , Sampler sampler) {
		if (o instanceof ModifiableOutbreak) {
			ModifiableOutbreak m = (ModifiableOutbreak) o;
			if ( 
					m.initialisedBaseline() &&
					m.initialisedSetupConfiguration() &&
					m.initialisedExecutionConfiguration() &&
					m.initialisedSocialNetwork()
			) {
				initialiseOutbreak(builder,o,sampler);
				return;
			}
		} 
		throw new RuntimeException("Outbreak is not configured correctly for initialisation."); 
	}
	
	public void doInitialisePerson(ImmutablePersonState.Builder builder, Person p, Sampler sampler) {
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
								initialisePerson(builder,p,sampler);
								return;
							}
						} 
					}
					throw new RuntimeException("Person is not configured correctly for initialisation.");
				}
	
	

	public abstract void setupOutbreak(ModifiableOutbreak outbreak, SetupConfiguration config, Sampler sampler);
	
	public abstract void baselineOutbreak(ImmutableOutbreakBaseline.Builder builder, Outbreak outbreak, Sampler sampler);
	
	public abstract void baselinePerson(ImmutablePersonBaseline.Builder builder, Person person, Sampler rng);

	public abstract void initialisePerson(ImmutablePersonState.Builder builder, Person person, Sampler rng);

	public abstract void initialiseOutbreak(ImmutableOutbreakState.Builder builder, Outbreak outbreak, Sampler sampler);

	
}
