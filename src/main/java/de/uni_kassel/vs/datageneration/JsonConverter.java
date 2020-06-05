package de.uni_kassel.vs.datageneration;

import com.google.gson.Gson;
import de.uni_kassel.vs.datageneration.classification.instances.Turn;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.LinkedList;

public class JsonConverter implements Serializable {

    public static void convert(File file) throws IOException {
        String game = file.getName().substring(file.getName().lastIndexOf("]")+1, file.getName().lastIndexOf("."));

        LinkedList<Turn> turns = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split(",");
            Turn turn = new Turn();

            turn.setGame(game);
            turn.setEngine(fields[0].trim());
            turn.setColor(fields[1].trim());

            String inputs = fields[2];
            LinkedList<String> list = new LinkedList<>();
            for (String input : inputs.split(" ")) {
                list.add(input.trim());
            }

            turn.setInputs(list);
            turn.setOutput(fields[3].trim());

            turns.add(turn);
        }

        File jsonFolder = new File(file.getParentFile().getAbsolutePath() + File.separatorChar + "json" + File.separatorChar);
        if (!jsonFolder.exists()) {
            jsonFolder.mkdirs();
        }

        try {
            File jsonFile = new File(jsonFolder, file.getName());

            FileWriter writer = new FileWriter(jsonFile);
            Gson gson = new Gson();
            for (Turn turn : turns) {
                writer.append(gson.toJson(turn));
                writer.append("\n");
            }
            writer.close();
        } catch (Exception e) {
            DebugLogger.writeError(JsonConverter.class, "Error while writing jsonFile");
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
            for (File file : new File(test).listFiles()) {
                if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                    convert(file);
                }
            }
            for (File file : new File(log).listFiles()) {
                if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                    convert(file);
                }
            }
        } catch (Exception e) {
            DebugLogger.writeError(de.uni_kassel.vs.datageneration.DataChecker.class, "Error while converting traning/test data");
            System.exit(1);
        }
    }

    public static void convert(File[] listFiles) {
        try {
            for (File file : listFiles) {
                if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                    convert(file);
                }
            }
        } catch (Exception e) {
            DebugLogger.writeError(de.uni_kassel.vs.datageneration.DataChecker.class, "Error while converting traning/test data");
            System.exit(1);
        }
    }
}
