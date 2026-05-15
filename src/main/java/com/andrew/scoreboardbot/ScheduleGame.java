package com.andrew.scoreboardbot;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public record ScheduleGame(
        int gamePk,
        ZonedDateTime gameDate,
        String detailedState,
        String seriesDescription,
        String dayNight,
        String gameType,
        TeamSnapshot away,
        TeamSnapshot home,
        ProbablePitcherInfo awayProbable,
        ProbablePitcherInfo homeProbable,
        boolean freeGame,
        BroadcastInfo broadcasts,
        String doubleHeader,
        int gameNumber,
        LocalDate makeupFromDate,
        LocalDate resumedFromDate,
        ZonedDateTime resumeDateTime,
        int numberInSeries,
        LocalDate makeupDate,
        String disruptionReason,
        LocalDate originalDate
)
{

    public boolean isResumedSuspendedGame() { return resumedFromDate != null; }

    public boolean hasFutureResumeDate() { return resumeDateTime != null; }

    public boolean isDoubleheader()
    {
        return doubleHeader != null && !doubleHeader.equalsIgnoreCase("N") && gameNumber > 0;
    }

    public boolean isSplitDoubleheader()
    {
        return doubleHeader.equalsIgnoreCase("S");
    }

    public boolean isStraightDoubleheader()
    {
        return doubleHeader.equalsIgnoreCase("Y");
    }

    public boolean isMakeupGame()
    {
        return makeupFromDate != null;
    }

    public boolean isInterleague()
    {
        return away.teamInfo().league() != home.teamInfo.league();
    }

    public boolean isExhibition() { return gameType.equalsIgnoreCase("E"); }

    public boolean isSpringTraining() { return gameType.equalsIgnoreCase("S") || gameType.equalsIgnoreCase("E"); }

    public boolean isRegularSeason() { return gameType.equalsIgnoreCase("R"); }

    public boolean isPostseason()
    {
        return gameType.equalsIgnoreCase("F") ||
                gameType.equalsIgnoreCase("D") ||
                gameType.equalsIgnoreCase("L") ||
                gameType.equalsIgnoreCase("W");
    }

    public boolean isRegularOrPostseason() { return isRegularSeason() || isPostseason(); }

    public String awayProbableName() { return awayProbable != null ? awayProbable.fullName() : null; }
    public String homeProbableName() { return homeProbable != null ? homeProbable.fullName() : null; }
    public boolean isDisrupted() { return detailedState().equalsIgnoreCase("Postponed") ||
    detailedState().equalsIgnoreCase("Cancelled") ||
    detailedState().equalsIgnoreCase("Canceled") ||
    detailedState().equalsIgnoreCase("Suspended"); }

    public int getSeriesGameNumber()
    {
        return numberInSeries;
    }

    public boolean isPregame()
    {
        String state = detailedState == null ? "" : detailedState.toLowerCase();
        return state.contains("scheduled") || state.contains("pre-game")
                || state.contains("pregame")
                || state.contains("warmup");
    }

    public boolean hasStarted() { return !isPregame(); }

    public record TeamSnapshot(
            TeamInfo teamInfo,
            int wins,
            int losses,
            boolean splitSquad,
            Integer score
    )
    {

    }
}