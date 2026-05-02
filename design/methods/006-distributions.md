# Distribution and Function Fitting

**Status**: Accepted

## Background

JPanSim2 uses a wide variety of distributions for:
- Sampling agent baseline attributes (mobility, compliance)
- Modelling incubation and infectious periods
- Delay distributions for test results and symptom reporting
- Empirical data fitting from observed patterns

The distribution system supports parametric families, empirical CDFs, user-defined expressions, and delay profiles.

## Method

### Parametric Distributions

The `SimpleDistribution` factory provides parametric families:

| Family | Parameters | Usage |
|--------|-----------|-------|
| Beta | mean, sd | Bounded proportions (e.g., compliance) |
| Log-normal | median, scale | Positive skewed durations (incubation, delay) |
| Normal | mean, sd | Symmetric quantities with known bounds |
| Binomial | n, p or mean, sd | Count data (number of contacts) |
| Poisson | rate | Count data with mean = variance |
| Neg-binomial | mean, sd | Over-dispersed counts |
| Gamma | mean, sd | Positive continuous variables |
| Point | value (degenerate) | Fixed constants |
| Uniform | lower, upper | Unbounded exploration |
| Unimodal-Beta | mean, dispersion | Beta constrained to be unimodal |

**Unimodal-Beta**: The dispersion parameter in \([0, 1]\) constrains the standard deviation:

\[
\sigma = \sqrt{\min\left(\frac{\mu^2(1-\mu)}{1+\mu}, \frac{(1-\mu)^2\mu}{2-\mu}\right)} \times \text{dispersion}
\]

This ensures the beta distribution remains unimodal (mode at \((\alpha-1)/(\alpha+\beta-2)\)).

### Empirical Distributions

Empirical CDFs are built from sorted data with a cubic spline on the logit scale:

\[
\log\text{it}(F(x)) \approx \text{spline}_{\text{cubic}}(\text{link}(x))
\]

Density is derived via the chain rule:

\[
f(x) = \frac{d}{dx}F(x) = \text{expit}'(\text{spline}(\text{link}(x))) \times \text{spline}'(\text{link}(x)) \times \text{link}'(x)
\]

Data is downsampled using `KNOTS=50` to limit the spline complexity.

### User-Defined Mathematical Functions

`MathematicalFunction` wraps the mXparser library for user-defined string expressions (e.g., `"x^2 * exp(-x/5)"`). The function is evaluated at 100 knots and fitted with a cubic spline.

### Delay Distributions

`DelayDistribution` models discrete-time delay profiles:

\[
\text{density}[i] = \frac{\text{profile}[i]}{\sum \text{profile}} \times p_{\text{affected}}
\]

\[
\text{survival}[i] = 1 - F[i]
\]

\[
\text{hazard}[i] = \frac{\text{density}[i]}{\text{survival}[i-1]}
\]

Delay distributions support convolution with other distributions (e.g., incubation + symptom onset delay = total time from infection to symptom).

### Interpolation

Multiple interpolation methods:
- `CubicSplineInterpolator` — monotone cubic spline (avoids overshooting)
- `LoessInterpolator` — LOESS non-parametric smoothing
- `LinearInterpolator` — piecewise linear

### Link Functions

Transformations applied to data or predicted values:
- `NONE` — Identity: \(f(x) = x\)
- `LOG` — Natural log: \(f(x) = \ln(x)\)
- `LOGIT` — Logit: \(f(x) = \ln(x/(1-x))\)

### Resampled Distribution

Combines two distributions by sampling \(N = 10,000\) pairs \((X_i, Y_i)\) and applying a combiner function \(Z = c(X, Y)\).

## Testing

`TestDistribution`, `TestDelayDistribution`, `TestEmpirical`, `TestEmpiricalDistribution`, `TestEmpiricalFunction`, `TestFunctions`, and `TestMathematicalFunction` exercise the distribution and function families.

## Used in

- `functions/SimpleDistribution.java` — parametric factory (556 lines)
- `functions/Distribution.java` — base interface
- `functions/EmpiricalDistribution.java` — CDF from data (382 lines)
- `functions/DelayDistribution.java` — delay profiles (442 lines)
- `functions/MathematicalFunction.java` — user-defined expressions (195 lines)
- `util/Sampler.java` — thread-local RNG, uses all distributions
- `abm/flow/DefaultPersonBaseliner.java` — CDF sampling of agent baselines
- `functions/ExoticDistributions.java` — Hilbert curve distances, network statistics

## References

- The unimodal-beta constraint follows from the standard result that the beta distribution is unimodal when \(\alpha, \beta > 1\), with bounds on the coefficients of variation.
- Cubic splines on the logit scale for CDFs prevent boundary artefacts (probabilities > 1 or < 0).
