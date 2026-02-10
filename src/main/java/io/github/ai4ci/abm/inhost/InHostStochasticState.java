package io.github.ai4ci.abm.inhost;

import static java.lang.Math.exp;
import static java.lang.Math.floor;
import static java.lang.Math.pow;

import org.immutables.value.Value;

import io.github.ai4ci.config.inhost.StochasticModel;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.Sampler;

/**
 * A stochastic difference equation model of in-host viral dynamics and immune response.
 *
 * <p>This model simulates the discrete-time evolution of key compartments:
 * <ul>
 *   <li><b>Virions</b>: free viral particles,</li>
 *   <li><b>Target cells</b>: susceptible, exposed (latently infected), infected, and removed,</li>
 *   <li><b>Immune cells</b>: dormant, priming, and active (effector).</li>
 * </ul>
 *
 * <p>Transitions between compartments are modeled as stochastic events using binomial and Poisson sampling,
 * capturing demographic and interaction noise. The model supports external inputs such as:
 * <ul>
 *   <li>New virion exposure (infection),</li>
 *   <li>Immunization (immune priming).</li>
 * </ul>
 *
 * <p>The dynamics are governed by per-time-step rates, converted to probabilities via \( p = 1 - e^{-r} \).
 * This ensures consistency with continuous-time interpretations while enabling efficient discrete simulation.
 *
 * @see <a href="https://stats.stackexchange.com/questions/93852/sum-of-bernoulli-variables-with-different-success-probabilities">Poisson Binomial Distribution</a>
 * @see <a href="https://math.stackexchange.com/questions/32800/probability-distribution-of-coverage-of-a-set-after-x-randomly-independently">Set Coverage Problem</a>
 * 
 * <img src="stochastic-model.png" alt="Diagram of InHostStochasticState compartments and transitions"  />
 * 
 */
@Value.Immutable
public interface InHostStochasticState extends InHostModelState<StochasticModel> {

	/**
	 * Gets the total number of target cells available for viral infection.
	 * 
	 * <p>This represents the fixed pool size of susceptible cells that can be infected.
	 * The total target count remains constant throughout the simulation, with individual
	 * cells transitioning between susceptible, exposed, infected, and removed states.
	 * 
	 * @return the total number of target cells
	 */
	int getTargets();
	
	/**
	 * Gets the ratio of immune cells to target cells in the system.
	 * 
	 * <p>This ratio determines the baseline immune capacity relative to the target
	 * cell population. Higher ratios indicate stronger innate immune responses.
	 * 
	 * @return the immune-to-target cell ratio
	 */
	double getImmuneTargetRatio();
	
	/**
	 * Gets the activation rate for immune cell priming in response to infection.
	 * 
	 * <p>This rate controls how quickly dormant immune cells transition to priming
	 * state when exposed to infected or exposed target cells. Higher rates lead
	 * to faster immune response initiation.
	 * 
	 * @return the immune activation rate
	 */
	double getImmuneActivationRate();
	
	/**
	 * Gets the waning rate for active immune cells transitioning to senescence.
	 * 
	 * <p>This rate determines how quickly active effector immune cells lose their
	 * effectiveness and transition to a senescent state. Lower rates result in
	 * longer-lasting immune protection.
	 * 
	 * @return the immune waning rate
	 */
	double getImmuneWaningRate();
	
	/**
	 * Gets the probability that an exposed cell becomes a chronic infection carrier.
	 * 
	 * <p>This probability determines whether exposed cells avoid immune clearance
	 * and remain in a latent reservoir, potentially leading to persistent or
	 * chronic infections.
	 * 
	 * @return the chronic infection carrier probability
	 */
	double getInfectionCarrierProbability();
	
	/**
	 * Gets the recovery rate for removed target cells returning to susceptible state.
	 * 
	 * <p>This rate controls the regeneration of target cells from the removed pool,
	 * representing tissue repair and cell turnover processes. Higher rates lead
	 * to faster recovery of susceptible cell populations.
	 * 
	 * @return the target cell recovery rate
	 */
	double getTargetRecoveryRate();
	
