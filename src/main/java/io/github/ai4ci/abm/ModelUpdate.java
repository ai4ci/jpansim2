package io.github.ai4ci.abm;

import static io.github.ai4ci.abm.mechanics.ModelOperation.updateOutbreakState;
import static io.github.ai4ci.abm.mechanics.ModelOperation.updatePersonState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.mechanics.ModelOperation;
import io.github.ai4ci.abm.mechanics.ModelOperation.OutbreakStateUpdater;
import io.github.ai4ci.abm.mechanics.ModelOperation.PersonStateUpdater;
import io.github.ai4ci.util.ModelNav;

/**
 * Functions updating the model states ({@link OutbreakState} and {@link PersonState})
 * during the update cycle. This is where time dependent comparisons can be made.
 * These model update functions happen after the current state is mapped across
 * and the behaviour or policy state machines are updated. This means that they 
 * can see the new behaviour state but nothing else will have changed. This is 
 * where time dependent functions can be applied
 */
public class ModelUpdate {

	static Logger log = LoggerFactory.getLogger(ModelUpdate.class);
	
	public static enum OutbreakUpdaterFn {
		
		DEFAULT(
			updateOutbreakState(
					(outbreak) -> true,
					(builder, outbreak, rng) -> {
						//OutbreakBaseline baseline = outbreak.getBaseline();
						//OutbreakState current = outbreak.getCurrentState();
			})
		);
		
		OutbreakStateUpdater fn;
		OutbreakUpdaterFn(ModelOperation.OutbreakStateUpdater fn) {this.fn=fn;}
		public OutbreakStateUpdater fn() {return fn;}
	}
	
	public static enum PersonUpdaterFn {
		
		DEFAULT (
			updatePersonState(
				(person) -> true,
				(builder, person, rng) -> {
					//PersonBaseline baseline = person.getBaseline();
					//PersonState current = person.getCurrentState();
					
				
			})
		),
		
		IMMUNISATION_PROTOCOL (
				updatePersonState(
					(person) -> true,
					(builder, person, rng) -> {
						//PersonBaseline baseline = person.getBaseline();
						//PersonState current = person.getCurrentState();
						builder.setImmunisationDose(0D);
				})
		),

		IMPORTATION_PROTOCOL (
				updatePersonState(
					(person) -> true,
					(builder, person, rng) -> {
						//PersonBaseline baseline = person.getBaseline();
						//PersonState current = person.getCurrentState();
						if (rng.uniform() < ModelNav.modelParam(person).getImportationProbability() *
								person.getCurrentState().getAdjustedMobility()) {
							builder.setImportationExposure(2D);
						} else {
							builder.setImportationExposure(0D);
						}
				})
		)
		
		;
		PersonStateUpdater fn;
		PersonUpdaterFn(ModelOperation.PersonStateUpdater fn) {this.fn=fn;}
		public PersonStateUpdater fn() {return fn;}
	}
	
}
