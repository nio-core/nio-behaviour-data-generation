package de.uni_kassel.vs.datageneration.engines;

import de.uni_kassel.vs.datageneration.engines.commands.ChineseChessCommandController;

public class ChineseChessEngineController extends EngineController {

        public enum Type implements EngineType {
            eleeye(Language.c),
            mars(Language.c);

            private final Language lang;

            Type(Language lang) {
                this.lang = lang;
            }

            @Override
            public Language getLang() {
                return lang;
            }
        }

    ChineseChessEngineController(EngineType type, GameBoard gameBoard) {
            super(type, new ChineseChessCommandController(gameBoard, type));
        }

        @Override
        String getFolderName() throws Exception {
            return getFolderName("_chineseChessEngines");
        }
}
