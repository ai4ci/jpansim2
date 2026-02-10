package io.github.ai4ci.flow.mechanics;

import static io.github.ai4ci.abm.HistoryMapper.MAPPER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.Contact;
import io.github.ai4ci.abm.Exposure;
import io.github.ai4ci.abm.ImmutableContact;
import io.github.ai4ci.abm.ImmutableExposure;
import io.github.ai4ci.abm.ImmutableOutbreakHistory;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModelUpdate;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonHistory;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.SocialRelationship;
import io.github.ai4ci.flow.mechanics.ModelOperation.OutbreakStateUpdater;
import io.github.ai4ci.flow.mechanics.ModelOperation.PersonStateUpdater;
import io.github.ai4ci.flow.mechanics.ModelOperation.TriConsumer;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.Ephemeral;
import io.github.ai4ci.util.Sampler;
import io.github.ai4ci.util.ThreadSafeArray;

/**
 * The updater is the main handler of the update cycle. It contains the hard
 * coded bits of the update cycle and configurable aspects can be added with
 * person processors and outbreak processors which can be created with 
 * {@link #withPersonProcessor(Predicate, TriConsumer)} or 
 * {@link #withOutbreakProcessor(Predicate, TriConsumer)} and pre-canned update 
 * configurations are found in the {@link ModelUpdate} class.
 * 
 * <br><br> Within the update process at each time step the contact network is 
 * determined by the {@link #contactNetwork(Outbreak)} method. This is the key
 * determinant of exposures, from social network connections and, if supported,
 * venue driven connections. 
 *  
 * <br><br> The updater follows the following process:
 * <ul>
 * <li> <u>prepareUpdate:</u> Creates a Ephemeral OutbreakState and PersonState builder
 * in Outbreak and each Person respectively, from the current state. This will 
 * be used to process state changes and will become the current state at the 
 * next time point. Similarly creates a OutbreakHistory and PersonHistory builder
 * from the current states, using {@link io.github.ai4ci.abm.HistoryMapper HistoryMapper}. 
 * These will become the historical record of the 
 * current state at the original time point. At this point the head of the 
 * history list still points to the previous day (t-1) and the current state to (t).
 * </li>
 * <li> <u>updateHistory:</u> This stage is responsible for filling in events that
 *   can be regarded as happening at the end of the day (t). Firstly the contacts made 
 *   during the day are simulated and recorded, depending on the state of the 
 *   agents. Secondly updates are made to the 
 *   model as defined by policy and behaviours. This mostly includes updating
 *   agents with records of the testing performed during the current day (t).
 *   This is done by the end of day function in the 
 *   the behaviour state (see {@link io.github.ai4ci.flow.mechanics.State.BehaviourState})
 *   which implements an <em>updateHistory</em> hook for person histories, 
 *   and in the policy state (see {@link io.github.ai4ci.flow.mechanics.State.PolicyState})
 *   which implements an <em>updateHistory</em> hook for outbreak histories. At the end of
 *   this stage the head of the 
 *   history list points to the original previous day (t-1) and the current state 
 *   is still at the original day (t).
 *   
 *   <br> The updateHistory phase first operates on the Outbreak and then on
 * each Person in the simulation.
 * </li>
 * <li> <u>switchHistory:</u> At this point the head of the OutbreakHistory list 
 *   and PersonHistory lists are updated to point to the copy created 
 *   from the current state in the last step. After this point the head of the 
 *   history list points to the same day (t) as the current state (t).
 * </li>
 * <li> <u>updateState:</u> The second phase of the updates is focussed on 
 * changes that happen at the start of the next day (t+1). For all agents their in 
 * host viral load model is updated with information about any exposures that 
 * happened in the previous day (t) and the model advanced one time step top (t+1). 
 * Similarly based on observed contacts, and their observed risks and test 
 * results the per agent risk model is updated to 
 * represent what each agent could know about their risk at time (t+1) based on
 * information up to and including time (t). Behavioural changes are then 
 * applied using the behaviour state's {@code nextState} hook ({@link io.github.ai4ci.flow.mechanics.State.BehaviourState}),
 * and in the policy state via its {@code nextState} hook ({@link io.github.ai4ci.flow.mechanics.State.PolicyState}).
 * State implementations use the context and RNG to decide the next state and may
 * mutate the supplied builders where necessary.
 * 
 * <br><br> When using this care must be taken to
 * recognise that the first day in history is the current day that is being 
 * updated (t). After this the current state is still at time (t) and the
 * head of the history list at time (t) - however the agents in host viral load
 * and risk models are already updated to (t+1).
 * 
 * <br> The updateState phase first operates on the Outbreak and then on
 * each Person in the simulation.
 * </li>
 * <li> <u>switchState:</u> At this point the current states of the outbreak (t)
 *   or agents is switched to point to the newly created future state representing
 *   the next day in the simulation (t+1). When this happens the simulation day 
 *   counter is effectively incremented. After this point the head of an agents 
 *   history list points to the previous day (t) and the current state is 
 *   referring to (t+1) (including in host and risk models)
 * </li>   
 * </ul>
 * 
 * <img src="updater.png" alt="Updater process flow diagram">
 */
