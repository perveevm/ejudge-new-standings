package ru.strategy48.ejudge;

import ru.strategy48.ejudge.server.StandingsServer;
import ru.strategy48.ejudge.server.StandingsServerException;

public class Main {
    public static void main(String[] args) {
        if (args == null || args.length != 1) {
            System.out.println("Expected one not-null argument");
            return;
        }

        try {
            StandingsServer server = new StandingsServer(args[0]);
        } catch (StandingsServerException e) {
            System.out.println("Error happened!");
            System.out.println(e.getMessage());
            return;
        }
    }
}
