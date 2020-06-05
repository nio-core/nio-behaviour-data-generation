package de.uni_kassel.vs.datageneration.engines;

import de.uni_kassel.vs.datageneration.GameType;
import de.uni_kassel.vs.datageneration.IController;
import de.uni_kassel.vs.datageneration.engines.commands.Command;
import de.uni_kassel.vs.datageneration.engines.commands.Response;
import de.uni_kassel.vs.datageneration.exceptions.DoubledResponseException;
import de.uni_kassel.vs.datageneration.exceptions.EmptyFieldException;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import de.uni_kassel.vs.datageneration.logger.GameLogger;

import java.util.Random;


public class GameController implements GameBoard.CheckMateListener, IController {

    public static final int LOWER_BOUND = 5000;
    public static final int UPPER_BOUND = 10000;
    private boolean end = false;
    private boolean giveUp;
    private static EngineController engineControllerBlack;
    private static EngineController engineControllerWhite;

    public void start(String whiteEngine, String blackEngine, GameType gameType, String fenInit) {

        GameBoard gameBoard = new GameBoard(fenInit, gameType, this);

        engineControllerWhite = EngineController.getFromString(whiteEngine, gameType, gameBoard);
        engineControllerBlack = EngineController.getFromString(blackEngine, gameType, gameBoard);

        if(engineControllerBlack == null || engineControllerWhite == null) {
            DebugLogger.writeError(this.getClass(), "False config. Not an engine.");
            quitWithError();
        }

        DebugLogger.writeMessage(this.getClass(), "Chosen game type: " + gameType.name() + " white engine: " + engineControllerWhite.getType().name() + " black engine: " + engineControllerBlack.getType().name());

        // start engine processes streams
        try {
            engineControllerWhite.startProcess();
            engineControllerBlack.startProcess();
        } catch (Exception e) {
            e.printStackTrace();
            DebugLogger.writeError(this.getClass(), "Can't create processes.");
            quitWithError();
        }

        try {
            // start engine
            engineControllerWhite.sendCommand(Command.start);
            engineControllerBlack.sendCommand(Command.start);

            // check status
            engineControllerWhite.sendCommand(Command.isready);
            engineControllerBlack.sendCommand(Command.isready);

            // init new game
            engineControllerWhite.sendCommand(Command.newgame);
            engineControllerBlack.sendCommand(Command.newgame);
        } catch (Exception e) {
            e.printStackTrace();
            DebugLogger.writeError(this.getClass(), "Error in startup");
            quitWithError();
        }

        // DebugLogger.writeMessage(this.getClass(), "Game starts with GameBoard:\n" + gameBoard.toString());

        Random random = new Random();
        int millis;

        // let them fight
        while (true) {
            try {
                // whites turn
                if (!end) {
                    millis = random.nextInt(UPPER_BOUND - LOWER_BOUND) + LOWER_BOUND;
                    engineControllerWhite.sendCommand(Command.position.setBoardPosition(gameBoard.getStartPos()).setMoves(gameBoard.getMovesAsString()));
                    engineControllerWhite.sendCommand(Command.search);
                    Thread.sleep(millis);
                    Response bestmove = engineControllerWhite.sendCommand(Command.stop);
                    GameLogger.writeMove(engineControllerWhite, "w", gameBoard.getMovesAsString(), bestmove);
                    try {
                        gameBoard.addMove(bestmove, 'B');
                    } catch (DoubledResponseException | EmptyFieldException e) {
                        DebugLogger.writeError(this.getClass(), "EngineController messed up " + engineControllerWhite.getType(), e);
                        quitWithError();
                        break;
                    }
                    engineControllerWhite.sendCommand(Command.position.setBoardPosition(gameBoard.getStartPos()).setMoves(gameBoard.getMovesAsString()));
                    DebugLogger.writeMessage(this.getClass(), engineControllerWhite.getType().name() + " (white) moves in ("+ millis +"ms): " + gameBoard.getLastMoveAsString());
                } else {
                    if (giveUp) {
                        DebugLogger.writeMessage(this.getClass(), engineControllerWhite.getType() + " (white) is the winner (black has found no move)");
                    } else {
                        DebugLogger.writeMessage(this.getClass(), engineControllerBlack.getType() + " (black) is the winner");
                    }
                    break;
                }

                // blacks turn
                if (!end) {
                    millis = random.nextInt(UPPER_BOUND - LOWER_BOUND) + LOWER_BOUND;
                    engineControllerBlack.sendCommand(Command.position.setBoardPosition(gameBoard.getStartPos()).setMoves(gameBoard.getMovesAsString()));
                    engineControllerBlack.sendCommand(Command.search);
                    Thread.sleep(millis);
                    Response bestmove = engineControllerBlack.sendCommand(Command.stop);
                    GameLogger.writeMove(engineControllerBlack, "b", gameBoard.getMovesAsString(), bestmove);
                    try {
                        gameBoard.addMove(bestmove, 'W');
                    } catch (DoubledResponseException | EmptyFieldException e) {
                        DebugLogger.writeError(this.getClass(), "EngineController messed up " + engineControllerBlack.getType(), e);
                        quitWithError();
                        break;
                    }
                    engineControllerBlack.sendCommand(Command.position.setBoardPosition(gameBoard.getStartPos()).setMoves(gameBoard.getMovesAsString()));
                    DebugLogger.writeMessage(this.getClass(), engineControllerBlack.getType().name() + " (black) moves in ("+ millis +"ms): " + gameBoard.getLastMoveAsString());
                } else {
                    if (giveUp) {
                        DebugLogger.writeMessage(this.getClass(), engineControllerBlack.getType() + " (black) is the winner (white has found no move)");
                    } else {
                        DebugLogger.writeMessage(this.getClass(), engineControllerWhite.getType() + " (white) is the winner");
                    }
                    break;
                }

                if(gameBoard.checkForCircle()) {
                    DebugLogger.writeMessage(this.getClass(), "it's a tie!");
                    break;
                }

            } catch (Exception e) {
                quitWithError();
                break;
            }
        }

        try {
            engineControllerWhite.sendCommand(Command.quit);
            engineControllerBlack.sendCommand(Command.quit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // DebugLogger.writeMessage(this.getClass(), "final GameBoard:\n " + gameBoard.toString());
    }

    @Override
    public void alertCheckMate(boolean giveUp) {
        this.end = true;
        this.giveUp = giveUp;
    }

    @Override
    public void quitWithError() {
        try {
            engineControllerWhite.sendCommand(Command.quit);
            engineControllerBlack.sendCommand(Command.quit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(1);
    }
}
