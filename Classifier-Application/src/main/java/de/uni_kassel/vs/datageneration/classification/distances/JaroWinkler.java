package de.uni_kassel.vs.datageneration.classification.distances;

import org.apache.commons.text.similarity.JaroWinklerDistance;

public class JaroWinkler extends Distance {

    private final JaroWinklerDistance distance;

    public JaroWinkler() {
        this.distance = new JaroWinklerDistance();
    }

    public JaroWinkler(String defaultString) {
        super(defaultString);
        this.distance = new JaroWinklerDistance();
    }

    @Override
    public double getScore(String turn) {
        return distance.apply(turn, compareString);
    }
}
