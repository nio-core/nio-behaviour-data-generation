package de.uni_kassel.vs.datageneration.engines;

import de.uni_kassel.vs.datageneration.GameType;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import de.uni_kassel.vs.datageneration.logger.GameLogger;
import org.apache.commons.cli.*;

public class Application {

    public static void main(String[] args) {

        // Processing Commandline Arguments
        Options options = new Options();

        Option help = new Option("?", "help", false, "prints this message");
        options.addOption(help);

        StringBuilder description = new StringBuilder("select type of game:\n[");
        for (GameType game : GameType.values()) {
            description.append(game.name());
            description.append(", ");
        }
        description.deleteCharAt(description.lastIndexOf(","));
        description.deleteCharAt(description.lastIndexOf(" "));
        description.append("]");
        Option gameTypeArg = new Option("g", "game", true, description.toString());
        gameTypeArg.setRequired(true);
        gameTypeArg.setType(String.class);
        options.addOption(gameTypeArg);

        description = new StringBuilder();
        for (GameType game : GameType.values()) {
            description.append("\n");
            description.append(game.name());
            description.append(": [");
            EngineController.EngineType[] enumConstants = game.getEngineTypes().getEnumConstants();
            for(EngineController.EngineType type : enumConstants) {
                description.append(type.name());
                description.append(", ");
            }
            description.deleteCharAt(description.lastIndexOf(","));
            description.deleteCharAt(description.lastIndexOf(" "));
            description.append("]");
        }

        Option engineWArg = new Option("w", "engineWhite", true, "engine that plays as white:" + description.toString());
        engineWArg.setRequired(true);
        engineWArg.setType(String.class);
        options.addOption(engineWArg);

        Option engineBArg = new Option("b", "engineBlack", true, "engine that plays as black:" + description.toString());
        engineBArg.setRequired(true);
        engineBArg.setType(String.class);
        options.addOption(engineBArg);

        Option initBoard = new Option("i", "initBoard", true, "(optional) start gameBoard config as fen string, when not set then use normal start");
        initBoard.setType(String.class);
        options.addOption(initBoard);

        Option debugArg = new Option("d", "debug", false, "(optional) enables debug output");
        options.addOption(debugArg);

        Option logArg = new Option("l", "log", true, "(optional) enables game log output [filePath]");
        options.addOption(logArg);

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

        GameType gameType = GameType.getFromString(cmd.getOptionValue("g"));

        boolean debug = cmd.hasOption("d");
        DebugLogger.init(debug, formatter, options);
        boolean log = cmd.hasOption("l");
        GameLogger.init(log, cmd.getOptionValue("l"), gameType);

        if (gameType == null) {
            DebugLogger.writeError(GameController.class, "False config. Not a game type,");
            System.exit(1);
        }

        String fenInit;
        if (cmd.hasOption("i")) {
            fenInit = cmd.getOptionValue("i");
        } else {
            fenInit = "startpos";
        }

        GameController enCon = new GameController();
        enCon.start(cmd.getOptionValue("w"), cmd.getOptionValue("b"), gameType, fenInit);
    }
}
