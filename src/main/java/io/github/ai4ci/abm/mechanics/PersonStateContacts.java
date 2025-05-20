package io.github.ai4ci.abm.mechanics;

import java.io.Serializable;

import io.github.ai4ci.abm.Contact;
import io.github.ai4ci.abm.Exposure;
import io.github.ai4ci.util.ThreadSafeArray;

public class PersonStateContacts implements Serializable {
	ThreadSafeArray<Contact>[] network;
	ThreadSafeArray<Exposure>[] exposure;
	int maxSize;
	
	@SuppressWarnings("unchecked")
	public PersonStateContacts(int nodes, int maxSize) {
		network = new ThreadSafeArray[nodes];
		exposure = new ThreadSafeArray[nodes];
		this.maxSize = maxSize;	
	}
	
	public ThreadSafeArray<Contact> write(int i) {
		if (network[i]==null) network[i] = new ThreadSafeArray<Contact>(Contact.class, maxSize);
		return network[i];
	}
	
	public ThreadSafeArray<Exposure> writeExp(int i) {
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