package de.uni_kassel.vs.datageneration;

import de.uni_kassel.vs.datageneration.engines.*;
import de.uni_kassel.vs.datageneration.engines.ChessEngineController;
import de.uni_kassel.vs.datageneration.engines.ChineseChessEngineController;

import org.apache.commons.text.similarity.LevenshteinDistance;

public enum GameType {
    Chess(ChessEngineController.Type.class),
    Checkers(CheckerEngineController.Type.class),
    ChineseChess(ChineseChessEngineController.Type.class);

    private final Class<? extends EngineController.EngineType> engine;

    GameType(Class<? extends EngineController.EngineType> engine) {
        this.engine = engine;
    }

    public Class<? extends EngineController.EngineType> getEngineTypes() {
        return engine;
    }

    public static GameType getFromString(String type) {
        type = type.toLowerCase();
        LevenshteinDistance lD = new LevenshteinDistance();
        for (GameType e : GameType.values()) {
            if (lD.apply(type, e.name().toLowerCase()) < 2) {
                return e;
            }
        }
        return null;
    }
}
