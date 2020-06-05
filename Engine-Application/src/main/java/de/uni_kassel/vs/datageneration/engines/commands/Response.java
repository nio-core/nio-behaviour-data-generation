package de.uni_kassel.vs.datageneration.engines.commands;

public class Response {

    private final String from;
    private final String to;

    private final boolean giveUp;

    Response(String from, String to) {
        this.from = from.trim();
        this.to = to.trim();
        giveUp = false;
    }

    Response(boolean giveUp) {
        this.giveUp = giveUp;
        from = null;
        to = null;
    }

    public boolean isGiveUp() {
        return giveUp;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }


    @Override
    public String toString() {
        if (giveUp) {
            return "nomove";
        } else {
            return getFrom() + getTo();
        }
    }

    public boolean isRevert(Response rev) {
        return this.getFrom().equals(rev.getTo()) && this.getTo().equals(rev.getFrom());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Response)) {
            return false;
        }

        boolean equals = this.toString().equals(o.toString());
        return equals;
    }
}
