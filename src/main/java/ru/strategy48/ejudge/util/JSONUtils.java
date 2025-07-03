package ru.strategy48.ejudge.util;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.strategy48.ejudge.contest.*;
import ru.strategy48.ejudge.standings.StandingsTableConfig;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;

public class JSONUtils {
    private static class ContestInfo {
        private String formal_name;
        private OffsetDateTime start_time;
        private OffsetDateTime end_time;
        private String scoreboard_freeze_duration;
        private String name;
        private String shortname;

        public ContestInfo() {
        }

        public String getFormal_name() {
            return formal_name;
        }

        public void setFormal_name(String formal_name) {
            this.formal_name = formal_name;
        }

        public OffsetDateTime getStart_time() {
            return start_time;
        }

        public void setStart_time(OffsetDateTime start_time) {
            this.start_time = start_time;
        }

        public OffsetDateTime getEnd_time() {
            return end_time;
        }

        public void setEnd_time(OffsetDateTime end_time) {
            this.end_time = end_time;
        }

        public String getScoreboard_freeze_duration() {
            return scoreboard_freeze_duration;
        }

        public void setScoreboard_freeze_duration(String scoreboard_freeze_duration) {
            this.scoreboard_freeze_duration = scoreboard_freeze_duration;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getShortname() {
            return shortname;
        }

        public void setShortname(String shortname) {
            this.shortname = shortname;
        }
    }

    private static class ProblemInfo {
        private int id;
        private String short_name;
        private String name;

