package de.uni_kassel.vs.datageneration;

import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.Collections;
import java.util.LinkedList;

public class WEKACombiner {

    public static void main(String[] args) throws IOException {
        File folder = new File("/Users/gatzi/Downloads/_log");

        File wekaFile = new File("/Users/gatzi/Downloads/weka.csv");
        BufferedWriter wekaWriter = new BufferedWriter(new FileWriter(wekaFile, true));

        wekaWriter.write("Game,Engine,Player,Input,Output\n");

        LinkedList<File> csvFiles = new LinkedList<>();
        for (File file : folder.listFiles()) {
            if (FilenameUtils.getExtension(file.getName()).contains("csv") && !file.getName().contains("weka")) {
                csvFiles.add(file);
            }
        }

        Collections.sort(csvFiles);
        for (File file: csvFiles) {
            String game = file.getName().substring(file.getName().lastIndexOf("]") + 1, file.getName().lastIndexOf("."));

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String newLine = game + "," + line + "\n";
                wekaWriter.write(newLine);
            }
        }

        wekaWriter.close();
    }
}
