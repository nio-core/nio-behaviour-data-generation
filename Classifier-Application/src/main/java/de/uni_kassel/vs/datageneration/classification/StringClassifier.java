package de.uni_kassel.vs.datageneration.classification;

import de.uni_kassel.vs.datageneration.classification.distances.Distance;
import de.uni_kassel.vs.datageneration.classification.instances.Turn;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import libsvm.svm_parameter;

import java.util.List;

public abstract class StringClassifier extends Classifier {
    private Distance distance;
    protected final svm_parameter param;

    protected StringClassifier(Distance distance, svm_parameter parameter) {
        this.distance = distance;
        param = parameter;
    }

    public Distance getDistance() {
        return distance;
    }

    @Override
    public void printStatistic(Suggestion.Type type) {
        StringBuilder b = new StringBuilder("Files for Learning: [" +  String.format("%04d",learnCounter) + "]");
        b.append(" [");
        b.append(type.name());
        b.append(":");
        b.append(distance.getClass().getSimpleName());
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

    public abstract void learn(List<Turn> turns);
    public abstract String checkByEngine(Turn turn);
    public abstract String checkByGame(Turn turn);
}
