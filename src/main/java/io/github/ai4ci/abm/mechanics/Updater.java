package io.github.ai4ci.abm.mechanics;

import static io.github.ai4ci.abm.HistoryMapper.MAPPER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.Contact;
import io.github.ai4ci.abm.ImmutableOutbreakHistory;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModelUpdate;
import io.github.ai4ci.abm.ModelUpdate.OutbreakUpdaterFn;
import io.github.ai4ci.abm.ModelUpdate.PersonUpdaterFn;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonHistory;
import io.github.ai4ci.abm.mechanics.ModelOperation.OutbreakStateUpdater;
import io.github.ai4ci.abm.mechanics.ModelOperation.PersonStateUpdater;
import io.github.ai4ci.abm.mechanics.ModelOperation.TriConsumer;
import io.github.ai4ci.util.Ephemeral;
import io.github.ai4ci.util.Sampler;


public class Updater {

	static Logger log = LoggerFactory.getLogger(Updater.class);
	
	private List<PersonStateUpdater> personProcessors = new ArrayList<>();
	private List<OutbreakStateUpdater> outbreakProcessors = new ArrayList<>();
	
	public Updater() {
		this.outbreakProcessors = new ArrayList<>();
		this.outbreakProcessors.add(ModelUpdate.OutbreakUpdaterFn.DEFAULT.fn());
		this.personProcessors = new ArrayList<>();
		this.personProcessors.add(ModelUpdate.PersonUpdaterFn.DEFAULT.fn());
		this.personProcessors.add(ModelUpdate.PersonUpdaterFn.IMPORTATION_PROTOCOL.fn());
		this.personProcessors.add(ModelUpdate.PersonUpdaterFn.IMMUNISATION_PROTOCOL.fn());
	}
	
	public Updater withPersonProcessor(
			Predicate<Person> test,
			TriConsumer<ImmutablePersonState.Builder, Person, Sampler> updater
			) {
		personProcessors.add(ModelOperation.updatePersonState(test,updater));
		return this;
	}
	
	public Updater withPersonProcessors(PersonStateUpdater... updaters) {
		this.personProcessors.addAll(Arrays.asList(updaters));
		return this;
	}
	
	public Updater withOutbreakProcessor(
			Predicate<Outbreak> test,
			TriConsumer<ImmutableOutbreakState.Builder, Outbreak, Sampler> updater
			) {
		outbreakProcessors.add(ModelOperation.updateOutbreakState(test,updater));
		return this;
	}
	
	public Updater withOutbreakPostProcessors(OutbreakStateUpdater... updaters) {
		this.outbreakProcessors.addAll(Arrays.asList(updaters));
		return this;
	}
	
	public Outbreak update(Outbreak outbreak) {
		prepareUpdate(outbreak);
		// at this point the "current history" is the same as the previous state
		updateHistory(outbreak);
		switchHistory(outbreak);
		// at this point the "current history" is the same as the current state
		updateState(outbreak);
		switchState(outbreak);
		// at this point the "current history" is the same as the previous state
		log.debug("Update: "+outbreak.getUrn()+"; Step:"+outbreak.getCurrentState().getTime());
		return outbreak;
	}
	 
	/**
	 * set the nextState builders for outbreak and each of the people,
	 * copying the current state and incrementing the time. At the same
	 * time we population the nextHisotry for outbreak and people from the
	 * current state (and current time)
	 * @param outbreak
	 */
	
