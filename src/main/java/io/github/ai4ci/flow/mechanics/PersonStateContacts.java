package io.github.ai4ci.flow.mechanics;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import io.github.ai4ci.abm.Contact;
import io.github.ai4ci.abm.Exposure;

/**
 * This is a temporary data structure that gets generated during the update
 * cycle to hold all the contact and exposures before they are written to the
 * individual {@link io.github.ai4ci.abm.PersonHistory} entries in the model. No
 * effort is made in this class to ensure that added items are unique.
 */
public class PersonStateContacts implements Serializable {

	// The contact network is an array of concurrent skip list maps. The index
	// of this array is the id of the primary contact. This is initialised for
	// the
	// whole population up front during the update cycle, and does not have to
	// be resized. The concurrent skip list maps hold the variable length of
	// contactees. This is not enforced to be unique which is up to the provider
	// The network is indexed by secondary contact id.

	ConcurrentHashMap<Integer, Contact>[] network;
	ConcurrentHashMap<Integer, Exposure>[] exposure;

	/**
	 * nodes is essentially the number of people in the model. Max size is the
	 * maximum number of contacts they could make. In the worst case this will
	 * create a data structure 2*nodes*maxSize big.
	 *
	 * @param nodes   the number of people in the model
	 * @param maxSize the maximum number of contacts a person could make in a day
	 */
	@SuppressWarnings("unchecked")
	public PersonStateContacts(int nodes, int maxSize) {
		this.network = new ConcurrentHashMap[nodes];
		this.exposure = new ConcurrentHashMap[nodes];

		for (var i = 0; i < nodes; i++) {
			this.network[i] = new ConcurrentHashMap<>();
			this.exposure[i] = new ConcurrentHashMap<>();
		}
	}

	/**
	 * Get the contacts for a given id. This is not thread safe and should only
	 * be called after all updates have been made to the contact network for the
	 * day.
	 *
	 * @param ref the id of the person whose contacts we want to retrieve
	 * @return an array of Contact objects representing the contacts for the
	 *         given id
	 */
	public Contact[] getContactsForId(Integer ref) {
		return this.network[ref].values().toArray(j -> new Contact[j]);
	}

	/**
	 * Get the exposures for a given id. This is not thread safe and should only
	 * be called after all updates have been made to the exposure network for the
	 * day.
	 *
	 * @param ref the id of the person whose exposures we want to retrieve
	 * @return an array of Exposure objects representing the exposures for the
	 *         given id
	 */
	public Exposure[] getExposuresForId(Integer ref) {
		return this.exposure[ref].values().toArray(j -> new Exposure[j]);
	}

	/**
	 * Get the contact map for a given id. This can be used to write to the
	 * contact map for a given id. It is thread safe to write to the contact map
	 * for any given id concurrently.
	 *
	 * @param i the id of the person whose contact map we want to retrieve
	 * @return a ConcurrentHashMap representing the contact map for the given id
	 */
	public ConcurrentHashMap<Integer, Contact> write(int i) {
		return this.network[i];
	}

	/**
	 * Get the exposure map for a given id.
	 *
	 * @param i the id of the person whose exposure map we want to retrieve
	 * @return a ConcurrentHashMap representing the exposure map for the given id
	 */
	public ConcurrentHashMap<Integer, Exposure> writeExp(int i) {
		return this.exposure[i];
	}
}