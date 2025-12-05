package ru.strategy48.ejudge.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.strategy48.ejudge.contest.*;
import ru.strategy48.ejudge.server.StandingsServerConfig;
import ru.strategy48.ejudge.standings.StandingsTableConfig;
import ru.strategy48.ejudge.standings.StandingsTableEntity;
import ru.strategy48.ejudge.standings.StandingsTableType;
import ru.strategy48.ejudge.standings.StandingsType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class XMLUtils {
    public static Contest parsePCMSLog(final File pcmsLogFile, final StandingsTableConfig config, final int contestId,
                                       final Map<String, Integer> loginToFakeId) throws ParserConfigurationException, SAXException, IOException, ParseException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(pcmsLogFile);

        document.getDocumentElement().normalize();

        Element contestNode = (Element) document.getElementsByTagName("contest").item(0);
        String contestName = contestNode.getAttribute("name");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Date startTime = format.parse(contestNode.getAttribute("start-time"));
        long duration = Long.parseLong(contestNode.getAttribute("length")) / 1000;
        long freezeTime = 0;
        if (contestNode.hasAttribute("freeze-millis")) {
            freezeTime = duration - Long.parseLong(contestNode.getAttribute("freeze-millis")) / 1000;
        }

        if (config.isOfficial && config.startDate != null && config.endDate != null) {
            startTime = config.startDate;
            if (config.freezeDate != null) {
                freezeTime = (config.endDate.getTime() - config.freezeDate.getTime()) / 1000;
            }
            duration = (config.endDate.getTime() - config.startDate.getTime()) / 1000;
        }

        Contest contest = new Contest(contestName, contestId, startTime, duration, freezeTime);

        Element challengeNode = (Element) document.getElementsByTagName("challenge").item(0);
        NodeList problemsList = challengeNode.getElementsByTagName("problem");
        NodeList sessionsList = contestNode.getElementsByTagName("session");

        for (int i = 0; i < problemsList.getLength(); i++) {
            Element problem = (Element) problemsList.item(i);
            int problemId = i + 1;
            String shortName = problem.getAttribute("alias");
            String longName = problem.getAttribute("name");

            if (config.ignoreProblems.getOrDefault(contestId, Collections.emptySet()).contains(problemId)) {
                continue;
            }
            contest.addProblem(new Problem(problemId, shortName, longName));
        }

        for (int i = 0; i < sessionsList.getLength(); i++) {
            Element session = (Element) sessionsList.item(i);
            String login = session.getAttribute("alias");
            String name = session.getAttribute("party");
            loginToFakeId.putIfAbsent(login, loginToFakeId.size());
            int id = loginToFakeId.get(login);
            contest.addUser(new User(id, name));

            NodeList problemsNodes = session.getElementsByTagName("problem");
            for (int j = 0; j < problemsNodes.getLength(); j++) {
                Element problem = (Element) problemsNodes.item(j);
                int problemScore = Integer.parseInt(problem.getAttribute("score"));
                contest.getPcmsScoreByUserAndProblemId().putIfAbsent(id, new HashMap<>());
                contest.getPcmsScoreByUserAndProblemId().get(id).put(j + 1, problemScore);

                NodeList runsNodes = problem.getElementsByTagName("run");
                for (int k = 0; k < runsNodes.getLength(); k++) {
                    Element run = (Element) runsNodes.item(k);

                    long time = Long.parseLong(run.getAttribute("time")) / 1000;
                    String outcome = run.getAttribute("outcome");
                    int score = Integer.parseInt(run.getAttribute("score"));
                    if (config.isOfficial && time >= duration) {
                        continue;
                    }

                    // TODO: support ICPC-style contests
                    Status status;
                    switch (outcome) {
                        case "accepted":
                            if (score == 100) {
                                status =  Status.OK;
                            } else {
                                status = Status.PT;
                            }
                            break;
                        case "compilation-error":
                            status = Status.CE;
                            break;
                        default:
                            status = Status.EM;
                    }

                    contest.addRun(new Run(-1, time, status, id, j + 1, score, false));
                }
            }
        }

        return contest;
    }

    public static Contest parseExternalLog(final File externalLogFile, final StandingsTableConfig config) throws ParserConfigurationException, SAXException, IOException, ParseException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(externalLogFile);

        document.getDocumentElement().normalize();

        NodeList problems = ((Element) document.getElementsByTagName("problems").item(0)).getElementsByTagName("problem");
        NodeList users = ((Element) document.getElementsByTagName("users").item(0)).getElementsByTagName("user");
        NodeList runs = ((Element) document.getElementsByTagName("runs").item(0)).getElementsByTagName("run");

        Element contestNode = (Element) document.getElementsByTagName("runlog").item(0);
        Node contestName = document.getElementsByTagName("name").item(0);

        DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        DateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String name = contestName.getTextContent();
        int contestId = Integer.parseInt(contestNode.getAttribute("contest_id"));
        Date startTime;
        try {
            startTime = format.parse(contestNode.getAttribute("start_time"));
        } catch (Exception ignored) {
            startTime = newFormat.parse(contestNode.getAttribute("start_time"));
        }
        long duration = -1;
        long freezeTime = -1;

        if (!contestNode.getAttribute("duration").isEmpty()) {
            duration = Long.parseLong(contestNode.getAttribute("duration"));
        }
        if (!contestNode.getAttribute("fog_time").isEmpty()) {
            freezeTime = Long.parseLong(contestNode.getAttribute("fog_time"));
        }

        if (config.isOfficial && config.startDate != null && config.endDate != null) {
            startTime = config.startDate;
            if (config.freezeDate != null) {
                freezeTime = (config.endDate.getTime() - config.freezeDate.getTime()) / 1000;
            }
            duration = ((config.endDate.getTime() - config.startDate.getTime()) / 1000);
        }

        Contest contest = new Contest(name, contestId, startTime, duration, freezeTime);
        if (document.getElementsByTagName("userrunheaders").getLength() > 0) {
            NodeList userRunHeaders = ((Element) document.getElementsByTagName("userrunheaders").item(0))
                    .getElementsByTagName("userrunheader");
            for (int i = 0; i < userRunHeaders.getLength(); i++) {
                Element userRunHeader = (Element) userRunHeaders.item(i);
                if (!userRunHeader.hasAttribute("is_virtual") || !userRunHeader.getAttribute("is_virtual").equals("yes")) {
                    continue;
                }

                int userId = Integer.parseInt(userRunHeader.getAttribute("user_id"));
                long userStartTime = newFormat.parse(userRunHeader.getAttribute("start_time")).getTime();
                contest.getVirtualTimes().put(userId, userStartTime);
            }
        }

        for (int i = 0; i < problems.getLength(); i++) {
            Element problem = (Element) problems.item(i);
            int problemId = Integer.parseInt(problem.getAttribute("id"));
            String shortName = problem.getAttribute("short_name");
            String longName = problem.getAttribute("long_name");

            if (config.ignoreProblems.getOrDefault(contestId, Collections.emptySet()).contains(problemId)) {
                continue;
            }
            contest.addProblem(new Problem(problemId, shortName, longName));
        }

        for (int i = 0; i < users.getLength(); i++) {
            Element user = (Element) users.item(i);
            int userId = Integer.parseInt(user.getAttribute("id"));
            String userName = user.getAttribute("name");

            contest.addUser(new User(userId, userName));
        }

        for (int i = 0; i < runs.getLength(); i++) {
            Element run = (Element) runs.item(i);
            int userId = Integer.parseInt(run.getAttribute("user_id"));
            int runId = Integer.parseInt(run.getAttribute("run_id"));
            long time = Long.parseLong(run.getAttribute("time"));
            if (contest.getVirtualTimes().get(userId) != null) {
                long userStartTime = contest.getVirtualTimes().get(userId);
                long submissionTime = startTime.getTime() + time * 1000L;
                System.out.println("Start timestamp = " + userStartTime);
                System.out.println("Submission timestamp = " + submissionTime);
                time = ((submissionTime - userStartTime) / 1000);
            }
            Status status;

            if (config.isOfficial && time >= duration) {
                continue;
            }

            switch (run.getAttribute("status")) {
                case "OK":
                    status = Status.OK;
                    break;
                case "CE":
                    status = Status.CE;
                    break;
                case "RT":
                    status = Status.RT;
                    break;
                case "TL":
                    status = Status.TL;
                    break;
                case "PE":
                    status = Status.PE;
                    break;
                case "WA":
                    status = Status.WA;
                    break;
                case "CF":
                    status = Status.CF;
                    break;
                case "PT":
                    status = Status.PT;
                    break;
                case "AC":
                    status = Status.AC;
                    break;
                case "IG":
                    status = Status.IG;
                    break;
                case "DQ":
                    status = Status.DQ;
                    break;
                case "PD":
                    status = Status.PD;
                    break;
                case "ML":
                    status = Status.ML;
                    break;
                case "SE":
                    status = Status.SE;
                    break;
                case "SV":
                    status = Status.SV;
                    break;
                case "WT":
                    status = Status.WT;
                    break;
                case "PR":
                    status = Status.PR;
                    break;
                case "RJ":
                    status = Status.RJ;
                    break;
                case "SK":
                    status = Status.SK;
                    break;
                case "SY":
                    status = Status.SY;
                    break;
                case "SM":
                    status = Status.SM;
                    break;
                case "RU":
                    status = Status.RU;
                    break;
                case "CD":
                    status = Status.CD;
                    break;
                case "CG":
                    status = Status.CG;
                    break;
                case "AV":
                    status = Status.AV;
                    break;
                case "EM":
                    status = Status.EM;
                    break;
                case "VS":
                    status = Status.VS;
                    break;
                case "VT":
                    status = Status.VT;
                    break;
                default:
                    status = Status.EM;
            }

            int problemId = -1;
            int score = -1;

            if (!run.getAttribute("prob_id").isEmpty()) {
                problemId = Integer.parseInt(run.getAttribute("prob_id"));
            }
            if (!run.getAttribute("score").isEmpty()) {
                if (status == Status.DQ) {
                    score = 0;
                } else {
                    score = Integer.parseInt(run.getAttribute("score"));
                }
            }

            contest.addRun(new Run(runId, time, status, userId, problemId, score, status == Status.DQ));
            if (status == Status.DQ) {
                contest.disqualifyUser(userId);
            }
        }

        return contest;
    }

    public static StandingsTableConfig parseConfigFile(final File configFile) throws ParserConfigurationException, SAXException, IOException, ParseException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(configFile);

        document.getDocumentElement().normalize();

        StandingsTableConfig config = new StandingsTableConfig();

        Element mainConfig = (Element) document.getElementsByTagName("config").item(0);
        Node standingsName = document.getElementsByTagName("name").item(0);
        Node standingsType = document.getElementsByTagName("type").item(0);
        NodeList contestsNode = ((Element) document.getElementsByTagName("contests").item(0)).getElementsByTagName("contest");

        for (int i = 0; i < contestsNode.getLength(); i++) {
            Element contest = (Element) contestsNode.item(i);
            int contestId = Integer.parseInt(contest.getAttribute("id"));
            config.contests.add(contestId);
            config.lastACProblems.put(contestId, new HashSet<>());

            NodeList lastACList = contest.getElementsByTagName("last_ac");
            for (int j = 0; j < lastACList.getLength(); j++) {
                Element problem = (Element) lastACList.item(j);
                config.lastACProblems.get(contestId).add(Integer.parseInt(problem.getTextContent()));
            }

            NodeList ignoreList = contest.getElementsByTagName("ignore");
            for (int j = 0; j < ignoreList.getLength(); j++) {
                Element problem = (Element) ignoreList.item(j);
                if (!config.ignoreProblems.containsKey(contestId)) {
                    config.ignoreProblems.put(contestId, new HashSet<>());
                }
                config.ignoreProblems.get(contestId).add(Integer.parseInt(problem.getTextContent()));
            }

            config.contestNames.put(contestId, contest.getAttribute("name"));
            if (contest.hasAttribute("pcms_dir")) {
                config.pcmsStandingsDir.put(contestId, Path.of(contest.getAttribute("pcms_dir")));
            }
            if (contest.hasAttribute("domjudge_api_url")) {
                config.domjudgeApiUrl.put(contestId, URI.create(contest.getAttribute("domjudge_api_url")));
            }
            if (contest.hasAttribute("max_judge")) {
                config.maxCountJudge = Integer.parseInt(contest.getAttribute("max_judge"));
            }
        }

        config.needFreeze = Boolean.parseBoolean(mainConfig.getAttribute("freeze"));
        config.showFirstAC = Boolean.parseBoolean(mainConfig.getAttribute("first_ac"));
        config.showUsersWithoutRuns = Boolean.parseBoolean(mainConfig.getAttribute("empty_users"));
        config.showZeros = Boolean.parseBoolean(mainConfig.getAttribute("show_zero"));
        config.showPenalty = !Boolean.parseBoolean(mainConfig.getAttribute("disable_penalty"));
        if (mainConfig.hasAttribute("type")) {
            if (mainConfig.getAttribute("type").equalsIgnoreCase("itmo")) {
                config.standingsType = StandingsType.ITMO;
            }
        }

        if (mainConfig.hasAttribute("users_info_path")) {
            config.usersInfoPath = mainConfig.getAttribute("users_info_path");
        }
        if (mainConfig.hasAttribute("users_login_path")) {
            config.usersLoginPath = mainConfig.getAttribute("users_login_path");
        }
        if (mainConfig.hasAttribute("login_matching_path")) {
            config.usersLoginMatchingPath = mainConfig.getAttribute("login_matching_path");
        }
        if (mainConfig.hasAttribute("ignore_users_without_info")) {
            if (mainConfig.getAttribute("ignore_users_without_info").equals("true")) {
                config.ignoreUsersWithoutUserInfo = true;
            }
        }
        if (mainConfig.hasAttribute("lang") && mainConfig.getAttribute("lang").equals("en")) {
            config.english = true;
        }
        if (mainConfig.hasAttribute("disable_search") && mainConfig.getAttribute("disable_search").equals("true")) {
            config.disableSearch = true;
        }

        if (mainConfig.hasAttribute("official")) {
            config.isOfficial = Boolean.parseBoolean(mainConfig.getAttribute("official"));
        }

        DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        if (mainConfig.hasAttribute("start_time")) {
            config.startDate = format.parse(mainConfig.getAttribute("start_time"));
        }
        if (mainConfig.hasAttribute("freeze_time")) {
            config.freezeDate = format.parse(mainConfig.getAttribute("freeze_time"));
        }
        if (mainConfig.hasAttribute("end_time")) {
            config.endDate = format.parse(mainConfig.getAttribute("end_time"));
        }

        config.standingsName = standingsName.getTextContent();
        switch (standingsType.getTextContent()) {
            case "ICPC":
                config.type = StandingsTableType.ICPC;
                break;
            case "IOI":
                config.type = StandingsTableType.IOI;
                break;
            default:
                break;
        }

        return config;
    }

    public static StandingsServerConfig parseServerConfigFile(final File configFile) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(configFile);

        document.getDocumentElement().normalize();

        Node hostNode = document.getElementsByTagName("host").item(0);
        Node portNode = document.getElementsByTagName("port").item(0);
        Node contestDirNode = document.getElementsByTagName("contests").item(0);
        Node vfsDirNode = document.getElementsByTagName("vfs").item(0);

        return new StandingsServerConfig(hostNode.getTextContent(), Integer.parseInt(portNode.getTextContent()), contestDirNode.getTextContent(), vfsDirNode.getTextContent());
    }

    public static List<StandingsTableEntity> parseAllConfigFiles(final Path dir) {
        XMLFileVisitor visitor = new XMLFileVisitor(dir);
        try {
            Files.walkFileTree(dir, visitor);
        } catch (Exception e) {
            return Collections.emptyList();
        }
        return visitor.getFoundConfigFiles();
    }
}
