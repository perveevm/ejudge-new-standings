package ru.strategy48.ejudge.contest;

import java.util.*;

public class Contest {
    private final int contestId;
    private final long duration;
    private final Date startTime;
    private final long freezeTime;
    private final String name;

    private final List<Problem> problems = new ArrayList<>();
    private final List<User> users = new ArrayList<>();
    private final List<Run> runs = new ArrayList<>();

    private final Map<Integer, Long> virtualTimes = new HashMap<>();

    private final Map<Integer, Map<Integer, Integer>> pcmsScoreByUserAndProblemId = new HashMap<>();

    private final Map<Integer, Integer> domjudgeFirstACFix = new HashMap<>();

    public Contest(final String name, final int contestId, final Date startTime, final long duration, final long freezeTime) {
        this.name = name;
        this.contestId = contestId;
        this.duration = duration;
        this.startTime = startTime;
        this.freezeTime = freezeTime;
    }

    public int getContestId() {
        return contestId;
    }

    public long getDuration() {
        return duration;
    }

    public Date getStartTime() {
        return startTime;
    }

    public long getFreezeTime() {
        return freezeTime;
    }

    public Map<Integer, Long> getVirtualTimes() {
        return virtualTimes;
    }

    public boolean isInfinite() {
        return duration == -1;
    }

    public String getName() {
        return name;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public List<User> getUsers() {
        return users;
    }

    public List<Run> getRuns() {
        return runs;
    }

    public void addProblem(final Problem problem) {
        problems.add(problem);
    }

    public void addUser(final User user) {
        users.add(user);
    }

    public void addRun(final Run run) {
        runs.add(run);
    }

    public boolean needFreeze(final long time) {
        return !isInfinite() && time >= duration - freezeTime;
    }

    public Map<Integer, Map<Integer, Integer>> getPcmsScoreByUserAndProblemId() {
        return pcmsScoreByUserAndProblemId;
    }

    public Map<Integer, Integer> getDomjudgeFirstACFix() {
        return domjudgeFirstACFix;
    }
}
