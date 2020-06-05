package de.uni_kassel.vs.datageneration.classification.distances;

import org.reflections.Reflections;

import java.util.Random;
import java.util.Set;

public abstract class Distance {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz1234567890";

    protected String compareString = "qaa804mx44";

    protected Distance() { }

    protected Distance(String compareString) {
        this.compareString = compareString;
    }

    /** Methode zum Generierung eines Vergleichsstrings der Läng 4-20 aus ALPHABET */
    public static String getRandomString() {
        StringBuilder result = new StringBuilder();
        Random rand = new Random();
        int resultLength = rand.nextInt(17) + 4;
        while (result.length() < resultLength) {
            int index = (int) (rand.nextFloat() * ALPHABET.length());
            result.append(ALPHABET.charAt(index));
        }
        return result.toString();
    }

    public abstract double getScore(String turn);

    public static Set<Class<? extends Distance>> getClasses() {
        Reflections reflections = new Reflections("de.uni_kassel.vs.datageneration.classification.distances");
        return reflections.getSubTypesOf(Distance.class);
    }
}
