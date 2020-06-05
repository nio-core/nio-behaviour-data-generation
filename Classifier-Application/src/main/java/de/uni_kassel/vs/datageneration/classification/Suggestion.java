package de.uni_kassel.vs.datageneration.classification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class Suggestion {

    public enum Type {
        Game,
        Engine;
    }

    private final String fileName;
    private final Type type;

    private String whiteReal;
    private String whiteSug;

    private String blackReal;
    private String blackSug;

    private HashMap<String, Double> whiteScore;
    private HashMap<String, Double> blackScore;

    public Suggestion(String fileName, Type type) {
        this.fileName = fileName;
        this.type = type;
    }

    public String getFile() {
        return fileName;
    }

    public Type getType() {
        return type;
    }

    public String getWhiteReal() {
        return whiteReal;
    }

    public void setWhiteReal(String whiteReal) {
        this.whiteReal = whiteReal;
    }

    public String getWhiteSug() {
        return whiteSug;
    }

    public void setWhiteSug(String whiteSug) {
        this.whiteSug = whiteSug;
    }

    public String getBlackReal() {
        return blackReal;
    }

    public void setBlackReal(String blackReal) {
        this.blackReal = blackReal;
    }

    public String getBlackSug() {
        return blackSug;
    }

    public void setBlackSug(String blackSug) {
        this.blackSug = blackSug;
    }

    public HashMap<String, Double> getWhiteScore() {
        return whiteScore;
    }

    public void setWhiteScore(HashMap<String, Double> whiteScore) {
        this.whiteScore = whiteScore;
    }

    public HashMap<String, Double> getBlackScore() {
        return blackScore;
    }

    public void setBlackScore(HashMap<String, Double> blackScore) {
        this.blackScore = blackScore;
    }

    public void setBlackScoreAndSug(HashMap<String, Double> calcScores) {
        setBlackScore(calcScores);
        Entry<String, Double> best = null;
        for (Entry<String, Double> entry : calcScores.entrySet()) {
            if (best == null) {
                best = entry;
            } else if (entry.getValue() > best.getValue()) {
                best = entry;
            }
        }
        if (best == null) {
            setBlackSug(null);
        } else {
            setBlackSug(best.getKey());
        }
    }

    public void setWhiteScoreAndSug(HashMap<String, Double> calcScores) {
        setWhiteScore(calcScores);
        Entry<String, Double> best = null;
        for (Entry<String, Double> entry : calcScores.entrySet()) {
            if (best == null) {
                best = entry;
            } else if (entry.getValue() > best.getValue()) {
                best = entry;
            }
        }
        if (best == null) {
            setWhiteSug(null);
        } else {
            setWhiteSug(best.getKey());
        }
    }


    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("File \"");
        b.append(getFile());
        b.append("\"");
        switch (getType()) {
            case Game: {
                b.append(" Game [");
                b.append(getWhiteReal());
                b.append("]->{White[");
                List<Entry<String, Double>> list = new ArrayList<>(whiteScore.entrySet());
                list.sort(Entry.comparingByValue());
                Collections.reverse(list);
                for (Entry<String, Double> entry : list) {
                    b.append(entry.getKey());
                    b.append("(");
                    b.append(String.format("%02.3f", entry.getValue() * 100));
                    b.append("%");
                    b.append("),");
                }
                if (!whiteScore.isEmpty()) {
                    b.deleteCharAt(b.lastIndexOf(","));
                }
                b.append("], Black[");
                list = new ArrayList<>(blackScore.entrySet());
                list.sort(Entry.comparingByValue());
                Collections.reverse(list);
                for (Entry<String, Double> entry : list) {
                    b.append(entry.getKey());
                    b.append("(");
                    b.append(String.format("%02.3f", entry.getValue() * 100));
                    b.append("%");
                    b.append("),");
                }
                if (!blackScore.isEmpty()) {
                    b.deleteCharAt(b.lastIndexOf(","));
                }
                b.append("]}");
                return b.toString();
            }
            case Engine: {
                b.append(" White[");
                b.append(getWhiteReal());
                b.append("]->{");
                List<Entry<String, Double>> list = new ArrayList<>(whiteScore.entrySet());
                list.sort(Entry.comparingByValue());
                Collections.reverse(list);
                for (Entry<String, Double> entry : list) {
                    b.append(entry.getKey());
                    b.append("(");
                    b.append(String.format("%02.3f", entry.getValue() * 100));
                    b.append("%");
                    b.append("),");
                }
                if (!whiteScore.isEmpty()) {
                    b.deleteCharAt(b.lastIndexOf(","));
                }
                b.append("} || Black[");
                b.append(getBlackReal());
                b.append("]->{");
                list = new ArrayList<>(blackScore.entrySet());
                list.sort(Entry.comparingByValue());
                Collections.reverse(list);
                for (Entry<String, Double> entry : list) {
                    b.append(entry.getKey());
                    b.append("(");
                    b.append(String.format("%02.3f", entry.getValue() * 100));
                    b.append("%");
                    b.append("),");
                }
                if (!blackScore.isEmpty()) {
                    b.deleteCharAt(b.lastIndexOf(","));
                }
                b.append("}");
                return b.toString();
            }
        }
        return "Error";
    }
}
