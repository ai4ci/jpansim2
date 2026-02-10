/**
 * Package io.github.ai4ci.config.refdata
 *
 * <p>This package is a work in progress providing support for loading
 * reference demographic and related data from CSV files. The long term aim
 * is to offer a straightforward way to include reference demographics into
 * the simulation model so that initialisers and other components can use
 * empiric population data when constructing agents and locations.
 *
 * <p>At present the code in this package (for example
 * {@link io.github.ai4ci.config.refdata.UKCensus}) is not yet wired into the
 * main simulation. The package is intended to be used together with the
 * CSV repository utilities in {@link io.github.ai4ci.util.Repository} which
 * provide CSV parsing, simple foreign‑key resolution and lazy indexing.
 *
 * <p>Planned features include:
 * <ul>
 *   <li>CSV-driven loading of geographies, age/sex strata, commuting and
 *       industry data used to seed population and contact matrices.</li>
 *   <li>Lazy indexing by foreign keys to allow efficient joins between CSVs.</li>
 *   <li>Integration points with {@link io.github.ai4ci.config.execution.DemographicAdjustment}
 *       and person/initialiser code to apply demographic adjustments.</li>
 * </ul>
 *
 * <p>Downstream consumers (future) will include the default person
 * initialiser and other components such as
 * {@link io.github.ai4ci.flow.builders.DefaultPersonInitialiser} and
 * demographic adjustment logic that parameterises agent behaviour from
 * census derived lookups.
 *
 * @author Rob Challen
 */
package io.github.ai4ci.config.refdata;
