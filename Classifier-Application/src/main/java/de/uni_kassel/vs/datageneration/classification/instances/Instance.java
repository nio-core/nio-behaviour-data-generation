package de.uni_kassel.vs.datageneration.classification.instances;

import java.util.HashMap;
import java.util.Set;

public abstract class Instance implements Comparable<Instance> {

    public static final int PUNISHMENT_NON = 0;

    private final String id;

    public int inputsCounter;
    public int outputsCounter;

    public final HashMap<String, Integer> inputs;
    public final HashMap<String, Integer> outputs;


    public Instance(String id) {
        this.id = id;
        this.inputs = new HashMap<String, Integer>();
        this.outputs = new HashMap<String, Integer>();
        this.inputsCounter = 0;
        this.outputsCounter = 0;
    }

    public String getID() {
        return id;
    }

    public void addInput(String input) {
        Integer counter = inputs.get(input);
        if (counter == null) {
            inputs.put(input, 1);
        } else {
            inputs.put(input, ++counter);
        }
        inputsCounter++;
    }

    public void addOutput(String output) {
        Integer counter = outputs.get(output);
        if (counter == null) {
            outputs.put(output, 1);
        } else {
            outputs.put(output, ++counter);
        }
        outputsCounter++;
    }

    @Override
    public int compareTo(Instance o) {
        return id.compareTo(o.id);
    }

    public Set<String> getInputs() {
        return inputs.keySet();
    }

    public Set<String> getOutputs() {
        return outputs.keySet();
    }

    abstract public double getInputScore(String input);
    abstract public double getOutputScore(String output);
}
