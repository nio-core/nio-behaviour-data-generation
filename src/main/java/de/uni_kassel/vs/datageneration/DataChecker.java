package de.uni_kassel.vs.datageneration;

import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DataChecker {
    public static void check(File[] files) throws IOException {
        for (File file : files) {
            if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                BufferedReader reader = new BufferedReader(new FileReader(file));

                String game = file.getName().replaceAll("\\[.*?\\]", "");
                game = game.substring(0, game.lastIndexOf('.'));

                // remove header
                String line = reader.readLine();

                int lineCount = 0;
                boolean moveFailure = false;
                while ((line = reader.readLine()) != null) {
                    lineCount++;

                    String[] fields = line.split(",");

                    String engine = fields[0];
                    String color = fields[1];

                    String inputs = fields[2];
                    for (String input : inputs.split(" ")) {
                        if (input.trim().length() > 4) {
                            moveFailure = true;
                        }
                    }

                    String output = fields[3].trim();
                    if (moveFailure) {
                        break;
                    }
                }
                if (lineCount < 2 || moveFailure) {
                    String pathname = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf("/")+1) + FilenameUtils.getBaseName(file.getName()) + ".broken";
                    File dest = new File(pathname);
                    if (!file.renameTo(dest)){
                        DebugLogger.writeError(DataChecker.class, "Error while handling rename to broken trainings/test data. File:" + file.getName());
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        // Processing Commandline Arguments
        Options options = new Options();

        Option help = new Option("?", "help", false, "prints this message");
        options.addOption(help);

        Option logArg = new Option("l", "log", true, "path to log folder");
        logArg.setRequired(true);
        logArg.setType(String.class);
        options.addOption(logArg);

        Option testArg = new Option("t", "test", true, "path to test folder");
        testArg.setRequired(true);
        testArg.setType(String.class);
        options.addOption(testArg);

        Option debugArg = new Option("d", "debug", false, "(optional) enables debug output");
        options.addOption(debugArg);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        // parsing cmd arguments
        String log = null;
        String test = null;
        try {
            cmd = parser.parse(options, args);
            test = cmd.getOptionValue(testArg.getOpt());
            log = cmd.getOptionValue(logArg.getOpt());
        } catch (ParseException | NumberFormatException e) {
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

        try {
            check(new File(test).listFiles());
            check(new File(log).listFiles());
        } catch (IOException e) {
            DebugLogger.writeError(DataChecker.class, "Error while handling inconsistent traning/test data");
            System.exit(1);
        }
    }
}
