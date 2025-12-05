package ru.strategy48.ejudge.contest;

public class User {
    private final int id;
    private final String name;
    private boolean disqualified;

    public User(final int id, final String name) {
        this.id = id;
        this.name = name;
        this.disqualified = false;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean getDisqualified() {
        return disqualified;
    }

    public void setDisqualified() {
        disqualified = true;
    }
}
