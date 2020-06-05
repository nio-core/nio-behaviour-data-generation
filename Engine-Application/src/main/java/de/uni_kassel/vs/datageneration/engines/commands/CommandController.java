package de.uni_kassel.vs.datageneration.engines.commands;

import de.uni_kassel.vs.datageneration.engines.GameBoard;
import de.uni_kassel.vs.datageneration.engines.EngineController;
import de.uni_kassel.vs.datageneration.exceptions.MalformedResponseException;

public abstract class CommandController {

    protected final GameBoard gameBoard;
    protected final EngineController.EngineType type;

    public CommandController(GameBoard gameBoard, EngineController.EngineType type) {
        this.gameBoard = gameBoard;
        this.type = type;
    }
    public abstract String commandAsString(Command command);
    public abstract boolean isResponseOK(Command command, String toString);
    public abstract Response getResponse(String response) throws MalformedResponseException;

    public EngineController.EngineType getType() {
        return type;
    }

    public GameBoard getGameBoard() {
        return gameBoard;
    }
}
