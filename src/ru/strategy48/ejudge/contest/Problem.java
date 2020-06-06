package ru.strategy48.ejudge.contest;

public class Problem {
    private final int id;
    private final String shortName;
    private final String longName;

    public Problem(final int id, final String shortName, final String longName) {
        this.id = id;
        this.shortName = shortName;
        this.longName = longName;
    }

    public int getId() {
        return id;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }
}
