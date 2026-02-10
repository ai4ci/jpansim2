/**
 * JPanSim2 — package overview and quick-run instructions.
 *
 * <p>
 * This package contains the entry points and runtime plumbing for the JPanSim2
 * agent-based outbreak simulator. The following notes summarise the common
 * steps for building and running the model from the command line, the
 * command-line options accepted by the launcher, and recommended usage with
 * SLURM for larger runs.
 *
 * <h2>Build</h2>
 * <p>
 * Use Maven to build a runnable jar with dependencies:
 *
 * <pre>
 * mvn -DskipTests package
 * </pre>
 *
 * This produces an executable jar under <code>target/</code>, for example
 * <code>target/jpansim2-0.1.1-jar-with-dependencies.jar</code>.
 *
 * <h2>Run (local)</h2>
 * <p>
 * Run the simulator with the provided launcher class. A minimal example:
 *
 * <pre>
 * java -jar target/jpansim2-0.1.1-jar-with-dependencies.jar -o ~/tmp
 * </pre>
 *
 * <p>
 * Command-line options recognised by the launcher (see
 * {@link io.github.ai4ci.JPanSim2}):
 * <ul>
 * <li><b>-o, --output &lt;output&gt;</b> — Path to the output directory.
 * Defaults to the current working directory. A leading <code>~</code> is
 * expanded to the user's home directory. If the directory does not exist it
 * will be created.</li>
 * <li><b>-c, --config &lt;config&gt;</b> — Path to a JSON configuration file.
 * By default the launcher looks for <code>config.json</code> in the output
 * directory. The configuration may define multiple setups and facets which the
 * launcher will expand into the set of simulations to run.</li>
 * </ul>
 *
 * <h2>Run (SLURM)</h2>
 * <p>
 * For large experiments the repository provides <code>slurm.sh</code> which is
 * a simple SLURM launcher script. On a cluster invoke the script with a task
 * array to parallelise environment setups. Example:
 *
 * <pre>
 * sbatch --array=1-32 slurm.sh
 * </pre>
 *
 * <p>
 * Notes when using SLURM:
 * <ul>
 * <li>The slurm script sets <code>JAVA_OPTS</code> and uses <code>srun</code>
 * to launch the jar. Adjust <code>-Xms</code>/<code>-Xmx</code> and other JVM
 * flags to match node memory and CPU allocation.</li>
 * <li>Each SLURM task typically writes outputs into a numbered subdirectory of
 * the output directory (e.g. <code>out/1</code>, <code>out/2</code>) to avoid
 * concurrent writes to the same paths.</li>
 * </ul>
 *
 * <h2>Configuration files and examples</h2>
 * <p>
 * Example configuration files and the JSON schema are provided under
 * <code>src/test/resources</code>. The JSON schema documents supported fields;
 * additional configuration details are available in the javadoc for
 * <code>io.github.ai4ci.config</code>.
 *
 * <h2>Programmatic usage</h2>
 * <p>
 * For programmatic integration or tests use
 * {@link io.github.ai4ci.flow.SimulationMonitor} and
 * {@link io.github.ai4ci.config.ExperimentConfiguration#readConfig(java.nio.file.Path)}
 * to construct and run experiments directly from Java.
 */
@Data.Style
package io.github.ai4ci;
