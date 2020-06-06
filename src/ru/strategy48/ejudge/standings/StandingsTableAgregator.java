package ru.strategy48.ejudge.standings;

import ru.strategy48.ejudge.contest.Contest;
import ru.strategy48.ejudge.contest.User;

import java.util.*;

public class StandingsTableAgregator {
    public final StandingsTableConfig config;
    public final List<Contest> contests;
    public final List<StandingsTable> standings;

    public final Map<Integer, User> users = new HashMap<>();
    public final Map<Integer, Integer> solved = new HashMap<>();
    public final Map<Integer, Integer> penalty = new HashMap<>();
    public final Map<Integer, Integer> score = new HashMap<>();

    public final List<Integer> sortedUsers = new ArrayList<>();

    public StandingsTableAgregator(final StandingsTableConfig config, final List<Contest> contests) {
        this.config = config;
        this.contests = contests;
        this.standings = new ArrayList<>();

        for (Contest contest : contests) {
            standings.add(new StandingsTable(contest, config));
        }

        processContests();
    }

    private void processContests() {
        for (StandingsTable table : standings) {
            for (Map.Entry<Integer, Integer> entry : table.userToRow.entrySet()) {
                int userId = entry.getKey();
                User user = table.sortedRows.get(entry.getValue()).user;

                if (!users.containsKey(userId)) {
                    users.put(userId, user);
                    solved.put(userId, 0);
                    penalty.put(userId, 0);
                    score.put(userId, 0);
                }

                solved.put(userId, solved.get(userId) + table.sortedRows.get(entry.getValue()).getSolvedCnt());
                penalty.put(userId, penalty.get(userId) + table.sortedRows.get(entry.getValue()).getPenalty());
                score.put(userId, score.get(userId) + table.sortedRows.get(entry.getValue()).getScore());
            }
        }

        Comparator<Integer> usersComparator;
        if (config.type == StandingsTableType.ICPC) {
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
    }
}
