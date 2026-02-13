/**
 * Bayesian risk estimation models for individual infectious probability
 * assessment.
 *
 * <p>
 * This package provides sophisticated temporal Bayesian filtering
 * implementations for estimating the probability that an individual is
 * infectious on any given day, incorporating both direct evidence (symptoms,
 * test results) and indirect evidence (contact exposures) with proper temporal
 * weighting.
 *
 * <h2>Core Components</h2>
 * <ul>
 * <li>{@link io.github.ai4ci.abm.riskmodel.RiskModel} - Main interface for
 * Bayesian risk estimation with temporal evidence accumulation</li>
 * <li>{@link io.github.ai4ci.abm.riskmodel.ConvolutionFilter} - Temporal
 * weighting functions for evidence convolution across time</li>
 * <li>{@link io.github.ai4ci.abm.riskmodel.ImmutableRiskModel} - Immutable
 * implementation of the risk model interface</li>
 * </ul>
 *
 * <h2>Bayesian Framework</h2>
 * <p>
 * The models use Bayes' theorem with log-odds transformation for computational
 * stability: \[ \log\left(\frac{p}{1-p}\right)_{\text{posterior}} =
 * \log\left(\frac{p}{1-p}\right)_{\text{prior}} + \log(\text{likelihood ratio})
 * \]
 *
 * <h2>Temporal Evidence Processing</h2>
 * <p>
 * Evidence is processed using convolution filters that weight information
 * appropriately across time, accounting for:
 * <ul>
 * <li><b>Symptom evolution</b>: Symptoms develop over time and provide
 * retrospective information</li>
 * <li><b>Test delays</b>: Test results become available after sampling with
 * variable delays</li>
 * <li><b>Contact recency</b>: Recent contacts provide more relevant risk
 * information</li>
 * </ul>
 *
 * <h2>Evidence Sources</h2>
 * <p>
 * The risk model incorporates three types of convolution-weighted evidence:
 *
 * <h3>1. Symptom Evidence</h3>
 * <p>
 * Uses symptom sensitivity and specificity to calculate log-likelihood ratios:
 * \[ LR_{\text{symptom}} = \log\left(\frac{\text{sensitivity}}{1 -
 * \text{specificity}}\right) \times P_{\text{report}} \] \[
 * LR_{\text{no-symptom}} = \log\left(\frac{1 -
 * \text{sensitivity}}{\text{specificity}}\right) \times P_{\text{no-report}} \]
 *
 * <h3>2. Test Evidence</h3>
 * <p>
 * Handles both immediate tests (LFTs) and delayed tests (PCRs) with proper
 * temporal convolution to account for result delays and test characteristics.
 *
 * <h3>3. Contact Evidence</h3>
 * <p>
 * Calculates risk from contact exposures using recursive risk assessment: \[
 * LR_{\text{indirect}} = \sum_{i=0}^{N_{\text{contacts}}}
 * \sum_{\text{contact}\in\text{contacts}_i} LR_{\text{contact}}(i) \times
 * K_{\text{contacts}}(i) \]
 *
 * <h2>Temporal Window Management</h2>
 * <p>
 * The model maintains evidence for a limited temporal window determined by: \[
 * \text{maxLength} = \max(\text{symptomKernel.retrospectiveSize},
 * \text{testKernel.retrospectiveSize}, \text{contactsKernel.retrospectiveSize})
 * \] This ensures computational efficiency while retaining relevant historical
 * evidence.
 *
 * <h2>Daily Update Cycle</h2>
 * <p>
 * The risk model is updated daily through a temporal Bayesian filtering
 * process: \[ \mathbf{evidence}_{t} = \text{update}(\mathbf{evidence}_{t-1},
 * \text{observations}_t) \] Each update incorporates new evidence and
 * re-evaluates past evidence in light of new information.
 *
 * <h2>Integration Points</h2>
 * <p>
 * The risk model integrates with:
 * <ul>
 * <li>{@link io.github.ai4ci.abm.PersonHistory} - For symptom and test
 * evidence</li>
 * <li>{@link io.github.ai4ci.abm.Contact} - For contact exposure
 * assessment</li>
 * <li>{@link io.github.ai4ci.config.execution.ExecutionConfiguration} - For
 * kernel configuration</li>
 * <li>{@link io.github.ai4ci.util.Conversions} - For log-odds
 * transformations</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <ul>
 * <li><b>Contact tracing</b>: Assessing risk from exposure events</li>
 * <li><b>Testing prioritization</b>: Identifying high-risk individuals for
 * testing</li>
 * <li><b>Behavioral interventions</b>: Informing isolation and quarantine
 * decisions</li>
 * <li><b>Epidemiological monitoring</b>: Longitudinal surveillance and trend
 * detection</li>
 * <li><b>Risk-based policy</b>: Implementing targeted interventions</li>
 * </ul>
 *
 * @see RiskModel
 * @see ConvolutionFilter
 * @see io.github.ai4ci.util.Conversions
 * @see io.github.ai4ci.abm.PersonHistory
 */
package io.github.ai4ci.abm.riskmodel;