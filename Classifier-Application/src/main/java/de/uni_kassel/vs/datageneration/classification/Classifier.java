package de.uni_kassel.vs.datageneration.classification;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import org.reflections.Reflections;

public abstract class Classifier {
    protected int learnCounter = 0;

    protected int testGameCounter = 0;
    protected int gamePositive = 0;
    protected int gameNegative = 0;

    protected int testEngineCounter = 0;
    protected int enginePositive = 0;
    protected int engineNegative = 0;


    public abstract void learn(File folder) throws IOException;
    public abstract List<Suggestion> checkByGame(File folder) throws IOException;
    public abstract List<Suggestion> checkByEngine(File folder) throws IOException;

    protected Integer convertMoveToInt(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }
        if (input.equals("nomove")) {
            return -1;
        }
        char[] chars = input.toCharArray();
        chars[0] = (char) (chars[0] - 49);
        chars[2] = (char) (chars[2] - 49);
        return Integer.parseInt(new String(chars));
    }

    public void printStatistic(Suggestion.Type type) {
        StringBuilder b = new StringBuilder("Files for Learning: [" +  String.format("%04d",learnCounter) + "]");
        b.append(" [");
        b.append(type.name());
        b.append("] ");
        b.append(" Classified Instances [");
        switch (type) {
            case Game: {
                b.append(String.format("%04d", testGameCounter));
                b.append("] ");
                b.append("correct classified [");
                b.append(String.format("%04d", gamePositive));
                b.append("] wrong classified [");
                b.append(String.format("%04d", gameNegative));
                b.append("] Classifier accuracy [");
                double acc = ((double) gamePositive / ((double) testGameCounter * 2)) * 100;
                b.append(String.format("%02.02f", acc));
            } break;
            case Engine: {
                b.append(String.format("%04d", testEngineCounter));
                b.append("] ");
                b.append("correct classified [");
                b.append(String.format("%04d", enginePositive));
                b.append("] wrong classified [");
                b.append(String.format("%04d", engineNegative));
                b.append("] Classifier accuracy [");
                double acc = ((double) enginePositive / ((double) testEngineCounter * 2)) * 100;
                b.append(String.format("%02.02f", acc));
            } break;
        }
        b.append("%]");
        DebugLogger.writeResult(this.getClass(), b.toString());
    }

    public static Set<Class<? extends Classifier>> getClasses() {
        String prefix = Classifier.class.getPackage().getName();
        Reflections reflections = new Reflections(prefix);
        return reflections.getSubTypesOf(Classifier.class);
    }
}
