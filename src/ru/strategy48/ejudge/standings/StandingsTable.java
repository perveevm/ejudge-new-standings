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

    public final Map<Integer, Integer> idMatching;

    public int submittedCnt = 0;
    public int acceptedCnt = 0;

    public StandingsTable(final Contest contest, final StandingsTableConfig config, final Map<Integer, Integer> idMatching) {
        this.contest = contest;
        this.config = config;
        this.idMatching = idMatching;
        rows = new HashMap<>(contest.getUsers().size());

        System.out.println("Getting users...");
        if (this.idMatching != null) {
            System.out.println(this.idMatching.size());
        }

        for (int i = 0; i < contest.getUsers().size(); i++) {
            int primaryID = contest.getUsers().get(i).getId();
            if (this.idMatching != null) {
                primaryID = this.idMatching.getOrDefault(primaryID, primaryID);
            }

            rows.put(primaryID, new StandingsTableRow(contest.getUsers().get(i), contest.getProblems(), config.maxCountJudge));
        }

        System.out.println("Processing runs...");

        processRuns();
    }

    private void processRuns() {
        for (Run run : contest.getRuns()) {
            int userId = run.getUserId();
            if (this.idMatching != null && userId != -1) {
                userId = this.idMatching.getOrDefault(userId, userId);
            }

            if (config.ignoreProblems.getOrDefault(contest.getContestId(), Collections.emptySet()).contains(run.getProblemId())) {
                continue;
            }
            if (!rows.get(userId).cells.containsKey(run.getProblemId()) || !rows.get(userId).problems.containsKey(run.getProblemId())) {
                continue;
            }

            int time = run.getTime() - virtualStarts.getOrDefault(userId, 0);

            boolean wasFreezed = false;
            if (run.getProblemId() != -1 && userId != -1) {
                wasFreezed = rows.get(userId).cells.get(run.getProblemId()).freezed;
            }

            boolean nowFreezed = config.needFreeze && contest.needFreeze(time);

            switch (run.getStatus()) {
                case VS:
                    virtualStarts.put(userId, run.getTime());
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
                    userWithSubmissions.add(userId);
                    rows.get(userId).cells.get(run.getProblemId()).running = true;
                    break;
                case OK:
                    userWithSubmissions.add(userId);
                    rows.get(userId).cells.get(run.getProblemId()).freezed = nowFreezed;

                    if (!wasFreezed && nowFreezed) {
                        rows.get(userId).cells.get(run.getProblemId()).makeFreezed();
                    }

                    boolean wasSolved = rows.get(userId).cells.get(run.getProblemId()).solved;
                    rows.get(userId).cells.get(run.getProblemId()).solved = true;
                    rows.get(userId).cells.get(run.getProblemId()).score = run.getScore();
                    if (!wasSolved) {
                        rows.get(userId).cells.get(run.getProblemId()).time = time;
                    }
                    rows.get(userId).cells.get(run.getProblemId()).running = false;

                    if (!nowFreezed) {
                        submittedRuns.put(run.getProblemId(), submittedRuns.getOrDefault(run.getProblemId(), 0) + 1);
                        acceptedRuns.put(run.getProblemId(), acceptedRuns.getOrDefault(run.getProblemId(), 0) + 1);
                        submittedCnt++;
                        acceptedCnt++;
                    }
                    break;
                default:
                    userWithSubmissions.add(userId);
                    rows.get(userId).cells.get(run.getProblemId()).freezed = nowFreezed;

                    if (!wasFreezed && nowFreezed) {
                        rows.get(userId).cells.get(run.getProblemId()).makeFreezed();
                    }

                    if (config.lastACProblems.get(contest.getContestId()).contains(run.getProblemId())) {
                        rows.get(userId).cells.get(run.getProblemId()).solved = false;
                        rows.get(userId).cells.get(run.getProblemId()).score = run.getScore();
                        rows.get(userId).cells.get(run.getProblemId()).attempts++;
                        rows.get(userId).cells.get(run.getProblemId()).time = time;
                    } else if (!rows.get(userId).cells.get(run.getProblemId()).solved) {
                        int curScore = rows.get(userId).cells.get(run.getProblemId()).score;
                        rows.get(userId).cells.get(run.getProblemId()).score = Math.max(curScore, run.getScore());

                        if (!rows.get(userId).cells.get(run.getProblemId()).solved) {
                            rows.get(userId).cells.get(run.getProblemId()).attempts++;
                            rows.get(userId).cells.get(run.getProblemId()).time = time;
                        }
                    }

                    rows.get(userId).cells.get(run.getProblemId()).running = false;
                    if (!nowFreezed) {
                        submittedRuns.put(run.getProblemId(), submittedRuns.getOrDefault(run.getProblemId(), 0) + 1);
                        submittedCnt++;
                    }
                    break;
            }
        }

        for (Problem problem : contest.getProblems()) {
            int bestId = -1;
            int bestTime = -1;

            for (User user : contest.getUsers()) {
                int userId = user.getId();
                if (this.idMatching != null) {
                    userId = this.idMatching.getOrDefault(userId, userId);
                }

                if (rows.get(userId).cells.get(problem.getId()).solved) {
                    int curTime = rows.get(userId).cells.get(problem.getId()).time;

                    if (bestId == -1 || curTime < bestTime) {
                        bestId = userId;
                        bestTime = curTime;
                    }
                }
            }

            if (bestId != -1 && config.showFirstAC) {
                rows.get(bestId).cells.get(problem.getId()).firstAC = true;
            }
        }

        System.out.println("Filtering...");

        Stream<StandingsTableRow> rowStream = rows.values().stream();
        if (!config.showZeros) {
            rowStream = rowStream.filter(row -> (config.type == StandingsTableType.ICPC ? row.getSolvedCnt() : row.getScore()) > 0);
        }
        if (!config.showUsersWithoutRuns) {
            rowStream = rowStream.filter(row -> userWithSubmissions.contains(this.idMatching == null ? row.user.getId() : this.idMatching.getOrDefault(row.user.getId(), row.user.getId())));
        }

//        Comparator<StandingsTableRow> comparator;
//        if (config.type == StandingsTableType.ICPC) {
//            comparator = Comparator.comparingInt(StandingsTableRow::getSolvedCnt).thenComparing(StandingsTableRow::getPenalty, Comparator.reverseOrder()).thenComparing(StandingsTableRow::getName);
//        } else {
//            comparator = Comparator.comparingInt(StandingsTableRow::getScore).thenComparing(StandingsTableRow::getName);
//        }

        sortedRows = rowStream.collect(Collectors.toList());
//        sortedRows = rowStream.sorted(comparator).collect(Collectors.toList());

        System.out.println("Sorting...");

        for (int i = 0; i < sortedRows.size(); i++) {
            int userId = sortedRows.get(i).user.getId();
            if (this.idMatching != null) {
                userId = this.idMatching.getOrDefault(userId, userId);
            }

            userToRow.put(userId, i);
        }
    }
}
