package de.uni_kassel.vs.datageneration.classification.classifier;

import de.uni_kassel.vs.datageneration.classification.Classifier;
import de.uni_kassel.vs.datageneration.classification.instances.DistributionInstance;
import de.uni_kassel.vs.datageneration.classification.instances.Instance;
import de.uni_kassel.vs.datageneration.classification.Suggestion;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import org.apache.commons.io.FilenameUtils;
import plotter.Graphic;
import plotter.LineStyle;
import plotter.Plotter;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

public class DistributionClassifier extends Classifier {

    private HashMap<String, Instance> instancesByEngine;
    private HashMap<String, Instance> instancesByGame;

    private void plott(HashMap<String, Instance> instances, String name) {
        Graphic graphic = new Graphic(name);
        Plotter plotter = graphic.getPlotter();

        plotter.setPreferredSize(new Dimension(1500, 1000));
        graphic.pack();

        double highest = 0;
        for (Entry<String, Instance> entry : instances.entrySet()) {
            plotter.setXLine(0);
            plotter.setYLine(0);
            plotter.setDataLineStyle(LineStyle.HISTOGRAM);
            plotter.setDataStroke(new BasicStroke(2));
            ArrayList<String> inputs = new ArrayList<>(entry.getValue().getInputs());
            Collections.sort(inputs);
            for (String input : inputs) {
                double inputScore = entry.getValue().getInputScore(input);
                plotter.add(convertMoveToInt(input), inputScore);
                if (inputScore > highest) {
                    highest = inputScore;
                }
            }
            plotter.nextDataSet();
        }
        plotter.setYrange(0, highest);
    }

