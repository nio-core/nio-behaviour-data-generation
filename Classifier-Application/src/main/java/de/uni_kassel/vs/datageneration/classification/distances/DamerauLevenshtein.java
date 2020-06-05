package de.uni_kassel.vs.datageneration.classification.distances;

import info.debatty.java.stringsimilarity.Damerau;

public class DamerauLevenshtein extends Distance {

    private final Damerau distance;

    public DamerauLevenshtein() {
        this.distance = new Damerau();
    }

    public DamerauLevenshtein(String defaultString) {
        super(defaultString);
        this.distance = new Damerau();
    }

    @Override
    public double getScore(String turn) {
        return distance.distance(turn, compareString);
    }
}
