# In-Host Markov Chain Disease Progression Model

**Status**: Accepted

## Background

The Markov state model is the simplest of the three in-host approaches. It sacrifices biological detail for computational efficiency and direct mapping to observable epidemiological rates (incubation, infectiousness, symptom onset, recovery/hospitalisation/death). It is designed for scenarios where the computational cost of tracking individual viral dynamics is prohibitive, or where the available data (case reports, test positivity) only constrain macro-level outcomes rather than within-host trajectories.

## Method

### Disease-State Machine

A discrete-time Markov chain for infection progression:

\[
\text{SUSCEPTIBLE} \to \text{EXPOSED} \to \text{INFECTIOUS} \to \text{IMMUNE} \to \text{SUSCEPTIBLE}
\]

Transitions are daily Bernoulli draws with probabilities derived from the log-normal duration parameters:

\[
p(\text{leave state } s \text{ in one day}) = \frac{1}{\text{mean\_duration}_s}
\]

(For log-normal duration with mean \(\mu\) and standard deviation \(\sigma\), the daily hazard is approximated as \(1/\mu\).)

### Symptom-State Machine

Independent disease-state chain for symptom severity:

\[
\text{ASYMPTOMATIC} \to \text{SYMPTOMATIC} \to \text{HOSPITALISED} \to \text{DEAD}
\]

The probability of becoming symptomatic given infection:

\[
p(\text{symptomatic}) = 1 - (1 - r_c)^{1/D}
\]

where \(r_c\) is the infection-case rate (\(1 - \text{asymptomaticFraction}\)) and \(D\) is the average infectious duration.

Regression paths (e.g., HOSPITALISED → SYMPTOMATIC, IMMUNE → SUSCEPTIBLE) are also modelled.

### Derived Quantities

| Quantity | Formula |
|----------|---------|
| `immuneActivity` | 1.0 if IMMUNE, else 0.0 (binary) |
| `normalisedSeverity` | Sampled uniformly within bands defined by symptom state, calibrated so \(P(S > 1 - r_{\text{event}}) = r_{\text{event}}\) |
| `normalisedViralLoad` | 1.5 if INFECTIOUS, 0.5 if EXPOSED, else 0.0 (fixed coarse proxy) |

### Immunisation and Waning

If a person has received immunisation (vaccination), they transition directly to IMMUNE. The IMMUNE state eventually wanes back to SUSCEPTIBLE via a log-normal duration parameter.

## Testing

The Markov model is exercised through integration tests in the phenom model tests because the in-host state is used in the Updater pipeline. No standalone unit tests for the Markov state transitions exist.

## Used in

- `abm/inhost/InHostMarkovState.java` — the state implementation (~594 lines)
- `abm/inhost/InHostMarkovStateMachine.java` — transition logic for disease and symptom states
- `config/inhost/MarkovStateModel.java` — configuration and parameter defaults

## References

- Standard discrete-time Markov chain formulations for disease progression (e.g., health-state transition models in health economics).
- The probability derivation \(p = 1 - (1 - r_c)^{1/D}\) follows from the equivalence of geometric and Bernoulli processes.
