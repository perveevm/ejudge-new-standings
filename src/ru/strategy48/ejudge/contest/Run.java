package ru.strategy48.ejudge.contest;

public class Run {
    private final int id;
    private final int time;
    private final Status status;
    private final int userId;
    private final int problemId;
    private final int score;

    public Run(final int id, final int time, final Status status, final int userId, final int problemId, final int score) {
        this.id = id;
        this.time = time;
        this.status = status;
        this.userId = userId;
        this.problemId = problemId;
        this.score = score;
    }

    public int getId() {
        return id;
    }

    public int getTime() {
        return time;
    }

    public Status getStatus() {
        return status;
    }

    public int getUserId() {
        return userId;
    }

    public int getProblemId() {
        return problemId;
    }

    public int getScore() {
        return score;
    }
}
