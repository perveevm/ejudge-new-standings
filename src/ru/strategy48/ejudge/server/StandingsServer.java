package ru.strategy48.ejudge.server;

import com.sun.net.httpserver.HttpServer;
import ru.strategy48.ejudge.util.XMLUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;

public class StandingsServer implements AutoCloseable {
    private final HttpServer server;

    public StandingsServer(final String serverConfigPath) throws StandingsServerException {
        StandingsServerConfig config;

        try {
            config = XMLUtils.parseServerConfigFile(Paths.get(serverConfigPath).toFile());
        } catch (Exception e) {
            throw new StandingsServerException("Can't find config file by given path: " + e.getMessage());
        }

        try {
            server = HttpServer.create();
            server.bind(new InetSocketAddress(config.host, config.port), 10000);
            server.createContext("/", new StandingsServerHandler(config, Paths.get(serverConfigPath).getParent().toString()));
            server.start();

            System.out.println("Server started at host: " + config.host + " and port: " + config.port);
        } catch (IOException e) {
            throw new StandingsServerException("Can't create server: " + e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        server.stop(0);

        System.out.println("Server closed");
    }
}