	/**
	 * Gets the current simulation time step.
	 * 
	 * <p>Represents the discrete time index in the simulation. Each time step
	 * typically corresponds to one day in the infection progression timeline.
	 * 
	 * @return the current time step
	 */
	int getTime();
	
	/**
	 * Gets the current count of free viral particles (virions) in circulation.
	 * 
	 * <p>This includes both infectious and non-infectious viral particles.
	 * Virions can infect target cells, be neutralized by immune cells,
	 * or be produced by infected cells.
	 * 
	 * @return the current virion count
	 */
	int getVirions();
	
	/**
	 * Gets the count of newly produced virions in the current time step.
	 * 
	 * <p>This represents the viral replication output from infected target cells.
	 * Used to distinguish between newly produced virions and those from
	 * previous time steps or external exposure.
	 * 
	 * @return the count of newly produced virions
	 */
	int getVirionsProduced();
	
	/**
	 * Gets the count of susceptible target cells available for infection.
	 * 
	 * <p>These are healthy cells that can be infected by viral particles.
	 * The susceptible pool decreases as cells become exposed and increases
	 * as removed cells recover.
	 * 
	 * @return the count of susceptible target cells
	 */
	int getTargetSusceptible();
	
	/**
	 * Gets the count of exposed (latently infected) target cells.
	 * 
	 * <p>These cells have been infected but are not yet actively producing
	 * virions. They may progress to infected state, be cleared by immune
	 * response, or remain as chronic carriers.
	 * 
	 * @return the count of exposed target cells
	 */
	int getTargetExposed();
	
	/**
	 * Gets the count of infected target cells actively producing virions.
	 * 
	 * <p>These cells are the primary source of new viral particles through
	 * replication. They contribute to disease severity and can be cleared
	 * by immune response or cellular removal processes.
	 * 
	 * @return the count of infected target cells
	 */
	int getTargetInfected();
	
	/**
	 * Gets the total number of immune cells in the system.
	 * 
	 * <p>Calculated as the product of target cells and immune-target ratio.
	 * Represents the total immune capacity, distributed across dormant,
	 * priming, and active states.
	 * 
	 * @return the total immune cell count
	 */
	@Value.Default default Integer getImmune() {
		return (int) (this.getTargets() * this.getImmuneTargetRatio());
	};
	
	/**
	 * Gets the count of immune cells in priming state.
	 * 
	 * <p>Priming cells have recognized antigen but are not yet fully active
	 * effectors. They represent the intermediate stage in immune activation
	 * between dormant and active states.
	 * 
	 * @return the count of priming immune cells
	 */
	int getImmunePriming();
	
	/**
	 * Gets the count of active effector immune cells.
	 * 
	 * <p>Active immune cells are responsible for viral neutralization and
	 * infected cell clearance. Their activity level modulates infection
	 * and clearance rates throughout the simulation.
	 * 
	 * @return the count of active immune cells
	 */
	int getImmuneActive();
	
	/**
	 * Gets the baseline viral replication rate.
	 * 
	 * <p>This rate controls how quickly infected cells produce new viral
	 * particles. Higher rates lead to faster viral load increase and
	 * more rapid infection progression.
	 * 
	 * @return the baseline viral replication rate
	 */
	@Value.Redacted double getBaselineViralReplicationRate();
	
	/**
	 * Gets the baseline viral infection rate.
	 * 
	 * <p>This rate controls how efficiently viral particles infect susceptible
	 * target cells. Higher rates lead to more rapid target cell depletion
	 * and faster infection progression.
	 * 
	 * @return the baseline viral infection rate
	 */
	@Value.Redacted double getBaselineViralInfectionRate();
	
