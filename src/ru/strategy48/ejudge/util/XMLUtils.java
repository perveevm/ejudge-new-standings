package ru.strategy48.ejudge.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.strategy48.ejudge.contest.*;
import ru.strategy48.ejudge.server.StandingsServerConfig;
import ru.strategy48.ejudge.standings.StandingsTableConfig;
import ru.strategy48.ejudge.standings.StandingsTableType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

public class XMLUtils {
    public static Contest parseExternalLog(final File externalLogFile) throws ParserConfigurationException, SAXException, IOException, ParseException {
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

        String name = contestName.getTextContent();
        int contestId = Integer.parseInt(contestNode.getAttribute("contest_id"));
        Date startTime = format.parse(contestNode.getAttribute("start_time"));
        int duration = -1;
        int freezeTime = -1;

        if (!contestNode.getAttribute("duration").isEmpty()) {
            duration = Integer.parseInt(contestNode.getAttribute("duration"));
        }
        if (!contestNode.getAttribute("fog_time").isEmpty()) {
            freezeTime = Integer.parseInt(contestNode.getAttribute("fog_time"));
        }

        Contest contest = new Contest(name, contestId, startTime, duration, freezeTime);

        for (int i = 0; i < problems.getLength(); i++) {
            Element problem = (Element) problems.item(i);
            int problemId = Integer.parseInt(problem.getAttribute("id"));
            String shortName = problem.getAttribute("short_name");
            String longName = problem.getAttribute("long_name");

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
            int runId = Integer.parseInt(run.getAttribute("run_id"));
            int time = Integer.parseInt(run.getAttribute("time"));
            Status status;

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

            int userId = Integer.parseInt(run.getAttribute("user_id"));
            int problemId = -1;
            int score = -1;

            if (!run.getAttribute("prob_id").isEmpty()) {
                problemId = Integer.parseInt(run.getAttribute("prob_id"));
            }
            if (!run.getAttribute("score").isEmpty()) {
                score = Integer.parseInt(run.getAttribute("score"));
            }

            contest.addRun(new Run(runId, time, status, userId, problemId, score));
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

            config.contestNames.put(contestId, contest.getAttribute("name"));
        }

        config.needFreeze = Boolean.parseBoolean(mainConfig.getAttribute("freeze"));
        config.showFirstAC = Boolean.parseBoolean(mainConfig.getAttribute("first_ac"));
        config.showUsersWithoutRuns = Boolean.parseBoolean(mainConfig.getAttribute("empty_users"));
        config.showZeros = Boolean.parseBoolean(mainConfig.getAttribute("show_zero"));

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

    public static StandingsServerConfig parseServerConfigFile(final File configFile) throws ParserConfigurationException, SAXException, IOException, ParseException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(configFile);

        document.getDocumentElement().normalize();

        Node hostNode = document.getElementsByTagName("host").item(0);
        Node portNode = document.getElementsByTagName("port").item(0);
        Node contestDirNode = document.getElementsByTagName("contests").item(0);

        return new StandingsServerConfig(hostNode.getTextContent(), Integer.parseInt(portNode.getTextContent()), contestDirNode.getTextContent());
    }
}
