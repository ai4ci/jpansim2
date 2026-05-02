# Calibration of Transmission Parameters to Target R₀

**Status**: Accepted

## Background

The model must reproduce a target basic reproduction number (R₀) given the social network structure and in-host viral dynamics. R₀ is the expected number of secondary infections from one typical infectious individual in a fully susceptible population. The calibration maps biological transmission properties (viral load profiles, per-contact transmission efficiency) to network-level R₀.

## Method

### Network-Level R₀ Adjustment

The model adjusts R₀ against the network structure using percolation theory. The critical transmissibility threshold for a configuration-free network is:

\[
T_c = \frac{\langle k \rangle}{\langle k^2 \rangle - \langle k \rangle}
\]

where \(\langle k \rangle\) is the mean degree and \(\langle k^2 \rangle\) is the second moment of the degree distribution. For a fully connected network reference:

\[
T_{\max} = \frac{\langle k \rangle}{\langle k \rangle^2 - \langle k \rangle}
\]

The adjusted R₀ is:

\[
R_{0,\text{adj}} = R_0 \times \frac{T_c}{T_{\max}}
\]

This decouples biological transmissibility from network topological effects.

### Viral Load Transmission Parameter

The target viral load transmission parameter (\(\beta\), called `parameter` or `viralLoadTransmissionParameter` in code) is found by root-finding. The model iterates:

\[
R_0(\beta) - R_{0,\text{adj}} = 0
\]

using Apache Commons Math `BrentSolver` (bracketing root-finder) because it combines superlinear convergence with guaranteed convergence on bracketed intervals.

### Expected Exposures Per Infection

For a given \(\beta\), expected exposures per infection is computed as:

\[
E[\text{exposures}] = \frac{2E}{N} \sum_{(i,j) \in E} \left( p_{\text{any}}(i \to j) + p_{\text{any}}(j \to i) \right)
\]

where \(p_{\text{any}}(i \to j)\) is the probability of at least one exposure from person \(i\) to person \(j\) over the infectious period.

### Fast Estimator: Poisson Binomial Quadratic Surrogate

For large networks, full enumeration of \(p_{\text{any}}(i \to j)\) over every directed edge pair is expensive. The `Estimator` class provides a quartic polynomial approximation:

On day \(t\) of an infection, the excess viral load is \(\delta_t = \max(V_t - 1, 0)\). The transmission probability on that day given contact probability \(k\) is:

\[
p_t = \beta \cdot k \cdot \delta_t
\]

The probability of at least one exposure over \(D\) infectious days:

\[
P(\text{any exposure}) = 1 - \prod_{t=1}^{D} (1 - k\beta \delta_t)
\]

Using the log expansion:

\[
\sum_{t=1}^{D} \log(1 - k\beta \delta_t) = \sum_{n=1}^{\infty} \frac{(-\beta k)^n}{n} B_n
\]

where \(B_n = \sum_{t=1}^{D} \delta_t^n\) are the raw moments of the excess viral load profile, truncated at \(n=4\) for a quartic surrogate:

\[
p_{\text{hat}}(\beta) = a_1\beta + a_2\beta^2 + a_3\beta^3 + a_4\beta^4
\]

The coefficients \(c_n\) are derived from the raw moments using Stirling numbers of the second kind:

\[
\begin{aligned}
c_1 &= d_1 \\
c_2 &= d_2 - \frac{d_1^2}{2} \\
c_3 &= d_3 - d_1 d_2 + \frac{d_1^3}{6} \\
c_4 &= d_4 - \frac{d_2^2}{2} - d_1 d_3 + \frac{d_1^2 d_2}{2} - \frac{d_1^4}{24}
\end{aligned}
\]

where \(d_n = k \cdot \frac{(-1)^{n+1} B_n}{n!}\).

## Testing

The `TestCalibration` test exercises the calibration pipeline end-to-end. The estimator coefficients are validated against the full enumeration in edge cases.

## Used in

- `abm/Calibration.java` — all calibration logic
- `abm/flow/DefaultOutbreakBaseliner.java` — invokes `inferViralLoadTransmissionParameter()` and `inferViralLoadTransmissionParameterErdosReyni()`
- `functions/ExoticDistributions.java` — network theory statistics (expected degree, variance)

## References

- Newman, M. E. J. (2002). "Distribution of degree of connectedness of networks." *Physical Review E* 66, 066123.
- Jackson, M. O. (2008). *Social and Economic Networks*. Princeton University Press.
- Apache Commons Math `BrentSolver` documentation.
