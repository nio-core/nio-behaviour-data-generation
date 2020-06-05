package de.uni_kassel.vs.datageneration.engines.commands;

import de.uni_kassel.vs.datageneration.engines.GameBoard;
import de.uni_kassel.vs.datageneration.engines.EngineController;
import de.uni_kassel.vs.datageneration.exceptions.MalformedResponseException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChineseChessCommandController extends CommandController {

    public ChineseChessCommandController(GameBoard gameBoard, EngineController.EngineType type) {
        super(gameBoard, type);
    }

    @Override
    public String commandAsString(Command command) {
        switch (command) {
            case start: {
                return "ucci";
            }
            case isready: {
                return "isready";
            }
            case newgame: {
                return "uccinewgame";
            }
            case position: {
                return "position " + command.getBoardPosition() + " moves " + command.getMoves();
            }
            case search: {
                return "search infinite";
            }
            case quit: {
                return "quit";
            }
            case stop: {
                return "stop";
            }
        }
        return "";
    }

    @Override
    public boolean isResponseOK(Command command, String response) {
        switch (command) {
            case start: {
                return response.contains("ucciok");
            }
            case isready: {
                return response.contains("readyok");
            }
            case newgame: {
                return response.isEmpty();
            }
            case position: {
                return response.isEmpty();
            }
            case search: {
                return true;
            }
            case quit: {
                return response.isEmpty();
            }
            case stop:{
                return response.contains("bestmove");
            }

        }
        return false;
    }

    @Override
    public Response getResponse(String response) throws MalformedResponseException {
        if (response.contains("bestmove")) {
            if (response.contains("nomove") || response.contains("none")) {
                return  new Response(true);
            } else {
                Pattern pattern;
                if (response.contains("ponder")){
                    pattern = Pattern.compile("bestmove.*[a-i]{1}[0-9]{1}[a-i]{1}[0-9]{1}.");
                }
                else {
                    pattern = Pattern.compile("bestmove.*[a-i]{1}[0-9]{1}[a-i]{1}[0-9]{1}");
                }
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    response = matcher.group(0).replace("bestmove", "").trim();
                    response = response.replaceAll("\\W", "");
                } else {
                    return new Response(true);
                }
                return new Response(response.substring(0,2), response.substring(2, 4));
            }
        }
        return null;
    }
}