        public ProblemInfo() {
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getShort_name() {
            return short_name;
        }

        public void setShort_name(String short_name) {
            this.short_name = short_name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static class ScoreboardInfo {
        private RowInfo[] rows;

        public ScoreboardInfo() {
        }

        public RowInfo[] getRows() {
            return rows;
        }

        public void setRows(RowInfo[] rows) {
            this.rows = rows;
        }
    }

    private static class RowInfo {
        private int team_id;
        private ProblemScoreInfo[] problems;

        public RowInfo() {
        }

        public int getTeam_id() {
            return team_id;
        }

        public void setTeam_id(int team_id) {
            this.team_id = team_id;
        }

        public ProblemScoreInfo[] getProblems() {
            return problems;
        }

        public void setProblems(ProblemScoreInfo[] problems) {
            this.problems = problems;
        }
    }

    private static class ProblemScoreInfo {
        private String label;
        private int problem_id;
        private int num_judged;
        private boolean solved;
        private long time;
        private boolean first_to_solve;

        public ProblemScoreInfo() {
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getProblem_id() {
            return problem_id;
        }

        public void setProblem_id(int problem_id) {
            this.problem_id = problem_id;
        }

        public int getNum_judged() {
            return num_judged;
        }

        public void setNum_judged(int num_judged) {
            this.num_judged = num_judged;
        }

        public boolean isSolved() {
            return solved;
        }

        public void setSolved(boolean solved) {
            this.solved = solved;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public boolean isFirst_to_solve() {
            return first_to_solve;
        }

        public void setFirst_to_solve(boolean first_to_solve) {
            this.first_to_solve = first_to_solve;
        }
    }

    private static class TeamInfo {
        private int id;
        private String display_name;
        private boolean hidden;

        public TeamInfo() {
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getDisplay_name() {
            return display_name;
        }

        public void setDisplay_name(String display_name) {
            this.display_name = display_name;
        }

        public boolean isHidden() {
            return hidden;
        }

        public void setHidden(boolean hidden) {
            this.hidden = hidden;
        }
    }

    public static Contest getDOMJudgeContest(final URI apiUrl, final StandingsTableConfig config, final int contestId) throws IOException {
        URL contestURL = apiUrl.toURL();
        URL problemsURL = apiUrl.resolve("problems").toURL();
        URL scoreboardURL = apiUrl.resolve("scoreboard").toURL();
        URL teamsURL = apiUrl.resolve("teams").toURL();

        HttpURLConnection contestConnection = (HttpURLConnection) contestURL.openConnection();
        HttpURLConnection problemsConnection = (HttpURLConnection) problemsURL.openConnection();
        HttpURLConnection scoreboardConnection = (HttpURLConnection) scoreboardURL.openConnection();
        HttpURLConnection teamsConnection = (HttpURLConnection) teamsURL.openConnection();
        contestConnection.setRequestMethod("GET");
        problemsConnection.setRequestMethod("GET");
        scoreboardConnection.setRequestMethod("GET");
        teamsConnection.setRequestMethod("GET");

        Gson gson = Converters.registerAll(new GsonBuilder()).create();
        ContestInfo contestInfo = gson.fromJson(new InputStreamReader(contestConnection.getInputStream()), ContestInfo.class);
        Duration freezeDuration = parseHHmmssXXX(contestInfo.scoreboard_freeze_duration);

        ProblemInfo[] problems = gson.fromJson(new InputStreamReader(problemsConnection.getInputStream()), ProblemInfo[].class);

        ScoreboardInfo scoreboardInfo = gson.fromJson(new InputStreamReader(scoreboardConnection.getInputStream()), ScoreboardInfo.class);

        TeamInfo[] teams = gson.fromJson(new InputStreamReader(teamsConnection.getInputStream()), TeamInfo[].class);

        String contestName = contestInfo.name;
        Date startTime = convertOffsetDateTime(contestInfo.start_time);
        long duration = Duration.between(contestInfo.start_time, contestInfo.end_time).toSeconds();
        long freezeTime = 0;
        if (freezeDuration != null) {
            freezeTime = freezeDuration.toSeconds();
        }

        if (config.isOfficial && config.startDate != null && config.endDate != null) {
            startTime = config.startDate;
            if (config.freezeDate != null) {
                freezeTime = (config.endDate.getTime() - config.freezeDate.getTime()) / 1000;
            }
            duration = (config.endDate.getTime() - config.startDate.getTime()) / 1000;
        }

        Contest contest = new Contest(contestName, contestId, startTime, duration, freezeTime);
        for (int i = 0; i < problems.length; i++) {
            int problemId = i + 1;
            String shortName = problems[i].short_name;
            String longName = problems[i].name;

            if (config.ignoreProblems.getOrDefault(contestId, Collections.emptySet()).contains(problemId)) {
                continue;
            }
            contest.addProblem(new Problem(problemId, shortName, longName));
        }

        for (TeamInfo team : teams) {
            if (!team.hidden) {
                contest.addUser(new User(team.id, team.display_name));
            }
        }

        int runId = 0;
        for (RowInfo row : scoreboardInfo.rows) {
            int problemId = 0;
            for (ProblemScoreInfo problemScore : row.problems) {
                ++problemId;

                for (int it = 0; it < problemScore.num_judged - 1; it++) {
                    contest.addRun(new Run(runId, problemScore.time * 60, Status.WA, row.team_id, problemId, 0));
                    ++runId;
                }
                if (problemScore.solved) {
                    contest.addRun(new Run(runId, problemScore.time * 60, Status.OK, row.team_id, problemId, 100));
                    ++runId;
                } else if (problemScore.num_judged != 0) {
                    contest.addRun(new Run(runId, problemScore.time * 60, Status.WA, row.team_id, problemId, 0));
                    ++runId;
                }
            }
        }

        return contest;
    }

    private static Date convertOffsetDateTime(final OffsetDateTime odt) {
        return Date.from(odt.toInstant());
    }

    private static Duration parseHHmmssXXX(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        // splits "HH:MM:SS.mmm" into ["HH","MM","SS","mmm"]
        String[] p = s.split("[:\\.]");
        long h  = Long.parseLong(p[0]);
        long m  = Long.parseLong(p[1]);
        long sec= Long.parseLong(p[2]);
        long ms = p.length > 3 ? Long.parseLong(p[3]) : 0L;
        return Duration.ofHours(h)
                .plusMinutes(m)
                .plusSeconds(sec)
                .plusMillis(ms);
    }
}
