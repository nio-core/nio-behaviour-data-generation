package de.uni_kassel.vs.datageneration.classification.classifier;

import de.uni_kassel.vs.datageneration.classification.Classifier;
import de.uni_kassel.vs.datageneration.classification.instances.Instance;
import de.uni_kassel.vs.datageneration.classification.instances.SVMInstance;
import de.uni_kassel.vs.datageneration.classification.Suggestion;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import libsvm.*;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class MultiClassSVMClassifier extends Classifier {

    private LinkedList<String> engineStoI;
    private LinkedList<String> gameStoI;

    private svm_model engineModel;
    private svm_model gameModel;

    @Override
    public void learn(File folder) throws IOException {
        LinkedList<Instance> instancesByEngine = new LinkedList<>();
        LinkedList<Instance> instancesByGame = new LinkedList<>();

        HashSet<String> tempEngines =  new HashSet<>();
        HashSet<String> tempGames =  new HashSet<>();

        for (File file : folder.listFiles()) {
            if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                learnCounter++;
                BufferedReader reader = new BufferedReader(new FileReader(file));

                String game = file.getName().replaceAll("\\[.*?\\]", "");
                game = game.substring(0, game.lastIndexOf('.'));

                Instance currentInstanceByGameW = null;
                Instance currentInstanceByGameB = null;
                Instance currentInstanceByGame;

                Instance currentInstanceByEngineW = null;
                Instance currentInstanceByEngineB = null;
                Instance currentInstanceByEngine;

                // remove header
                String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",");

                    String engine = fields[0];
                    String player = fields[1];
                    if (player.trim().toLowerCase().equals("w")) {
                        if (currentInstanceByEngineW == null) {
                            currentInstanceByEngineW = new SVMInstance(engine);
                            instancesByEngine.add(currentInstanceByEngineW);
                            tempEngines.add(engine);
                        }
                        currentInstanceByEngine = currentInstanceByEngineW;
                        if (currentInstanceByGameW == null) {
                            currentInstanceByGameW = new SVMInstance(game);
                            instancesByGame.add(currentInstanceByGameW);
                            tempGames.add(game);
                        }
                        currentInstanceByGame = currentInstanceByGameW;
                    } else {
                        if (currentInstanceByEngineB == null) {
                            currentInstanceByEngineB = new SVMInstance(engine);
                            instancesByEngine.add(currentInstanceByEngineB);
                            tempEngines.add(engine);
                        }
                        currentInstanceByEngine = currentInstanceByEngineB;
                        if (currentInstanceByGameB == null) {
                            currentInstanceByGameB = new SVMInstance(game);
                            instancesByGame.add(currentInstanceByGameB);
                            tempGames.add(game);
                        }
                        currentInstanceByGame = currentInstanceByGameB;
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

        engineStoI = new LinkedList<>(tempEngines);
        gameStoI = new LinkedList<>(tempGames);

        engineModel = getSVMModel(instancesByEngine, engineStoI);
        gameModel = getSVMModel(instancesByGame, gameStoI);
    }

    private svm_model getSVMModel(List<Instance> instances, LinkedList<String> idStoI) {
        /*https://sites.google.com/site/nirajatweb/home/technical_and_coding_stuff/binary-and-multi-class-classification-java-code*/
        double node_values[][] = new double[instances.size()][];    //jagged array used to store values
        int node_indexes[][] = new int[instances.size()][];         //jagged array used to store node indexes
        double node_class_labels[] = new double[instances.size()]; //store class labels

        //Now store data values
        int k = -1;
        for (Instance inst: instances) {
            try {
                node_class_labels[++k] = idStoI.indexOf(inst.getID());

                LinkedList<Integer> list_index = new LinkedList<>();
                LinkedList<Double> list_val = new LinkedList<>();

                for (String input : inst.getInputs()) {
                    list_index.add(convertMoveToInt((input)));
                    list_val.add(inst.getInputScore(input));
                }

                for (String output : inst.getOutputs()) {
                    list_index.add(convertMoveToInt((output)));
                    list_val.add(inst.getOutputScore(output));
                }

                if(list_val.size() > 0) {
                    node_values[k] = new double[list_val.size()];
                    node_indexes[k] = new int[list_index.size()];
                }

                for (int m = 0; m < list_val.size(); m++) {
                    node_indexes[k][m] = list_index.get(m);
                    node_values[k][m] = list_val.get(m);
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        svm_problem prob = new svm_problem();
        prob.y = new double[instances.size()];
        prob.l = instances.size();
        prob.x = new svm_node[instances.size()][];

        for (int i = 0; i < instances.size(); i++) {
            prob.y[i] = node_class_labels[i];
            double[] values = node_values[i];
            int [] indexes = node_indexes[i];
            prob.x[i] = new svm_node[values.length];

            for (int j = 0; j < values.length; j++) {
                svm_node node = new svm_node();
                node.index = indexes[j];
                node.value = values[j];
                prob.x[i][j] = node;
            }
        }

        svm_parameter param = new svm_parameter();
        param.probability = 1;
        param.gamma = 0.5;
        param.nu = 0.5;
        param.C = 1;
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.cache_size = 20000;
        param.eps = 0.001;

        return svm.svm_train(prob, param);
    }

    @Override
    public List<Suggestion> checkByGame(File folder) throws IOException {
        ArrayList<Suggestion> result = new ArrayList<>();
        for (File file : folder.listFiles()) {
            if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                testGameCounter++;
                BufferedReader reader = new BufferedReader(new FileReader(file));

                Instance testWInstance = new SVMInstance("testW");
                Instance testBInstance = new SVMInstance("testB");

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
                                testWInstance.addInput(input.trim().substring(0, 4));
                                testWInstance.addInput(input.trim().substring(4));
                            } else {
                                testWInstance.addInput(input.trim());
                            }
                        } else {
                            if (input.trim().length() == 8) {
                                testBInstance.addInput(input.trim().substring(0, 4));
                                testBInstance.addInput(input.trim().substring(4));
                            } else {
                                testBInstance.addInput(input.trim());
                            }
                        }
                    }

                    String output = fields[3];
                    if (fields[1].trim().toLowerCase().equals("w")) {
                        if (output.trim().length() == 8) {
                            testWInstance.addInput(output.trim().substring(0, 4));
                            testWInstance.addInput(output.trim().substring(4));
                        } else {
                            testWInstance.addOutput(output.trim());
                        }
                    } else {
                        if (output.trim().length() == 8) {
                            testWInstance.addInput(output.trim().substring(0, 4));
                            testWInstance.addInput(output.trim().substring(4));
                        } else {
                            testBInstance.addOutput(output.trim());
                        }
                    }
                }

                Suggestion sug = new Suggestion(file.getName(), Suggestion.Type.Game);

                sug.setWhiteReal(game);
                sug.setBlackReal(game);

                sug.setWhiteScoreAndSug(calcScores(testWInstance, gameModel, gameStoI));
                sug.setBlackScoreAndSug(calcScores(testBInstance, gameModel, gameStoI));

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
        }
        return result;
    }

    @Override
    public List<Suggestion> checkByEngine(File folder) throws IOException {
        ArrayList<Suggestion> result = new ArrayList<>();
        for (File file : folder.listFiles()) {
            if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                testEngineCounter++;
                BufferedReader reader = new BufferedReader(new FileReader(file));

                Instance testWInstance = new SVMInstance("testW");
                Instance testBInstance = new SVMInstance("testB");

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

                sug.setWhiteScoreAndSug(calcScores(testWInstance, engineModel, engineStoI));
                sug.setBlackScoreAndSug(calcScores(testBInstance, engineModel, engineStoI));


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

    //write the code to test single feature each time by using SVM
    private HashMap<String, Double> calcScores(Instance testInstance, svm_model model, LinkedList<String> ItoS) {
        HashMap<String, Double> score = new HashMap<>();
        svm_node[] nodes = new svm_node[testInstance.getInputs().size() + testInstance.getOutputs().size()];

        int k = -1;
        for (String input : testInstance.getInputs()) {
            svm_node node = new svm_node();
            node.index = convertMoveToInt(input);
            node.value = testInstance.getInputScore(input);
            nodes[++k] = node;
        }

        for (String output : testInstance.getOutputs()) {
            svm_node node = new svm_node();
            node.index = convertMoveToInt(output);
            node.value = testInstance.getOutputScore(output);
            nodes[++k] = node;
        }

        int totalClasses = svm.svm_get_nr_class(model);
        int[] labels = new int[totalClasses];
        svm.svm_get_labels(model,labels);

        double[] prob_estimates = new double[totalClasses];
        double v = svm.svm_predict_probability(model, nodes, prob_estimates);

        for (int i = 0; i < totalClasses; i++) {
            score.put(ItoS.get(labels[i]), prob_estimates[i]);
        }
        //System.out.println(" Prediction:" + v );
        return score;
    }
}
