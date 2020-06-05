package de.uni_kassel.vs.datageneration.exceptions;

import de.uni_kassel.vs.datageneration.engines.commands.Command;

public class UnsupportedResponseException extends Exception {
    private final Command command;
    private final String response;

    public UnsupportedResponseException(Command command, String response) {
        this.command = command;
        this.response = response;
    }

    @Override
    public String getMessage() {
        return "Response did not match command: ("+ command.toString() + "|" + response +")";
    }
}
