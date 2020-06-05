package de.uni_kassel.vs.datageneration.classification.distances;

public class WeightedLevenshtein extends Distance {

    info.debatty.java.stringsimilarity.WeightedLevenshtein distance;

    public WeightedLevenshtein() {
        this.distance = _getDistance();
    }

    public WeightedLevenshtein(String defaultString) {
        super(defaultString);
        this.distance = _getDistance();
    }

    private info.debatty.java.stringsimilarity.WeightedLevenshtein _getDistance() {
        return new info.debatty.java.stringsimilarity.WeightedLevenshtein((c1, c2) -> {
            int num1 = Character.getNumericValue(c1);
            int num2 = Character.getNumericValue(c2);
            return (num1 > num2) ? (num1 - num2) : (num2 - num1);
        });
    }

    @Override
    public double getScore(String turn) {
        return distance.distance(turn, compareString);
    }
}