	private void prepareUpdate(Outbreak outbreak) {
		
		if (outbreak instanceof ModifiableOutbreak) {
			ModifiableOutbreak m = (ModifiableOutbreak) outbreak;
			
			m.setNextState(
				Ephemeral.of(
					ImmutableOutbreakState.builder()
						.from(m.getCurrentState())
						.setTime(m.getCurrentState().getTime()+1))
			);
			
			m.setNextHistory(
				Ephemeral.of(
					ImmutableOutbreakHistory.builder()
						.from(MAPPER.createHistory(m.getCurrentState())))
			);
			
			
		}
		
		outbreak.getPeople().parallelStream().forEach(person -> {
			if (person instanceof ModifiablePerson) {
				ModifiablePerson p = (ModifiablePerson) person;
				
				p.setNextState(
					Ephemeral.of(	
						ImmutablePersonState.builder()
							.from(p.getCurrentState())
							.setTime(p.getCurrentState().getTime()+1)
							.setImmunisationDose(0D)
							.setImportationExposure(0D)
					)
				);
				
				p.setNextHistory(
					Ephemeral.of(
						ImmutablePersonHistory.builder()
							.from(MAPPER.createHistory(p.getCurrentState()))
					)
				);
				
			}
		});
		
	}
	
	/**
	 * The contact network is established for every person.
	 * then update
	 * Update the outbreak history according to the current policy.
	 * the person histories according to the current behaviour. This second step
	 * is where testing is usually carried out. 
	 *  
	 * @param outbreak
	 */
	private void updateHistory(Outbreak outbreak) {
		// Sampler sampler = Sampler.getSampler();
		// TODO: ? ImmutableOutbreakHistory.Builder nextOutbreakHistory = outbreak.getNextHistory().get();
		if (outbreak instanceof ModifiableOutbreak) {
			ModifiableOutbreak m = (ModifiableOutbreak) outbreak;
			PersonStateContacts contactNetwork = Contact.contactNetwork(m);
			
			// Update the next history entry with anything from the current 
			Sampler sampler1 = Sampler.getSampler();
			ImmutableOutbreakHistory.Builder nextOutbreakHistory = outbreak.getNextHistory().toOptional().get();
			m.getStateMachine().performHistoryUpdate(nextOutbreakHistory, outbreak.getCurrentState(), sampler1);
			
			m.getPeople().parallelStream().forEach(person -> {
				
				Sampler sampler = Sampler.getSampler();
				ImmutablePersonHistory.Builder nextPersonHistory = person.getNextHistory().toOptional().get();
				
				if (person instanceof ModifiablePerson) {
					ModifiablePerson p = (ModifiablePerson) person;
					
					Integer ref = p.getId();
					nextPersonHistory
						.setTodaysContacts( contactNetwork.get(ref).finish() )
						.setTodaysExposures( contactNetwork.getExp(ref).finish());
					
					p.getStateMachine().performHistoryUpdate(nextPersonHistory, person.getCurrentState(), sampler);
					
				}
				
				
			});
		}
	}
	
	
	
	
	
	/**
	 * Creates the next model state factory, as a copy of current state and 
	 * applies a chain of OutbreakUpdaters to pre-process the next state factory.
	 * Then applies the updates to each of the agents, then 
	 * applies a chain of OutbreakUpdaters to post-process the next state factory.
	 * The pre and post processors can change the next state by modifying the state 
	 * factory  
	 * @param person the mutable holder for the immutable person state. 
	 */
	private void updateState(Outbreak outbreak) {
		
		// This is a bit complex to order changes so that things are built in 
		// correct order.
		// 1) make a new OutbreakState.Builder
		// 2) copy current state into new history and make a set of PersonHistory.Builder
		// 3) update PersonHistory.Builders with test results based on current state
		// 4) update PersonHistory.Builders with contacts based on current state
		// 5) switch the PersonHistory List with current step history from Builder
		// 6) make a set of next state PersonState.Builders based on current state
		// 7) update PersonState.Builders behaviour based on current step history
		// 8) 
		
		Sampler sampler = Sampler.getSampler();
		ImmutableOutbreakState.Builder nextState = outbreak.getNextState().toOptional().get();
		if (outbreak instanceof ModifiableOutbreak) {
			ModifiableOutbreak m = (ModifiableOutbreak) outbreak;
			// Update the state machine (for the outbreak).
			m.getStateMachine().performStateUpdate(nextState, m.getCurrentState(), sampler);
			
			// update nextState... pre-agent processing
			outbreakProcessors.forEach(p -> {
				if (p.getSelector().test(m)) {
					p.getConsumer().accept(nextState, m, sampler);
				}});
			
			// agent processing
			m.getPeople()
				.parallelStream()
				.forEach(p -> updateState(p));
			
			
		}
	}
	
	
	
