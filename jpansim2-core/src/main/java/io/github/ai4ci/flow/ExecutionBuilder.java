package io.github.ai4ci.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.ImmutableOutbreakBaseline;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.flow.builders.AbstractModelBuilder;
import io.github.ai4ci.flow.builders.DefaultModelBuilder;
import io.github.ai4ci.util.Cloner;
import io.github.ai4ci.util.Sampler;

/**
 * Builder that orchestrates setup, baselining and initialisation of an
 * {@link Outbreak} instance for an experiment.
 *
 * <p>
 * The {@code ExecutionBuilder} performs the three main phases required to turn
 * configuration objects into a runnable outbreak: setup, model baselining and
 * runtime initialisation. It delegates stage behaviour to a model builder which
 * implements {@link AbstractModelBuilder}.
 *
 * <p>
 * Important note: at present the implementation is hardwired to use
 * {@link DefaultModelBuilder} (see the constructor). The intended design is for
 * {@code ExecutionBuilder} to support compositional substitution of alternative
 * builders (for example subclasses of {@code DefaultModelBuilder} that mix
 * different default interfaces). The file includes clear TODO comments showing
 * the places to change to support factory injection, ServiceLoader discovery or
 * selection from {@link SetupConfiguration}.
 *
 * @author Rob Challen
 */
public class ExecutionBuilder {

	static Logger log = LoggerFactory.getLogger(ExecutionBuilder.class);

	/**
	 * Build an outbreak for an experiment from the supplied setup and execution
	 * configurations and an optional URN base string.
	 *
	 * @param setupConfig the setup configuration defining the population and
	 *                    network structure
	 * @param execConfig  the execution configuration defining runtime priors and
	 *                    parameters
	 * @param urnBase     an optional string to prefix the outbreak URN with (for
	 *                    example to distinguish different experiments or
	 *                    scenarios); if null the URN will be based on the setup
	 *                    and execution configuration alone
	 * @return a fully built and initialised outbreak ready for simulation
	 */
	public static Outbreak buildExperiment(
			SetupConfiguration setupConfig, ExecutionConfiguration execConfig,
			String urnBase
	) {

		ExecutionBuilder experiment = new ExecutionBuilder(setupConfig);
		experiment.setupOutbreak(urnBase);
		experiment.baselineModel(execConfig);
		experiment.initialiseStatus(execConfig);
		return experiment.build();
	}

	/**
	 * Utility method to check free memory in GB. This can be used to estimate
	 * the size of the outbreak that can be built without running out of memory.
	 * Note that this is a very rough estimate and actual memory usage will
	 * depend on many factors including the JVM's memory management and the
	 * specific data structures used in the outbreak model.
	 *
	 * @return the estimated free memory in gigabytes
	 */
	public static double freeMem() {
		Runtime runtime = Runtime.getRuntime();
		long allocatedMemory = runtime.totalMemory() - runtime.freeMemory();
		// allocatedMemory = allocatedMemory > maxMem ? maxMem : allocatedMemory;
		return ((double) runtime.maxMemory() - allocatedMemory)
				/ (1024 * 1024 * 1024);
	}

	SetupConfiguration setupConfig;

	ModifiableOutbreak outbreak;
	AbstractModelBuilder modelBuilder;

	ExecutionBuilder(SetupConfiguration setupConfig) {
		this.setupConfig = setupConfig;
		// NOTE: currently hardwired to DefaultModelBuilder. To support
		// compositional substitution of builders consider one of the following
		// approaches:
		// - Accept an AbstractModelBuilder (or a factory) as a constructor
		// parameter so callers can inject a custom builder implementation.
		// - Use a simple factory that chooses a builder implementation based
		// on values in SetupConfiguration (for example a "builderType"
		// string), allowing subclassed DefaultModelBuilder variants to be
		// selected without changing this class.
		// - Use ServiceLoader or a dependency injection framework to discover
		// available AbstractModelBuilder implementations at runtime and
		// select one according to configuration.
		// - Introduce a ModelBuilderFactory interface responsible for creating
		// builder instances; wire the factory via configuration or DI.
		//
		// The immediate technical change required is to replace the direct
		// construction below with a call to the chosen factory/selection
		// mechanism and store the resulting AbstractModelBuilder in
		// `this.modelBuilder`.
		this.modelBuilder = new DefaultModelBuilder();
		this.outbreak = Outbreak.createOutbreakStub();
	}

	void baselineModel(ExecutionConfiguration execConfig) {
		this.outbreak.setUrn(
				this.outbreak.getUrn() + ":" + execConfig.getName() + ":"
						+ execConfig.getReplicate()
		);
		Sampler sampler = Sampler.getSampler(this.outbreak.getUrn());
		this.outbreak.setExecutionConfiguration(execConfig);

		this.outbreak.getPeople().parallelStream().forEach(p -> {
			Sampler sampler2 = Sampler.getSampler(this.outbreak.getUrn());
			if (p instanceof ModifiablePerson) {
				ModifiablePerson m = (ModifiablePerson) p;

				ImmutablePersonBaseline.Builder builder = m.initialisedBaseline()
						? ImmutablePersonBaseline.builder().from(m.getBaseline())
						: ImmutablePersonBaseline.builder();

				// NOTE: baselining delegates to the current modelBuilder. If we
				// want to support alternative per-person baselining behaviour
				// via compositional builders then ensure the selected
				// AbstractModelBuilder provides the desired baselining
				// implementation (eg via mixing different DefaultPersonBaseliner
				// interfaces). Subclass selection or factory wiring is required.
				this.modelBuilder.doBaselinePerson(builder, p, sampler2);
				m.setBaseline(builder.build());
			}
		});

		// Calibrate R0 to a baseline transmission probability
		ImmutableOutbreakBaseline.Builder builder = this.outbreak
				.initialisedBaseline()
						? ImmutableOutbreakBaseline.builder()
								.from(this.outbreak.getBaseline())
						: ImmutableOutbreakBaseline.builder();

		this.modelBuilder.doBaselineOutbreak(builder, this.outbreak, sampler);
		this.outbreak.setBaseline(builder.build());

	}

