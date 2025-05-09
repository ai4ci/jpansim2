package io.github.ai4ci;

import java.nio.file.Path;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import io.github.ai4ci.config.BatchConfiguration;
import io.github.ai4ci.config.ExperimentConfiguration;

public class SlurmAwareLogger {

    public static void setupLogger(ExperimentConfiguration cfg, Path baseDirectory, Level console, Level file) {
    	
    	Path batchDirectory = cfg.getBatchDirectoryPath(baseDirectory);
    	
        LoggerContext ctx = Configurator.initialize(new NullConfiguration());
        Configuration config = ctx.getConfiguration();
        config.getAppenders().forEach((key, value) -> config.getRootLogger().removeAppender(value.getName()));

        String pattern = String.format(
        		"%%d{yyyy-MM-dd HH:mm:ss.SSS} [%s] [%%-5level] %%msg%%n",
        		cfg.getBatchConfig().getBatchName());
        
        Layout<?> layout = PatternLayout.newBuilder()
                .withPattern(pattern)
                .withConfiguration(config)
                .build();

        // Console Appender
        ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
                .setName("ConsoleAppender")
                .setLayout(layout)
                .build();
        
        consoleAppender.start();
        config.addAppender(consoleAppender);

        LoggerConfig rootLogger = config.getLoggerConfig(org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);
        rootLogger.addAppender(consoleAppender, console, null);
        
        if (!cfg.getBatchConfig().isSlurmBatch()) {
        
        	// File Appender if 
        	
	        String logFileName = batchDirectory.resolve("jpansim2.log").toString();
	        FileAppender fileAppender = FileAppender.newBuilder()
	                .withFileName(logFileName)
	                .setName("FileAppender")
	                .setLayout(layout)
	                .build();
	        
	        fileAppender.start();
	        config.addAppender(fileAppender);
	        rootLogger.addAppender(fileAppender, file, null);
        }

        ctx.updateLoggers();
    }
}