	/**
	 * Creates the next state factory, as a copy of current state and 
	 * applies a chain of PersonUpdaters to the next state factory. 
	 * The PersonUpdaters can change the next state by modifying the state 
	 * factory  
	 * @param person the mutable holder for the immutable person state. 
	 */
	private void updateState(Person person) {
		// This is a thread local instance of sampler. so there should be one 
		// per thread. We shouldn't reset the seed though. 
		Sampler sampler = Sampler.getSampler();
		ImmutablePersonState.Builder nextState = person.getNextState().toOptional().get();
		if (person instanceof ModifiablePerson) {
			ModifiablePerson m = (ModifiablePerson) person;
			
			// Update the state machine (for the behaviour).
			m.getStateMachine().performStateUpdate(nextState, person.getCurrentState(), sampler);
			
			nextState
				// Update the viral load model.
				// This requires an up to date history for the exposures.
				.setInHostModel(m.getCurrentState().getInHostModel().update(person, sampler))
				;
			
			
			
			// update nextState...
			personProcessors.forEach(p -> {
				if (p.getSelector().test(m)) {
					p.getConsumer().accept(nextState, m, sampler);
				}});
			
			
			
		}
	}
	
	/**
	 * Switches in a new state for a model by building the next state and 
	 * setting the current state to the new state. Makes sure that the agents
	 * state are all also updated before updating the model state, and record 
	 * the new state in the model history. This means the model history is always
	 * up to date with teh current model state
	 * @param outbreak the mutable model.
	 */
	private void switchHistory(Outbreak outbreak) {
		int limit = (int) outbreak.getExecutionConfiguration().getInfectivityProfile().size()*2;
		if (outbreak instanceof ModifiableOutbreak) {
			ModifiableOutbreak m = (ModifiableOutbreak) outbreak;
			m.getPeople()
				.parallelStream()
				.forEach(person -> {
					if (person instanceof ModifiablePerson) {
						ModifiablePerson p = (ModifiablePerson) person;
						synchronized(p) {
							List<PersonHistory> tmp = p.getHistory();
							tmp.add(0, p.getNextHistory().toOptional().get().build());
							while (tmp.size() > limit) {
								tmp.remove(limit);
							}
							p.setNextHistory( p.getNextHistory().clear());
						}
					}
				});
			synchronized(m) {
				m.getHistory().add(0, m.getNextHistory().toOptional().get().build());
				m.setNextHistory( m.getNextHistory().clear());
			}
		}
	}
	
	
	/**
	 * Switches in a new state for a model by building the next state and 
	 * setting the current state to the new state. Makes sure that the agents
	 * state are all also updated before updating the model state, and record 
	 * the new state in the model history. This means the model history is always
	 * up to date with teh current model state
	 * @param outbreak the mutable model.
	 */
	private void switchState(Outbreak outbreak) {
		if (outbreak instanceof ModifiableOutbreak) {
			ModifiableOutbreak m = (ModifiableOutbreak) outbreak;
			m.getPeople()
				.parallelStream()
				.forEach(person -> {
					if (person instanceof ModifiablePerson) {
						ModifiablePerson p = (ModifiablePerson) person;
						synchronized(p) {
							//TODO: Update spatio-temporal state network if explicit. 
							// This is where the new and old state co-exist
							// it is one place where we could make a record in a 
							// spatio-temporal network. Possibly the only place.
							// It would have to be thread safe and non blocking.
							// alternatively we can just use the PersonHistory for this
							p.setCurrentState(p.getNextState().toOptional().get().build());
							p.setNextState(p.getNextState().clear());
						}
					}
				});
			synchronized(m) {
				m.setCurrentState(m.getNextState().toOptional().get().build());
				m.setNextState(
						m.getNextState().clear());
				
			}
		}
	}
	
	
}
