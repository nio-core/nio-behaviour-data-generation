package de.uni_kassel.vs.datageneration.logger;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class DebugLogger {

    private static HelpFormatter helpFormatter;
    private static boolean debug;
    private static Options options;
    private static File debugLog;
    private static File resultLog;
    private static SimpleDateFormat sD = new SimpleDateFormat("[yyyy-MM-dd HH-mm-ss.SSS]");
    private static int id;

    public static void writeMessage(Class clazz, String msg) {
        if (debug) {
            StringBuilder output = new StringBuilder(sD.format(new Date()));
            output.append(" [").append(String.format("%04d", id)).append("]");
            output.append(" [INFO] [");
            output.append(clazz.getSimpleName());
            output.append("]: ");
            output.append(msg);
            System.out.println(output.toString());

            try {
                FileWriter writer = new FileWriter(debugLog, true);
                writer.append(output.toString()).append("\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeResult(Class clazz, String msg) {
        if (debug) {
            StringBuilder output = new StringBuilder(sD.format(new Date()));
            output.append(" [").append(String.format("%04d", id)).append("]");
            output.append(" [RESULT] [");
            output.append(clazz.getSimpleName());
            output.append("]: ");
            output.append(msg);
            System.out.println(output.toString());

            try {
                FileWriter writer = new FileWriter(resultLog, true);
                writer.append(output.toString()).append("\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeError(Class clazz, String msg) {
        if (debug) {
            StringBuilder output = new StringBuilder(sD.format(new Date()));
            output.append(" [").append(String.format("%04d", id)).append("]");
            output.append(" [ERROR] [");
            output.append(clazz.getSimpleName());
            output.append("]: ");
            output.append(msg);

            System.err.println(output.toString());
            helpFormatter.printHelp("help message", options);

            try {
                FileWriter writer = new FileWriter(debugLog, true);
                writer.append(output.toString()).append("\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeError(Class clazz, String msg, Throwable e) {
        if(debug) {
            StringBuilder output = new StringBuilder(sD.format(new Date()));
            output.append(" [").append(String.format("%04d", id)).append("]");
            output.append(" [ERROR] [");
            output.append(clazz.getSimpleName());
            output.append("]: ");
            output.append(msg);
            output.append(" - ");
            output.append((e.getMessage() == null) ? e.toString() : e.getMessage());


            System.err.println(output.toString());
            helpFormatter.printHelp("help message", options);

            try {
                FileWriter writer = new FileWriter(debugLog, true);
                writer.append(output.toString()).append("\n");
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void init(boolean debug, HelpFormatter helpFormatter, Options options) {
        DebugLogger.helpFormatter = helpFormatter;
        DebugLogger.debug = debug;
        DebugLogger.options = options;
        DebugLogger.debugLog = new File("app.log");
        DebugLogger.resultLog = new File("result.log");
        DebugLogger.id = new Random(System.currentTimeMillis()).nextInt(10000);
    }
}
