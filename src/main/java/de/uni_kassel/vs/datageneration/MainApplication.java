package de.uni_kassel.vs.datageneration;

import de.uni_kassel.vs.datageneration.classification.Classifier;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import org.apache.commons.cli.*;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Semaphore;

public class MainApplication {

    private static Semaphore semaphore;

    public static void main(String[] args) {

        // Processing Commandline Arguments
        Options options = new Options();

        Option help = new Option("?", "help", false, "prints this message");
        options.addOption(help);

        Option parallelArg = new Option("p", "parallel", true, "games to play at a time");
        parallelArg.setRequired(true);
        parallelArg.setType(Integer.class);
        options.addOption(parallelArg);

        Option countArg = new Option("c", "count", true, "games to play");
        countArg.setRequired(true);
        countArg.setType(Integer.class);
        options.addOption(countArg);

        Option debugArg = new Option("d", "debug", false, "(optional) enables debug output");
        options.addOption(debugArg);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        // parsing cmd arguments
        int parallel = 0;
        int count = 0;
        try {
            cmd = parser.parse(options, args);
            count = Integer.valueOf(cmd.getOptionValue(countArg.getOpt()));
            parallel = Integer.valueOf(cmd.getOptionValue(parallelArg.getOpt()));
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

        semaphore = new Semaphore(parallel);

        // Generate Data
        File logFolder = new File("_log/");
        File logTestFolder = new File("_log/test");


        try {
            DataChecker.check(logFolder.listFiles());
            DataChecker.check(logTestFolder.listFiles());
            JsonConverter.convert(logFolder.listFiles());
            JsonConverter.convert(logTestFolder.listFiles());
        } catch (IOException e) {
            DebugLogger.writeError(MainApplication.class, "Error in DataChecker", e);
        }

        for (int i = 0; i < count; i++) {
            Random ran = new Random();

            int ranValue = Math.abs(ran.nextInt()) % GameType.values().length;
            GameType gameType = GameType.values()[ranValue];

            ranValue = Math.abs(ran.nextInt()) % gameType.getEngineTypes().getEnumConstants().length;
            String blackEngine = gameType.getEngineTypes().getEnumConstants()[ranValue].name();

            ranValue = Math.abs(ran.nextInt()) % gameType.getEngineTypes().getEnumConstants().length;
            String whiteEngine = gameType.getEngineTypes().getEnumConstants()[ranValue].name();

            List<String> commandList = new LinkedList<>();
            commandList.add("java");
            commandList.add("-jar");
            commandList.add("Engine-Application/Engine-Application.jar");
            commandList.add("-d");
            commandList.add("-l");
            if ((i % 10) == 0) {
                commandList.add(logTestFolder.getAbsolutePath());
            } else {
                commandList.add(logFolder.getAbsolutePath());
            }
            commandList.add("-g");
            commandList.add(gameType.name());
            commandList.add("-b");
            commandList.add(blackEngine);
            commandList.add("-w");
            commandList.add(whiteEngine);
            ProcessBuilder b = new ProcessBuilder(commandList);
            b.inheritIO();

            try {
                semaphore.acquire();
                Process process = b.start();
                int finalI = i;
                new ProcessExitDetector(process, () -> {
                    DebugLogger.writeMessage(MainApplication.class, "Game " + String.format("%04d", finalI) + ": terminated");
                    semaphore.release();
                }).start();

                DebugLogger.writeMessage(MainApplication.class, "Game " + String.format("%04d", i) + ": " + gameType.name() + "[" + whiteEngine + " vs. " + blackEngine + "]");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Set<Class<? extends Classifier>> classes = Classifier.getClasses();

        for (Class<? extends Classifier> clazz : classes) {
            List<String> commandList = new LinkedList<>();
            commandList.add("java");
            commandList.add("-Xms2048M");
            commandList.add("-Xmx8192M");
            commandList.add("-jar");
            commandList.add("Classifier-Application/Classifier-Application.jar");
            commandList.add("-d");
            commandList.add("-l");
            commandList.add(logFolder.getAbsolutePath());
            commandList.add("-t");
            commandList.add(logTestFolder.getAbsolutePath());
            commandList.add("-c");
            commandList.add(clazz.getSimpleName());
            ProcessBuilder b = new ProcessBuilder(commandList);
            b.inheritIO();

            try {
                semaphore.acquire(parallel);
                Process process = b.start();
                final int finalParallel = parallel;
                new ProcessExitDetector(process, () -> {
                    DebugLogger.writeMessage(MainApplication.class, "Classifier " + clazz + ": terminated");
                    semaphore.release(finalParallel);
                }).start();

                DebugLogger.writeMessage(MainApplication.class, "Classifier " + clazz + ": " + " started");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            semaphore.acquire(parallel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        DebugLogger.writeMessage(MainApplication.class, "Stopped");
    }
}
