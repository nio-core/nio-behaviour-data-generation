package de.uni_kassel.vs.datageneration.logger;

import de.uni_kassel.vs.datageneration.GameType;
import de.uni_kassel.vs.datageneration.engines.EngineController;
import de.uni_kassel.vs.datageneration.engines.commands.Response;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GameLogger {
    private static boolean log;
    private static File moveFile;
    private static File commandFile;

    public static void init(boolean log, String l, GameType gameType) {
        GameLogger.log = log;
        if (GameLogger.log) {
            File folder = new File(l);
            folder.mkdirs();
            Date date = new Date();
            GameLogger.moveFile = new File(folder.getAbsolutePath() + File.separator + "[" + new SimpleDateFormat("yyyy-MM-dd HH-mm-ss.SSS").format(date) + "]" + gameType.name() + ".csv");
            try (FileWriter fileWriter = new FileWriter(GameLogger.moveFile)) {
                fileWriter.write("EngineController, Player, Input, Output\n");
            } catch (IOException e) {
                DebugLogger.writeError(GameLogger.class, "Error creating File", e);
            }
            GameLogger.commandFile = new File(folder.getAbsolutePath() + File.separator + "[" + new SimpleDateFormat("yyyy-MM-dd HH-mm-ss.SSS").format(date) + "]" + gameType.name() + ".commands");
            try (FileWriter fileWriter = new FileWriter(GameLogger.commandFile)) {
                fileWriter.write("EngineController, Command, Response\n");
            } catch (IOException e) {
                DebugLogger.writeError(GameLogger.class, "Error creating File", e);
            }
        }
    }

    public static void writeMove(EngineController engineController, String player, String movesAsString, Response bestmove) {
        if (GameLogger.log) {
            try (FileWriter fileWriter = new FileWriter(GameLogger.moveFile, true)) {
                fileWriter.append(engineController.getType().name() + ", " + player + ", " + movesAsString + ", " + bestmove + "\n");
            } catch (IOException e) {
                DebugLogger.writeError(GameLogger.class, "Error writing File", e);
            }
        }
    }

    public static void writeCommand(EngineController.EngineType type, String command, String response) {
        if (GameLogger.log) {
            try (FileWriter fileWriter = new FileWriter(GameLogger.commandFile, true)) {
                fileWriter.append(type.name()  + ", " + command + ", " + response.replaceAll("\n", " \\\\n ") + "\n");
            } catch (IOException e) {
                DebugLogger.writeError(GameLogger.class, "Error writing File", e);
            }
        }
    }
}