public class Updater {

	static Logger log = LoggerFactory.getLogger(Updater.class);
	
	private List<PersonStateUpdater> personProcessors = new ArrayList<>();
	private List<OutbreakStateUpdater> outbreakProcessors = new ArrayList<>();
	
	public Updater() {
		this.outbreakProcessors = new ArrayList<>();
		for (ModelUpdate.OutbreakUpdaterFn value: ModelUpdate.OutbreakUpdaterFn.values())
			this.outbreakProcessors.add(value.fn());
		this.personProcessors = new ArrayList<>();
		for (ModelUpdate.PersonUpdaterFn value: ModelUpdate.PersonUpdaterFn.values())
			this.personProcessors.add(value.fn());
		
	}
	
	/**
	 * This method allows you to add a person processor to the update cycle. Person processors are the final step to be run in updating an person
	 * immediately before the final update. When they
	 * operate the head of the history list will be at time (t) and the current
	 * state will also refer to time (t). The in host model and risk model will
	 * have been updated to (t+1) but this will not yet have been applied to
	 * the current state. However the behaviour state machine state will reflect
	 * time (t+1).
	 * 
	 * @see Updater#withPersonProcessors(PersonStateUpdater...)
	 * @param test A predicate that test whether the person should be updated
	 * @param updater An 3 parameter function referring to a person state, the 
	 *   current state, and a RNG sampler.
	 * @return The updater ( a fluent method)
	 */
	public Updater withPersonProcessor(
			Predicate<Person> test,
			TriConsumer<ImmutablePersonState.Builder, Person, Sampler> updater
			) {
		personProcessors.add(ModelOperation.updatePersonState(test,updater));
		return this;
	}
	
	/**
	 * Person processors are the final step to be run in updating an person
	 * immediately before the final update. When they 
	 * operate the head of the history list will be at time (t) and the current
	 * state will also refer to time (t). The in host model and risk model will 
	 * have been updated to (t+1) but this will not yet have been applied to 
	 * the current state. However the behaviour state machine state will reflect 
	 * time (t+1).
	 * @param updaters see {@link io.github.ai4ci.flow.mechanics.ModelOperation#updatePersonState(Predicate, TriConsumer)}
	 * @return the updater as a fluent method
	 */
	public Updater withPersonProcessors(PersonStateUpdater... updaters) {
		this.personProcessors.addAll(Arrays.asList(updaters));
		return this;
	}
	
	/**
	 * @see Updater#withOutbreakProcessors(OutbreakStateUpdater...)
	 * @param test A predicate that test whether the outbreak should be updated. 
	 *   This could for example be a time based filter.
	 * @param updater An 3 parameter function referring to a outbreak state, the 
	 *   current state, and a RNG sampler. An example here could change the
	 *   transmissibility of the outbreak at a set time.
	 * @return The updater ( a fluent method)
	 */
	public Updater withOutbreakProcessor(
			Predicate<Outbreak> test,
			TriConsumer<ImmutableOutbreakState.Builder, Outbreak, Sampler> updater
			) {
		outbreakProcessors.add(ModelOperation.updateOutbreakState(test,updater));
		return this;
	}
	
	/**
	 * Outbreak processors are the final step to be run in updating an outbreak
	 * immediately before the agents in the outbreak are updated. When they 
	 * operate the head of the history list will be at time (t) and the current
	 * state will also refer to time (t). However the policy state machine state 
	 * will reflect time (t+1).
	 * @param updaters see {@link io.github.ai4ci.flow.mechanics.ModelOperation#updateOutbreakState(Predicate, TriConsumer)}
	 * @return the updater as a fluent method
	 */
	public Updater withOutbreakProcessors(OutbreakStateUpdater... updaters) {
		this.outbreakProcessors.addAll(Arrays.asList(updaters));
		return this;
	}
	
	/**
	 * The main update method is called to advance the simulation by one day.
	 * @param outbreak a simulation to update
	 * @return the same outbreak with new states and histories
	 */
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
	 * time we populate the nextHistory for outbreak and people from the
	 * current state (and current time)
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
			
			m.getStateMachine().prepareUpdate();
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
				
