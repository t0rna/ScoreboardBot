package com.andrew.scoreboardbot;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ThreadTitleFormatter
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M/d/yy");

    public String format(ScheduleGame game)
    {
        String away = game.away().teamInfo().abbreviation();
        String home = game.home().teamInfo().abbreviation();

        return switch(game.gameType().toUpperCase())
        {
            case "A" -> "All-Star Game";
            case "F" -> game.away().teamInfo().league().equals(League.AMERICAN) ? "ALWC GM " + game.getSeriesGameNumber() + " - " + away + " @ " + home : "NLWC GM " + game.getSeriesGameNumber() + " - " + away + " @ " + home;
            case "D" -> game.away().teamInfo().league().equals(League.AMERICAN) ? "ALDS GM " + game.getSeriesGameNumber() + " - " + away + " @ " + home : "NLDS GM " + game.getSeriesGameNumber() + " - " + away + " @ " + home;
            case "L" -> game.away().teamInfo().league().equals(League.AMERICAN) ? "ALCS GM " + game.getSeriesGameNumber() + " - " + away + " @ " + home : "NLCS GM " + game.getSeriesGameNumber() + " - " + away + " @ " + home;
            case "W" -> "WS GM " + game.getSeriesGameNumber() + " - " + away + " @ " + home;
            default -> away + " @ " + home + " - " + game.gameDate().withZoneSameInstant(ZoneId.of("America/New_York")).format(DATE_FORMAT);
        };
    }
}
