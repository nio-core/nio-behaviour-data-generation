package de.uni_kassel.vs.datageneration.classification.classifier;

import de.uni_kassel.vs.datageneration.GameType;
import de.uni_kassel.vs.datageneration.classification.Classifier;
import de.uni_kassel.vs.datageneration.classification.instances.Instance;
import de.uni_kassel.vs.datageneration.classification.instances.SVMInstance;
import de.uni_kassel.vs.datageneration.classification.Suggestion;
import de.uni_kassel.vs.datageneration.engines.*;
import de.uni_kassel.vs.datageneration.engines.ChessEngineController;
import de.uni_kassel.vs.datageneration.engines.EngineController;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import libsvm.*;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SVMModelClassifier extends Classifier {
    private LinkedList<String> engineStoI;
    private LinkedList<String> gameStoI;

    private HashMap<GameType, svm_model> gameModels;
    private HashMap<String, svm_model> engineModels;

    @Override
    public void learn(File folder) throws IOException {
        HashMap<String, LinkedList<Instance>> instancesByEngine = new HashMap<>();
        HashMap<String, LinkedList<Instance>> instancesByGame = new HashMap<>();

        HashSet<String> tempEngines =  new HashSet<>();
        HashSet<String> tempGames =  new HashSet<>();

        for (File file : folder.listFiles()) {
            if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                learnCounter++;
                BufferedReader reader = new BufferedReader(new FileReader(file));

                String gameString = file.getName().replaceAll("\\[.*?\\]", "");
                GameType game = GameType.getFromString(gameString.substring(0, gameString.lastIndexOf('.')));


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

                    LinkedList<Instance> tempEngineList = instancesByEngine.computeIfAbsent(engine, k -> new LinkedList<>());
                    LinkedList<Instance> tempGameList = instancesByGame.computeIfAbsent(game.name(), k -> new LinkedList<>());

                    if (player.trim().toLowerCase().equals("w")) {
                        if (currentInstanceByEngineW == null) {
                            currentInstanceByEngineW = new SVMInstance(engine);
                            tempEngineList.add(currentInstanceByEngineW);
                            tempEngines.add(engine);
                        }
                        currentInstanceByEngine = currentInstanceByEngineW;
                        if (currentInstanceByGameW == null) {
                            currentInstanceByGameW = new SVMInstance(game.name());
                            tempGameList.add(currentInstanceByGameW);
                            tempGames.add(game.name());
                        }
                        currentInstanceByGame = currentInstanceByGameW;
                    } else {
                        if (currentInstanceByEngineB == null) {
                            currentInstanceByEngineB = new SVMInstance(engine);
                            tempEngineList.add(currentInstanceByEngineB);
                            tempEngines.add(engine);
                        }
                        currentInstanceByEngine = currentInstanceByEngineB;
                        if (currentInstanceByGameB == null) {
                            currentInstanceByGameB = new SVMInstance(game.name());
                            tempGameList.add(currentInstanceByGameB);
                            tempGames.add(game.name());
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

        gameModels = new HashMap<>();
        for (Map.Entry<String, LinkedList<Instance>> entry : instancesByGame.entrySet()) {
            gameModels.put(GameType.getFromString(entry.getKey()), getSVMModel(entry.getValue(), gameStoI));
        }

        engineModels = new HashMap<>();
        for (Map.Entry<String, LinkedList<Instance>> entry : instancesByEngine.entrySet()) {
            engineModels.put(entry.getKey(), getSVMModel(entry.getValue(), engineStoI));
        }
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

                String gameString = file.getName().replaceAll("\\[.*?\\]", "");
                GameType game = GameType.getFromString(gameString.substring(0, gameString.lastIndexOf('.')));

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

                HashMap<String, Double> foundW = new HashMap<>();
                HashMap<String, Double> foundB = new HashMap<>();
                for (Map.Entry<GameType, svm_model> entry: gameModels.entrySet()) {
                    if (check(testWInstance, gameStoI, entry.getValue())) {
                        foundW.put(entry.getKey().name(), 1.0);
                    }
                    if (check(testBInstance, gameStoI, entry.getValue())) {
                        foundB.put(entry.getKey().name(), 1.0);
                    }
                }


                Suggestion sug = new Suggestion(file.getName(), Suggestion.Type.Game);

                sug.setWhiteReal(game.name());
                sug.setBlackReal(game.name());

                sug.setWhiteScoreAndSug(foundW);
                sug.setBlackScoreAndSug(foundB);

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

                HashMap<String, Double> foundW = new HashMap<>();
                HashMap<String, Double> foundB = new HashMap<>();
                for (Map.Entry<GameType, svm_model> entryG: gameModels.entrySet()) {
                    boolean w = (check(testWInstance, engineStoI, entryG.getValue()));
                    boolean b = (check(testBInstance, engineStoI, entryG.getValue()));
                    for (Map.Entry<String, svm_model> entryE: engineModels.entrySet()) {
                        if (util(ChessEngineController.Type.values(), entryE.getKey()) && entryG.getKey() == GameType.Chess) {
                            if (w && check(testWInstance, engineStoI, entryE.getValue())) {
                                foundW.put(entryE.getKey(), 1.0);
                            }
                            if (b && check(testBInstance, engineStoI, entryE.getValue())) {
                                foundB.put(entryE.getKey(), 1.0);
                            }
                        }
                        if (util(CheckerEngineController.Type.values(), entryE.getKey()) && entryG.getKey() == GameType.Checkers) {
                            if (w && check(testWInstance, engineStoI, entryE.getValue())) {
                                foundW.put(entryE.getKey(), 1.0);
                            }
                            if (b && check(testBInstance, engineStoI, entryE.getValue())) {
                                foundB.put(entryE.getKey(), 1.0);
                            }
                        }
                        if (util(ChineseChessEngineController.Type.values(), entryE.getKey()) && entryG.getKey() == GameType.ChineseChess) {
                            if (w && check(testWInstance, engineStoI, entryE.getValue())) {
                                foundW.put(entryE.getKey(), 1.0);
                            }
                            if (b && check(testBInstance, engineStoI, entryE.getValue())) {
                                foundB.put(entryE.getKey(), 1.0);
                            }
                        }
                    }
                }


                Suggestion sug = new Suggestion(file.getName(), Suggestion.Type.Engine);

                sug.setWhiteReal(engineW);
                sug.setBlackReal(engineB);

                sug.setWhiteScoreAndSug(foundW);
                sug.setBlackScoreAndSug(foundB);

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

    private boolean check(Instance testInstance, LinkedList<String> idStoI, svm_model model) {
        LinkedList<Instance> instances = new LinkedList<>();
        instances.add(testInstance);
        svm_model testmodel = getSVMModel(instances, idStoI);
        return compareModel(model, testmodel);
    }

    private boolean compareModel(svm_model model, svm_model testmodel) {

        return false;
    }

    private svm_model getSVMModel(List<Instance> instances, LinkedList<String> idStoI) {
        /*https://sites.google.com/site/nirajatweb/home/technical_and_coding_stuff/binary-and-multi-class-classification-java-code*/
        double node_values[][] = new double[instances.size()][];    //jagged array used to store values
        int node_indexes[][] = new int[instances.size()][];         //jagged array used to store node indexes
        double node_class_labels[] = new double[instances.size()];  //store class labels

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
        param.nu = 0.1;
        param.C = 2;
        param.svm_type = svm_parameter.ONE_CLASS;
        param.kernel_type = svm_parameter.RBF;
        param.cache_size = 20000;
        param.eps = 0.001;

        return svm.svm_train(prob, param);
    }

    private boolean util(EngineController.EngineType[] typeArray, String name) {
        for (EngineController.EngineType type : typeArray){
            if (type.name().equals(name)) {
                return true;
            }
        }
        return false;
    }
}

