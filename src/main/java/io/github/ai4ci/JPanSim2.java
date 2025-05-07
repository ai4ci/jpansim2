package io.github.ai4ci;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

import io.github.ai4ci.config.Exporters;
import io.github.ai4ci.config.ImmutableBatchConfiguration;
import io.github.ai4ci.flow.BatchRunner;

public class JPanSim2 {

	/**
	 * The main entry point for a SLURM batch job.
	 * 
	 * 
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String... args) throws IOException, InterruptedException {
		
		// define options
        Options options = new Options();
        
        Option duration = Option.builder("d").longOpt("duration")
        	.argName("duration")
        	.hasArg(true)
        	.converter(Converter.NUMBER)
        	.desc("The duration of the simulations in this experiment, i.e. number of steps.")
        	.required(true)
        	.build(); 
        
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
        
        Option urn = Option.builder("u").longOpt("urn")
	    	.argName("urn")
	    	.hasArg(true)
	    	.desc("The URN for this experiment. This is optional and defaults to nothing.")
	    	.required(false)
	    	.build(); 
        
        Option export = Option.builder("e").longOpt("export")
    	    	.argName("export")
    	    	.hasArg(true)
    	    	.desc("A list of exporters that are to be included.\n"+
    	    			"This should take the form of a comma separated list with valid values:\n"+
    	    			Arrays.stream(Exporters.values()).map(e -> e.name()).collect(Collectors.joining(","))+"\n"+
    	    			"or the special value 'ALL' which exports everything"
    	    	)
    	    	.valueSeparator(',')
    	    	.required(false)
    	    	.build(); 
        
        options.addOption(duration);
        options.addOption(outputPath);
        options.addOption(configPath);
        options.addOption(urn);
        options.addOption(export);
        
        Configurator.initialize(new DefaultConfiguration());
		Configurator.setRootLevel(Level.DEBUG);
     
        // define parser
        CommandLine cmd;
        CommandLineParser parser = new DefaultParser();
        HelpFormatter helper = new HelpFormatter();

        ImmutableBatchConfiguration.Builder cfg = ImmutableBatchConfiguration.builder();
        
        try {
            cmd = parser.parse(options, args);
            
            cfg.setSimulationDuration(
            	(int) ((long) cmd.getParsedOptionValue(duration))
            );
            
            if (cmd.hasOption(outputPath)) {
                Path tmp = expand(cmd.getParsedOptionValue(outputPath));
                try {
					Files.createDirectories(tmp);
				} catch (IOException e) {
					throw new RuntimeException("Could not create output directory at: "+tmp);
				}
                cfg.setDirectoryPath(tmp);
            }
            
            if (cmd.hasOption(configPath)) {
                Path tmp = expand(cmd.getParsedOptionValue(configPath));
                if (!Files.exists(tmp)) throw new RuntimeException("Could not find configuration at: "+tmp);
                cfg.setExecutionConfigurationPath(tmp);
            }
            
            if (cmd.hasOption(urn)) {
            	cfg.setUrnBase(cmd.getOptionValue(urn));
            }
            
            if (cmd.hasOption(export)) {
            	String tmp = cmd.getOptionValue(export).trim();
            	if (tmp.equals("ALL")) {
            		cfg.setExporters(Exporters.values());
            	} else {
	            	cfg.setExporters(
		            	Arrays.stream(tmp.split(","))
		            		.map(n -> {
		            			try {
		            				return Exporters.valueOf(n.trim());
		            			} catch (IllegalArgumentException e) {
		            				throw new RuntimeException(n+" is not a valid option for export.");
		            			}
		            		})
		            		.toArray( Exporters[]::new )
	            	);
            	}
            }
            
            {
	            String tmp = System.getProperty("SLURM_ARRAY_TASK_ID"); // will be set to the job array index value or null.
	            if (tmp != null) cfg.setBatchNumber(Integer.parseInt(tmp));
            }
            {
	            String tmp = System.getProperty("SLURM_ARRAY_TASK_COUNT"); //will be set to the number of tasks in the job array or null.
	            if (tmp != null) cfg.setBatchTotal(Integer.parseInt(tmp));
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            helper.printHelp(
            		"Usage:",  "JPanSim2 command line options.", options, 
            		"Slurm support: batch commands must be continuous and start at 1\n"+
            		"e.g. sbatch --array=1-32");
            System.exit(0);
        }
        
        BatchRunner runner = new BatchRunner(cfg.build()); 
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