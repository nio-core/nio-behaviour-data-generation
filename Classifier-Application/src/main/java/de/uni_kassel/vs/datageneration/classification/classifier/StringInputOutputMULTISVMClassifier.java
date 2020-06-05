package de.uni_kassel.vs.datageneration.classification.classifier;

import com.google.gson.Gson;
import de.uni_kassel.vs.datageneration.GameType;
import de.uni_kassel.vs.datageneration.classification.StringClassifier;
import de.uni_kassel.vs.datageneration.classification.Suggestion;
import de.uni_kassel.vs.datageneration.classification.distances.Distance;
import de.uni_kassel.vs.datageneration.classification.instances.Turn;
import libsvm.*;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class StringInputOutputMULTISVMClassifier extends StringClassifier {

    private static final int OWN_INPUT_INDEX = 0;
    private static final int OPPONENT_INPUT_INDEX = 1;
    private static final int OUTPUT_INDEX = 2;

    private LinkedList<String> engineStoI;
    private LinkedList<String> gameStoI;

    private svm_model gameModel;
    private svm_model engineModel;

    public StringInputOutputMULTISVMClassifier(Distance distance, svm_parameter parameter) {
        super(distance, (parameter == null) ? getSTDParameter() : parameter );
    }

    @Override
    public void learn(List<Turn> turns) {
        HashMap<String, LinkedList<Turn>> turnsByEngine = new HashMap<>();
        HashMap<String, LinkedList<Turn>> turnsByGame = new HashMap<>();

        learnCounter = turns.size();

        for (Turn turn: turns) {
            turnsByGame.computeIfAbsent(turn.getGame(), k -> new LinkedList<>()).add(turn);
            turnsByEngine.computeIfAbsent(turn.getEngine(), k -> new LinkedList<>()).add(turn);
        }

        gameStoI = new LinkedList<>(turnsByGame.keySet());
        engineStoI = new LinkedList<>(turnsByEngine.keySet());

        gameModel = getSVMModel(turnsByGame, gameStoI);

        engineModel = getSVMModel(turnsByEngine, engineStoI);
    }

    @Override
    public String checkByEngine(Turn turn) {
        LinkedList<Turn> turns = new LinkedList<>();
        turns.add(turn);
        String engine = turn.getEngine();
        Map.Entry<String, Double> best = null;
        for (Map.Entry<String, Double> entry : calcScores(turns, engineModel, engineStoI).entrySet()) {
            if (best == null) {
                best = entry;
            } else {
                if (best.getValue() < entry.getValue()) {
                    best = entry;
                }
            }
        }
        return engine.equals(best.getKey()) ? "TRUE" : "FALSE";
    }

    @Override
    public String checkByGame(Turn turn) {
        LinkedList<Turn> turns = new LinkedList<>();
        turns.add(turn);
        String game = turn.getGame();
        Map.Entry<String, Double> best = null;
        for (Map.Entry<String, Double> entry : calcScores(turns, gameModel, gameStoI).entrySet()) {
            if (best == null) {
                best = entry;
            } else {
                if (best.getValue() < entry.getValue()) {
                    best = entry;
                }
            }
        }
        return game.equals(best.getKey()) ? "TRUE" : "FALSE";
    }

    private static svm_parameter getSTDParameter() {
        svm_parameter param = new svm_parameter();
        param.probability = 1;
        param.gamma = 0.5;
        param.nu = 0.5;
        param.C = 1;
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.cache_size = 20000;
        param.eps = 0.001;
        return param;
    }

    @Override
    public void learn(File folder) throws IOException {
        File jsonFolder = new File(folder.getAbsolutePath() + File.separatorChar + "json");

        HashMap<String, LinkedList<Turn>> turnsByEngine = new HashMap<>();
        HashMap<String, LinkedList<Turn>> turnsByGame = new HashMap<>();

        Gson gson = new Gson();

        for (File file : jsonFolder.listFiles()) {
            if (FilenameUtils.getExtension(file.getName()).equals("csv")) {
                learnCounter++;
                BufferedReader reader = new BufferedReader(new FileReader(file));

                String gameString = file.getName().replaceAll("\\[.*?\\]", "");
                GameType game = GameType.getFromString(gameString.substring(0, gameString.lastIndexOf('.')));

                LinkedList<Turn> jsonGameList = turnsByGame.computeIfAbsent(game.name(), k -> new LinkedList<>());

                String line;
                while ((line = reader.readLine()) != null) {
                    Turn turn = gson.fromJson(line, Turn.class);
                    LinkedList<Turn> jsonEngineList = turnsByEngine.computeIfAbsent(turn.getEngine(), k -> new LinkedList<>());
                    jsonEngineList.add(turn);
                    jsonGameList.add(turn);
                }
            }
        }

        gameStoI = new LinkedList<>(turnsByGame.keySet());
        engineStoI = new LinkedList<>(turnsByEngine.keySet());

        gameModel = getSVMModel(turnsByGame, gameStoI);

        engineModel = getSVMModel(turnsByEngine, engineStoI);

        //plott(turnsByGame, "Game:"+getDistance().getClass().getSimpleName());
        //plott(turnsByEngine, "EngineController:"+getDistance().getClass().getSimpleName());
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

                LinkedList<Turn> testWInstance = new LinkedList<>();
                LinkedList<Turn> testBInstance = new LinkedList<>();

                String gameString = file.getName().replaceAll("\\[.*?\\]", "");
                GameType game = GameType.getFromString(gameString.substring(0, gameString.lastIndexOf('.')));

                String line;
                while ((line = reader.readLine()) != null) {
                    Turn turn = gson.fromJson(line, Turn.class);
                    if (turn.getColor().toLowerCase().equals("w")) {
                        testWInstance.add(turn);
                    } else {
                        testBInstance.add(turn);
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


                //DebugLogger.writeMessage(this.getClass(), sug.toString());

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

                LinkedList<Turn> testWInstance = new LinkedList<>();
                LinkedList<Turn> testBInstance = new LinkedList<>();

                String gameString = file.getName().replaceAll("\\[.*?\\]", "");
                GameType game = GameType.getFromString(gameString.substring(0, gameString.lastIndexOf('.')));

                String engineW = null;
                String engineB = null;
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    Turn turn = gson.fromJson(line, Turn.class);
                    if (turn.getColor().toLowerCase().equals("w")) {
                        engineW = turn.getEngine();
                        testWInstance.add(turn);
                    } else {
                        engineB = turn.getEngine();
                        testBInstance.add(turn);
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

                //DebugLogger.writeMessage(this.getClass(), sug.toString());

                result.add(sug);
            }
        }
        return result;
    }

    private svm_model getSVMModel(HashMap<String, LinkedList<Turn>> turns, LinkedList<String> StoI) {
        /*https://sites.google.com/site/nirajatweb/home/technical_and_coding_stuff/binary-and-multi-class-classification-java-code*/
        int size = 0;
        for (Map.Entry<String, LinkedList<Turn>> entry : turns.entrySet()) {
            size += entry.getValue().size();
        }

        double[][] node_values = new double[size][];    //jagged array used to store values
        int[][] node_indexes = new int[size][];         //jagged array used to store node indexes
        double[] node_class_labels = new double[size];  //store class labels

        //Now store data values
        int k = -1;
        for (Map.Entry<String, LinkedList<Turn>> entry : turns.entrySet()) {
            for (Turn turn : entry.getValue()) {
                node_class_labels[++k] = StoI.indexOf(entry.getKey());

                LinkedList<Integer> list_index = new LinkedList<>();
                LinkedList<Double> list_val = new LinkedList<>();

                list_index.add(OWN_INPUT_INDEX);
                list_val.add(getDistance().getScore(turn.getInputsAsString()));

                //list_index.add(OPPONENT_INPUT_INDEX);
                //list_val.add(getDistance().getScore(turn.getOpponentInput()));

                list_index.add(OUTPUT_INDEX);
                list_val.add(getDistance().getScore(turn.getOutput()));

                if (list_val.size() > 0) {
                    node_values[k] = new double[list_val.size()];
                    node_indexes[k] = new int[list_index.size()];
                }

                for (int m = 0; m < list_val.size(); m++) {
                    node_indexes[k][m] = list_index.get(m);
                    node_values[k][m] = list_val.get(m);
                }
            }
        }

        svm_problem prob = new svm_problem();
        prob.y = new double[size];
        prob.l = size;
        prob.x = new svm_node[size][];

        for (int i = 0; i < size; i++) {
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

        return svm.svm_train(prob, param);
    }

    private HashMap<String, Double> calcScores(LinkedList<Turn> turns, svm_model model, LinkedList<String> ItoS) {
        HashMap<String, Double> score = new HashMap<>();
        svm_node[] nodes = new svm_node[2 * turns.size()];

        int k = -1;
        for (Turn turn : turns) {
            svm_node node = new svm_node();
            node.index = OWN_INPUT_INDEX;
            node.value = getDistance().getScore(turn.getInputsAsString());
            nodes[++k] = node;

            node = new svm_node();
            node.index = OUTPUT_INDEX;
            node.value = getDistance().getScore(turn.getOutput());
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
