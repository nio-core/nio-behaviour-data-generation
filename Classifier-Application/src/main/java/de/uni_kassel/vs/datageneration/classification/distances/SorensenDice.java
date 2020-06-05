package de.uni_kassel.vs.datageneration.classification.distances;

import java.util.HashSet;
import java.util.Set;

public class SorensenDice extends Distance {

    private Set<String> turn_bigrams;
    private Set<String> default_bigrams;

    public SorensenDice() {
        turn_bigrams = new HashSet<>();
        default_bigrams = new HashSet<>();
        _getBigrams(compareString, default_bigrams);
    }

    public SorensenDice(String defaultString) {
        super(defaultString);
        turn_bigrams = new HashSet<>();
        default_bigrams = new HashSet<>();
        _getBigrams(compareString, default_bigrams);
    }

    @Override
    public double getScore(String turn) {
        _getBigrams(turn, turn_bigrams);

        Set<String> intersection = new HashSet<>(turn_bigrams);
        intersection.retainAll(default_bigrams);
        double totalCombigrams = intersection.size();

        double score = (2 * totalCombigrams) / (turn_bigrams.size() + default_bigrams.size());
        turn_bigrams.clear();
        return score;
    }

    private void _getBigrams(String compareString, Set<String> default_bigrams) {
        for (int j = 0; j < compareString.length() - 1; j++) {
            char y1 = compareString.charAt(j);
            char y2 = compareString.charAt(j + 1);
            String tmp = "" + y1 + y2;
            default_bigrams.add(tmp);
        }
    }
}
