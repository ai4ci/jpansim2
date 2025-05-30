package io.github.ai4ci.abm.mechanics;

import java.io.Serializable;

import io.github.ai4ci.abm.Contact;
import io.github.ai4ci.abm.Exposure;
import io.github.ai4ci.util.ThreadSafeArray;

/**
 * This is a temporary data structure that gets generated during the update
 * cycle to hold all the contact and exposures before they are written to the
 * individual {@link io.github.ai4ci.abm.PersonHistory} entries in the model.
 */
public class PersonStateContacts implements Serializable {
	ThreadSafeArray<Contact>[] network;
	ThreadSafeArray<Exposure>[] exposure;
	int maxSize;
	
	@SuppressWarnings("unchecked")
	/** nodes is essentially the number of people in the model. Max size is the
	 * maximum number of contacts they could make. In the worst case this will
	 * create a data structure 2*nodes*maxSize big.
	 */
	public PersonStateContacts(int nodes, int maxSize) {
		network = new ThreadSafeArray[nodes];
		exposure = new ThreadSafeArray[nodes];
		this.maxSize = maxSize;	
	}
	
	public synchronized ThreadSafeArray<Contact> write(int i) {
		if (network[i]==null) network[i] = new ThreadSafeArray<Contact>(Contact.class, maxSize);
		return network[i];
	}
	
	public synchronized ThreadSafeArray<Exposure> writeExp(int i) {
		if (exposure[i]==null) exposure[i] = new ThreadSafeArray<Exposure>(Exposure.class, maxSize);
		return exposure[i];
	}
	
	public ThreadSafeArray<Contact> get(int i) {
		if (network[i]==null) return ThreadSafeArray.empty(Contact.class);
		return network[i];
	}
	
	public ThreadSafeArray<Exposure> getExp(int i) {
		if (exposure[i]==null) return ThreadSafeArray.empty(Exposure.class);
		return exposure[i];
	}
}