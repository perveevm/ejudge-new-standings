package ru.strategy48.ejudge.standings;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;

public class StandingsTableConfig {
    public boolean needFreeze = false;
    public boolean showZeros = true;
    public boolean showUsersWithoutRuns = true;
    public boolean showFirstAC = true;
    public boolean showPenalty = true;
    public boolean isOfficial = false;
    public boolean ignoreUsersWithoutUserInfo = false;
    public boolean english = false;

    public StandingsType standingsType = StandingsType.STANDARD;

    public Date startDate = null;
    public Date endDate = null;
    public Date freezeDate = null;

    public StandingsTableType type = StandingsTableType.ICPC;
    public Map<Integer, Set<Integer>> lastACProblems = new HashMap<>();
    public Map<Integer, Set<Integer>> ignoreProblems = new HashMap<>();

    public List<Integer> contests = new ArrayList<>();
    public Map<Integer, String> contestNames = new HashMap<>();
    public Map<Integer, Path> pcmsStandingsDir = new HashMap<>();
    public Map<Integer, URI> domjudgeApiUrl = new HashMap<>();

    public String standingsName = "";
    public String usersInfoPath = "";
    public String usersLoginPath = "";

    public String usersLoginMatchingPath = "";

    public int maxCountJudge = -1;
}
