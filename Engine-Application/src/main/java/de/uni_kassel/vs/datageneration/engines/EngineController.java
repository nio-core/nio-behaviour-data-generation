package de.uni_kassel.vs.datageneration.engines;

import de.uni_kassel.vs.datageneration.GameType;
import de.uni_kassel.vs.datageneration.exceptions.MalformedResponseException;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import de.uni_kassel.vs.datageneration.logger.GameLogger;
import de.uni_kassel.vs.datageneration.engines.commands.Command;
import de.uni_kassel.vs.datageneration.engines.commands.CommandController;
import de.uni_kassel.vs.datageneration.engines.commands.Response;
import de.uni_kassel.vs.datageneration.exceptions.ProcessNotReadyException;
import de.uni_kassel.vs.datageneration.exceptions.UnsupportedResponseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.*;

public abstract class EngineController {
    private final EngineType type;
    private final CommandController commandController;

    private Process process;

    public interface EngineType {
        enum Language {
            java,
            c
        }
        String name();
        Language getLang();
    }

    EngineController(EngineType type, CommandController commandController) {
        this.type = type;
        this.commandController = commandController;
    }

    public EngineType getType() {
        return type;
    }

    public static EngineController getFromString(String type, GameType gameType, GameBoard gameBoard) {
        type = type.toLowerCase();
        LevenshteinDistance lD = new LevenshteinDistance();

        switch (gameType) {
            case Chess: {
                for (ChessEngineController.Type e : ChessEngineController.Type.values()) {
                    if (lD.apply(type, e.name().toLowerCase()) < 2) {
                        return new ChessEngineController(e, gameBoard);
                    }
                }
            } break;
            case Checkers: {
                for (CheckerEngineController.Type e : CheckerEngineController.Type.values()) {
                    if (lD.apply(type, e.name().toLowerCase()) < 2) {
                        return new CheckerEngineController(e, gameBoard);
                    }
                }
            } break;
            case ChineseChess: {
                for (ChineseChessEngineController.Type e : ChineseChessEngineController.Type.values()) {
                    if (lD.apply(type, e.name().toLowerCase()) < 2) {
                        return new ChineseChessEngineController(e, gameBoard);
                    }
                }
            } break;
        }

        return null;
    }

    public void startProcess() throws Exception {
        String[] commands = new String[0];
        switch (type.getLang()) {
            case java: {
                commands = new String[]{"java", "-jar", this.getFolderName() + File.separator + getType() + File.separator + getType() + ".jar"};
            } break;
            case c: {
                commands = new String[]{this.getFolderName() + File.separator + getType() + File.separator + getType()};
            }
                break;
        }
        if (ArrayUtils.isEmpty(commands)) {
            DebugLogger.writeError(this.getClass(), "Wrong type");
        } else {
            this.process = new ProcessBuilder(commands).start();
        }
    }

    public Response sendCommand(Command command) throws Exception {
        try {
            if (process != null) {
                try {
                    BufferedWriter engineWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                    engineWriter.write(commandController.commandAsString(command) + "\n");
                    engineWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    DebugLogger.writeError(this.getClass(), "Something went wrong while writing to engine " + commandController.getType(), e);
                    throw e;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    DebugLogger.writeError(this.getClass(), "Interrupted Thread.", e);
                    throw e;
                }
                StringBuilder responseString = new StringBuilder();
                try {
                    if (command.withResponse) {
                        BufferedReader engineReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        for (char c = (char) engineReader.read(); (int) c != -1 && engineReader.ready(); c = (char) engineReader.read()) {
                            responseString.append(c);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    DebugLogger.writeError(this.getClass(), "Something went wrong while reading from engine " + commandController.getType(), e);
                    throw e;
                }

                GameLogger.writeCommand(commandController.getType(), commandController.commandAsString(command), responseString.toString());

                try {
                    if (commandController.isResponseOK(command, responseString.toString())) {
                        return commandController.getResponse(responseString.toString());
                    } else {
                        Exception e = new UnsupportedResponseException(command, responseString.toString());
                        e.printStackTrace();
                        DebugLogger.writeError(this.getClass(), "Unsupported response.", e);
                        throw e;
                    }
                } catch (MalformedResponseException e) {
                    e.printStackTrace();
                    DebugLogger.writeError(this.getClass(), "Malformed response.", e);
                    throw e;
                }
            } else{
                Exception e = new ProcessNotReadyException(this);
                e.printStackTrace();
                DebugLogger.writeError(this.getClass(), "Process not Ready.", e);
                throw e;

            }
        } catch (ProcessNotReadyException e){
            e.printStackTrace();
            DebugLogger.writeError(this.getClass(), "Processes not started.");
            throw e;
        }
    }

    abstract String getFolderName() throws Exception;

    String getFolderName(String folder) throws Exception {
        File file = new File(EngineController.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            os = "macosx";
        } else if (os.contains("linux")) {
            os = "linux";
        } else {
            DebugLogger.writeError(this.getClass(), "OS not supported");
            throw new Exception();
        }
        String path = file.getParent() + File.separatorChar + os + File.separatorChar + folder;
        return path;
    }
}