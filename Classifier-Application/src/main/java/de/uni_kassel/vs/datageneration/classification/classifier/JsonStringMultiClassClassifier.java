package de.uni_kassel.vs.datageneration.classification.classifier;

import com.google.gson.Gson;
import de.uni_kassel.vs.datageneration.GameType;
import de.uni_kassel.vs.datageneration.classification.StringClassifier;
import de.uni_kassel.vs.datageneration.classification.Suggestion;
import de.uni_kassel.vs.datageneration.classification.distances.Distance;
import de.uni_kassel.vs.datageneration.classification.instances.Turn;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import libsvm.*;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class JsonStringMultiClassClassifier extends StringClassifier {
    private LinkedList<String> engineStoI;
    private LinkedList<String> gameStoI;
    private svm_model engineModel;
    private svm_model gameModel;

    protected JsonStringMultiClassClassifier(Distance distance, svm_parameter parameter) {
        super(distance, parameter == null ? getSTDParameter() : parameter);
    }

    @Override
    public void learn(List<Turn> turns) {
        HashMap<String, LinkedList<String>> turnsByEngine = new HashMap<>();
        HashMap<String, LinkedList<String>> turnsByGame = new HashMap<>();

        HashSet<String> tempEngines =  new HashSet<>();
        HashSet<String> tempGames =  new HashSet<>();

        learnCounter = turns.size();
        for (Turn turn : turns) {
            LinkedList<String> jsonGameList = turnsByGame.computeIfAbsent(turn.getGame(), k -> new LinkedList<>());
            LinkedList<String> jsonEngineList = turnsByEngine.computeIfAbsent(turn.getEngine(), k -> new LinkedList<>());
            jsonEngineList.add(new Gson().toJson(turn));
            jsonGameList.add(new Gson().toJson(turn));
            tempEngines.add(turn.getEngine());
            tempGames.add(turn.getGame());
        }

        engineStoI = new LinkedList<>(tempEngines);
        gameStoI = new LinkedList<>(tempGames);

        engineModel = getSVMModel(turnsByEngine, engineStoI);
        gameModel = getSVMModel(turnsByGame, gameStoI);
    }

    @Override
    public String checkByEngine(Turn turn) {
        LinkedList<String> turns = new LinkedList<>();
        turns.add(new Gson().toJson(turn));

        String key = null;
        double highest = 0;
        for (Entry<String, Double> entry : calcScores(turns, engineModel, engineStoI).entrySet()) {
            if (entry.getValue() > highest) {
                key = entry.getKey();
            }
        }
        return key;
    }

    @Override
    public String checkByGame(Turn turn) {
        LinkedList<String> turns = new LinkedList<>();
        turns.add(new Gson().toJson(turn));

        String key = null;
        double highest = 0;
        for (Entry<String, Double> entry : calcScores(turns, gameModel, gameStoI).entrySet()) {
            if (entry.getValue() > highest) {
                key = entry.getKey();
            }
        }
        return key;
    }

    @Override
    public void learn(File folder) throws IOException {
        File jsonFolder = new File(folder.getAbsolutePath() + File.separatorChar + "json");

        HashMap<String, LinkedList<String>> turnsByEngine = new HashMap<>();
        HashMap<String, LinkedList<String>> turnsByGame = new HashMap<>();

        HashSet<String> tempEngines =  new HashSet<>();
        HashSet<String> tempGames =  new HashSet<>();

        Gson gson = new Gson();

        for (File file : jsonFolder.listFiles()) {
            if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                learnCounter++;
                BufferedReader reader = new BufferedReader(new FileReader(file));

                String gameString = file.getName().replaceAll("\\[.*?\\]", "");
                GameType game = GameType.getFromString(gameString.substring(0, gameString.lastIndexOf('.')));

                LinkedList<String> jsonGameList = turnsByGame.computeIfAbsent(game.name(), k -> new LinkedList<>());

                String line;
                while ((line = reader.readLine()) != null) {
                    Turn turn = gson.fromJson(line, Turn.class);
                    LinkedList<String> jsonEngineList = turnsByEngine.computeIfAbsent(turn.getEngine(), k -> new LinkedList<>());
                    jsonEngineList.add(line);
                    jsonGameList.add(line);
                    tempEngines.add(turn.getEngine());
                }
                tempGames.add(game.name());
            }
        }

        engineStoI = new LinkedList<>(tempEngines);
        gameStoI = new LinkedList<>(tempGames);

        engineModel = getSVMModel(turnsByEngine, engineStoI);
        gameModel = getSVMModel(turnsByGame, gameStoI);
    }

    @Override
    public List<Suggestion> checkByGame(File folder) throws IOException {
        File jsonFolder = new File(folder.getAbsolutePath() + File.separatorChar + "json");
        ArrayList<Suggestion> result = new ArrayList<>();

        Gson gson = new Gson();
        for (File file : jsonFolder.listFiles()) {
            if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                testGameCounter++;
                BufferedReader reader = new BufferedReader(new FileReader(file));

                LinkedList<String> testWInstance = new LinkedList<>();
                LinkedList<String> testBInstance = new LinkedList<>();

                String gameString = file.getName().replaceAll("\\[.*?\\]", "");
                GameType game = GameType.getFromString(gameString.substring(0, gameString.lastIndexOf('.')));

                String line;
                while ((line = reader.readLine()) != null) {
                    Turn turn = gson.fromJson(line, Turn.class);
                    if (turn.getColor().toLowerCase().equals("w")) {
                        testWInstance.add(line);
                    } else {
                        testBInstance.add(line);
                    }
                }

                Suggestion sug = new Suggestion(file.getName(), Suggestion.Type.Game);

                sug.setWhiteReal(game.name());
                sug.setBlackReal(game.name());

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
        File jsonFolder = new File(folder.getAbsolutePath() + File.separatorChar + "json");
        ArrayList<Suggestion> result = new ArrayList<>();

        Gson gson = new Gson();
        for (File file : jsonFolder.listFiles()) {
            if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                testEngineCounter++;
                BufferedReader reader = new BufferedReader(new FileReader(file));

                LinkedList<String> testWInstance = new LinkedList<>();
                LinkedList<String> testBInstance = new LinkedList<>();

                String gameString = file.getName().replaceAll("\\[.*?\\]", "");
                GameType game = GameType.getFromString(gameString.substring(0, gameString.lastIndexOf('.')));

                String line;
                String engineB = null;
                String engineW = null;
                while ((line = reader.readLine()) != null) {
                    Turn turn = gson.fromJson(line, Turn.class);
                    if (turn.getColor().toLowerCase().equals("w")) {
                        testWInstance.add(line);
                        engineW = turn.getEngine();
                    } else {
                        testBInstance.add(line);
                        engineB = turn.getEngine();
                    }
                }

                Suggestion sug = new Suggestion(file.getName(), Suggestion.Type.Game);

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

    private svm_model getSVMModel(HashMap<String, LinkedList<String>> turnsByType, List idToI) {
        /*https://sites.google.com/site/nirajatweb/home/technical_and_coding_stuff/binary-and-multi-class-classification-java-code*/
        int size = 0;
        for (Entry<String, LinkedList<String>> entry: turnsByType.entrySet()) {
            for (String turn : entry.getValue()) {
                size++;
            }
        }
        double node_values[] = new double[size];    //jagged array used to store values
        int node_indexes[] = new int[size];         //jagged array used to store node indexes
        double node_class_labels[] = new double[size];  //store class labels

        //Now store data values
        int k = -1;
        for (Entry<String, LinkedList<String>> entry: turnsByType.entrySet()) {
            for(String turn: entry.getValue()) {
                node_class_labels[++k] = idToI.indexOf(entry.getKey());
                node_values[k] = getDistance().getScore(turn);
                node_indexes[k] = 1;
            }
        }

        svm_problem prob = new svm_problem();
        prob.y = new double[size];
        prob.l = size;
        prob.x = new svm_node[size][];

        for (int i = 0; i < size; i++) {
            prob.y[i] = node_class_labels[i];
            double values = node_values[i];
            int indexes = node_indexes[i];
            prob.x[i] = new svm_node[1];

            svm_node node = new svm_node();
            node.index = indexes;
            node.value = values;
            prob.x[i][0] = node;
        }

        //std params
        svm_parameter param = getSTDParameter();

        return svm.svm_train(prob, param);
    }

    private static svm_parameter getSTDParameter() {
        // svm_parameter param = new svm_parameter();
        // param.probability = 1;
        // param.gamma = 0.5;
        // param.nu = 0.1;
        // param.C = 2;
        // param.svm_type = svm_parameter.ONE_CLASS;
        // param.kernel_type = svm_parameter.RBF;
        // param.cache_size = 20000;
        // param.eps = 0.001;

        svm_parameter param = new svm_parameter();
        param.probability = 1;
        param.gamma = 0.5;
        param.nu = 0.5;
        param.C = 2;
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.cache_size = 20000;
        param.eps = 0.001;
        return param;
    }

    private HashMap<String, Double> calcScores(LinkedList<String> testInstance, svm_model model, LinkedList<String> ItoS) {
        HashMap<String, Double> score = new HashMap<>();
        svm_node[] nodes = new svm_node[testInstance.size()];
        int k = -1;
        for (String turn : testInstance) {
            svm_node node = new svm_node();
            node.index = 1;
            node.value = getDistance().getScore(turn);
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
