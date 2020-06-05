package de.uni_kassel.vs.datageneration.classification.instances;

public class SVMInstance extends Instance {
    public SVMInstance(String id) {
        super(id);
    }

    @Override
    public double getInputScore(String input) {
        return inputs.get(input);
    }

    @Override
    public double getOutputScore(String output) {
        return outputs.get(output);
    }
}
