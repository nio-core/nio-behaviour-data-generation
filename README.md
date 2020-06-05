# Behaviour Data Generation
Repository for behaviour data generation by utilizing chessboard-like games. The extracted skills are to be used for a change management system. 

## Part 1: Generate the data set
Two engines play several games against each other and thus produce data for learning the classifiers.

### Chess Engines
- pulse	            https://github.com/fluxroot/pulse
- stockfish         https://github.com/mcostalba/Stockfish
- laser 	        https://github.com/jeffreyan11/uci-chess-engine
- fruit-reloaded    https://www.chessprogramming.net/fruit-reloaded/

### Checker Engine 
- Ponder	https://github.com/neo954/checkers

### Chinese Chess Engines 
- ElephantEye (eleeye)  https://github.com/xqbase/eleeye
- mars  https://github.com/yytdfc/ChineseChess-engines/tree/master/mars
- (UCCI Chess Engines) https://github.com/yytdfc/ChineseChess-engines

#### Usage
```Shell
$ ./gradlew EngineApplication:run --args="--help"
usage: help message
 -?,--help                prints this message
 -b,--engineBlack <arg>   engine that plays as black:
                          Chess: [pulse, stockfish, laser, fruitReloaded]
                          Checkers: [ponder]
                          ChineseChess: [eleeye, mars]
 -d,--debug               (optional) enables debug output
 -g,--game <arg>          select type of game:
                          [Chess, Checkers, ChineseChess]
 -i,--initBoard <arg>     (optional) start board config as fen string,
                          when not set then use normal start
 -l,--log <arg>           (optional) enables game log output [filePath]
 -w,--engineWhite <arg>   engine that plays as white:
                          Chess: [pulse, stockfish, laser, fruitReloaded]
                          Checkers: [ponder]
                          ChineseChess: [eleeye, mars]
```

## Part 2: First experiments to learning and testing classifiers
Classifiers are learned with training data and output results as test data.

### DistributionClassifier
This classifier works with the frequency distribution of the played moves. Opening moves are the most common.

### <a name="OneLabelSVMClassifier"></a> OneLabelSVMClassifier
This classifier learns a separate SVM for each game and each engine.  Subsequently, it checks first which game it could be and then which engine. By selecting the game first, only the engines of the respective game type are checked.

### MultiLabelSVMClassifier
This classifier learns one SVM model for all game and one model for all engines and gives the probability for each class.

### SVMModelClassifier
This classifier learns a separate SVM for each game and engine. In addition, a model is generated from each test instance, which is then compared with the other models. The model is not used for prediction, but 2 models are compared with each other.

#### Usage
```Shell
$ ./gradlew ClassifierApplication:run --args="--help"
usage: help message
 -?,--help               prints this message
 -c,--classifier <arg>   classifier to use:
                         [OneLabelSVMClassifier, DistributionClassifier,
                         MultiLabelSVMClassifier, ModelSVMClassifier]
 -d,--debug              (optional) enables debug output
 -l,--log <arg>          path to log folder
 -t,--test <arg>         path to test folder
```
