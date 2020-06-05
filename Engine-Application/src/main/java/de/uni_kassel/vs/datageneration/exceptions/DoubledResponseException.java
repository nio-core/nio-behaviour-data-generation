package de.uni_kassel.vs.datageneration.exceptions;

import de.uni_kassel.vs.datageneration.engines.commands.Response;

public class DoubledResponseException extends Exception {
    private final Response response;

    public DoubledResponseException(Response response) {
        this.response = response;
    }

    @Override
    public String getMessage() {
        return "Last response was doubled " + response.toString();
    }
}
