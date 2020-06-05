package de.uni_kassel.vs.datageneration.engines;

import de.uni_kassel.vs.datageneration.engines.commands.CheckerCommandController;

public class CheckerEngineController extends EngineController {

    public enum Type implements EngineType {
        ponder(Language.c);

        private final Language lang;

        Type(Language lang) {
            this.lang = lang;
        }

        @Override
        public Language getLang() {
            return lang;
        }
    }

    CheckerEngineController(EngineType type, GameBoard gameBoard) {
        super(type, new CheckerCommandController(gameBoard, type));
    }

    @Override
    String getFolderName() throws Exception {
        return getFolderName("_checkerEngines");
    }
}
