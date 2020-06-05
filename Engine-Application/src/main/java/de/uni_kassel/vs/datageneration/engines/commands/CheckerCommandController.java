package de.uni_kassel.vs.datageneration.engines.commands;

import de.uni_kassel.vs.datageneration.engines.GameBoard;
import de.uni_kassel.vs.datageneration.engines.EngineController;
import de.uni_kassel.vs.datageneration.exceptions.MalformedResponseException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CheckerCommandController extends CommandController {

    private class ConvertTable  {
        String[] table = new String[] {
                "b8", //1
                "d8", //2
                "f8", //3
                "h8", //4
                "a7", //5
                "c7", //6
                "e7", //7
                "g7", //8
                "b6", //9
                "d6", //10
                "f6", //11
                "h6", //12
                "a5", //13
                "c5", //14
                "e5", //15
                "g5", //16
                "b4", //17
                "d4", //18
                "f4", //19
                "h4", //20
                "a3", //21
                "c3", //22
                "e3", //23
                "g3", //24
                "b2", //25
                "d2", //26
                "f2", //27
                "h2", //28
                "a1", //29
                "c1", //30
                "e1", //31
                "g1", //32

        };

        public int fromNamed(String named) {
            for (int i = 0; i < table.length; i++) {
                if (named.equals(table[i])) {
                    return i+1;
                }
            }
            return -1;
        }

        public String fromNumeric(int numeric) {
            return table[numeric - 1];
        }
    }

    public CheckerCommandController(GameBoard gameBoard, EngineController.EngineType type) {
        super(gameBoard, type);
    }

    @Override
    public String commandAsString(Command command) {
        switch (command) {
            case start: {
                return "ping 0";
            }
            case isready: {
                return "ping 0";
            }
            case newgame: {
                return "new";
            }
            case position: {
                return "setboard " + convert();
            }
            case search: {
                return "search infinite";
            }
            case stop: {
                return "stop";
            }
            case quit: {
                return "quit";
            }
        }
        return "";
    }

    @Override
    public boolean isResponseOK(Command command, String response) {
        switch (command) {
            case start: {
                return response.contains("pong 0");
            }
            case isready: {
                return response.contains("pong 0");
            }
            case newgame: {
                return true;
            }
            case position: {
                return true;
            }
            case search: {
                return true;
            }
            case quit: {
                return response.isEmpty();
            }
            case stop:{
                return true;
            }

        }
        return false;
    }

    @Override
    public Response getResponse(String response) throws MalformedResponseException {
        if (response.contains("pong")) {
            return null;
        }
        if (response.isEmpty()) {
            return null;
        }
        if (response.contains("win")) {
            return new Response(true);
        }
        ConvertTable t = new ConvertTable();
        String[] split = response.split("\n");
        String from;
        String to;
        if (split[split.length - 4].contains("-")) {
            split = split[split.length-4].split("-");
            from = t.fromNumeric(Integer.valueOf(split[0]));
            to = t.fromNumeric(Integer.valueOf(split[1]));
        } else {
            split = split[split.length-4 ].split("x");
            from = t.fromNumeric(Integer.valueOf(split[0]));
            to = t.fromNumeric(Integer.valueOf(split[1]));

            char[][] realBoard = gameBoard.getBoardArray();
            int hitChar = Math.min(from.charAt(0) - 'a', to.charAt(0) - 'a') + 1;
            int hitNum = Math.min(Character.getNumericValue(from.charAt(1)), Character.getNumericValue(to.charAt(1)));

            realBoard[hitChar][hitNum] = '\u0000';
        }
        return new Response(from, to);
    }

    private String convert() {
        char[][] realBoard = gameBoard.getBoardArray();
        List<Integer> whites = new LinkedList<Integer>();
        List<Integer> blacks =   new LinkedList<Integer>();
        ConvertTable t = new ConvertTable();

        for(int i = 0; i < 8; i++) {
            char charRow = (char) (i + 'a');
            for(int j = 0; j < 8; j++) {
                int numRow = j + 1;
                if (realBoard[i][j] =='B') {
                    blacks.add(t.fromNamed(charRow + "" + numRow));
                } else if (realBoard[i][j] =='W') {
                    whites.add(t.fromNamed(charRow + "" + numRow));
                }
            }
        }

        Collections.sort(whites);
        Collections.sort(blacks);

        StringBuilder b = new StringBuilder();

        b.append(gameBoard.getNextMove());
        b.append(":W");
        String whiteString = whites.toString().substring(1, whites.toString().length() - 1);
        b.append(whiteString.replace(" ", ""));
        b.append(":B");
        String blackString = blacks.toString().substring(1, blacks.toString().length() - 1);
        b.append(blackString.replace(" ", ""));

        return b.toString();
    }
}
