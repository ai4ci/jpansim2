package io.github.ai4ci.abm.mechanics;

import java.io.Serializable;

import io.github.ai4ci.abm.Contact;
import io.github.ai4ci.abm.Exposure;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.util.PagedArray;

public class PersonStateContacts implements Serializable {
	PagedArray<Contact>[] network;
	PagedArray<Exposure>[] exposure;
	int maxSize;
	
	@SuppressWarnings("unchecked")
	public PersonStateContacts(int nodes, int maxSize) {
		network = new PagedArray[nodes];
		exposure = new PagedArray[nodes];
		this.maxSize = maxSize;	
	}
	
	public PagedArray<Contact> write(int i) {
		if (network[i]==null) network[i] = new PagedArray<Contact>(Contact.class, maxSize);
		return network[i];
	}
	
	public PagedArray<Exposure> writeExp(int i) {
		if (exposure[i]==null) exposure[i] = new PagedArray<Exposure>(Exposure.class, maxSize);
		return exposure[i];
	}
	
	public PagedArray<Contact> get(int i) {
		if (network[i]==null) return PagedArray.empty(Contact.class);
		return network[i];
	}
	
	public PagedArray<Exposure> getExp(int i) {
		if (exposure[i]==null) return PagedArray.empty(Exposure.class);
		return exposure[i];
	}
}