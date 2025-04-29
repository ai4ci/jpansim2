package io.github.ai4ci.abm.builders;

import io.github.ai4ci.abm.ImmutableOutbreakBaseline.Builder;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.config.WattsStrogatzConfiguration;
import io.github.ai4ci.util.Sampler;

public class DefaultModelBuilder extends AbstractModelBuilder implements
	DefaultNetworkSetup, DefaultOutbreakBaseliner, DefaultOutbreakInitialiser,
	DefaultPersonBaseliner, DefaultPersonInitialiser
{

	@Override
	public void setupOutbreak(ModifiableOutbreak outbreak, SetupConfiguration config, Sampler sampler) {
		DefaultNetworkSetup.super.setupOutbreak(outbreak, (WattsStrogatzConfiguration) config, sampler);
	}

	@Override
	public void baselineOutbreak(Builder builder, Outbreak outbreak, Sampler sampler) {
		DefaultOutbreakBaseliner.super.baselineOutbreak(builder, outbreak, sampler);
	}

	@Override
	public void baselinePerson(ImmutablePersonBaseline.Builder builder, Person person,
			Sampler rng) {
		DefaultPersonBaseliner.super.baselinePerson(builder, person, rng);
	}

	@Override
	public void initialisePerson(ImmutablePersonState.Builder builder, Person person, Sampler rng) {
		DefaultPersonInitialiser.super.initialisePerson(builder, person, rng);
	}

	@Override
	public void initialiseOutbreak(ImmutableOutbreakState.Builder builder, Outbreak outbreak,
			Sampler sampler) {
		DefaultOutbreakInitialiser.super.initialiseOutbreak(builder, outbreak, sampler);
	}

	

	

	

}
