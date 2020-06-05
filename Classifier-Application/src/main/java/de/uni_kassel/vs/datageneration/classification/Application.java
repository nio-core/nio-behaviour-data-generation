package de.uni_kassel.vs.datageneration.classification;

import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import org.apache.commons.cli.*;
import org.reflections.Reflections;

import java.io.File;

public class Application {

    public static void main(String[] args) {

        // Processing Commandline Arguments
        Options options = new Options();

        Option help = new Option("?", "help", false, "prints this message");
        options.addOption(help);

        Option logFolderArg = new Option("l", "log", true, "path to log folder");
        logFolderArg.setRequired(true);
        logFolderArg.setType(String.class);
        options.addOption(logFolderArg);

        Option logTestFolderArg = new Option("t", "test", true, "path to test folder");
        logTestFolderArg.setRequired(true);
        logTestFolderArg.setType(String.class);
        options.addOption(logTestFolderArg);

        Option randomStringArg = new Option("r", "random", false, "uses random string (only distance)");
        randomStringArg.setRequired(false);
        options.addOption(randomStringArg);

        StringBuilder description = new StringBuilder("classifier to use:\n[");
        for (Class clazz : Classifier.getClasses()) {
            description.append(clazz.getSimpleName());
            description.append(", ");
        }
        description.deleteCharAt(description.lastIndexOf(","));
        description.deleteCharAt(description.lastIndexOf(" "));
        description.append("]");
        Option classifierArg = new Option("c", "classifier", true, description.toString());
        classifierArg.setRequired(true);
        classifierArg.setType(String.class);
        options.addOption(classifierArg);

        Option debugArg = new Option("d", "debug", false, "(optional) enables debug output");
        options.addOption(debugArg);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        // parsing cmd arguments
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            for (String arg : args) {
                if (!(arg.equals("-?") || arg.equals("--help"))) {
                    System.out.println(e.getMessage());
                }
            }
            formatter.printHelp("help message", options);
            System.exit(1);
        }

        if (cmd.hasOption("?")) {
            formatter.printHelp("help message", options);
        }

        boolean debug = cmd.hasOption("d");
        DebugLogger.init(debug, formatter, options);

        String classifierString = cmd.getOptionValue(classifierArg.getOpt());

        for (int i = 0; i < 100; i++) {
            ClassifierController clCon = new ClassifierController();
            clCon.start(new File(cmd.getOptionValue(logFolderArg.getOpt())), new File(cmd.getOptionValue(logTestFolderArg.getOpt())), classifierString, cmd.hasOption("r"));
       	    System.gc();
	 }
    }
}
