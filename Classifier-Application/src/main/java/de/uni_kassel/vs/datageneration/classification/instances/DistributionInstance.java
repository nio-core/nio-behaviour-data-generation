package de.uni_kassel.vs.datageneration.classification.instances;

public class DistributionInstance extends Instance {

    public DistributionInstance(String id) {
        super(id);
    }

    public double getInputScore(String input) {
        Integer integer = inputs.get(input);
        return (integer == null) ? ((double) PUNISHMENT_NON / (double) inputsCounter) : ((double) integer / (double) inputsCounter);
    }

    public double getOutputScore(String output) {
        Integer integer = outputs.get(output);
        return (integer == null) ? ((double) PUNISHMENT_NON / (double) outputsCounter) : ((double) integer / (double) outputsCounter);
    }
}
