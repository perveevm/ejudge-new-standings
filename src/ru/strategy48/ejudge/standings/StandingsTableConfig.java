package ru.strategy48.ejudge.standings;

import java.util.*;

public class StandingsTableConfig {
    public boolean needFreeze = false;
    public boolean showZeros = true;
    public boolean showUsersWithoutRuns = true;
    public boolean showFirstAC = true;
    public boolean showPenalty = true;

    public StandingsTableType type = StandingsTableType.ICPC;
    public Map<Integer, Set<Integer>> lastACProblems = new HashMap<>();

    public List<Integer> contests = new ArrayList<>();
    public Map<Integer, String> contestNames = new HashMap<>();

    public String standingsName = "";
}
