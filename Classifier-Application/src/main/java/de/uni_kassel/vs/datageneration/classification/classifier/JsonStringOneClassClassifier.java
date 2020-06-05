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
import de.uni_kassel.vs.datageneration.engines.EngineController.EngineType;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class JsonStringOneClassClassifier extends StringClassifier {

    private HashMap<String, svm_model> gameModels;
    private HashMap<String, svm_model> engineModels;

    public JsonStringOneClassClassifier(Distance distance, svm_parameter parameter) {
        super(distance, parameter == null ? getSTDParameter() : parameter);
    }

    @Override
    public void learn(List<Turn> turns) {
        HashMap<String, LinkedList<String>> turnsByEngine = new HashMap<>();
        HashMap<String, LinkedList<String>> turnsByGame = new HashMap<>();

        learnCounter = turns.size();
        for (Turn turn : turns) {
            LinkedList<String> jsonGameList = turnsByGame.computeIfAbsent(turn.getGame(), k -> new LinkedList<>());
            LinkedList<String> jsonEngineList = turnsByEngine.computeIfAbsent(turn.getEngine(), k -> new LinkedList<>());
            jsonEngineList.add(new Gson().toJson(turn));
            jsonGameList.add(new Gson().toJson(turn));
        }

        gameModels = new HashMap<>();
        for (Entry<String, LinkedList<String>> entry : turnsByGame.entrySet()) {
            gameModels.put(entry.getKey(), getSVMModel(entry.getValue()));
        }

        engineModels = new HashMap<>();
        for (Entry<String, LinkedList<String>> entry : turnsByEngine.entrySet()) {
            engineModels.put(entry.getKey(), getSVMModel(entry.getValue()));
        }
    }

    @Override
    public String checkByEngine(Turn turn) {
        return null;
    }

    @Override
    public String checkByGame(Turn turn) {
        return null;
    }

    private static svm_parameter getSTDParameter() {
        //std params
        svm_parameter param = new svm_parameter();
        param.probability = 1;
        param.gamma = 0.55;
        param.nu = 0.5;
        param.C = 2;
        param.svm_type = svm_parameter.ONE_CLASS;
        param.kernel_type = svm_parameter.LINEAR;
        param.cache_size = 20000;
        param.eps = 0.00001;

        // svm_parameter param = new svm_parameter();
        // param.probability = 1;
        // param.gamma = 0.5;
        // param.nu = 0.1;
        // param.C = 2;
        // param.svm_type = svm_parameter.ONE_CLASS;
        // param.kernel_type = svm_parameter.RBF;
        // param.cache_size = 20000;
        // param.eps = 0.001;

        return param;
    }


    @Override
    public void learn(File folder) throws IOException {
        File jsonFolder = new File(folder.getAbsolutePath() + File.separatorChar + "json");

        HashMap<String, LinkedList<String>> turnsByEngine = new HashMap<>();
        HashMap<String, LinkedList<String>> turnsByGame = new HashMap<>();

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
                }
            }
        }

        gameModels = new HashMap<>();
        for (Entry<String, LinkedList<String>> entry : turnsByGame.entrySet()) {
            gameModels.put(entry.getKey(), getSVMModel(entry.getValue()));
        }

        engineModels = new HashMap<>();
        for (Entry<String, LinkedList<String>> entry : turnsByEngine.entrySet()) {
            engineModels.put(entry.getKey(), getSVMModel(entry.getValue()));
        }

        //plott(turnsByEngine, "EngineController");
        //plott(turnsByGame, "Game");
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

                HashMap<String, Double> foundW = new HashMap<>();
                HashMap<String, Double> foundB = new HashMap<>();
                for (Entry<String, svm_model> entry: gameModels.entrySet()) {
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

                String engineW = null;
                String engineB = null;
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    Turn turn = gson.fromJson(line, Turn.class);
                    if (turn.getColor().toLowerCase().equals("w")) {
                        engineW = turn.getEngine();
                        testWInstance.add(line);
                    } else {
                        engineB = turn.getEngine();
                        testBInstance.add(line);
                    }
                }

                HashMap<String, Double> foundGameW = new HashMap<>();
                HashMap<String, Double> foundGameB = new HashMap<>();
                for (Entry<String, svm_model> entry: gameModels.entrySet()) {
                    if (check(testWInstance, entry.getValue())) {
                        foundGameW.put(entry.getKey(), 1.0);
                    }
                    if (check(testBInstance, entry.getValue())) {
                        foundGameB.put(entry.getKey(), 1.0);
                    }
                }

                HashMap<String, Double> foundW = new HashMap<>();
                for (Entry<String, Double> entry : foundGameW.entrySet()) {
                    if (entry.getValue() == 1) {
                        EngineType[] types = new ChessEngineController.Type[0];
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
                        for (EngineType type : types) {
                            svm_model model = engineModels.get(type.name());
                            if (check(testWInstance, model)) {
                                foundW.put(type.name(), 1.0);
                            }
                        }
                    }
                }

                HashMap<String, Double> foundB = new HashMap<>();
                for (Entry<String, Double> entry : foundGameB.entrySet()) {
                    if (entry.getValue() == 1) {
                        EngineType[] types = new ChessEngineController.Type[0];
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
                        for (EngineType type : types) {
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

                DebugLogger.writeMessage(this.getClass(), sug.toString());

                result.add(sug);
            }
        }
        return result;
    }

    private boolean check(List<String> testInstance, svm_model model) {
        svm_node[] nodes = new svm_node[testInstance.size()];
        int k = -1;
        for (String turn : testInstance) {
            svm_node node = new svm_node();
            node.index = 1;
            node.value = getDistance().getScore(turn);
            nodes[++k] = node;
        }
        double pre = svm.svm_predict(model, nodes);
        return pre > 0;
    }

    private svm_model getSVMModel(List<String> jsonTurns) {
        /*https://sites.google.com/site/nirajatweb/home/technical_and_coding_stuff/binary-and-multi-class-classification-java-code*/
        int size = jsonTurns.size();
        double node_values[] = new double[size];    //jagged array used to store values
        int node_indexes[] = new int[size];         //jagged array used to store node indexes
        double node_class_labels[] = new double[size];  //store class labels

        //Now store data values
        int k = -1;
        for (String turn: jsonTurns) {
            node_class_labels[++k] = 1;

            node_values[k] = getDistance().getScore(turn);
            node_indexes[k] = 1;
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

        return svm.svm_train(prob, param);
    }

    private void plott(HashMap<String, LinkedList<String>> instances, String name) {
        Graphic graphic = new Graphic(name);
        Plotter plotter = graphic.getPlotter();

        plotter.setPreferredSize(new Dimension(500, 500));
        graphic.pack();

        int k = 1;
        double highest = 0;
        for (Entry<String, LinkedList<String>> entry : instances.entrySet()) {
            plotter.setXLine(0);
            plotter.setYLine(0);
            plotter.setDataLineStyle(LineStyle.DOT);
            plotter.setDataStroke(new BasicStroke(2));

            for (String turn : entry.getValue()) {
                double score = getDistance().getScore(turn) - 400;
                plotter.add(k, score);
                if (score > highest) {
                    highest = score;
                }
            }
            plotter.nextDataSet();
            k++;
        }
        plotter.setYrange(0, highest);
        plotter.setXrange(0, instances.size() + 1);
    }
}
