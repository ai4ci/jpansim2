# In-Host Stochastic Viral Dynamics Model

**Status**: Accepted

## Background

The stochastic in-host model uses discrete-time stochastic difference equations to simulate explicit compartments for virions, target cells in the infection cascade (susceptible, exposed, infected, removed), and immune cells (dormant, priming, active). This provides a mechanistic (instead of phenomenological) representation of within-host dynamics.

## Method

### Compartment Structure

**Virions**: Free viral particles.

| Process | Formula | Distribution |
|---------|---------|-------------|
| Replication | \(\lambda V \cdot I\) | Poisson(\(\text{rate}_{\text{virion\_replication}} \times I\)) |
| Neutralization | \(V \times p_{\text{neutral}}\) | Binomial(\(V\), \(p_{\text{neutralization}}\)) |
| Infection of targets | — | See coverage formula below |

**Target cells** (4 states: S, E, I, R):

| Transition | Probability | Distribution |
|------------|-------------|-------------|
| S → E (exposure) | \(p_{\text{infection}}\) | Binomial (with coverage correction) |
| E → I (progression) | \((1 - p_{\text{chronic}}) \times p_{\text{cellular\_removal}}\) | Binomial |
| E → R (latent removal) | \((1 - p_{\text{chronic}}) \times p_{\text{cellular\_removal}}\) | Binomial |
| R → S (recovery) | \(p_{\text{recovery}}\) | Binomial |

**Immune cells** (3 states: dormant, priming, active):

| Transition | Probability | Distribution |
|------------|-------------|-------------|
| Dormant → Priming | \(p_{\text{priming}}\) | Binomial |
| Priming → Active | \(p_{\text{active}}\) | Binomial |
| Active → Senescence | \(p_{\text{senescence}}\) | Binomial |

### Infection Modelling

The model uses a two-step infection approach:

1. **Virion-level**: Number of virions that successfully infect = Binomial(\(V_{\text{neutralised\_complement}}\), \(p_{\text{infection}}\))

2. **Target-level coverage correction**: The number of unique targets infected uses the set/coverage formula:

\[
\text{newly\_exposed} = \left\lfloor S_{\text{available}} \times (1 - \exp(-V_{\text{infecting}} / S_{\text{available}})) \right\rfloor
\]

This is mathematically equivalent to the occupancy problem: throwing \(V_{\text{infecting}}\) balls into \(S_{\text{available}}\) bins and counting occupied bins.

### Neutralization Rate

\[
p_{\text{neutralization}} = 1 - \exp\left(-\text{rate}_{\text{neutralization}} \times \frac{I_{\text{active}}}{I_{\text{total}}}\right)
\]

The neutralization fraction scales with the *fraction* of active immunity, not the absolute count. This is noted in the code as a potential issue.

### Derived Quantities

\[
\text{immuneActivity} = \frac{I_{\text{active}}}{I_{\text{total}}}
\]

\[
\text{normalisedSeverity} = \frac{T - S - E}{T} = \frac{I + R}{\text{total targets}}
\]

\[
\text{normalisedViralLoad} = \frac{\text{rateRatio}(V_{\text{produced}}, V_{\text{diseaseCutoff}})}{\text{rateRatio}(V_{\text{produced}}^{\max}, V_{\text{diseaseCutoff}})}
\]

### TODO / Known Critiques in Code

The embedded critique in `InHostStochasticState.java:603-796` raises several concerns:

1. **Double-counting of infection probability**: \(p_{\text{infection}}\) is applied both to virions (getting \(V_{\text{infecting}}\)) and then again to interacted targets (getting \(T_{\text{newly\_exposed}}\)). Suggested fix: use a single Poisson approximation.
2. **Virions vs. infectious units**: Not all virions are infectious; the model lacks a clear separation.
3. **Target cell recovery**: The R → S recovery transition may not be biologically justified.
4. **Large cell counts**: For large target cell counts (10,000 default), a deterministic ODE approximation would be faster with negligible accuracy loss.

## Testing

The stochastic model is exercised through integration tests in the phenom model tests because the in-host state is used in the Updater pipeline. No standalone unit tests for the stochastic compartment transitions exist.

## Used in

- `abm/inhost/InHostStochasticState.java` — the state implementation (796 lines)
- `config/inhost/StochasticModel.java` — configuration and parameter defaults

## References

- The coverage formula \(\lfloor S(1 - \exp(-V/S)) \rfloor\) is from the occupancy problem in probability theory.
- Standard compartmental viral dynamics models follow the framework of Perelson et al. (1996) and Nowak & May (2000).
