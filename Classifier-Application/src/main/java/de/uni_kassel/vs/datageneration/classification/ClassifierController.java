package de.uni_kassel.vs.datageneration.classification;

import de.uni_kassel.vs.datageneration.classification.Suggestion.Type;
import de.uni_kassel.vs.datageneration.IController;
import de.uni_kassel.vs.datageneration.classification.distances.Distance;
import de.uni_kassel.vs.datageneration.logger.DebugLogger;
import libsvm.svm_parameter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class ClassifierController implements IController {

    private String packageName = "de.uni_kassel.vs.datageneration.classification.classifier.";

    public void start(File logFolder, File logTestFolder, String classifierClass, Boolean random) {
        List<Classifier> classifiers = new ArrayList<>();
        try {
            Class<Classifier> clazz = (Class<Classifier>) Class.forName(packageName + classifierClass);
            if (StringClassifier.class.isAssignableFrom(clazz)) {
                String randomString = Distance.getRandomString();
                if (random) {
                    DebugLogger.writeResult(this.getClass(), "Random string: " + randomString);
                }
                for (Class<? extends Distance> distance: Distance.getClasses()) {
                    Distance distInstance;
                    if (random) {
                        Constructor<? extends Distance> ctor = distance.getConstructor(String.class);
                        distInstance = ctor.newInstance(randomString);
                    } else {
                        distInstance = distance.newInstance();
                    }
                    Constructor<Classifier> ctor = clazz.getConstructor(Distance.class, svm_parameter.class);
                    classifiers.add(ctor.newInstance(distInstance, null));
                }
            } else {
                Constructor<Classifier> ctor = clazz.getConstructor();
                classifiers.add(ctor.newInstance());
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            DebugLogger.writeError(this.getClass(), "Error creating Classifier " + classifierClass, e);
            quitWithError();
        }


        for (Classifier classifier : classifiers) {
            try {
                classifier.learn(logFolder);
            } catch (IOException e) {
                DebugLogger.writeError(this.getClass(), "Error learning Classifier " + classifierClass, e);
                quitWithError();
            }

            try {
                List<Suggestion> bestInstancesByGame = classifier.checkByGame(logTestFolder);
            } catch (Exception e) {
                DebugLogger.writeError(this.getClass(), "Error finding best instances for engine with Classifier " + classifierClass, e);
                quitWithError();
            }

            try {
                List<Suggestion> bestInstancesByEngine = classifier.checkByEngine(logTestFolder);
            } catch (IOException e) {
                DebugLogger.writeError(this.getClass(), "Error finding best instances for game with Classifier " + classifierClass, e);
                quitWithError();
            }

            classifier.printStatistic(Type.Game);
            classifier.printStatistic(Type.Engine);
        }
    }

    @Override
    public void quitWithError() {
        System.exit(1);
    }
}