    @Override
    public void learn(File folder) throws IOException {
        instancesByEngine = new HashMap<>();
        instancesByGame = new HashMap<>();

        for (File file : folder.listFiles()) {
            if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                learnCounter++;
                BufferedReader reader = new BufferedReader(new FileReader(file));

                String game = file.getName().replaceAll("\\[.*?\\]", "");
                game = game.substring(0, game.lastIndexOf('.'));

                Instance currentInstanceByGame = instancesByGame.get(game);
                if (currentInstanceByGame == null) {
                    currentInstanceByGame = new DistributionInstance(game);
                    instancesByGame.put(game, currentInstanceByGame);
                }

                // remove header
                String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",");

                    String engine = fields[0];
                    Instance currentInstanceByEngine = instancesByEngine.get(engine);
                    if (currentInstanceByEngine == null) {
                        currentInstanceByEngine = new DistributionInstance(engine);
                        instancesByEngine.put(engine, currentInstanceByEngine);
                    }

                    String inputs = fields[2];
                    for (String input : inputs.split(" ")) {
                        if (input.trim().length() == 8) {
                            currentInstanceByEngine.addInput(input.trim().substring(0, 4));
                            currentInstanceByGame.addInput(input.trim().substring(0,4));
                            currentInstanceByEngine.addInput(input.trim().substring(4));
                            currentInstanceByGame.addInput(input.trim().substring(4));
                        } else {
                            currentInstanceByEngine.addInput(input.trim());
                            currentInstanceByGame.addInput(input.trim());
                        }
                    }

                    String output = fields[3].trim();
                    if (output.length() == 8) {
                        currentInstanceByEngine.addOutput(output.substring(0,4));
                        currentInstanceByGame.addOutput(output.substring(0,4));
                        currentInstanceByEngine.addOutput(output.substring(4));
                        currentInstanceByGame.addOutput(output.substring(4));
                    } else {
                        currentInstanceByEngine.addOutput(output);
                        currentInstanceByGame.addOutput(output);
                    }
                }
            }
        }

        print(instancesByGame, "Game");
        print(instancesByEngine, "EngineController");
    }

    @Override
    public ArrayList<Suggestion> checkByGame(File folder) throws IOException {
        ArrayList<Suggestion> result = new ArrayList<>();
        for (File file : folder.listFiles())
            if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                testGameCounter++;
                BufferedReader reader = new BufferedReader(new FileReader(file));

                Instance testWInstance = new DistributionInstance("testW");
                Instance testBInstance = new DistributionInstance("testB");

                String game = file.getName().replaceAll("\\[.*?\\]", "");
                game = game.substring(0, game.lastIndexOf('.'));

                // remove header
                String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",");

                    String inputs = fields[2];
                    for (String input : inputs.split(" ")) {
                        if (fields[1].trim().toLowerCase().equals("w")) {
                            if (input.trim().length() == 8) {
                                testWInstance.addInput(input.trim().substring(0,4));
                                testWInstance.addInput(input.trim().substring(4));
                            } else {
                                testWInstance.addInput(input.trim());
                            }
                        } else {
                            if (input.trim().length() == 8) {
                                testBInstance.addInput(input.trim().substring(0,4));
                                testBInstance.addInput(input.trim().substring(4));
                            } else {
                                testBInstance.addInput(input.trim());
                            }
                        }
                    }

                    String output = fields[3];
                    if (fields[1].trim().toLowerCase().equals("w")) {
                        if (output.trim().length() == 8) {
                            testWInstance.addInput(output.trim().substring(0,4));
                            testWInstance.addInput(output.trim().substring(4));
                        } else {
                            testWInstance.addOutput(output.trim());
                        }
                    } else {
                        if (output.trim().length() == 8) {
                            testWInstance.addInput(output.trim().substring(0,4));
                            testWInstance.addInput(output.trim().substring(4));
                        } else {
                            testBInstance.addOutput(output.trim());
                        }
                    }
                }

                Suggestion sug = new Suggestion(file.getName(), Suggestion.Type.Game);

                sug.setWhiteReal(game);
                sug.setBlackReal(game);

                sug.setWhiteScoreAndSug(calcScores(testWInstance, instancesByGame));
                sug.setBlackScoreAndSug(calcScores(testBInstance, instancesByGame));

                if (sug.getWhiteReal().equals(sug.getWhiteSug())) {
                    gamePositive++;
                } else {
                    gameNegative++;
                }

                if (sug.getBlackReal().equals(sug.getBlackSug())) {
                    gamePositive++;
                } else {
                    gameNegative++;
                }

                DebugLogger.writeMessage(this.getClass(), sug.toString());

                result.add(sug);
            }
        return result;
    }

    @Override
    public ArrayList<Suggestion> checkByEngine(File folder) throws IOException {
        ArrayList<Suggestion> result = new ArrayList<>();
        for (File file : folder.listFiles()) {
            if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                testEngineCounter++;
                BufferedReader reader = new BufferedReader(new FileReader(file));

                Instance testWInstance = new DistributionInstance("testW");
                Instance testBInstance = new DistributionInstance("testB");

                String engineW = null;
                String engineB = null;

                // remove header
                String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",");

                    if (engineW == null && fields[1].trim().toLowerCase().equals("w")) {
                        engineW = fields[0].trim();
                    } else if (engineB == null && fields[1].trim().toLowerCase().equals("b")) {
                        engineB = fields[0].trim();
                    }

                    String inputs = fields[2];
                    for (String input : inputs.split(" ")) {
                        if (fields[1].trim().toLowerCase().equals("w")) {
                            if (input.trim().length() == 8) {
                                testWInstance.addInput(input.trim().substring(0,4));
                                testWInstance.addInput(input.trim().substring(4));
                            } else {
                                testWInstance.addInput(input.trim());
                            }
                        } else {
                            if (input.trim().length() == 8) {
                                testBInstance.addInput(input.trim().substring(0,4));
                                testBInstance.addInput(input.trim().substring(4));
                            } else {
                                testBInstance.addInput(input.trim());
                            }
                        }
                    }

                    String output = fields[3];
                    if (fields[1].trim().toLowerCase().equals("w")) {
                        if (output.trim().length() == 8) {
                            testWInstance.addInput(output.trim().substring(0,4));
                            testWInstance.addInput(output.trim().substring(4));
                        } else {
                            testWInstance.addOutput(output.trim());
                        }
                    } else {
                        if (output.trim().length() == 8) {
                            testWInstance.addInput(output.trim().substring(0,4));
                            testWInstance.addInput(output.trim().substring(4));
                        } else {
                            testBInstance.addOutput(output.trim());
                        }
                    }
                }

                Suggestion sug = new Suggestion(file.getName(), Suggestion.Type.Engine);

                sug.setWhiteReal(engineW);
                sug.setBlackReal(engineB);

                sug.setWhiteScoreAndSug(calcScores(testWInstance, instancesByEngine));
                sug.setBlackScoreAndSug(calcScores(testBInstance, instancesByEngine));


                if (sug.getWhiteReal().equals(sug.getWhiteSug())) {
                    enginePositive++;
                } else {
                    engineNegative++;
                }

                if (sug.getBlackReal().equals(sug.getBlackSug())) {
                    enginePositive++;
                } else {
                    engineNegative++;
                }

                DebugLogger.writeMessage(this.getClass(), sug.toString());

                result.add(sug);
            }
        }
        return result;
    }

    private HashMap<String, Double> calcScores(Instance testInstance, HashMap<String, Instance> instances) {
        HashMap<String, Double> scoreStore = new HashMap<>();

        for (String input: testInstance.getInputs()) {
            double testScore = testInstance.getInputScore(input);
            for (Instance i : instances.values()) {
                Double score = scoreStore.get(i.getID());
                if (score == null) {
                    double instanceScore = i.getInputScore(input);
                    double newScore = Math.abs(testScore - instanceScore);
                    scoreStore.put(i.getID(), newScore);
                } else {
                    double instanceScore = i.getInputScore(input);
                    double newScore = (Math.abs(testScore - instanceScore) + score) / 2.0;
                    scoreStore.put(i.getID(), newScore);
                }
            }
        }

        for (String output: testInstance.getOutputs()) {
            double testScore = testInstance.getOutputScore(output);
            for (Instance i : instances.values()) {
                Double score = scoreStore.get(i.getID());
                if (score == null) {
                    double instanceScore = i.getOutputScore(output);
                    double newScore = Math.abs(testScore - instanceScore);
                    scoreStore.put(i.getID(), newScore);
                } else {
                    double instanceScore = i.getOutputScore(output);
                    double newScore = Math.abs(testScore - instanceScore) / 2.0;
                    scoreStore.put(i.getID(), newScore);
                }
            }
        }

        for (Entry<String, Double> entry : scoreStore.entrySet()) {
            scoreStore.put(entry.getKey(), 1-entry.getValue());
        }

        return scoreStore;
    }

    private void print(HashMap<String, Instance> instances, String name) {
        if ("Game".equals(name)) {
            System.out.println("Turn,Chess,ChineseChess,Checkers");
            System.out.println("" + "," + instances.get("Chess").getInputScore("") + "," + instances.get("ChineseChess").getInputScore("") + "," + instances.get("Checkers").getInputScore(""));
            for (int a = 0; a < 9; a++) {
                char first = (char) ('a' + a);
                for (int b = 0; b < 10; b++) {
                    int second = b;
                    for (int c = 0; c < 9; c++) {
                        char third = (char) ('a' + c);
                        for (int d = 0; d < 10; d++) {
                            int fourth = d;
                            String turn = String.valueOf(first) + second + third + fourth;
                            System.out.println(turn + "," + instances.get("Chess").getInputScore(turn) + "," + instances.get("ChineseChess").getInputScore(turn) + "," + instances.get("Checkers").getInputScore(turn));
                        }
                    }
                }
            }
        } else {
            System.out.println("Turn,stockfish,laser,mars,pulse,eleeye,ponder,fruitReloaded");
            System.out.println("" + "," + instances.get("stockfish").getInputScore("") + "," + instances.get("laser").getInputScore("") + "," + instances.get("mars").getInputScore("") + "," + instances.get("pulse").getInputScore("") + "," + instances.get("eleeye").getInputScore("") + "," + instances.get("ponder").getInputScore("") + "," + instances.get("fruitReloaded").getInputScore(""));
            for (int a = 0; a < 9; a++) {
                char first = (char) ('a' + a);
                for (int b = 0; b < 10; b++) {
                    int second = b;
                    for (int c = 0; c < 9; c++) {
                        char third = (char) ('a' + c);
                        for (int d = 0; d < 10; d++) {
                            int fourth = d;
                            String turn = String.valueOf(first) + second + third + fourth;
                            System.out.println(turn + "," + instances.get("stockfish").getInputScore(turn) + "," + instances.get("laser").getInputScore(turn) + "," + instances.get("mars").getInputScore(turn) + "," + instances.get("pulse").getInputScore(turn) + "," + instances.get("eleeye").getInputScore(turn) + "," + instances.get("ponder").getInputScore(turn) + "," + instances.get("fruitReloaded").getInputScore(turn));
                        }
                    }
                }
            }
        }
    }
}
