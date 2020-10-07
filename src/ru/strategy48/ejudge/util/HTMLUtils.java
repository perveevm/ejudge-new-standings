package ru.strategy48.ejudge.util;

import org.jsoup.Jsoup;
import ru.strategy48.ejudge.contest.Contest;
import ru.strategy48.ejudge.contest.Problem;
import ru.strategy48.ejudge.standings.*;

import java.io.*;
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

    public static String getStandingsHTML(final StandingsTableAgregator standings) {
        StringBuilder html = new StringBuilder();

        // Add standings table name
        html.append(String.format("<div align=\"center\"><h3>%s</h3></div>\n", standings.config.standingsName + (standings.config.needFreeze ? " (Результаты заморожены)" : "")));

        // Add standings table header
        html.append("<table class=\"new-standings\">\n");
        html.append("<tbody>\n");

        // Add caption row
        html.append("<tr>\n");
        html.append("<th rowspan=\"2\"><div class=\"new-standings-cell\" valign=\"middle\">Место</div></th>\n");
        html.append("<th rowspan=\"2\">Участник</th>\n");

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
            html.append(String.format("<td>%s</td>\n", standings.users.get(userId).getName()));

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

                    for (StandingsTableCell cell : row.cells.values()) {
                        int time = cell.time / 60;

                        if (cell.freezed) {
                            html.append("<td class=\"freezed\">");
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
                            html.append("<td class=\"running\">");
                            html.append("TODO");
                        } else if (cell.solved) {
                            if (cell.firstAC) {
                                html.append("<td class=\"firstAC\">");
                            } else {
                                html.append("<td class=\"ok\">");
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
                            html.append("<td class=\"rj\">");

                            if (standings.config.type == StandingsTableType.ICPC) {
                                html.append("-");
                                html.append(cell.attempts);

                                if (standings.config.showPenalty) {
                                    html.append(String.format("<div>%d:%02d</div>", time / 60, time % 60));
                                }
                            } else {
                                html.append(cell.score);
                            }
                        } else {
                            html.append("<td>");
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

        html.append("<td colspan=\"2\">Всего решений</td>");
        html.append(String.format("<td colspan=\"%d\" class=\"stat\" valign=\"center\">%d</td>", colspan, standings.standings.stream().map(table -> table.submittedCnt).reduce((a, b) -> a + b).get()));
        for (StandingsTable table : standings.standings) {
            for (Problem problem : table.contest.getProblems()) {
                html.append(String.format("<td class=\"stat\" valign=\"center\">%d</td>", table.submittedRuns.getOrDefault(problem.getId(), 0)));
            }
        }

        html.append(String.format("<td colspan=\"%d\" class=\"stat\" valign=\"center\">%d</td>", colspan, standings.standings.stream().map(table -> table.submittedCnt).reduce((a, b) -> a + b).get()));
        html.append("</tr>");

        // Problem statistics: AC
        html.append("<tr class=\"allstats\">");
        html.append("<td colspan=\"2\">Правильных решений</td>");
        html.append(String.format("<td colspan=\"%d\" class=\"stat\" valign=\"center\">%d</td>", colspan, standings.standings.stream().map(table -> table.acceptedCnt).reduce((a, b) -> a + b).get()));
        for (StandingsTable table : standings.standings) {
            for (Problem problem : table.contest.getProblems()) {
                html.append(String.format("<td class=\"stat\" valign=\"center\">%d</td>", table.acceptedRuns.getOrDefault(problem.getId(), 0)));
            }
        }

        html.append(String.format("<td colspan=\"%d\" class=\"stat\" valign=\"center\">%d</td>", colspan, standings.standings.stream().map(table -> table.acceptedCnt).reduce((a, b) -> a + b).get()));
        html.append("</tr>");

        html.append("</tbody>");
        html.append("</table>");

        return html.toString();
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
