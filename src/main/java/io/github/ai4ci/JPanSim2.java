//package io.github.ai4ci;
//
//import org.apache.commons.cli.*;
//
//public class JPanSim2 {
//
//	/**
//	 * The main entry point for a SLURM batch job.
//	 * 
//	 * 
//	 * @param args
//	 */
//	public static void main(String... args) {
//		
//		// define options
//        Options options = new Options();
//        
//        Option alpha = new Option("a", "alpha", false, "Activate feature alpha");
//        options.addOption(alpha);
//        
//        Option config = Option.builder("c").longOpt("config")
//                .argName("config")
//                .hasArg()
//                .required(true)
//                .desc("Set configuration file location").build();
//        options.addOption(config);
//     
//        // define parser
//        CommandLine cmd;
//        CommandLineParser parser = new DefaultParser();
//        HelpFormatter helper = new HelpFormatter();
//
//        try {
//            cmd = parser.parse(options, args);
//            if(cmd.hasOption("a")) {
//                System.out.println("Alpha activated");
//            }
//          
//            if (cmd.hasOption(config)) {
//                String opt_config = cmd.get.getOptionValue(config);
//                System.out.println("Config set to " + opt_config);
//            }
//        } catch (ParseException e) {
//            System.out.println(e.getMessage());
//            helper.printHelp("Usage:", options);
//            System.exit(0);
//        }
//    }
//		
//	}
//	
//}
