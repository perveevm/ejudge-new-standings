package ru.strategy48.ejudge.util;

import ru.strategy48.ejudge.contest.Contest;
import ru.strategy48.ejudge.contest.Problem;
import ru.strategy48.ejudge.standings.*;

import java.io.*;
import java.util.stream.Collectors;

public class HTMLUtils {
    public static String getStandingsHTML(final StandingsTableAgregator standings) {
        StringBuilder html = new StringBuilder();

        // Add standings table name
        html.append(String.format("<div align=\"center\"><h3>%s</h3></div>\n", standings.config.standingsName));

        // Add standings table header
        html.append("<table class=\"new-standings\">\n");
        html.append("<tbody>\n");

        // Add caption row
        html.append("<tr>\n");
        html.append("<th rowspan=\"2\"><div class=\"new-standings-cell\" valign=\"middle\">Место</div></th>\n");
        html.append("<th rowspan=\"2\">Участник</th>\n");
        for (Contest contest : standings.contests) {
            html.append(String.format("<th colspan=\"%d\">%s</th>\n", contest.getProblems().size(), standings.config.contestNames.get(contest.getContestId())));
        }

        if (standings.config.type == StandingsTableType.ICPC) {
            html.append("<th rowspan=\"2\">Решено задач</th>\n");
            html.append("<th rowspan=\"2\">Штраф</th>\n");
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
        for (Integer userId : standings.sortedUsers) {
            html.append("<tr>\n");

            // Add place
            html.append("<td>???</td>\n");

            // Add name
            html.append(String.format("<td>%s</td>\n", standings.users.get(userId).getName()));

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
                        if (cell.freezed) {
                            // TODO: freezed cells
                            html.append("<td class=\"freezed\">");
                            html.append("TODO");
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

                                html.append(String.format("<div>%d:%02d</div>", cell.time / 60, cell.time % 60));
                            } else {
                                // TODO: IOI-cells
                                html.append("TODO");
                            }
                        } else if (cell.attempts != 0) {
                            html.append("<td class=\"rj\">");

                            if (standings.config.type == StandingsTableType.ICPC) {
                                html.append("-");
                                html.append(cell.attempts);

                                html.append(String.format("<div>%d:%02d</div>", cell.time / 60, cell.time % 60));
                            } else {
                                // TODO: IOI-cells
                                html.append("TODO");
                            }
                        } else {
                            html.append("<td>");
                        }

                        html.append("</td>");
                    }
                }
            }

            if (standings.config.type == StandingsTableType.ICPC) {
                html.append("<td>");
                html.append(standings.solved.get(userId));
                html.append("</td>");

                html.append("<td>");
                html.append(standings.penalty.get(userId));
                html.append("</td>");
            } else {
                html.append("<td>");
                html.append(standings.score.get(userId));
                html.append("</td>");
            }

            html.append("</tr>");
        }

        html.append("</tbody>");
        html.append("</table>");

        return html.toString();
    }

    public static String getStandingsHTMLFormatted(final String standingsHTML, final File header, final File footer) throws FileNotFoundException, IOException {
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

        return html.toString();
    }
}
