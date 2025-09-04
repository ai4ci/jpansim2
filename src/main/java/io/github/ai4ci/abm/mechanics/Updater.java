package io.github.ai4ci.abm.mechanics;

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
import io.github.ai4ci.abm.mechanics.ModelOperation.OutbreakStateUpdater;
import io.github.ai4ci.abm.mechanics.ModelOperation.PersonStateUpdater;
import io.github.ai4ci.abm.mechanics.ModelOperation.TriConsumer;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.Ephemeral;
import io.github.ai4ci.util.Sampler;
import io.github.ai4ci.util.ThreadSafeArray;

/**
 * The updater is the main handler of the update cycle. It contains the hard
 * coded bits of the update cycle and configurable aspects can be added with
 * person processors and outbreak processors which can be created with 
 * {@link #withPersonProcessor(Predicate, TriConsumer)} or 
 * {@link #withOutbreakProcessor(Predicate, TriConsumer)}.
 * 
 * Pre-canned update configurations are found in the {@link ModelUpdate} class.
 * 
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
	 * time we population the nextHisotry for outbreak and people from the
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
			
			nextState
				// Update the viral load model and the risk models.
				// These requires an up to date history for the exposures.
				// 
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
