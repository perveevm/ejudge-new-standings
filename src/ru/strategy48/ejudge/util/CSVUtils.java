package ru.strategy48.ejudge.util;

import au.com.bytecode.opencsv.CSVReader;
import ru.strategy48.ejudge.standings.StandingsTableUsersInfo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CSVUtils {
    public static StandingsTableUsersInfo parseUserInfo(final File csvFile) {
        System.out.println(csvFile + " started");

        List<String[]> rows = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvFile), ';')) {
            System.out.println("KEK");
            String[] line;
            while ((line = reader.readNext()) != null) {
                rows.add(line);
                System.out.println("+1 line");
            }
//            rows = reader.readAll();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }

        for (String[] row : rows) {
            for (String j : row) {
                System.out.print(j + " ");
            }
            System.out.println();
        }

        if (rows.isEmpty() || rows.get(0).length == 0) {
            return null;
        }

        StandingsTableUsersInfo usersInfo = new StandingsTableUsersInfo();
        usersInfo.header = Arrays.asList(rows.get(0)).subList(1, rows.get(0).length);

        for (int i = 1; i < rows.size(); i++) {
            String login = rows.get(i)[0];

            if (usersInfo.fields.containsKey(login)) {
                return null;
            }

            usersInfo.fields.put(login, Arrays.asList(rows.get(i)).subList(1, rows.get(i).length));
        }

        System.out.println(csvFile + " OK!");

        return usersInfo;
    }

    public static Map<Integer, String> getLogins(final File csvFile) {
        System.out.println(csvFile + " started");

        Map<Integer, String> result = new HashMap<>();
        List<String[]> rows;

        try (CSVReader reader = new CSVReader(new FileReader(csvFile), ';')) {
            rows = reader.readAll();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        for (int i = 1; i < rows.size(); i++) {
            Integer userID = Integer.parseInt(rows.get(i)[0]);
            String login = rows.get(i)[1];
            result.put(userID, login);
        }

        System.out.println(csvFile + " OK!");

        return result;
    }
}
