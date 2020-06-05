package de.uni_kassel.vs.datageneration.classifictation.test;

import com.google.gson.Gson;
import de.uni_kassel.vs.datageneration.classification.classifier.StringInputOutputMULTISVMClassifier;
import de.uni_kassel.vs.datageneration.classification.classifier.StringInputOutputONESVMClassifier;
import de.uni_kassel.vs.datageneration.classification.distances.Distance;
import de.uni_kassel.vs.datageneration.classification.distances.JaroWinkler;
import de.uni_kassel.vs.datageneration.classification.instances.Turn;
import libsvm.svm_parameter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Random;

public class StringClassifierTest {

    @Test
    public void testClassifier() throws IOException {
        Distance distance = new JaroWinkler("testString");

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

        LinkedList<Turn> turns = new LinkedList<>();
        Turn turn = new Gson().fromJson("{\"game\":\"Checkers\",\"engine\":\"ponder\",\"color\":\"w\",\"inputs\":[],\"output\":\"c3b4\"}", Turn.class);
        turns.add(turn);

        StringInputOutputMULTISVMClassifier classifier = new StringInputOutputMULTISVMClassifier(distance, param);
        classifier.learn(turns);
        System.out.println(classifier.checkByEngine(turn));
        System.out.println(classifier.checkByGame(turn));
    }

    private String randomString(int length) {
        byte[] array = new byte[length];
        new Random().nextBytes(array);
        return new String(array, Charset.forName("UTF-8"));
    }

}