				p.getStateMachine().prepareUpdate();
			}
		});
		
	}
	
	/**
	 * The contact network is established for every person.
	 * then update the outbreak history according to the current policy. Then
	 * the person histories according to their current behaviour. This second step
	 * is where the behaviour models implement testing. 
	 */
	private void updateHistory(Outbreak outbreak) {
		// Sampler sampler = Sampler.getSampler();
		if (outbreak instanceof ModifiableOutbreak) {
			// This is always true BTW.
			ModifiableOutbreak m = (ModifiableOutbreak) outbreak;
			PersonStateContacts contactNetwork = contactNetwork(m);
//			Contact[][] contacts = contactNetwork.finaliseContacts();
//			Exposure[][] exposures = contactNetwork.finaliseExposures();
			
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
						.setTodaysContacts( contactNetwork.getContactsForId(ref) )
						.setTodaysExposures( contactNetwork.getExposuresForId(ref) );
					
					p.getStateMachine().performHistoryUpdate(nextPersonHistory, person.getCurrentState(), sampler);
					
				}
				
				
			});
		}
	}
	
	/**
	 * Switches in a new state for a model by building the next state and 
	 * setting the current state to the new state. Makes sure that the agents
	 * state are all also updated before updating the model state, and record 
	 * the new state in the model history. This means the model history is always
	 * up to date with the current model state.
	 * 
	 * <br> N.B. This is where the limit in person history length is implemented.
	 */
	private void switchHistory(Outbreak outbreak) {
		int limit = (int) Math.max(
				outbreak.getBaseline().getSymptomDuration(),
				outbreak.getBaseline().getInfectiveDuration()
		) * 2;
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
			
			// What is the correct order here?
			// It feels like this should be done before the state update above for 
			// each person but realistically it doesn't matter as the updates are 
			// unaware of each others effects. The only thing that would have 
			// changed is the individual's behaviour e.g. m.getStateMachine().
			
			nextState
				// Update the viral load model and the risk models.
				// These requires an up to date history for the exposures.
				.setInHostModel(m.getCurrentState().getInHostModel().update(person, sampler))
				.setRiskModel(m.getCurrentState().getRiskModel().update())
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
	 * up to date with the current model state
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
	
	/**
	 * Build the contact network for a specific outbreak day.
	 * @param outbreak the simulation
	 * @return a PersonStateContacts data structure with contacts and
	 * exposures for every individual in the model (potentially empty). 
	 */
	public static PersonStateContacts contactNetwork(Outbreak outbreak) {
		//Do the contact network here? and pass it as a parameter to the
		//person updateState
		ThreadSafeArray<SocialRelationship> network = outbreak.getSocialNetwork();
		PersonStateContacts out = new PersonStateContacts(
				outbreak.getPeople().size(),
				network.size() / outbreak.getPeople().size() * 4
		);
		
		network.parallelStream().forEach(r -> {
			Sampler sampler = Sampler.getSampler();
			PersonState one = r.getSource(outbreak).getCurrentState();
			PersonState two = r.getTarget(outbreak).getCurrentState();
			
			// TODO: contacts stratified by venue such as work or school  
			// connectedness quantile is a proxy for the context of a contact
			// If we wanted to control this a different set of features of the 
			// relationship could be used. At the moment this overloads mobility
			// with type of contact, but in reality WORK contacts may be less
			// Significant that home contacts. This is where we would implement
			// something along these lines.
			
			double contactProbability = r.contactProbability(
					one.getAdjustedMobility(),
					two.getAdjustedMobility());
			
			if (sampler.bern(contactProbability)) {
				// This is a contact. 
				// Contact transmission probability depends on lowest transmissibility
				
				Integer oneref = one.getEntity().getId();
				Integer tworef = two.getEntity().getId();
				
				double jointDetect = 
						one.getAdjustedAppUseProbability()*
						two.getAdjustedAppUseProbability()*
						outbreak.getCurrentState().getContactDetectedProbability();
				
				boolean detected = sampler.bern(jointDetect);
				
				Contact contact = ImmutableContact.builder()
					.setDetected(detected)
					.setParticipant1Id(oneref)
					.setParticipant2Id(tworef)
					// TODO: Proximity and duration of a contact aren't handled
					// .setProximityDuration(contactProbability)
					.build();
				
				out.write(oneref).put(tworef, contact);
				out.write(tworef).put(oneref, contact);
				
				asExposure(contact, one, two).ifPresent(e -> out.writeExp(oneref).put(tworef, e));
				asExposure(contact, two, one).ifPresent(e -> out.writeExp(tworef).put(oneref, e));
			}
		});
		
		return out;
		

	}
	
	/**
	 * Is a contact an exposure? This is a directional relationship so is called
	 * two times for each contact. 
	 */
	public static Optional<Exposure> asExposure(Contact contact, PersonState infectee, PersonState infector) {
		
		Sampler sampler = Sampler.getSampler();
		
		if (!infector.isInfectious()) return Optional.empty();
		
		// This is where transmission rate / susceptability plays a role.
		double trans =   
			Conversions.scaleProbabilityByOR(
				infector.getAdjustedTransmissibility(),
				infectee.getSusceptibilityModifier()
			);
		
		boolean transmitted = sampler.bern(trans);
		if (transmitted == false) return Optional.empty();
		
		return Optional.of(
					ImmutableExposure.builder()
						.setExposerId(infector.getEntity().getId())
						// Should the amount of exposure be dependent on the 
						// probability of transmission or is this a stochastic 
						// event? My belief is the latter. If it happens, the
						// dose of virus is independent of how likely it was
						// to happen. Probability of transmission depends on
						// whether contact coughs, dose depends on how much 
						// virus they cough over you.
						// See PersonState#getContactExposure for where this is
						// picked up and fed into the in host model.
						.setExposure(infector.getNormalisedViralLoad())
						//.setTransmissionProbability(trans)
						.build()
				);
	}
}