package ru.strategy48.ejudge.server;

public class StandingsServerConfig {
    public final String host;
    public final int port;
    public final String contestsDir;

    public final String vfsDir;

    public StandingsServerConfig(final String host, final int port, final String contestsDir, final String vfsDir) {
        this.host = host;
        this.port = port;
        this.contestsDir = contestsDir;
        this.vfsDir = vfsDir;
    }
}
