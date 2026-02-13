package io.github.ai4ci;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import io.github.ai4ci.config.ExperimentConfiguration;

/**
 * Logging setup helper that adapts the logging sinks depending on whether the
 * simulator is running inside a SLURM batch job or interactively.
 *
 * <p>
 * Rationale for routing logging to console when running under SLURM:
 * <ul>
 * <li>SLURM captures and manages the stdout/stderr streams for each job/task
 * and stores them in job-specific files (e.g. <code>slurm-{jobid}.out</code>).
 * Writing to the console ensures that a single canonical per-task log file is
 * produced by the scheduler rather than having several processes write
 * concurrently to a shared log file on a network filesystem.</li>
 * <li>Using console output avoids contention and atomicity issues that can
 * arise when multiple parallel tasks attempt to append to the same file (which
 * may be on NFS), and reduces the need for additional file locking logic inside
 * the application.</li>
 * <li>Scheduler-captured logs are easier to aggregate, view, and associate with
 * a particular job or array task when debugging or post-processing. It also
 * preserves SLURM-provided job metadata (timestamps, job id) together with the
 * application output.</li>
 * <li>When running interactively (not under SLURM), a file appender is created
 * so logs are written to a consistent location inside the experiment
 * <code>batchDirectory</code>. This makes it easy to inspect logs from local
 * development runs.</li>
 * </ul>
 *
 * <p>
 * Behaviour summary:
 * <ul>
 * <li>If <code>cfg.getBatchConfig().isSlurmBatch()</code> is true, the logger
 * configures a console appender only; SLURM (or whatever batch system is in
 * use) is expected to capture that output into its per-job files.</li>
 * <li>If not running under a SLURM batch, a FileAppender is also created and an
 * existing <code>jpansim2.log</code> file in the batch directory is removed
 * before starting to keep local runs tidy.</li>
 * </ul>
 */
public class SlurmAwareLogger {

	/**
	 * Configure logging for the experiment run.
	 *
	 * <p>
	 * This method creates a <b>console</b> appender in all cases and will add a
	 * <b>file</b> appender only when
	 * <code>cfg.getBatchConfig().isSlurmBatch()</code> is false. The console
	 * appender is intentionally used when running under SLURM so that the
	 * scheduler can capture the complete per-task output in its job files. See
	 * class-level Javadoc for rationale.
	 *
	 * @param cfg           the experiment configuration (used to determine batch
	 *                      mode and names)
	 * @param baseDirectory the experiment base/batch directory where local logs
	 *                      are written
	 * @param console       the log level to use for the console appender
	 * @param file          the log level to use for the file appender (only used
	 *                      when not running under SLURM)
	 */
	public static void setupLogger(
			ExperimentConfiguration cfg, Path baseDirectory, Level console,
			Level file
	) {

		var batchDirectory = cfg.getBatchDirectoryPath(baseDirectory);

		var ctx = Configurator.initialize(new NullConfiguration());
		var config = ctx.getConfiguration();
		config.getAppenders().forEach(
				(key, value) -> config.getRootLogger()
						.removeAppender(value.getName())
		);

		var pattern = String.format(
				"%%d{yyyy-MM-dd HH:mm:ss.SSS} [%s] [%%-5level] %%msg%%n",
				cfg.getBatchConfig().getBatchName()
		);

		Layout<?> layout = PatternLayout.newBuilder().withPattern(pattern)
				.withConfiguration(config).build();

		// Console Appender — always configured; SLURM captures stdout/stderr
		var consoleAppender = ConsoleAppender.newBuilder()
				.setName("ConsoleAppender").setLayout(layout).build();

		consoleAppender.start();
		config.addAppender(consoleAppender);

		var rootLogger = config.getLoggerConfig(
				org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME
		);
		rootLogger.setLevel(Level.DEBUG);
		rootLogger.addAppender(consoleAppender, console, null);

		if (!cfg.getBatchConfig().isSlurmBatch()) {

			// File Appender only if SLURM not running otherwise console output
			// is directed to file by SLURM. Writing a local per-run file is useful
			// for interactive development and reproducible local debugging.

			var logFileName = batchDirectory.resolve("jpansim2.log").toString();
			try {
				Files.deleteIfExists(batchDirectory.resolve("jpansim2.log"));
			} catch (IOException e) {
				System.out
						.println("Problem deleting old log files: " + e.getMessage());
			}

			var fileAppender = FileAppender.newBuilder().withFileName(logFileName)
					.setName("FileAppender").setLayout(layout).build();

			fileAppender.start();
			config.addAppender(fileAppender);
			rootLogger.addAppender(fileAppender, file, null);
		}

		ctx.updateLoggers();
	}
}