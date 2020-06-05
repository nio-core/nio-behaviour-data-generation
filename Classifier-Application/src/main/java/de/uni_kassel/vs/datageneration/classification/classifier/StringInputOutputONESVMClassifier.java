package de.uni_kassel.vs.datageneration.classification.classifier;

import com.google.gson.Gson;
import de.uni_kassel.vs.datageneration.GameType;
import de.uni_kassel.vs.datageneration.classification.StringClassifier;
import de.uni_kassel.vs.datageneration.classification.Suggestion;
import de.uni_kassel.vs.datageneration.classification.distances.Distance;
import de.uni_kassel.vs.datageneration.classification.instances.Turn;
import de.uni_kassel.vs.datageneration.engines.CheckerEngineController;
import de.uni_kassel.vs.datageneration.engines.ChessEngineController;
import de.uni_kassel.vs.datageneration.engines.ChineseChessEngineController;
import de.uni_kassel.vs.datageneration.engines.EngineController;
import libsvm.*;
import org.apache.commons.io.FilenameUtils;
import plotter.Graphic;
import plotter.LineStyle;
import plotter.Plotter;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class StringInputOutputONESVMClassifier extends StringClassifier {

    private static final int OWN_INPUT_INDEX = 0;
    private static final int OPPONENT_INPUT_INDEX = 1;
    private static final int OUTPUT_INDEX = 2;

    private HashMap<String, svm_model> gameModels;
    private HashMap<String, svm_model> engineModels;

    public StringInputOutputONESVMClassifier(Distance distance, svm_parameter parameter) {
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

        gameModels = new HashMap<>();
        for (Map.Entry<String, LinkedList<Turn>> entry : turnsByGame.entrySet()) {
            gameModels.put(entry.getKey(), getSVMModel(entry.getValue()));
        }

        engineModels = new HashMap<>();
        for (Map.Entry<String, LinkedList<Turn>> entry : turnsByEngine.entrySet()) {
            engineModels.put(entry.getKey(), getSVMModel(entry.getValue()));
        }

    }

    @Override
    public String checkByEngine(Turn turn) {
        LinkedList<Turn> turns = new LinkedList<>();
        turns.add(turn);
        String engine = turn.getEngine();
        svm_model model = engineModels.get(engine);
        return check(turns, model) ? "TRUE" : "FALSE";
    }

    @Override
    public String checkByGame(Turn turn) {
        LinkedList<Turn> turns = new LinkedList<>();
        turns.add(turn);
        return check(turns, gameModels.get(turn.getGame())) ? "TRUE": "FALSE";
    }

    private static svm_parameter getSTDParameter() {
        svm_parameter param = new svm_parameter();
        param.probability = 1;
        param.gamma = 0.1;
        param.nu = 0.02;
        param.C = 2;
        param.svm_type = svm_parameter.ONE_CLASS;
        param.kernel_type = svm_parameter.LINEAR;
        param.cache_size = 20000;
        param.eps = 0.01;
        param.p = 0.1;
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

        gameModels = new HashMap<>();
        for (Map.Entry<String, LinkedList<Turn>> entry : turnsByGame.entrySet()) {
            gameModels.put(entry.getKey(), getSVMModel(entry.getValue()));
        }

        engineModels = new HashMap<>();
        for (Map.Entry<String, LinkedList<Turn>> entry : turnsByEngine.entrySet()) {
            engineModels.put(entry.getKey(), getSVMModel(entry.getValue()));
        }

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

                HashMap<String, Double> foundW = new HashMap<>();
                HashMap<String, Double> foundB = new HashMap<>();
                for (Map.Entry<String, svm_model> entry: gameModels.entrySet()) {
                    if (check(testWInstance, entry.getValue())) {
                        foundW.put(entry.getKey(), 1.0);
                    }
                    if (check(testBInstance, entry.getValue())) {
                        foundB.put(entry.getKey(), 1.0);
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

                HashMap<String, Double> foundGameW = new HashMap<>();
                HashMap<String, Double> foundGameB = new HashMap<>();
                for (Map.Entry<String, svm_model> entry: gameModels.entrySet()) {
                    if (check(testWInstance, entry.getValue())) {
                        foundGameW.put(entry.getKey(), 1.0);
                    }
                    if (check(testBInstance, entry.getValue())) {
                        foundGameB.put(entry.getKey(), 1.0);
                    }
                }

                HashMap<String, Double> foundW = new HashMap<>();
                for (Map.Entry<String, Double> entry : foundGameW.entrySet()) {
                    if (entry.getValue() == 1) {
                        EngineController.EngineType[] types = new ChessEngineController.Type[0];
                        switch (GameType.getFromString(entry.getKey())) {
                            case Chess:
                                types = ChessEngineController.Type.values();
                                break;
                            case Checkers:
                                types = CheckerEngineController.Type.values();
                                break;
                            case ChineseChess:
                                types = ChineseChessEngineController.Type.values();
                                break;
                        }
                        for (EngineController.EngineType type : types) {
                            svm_model model = engineModels.get(type.name());
                            if (check(testWInstance, model)) {
                                foundW.put(type.name(), 1.0);
                            }
                        }
                    }
                }

                HashMap<String, Double> foundB = new HashMap<>();
                for (Map.Entry<String, Double> entry : foundGameB.entrySet()) {
                    if (entry.getValue() == 1) {
                        EngineController.EngineType[] types = new ChessEngineController.Type[0];
                        switch (GameType.getFromString(entry.getKey())) {
                            case Chess:
                                types = ChessEngineController.Type.values();
                                break;
                            case Checkers:
                                types = CheckerEngineController.Type.values();
                                break;
                            case ChineseChess:
                                types = ChineseChessEngineController.Type.values();
                                break;
                        }
                        for (EngineController.EngineType type : types) {
                            if (check(testBInstance, engineModels.get(type.name()))) {
                                foundB.put(type.name(), 1.0);
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

                //DebugLogger.writeMessage(this.getClass(), sug.toString());

                result.add(sug);
            }
        }
        return result;
    }

    private svm_model getSVMModel(List<Turn> turns) {
        /*https://sites.google.com/site/nirajatweb/home/technical_and_coding_stuff/binary-and-multi-class-classification-java-code*/
        int size = turns.size();
        double[][] node_values = new double[size][];    //jagged array used to store values
        int[][] node_indexes = new int[size][];         //jagged array used to store node indexes
        double[] node_class_labels = new double[size];  //store class labels

        //Now store data values
        int k = -1;
        for (Turn turn: turns) {
            node_class_labels[++k] = 1;

            LinkedList<Integer> list_index = new LinkedList<>();
            LinkedList<Double> list_val = new LinkedList<>();

            list_index.add(OWN_INPUT_INDEX);
            list_val.add(getDistance().getScore(turn.getInputsAsString()));

            //list_index.add(OPPONENT_INPUT_INDEX);
            //list_val.add(getDistance().getScore(turn.getOpponentInput()));

            list_index.add(OUTPUT_INDEX);
            list_val.add(getDistance().getScore(turn.getOutput()));

            if(list_val.size() > 0) {
                node_values[k] = new double[list_val.size()];
                node_indexes[k] = new int[list_index.size()];
            }

            for (int m = 0; m < list_val.size(); m++) {
                node_indexes[k][m] = list_index.get(m);
                node_values[k][m] = list_val.get(m);
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

    private boolean check(LinkedList<Turn> testInstance, svm_model model) {
        int size = testInstance.size();
        svm_node[] nodes = new svm_node[size * 3];
        int k = -1;

        for (Turn turn : testInstance) {
            svm_node node = new svm_node();
            node.index = OWN_INPUT_INDEX;
            node.value = getDistance().getScore(turn.getInputsAsString());
            nodes[++k] = node;

            //node = new svm_node();
            //node.index = OPPONENT_INPUT_INDEX;
            //node.value = getDistance().getScore(turn.getOpponentInput());
            //nodes[++k] = node;

            node = new svm_node();
            node.index = OUTPUT_INDEX;
            node.value = getDistance().getScore(turn.getOutput());
            nodes[++k] = node;
        }

        //double[] prop = new double[2];
        double pre = svm.svm_predict(model, nodes);
        return pre > 0;
    }

    private void plott(HashMap<String, LinkedList<Turn>> instances, String name) {
        Graphic graphic = new Graphic(name);
        Plotter plotter = graphic.getPlotter();

        plotter.setPreferredSize(new Dimension(500, 500));
        graphic.pack();

        double xHighest = Double.MIN_VALUE;
        double yHighest = Double.MIN_VALUE;
        double xLowest = Double.MAX_VALUE;
        double yLowest = Double.MAX_VALUE;
        for (Map.Entry<String, LinkedList<Turn>> entry : instances.entrySet()) {
            plotter.setXLine(0);
            plotter.setYLine(0);
            plotter.setDataLineStyle(LineStyle.DOT);
            plotter.setDataStroke(new BasicStroke(2));

            for (Turn turn : entry.getValue()) {
                String inputsAsString = turn.getInputsAsString();
                String output = turn.getOutput();
                double xScore = getDistance().getScore(inputsAsString);
                double yScore = getDistance().getScore(output);
                plotter.add(xScore, yScore);
                if (yScore > yHighest) {
                    yHighest = yScore;
                }
                if (xScore > xHighest) {
                    xHighest = xScore;
                }
                if (yScore < yLowest) {
                    yLowest = yScore;
                }
                if (xScore < xLowest) {
                    xLowest = xScore;
                }
            }
            plotter.nextDataSet();
        }
        plotter.setYrange(yLowest, yHighest);
        plotter.setXrange(xLowest, xHighest);
    }
}
