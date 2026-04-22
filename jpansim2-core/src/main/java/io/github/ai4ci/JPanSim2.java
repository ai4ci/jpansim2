package io.github.ai4ci;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Converter;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.mariuszgromada.math.mxparser.License;

import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.example.Experiment;
import io.github.ai4ci.flow.SimulationMonitor;

/**
 * Command-line entry for the simulator and a small launcher used by batch
 * systems.
 *
 * <p>
 * This class exposes a simple CLI for running experiments. Options accepted by
 * the main method are:
 *
 * <ul>
 * <li><b>-o, --output &lt;output&gt;</b> — Path to the output directory.
 * Defaults to the current working directory. A leading <code>~</code> is
 * expanded to the invoking user's home directory.</li>
 * <li><b>-c, --config &lt;config&gt;</b> — Path to the configuration JSON file.
 * By default the launcher looks for <code>config.json</code> in the output
 * directory.</li>
 * <li><b>-g, --generate-config &lt;generate-config&gt;</b> — generates a
 * default configuration</li>
 * <li><b>-v, --validate-config &lt;validate-config&gt;</b> — parses a
 * configuration file and displays a summary if successful</li>
 * </ul>
 *
 * Behaviour notes:
 * <ul>
 * <li>If the output directory does not exist it will be created (or a
 * RuntimeException is raised if creation fails).</li>
 * <li>If the configuration file cannot be found a RuntimeException is
 * raised.</li>
 * <li>On CLI parse errors the program prints usage help and exits (status 0) —
 * see the printed help for SLURM usage hints.</li>
 * </ul>
 *
 * <p>
 * The launcher integrates with SLURM workflows by accepting continuous batch
 * array parameters; see the usage help printed by the program for an example
 * invocation. For programmatic callers use {@link SimulationMonitor} and
 * {@link ExperimentConfiguration#readConfig(Path)} directly.
 */
public class JPanSim2 {

	private static Path expand(Path path) {
		if (path.startsWith(
			"~" + File.separator
		)) path = Paths.get(System.getProperty("user.home"), path.toString()
			.substring(1)
		);
		return path;
	}

	/**
	 * The main entry point for a command line or SLURM batch job.
	 *
	 * @throws InterruptedException if the simulation is interrupted by the user
	 *                              or a batch system signal. The simulation will
	 *                              attempt to save progress before exiting when
	 *                              interrupted.
	 * @throws IOException          if the output directory cannot be created or
	 *                              the configuration file cannot be read.
	 * @param args the command line arguments; see the class-level Javadoc for
	 *             accepted options and usage notes.
	 */
	public static void main(String... args)
			throws IOException, InterruptedException {

		License.iConfirmNonCommercialUse("rob.challen@bristol.ac.uk");

		var dir = SystemUtils.getUserDir()
			.toPath();
		Path configFile = null;

		Experiment experiment = null;

		// define options via CLI
		var options = new Options();

		var outputPath = Option.builder("o")
			.longOpt("output")
			.argName("output")
			.hasArg(true)
			.converter(Converter.PATH)
			.desc(
				"The path to the output directory. Defaults to the current working directory."
			)
			.required(false)
			.build();

		var configPath = Option.builder("c")
			.longOpt("config")
			.argName("config")
			.hasArg(true)
			.converter(Converter.PATH)
			.desc(
				"The path to the configuration file. Defaults to \"config.json\" in the output directory."
			)
			.required(false)
			.build();

		var configs = Arrays.stream(Experiment.values())
			.map(v -> v.name)
			.collect(Collectors.joining(",", "'", "'"));

		var generateConfigName = Option.builder("g")
			.longOpt("generate-config")
			.argName("generate-config")
			.hasArg(true)
			.type(String.class)
			.desc("The example configuration name to generate. One of: " + configs)
			.required(false)
			.build();

		var validateConfig = Option.builder("v")
			.longOpt("validate-config")
			.argName("validate-config")
			.hasArg(false)
			.desc("Parse the config file and make sure it is valid")
			.required(false)
			.build();

		var validate = false;

		options.addOption(outputPath);
		options.addOption(configPath);
		options.addOption(generateConfigName);
		options.addOption(validateConfig);
		// define parser
		CommandLine cmd;
		CommandLineParser parser = new DefaultParser();
		var helper = new HelpFormatter();

		try {
			cmd = parser.parse(options, args);

			if (cmd.hasOption(outputPath)) {
				var tmp = expand(cmd.getParsedOptionValue(outputPath));
				dir = tmp;
			}

			if (cmd.hasOption(generateConfigName)) {
				var tmp = cmd.getParsedOptionValue(generateConfigName)
					.toString();
				experiment = Arrays.stream(Experiment.values())
					.filter(v -> v.name.equalsIgnoreCase(tmp))
					.findFirst()
					.orElseThrow(
						() -> new RuntimeException(
								"Couldn't find experiment: " + generateConfigName
						)
					);
			}

			if (cmd.hasOption(configPath)) {
				var tmp = expand(cmd.getParsedOptionValue(configPath));
				configFile = tmp;
			} else
				configFile = dir.resolve("config.json");

			if (cmd.hasOption(validateConfig)) validate = true;

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			helper.printHelp(
				"Usage:",
				"JPanSim2 command line options.",
				options,
				"Slurm support: batch commands must be continuous and start at 1\n"
						+ "e.g. sbatch --array=1-32"
			);
			System.exit(0);
		}
		// finish configuration using CLI

		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new RuntimeException(
					"Could not create output directory at: " + dir
			);
		}

		if (!Files.exists(configFile)) {
			if (experiment == null) throw new RuntimeException(
					"Could not find configuration at: " + configFile
			);
			experiment.config.writeConfig(configFile, false);
			System.out.println(
				String.format(
					"Writing example configuration '%s' to file: %s",
					experiment.name,
					configFile
				)
			);
			System.exit(0);
		}

		var conf = ExperimentConfiguration.readConfig(configFile);
		if (validate) {
			System.out.println("================================");
			System.out.println("Configuration file validated OK.");
			var setup = conf.getSetup();
			var expt = conf.getExecution();
			System.out.println("================================");
			System.out.println("Setup configurations:");
			System.out.println("================================");
			setup.stream()
				.forEach(System.out::println);
			System.out.println("Execution configurations:");
			System.out.println("================================");
			setup.stream()
				.forEach(System.out::println);
			System.out.println("================================");
			System.out.println("SUMMARY:");
			System.out.println("================================");
			System.out
				.println("- Contains " + setup.size() + " setup configurations");
			System.out.println(
				"- Each setup contains " + expt.size() + " execution configurations"
			);
			System.out
				.println("Total simulations to run: " + setup.size() * expt.size());
			System.out.println("================================");
			System.out.println("SUCESS");
			System.exit(0);
		}

		// Common SLURM options
		SlurmAwareLogger.setupLogger(conf, dir, Level.INFO, Level.DEBUG);
		var runner = new SimulationMonitor(conf, dir);
		runner.run();

	}

}