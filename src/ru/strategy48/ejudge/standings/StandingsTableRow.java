package ru.strategy48.ejudge.standings;

import ru.strategy48.ejudge.contest.Problem;
import ru.strategy48.ejudge.contest.User;

import java.util.*;

public class StandingsTableRow {
    public final LinkedHashMap<Integer, StandingsTableCell> cells;
    public final User user;
//    public final List<Problem> problems;
    public final Map<Integer, Problem> problems = new LinkedHashMap<>();
    public final int maxCount;

    public StandingsTableRow(final User user, final List<Problem> problems, final int maxCount) {
        cells = new LinkedHashMap<>();
        this.user = user;
        for (Problem problem : problems) {
            this.problems.put(problem.getId(), problem);
        }
//        this.problems = problems;
        this.maxCount = maxCount;

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
        List<Integer> scores = new ArrayList<>();
        for (StandingsTableCell cell : cells.values()) {
            scores.add(cell.freezed ? cell.freezedScore : cell.score);
        }
        scores.sort((s1, s2) -> -Integer.compare(s1, s2));

        int score = 0;
        if (maxCount == -1) {
            for (Integer curScore : scores) {
                score += curScore;
            }
        } else {
            for (int i = 0; i < Math.min(maxCount, scores.size()); i++) {
                score += scores.get(i);
            }
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
