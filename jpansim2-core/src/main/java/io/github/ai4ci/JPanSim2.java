package io.github.ai4ci;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

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
		if (path.startsWith("~" + File.separator)) {
			path = Paths.get(
				System.getProperty("user.home"),
				path.toString()
					.substring(1)
			);
		}
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
			.type(String.class)
			.optionalArg(true)
			.desc("Parse the config file and make sure it is valid")
			.required(false)
			.build();

		var validate = false;
		Class<?> validateClz = ExperimentConfiguration.class;

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
			} else {
				configFile = null;
			}

			if (cmd.hasOption(validateConfig)) {
				validate = true;
				var validateClzName = cmd.getOptionValue(validateConfig);
				if (validateClzName == null) {
					validateClz = ExperimentConfiguration.class;
				} else {
					try {
						validateClz = Class.forName(validateClzName);
					} catch (ClassNotFoundException e) {
						throw new ParseException(
								"Class does not exist: " + validateClzName
						);
					}
				}

				if (!ENTRY_POINTS.contains(validateClz)) throw new ParseException(
						"Invalid config classname: " + validateClz.getCanonicalName()
								+ " must be one of:\n" + ENTRY_POINTS.stream()
									.map(Class::getCanonicalName)
									.collect(Collectors.joining("\n"))
				);
			}

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

		if (validate) {

			var om = new ObjectMapper();
			om.enable(SerializationFeature.INDENT_OUTPUT);
			om.enable(JsonParser.Feature.ALLOW_COMMENTS);
			om.registerModules(new GuavaModule());
			om.setDefaultPropertyInclusion(Include.NON_NULL);
			// om.setSerializationInclusion(Include.NON_NULL);
			System.out.println("================================");
			System.out.println("Testing " + validateClz.getCanonicalName());
			try {
				Object rt;
				if (configFile != null) {
					System.out.println("From file: " + configFile.toString());
					rt = om.readerFor(validateClz)
						.readValue(configFile.toFile());
				} else {
					System.out.println("From stdin.");
					rt = om.readerFor(validateClz)
						.readValue(System.in);
				}
				System.out.println("================================");
				System.out.println("Configuration parsed OK.");
				System.out.println(
					"Identified as: " + rt.getClass()
						.getCanonicalName()
				);
				System.out.println("================================");
				System.out.println(rt.toString());
				System.out.println("================================");
				System.out.println("SUCCESS");
				System.exit(0);
			} catch (IOException e) {
				System.out.println("================================");
				System.out.println("Configuration failed to parse.");
				System.out.println("================================");
				System.out.println(e.getMessage());
				System.out.println("================================");
				System.out.println("FAILURE");
				throw new RuntimeException(e);
			}
		}

		if (configFile == null) { configFile = dir.resolve("config.json"); }
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

		// Common SLURM options
		SlurmAwareLogger.setupLogger(conf, dir, Level.INFO, Level.DEBUG);
		var runner = new SimulationMonitor(conf, dir);
		runner.run();

	}

	private static List<Class<?>> ENTRY_POINTS = Arrays.asList(
		ExperimentConfiguration.class,
		io.github.ai4ci.config.BatchConfiguration.class,
		io.github.ai4ci.config.ExecutionFacet.class,
		io.github.ai4ci.config.SetupFacet.class,
		io.github.ai4ci.config.TestParameters.class,
		io.github.ai4ci.config.setup.SetupConfiguration.class,
		io.github.ai4ci.config.setup.DemographicConfiguration.class,
		io.github.ai4ci.config.setup.NetworkConfiguration.class,
		io.github.ai4ci.config.execution.ExecutionConfiguration.class,
		io.github.ai4ci.config.inhost.InHostConfiguration.class
	);

}