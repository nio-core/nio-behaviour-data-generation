package de.uni_kassel.vs.datageneration.engines.commands;

public enum Command {
    start(true),
    isready(true),
    newgame(false),
    position(false),
    search(false),
    stop(true),
    quit(false);

    Command(boolean withResponse) {
        this.withResponse = withResponse;
    }

    public boolean withResponse;

    private String boardPosition;
    private String moves;

    public Command setBoardPosition(String boardPosition) {
        this.boardPosition = boardPosition;
        return this;
    }

    public Command setMoves(String moves) {
        this.moves = moves;
        return this;
    }

    public String getBoardPosition() {
        return boardPosition;
    }

    public String getMoves() {
        return moves;
    }
}
