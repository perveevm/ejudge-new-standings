package ru.strategy48.ejudge.util;

import org.jsoup.Jsoup;
import ru.strategy48.ejudge.contest.Contest;
import ru.strategy48.ejudge.contest.Problem;
import ru.strategy48.ejudge.standings.*;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HTMLUtils {
    public static ResourceBundle getBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle("messages", locale);
        } catch (MissingResourceException | NullPointerException e) {
            System.out.println("Shit happened!!! NO RESOURCE BUNDLE!!!");
            return ResourceBundle.getBundle("messages", locale);
        }
    }

    public static String getMessage(String messageKey, boolean english) {
        String message;
        if (english) {
            message = getBundle(Locale.forLanguageTag("en")).getString(messageKey);
        } else {
            message = getBundle(Locale.forLanguageTag("ru")).getString(messageKey);
        }
        return message;
    }

    private static String getStatsHTML(final StandingsTableAgregator standings, final int userId, final boolean fixedCol) {
        StringBuilder html = new StringBuilder();

        if (standings.config.standingsType == StandingsType.ITMO) {
            if (standings.contestsCntByUser.getOrDefault(userId, 0) == 0) {
                for (int i = 0; i < 5; ++i) {
                    html.append(String.format("<td class=\"stat%s\">", fixedCol ? " fixed-side" : ""));
                    html.append("–");
                    html.append("</td>");
                }
            } else {
                html.append(String.format("<td class=\"stat%s\">", fixedCol ? " fixed-side" : ""));
                html.append(String.format("%.2f", standings.itmoRating.get(userId) / standings.contestsCntByUser.get(userId)));
                html.append("</td>");
                html.append(String.format("<td class=\"stat%s\">", fixedCol ? " fixed-side" : ""));
                html.append(String.format("%.2f", standings.minItmoRating.get(userId)));
                html.append("</td>");
                html.append(String.format("<td class=\"stat%s\">", fixedCol ? " fixed-side" : ""));
                html.append(String.format("%.2f", standings.maxItmoRating.get(userId)));
                html.append("</td>");
                html.append(String.format("<td class=\"stat%s\">", fixedCol ? " fixed-side" : ""));
                html.append(String.format("%d", standings.solved.get(userId)));
                html.append("</td>");
                html.append(String.format("<td class=\"stat%s\">", fixedCol ? " fixed-side" : ""));
                html.append(String.format("%.2f", standings.dirt.get(userId) / standings.contestsCntByUser.get(userId)));
                html.append("</td>");
//                html.append(String.format("<td class=\"stat%s\">", fixedCol ? " fixed-side" : ""));
//                html.append(String.format("%.2f", (double)standings.lastHourACRuns.get(userId) / (double)standings.contestsCntByUser.get(userId)));
//                html.append("</td>");
            }
        } else if (standings.config.type == StandingsTableType.ICPC) {
            html.append(String.format("<td class=\"stat%s\">", fixedCol ? " fixed-side" : ""));
            html.append(standings.solved.get(userId));
            html.append("</td>");

            if (standings.config.showPenalty) {
                html.append(String.format("<td class=\"stat%s\">", fixedCol ? " fixed-side" : ""));
                html.append(standings.penalty.get(userId));
                html.append("</td>");
            }
        } else {
            html.append(String.format("<td class=\"stat%s\">", fixedCol ? " fixed-side" : ""));
            html.append(standings.score.get(userId));
            html.append("</td>");
        }

        return html.toString();
    }

    private static String formatDuration(final long duration) {
        long hh = TimeUnit.MILLISECONDS.toHours(duration);
        long mm = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
        long ss = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
        return String.format("%d:%02d:%02d", hh, mm, ss);
    }

    public static String getStandingsHTML(final StandingsTableAgregator standings, final String url) {
        StringBuilder html = new StringBuilder();

        // Add standings table name
        html.append(String.format("<div align=\"center\"><h3>%s</h3></div>\n", standings.config.standingsName + (standings.config.needFreeze ? " " + getMessage("resultsFrozen", standings.config.english) : "")));

        if (standings.config.isOfficial && standings.config.startDate != null && standings.config.endDate != null) {
            Date start = standings.config.startDate;
            Date end = standings.config.endDate;
            Date now = new Date(System.currentTimeMillis());

            long contestDuration = end.getTime() - start.getTime();
            long curDuration = now.getTime() - start.getTime();

            html.append(String.format("<div align=\"center\"><h4>%s %s %s<br/>", formatDuration(Math.min(curDuration, contestDuration)), getMessage("of", standings.config.english), formatDuration(contestDuration)));
            if (curDuration >= contestDuration) {
                html.append(getMessage("finished", standings.config.english));
            } else {
                html.append(getMessage("inProgress", standings.config.english));
            }
            html.append("</h4></div>");
        }

        html.append("<div class=\"main-table-shit-class\">\n");
        if (standings.config.maxCountJudge != -1) {
            html.append(String.format("<p class=\"contest-shit-info\">Обратите внимание, что в данном контесте применяется необычная система оценивания! В качестве результата будет взята сумма по %d задачам с <b>наилучшими</b> баллами.</p>", standings.config.maxCountJudge));
        }
        if (standings.config.ignoreUsersWithoutUserInfo) {
            html.append("<p class=\"contest-shit-info\">Отображаются только <b>официальные</b> участники. Места в таблице рассчитываются с учетом всех участников.</p>");
        }

        if (!standings.config.disableSearch) {
            html.append(String.format("<div class=\"main\">\n" +
                    "            <div class=\"input-group\">\n" +
                    "                <input type=\"text\" class=\"form-control\" placeholder=\"%s...\" id=\"search-field\">\n" +
                    "                <div class=\"input-group-append\">\n" +
                    "                    <button class=\"btn btn-secondary\" type=\"button\">\n" +
                    "                        <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-search\" viewBox=\"0 0 16 16\">\n" +
                    "                            <path d=\"M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z\"/>\n" +
                    "                        </svg>\n" +
                    "                        <i class=\"fa fa-search\"></i>\n" +
                    "                    </button>\n" +
                    "                    <button class=\"btn btn-info btn-secondary\" type=\"button\" onclick=\"clearFilter()\">\n" +
                    "                        %s\n" +
                    "                    </button>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>", getMessage("search", standings.config.english), getMessage("flushFilter", standings.config.english)));
        }

        // Add standings table header
        html.append("<div id=\"table-scroll\" class=\"table-scroll\"><div class=\"table-wrap\">");
        html.append("<table class=\"new-standings main-table\" id=\"standings-table-id\">\n");
        html.append("<thead>\n");

        int defaultRowSpan = 2;
        if (standings.config.standingsType == StandingsType.ITMO) {
            defaultRowSpan = 1;
        }
        // Add caption row
        html.append("<tr>\n");
        html.append(String.format("<th rowspan=\"%s\" class=\"fixed-side\"><div class=\"new-standings-cell\" valign=\"middle\">%s</div></th>\n", defaultRowSpan, getMessage("place", standings.config.english)));

        if (standings.usersInfo == null) {
            html.append(String.format("<th rowspan=\"%d\" class=\"user_info_header fixed-side\">%s</th>\n", defaultRowSpan, getMessage("participant", standings.config.english)));
        } else {
            for (String caption : standings.usersInfo.header) {
                html.append(String.format("<th rowspan=\"%d\" class=\"user_info_header fixed-side\">%s</th>", defaultRowSpan, caption));
            }
        }

        if (standings.config.standingsType == StandingsType.ITMO) {
//            html.append(String.format("<th class=\"fixed-side\">%s</th>\n", getMessage("rating", standings.config.english)));
        } else if (standings.config.type == StandingsTableType.ICPC) {
            html.append(String.format("<th rowspan=\"2\" class=\"fixed-side\">%s</th>\n", getMessage("solved", standings.config.english)));
            if (standings.config.showPenalty) {
                html.append(String.format("<th rowspan=\"2\" class=\"fixed-side\">%s</th>\n", getMessage("penalty", standings.config.english)));
            }
        } else {
            html.append(String.format("<th rowspan=\"2\" class=\"fixed-side\">%s</th>\n", getMessage("score", standings.config.english)));
        }

        for (Contest contest : standings.contests) {
            if (standings.config.standingsType == StandingsType.ITMO) {
                if (standings.config.pcmsStandingsDir.containsKey(contest.getContestId())) {
                    html.append(String.format("<th><a href=\"/standings%s\" class=\"link-dark\">%s</a> <small><a href=\"/pcms\">(%s)</a></small></th>\n", url + "?contests=" + contest.getContestId(), standings.config.contestNames.get(contest.getContestId()), getMessage("go", standings.config.english)));
                } else if (standings.config.domjudgeApiUrl.containsKey(contest.getContestId())) {
                    String domjudgeURL = "";
                    try {
                        URL tmp = standings.config.domjudgeApiUrl.get(contest.getContestId()).toURL();
                        domjudgeURL = "https://" + tmp.getHost();
                    } catch (Exception ignored) {
                    }
                    html.append(String.format("<th><a href=\"/standings%s\" class=\"link-dark\">%s</a> <small><a href=\"%s\">(%s)</a></small></th>\n", url + "?contests=" + contest.getContestId(), standings.config.contestNames.get(contest.getContestId()), domjudgeURL, getMessage("go", standings.config.english)));
                } else {
                    html.append(String.format("<th><a href=\"/standings%s\" class=\"link-dark\">%s</a> <small><a href=\"/cgi-bin/new-client?contest_id=%d\">(%s)</a></small></th>\n", url + "?contests=" + contest.getContestId(), standings.config.contestNames.get(contest.getContestId()), contest.getContestId(), getMessage("go", standings.config.english)));
                }
            } else {
                if (standings.config.pcmsStandingsDir.containsKey(contest.getContestId())) {
                    html.append(String.format("<th colspan=\"%d\"><a href=\"/standings%s\" class=\"link-dark\">%s</a> <small><a href=\"/pcms\">(%s)</a></small></th>\n", contest.getProblems().size(), url + "?contests=" + contest.getContestId(), standings.config.contestNames.get(contest.getContestId()), getMessage("go", standings.config.english)));
                } else if (standings.config.domjudgeApiUrl.containsKey(contest.getContestId())) {
                    String domjudgeURL = "";
                    try {
                        URL tmp = standings.config.domjudgeApiUrl.get(contest.getContestId()).toURL();
                        domjudgeURL = "https://" + tmp.getHost();
                    } catch (Exception ignored) {
                    }
                    html.append(String.format("<th colspan=\"%d\"><a href=\"/standings%s\" class=\"link-dark\">%s</a> <small><a href=\"%s\">(%s)</a></small></th>\n", contest.getProblems().size(), url + "?contests=" + contest.getContestId(), standings.config.contestNames.get(contest.getContestId()), domjudgeURL, getMessage("go", standings.config.english)));
                } else {
                    html.append(String.format("<th colspan=\"%d\"><a href=\"/standings%s\" class=\"link-dark\">%s</a> <small><a href=\"/cgi-bin/new-client?contest_id=%d\">(%s)</a></small></th>\n", contest.getProblems().size(), url + "?contests=" + contest.getContestId(), standings.config.contestNames.get(contest.getContestId()), contest.getContestId(), getMessage("go", standings.config.english)));
                }
            }
        }

        if (standings.config.standingsType == StandingsType.ITMO) {
            html.append(String.format("<th class=\"fixed-side\">%s</th>\n", getMessage("rating", standings.config.english)));
            html.append(String.format("<th class=\"fixed-side\">%s</th>\n", getMessage("minRating", standings.config.english)));
            html.append(String.format("<th class=\"fixed-side\">%s</th>\n", getMessage("maxRating", standings.config.english)));
            html.append(String.format("<th class=\"fixed-side\">%s</th>\n", getMessage("solved", standings.config.english)));
            html.append(String.format("<th class=\"fixed-side\">%s</th>\n", getMessage("dirt", standings.config.english)));
//            html.append(String.format("<th class=\"fixed-side\">%s</th>\n", getMessage("lastHour", standings.config.english)));
        } else if (standings.config.type == StandingsTableType.ICPC) {
            html.append(String.format("<th rowspan=\"2\" class=\"fixed-side\">%s</th>\n", getMessage("solved", standings.config.english)));
            if (standings.config.showPenalty) {
                html.append(String.format("<th rowspan=\"2\" class=\"fixed-side\">%s</th>\n", getMessage("penalty", standings.config.english)));
            }
        } else {
            html.append(String.format("<th rowspan=\"2\" class=\"fixed-side\">%s</th>\n", getMessage("score", standings.config.english)));
        }

        html.append("</tr>\n");
        if (standings.config.standingsType == StandingsType.STANDARD) {
            html.append("<tr>\n");

            for (Contest contest : standings.contests) {
                for (Problem problem : contest.getProblems()) {
                    html.append(String.format("<th title=\"%s\">%s</th>\n", problem.getLongName(), problem.getShortName()));
                }
            }

            html.append("</tr>\n");
        }

        html.append("</thead>\n");
        html.append("<tbody>\n");

        // Add results
        int groupParity = 1, parity = 0;
        int prevScore = -1;

        for (Integer userId : standings.sortedUsers) {
            // Add place
            int minPlace = standings.minPlace.get(userId);
            int maxPlace = standings.maxPlace.get(userId);

            int curScore;
            if (standings.config.type == StandingsTableType.ICPC) {
                curScore = standings.solved.get(userId);
            } else {
                curScore = standings.score.get(userId);
            }

            if (prevScore != curScore) {
                prevScore = curScore;
                groupParity ^= 1;
                parity = 0;
            }

            String rowType = "r" + groupParity + parity;
            parity ^= 1;

            StringBuilder prefix = new StringBuilder();

            prefix.append(String.format("<tr class=\"%s\">\n", rowType));

            if (minPlace == maxPlace) {
                prefix.append(String.format("<td class=\"stat fixed-side\" valign=\"center\">%d</td>", minPlace));
            } else {
                prefix.append(String.format("<td class=\"stat fixed-side\" valign=\"center\">%d-%d</td>", minPlace, maxPlace));
            }

            boolean skipUser = false;
            // Add name
            if (standings.usersInfo == null) {
                if (!standings.users.get(userId).getName().isEmpty() || standings.idToLogin == null) {
                    prefix.append(String.format("<td class=\"user_info fixed-side\">%s</td>\n", standings.users.get(userId).getName()));
                } else {
                    String login = standings.idToLogin.get(standings.users.get(userId).getId());
                    prefix.append(String.format("<td class=\"user_info fixed-side\">%s</td>\n", login));
                }
            } else {
                String login = standings.idToLogin.get(standings.users.get(userId).getId());
                if (!standings.usersInfo.fields.containsKey(login)) {
                    if (standings.config.ignoreUsersWithoutUserInfo) {
                        skipUser = true;
                    }
                    for (int i = 0; i < standings.usersInfo.header.size(); i++) {
                        prefix.append(String.format("<td class=\"user_info fixed-side\">%s</td>\n", login));
                    }
                } else {
                    for (String param : standings.usersInfo.fields.get(login)) {
                        prefix.append(String.format("<td class=\"user_info fixed-side\">%s</td>\n", param));
                    }
                }
            }

            if (skipUser) {
                continue;
            }

            html.append(prefix);

            // Add stats
            if (standings.config.standingsType != StandingsType.ITMO) {
                html.append(getStatsHTML(standings, userId, true));
            }

            // Add results
            for (StandingsTable table : standings.standings) {
                int rowId = table.userToRow.getOrDefault(userId, -1);

                if (standings.config.standingsType == StandingsType.ITMO) {
                    double curRating = table.getRating(userId);
                    if (curRating == -1.0) {
                        html.append("<td style=\"text-align: center;\">");
                        html.append("–");
                        html.append("</td>");
                    } else {
                        int r = (int) (153 + (22 - 153) * Math.sqrt(curRating / 200.0));
                        int g = (int) (27 + (101 - 27) * Math.sqrt(curRating / 200.0));
                        int b = (int) (27 + (52 - 27) * Math.sqrt(curRating / 200.0));
                        html.append(String.format("<td style=\"background-color: rgb(%d, %d, %d); text-align: center;\">", r, g, b));
                        html.append(String.format("%.2f", curRating));
                        html.append("</td>");
                    }
                } else {
                    if (rowId == -1) {
                        for (int i = 0; i < table.contest.getProblems().size(); i++) {
                            if (i == table.contest.getProblems().size() - 1) {
                                html.append("<td class=\"last\"></td>\n");
                            } else {
                                html.append("<td></td>\n");
                            }
                        }
                    } else {
                        StandingsTableRow row = table.sortedRows.get(rowId);
                        int cur = 0;

                        int i = 0;
                        for (StandingsTableCell cell : row.cells.values()) {
                            long time = cell.time / 60;
                            cur++;

                            String style = "";
                            // TODO: borders
//                        if (cur == row.cells.size()) {
//                            style = " style=\"border-right: solid black 3px;\"";
//                        }

                            boolean isLast = (i == row.cells.size() - 1);
                            if (cell.freezed) {
                                html.append(String.format("<td class=\"freezed%s\">%s", isLast ? " last" : "", style));
                                time = cell.freezedTime / 60;

                                if (cell.freezedSolved) {
                                    if (standings.config.type == StandingsTableType.ICPC) {
                                        html.append("<div class=\"sign\">");
                                        html.append("+");
                                        if (cell.freezedAttempts != 0) {
                                            html.append(cell.freezedAttempts);
                                        }
                                        html.append("</div>");

                                        if (standings.config.showPenalty) {
                                            html.append(String.format("<div class=\"penalty-time\">%d:%02d</div>", time / 60, time % 60));
                                        }
                                    } else {
                                        html.append(cell.freezedScore);
                                    }
                                } else if (cell.attempts != 0) {
                                    if (standings.config.type == StandingsTableType.ICPC) {
                                        if (cell.freezedAttempts != 0) {
                                            html.append("<div class=\"sign\">");
                                            html.append("-");
                                            html.append(cell.freezedAttempts);
                                            html.append("</div>");

                                            if (standings.config.showPenalty) {
                                                html.append(String.format("<div class=\"penalty-time\">%d:%02d</div>", time / 60, time % 60));
                                            }
                                        }
                                    } else {
                                        html.append(cell.freezedScore);
                                    }
                                }

                            } else if (cell.running) {
                                // TODO: running cells
                                html.append(String.format("<td class=\"running%s\"%s>", isLast ? " last" : "", style));
                                html.append("TODO");
                            } else if (cell.solved) {
                                if (cell.firstAC) {
                                    html.append(String.format("<td class=\"firstAC%s\"%s>", isLast ? " last" : "", style));
                                } else {
                                    html.append(String.format("<td class=\"ok%s\"%s>", isLast ? " last" : "", style));
                                }

                                if (standings.config.type == StandingsTableType.ICPC) {
                                    html.append("<div class=\"sign\">");
                                    html.append("+");
                                    if (cell.attempts != 0) {
                                        html.append(cell.attempts);
                                    }
                                    html.append("</div>");

                                    if (standings.config.showPenalty) {
                                        html.append(String.format("<div class=\"penalty-time\">%d:%02d</div>", time / 60, time % 60));
                                    }
                                } else {
                                    html.append(cell.score);
                                }
                            } else if (cell.attempts != 0) {
                                if (standings.config.type == StandingsTableType.ICPC) {
                                    html.append(String.format("<td class=\"rj%s\"%s>", isLast ? " last" : "", style));
                                    html.append("<div class=\"sign\">");
                                    html.append("-");
                                    html.append(cell.attempts);
                                    html.append("</div>");

                                    if (standings.config.showPenalty) {
                                        html.append(String.format("<div class=\"penalty-time\">%d:%02d</div>", time / 60, time % 60));
                                    }
                                } else {
                                    int r = (int) (247 + (208 - 247) * Math.sqrt((double) cell.score / 100.0));
                                    int g = (int) (94 + (240 - 94) * Math.sqrt((double) cell.score / 100.0));
                                    int b = (int) (99 + (208 - 99) * Math.sqrt((double) cell.score / 100.0));
                                    html.append(String.format("<td style=\"background-color: rgb(%d, %d, %d); text-align: center;\">", r, g, b));
                                    html.append(cell.score);
                                }
                            } else {
                                html.append(String.format("<td class=\"%s\">", isLast ? " last" : ""));
                            }

                            html.append("</td>");
                            ++i;
                        }
                    }
                }
            }

            // Add stats
            html.append(getStatsHTML(standings, userId, false));

            html.append("</tr>");
        }

        // Problem statistics: all
        if (standings.config.standingsType == StandingsType.STANDARD) {
            html.append("<tr class=\"allstats\">");
            int colspan = 1;
            if (standings.config.type == StandingsTableType.ICPC && standings.config.showPenalty) {
                colspan++;
            }

            int userSpan = 2;
            if (standings.usersInfo != null) {
                userSpan = 1 + standings.usersInfo.header.size();
            }

            List<Integer> allStats = new ArrayList<>();
            html.append(String.format("<td colspan=\"%d\" class=\"fixed-side\">%s</td>", userSpan, getMessage("totalSolutions", standings.config.english)));
            int allCnt = standings.standings.stream().map(table -> table.submittedCnt).reduce((a, b) -> a + b).get();
            allStats.add(allCnt);
            html.append(String.format("<td colspan=\"%d\" class=\"stat fixed-side\" valign=\"center\">%d</td>", colspan, allCnt));
            for (StandingsTable table : standings.standings) {
                for (Problem problem : table.contest.getProblems()) {
                    int curCnt = table.submittedRuns.getOrDefault(problem.getId(), 0);
                    allStats.add(curCnt);
                    html.append(String.format("<td class=\"stat\" valign=\"center\">%d</td>", curCnt));
                }
            }

            html.append(String.format("<td colspan=\"%d\" class=\"stat\" valign=\"center\">%d</td>", colspan, standings.standings.stream().map(table -> table.submittedCnt).reduce((a, b) -> a + b).get()));
            html.append("</tr>");

            // Problem statistics: AC
            List<Integer> correctStats = new ArrayList<>();
            html.append("<tr class=\"allstats\">");
            html.append(String.format("<td colspan=\"%d\" class=\"fixed-side\">%s</td>", userSpan, getMessage("correctSolutions", standings.config.english)));
            allCnt = standings.standings.stream().map(table -> table.acceptedCnt).reduce(Integer::sum).get();
            correctStats.add(allCnt);
            html.append(String.format("<td colspan=\"%d\" class=\"stat fixed-side\" valign=\"center\">%d</td>", colspan, allCnt));
            for (StandingsTable table : standings.standings) {
                for (Problem problem : table.contest.getProblems()) {
                    int curCnt = table.acceptedRuns.getOrDefault(problem.getId(), 0);
                    correctStats.add(curCnt);
                    html.append(String.format("<td class=\"stat\" valign=\"center\">%d</td>", curCnt));
                }
            }

            html.append(String.format("<td colspan=\"%d\" class=\"stat\" valign=\"center\">%d</td>", colspan, standings.standings.stream().map(table -> table.acceptedCnt).reduce((a, b) -> a + b).get()));
            html.append("</tr>");

            // Problem statistics: percent
            html.append("<tr class=\"allstats\">");
            html.append(String.format("<td colspan=\"%d\" class=\" fixed-side\">%s</td>", userSpan, getMessage("correctSolutionsPercent", standings.config.english)));
            html.append(String.format("<td colspan=\"%d\" class=\"stat fixed-side\" valign=\"center\">%s</td>", colspan, getPercent(allStats.get(0), correctStats.get(0))));
            int ptr = 0;
            for (StandingsTable table : standings.standings) {
                for (Problem problem : table.contest.getProblems()) {
                    ptr++;
                    html.append(String.format("<td class=\"stat\" valign=\"center\">%s</td>", getPercent(allStats.get(ptr), correctStats.get(ptr))));
                }
            }

            html.append(String.format("<td colspan=\"%d\" class=\"stat\" valign=\"center\">%s</td>", colspan, getPercent(allStats.get(0), correctStats.get(0))));
            html.append("</tr>");
        }

        html.append("</tbody>");
        html.append("</table>");

        html.append("</div>\n");

        return html.toString();
    }

    public static String getPercent(int all, int correct) {
        if (all == 0) {
            return "0%";
        } else {
            return ((correct * 100 + all - 1) / all) + "%";
        }
    }

    public static String getStandingsHTMLFormatted(final String standingsHTML, final File header, final File footer) throws IOException {
        StringBuilder html = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(header)));
        String headerHTML = reader.lines().collect(Collectors.joining());
        reader.close();
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(footer)));
        String footerHTML = reader.lines().collect(Collectors.joining());
        reader.close();

        html.append(headerHTML);
        html.append(standingsHTML);
        html.append(footerHTML);

        return Jsoup.parse(html.toString()).toString();
    }

    public static String generateStandingsListPage(List<StandingsTableEntity> tables) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"col-md-12 text-center\">\n" +
                "        <h3>Доступные таблицы результатов</h3>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"main\">\n" +
                "        <div class=\"input-group\">\n" +
                "            <input type=\"text\" class=\"form-control\" placeholder=\"Поиск результатов\" id=\"search-field\">\n" +
                "            <div class=\"input-group-append\">\n" +
                "                <button class=\"btn btn-secondary\" type=\"button\" onclick=\"searchStandings()\">\n" +
                "                    <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-search\" viewBox=\"0 0 16 16\">\n" +
                "                        <path d=\"M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z\"/>\n" +
                "                    </svg>\n" +
                "                    <i class=\"fa fa-search\"></i>\n" +
                "                </button>\n" +
                "                <button class=\"btn btn-info btn-secondary\" type=\"button\" onclick=\"clearFilter()\">\n" +
                "                    Сбросить фильтр\n" +
                "                </button>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>");
        html.append("<div class=\"table-responsive fixed-table-body\">\n" +
                "        <table class=\"table table-striped table-bordered\">\n" +
                "            <tr>\n" +
                "                <th>№</th>\n" +
                "                <th>Название турнира</th>\n" +
//                "                <th>Список контестов</th>\n" +
                "                <th>Результаты</th>\n" +
                "            </tr>");

        int counter = 0;
        for (StandingsTableEntity table : tables) {
            html.append("<tr class=\"standings-entity\">\n");
            html.append(String.format("<td class=\"standings-id\">%d</td>\n", ++counter));
            html.append(String.format("<td class=\"standings-name\">%s</td>\n", table.standingsConfig.standingsName));
//            html.append(String.format("<td class=\"standings-contests\">%s</td>", String.join(", ", table.contestNames.values())));
            html.append(String.format("<td><a href=\"%s\">Результаты</a></td>\n", table.standingsName));
            html.append("</tr>\n");
        }

        html.append("</table>\n" +
                "</div></div></div>");

        return html.toString();
    }
}
