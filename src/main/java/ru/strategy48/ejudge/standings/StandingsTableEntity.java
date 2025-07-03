package ru.strategy48.ejudge.standings;

public class StandingsTableEntity {
    public StandingsTableConfig standingsConfig;
    public String standingsName;

    public StandingsTableEntity(StandingsTableConfig standingsConfig, String standingsName) {
        this.standingsConfig = standingsConfig;
        this.standingsName = standingsName;
    }
}
