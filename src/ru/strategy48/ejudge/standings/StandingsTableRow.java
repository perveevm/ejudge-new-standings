package ru.strategy48.ejudge.standings;

import ru.strategy48.ejudge.contest.Problem;
import ru.strategy48.ejudge.contest.User;

import java.util.*;

public class StandingsTableRow {
    public final LinkedHashMap<Integer, StandingsTableCell> cells;
    public final User user;
    public final List<Problem> problems;

    public StandingsTableRow(final User user, final List<Problem> problems) {
        cells = new LinkedHashMap(problems.size());
        this.user = user;
        this.problems = problems;

        for (int i = 0; i < problems.size(); i++) {
            cells.put(problems.get(i).getId(), new StandingsTableCell(problems.get(i)));
        }
    }

    public int getSolvedCnt() {
        int cnt = 0;
        for (StandingsTableCell cell : cells.values()) {
            if (cell.freezed && cell.freezedSolved) {
                cnt++;
            }
            if (!cell.freezed && cell.solved) {
                cnt++;
            }
        }
        return cnt;
    }

    public int getPenalty() {
        int penalty = 0;
        for (StandingsTableCell cell : cells.values()) {
            penalty += cell.getPenalty();
        }
        return penalty;
    }

    public int getScore() {
        int score = 0;
        for (StandingsTableCell cell : cells.values()) {
            score += cell.freezed ? cell.freezedScore : cell.score;
        }
        return score;
    }

    public String getName() {
        return user.getName();
    }

    public int getUserId() {
        return user.getId();
    }
}