	/**
	 * Return the built outbreak. This method should be called after setup,
	 * baselining and initialisation have been completed. It checks that the
	 * outbreak is fully initialised before returning it; if the outbreak is not
	 * initialised a RuntimeException is thrown to indicate that the build
	 * process has not been completed successfully. After returning the outbreak,
	 * the internal reference is set to null to prevent further modifications and
	 * to allow for garbage collection.
	 *
	 * @return the fully built and initialised outbreak ready for simulation
	 */
	public Outbreak build() {
		if (!this.outbreak.isInitialized())
			throw new RuntimeException("Not initialised");
		Outbreak tmp = this.outbreak;
		this.outbreak = null;
		return tmp;
	}

	/**
	 * Create a copy of this builder with the same setup configuration and a copy
	 * of the outbreak. The outbreak copy can be a shallow or deep copy depending
	 * on the estimated size of the outbreak; if estSize is negative a deep copy
	 * is performed, otherwise a shallow copy is used to save time and memory.
	 *
	 * This is used by the parallel execution framework to create separate
	 * builder instances for each parallel thread while sharing the same setup
	 * configuration. The outbreak copy allows each thread to work with its own
	 * outbreak instance without interference, while the setup configuration can
	 * be shared since it is immutable. {@link Cloner} is used to perform the
	 * copying; it should be noted that the efficiency of the copy operation will
	 * depend on the size of the outbreak and the type of copy performed. For
	 * large outbreaks, a shallow copy may be more efficient, while for smaller
	 * outbreaks a deep copy may be acceptable.
	 *
	 * @param estSize an estimate of the outbreak size used to determine the type
	 *                of copy; if negative a deep copy is performed, otherwise a
	 *                shallow copy is used
	 * @return a new ExecutionBuilder instance with the same setup configuration
	 *         and a copy of the outbreak
	 */
	public ExecutionBuilder copy(long estSize) {
		ExecutionBuilder tmp = new ExecutionBuilder(this.setupConfig);
		if (estSize < 0) {
			tmp.outbreak = Cloner.copy(this.outbreak);
		} else {
			tmp.outbreak = Cloner.copy(this.outbreak, estSize);
		}
		return tmp;
	}

	void initialiseStatus(ExecutionConfiguration execConfig) {
		Sampler sampler = Sampler.getSampler(this.outbreak.getUrn());

		ImmutableOutbreakState.Builder builder = ImmutableOutbreakState.builder();
		if (this.outbreak.initialisedCurrentState()) {
			builder.from(this.outbreak.getCurrentState());
		}

		builder.setEntity(this.outbreak).setTime(0);

		// NOTE: ensure the selected modelBuilder has been initialised/selected
		// prior to invoking initialise; the current code assumes a single
		// implementation and therefore no selection step is required. When
		// adding compositional builder selection insert the selection logic
		// earlier (for example in the constructor) so that modelBuilder here
		// refers to the chosen implementation.
		this.modelBuilder.doInitialiseOutbreak(builder, this.outbreak, sampler);
		this.outbreak.setCurrentState(builder.build());

		this.outbreak.getPeople().parallelStream().forEach(p -> {
			if (p instanceof ModifiablePerson) {
				ModifiablePerson m = (ModifiablePerson) p;

				Sampler sampler2 = Sampler.getSampler(this.outbreak.getUrn());

				ImmutablePersonState.Builder builder2 = ImmutablePersonState
						.builder();
				if (m.initialisedCurrentState()) {
					builder2.from(m.getCurrentState());
				}

				builder2.setEntity(p).setTime(0);

				// Delegate person initialisation to the selected builder. When
				// enabling compositional selection ensure the chosen builder
				// implements the combination of DefaultInHost*Initialiser
				// interfaces you require (or provide alternatives via
				// subclassing the DefaultModelBuilder).
				m.setCurrentState(
						this.modelBuilder.doInitialisePerson(builder2, p, sampler2)
				);

			} else
				throw new RuntimeException("Not modifiable person");
		});

	}

	void setupOutbreak(String urnBase) {
		this.outbreak.setUrn(
				(urnBase != null ? urnBase + ":" : "") + this.setupConfig.getName()
						+ ":" + this.setupConfig.getReplicate()
		);
		Sampler sampler = Sampler.getSampler(this.outbreak.getUrn());
		// TODO: when builder selection is compositional ensure selection
		// happens before any builder method is invoked (eg here). For example
		// if using a factory the factory must be consulted during construction
		// and `this.modelBuilder` must be the chosen implementation.
		this.modelBuilder
				.doSetupOutbreak(this.outbreak, this.setupConfig, sampler);
	}

}