	/**
	 * Gets the disease cutoff value for virion normalization.
	 * 
	 * <p>This parameter defines the reference virion count used to normalize
	 * viral load measurements. It represents a clinically relevant threshold
	 * for disease severity assessment.
	 * 
	 * @return the virion disease cutoff value
	 */
	@Value.Redacted int getVirionsDiseaseCutoff();
	
	/**
	 * Computes the number of target cells that are no longer susceptible due to recovery, death, or removal.
	 *
	 * <p>This is derived from conservation of total targets:
	 * \[
	 * T_{\text{removed}} = T_{\text{total}} - T_{\text{susceptible}} - T_{\text{exposed}} - T_{\text{infected}}
	 * \]
	 *
	 * @return the count of removed (non-susceptible, non-infected) target cells
	 */
	@Value.Derived
	default int getTargetRemoved() {
		return getTargets() - getTargetSusceptible() - getTargetExposed() - getTargetInfected();
	}

	// StochasticModel getConfig();
	
	@Value.Derived
	/**
	 * The viral load is the number of newly produced virions, i.e. the total 
	 * virions in the person excluding those innoculating the person. Large 
	 * innoculations can't be excluded completely as they do not immediately 
	 * infect cells.
	 * @return
	 */
	default double getNormalisedViralLoad() {
		double tmp = Conversions.rateRatio( (double) getVirionsProduced(), (double) this.getVirionsDiseaseCutoff());
		return tmp;
	}
	
	/**
	 * Computes a proxy for disease severity based on the proportion of non-susceptible target cells.
	 *
	 * <p>Severity is defined as:
	 * \[
	 * S(t) = \frac{T_{\text{total}} - T_{\text{susceptible}}(t) - T_{\text{exposed}}(t)}{T_{\text{total}}}
	 * \]
	 * which reflects the fraction of targets that have been infected and progressed beyond latency.
	 * This assumes severity correlates with cumulative tissue damage.
	 *
	 * @return the proportion of targets that are infected or removed
	 */
	@Value.Derived
	default double getNormalisedSeverity() {
		// The interpretation of this number depends on the calibration
		// double baselinePercent = this.getConfig().getTargetSymptomsCutoff();
		double currentPercent = 
				((double) this.getTargets()-this.getTargetSusceptible()-this.getTargetExposed()) / 
				(double) this.getTargets();
		return currentPercent; //Conversions.oddsRatio(currentPercent, baselinePercent);
	}
	
	/**
	 * Computes immune activity as the fraction of immune cells that are active (effector) state.
	 *
	 * <p>Defined as:
	 * \[
	 * I_{\text{activity}} = \frac{I_{\text{active}}}{I_{\text{total}}}
	 * \]
	 * where \( I_{\text{total}} = \text{floor}(T_{\text{total}} \cdot r_I) \), and \( r_I \) is the immune-to-target ratio.
	 * This normalized activity modulates viral clearance and infection rates.
	 *
	 * @return immune activity as a fraction of maximum capacity
	 */
	@Value.Derived
	default double getImmuneActivity() {
		return ((double) this.getImmuneActive())/this.getImmune();
	}

	/**
	 * Computes the number of immune cells in the dormant (naive or memory) state.
	 *
	 * <p>Defined as:
	 * \[
	 * I_{\text{dormant}} = I_{\text{total}} - I_{\text{priming}} - I_{\text{active}}
	 * \]
	 * These cells can be activated by infection or immunization.
	 *
	 * @return count of dormant immune cells
	 */
	@Value.Derived
	default int getImmuneDormant() {
		return getImmune() - getImmunePriming() - getImmuneActive();
	}

