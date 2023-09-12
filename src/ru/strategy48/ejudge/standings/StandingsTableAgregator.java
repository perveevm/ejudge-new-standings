package ru.strategy48.ejudge.standings;

import ru.strategy48.ejudge.contest.Contest;
import ru.strategy48.ejudge.contest.User;
import ru.strategy48.ejudge.util.CSVUtils;

import java.nio.file.Paths;
import java.util.*;

public class StandingsTableAgregator {
    public final StandingsTableConfig config;
    public final List<Contest> contests;
    public final List<StandingsTable> standings;

    public final Map<Integer, User> users = new HashMap<>();
    public final Map<Integer, Integer> solved = new HashMap<>();
    public final Map<Integer, Integer> penalty = new HashMap<>();
    public final Map<Integer, Integer> score = new HashMap<>();

    public final Map<Integer, Double> itmoRating = new HashMap<>();

    public final Map<Integer, Integer> minPlace = new HashMap<>();
    public final Map<Integer, Integer> maxPlace = new HashMap<>();

    public final List<Integer> sortedUsers = new ArrayList<>();

    public final StandingsTableUsersInfo usersInfo;
    public final Map<Integer, String> idToLogin;
    public final Map<String, Integer> loginToId;

    public final Map<String, String> loginMatching;
    public final Map<Integer, Integer> idMatching;

    public StandingsTableAgregator(final StandingsTableConfig config, final List<Contest> contests) {
        this.config = config;
        this.contests = contests;
        this.standings = new ArrayList<>();

        if (!config.usersInfoPath.isEmpty()) {
            System.out.println("Users info start");
            this.usersInfo = CSVUtils.parseUserInfo(Paths.get(config.usersInfoPath).toFile());
            System.out.println(String.join(", ", this.usersInfo.header));
        } else {
            this.usersInfo = null;
        }

        if (!config.usersLoginPath.isEmpty()) {
            System.out.println("Users login start");
            this.idToLogin = CSVUtils.getLogins(Paths.get(config.usersLoginPath).toFile());

            this.loginToId = new HashMap<>();
            for (Map.Entry<Integer, String> e : this.idToLogin.entrySet()) {
                loginToId.put(e.getValue(), e.getKey());
            }
        } else {
            this.idToLogin = null;
            this.loginToId = null;
        }

        if (!config.usersLoginMatchingPath.isEmpty()) {
            System.out.println("Users matching start");
            this.loginMatching = CSVUtils.getLoginsMatching(Paths.get(config.usersLoginMatchingPath).toFile());
        } else {
            this.loginMatching = null;
        }

        if (!config.usersLoginPath.isEmpty() && !config.usersLoginMatchingPath.isEmpty()) {
            this.idMatching = new HashMap<>();

            for (Map.Entry<String, String> e : this.loginMatching.entrySet()) {
                int key = loginToId.get(e.getKey());
                int value = loginToId.get(e.getValue());
                this.idMatching.put(key, value);
            }
        } else {
            this.idMatching = null;
        }

        for (Contest contest : contests) {
            standings.add(new StandingsTable(contest, config, this.idMatching));
        }

//        if (!config.usersInfoPath.isEmpty() && !config.usersLoginPath.isEmpty()) {
//            System.out.println("Users info start");
//            this.usersInfo = CSVUtils.parseUserInfo(Paths.get(config.usersInfoPath).toFile());
//            System.out.println("Users login start");
//            this.idToLogin = CSVUtils.getLogins(Paths.get(config.usersLoginPath).toFile());
//        } else {
//            this.usersInfo = null;
//            this.idToLogin = null;
//        }

        System.out.println("Parsing completed!");

        processContests();
    }

    private void processContests() {
        for (StandingsTable table : standings) {
            for (Map.Entry<Integer, Integer> entry : table.userToRow.entrySet()) {
                int userId = entry.getKey();

                if (this.idMatching != null) {
                    userId = this.idMatching.get(userId);
                }

                User user = table.sortedRows.get(entry.getValue()).user;

                if (!users.containsKey(userId)) {
                    users.put(userId, user);
                    solved.put(userId, 0);
                    penalty.put(userId, 0);
                    score.put(userId, 0);
                    itmoRating.put(userId, 0.0);
                }

                solved.put(userId, solved.get(userId) + table.sortedRows.get(entry.getValue()).getSolvedCnt());
                penalty.put(userId, penalty.get(userId) + table.sortedRows.get(entry.getValue()).getPenalty());
                score.put(userId, score.get(userId) + table.sortedRows.get(entry.getValue()).getScore());
                itmoRating.put(userId, itmoRating.get(userId) + table.getRating(userId));
            }
        }

        System.out.println("Finishing...");
        Comparator<Integer> usersComparator;
        if (config.standingsType == StandingsType.ITMO) {
            usersComparator = (user1, user2) -> {
                double rating1 = itmoRating.get(user1);
                double rating2 = itmoRating.get(user2);
                return -Double.compare(rating1, rating2);
            };
        } else if (config.type == StandingsTableType.ICPC) {
            usersComparator = (user1, user2) -> {
                int solved1 = solved.get(user1);
                int solved2 = solved.get(user2);

                int penalty1 = penalty.get(user1);
                int penalty2 = penalty.get(user2);

                String name1 = users.get(user1).getName();
                String name2 = users.get(user2).getName();

                if (solved1 != solved2) {
                    return -Integer.compare(solved1, solved2);
                } else if (penalty1 != penalty2) {
                    return Integer.compare(penalty1, penalty2);
                } else {
                    return name1.compareTo(name2);
                }
            };
        } else {
            usersComparator = (user1, user2) -> {
                int score1 = score.get(user1);
                int score2 = score.get(user2);

                String name1 = users.get(user1).getName();
                String name2 = users.get(user2).getName();

                if (score1 != score2) {
                    return -Integer.compare(score1, score2);
                } else {
                    return name1.compareTo(name2);
                }
            };
        }

        sortedUsers.addAll(users.keySet());
        sortedUsers.sort(usersComparator);

        int l = 0;
        int lastPlace = 0;
        for (int i = 0; i < sortedUsers.size(); i++) {
            int userId = sortedUsers.get(i);
            int prevUserId = sortedUsers.get(l);
            if ((config.type == StandingsTableType.IOI && !score.get(userId).equals(score.get(prevUserId)))
            || (config.type == StandingsTableType.ICPC && (!solved.get(userId).equals(solved.get(prevUserId)) || (config.showPenalty && !penalty.get(userId).equals(penalty.get(prevUserId)))))) {
                int curMin = lastPlace + 1;
                int curMax = lastPlace + i - l;

                for (int j = l; j < i; j++) {
                    int curUserId = sortedUsers.get(j);
                    minPlace.put(curUserId, curMin);
                    maxPlace.put(curUserId, curMax);
                }

                l = i;
                lastPlace = curMax;
            }
        }

        int curMin = lastPlace + 1;
        int curMax = lastPlace + sortedUsers.size() - l;

        for (int j = l; j < sortedUsers.size(); j++) {
            int curUserId = sortedUsers.get(j);
            minPlace.put(curUserId, curMin);
            maxPlace.put(curUserId, curMax);
        }
    }
}
