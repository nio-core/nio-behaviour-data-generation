package de.uni_kassel.vs.datageneration.classification.instances;

import java.util.LinkedList;

public class Turn {
    private String game;
    private String engine;
    private String color;
    private LinkedList<String> inputs;
    private String output;

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public LinkedList<String> getInputs() {
        return inputs;
    }

    public void setInputs(LinkedList<String> inputs) {
        this.inputs = inputs;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getOwnInput() {
        int cond = inputs.size() % 2;
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < inputs.size(); i++) {
            if(i%2 == cond) {
                String s = inputs.get(i);
                result.append(s);
            }
        }
        return result.toString();
    }

    public String getOpponentInput() {
        int cond = ((inputs.size() % 2 ) == 1) ? 0 : 1;
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < inputs.size(); i++) {
            if(i%2 == cond) {
                String s = inputs.get(i);
                result.append(s);
            }
        }
        return result.toString();
    }

    public String getInputsAsString() {
        StringBuilder builder = new StringBuilder();
        for (String string: inputs) {
            builder.append(string);
        }
        return builder.toString();
    }
}