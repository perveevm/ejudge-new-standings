package ru.strategy48.ejudge.util;

import org.jsoup.Jsoup;
import ru.strategy48.ejudge.contest.Contest;
import ru.strategy48.ejudge.contest.Problem;
import ru.strategy48.ejudge.standings.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HTMLUtils {
    private static String getStatsHTML(final StandingsTableAgregator standings, final int userId) {
        StringBuilder html = new StringBuilder();

        if (standings.config.type == StandingsTableType.ICPC) {
            html.append("<td class=\"stat\">");
            html.append(standings.solved.get(userId));
            html.append("</td>");

            if (standings.config.showPenalty) {
                html.append("<td class=\"stat\">");
                html.append(standings.penalty.get(userId));
                html.append("</td>");
            }
        } else {
            html.append("<td class=\"stat\">");
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

    public static String getStandingsHTML(final StandingsTableAgregator standings) {
        StringBuilder html = new StringBuilder();

        // Add standings table name
        html.append(String.format("<div align=\"center\"><h3>%s</h3></div>\n", standings.config.standingsName + (standings.config.needFreeze ? " (Результаты заморожены)" : "")));

        if (standings.config.isOfficial) {
            Date start = standings.config.startDate;
            Date end = standings.config.endDate;
            Date now = new Date(System.currentTimeMillis());

            long contestDuration = end.getTime() - start.getTime();
            long curDuration = now.getTime() - start.getTime();

            html.append(String.format("<div align=\"center\"><h4>%s из %s<br/>", formatDuration(Math.min(curDuration, contestDuration)), formatDuration(contestDuration)));
            if (curDuration >= contestDuration) {
                html.append("ЗАВЕРШЕНО");
            } else {
                html.append("В ПРОЦЕССЕ");
            }
            html.append("</h4></div>");
        }

        // Add standings table header
        html.append("<table class=\"new-standings\">\n");
        html.append("<tbody>\n");

        // Add caption row
        html.append("<tr>\n");
        html.append("<th rowspan=\"2\"><div class=\"new-standings-cell\" valign=\"middle\">Место</div></th>\n");

        if (standings.usersInfo == null) {
            html.append("<th rowspan=\"2\">Участник</th>\n");
        } else {
            for (String caption : standings.usersInfo.header) {
                html.append(String.format("<th rowspan=\"2\">%s</th>", caption));
            }
        }

        if (standings.config.type == StandingsTableType.ICPC) {
            html.append("<th rowspan=\"2\">Решено задач</th>\n");
            if (standings.config.showPenalty) {
                html.append("<th rowspan=\"2\">Штраф</th>\n");
            }
        } else {
            html.append("<th rowspan=\"2\">Баллы</th>\n");
        }

        for (Contest contest : standings.contests) {
            html.append(String.format("<th colspan=\"%d\">%s</th>\n", contest.getProblems().size(), standings.config.contestNames.get(contest.getContestId())));
        }

        if (standings.config.type == StandingsTableType.ICPC) {
            html.append("<th rowspan=\"2\">Решено задач</th>\n");
            if (standings.config.showPenalty) {
                html.append("<th rowspan=\"2\">Штраф</th>\n");
            }
        } else {
            html.append("<th rowspan=\"2\">Баллы</th>\n");
        }

        html.append("</tr>\n");
        html.append("<tr>\n");

        for (Contest contest : standings.contests) {
            for (Problem problem : contest.getProblems()) {
                html.append(String.format("<th title=\"%s\">%s</th>\n", problem.getLongName(), problem.getShortName()));
            }
        }

        html.append("</tr>\n");

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

            html.append(String.format("<tr class=\"%s\">\n", rowType));

            if (minPlace == maxPlace) {
                html.append(String.format("<td class=\"stat\" valign=\"center\">%d</td>", minPlace));
            } else {
                html.append(String.format("<td class=\"stat\" valign=\"center\">%d-%d</td>", minPlace, maxPlace));
            }

            // Add name
            if (standings.usersInfo == null) {
                if (standings.idToLogin != null) {
                    String login = standings.idToLogin.get(standings.users.get(userId).getId());
                    html.append(String.format("<td>%s</td>\n", login));
                } else {
                    html.append(String.format("<td>%s</td>\n", standings.users.get(userId).getName()));
                }
            } else {
                String login = standings.idToLogin.get(standings.users.get(userId).getId());
                if (!standings.usersInfo.fields.containsKey(login)) {
                    for (int i = 0; i < standings.usersInfo.header.size(); i++) {
                        html.append(String.format("<td>%s</td>\n", login));
                    }
                } else {
                    for (String param : standings.usersInfo.fields.get(login)) {
                        html.append(String.format("<td>%s</td>\n", param));
                    }
                }
            }

            // Add stats
            html.append(getStatsHTML(standings, userId));

            // Add results
            for (StandingsTable table : standings.standings) {
                int rowId = table.userToRow.getOrDefault(userId, -1);

                if (rowId == -1) {
                    for (int i = 0; i < table.contest.getProblems().size(); i++) {
                        html.append("<td></td>\n");
                    }
                } else {
                    StandingsTableRow row = table.sortedRows.get(rowId);
                    int cur = 0;

                    for (StandingsTableCell cell : row.cells.values()) {
                        int time = cell.time / 60;
                        cur++;

                        String style = "";
                        // TODO: borders
//                        if (cur == row.cells.size()) {
//                            style = " style=\"border-right: solid black 3px;\"";
//                        }

                        if (cell.freezed) {
                            html.append(String.format("<td class=\"freezed\">%s", style));
                            time = cell.freezedTime / 60;

                            if (cell.freezedSolved) {
                                if (standings.config.type == StandingsTableType.ICPC) {
                                    html.append("+");
                                    if (cell.freezedAttempts != 0) {
                                        html.append(cell.freezedAttempts);
                                    }

                                    if (standings.config.showPenalty) {
                                        html.append(String.format("<div>%d:%02d</div>", time / 60, time % 60));
                                    }
                                } else {
                                    html.append(cell.freezedScore);
                                }
                            } else if (cell.attempts != 0) {
                                if (standings.config.type == StandingsTableType.ICPC) {
                                    if (cell.freezedAttempts != 0) {
                                        html.append("-");
                                        html.append(cell.freezedAttempts);

                                        if (standings.config.showPenalty) {
                                            html.append(String.format("<div>%d:%02d</div>", time / 60, time % 60));
                                        }
                                    }
                                } else {
                                    html.append(cell.freezedScore);
                                }
                            }

                        } else if (cell.running) {
                            // TODO: running cells
                            html.append(String.format("<td class=\"running\"%s>", style));
                            html.append("TODO");
                        } else if (cell.solved) {
                            if (cell.firstAC) {
                                html.append(String.format("<td class=\"firstAC\"%s>", style));
                            } else {
                                html.append(String.format("<td class=\"ok\"%s>", style));
                            }

                            if (standings.config.type == StandingsTableType.ICPC) {
                                html.append("+");
                                if (cell.attempts != 0) {
                                    html.append(cell.attempts);
                                }

                                if (standings.config.showPenalty) {
                                    html.append(String.format("<div>%d:%02d</div>", time / 60, time % 60));
                                }
                            } else {
                                html.append(cell.score);
                            }
                        } else if (cell.attempts != 0) {
                            if (standings.config.type == StandingsTableType.ICPC) {
                                html.append(String.format("<td class=\"rj\"%s>", style));
                                html.append("-");
                                html.append(cell.attempts);

                                if (standings.config.showPenalty) {
                                    html.append(String.format("<div>%d:%02d</div>", time / 60, time % 60));
                                }
                            } else {
                                int r = (int) (247 + (208 - 247) * Math.sqrt((double) cell.score / 100.0));
                                int g = (int) (94 + (240 - 94) * Math.sqrt((double) cell.score / 100.0));
                                int b = (int) (99 + (208 - 99) * Math.sqrt((double) cell.score / 100.0));
                                html.append(String.format("<td style=\"background-color: rgb(%d, %d, %d); text-align: center;\">", r, g, b));
                                html.append(cell.score);
                            }
                        } else {
                            html.append(String.format("<td%s>", style));
                        }

                        html.append("</td>");
                    }
                }
            }

            // Add stats
            html.append(getStatsHTML(standings, userId));

            html.append("</tr>");
        }

        // Problem statistics: all
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
        html.append(String.format("<td colspan=\"%d\">Всего решений</td>", userSpan));
        int allCnt = standings.standings.stream().map(table -> table.submittedCnt).reduce((a, b) -> a + b).get();
        allStats.add(allCnt);
        html.append(String.format("<td colspan=\"%d\" class=\"stat\" valign=\"center\">%d</td>", colspan, allCnt));
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
        html.append(String.format("<td colspan=\"%d\">Правильных решений</td>", userSpan));
        allCnt = standings.standings.stream().map(table -> table.acceptedCnt).reduce(Integer::sum).get();
        correctStats.add(allCnt);
        html.append(String.format("<td colspan=\"%d\" class=\"stat\" valign=\"center\">%d</td>", colspan, allCnt));
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
//        html.append("<tr class=\"allstats\">");
//        html.append(String.format("<td colspan=\"%d\">Процент правильных решений</td>", userSpan));
//        html.append(String.format("<td colspan=\"%d\" class=\"stat\" valign=\"center\">%s</td>", colspan, getPercent(allStats.get(0), correctStats.get(0))));
//        int ptr = 0;
//        for (StandingsTable table : standings.standings) {
//            for (Problem problem : table.contest.getProblems()) {
//                ptr++;
//                html.append(String.format("<td class=\"stat\" valign=\"center\">%s</td>", getPercent(allStats.get(ptr), correctStats.get(ptr))));
//            }
//        }
//
//        html.append(String.format("<td colspan=\"%d\" class=\"stat\" valign=\"center\">%s</td>", colspan, getPercent(allStats.get(0), correctStats.get(0))));
//        html.append("</tr>");

        html.append("</tbody>");
        html.append("</table>");

        return html.toString();
    }

    public static String getPercent(int all, int correct) {
        return String.valueOf(correct * 100 / all) + "%";
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
}
