package de.uni_kassel.vs.datageneration.engines;

import de.uni_kassel.vs.datageneration.GameType;
import de.uni_kassel.vs.datageneration.engines.commands.Response;
import de.uni_kassel.vs.datageneration.exceptions.DoubledResponseException;
import de.uni_kassel.vs.datageneration.exceptions.EmptyFieldException;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;

import java.util.LinkedList;
import java.util.List;

public class GameBoard {

    private final char[][] board;
    private final GameType type;

    private final String startPos;

    private final LinkedList<Response> moves;

    private final CheckMateListener listener;
    private char nextMove = 'W';

    public GameBoard(String fenInit, GameType gameType, CheckMateListener listener) {
        this.type = gameType;
        this.startPos = fenInit;
        this.moves = new LinkedList<>();
        this.listener = listener;
        switch (type) {
            case Chess: {
                this.board = new char[8][8];
                if (fenInit.equals("startpos")) {
                    initPos('R', 'a', 1);
                    initPos('R', 'h', 1);

                    initPos('r', 'a', 8);
                    initPos('r', 'h', 8);

                    initPos('N', 'b', 1);
                    initPos('N', 'g', 1);

                    initPos('n', 'b', 8);
                    initPos('n', 'g', 8);

                    initPos('B', 'c', 1);
                    initPos('B', 'f', 1);

                    initPos('b', 'c', 8);
                    initPos('b', 'f', 8);

                    initPos('Q', 'd', 1);
                    initPos('K', 'e', 1);

                    initPos('q', 'd', 8);
                    initPos('k', 'e', 8);
                    for (int i = 0; i < 8; i++) {
                        initPos('P', (char) ('a' + i), 2);
                        initPos('p', (char) ('a' + i), 7);
                    }
                } else {
                    // TODO
                }
            } break;
            case Checkers: {
                this.board = new char[8][8];
                if (fenInit.equals("startpos")) {
                    initPos('W', 'a', 1);
                    initPos('W', 'c', 1);
                    initPos('W', 'e', 1);
                    initPos('W', 'g', 1);

                    initPos('W', 'b', 2);
                    initPos('W', 'd', 2);
                    initPos('W', 'f', 2);
                    initPos('W', 'h', 2);

                    initPos('W', 'a', 3);
                    initPos('W', 'c', 3);
                    initPos('W', 'e', 3);
                    initPos('W', 'g', 3);


                    initPos('B', 'b', 6);
                    initPos('B', 'd', 6);
                    initPos('B', 'f', 6);
                    initPos('B', 'h', 6);

                    initPos('B', 'a', 7);
                    initPos('B', 'c', 7);
                    initPos('B', 'e', 7);
                    initPos('B', 'g', 7);

                    initPos('B', 'b', 8);
                    initPos('B', 'd', 8);
                    initPos('B', 'f', 8);
                    initPos('B', 'h', 8);
                } else {
                    // TODO
                }
            } break;
            case ChineseChess: {
                this.board = new char[9][10];
                if (fenInit.equals("startpos")) {
                    initPos('R', 'a', 1);
                    initPos('R', 'i', 1);

                    initPos('r', 'a', 10);
                    initPos('r', 'i', 10);

                    initPos('N', 'b', 1);
                    initPos('N', 'h', 1);

                    initPos('n', 'b', 10);
                    initPos('n', 'h', 10);

                    initPos('B', 'c', 1);
                    initPos('B', 'g', 1);

                    initPos('b', 'c', 10);
                    initPos('b', 'g', 10);

                    initPos('A', 'd', 1);
                    initPos('A', 'f', 1);

                    initPos('a', 'd', 10);
                    initPos('a', 'f', 10);

                    initPos('K', 'e', 1);

                    initPos('k', 'e', 10);

                    initPos('C', 'b', 3);
                    initPos('C', 'h', 3);

                    initPos('c', 'b', 8);
                    initPos('c', 'h', 8);

                    for (int i = 0; i < 5; i++) {
                        initPos('P', (char) ((i * 2) + 'a'), 4);
                        initPos('p', (char) ((i * 2) + 'a'), 7);
                    }
                } else {
                    // TODO
                }
            } break;
            default: {
                board = new char[8][8];
            }
        }

    }

