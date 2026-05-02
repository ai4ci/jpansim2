# Bayesian Temporal Risk Estimation — Contact Tracing Model

**Status**: Accepted

## Background

Each agent maintains a Bayesian risk model that estimates the probability of being infectious today based on all past and present evidence: symptoms, test results, and detected contacts with potentially infectious individuals. This is the mathematical core of smart-agent contact tracing and risk-based testing behaviours.

## Method

### Log-Odds Framework

Risk is maintained in log-odds space for numerical stability and additive combination of evidence:

\[
\text{logit}(p_{\text{posterior}}) = \text{logit}(p_{\text{prior}}) + LR_{\text{direct}} + LR_{\text{indirect}}
\]

where:
- \(p_{\text{prior}} = 0.0025\) (fixed prior, about 1 in 400 population prevalence)
- \(LR_{\text{direct}}\) is the log-likelihood ratio from symptoms and test results
- \(LR_{\text{indirect}}\) is the log-likelihood ratio from contacts with other agents

### Log-Odds Ratio for Probability

\[
\text{logit}(p) = \ln\left(\frac{p}{1 - p}\right)
\]

The posterior probability is recovered via:

\[
p = \text{expit}(\text{logit}(p)) = \frac{1}{1 + \exp(-\text{logit}(p))}
\]

### Direct Evidence — Symptoms

When an agent reports being symptomatic, the log-odds contribution is positive (supporting infection). Non-reporting of symptoms by a compliant agent provides weak negative evidence:

- \(P(\text{reporting} \mid \text{symptomatic}) = 1.0\) (PROB_REPORTING_POSITIVE_SYMPTOMS)
- \(P(\text{reporting negative} \mid \text{not symptomatic}) = 0.02\) (PROB_REPORTING_NEGATIVE_SYMPTOMS)

The log-odds are computed as:

\[
LR_{\text{symptom}} = \ln\left(\frac{P(\text{symptom} \mid \text{infectious})}{P(\text{symptom} \mid \text{not infectious})}\right)
\]

which is then weighted by the temporal kernel.

### Direct Evidence — Tests

Each test result contributes a log-likelihood ratio at its reported delay \(d\):

\[
LR_{\text{test}}(d) = \ln\left(\frac{P(\text{result} \mid \text{true state}, d)}{P(\text{result} \mid \text{other state}, d)}\right)
\]

The test result stores `logLikelihoodRatio(delay)` which is computed from the test's sensitivity, specificity, and the person's true viral load at the sampling time.

### Indirect Evidence — Contact Tracing

For each detected contact, the contact's own direct LR from the same day contributes to this agent's risk:

\[
LR_{\text{indirect}} = \sum_{c \in \text{contacts}} LR_{\text{direct}}^{(c)} \times K_{\text{contacts}}(\text{age\_gap}, \tau) / E[NC]
\]

where the contacts kernel normalises by expected number of contacts so the total signal doesn't grow linearly with network degree.

### Convolution Kernels — Temporal Weighting

Evidence from past days is weighted by temporal kernels that encode how diagnostic information decays over time:

\[
\text{logit}(p_{\text{posterior}}) = \text{logit}(p_{\text{prior}}) + \sum_{\tau=-\text{prospective}}^{\text{retrospective}} K_{\text{type}}(\tau) \times LR_{\text{type}}(\text{history}[\tau])
\]

Three kernels:
- **Symptom kernel**: \(K_{\text{symptom}}(\tau)\) — symmetric weighting around \(\tau = 0\). Symptoms reported on day \(t\) inform risk on day \(t\) (retrospective: \(\tau < 0\)) and on future days (prospective: \(\tau > 0\)).
- **Test kernel**: \(K_{\text{test}}(\tau)\) — accounts for result delays. A test taken on day \(t-d\) with positive result on day \(t\) has weight at \(\tau = d\). Negative results have weight only when the result is actually reported.
- **Contacts kernel**: \(K_{\text{contacts}}(\tau)\) — temporal weighting of contact exposure risk.

### Daily Retrospective Updating

The risk model is recalculated every day because past evidence gains new meaning as contacts test and develop symptoms. This is a form of **retrospective updating**: what we knew about agent A's infection risk three days ago changes when agent B (a detected contact) tests positive today.

The implementation uses `updateDirectLogOdds()` which shifts the evidence array by one index each day and adds today's observations.

## Testing

`TestRiskModel` in `flow/mechanics/` validates the Bayesian updating logic including kernel application, evidence accumulation, and log-odds combination.

## Used in

- `abm/riskmodel/RiskModel.java` — core risk estimation (582 lines)
- `abm/riskmodel/ConvolutionFilter.java` — temporal weighting kernels
- `flow/mechanics/Updater.java` — calls `setRiskModel()` each day for each person

## References

- The log-odds framework follows standard Bayesian diagnostic reasoning (e.g., Fagan's nomogram).
- The retrospective updating approach is inspired by modern contact-tracing algorithms that incorporate time-varying evidence sources.
