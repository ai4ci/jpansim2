package io.github.ai4ci;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.flow.SimulationMonitor;

public class JPanSim2 {

	/**
	 * The main entry point for a command line or SLURM batch job.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String... args) throws IOException, InterruptedException {
		
		Path dir = SystemUtils.getUserDir().toPath();
		Path configFile = dir.resolve("config.json");
		
			// define options via CLI
	        Options options = new Options();
	        
	        Option outputPath = Option.builder("o").longOpt("output")
	        	.argName("output")
	        	.hasArg(true)
	        	.converter(Converter.PATH)
	        	.desc("The path to the output directory. Defaults to the current working directory.")
	        	.required(false)
	        	.build();
	        
	        Option configPath = Option.builder("c").longOpt("config")
	        	.argName("config")
	        	.hasArg(true)
	        	.converter(Converter.PATH)
	        	.desc("The path to the configuration file. Defaults to \"config.json\" in the output directory.")
	        	.required(false)
	        	.build(); 
	        
	        options.addOption(outputPath);
	        options.addOption(configPath);
	        // define parser
	        CommandLine cmd;
	        CommandLineParser parser = new DefaultParser();
	        HelpFormatter helper = new HelpFormatter();
	        
	        try {
	            cmd = parser.parse(options, args);
	            
	            if (cmd.hasOption(outputPath)) {
	                Path tmp = expand(cmd.getParsedOptionValue(outputPath));
	                dir = tmp;
	            }
	            
	            if (cmd.hasOption(configPath)) {
	                Path tmp = expand(cmd.getParsedOptionValue(configPath));
	                configFile = tmp;
	            }
	            
	        } catch (ParseException e) {
	            System.out.println(e.getMessage());
	            helper.printHelp(
	            		"Usage:",  "JPanSim2 command line options.", options, 
	            		"Slurm support: batch commands must be continuous and start at 1\n"+
	            		"e.g. sbatch --array=1-32");
	            System.exit(0);
	        }
	        // finish configuration using CLI
		
		
	try {
		Files.createDirectories(dir);
	} catch (IOException e) {
		throw new RuntimeException("Could not create output directory at: "+dir);
	}
	
	if (!Files.exists(configFile)) throw new RuntimeException("Could not find configuration at: "+configFile);
    
	ExperimentConfiguration conf = ExperimentConfiguration.readConfig(configFile);
	
	// Common SLURM options
    SlurmAwareLogger.setupLogger(conf, dir, Level.INFO, Level.DEBUG);
    SimulationMonitor runner = new SimulationMonitor(conf, dir); 
    runner.run();
		
    }
		
	public static Path expand(Path path) {
		if (path.startsWith("~" + File.separator)) {
		    path =  Paths.get(
		    		System.getProperty("user.home"),
		    		path.toString().substring(1)
		    );
		}
		return path;
	}
	
}