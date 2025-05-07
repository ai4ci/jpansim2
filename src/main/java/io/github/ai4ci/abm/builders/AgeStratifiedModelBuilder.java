package io.github.ai4ci.abm.builders;

import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.ImmutableOutbreakBaseline.Builder;
import io.github.ai4ci.config.AgeStratifiedNetworkConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.util.Sampler;

public class AgeStratifiedModelBuilder extends AbstractModelBuilder implements
	AgeStratifiedNetworkSetup, DefaultOutbreakBaseliner, DefaultOutbreakInitialiser,
	AgeStratifiedPersonBaseliner, AgeStratifiedPersonInitialiser 
{

	@Override
	public void setupOutbreak(ModifiableOutbreak outbreak, SetupConfiguration config, Sampler sampler) {
		AgeStratifiedNetworkSetup.super.setupOutbreak(outbreak, (AgeStratifiedNetworkConfiguration) config, sampler);
	}

	@Override
	public void baselineOutbreak(Builder builder, Outbreak outbreak, Sampler sampler) {
		DefaultOutbreakBaseliner.super.baselineOutbreak(builder, outbreak, sampler);
	}

	@Override
	public void baselinePerson(ImmutablePersonBaseline.Builder builder, Person person,
			Sampler rng) {
		AgeStratifiedPersonBaseliner.super.baselinePerson(builder, person, rng);
	}

	@Override
	public void initialisePerson(ImmutablePersonState.Builder builder, Person person, Sampler rng) {
		AgeStratifiedPersonInitialiser.super.initialisePerson(builder, person, rng);
	}

	@Override
	public void initialiseOutbreak(ImmutableOutbreakState.Builder builder, Outbreak outbreak,
			Sampler sampler) {
		DefaultOutbreakInitialiser.super.initialiseOutbreak(builder, outbreak, sampler);
	}
	
}
