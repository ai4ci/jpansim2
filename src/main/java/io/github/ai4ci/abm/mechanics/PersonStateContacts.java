package io.github.ai4ci.abm.mechanics;

import java.io.Serializable;
import java.util.concurrent.ConcurrentSkipListMap;

import io.github.ai4ci.abm.Contact;
import io.github.ai4ci.abm.Exposure;

/**
 * This is a temporary data structure that gets generated during the update
 * cycle to hold all the contact and exposures before they are written to the
 * individual {@link io.github.ai4ci.abm.PersonHistory} entries in the model.
 */
public class PersonStateContacts implements Serializable {
	//volatile ThreadSafeArray<Contact>[] network;
	ConcurrentSkipListMap<Integer,Contact>[] network;
	//volatile ThreadSafeArray<Exposure>[] exposure;
	ConcurrentSkipListMap<Integer,Exposure>[] exposure;
	
	@SuppressWarnings("unchecked")
	/** nodes is essentially the number of people in the model. Max size is the
	 * maximum number of contacts they could make. In the worst case this will
	 * create a data structure 2*nodes*maxSize big.
	 */
	public PersonStateContacts(int nodes, int maxSize) {
		network = new ConcurrentSkipListMap[nodes]; //ThreadSafeArray[nodes];
		exposure = new ConcurrentSkipListMap[nodes]; //new ThreadSafeArray[nodes];
		
		for (int i=0; i<nodes; i++) {
			network[i] = //new ThreadSafeArray<Contact>(Contact.class,maxSize);
					new ConcurrentSkipListMap<Integer,Contact>();
			exposure[i] = //ThreadSafeArray.empty(Exposure.class);
					new ConcurrentSkipListMap<Integer,Exposure>();
		}
	}
	
	public ConcurrentSkipListMap<Integer,Contact> write(int i) {
		return network[i];
	}
	
	public ConcurrentSkipListMap<Integer, Exposure> writeExp(int i) {
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