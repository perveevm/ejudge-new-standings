package ru.strategy48.ejudge.util;

import au.com.bytecode.opencsv.CSVReader;
import ru.strategy48.ejudge.standings.StandingsTableUsersInfo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CSVUtils {
    public static StandingsTableUsersInfo parseUserInfo(final File csvFile) {
        List<String[]> rows;

        try (CSVReader reader = new CSVReader(new FileReader(csvFile), ';', '\"', 0)) {
            rows = reader.readAll();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

        return usersInfo;
    }

    public static Map<Integer, String> getLogins(final File csvFile) {
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

        return result;
    }

    public static Map<String, String> getLoginsMatching(final File csvFile) {
        Map<String, String> result = new HashMap<>();
        List<String[]> rows;

        try (CSVReader reader = new CSVReader(new FileReader(csvFile), ';')) {
            rows = reader.readAll();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        for (int i = 1; i < rows.size(); i++) {
            String primaryLogin = rows.get(i)[0];

            for (int j = 0; j < rows.get(i).length; j++) {
                String curLogin = rows.get(i)[j];
                result.put(curLogin, primaryLogin);
            }
        }

        return result;
    }
}
