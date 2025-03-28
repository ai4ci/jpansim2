package io.github.ai4ci.abm;

import static io.github.ai4ci.abm.HistoryMapper.MAPPER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.ModelOperation.OutbreakStateUpdater;
import io.github.ai4ci.abm.ModelOperation.PersonStateUpdater;
import io.github.ai4ci.abm.ModelOperation.TriConsumer;
import io.github.ai4ci.abm.PersonHistory.Infection;
import io.github.ai4ci.util.Ephemeral;
import io.github.ai4ci.util.Sampler;


public class Updater {

	Logger log = LoggerFactory.getLogger(Updater.class);
	
	private List<PersonStateUpdater> personProcessors = new ArrayList<>();
	private List<OutbreakStateUpdater> outbreakProcessors = new ArrayList<>();
	
	public Updater() {
		this.outbreakProcessors = new ArrayList<>();
		this.outbreakProcessors.add(ModelRun.OutbreakUpdaterFn.DEFAULT.fn());
		this.personProcessors = new ArrayList<>();
		this.personProcessors.add(ModelRun.PersonUpdaterFn.DEFAULT.fn());
		this.personProcessors.add(ModelRun.PersonUpdaterFn.IMPORTATION_PROTOCOL.fn());
		this.personProcessors.add(ModelRun.PersonUpdaterFn.IMMUNISATION_PROTOCOL.fn());
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
		
		if (outbreak instanceof ModifiableOutbreak m) {
			
			
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
			if (person instanceof ModifiablePerson p) {
				
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
		if (outbreak instanceof ModifiableOutbreak m) {
			
			ContactNetwork contactNetwork = contactNetwork(m);
			
			// Update the next history entry with anything from the current 
			Sampler sampler1 = Sampler.getSampler();
			ImmutableOutbreakHistory.Builder nextOutbreakHistory = outbreak.getNextHistory().toOptional().get();
			m.getStateMachine().performHistoryUpdate(nextOutbreakHistory, outbreak.getCurrentState(), sampler1);
			
			m.getPeople().parallelStream().forEach(person -> {
				
				Sampler sampler = Sampler.getSampler();
				ImmutablePersonHistory.Builder nextPersonHistory = person.getNextHistory().toOptional().get();
				
				if (person instanceof ModifiablePerson p) {
					
					// Contacts:
					PersonHistoryReference ref = PersonHistoryReference.from(p.getCurrentState());
					nextPersonHistory.setTodaysContacts( contactNetwork.getOrDefault(ref, Collections.emptySet()) );
					
					p.getStateMachine().performHistoryUpdate(nextPersonHistory, person.getCurrentState(), sampler);
					
				}
				
				
			});
		}
	}
	
	private class ContactNetwork extends ConcurrentHashMap<PersonHistoryReference, Set<Contact>> {
	}
	
	private ContactNetwork contactNetwork(Outbreak outbreak) {
		//Do the contact network here? and pass it as a parameter to the
		//person updateState
		SimpleWeightedGraph<Person, Person.Relationship> network = outbreak.getSocialNetwork();
		ContactNetwork out = new ContactNetwork();
		
		network.edgeSet().parallelStream().forEach(r -> {
			Sampler sampler = Sampler.getSampler();
			PersonState one = network.getEdgeSource(r).getCurrentState();
			PersonState two = network.getEdgeTarget(r).getCurrentState();
			// jointProb is a contact intensity. This can be used to 
			// estimate contact observation probability
			double jointProb = 
					// Conversions.scaleProbability(
							// This will be the lowest common value 
							one.getAdjustedMobility()*two.getAdjustedMobility(); //,
							// This is just a way of introducing noise into
							// the probability.
							// sampler.gamma(1, 0.1)
					// );
			// TODO: connectedness quantile is a proxy for the context of a contact
			// If we wanted to control this a different set of fetaures of the 
			// relationship could be used. At the moment this overloads mobility
			// with type of contact, but in reality WORK contacts may be less
			// Significant that home contacts. This is where we would implement
			// something along these lines.
			if (jointProb > r.getConnectednessQuantile()) {
				// This is a contact. 
				// Contact transmission probability depends on lowest transmissibility
				// If both parties wearing masks will be less than if one only.
				// TODO: where does this fit with the notions of proximity and
				// duration?
				double jointTrans = 
						//Conversions.scaleProbability(
								one.getAdjustedTransmissibility()*two.getAdjustedTransmissibility();
						//		sampler.gamma(1, 0.1));
				
				PersonHistoryReference oneref = PersonHistoryReference.from(one);
				PersonHistoryReference tworef = PersonHistoryReference.from(two);
				
				// Detection probability. 
				// This depends on the intensity of the contact, and the proabability
				// that both users have their phone switched on etc.
				// Contact detected is going to be a function of compliance
				double jointDetect = one.getContactDetectedProbability()*two.getContactDetectedProbability();
				boolean detected = sampler.bern(jointDetect);
				boolean transmitted = sampler.bern(jointTrans);
				out.putIfAbsent(oneref, ConcurrentHashMap.newKeySet());
				out.get(oneref)
					.add(ImmutableContact.builder()
							.setProximityDuration(jointProb)
							.setDetected(detected)
							.setTransmitted(transmitted)
							.setParticipant(tworef)
							.build());
				// Significant overhead here I think
				out.putIfAbsent(tworef, ConcurrentHashMap.newKeySet());
				out.get(tworef)
					.add(ImmutableContact.builder()
							.setProximityDuration(jointProb)
							.setDetected(detected)
							.setTransmitted(transmitted)
							.setParticipant(oneref)
							.build());
				
			}
		});
		
		return out;
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
		if (outbreak instanceof ModifiableOutbreak m) {
			
			// Update the state machine.
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
		
			// update infection network...
			// TODO: this does not look to be correct...
			DirectedAcyclicGraph<PersonHistory, Infection> infections = m.getInfections();
			outbreak.getPeople()
				.stream()
				.flatMap(p -> p.getCurrentHistory().stream())
				.filter(ph -> ph.isInfectious())
				.forEach(p -> {
					// getInfector() is only defined on first day of infection
					// It also decides which person is the infector if there were
					// multiple exposures
					// If there is an importation then this will most likely
					// not have an infector.
					p.getInfector().ifPresent(i -> {
						infections.addVertex(i);
						infections.addVertex(p);
						Contact contact = p.getInfectiousContact().get();
						try {
							infections.addEdge(i, p, Infection.create(contact));
						} catch (Exception e) {
							// TODO: figure out this exception cause.
							// It is because the infection network can cause a cycle
							// which could be because the heuristics for determining
							// who infected whom depends on picking the person]
							// who contributed the largest viral load in recent
							// history.
							log.debug(e.getMessage());
						}
					});
				});
			
			
			
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
		if (person instanceof ModifiablePerson m) {
			
			// Update the state machine.
			m.getStateMachine().performStateUpdate(nextState, person.getCurrentState(), sampler);
			
			nextState
				// Update the viral load model.
				// This requires an up to date history for the exposures.
				.setViralLoad(m.getCurrentState().getViralLoad().update(sampler))
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
		if (outbreak instanceof ModifiableOutbreak m) {
			m.getPeople()
				.parallelStream()
				.forEach(person -> {
					if (person instanceof ModifiablePerson p) {
						synchronized(p) {
							p.getHistory().add(0, p.getNextHistory().toOptional().get().build());
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
		if (outbreak instanceof ModifiableOutbreak m) {
			m.getPeople()
				.parallelStream()
				.forEach(person -> {
					if (person instanceof ModifiablePerson p) {
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
