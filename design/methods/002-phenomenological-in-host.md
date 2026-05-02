# In-Host Phenomenological Viral Dynamics Model

**Status**: Accepted

## Background

The phenomenological in-host model uses curve-fitted biphasic logistic functions to reproduce observed infection patterns: viral load growth, peak, and decay. It is designed to be computationally efficient while capturing the key features of within-host viral dynamics relevant to transmission: viral load trajectory, infectivity, and symptom severity.

The model supports multiple exposures (reinfection or vaccination events), combining their effects through probability complementarity (superposition).

## Method

### Biphasic Logistic Curve

The viral load or immune activity trajectory is modelled as:

\[
y(t) = f(t; r_g, s_g) \times (1 - f(t; r_d, s_d))
\]

where \(f(x; r, s)\) is the logistic function:

\[
f(x; r, s) = \frac{1}{1 + \exp(-r(x - s))}
\]

and \(r_g, s_g\) control growth rate and inflection, while \(r_d, s_d\) control decay timing. The product of growth and reversed-decay produces the characteristic asymmetric peak shape.

### Calibration

The biphasic logistic is calibrated to 5 reference points:
- Onset time (first detectable viral load)
- Peak delay (days from onset to peak)
- Post-peak duration (days from peak to return to baseline)
- Threshold level (viral load cutoff for detection, default 1.0)
- Peak level (maximum normalised viral load)

The calibration uses `MathematicalFunction` (mXparser) to fit the logistic parameters to these reference points.

### Multiple Exposure Superposition

For multiple exposures \(e = 1, \dots, E\), the combined viral load normalised to \([0,1]\):

\[
V_{\text{norm}}(t) = 1 - \prod_{e=1}^{E} (1 - v_e(t))
\]

This is a probability complement: \(V_e(t)\) is the normalised viral load contribution from exposure \(e\), and the product computes the probability that *no* exposure is active at time \(t\). The immune activity follows the same pattern:

\[
I(t) = 1 - \prod_{e=1}^{E} (1 - i_e(t))
\]

### Severity

Severity is the peak viral load from any single exposure:

\[
S(t) = \max_{e} (v_e(t))
\]

This does not sum across exposures, reflecting the observation that symptom severity is driven by the most intense active infection.

### Viral Load Normalisation

The normalised viral load used for transmission is:

\[
V_{\text{normalized}} = \frac{\text{rateRatio}(V(t), V_{\text{cutoff}})}{\text{rateRatio}(V_{\max}, V_{\text{cutoff}})}
\]

where \(\text{rateRatio}(x, y) = \frac{x/y}{1 + x/y}\) (logistic saturation).

### Exposure Model

When a new exposure arrives, the growth curve is offset so that it starts at the correct dose level:

\[
g_e = F^{-1}(\text{dose} \times \text{unit}, r_g, s_g)
\]

The decay curve is offset to align with current immunity:

\[
d_e = \max(0, F^{-1}(\text{immuneActivity}, r_d, s_d))
\]

## Testing

`TestPhenomenologicalModel` in `flow/mechanics/` exercises the model. The model also features in `TestViralLoadModel` for viral load-specific validation.

## Used in

- `abm/inhost/InHostPhenomenologicalState.java` — the state implementation
- `config/inhost/PhenomenologicalModel.java` — configuration and parameter defaults
- `abm/flow/DefaultInHostPhenomenologicalStateInitialiser.java` — state initialisation

## References

- The biphasic logistic approach follows standard epidemiological practice for fitting viral load trajectories (e.g., Kucharski et al., Lancet Infectious Diseases 2020).
- The mXparser library is used for symbolic expression evaluation during calibration.