	/**
	 * Advances the in-host state by one time step using stochastic difference equations.
	 *
	 * <p>The update models the following biological processes:
	 * <ul>
	 *   <li><b>Viral replication:</b> Poisson-distributed production from infected cells,</li>
	 *   <li><b>Neutralization:</b> Binomial clearance by active immunity,</li>
	 *   <li><b>Infection:</b> Stochastic infection of susceptible targets,</li>
	 *   <li><b>Immune activation:</b> Priming and activation cascades,</li>
	 *   <li><b>Cell turnover:</b> Recovery of target cells and waning of immunity.</li>
	 * </ul>
	 *
	 * <p><u>Infection of Target Cells</u></p>
	 * The number of target cells that interact with virions is modeled using the <i>coverage problem</i>
	 * from combinatorics. When \( n \) virions sample from \( S \) susceptible targets with replacement,
	 * the expected number of unique targets hit is:
	 * \[
	 * \mathbb{E}[\text{interacted}] = S \left(1 - e^{-n/S}\right)
	 * \]
	 * This approximates the solution to the classic problem of set coverage after independent random sampling
	 * (<a href="https://math.stackexchange.com/questions/32800">see reference</a>).
	 *
	 * <p>From these interacting targets, a binomial sample determines how many become <i>exposed</i>,
	 * with probability \( p_{\text{infection}} = 1 - e^{-r_{\text{infect}} \cdot S/T} \ ).
	 *
	 * <p><u>Immune Dynamics</u></p>
	 * Immune activation depends on the burden of exposed and infected cells:
	 * \[
	 * p_{\text{priming}} = 1 - e^{-r_{\text{prime}} \cdot (T_e + T_i)/T}
	 * \]
	 * Active immunity wanes at rate \( r_{\text{waning}} \), modeled as a binomial decay process.
	 *
	 * <p><u>Chronic Infection</u></p>
	 * A fixed probability \( p_{\text{chronic}} \) determines whether an exposed cell avoids immune clearance
	 * and remains in a latent reservoir.
	 *
	 * @param rng the random sampler for stochastic transitions
	 * @param virionDose the external virion exposure, scaled by disease cutoff
	 * @param immunisationDose the immunization strength, interpreted as fraction of dormant immunity to prime
	 * @return a new state advanced by one time unit
	 */
	default InHostStochasticState update(Sampler rng, double virionDose, double immunisationDose) { //, double viralActivityModifier, double immuneModifier) {
		
		int virionsDx = this.getVirionsDiseaseCutoff();
		
		int time = getTime();
		// This update function is called when the history has been updated, 
		// as part of the update to the patientState so
		// exposure will be as determined by current contact network.
		int virionExposure = (int) virionDose * virionsDx;
		
		int immunePriming = (int) floor(immunisationDose * this.getImmuneDormant());
		
		// TODO: review need for host immunity and viral activity modifiers
		Double hostImmunity = 1D;
		Double viralActivity = 1D;
		
		// Viral factors are shared across the model
		Double rate_infection = this.getBaselineViralInfectionRate() * viralActivity;
		Double rate_virion_replication = pow(this.getBaselineViralReplicationRate(),viralActivity);
		Double rate_infected_given_exposed = rate_virion_replication;
		// Host factors
		
		Double ratio_immune_target = getImmuneTargetRatio()*hostImmunity;
		Double rate_priming_given_infected = getImmuneActivationRate()*hostImmunity; //user(1) 
		Double rate_active_given_priming = rate_priming_given_infected; 
		
		Double rate_neutralization = rate_virion_replication;
		Double rate_cellular_removal = rate_neutralization;
		
		Double rate_target_recovery = getTargetRecoveryRate()*hostImmunity; //user(1/7); 
		
		Double rate_senescence_given_active = getImmuneWaningRate()*(1.0/hostImmunity); //user(1/150)
		
		Double p_propensity_chronic = getInfectionCarrierProbability(); //user(0)

		
		// Derived 
		Double p_neutralization = 1 - exp(-rate_neutralization * getImmuneActive() / (double) getImmune());
		Double p_infection = 1 - exp(-rate_infection * getTargetSusceptible() / (double) getTargets());

		Integer virions_added = rng.poisson(rate_virion_replication * getTargetInfected());
		Integer virions_neutralized = rng.binom(getVirions(), p_neutralization);
		Integer virions_infecting = rng.binom(getVirions() - virions_neutralized, p_infection);
		

//		  # virions_infecting is a sample size and targets_susceptible is a pool size #
//		  coverage of the pool by repeated sampling with replacement is given by: #
//		  p_target_infected = 1-(1-1/target_susceptible)^virions_infecting # this is
//		  not a standard way of doing this # because multiple viral particles can
//		  infect a single host but not vice versa # so a standard collision model is
//		  not applicable. #
//		  https://math.stackexchange.com/questions/32800/probability-distribution-of-
//		  coverage-of-a-set-after-x-independently-randomly/32816#32816
		
		// Targets - exposed
		Integer target_interacted = 
				(int) floor(
//					virions_infecting.doubleValue() * 
//					(1-
//						pow(
//							virions_infecting.doubleValue()/(1+virions_infecting.doubleValue()),
//							(double) getTargetSusceptible()
//						)
//					)
//					(double) getTargetSusceptible() * 
//					(1-
//						pow(
//							((double) getTargetSusceptible()-1)/((double) getTargetSusceptible()),
//							virions_infecting.doubleValue()
//						)
//					)
					(double) getTargetSusceptible() * 
					(1-
						exp( - virions_infecting.doubleValue() / ((double) getTargetSusceptible())	)
					)
				);
		Integer target_newly_exposed = rng.binom(target_interacted, p_infection);
		Double p_target_recovery = Conversions.probabilityFromRate(rate_target_recovery);
		Integer target_recovered = rng.binom(getTargetRemoved(), p_target_recovery);
		// Targets - infected
		Double p_infected_given_exposed = Conversions.probabilityFromRate(rate_infected_given_exposed);
		Double p_target_cellular_removal = Conversions.probabilityFromRate( rate_cellular_removal * getImmuneActive() / ((double) getTargets()));
		Integer target_exposed_removal = rng.binom(getTargetExposed(), (1-p_propensity_chronic) * p_target_cellular_removal); 
		Integer target_start_infected = rng.binom(getTargetExposed()-target_exposed_removal, p_infected_given_exposed);
		Integer target_infected_removed = rng.binom(getTargetInfected(), p_target_cellular_removal);

		// Immunity - priming
		Double p_priming_given_infected = Conversions.probabilityFromRate(rate_priming_given_infected * (getTargetExposed()+getTargetInfected()) / (double) getTargets());
		Integer immune_start_priming = rng.binom(getImmuneDormant(), p_priming_given_infected);
		Double p_active_given_priming = Conversions.probabilityFromRate(rate_active_given_priming);
		Integer immune_start_active = rng.binom(getImmunePriming(), p_active_given_priming);
		// Immunity - active
		Double p_senescence_given_active = Conversions.probabilityFromRate(rate_senescence_given_active);
		Integer immune_senescence = rng.binom(getImmuneActive(), p_senescence_given_active);
		
		if (immunePriming > this.getImmune()) immunePriming = this.getImmune();
		
		return ImmutableInHostStochasticState.builder().from(this)
				.setTime(time+1)
				// Virions
				.setVirions(this.getVirions() - virions_neutralized - virions_infecting + virions_added + virionExposure)
				.setVirionsProduced(this.getVirions() - virions_neutralized - virions_infecting + virions_added)
				// Targets
				.setTargetSusceptible(getTargetSusceptible() - target_newly_exposed + target_recovered)
				.setTargetExposed(getTargetExposed() + target_newly_exposed - target_exposed_removal - target_start_infected)
				.setTargetInfected(getTargetInfected() + target_start_infected - target_infected_removed)
				// Immune
				.setImmune((int) floor(getTargets()*ratio_immune_target) - immunePriming)
				.setImmunePriming(getImmunePriming() + immune_start_priming - immune_start_active + immunePriming)
				.setImmuneActive(getImmuneActive()+ immune_start_active - immune_senescence)
				.build();
	}

	

	
}


