package ru.strategy48.ejudge.standings;

import ru.strategy48.ejudge.contest.Problem;

public class StandingsTableCell {
    public boolean solved = false;
    public boolean freezed = false;
    public boolean firstAC = false;
    public boolean running = false;

    public int score = 0;
    public int time = -1;
    public int attempts = 0;

    public boolean freezedSolved = false;
    public int freezedScore = 0;
    public int freezedTime = -1;
    public int freezedAttempts = 0;

    public final Problem problem;

    public StandingsTableCell(final Problem problem) {
        this.problem = problem;
    }

    public int getPenalty() {
        if (freezed) {
            if (!freezedSolved) {
                return 0;
            } else {
                return freezedTime / 60 + 20 * freezedAttempts;
            }
        } else {
            if (!solved) {
                return 0;
            }
            return time / 60 + 20 * attempts;
        }
    }

    public void makeFreezed() {
        freezedScore = score;
        freezedTime = time;
        freezedAttempts = attempts;
        freezedSolved = solved;
    }

    public boolean isUnusual() {
        return solved || freezed || firstAC || running;
    }
}
