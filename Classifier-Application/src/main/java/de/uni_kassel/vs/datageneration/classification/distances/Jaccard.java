package de.uni_kassel.vs.datageneration.classification.distances;

import org.apache.commons.text.similarity.JaccardDistance;

public class Jaccard extends Distance {
    private final JaccardDistance distance;

    public Jaccard() {
        this.distance = new JaccardDistance();
    }

    public Jaccard(String defaultString) {
        super(defaultString);
        this.distance = new JaccardDistance();
    }

    @Override
    public double getScore(String turn) {
        return distance.apply(turn, compareString);
    }
}
