package de.uni_kassel.vs.datageneration.exceptions;

import de.uni_kassel.vs.datageneration.engines.commands.Response;

public class MalformedResponseException extends Exception {
    private final Response response;

    public MalformedResponseException(Response response) {
        this.response = response;
    }

    @Override
    public String getMessage() {
        return "Response is malformed " + response.toString();
    }
}
