package ru.strategy48.ejudge.standings;

import ru.strategy48.ejudge.contest.Contest;
import ru.strategy48.ejudge.contest.Problem;
import ru.strategy48.ejudge.contest.Run;
import ru.strategy48.ejudge.contest.User;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StandingsTable {
    public final Contest contest;
    public final HashMap<Integer, StandingsTableRow> rows;
    public final StandingsTableConfig config;

    public final Set<Integer> userWithSubmissions = new HashSet<>();
    public final Map<Integer, Integer> virtualStarts = new HashMap<>();

    public List<StandingsTableRow> sortedRows = new ArrayList<>();
    public Map<Integer, Integer> userToRow = new HashMap<>();

    public Map<Integer, Integer> submittedRuns = new HashMap<>();
    public Map<Integer, Integer> acceptedRuns = new HashMap<>();

    public StandingsTable(final Contest contest, final StandingsTableConfig config) {
        this.contest = contest;
        this.config = config;
        rows = new HashMap<>(contest.getUsers().size());

        for (int i = 0; i < contest.getUsers().size(); i++) {
            rows.put(contest.getUsers().get(i).getId(), new StandingsTableRow(contest.getUsers().get(i), contest.getProblems()));
        }

        processRuns();
    }

    private void processRuns() {
        for (Run run : contest.getRuns()) {
            int time = run.getTime() - virtualStarts.getOrDefault(run.getUserId(), 0);

            boolean wasFreezed = false;
            if (run.getProblemId() != -1 && run.getUserId() != -1) {
                wasFreezed = rows.get(run.getUserId()).cells.get(run.getProblemId()).freezed;
            }

            boolean nowFreezed = config.needFreeze && contest.needFreeze(time);

            switch (run.getStatus()) {
                case VS:
                    virtualStarts.put(run.getUserId(), run.getTime());
                    break;
                case CE:
                case CF:
                case IG:
                case DQ:
                case SK:
                case EM:
                case VT:
                    break;
                case RU:
                case CD:
                case CG:
                case AV:
                    userWithSubmissions.add(run.getUserId());
                    rows.get(run.getUserId()).cells.get(run.getProblemId()).running = true;
                    break;
                case OK:
                    userWithSubmissions.add(run.getUserId());
                    rows.get(run.getUserId()).cells.get(run.getProblemId()).freezed = nowFreezed;

                    if (!wasFreezed && nowFreezed) {
                        rows.get(run.getUserId()).cells.get(run.getProblemId()).makeFreezed();
                    }

                    rows.get(run.getUserId()).cells.get(run.getProblemId()).solved = true;
                    rows.get(run.getUserId()).cells.get(run.getProblemId()).score = run.getScore();
                    rows.get(run.getUserId()).cells.get(run.getProblemId()).time = time;
                    rows.get(run.getUserId()).cells.get(run.getProblemId()).running = false;

                    if (!nowFreezed) {
                        submittedRuns.put(run.getProblemId(), submittedRuns.getOrDefault(run.getProblemId(), 0) + 1);
                        acceptedRuns.put(run.getProblemId(), acceptedRuns.getOrDefault(run.getProblemId(), 0) + 1);
                    }
                    break;
                default:
                    userWithSubmissions.add(run.getUserId());
                    rows.get(run.getUserId()).cells.get(run.getProblemId()).freezed = nowFreezed;

                    if (!wasFreezed && nowFreezed) {
                        rows.get(run.getUserId()).cells.get(run.getProblemId()).makeFreezed();
                    }

                    if (config.lastACProblems.get(contest.getContestId()).contains(run.getProblemId())) {
                        rows.get(run.getUserId()).cells.get(run.getProblemId()).solved = false;
                        rows.get(run.getUserId()).cells.get(run.getProblemId()).score = run.getScore();
                        rows.get(run.getUserId()).cells.get(run.getProblemId()).attempts++;
                        rows.get(run.getUserId()).cells.get(run.getProblemId()).time = time;
                    } else {
                        int curScore = rows.get(run.getUserId()).cells.get(run.getProblemId()).score;
                        rows.get(run.getUserId()).cells.get(run.getProblemId()).score = Math.max(curScore, run.getScore());

                        if (!rows.get(run.getUserId()).cells.get(run.getProblemId()).solved) {
                            rows.get(run.getUserId()).cells.get(run.getProblemId()).attempts++;
                            rows.get(run.getUserId()).cells.get(run.getProblemId()).time = time;
                        }
                    }

                    rows.get(run.getUserId()).cells.get(run.getProblemId()).running = false;
                    if (!nowFreezed) {
                        submittedRuns.put(run.getProblemId(), submittedRuns.getOrDefault(run.getProblemId(), 0) + 1);
                    }
                    break;
            }
        }

        for (Problem problem : contest.getProblems()) {
            int bestId = -1;
            int bestTime = -1;

            for (User user : contest.getUsers()) {
                if (rows.get(user.getId()).cells.get(problem.getId()).solved) {
                    int curTime = rows.get(user.getId()).cells.get(problem.getId()).time;

                    if (bestId == -1 || curTime < bestTime) {
                        bestId = user.getId();
                        bestTime = curTime;
                    }
                }
            }

            if (bestId != -1 && config.showFirstAC) {
                rows.get(bestId).cells.get(problem.getId()).firstAC = true;
            }
        }

        Stream<StandingsTableRow> rowStream = rows.values().stream();
        if (!config.showZeros) {
            rowStream = rowStream.filter(row -> (config.type == StandingsTableType.ICPC ? row.getSolvedCnt() : row.getScore()) > 0);
        }
        if (!config.showUsersWithoutRuns) {
            rowStream = rowStream.filter(row -> userWithSubmissions.contains(row.user.getId()));
        }

//        Comparator<StandingsTableRow> comparator;
//        if (config.type == StandingsTableType.ICPC) {
//            comparator = Comparator.comparingInt(StandingsTableRow::getSolvedCnt).thenComparing(StandingsTableRow::getPenalty, Comparator.reverseOrder()).thenComparing(StandingsTableRow::getName);
//        } else {
//            comparator = Comparator.comparingInt(StandingsTableRow::getScore).thenComparing(StandingsTableRow::getName);
//        }

        sortedRows = rowStream.collect(Collectors.toList());
//        sortedRows = rowStream.sorted(comparator).collect(Collectors.toList());

        for (int i = 0; i < sortedRows.size(); i++) {
            userToRow.put(sortedRows.get(i).user.getId(), i);
        }
    }
}
