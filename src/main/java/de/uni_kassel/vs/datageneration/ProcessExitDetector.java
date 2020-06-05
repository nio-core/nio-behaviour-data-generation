package de.uni_kassel.vs.datageneration;

public class ProcessExitDetector extends Thread {

    private final Process process;
    private final Listener listener;

    public ProcessExitDetector(Process process, Listener listener) {
        this.process = process;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            process.waitFor();
            listener.processExited();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface Listener {
        void processExited();
    }
}