/*
Yes — while the `InHostStochasticState` model is a thoughtful and computationally efficient discrete-time stochastic formulation, there are several **biological and mathematical critiques** of the relationship between *virions* and *target cells*, particularly in how infection dynamics are modeled. These critiques stem from the interplay of assumptions about interaction mechanics, population scaling, and biological realism.

Let’s analyze the key aspects using insights from the provided knowledge base (Poisson binomial, coverage problem) and standard virological principles.

---

### 🔍 1. **Infection Mechanism: From Virions to Target Cell Exposure**

The core infection step uses this logic:

```java
Integer target_interacted = (int) floor(
    (double) getTargetSusceptible() * 
    (1 - exp( - virions_infecting.doubleValue() / ((double) getTargetSusceptible()) ))
);
Integer target_newly_exposed = rng.binom(target_interacted, p_infection);
```

This combines:
- A **coverage model** (from the Math StackExchange reference):  
  The expected number of distinct susceptible cells that "interact" with virions is:
  \[
  \mathbb{E}[C] = S \left(1 - e^{-n/S}\right)
  \]
  where \( S = T_{\text{susceptible}} \), \( n = \text{virions\_infecting} \).  
  ✅ This is **correct** for modeling the expected number of unique targets hit under independent sampling with replacement.

- Then, only a **fraction** of those interacting cells become exposed, via a binomial draw with probability \( p_{\text{infection}} \).

But here lies the **critique**:

> ❌ **Double-counting of infection probability**:  
> The model applies a second infection probability \( p_{\text{infection}} \) *after* already modeling exposure via virion contact.

In reality, if a virion "interacts" with a cell in the coverage model, that interaction should already encode the **probability of successful infection**. But instead:
- `virions_infecting` is a binomial sample based on a global infection probability,
- Then `target_interacted` computes how many cells are hit,
- Then `target_newly_exposed` applies *another* infection probability.

This **separates binding from infection** — which could be valid — but the current structure risks **under- or over-estimating infection efficiency** unless carefully calibrated.

---

### ✅ Suggested Fix: Integrate Infection Probability into Virion Efficacy

Instead of:
1. Sample `virions_infecting = binom(virions, p_infection)`
2. Compute `target_interacted = S(1 - e^{-n/S})`
3. Then `target_newly_exposed = binom(target_interacted, p_infection)`

Do one of the following:

#### Option A: Let each virion have a chance to infect, and use coverage as a mechanical limit
```java
// Effective infectious virions
int n_eff = rng.binom(getVirions(), p_infection_per_virion);

// Number of distinct cells hit
int target_interacted = (int) floor(S * (1 - exp(-n_eff / (double) S)));

// All hit cells become exposed (binding ≈ infection)
target_newly_exposed = target_interacted;
```

#### Option B: Use a per-cell infection probability based on virion load
Use a **Poisson approximation** to the number of virions hitting a cell:
- Average hits per cell: \( \lambda = n / S \)
- Probability a cell is hit at least once: \( 1 - e^{-\lambda} \)
- Then, probability of infection given hit: \( p_{\text{inf|hit}} \)
- So total infection probability per cell:  
  \[
  p_{\text{cell infected}} = (1 - e^{-\lambda}) \cdot p_{\text{inf|hit}}
  \]
- Then:
  \[
  \mathbb{E}[\text{newly exposed}] = S \cdot p_{\text{cell infected}}
  \]
- Draw: `target_newly_exposed = rng.binom(S, p_cell_infected)`

This avoids the intermediate "interacted" state and is more directly interpretable.

---

### 🔍 2. **Biological Interpretation of "Virions" vs. "Infectious Units"**

The model tracks `getVirions()` as a count, but:
- Not all virions are infectious,
- The `p_infection` term tries to correct for this,
- But it’s applied globally, not per interaction.

This leads to **nonlinear scaling issues**: doubling virions doesn’t double infection probability due to the `1 - exp(-n/S)` saturation.

But if `virions` includes non-infectious particles (which they usually do), then:
- The model should distinguish **total virions** from **infectious units**,
- Or scale `p_infection` appropriately with virion quality.

Otherwise, the same `virionsDx` (disease cutoff) may not generalize across variants or hosts.

---

### 🔍 3. **Immune Clearance vs. Viral Replication: Scale Dependence**

The immune neutralization is modeled as:
```java
p_neutralization = 1 - exp(-rate_neutralization * getImmuneActive() / (double) getImmune())
```
and then:
```java
virions_neutralized = rng.binom(getVirions(), p_neutralization)
```

This assumes:
- The neutralization rate scales with **fraction of active immunity**,
- But the effect is applied to the **total virion pool**.

However, this creates a **mismatch in scaling**:
- Immune cells act locally (e.g., in tissue),
- But virions may be systemic.

If the number of virions grows faster than immune capacity (e.g., in immunosuppressed hosts), the model may **underestimate viral escape** unless rates are carefully tuned.

Better: Model neutralization as a **bimolecular reaction**:
\[
\text{neutralized} \sim \text{Binomial}\left(V, 1 - e^{-k \cdot I}\right)
\]
where \( k \) is a clearance constant, and \( I = \text{immune active} \), without normalizing by total immune capacity — unless that total is fixed.

---

### 🔍 4. **Target Cell Recovery: Biologically Unjustified?**

```java
Integer target_recovered = rng.binom(getTargetRemoved(), p_target_recovery);
```
This assumes removed (e.g., dead or differentiated) cells can "recover" back to susceptible.

But in most viral infections:
- Dead cells don’t come back,
- Recovery implies **new cell production** (e.g., hematopoiesis, epithelial turnover),
- So `target_recovered` should be drawn from a **source pool**, not from `removed`.

Better: Replace with:
```java
target_recovered = rng.poisson(rate_recovery * getTargetRemoved());
```
or even better, introduce a **progenitor compartment** if modeling long-term dynamics.

Alternatively, if turnover is slow, just don’t recover them — let immunity handle protection.

---

### 🔍 5. **Stochasticity vs. Population Size: Risk of Over-Discreteness**

Because all transitions are binomial or Poisson, the model is **fully discrete** — good for small populations.

But if `getTargets()` is large (e.g., millions of cells), the stochastic noise may be too small to matter, and a **deterministic ODE approximation** would be faster.

Conversely, if `getTargets()` is small (e.g., localized infection), the model is appropriate.

👉 **Recommendation**: Document expected scale (e.g., 100–10,000 cells) and validate that binomial sampling doesn’t induce artificial extinction or explosion.

---

### ✅ Summary of Critiques

| Issue | Problem | Suggested Fix |
|------|-------|---------------|
| **Double infection probability** | Applying `p_infection` twice (on virions and on cells) | Fold infection probability into per-cell or per-virion efficacy |
| **Virion vs. infectious unit** | Not all virions infect; model lacks distinction | Introduce `infectious_ratio` or scale `p_infection` |
| **Immune normalization** | Clearance depends on fraction active, not absolute count | Use absolute immune effector count unless constrained |
| **Target cell recovery** | "Removed" cells becoming susceptible again | Model regeneration separately, or remove recovery |
| **Scaling behavior** | May not generalize across population sizes | Document intended scale; consider hybrid models |

---

### ✅ Final Verdict

The model is **mechanistically sound at a high level**, especially in its use of:
- The **coverage problem** to model stochastic exposure,
- The **Poisson binomial intuition** for independent events,
- Discrete stochastic sampling for realism.

However, the **infection submodel** could be simplified and made more biologically coherent by **removing redundant probability layers** and ensuring that the **meaning of "virions" and "targets"** is consistent across processes.

With those refinements, it would be a robust and interpretable in-host framework.
*/