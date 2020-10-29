package ru.strategy48.ejudge.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.strategy48.ejudge.contest.Contest;
import ru.strategy48.ejudge.standings.StandingsTableAgregator;
import ru.strategy48.ejudge.standings.StandingsTableConfig;
import ru.strategy48.ejudge.util.HTMLUtils;
import ru.strategy48.ejudge.util.XMLUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class StandingsServerHandler implements HttpHandler {
    private final StandingsServerConfig config;
    private final String configDirectory;

    public StandingsServerHandler(final StandingsServerConfig config, final String configDirectory) {
        this.config = config;
        this.configDirectory = configDirectory;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().toString();

        switch (path) {
            case "config":
            case "/":
                System.out.println("There is no standings at: " + path);
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
                return;
            default:
                Path configPath;
                try {
                    configPath = Path.of(configDirectory + path + ".xml");
                } catch (InvalidPathException e) {
                    System.out.println("There is no standings at: " + path);
                    exchange.sendResponseHeaders(404, 0);
                    exchange.close();
                    return;
                }

                if (!Files.exists(configPath)) {
                    System.out.println("There is no standings at: " + path);
                    exchange.sendResponseHeaders(404, 0);
                    exchange.close();
                    return;
                }

                StandingsTableConfig standingsConfig;
                try {
                    standingsConfig = XMLUtils.parseConfigFile(configPath.toFile());
                } catch (Exception e) {
                    System.out.println("Error reading config file at: " + path);
                    exchange.sendResponseHeaders(404, 0);
                    exchange.close();
                    return;
                }

                List<Contest> contests = new ArrayList<>();
                for (Integer contestId : standingsConfig.contests) {
                    Path externalLog;
                    try {
                        externalLog = Path.of(String.format("%s/%06d/var/status/dir/external.xml", config.contestsDir, contestId));
                    } catch (InvalidPathException e) {
                        System.out.println("Error parsing external log for contest: " + contestId);
                        exchange.sendResponseHeaders(404, 0);
                        exchange.close();
                        return;
                    }

                    try {
                        contests.add(XMLUtils.parseExternalLog(externalLog.toFile(), standingsConfig));
                    } catch (Exception e) {
                        System.out.println("Error parsing external log for contest: " + contestId);
                        exchange.sendResponseHeaders(404, 0);
                        exchange.close();
                        return;
                    }
                }

                System.out.println("Generating standings...");
                StandingsTableAgregator agregator = new StandingsTableAgregator(standingsConfig, contests);
                System.out.println("Standings generated!");
                String standingsHTML = HTMLUtils.getStandingsHTML(agregator);
                System.out.println("HTML generated!");

                System.out.println(standingsHTML);

                String html;
                try {
                    html = HTMLUtils.getStandingsHTMLFormatted(standingsHTML, new File(configDirectory + "/header.html"), new File(configDirectory + "/footer.html"));
                } catch (Exception e) {
                    System.out.println("Can't generate standings with header and footer");
                    exchange.sendResponseHeaders(404, 0);
                    exchange.close();
                    return;
                }

//                System.out.println(html);

                exchange.sendResponseHeaders(200, html.getBytes().length);
                OutputStream stream = exchange.getResponseBody();
                stream.write(html.getBytes());
                stream.close();
        }
    }
}
