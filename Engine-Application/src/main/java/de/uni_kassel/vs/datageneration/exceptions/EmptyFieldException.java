package de.uni_kassel.vs.datageneration.exceptions;

import de.uni_kassel.vs.datageneration.engines.commands.Response;

public class EmptyFieldException extends Exception {
    private final Response response;
    private final char from;
    private final char to;

    public EmptyFieldException(Response response, char fromStone, char toStone) {
        this.response = response;
        this.from = fromStone;
        this.to = toStone;
    }

    @Override
    public String getMessage() {
        return "Last move was a move from an empty field " + response.toString() + " (" + from + "->" + to + ")";
    }
}
