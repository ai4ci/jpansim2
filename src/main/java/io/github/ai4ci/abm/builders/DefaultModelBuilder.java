package io.github.ai4ci.abm.builders;

import io.github.ai4ci.abm.ImmutableOutbreakBaseline;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.config.setup.WattsStrogatzConfiguration;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.mechanics.AbstractModelBuilder;
import io.github.ai4ci.util.Sampler;

public class DefaultModelBuilder extends AbstractModelBuilder implements
	DefaultNetworkSetup, DefaultOutbreakBaseliner, DefaultOutbreakInitialiser,
	DefaultPersonBaseliner, DefaultPersonInitialiser
{

	@Override
	public ModifiableOutbreak setupOutbreak(ModifiableOutbreak outbreak, SetupConfiguration config, Sampler sampler) {
		return DefaultNetworkSetup.super.setupOutbreak(outbreak, (WattsStrogatzConfiguration) config, sampler);
	}

	@Override
	public ImmutableOutbreakBaseline baselineOutbreak(ImmutableOutbreakBaseline.Builder builder, Outbreak outbreak, Sampler sampler) {
		return DefaultOutbreakBaseliner.super.baselineOutbreak(builder, outbreak, sampler);
	}

	@Override
	public ImmutablePersonBaseline baselinePerson(ImmutablePersonBaseline.Builder builder, Person person,
			Sampler rng) {
		return DefaultPersonBaseliner.super.baselinePerson(builder, person, rng);
	}

	@Override
	public ImmutablePersonState initialisePerson(ImmutablePersonState.Builder builder, Person person, Sampler rng) {
		return DefaultPersonInitialiser.super.initialisePerson(builder, person, rng);
	}

	@Override
	public ImmutableOutbreakState initialiseOutbreak(ImmutableOutbreakState.Builder builder, Outbreak outbreak,
			Sampler sampler) {
		return DefaultOutbreakInitialiser.super.initialiseOutbreak(builder, outbreak, sampler);
	}

	

	

	

}
