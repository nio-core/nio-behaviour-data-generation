package de.uni_kassel.vs.datageneration.exceptions;

import de.uni_kassel.vs.datageneration.engines.EngineController;

public class ProcessNotReadyException extends Exception {
    private final EngineController engineController;

    public ProcessNotReadyException(EngineController engineController) {
        this.engineController = engineController;
    }

    @Override
    public String getMessage() {
        return "Process not ready for " + engineController.getType().name();
    }
}