    private void initPos(char type, char charRow, int numberRow) {
        int i = charRow - 'a';
        int j = numberRow - 1;
        this.board[i][j] = type;
    }

    public void addMove(Response bestmove, char nextMove) throws DoubledResponseException, EmptyFieldException {
        if (moves.size() > 2) {
            if (bestmove.equals(moves.getLast()) || bestmove.equals(moves.get(moves.size() - 2))) {
                throw new DoubledResponseException(bestmove);
            }
        }

        this.nextMove = nextMove;
        this.moves.add(bestmove);

        if (bestmove.isGiveUp()) {
            this.listener.alertCheckMate(true);

            return;
        }

        char fromChar = bestmove.getFrom().charAt(0);
        int fromNumber = Character.getNumericValue(bestmove.getFrom().charAt(1));

        char toChar = bestmove.getTo().charAt(0);
        int toNumber = Character.getNumericValue(bestmove.getTo().charAt(1));

        if (type == GameType.ChineseChess) {
            fromNumber++;
            toNumber++;
        }

        if ((toChar - 'a') > ((board[0].length * board.length) - 1) || (toChar - 'a') < (0)
                || (toNumber - 1) > ((board[0].length * board.length) - 1) || (toNumber - 1) < (0)
                || (fromChar - 'a') > ((board[0].length * board.length) - 1) || (fromChar - 'a') < (0)
                || (fromNumber - 1) > ((board[0].length * board.length) - 1) || (fromNumber - 1) < (0)) {
            DebugLogger.writeError(this.getClass(), "Move not possible:\n" + toString() + "\n" + bestmove.toString());
        }

        char tempTo = board[(toChar - 'a')][(toNumber - 1)];
        char tempFrom = board[(fromChar - 'a')][(fromNumber - 1)];

        if (tempFrom == '\u0000') {
            throw new EmptyFieldException(bestmove, tempFrom, tempTo);
        }

        board[(toChar - 'a')][(toNumber - 1)] = tempFrom;
        board[(fromChar - 'a')][(fromNumber - 1)] = '\u0000';

        if (tempTo == 'k' || tempTo == 'K') {
            listener.alertCheckMate(false);
        }
    }

    public String getStartPos() {
        return startPos;
    }

    public String getMovesAsString() {
        StringBuilder b = new StringBuilder();
        for (Response r : moves) {
            b.append(r.toString());
            if (!r.equals(moves.getLast())) {
                b.append(" ");
            }
        }
        return b.toString();
    }

    public String getLastMoveAsString() {
        return moves.getLast().toString();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < board.length; i++) {
            b.append("\t\t");
            char character = (char) (i + 'a');
            b.append(character);
        }
        b.append("\n\t-------------------------------------------------------------------------\n");
        for (int i = 0; i < board[0].length; i++) {
            b.append(i + ((type == GameType.ChineseChess) ? 0 : 1));
            b.append("\t|");
            for (int j = 0; j < board.length; j++) {
                b.append("\t");
                b.append(board[j][i]);
                b.append("\t|");
            }
            b.append("\n\t-------------------------------------------------------------------------\n");
        }
        return b.toString();
    }

    public char[][] getBoardArray() {
        return board;
    }

    public char getNextMove() {
        return nextMove;
    }

    public boolean checkForCircle() {
        if (moves.size() < 6) {
            return false;
        }

        Response lastB = moves.get(moves.size() - 1);
        Response lastW = moves.get(moves.size() - 2);

        boolean foundCircleBlack = false;
        boolean foundCircleWhite = false;
        List<Response> circleCheck = moves.subList(moves.size() - 6, moves.size());
        for (Response rep: circleCheck) {
            if (lastB.isRevert(rep)) {
                foundCircleBlack = true;
            }
            if (lastW.isRevert(rep)) {
                foundCircleWhite = true;
            }
        }
        if ((foundCircleBlack && foundCircleWhite)){
            return true;
        }
        else {
            return false;
        }

    }

    public interface CheckMateListener {
        void alertCheckMate(boolean giveUp);
    }
}
