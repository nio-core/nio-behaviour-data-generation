package de.uni_kassel.vs.datageneration.engines;

import de.uni_kassel.vs.datageneration.engines.commands.ChessCommandController;

public class ChessEngineController extends EngineController {

    public enum Type implements EngineType {
        pulse(Language.java),
        stockfish(Language.c),
        laser(Language.c),
        fruitReloaded(Language.c);
        //senpai(Language.c);

        private final Language lang;

        Type(Language lang) {
            this.lang = lang;
        }

        @Override
        public Language getLang() {
            return lang;
        }
    }

    public ChessEngineController(EngineType type, GameBoard gameBoard) {
        super(type, new ChessCommandController(gameBoard, type));
    }

    @Override
    public String getFolderName() throws Exception {
        return getFolderName("_chessEngines");
    }


}
