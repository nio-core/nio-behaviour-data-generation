package de.uni_kassel.vs.datageneration.classification.distances;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class NormalLevenshtein extends Distance {

    private final LevenshteinDistance distance;

    public NormalLevenshtein() {
        this.distance = new LevenshteinDistance();
    }

    public NormalLevenshtein(String defaultString) {
        super(defaultString);
        this.distance = new LevenshteinDistance();
    }

    @Override
    public double getScore(String turn) {
        return distance.apply(turn, compareString);
    }
}
