package io.github.ai4ci.flow.mechanics;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import io.github.ai4ci.abm.Contact;
import io.github.ai4ci.abm.Exposure;

/**
 * This is a temporary data structure that gets generated during the update
 * cycle to hold all the contact and exposures before they are written to the
 * individual {@link io.github.ai4ci.abm.PersonHistory} entries in the model.
 * No effort is made in this class to ensure that added items are unique.
 */
public class PersonStateContacts implements Serializable {
	
	// The contact network is an array of concurrent skip list maps. The index
	// of this array is the id of the index contact. This is initialised for the 
	// whole population up front during the update cycle, and does not have to
	// be resized. The concurrent skip list maps hold the variable length of 
	// contactees. This is not enforced to be unique which is up to the provider 
	ConcurrentHashMap<Integer,Contact>[] network;
	// ConcurrentSkipListMap<Integer,Contact>[] network;
	//volatile ThreadSafeArray<Exposure>[] exposure;
	ConcurrentHashMap<Integer,Exposure>[] exposure;
	// ConcurrentSkipListMap<Integer,Exposure>[] exposure;
	
	@SuppressWarnings("unchecked")
	
	/** nodes is essentially the number of people in the model. Max size is the
	 * maximum number of contacts they could make. In the worst case this will
	 * create a data structure 2*nodes*maxSize big.
	 */
	public PersonStateContacts(int nodes, int maxSize) {
		network = new ConcurrentHashMap[nodes];
				// new ConcurrentLinkedQueue[nodes];
				// new ConcurrentSkipListMap[nodes]; //ThreadSafeArray[nodes];
		exposure = 
				new ConcurrentHashMap[nodes];
				// new ConcurrentLinkedQueue[nodes];
				// new ConcurrentSkipListMap[nodes]; //new ThreadSafeArray[nodes];
		
		for (int i=0; i<nodes; i++) {
			network[i] = //new ThreadSafeArray<Contact>(Contact.class,maxSize);
					// new ConcurrentSkipListMap<Integer,Contact>();
					// new ConcurrentLinkedQueue<>();
					new ConcurrentHashMap<Integer,Contact>();
			exposure[i] = //ThreadSafeArray.empty(Exposure.class);
					// new ConcurrentSkipListMap<Integer,Exposure>();
					// new ConcurrentLinkedQueue<>();
					new ConcurrentHashMap<Integer,Exposure>();
		}
	}
	
	public ConcurrentHashMap<Integer,Contact> write(int i) {
		return network[i];
	}
	
	public ConcurrentHashMap<Integer,Exposure> writeExp(int i) {
		return exposure[i];
	}
	
//	public Contact[][] finaliseContacts() {
//		Contact[][] out = new Contact[network.length][];
//		IntStream.range(0, network.length).parallel().forEach(
//			// i -> out[i] = network[i].finish()
//			i -> out[i] = network[i].values().toArray(j -> new Contact[j])
//		);
//		return out;
//	}
//	
//	public Exposure[][] finaliseExposures() {
//		Exposure[][] out = new Exposure[exposure.length][];
//		IntStream.range(0, exposure.length).parallel().forEach(
//			//i -> out[i] = exposure[i].finish()
//			i -> out[i] = exposure[i].values().toArray(j -> new Exposure[j])
//		);
//		return out;
//	}

	
	public Contact[] getContactsForId(Integer ref) {
		return network[ref].values().toArray(j -> new Contact[j]);
	}
	
	public Exposure[] getExposuresForId(Integer ref) {
		return exposure[ref].values().toArray(j -> new Exposure[j]);
	}
}