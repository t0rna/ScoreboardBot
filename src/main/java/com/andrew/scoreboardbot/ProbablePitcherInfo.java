package com.andrew.scoreboardbot;

public record ProbablePitcherInfo(Integer playerId, String fullName, Integer wins, Integer losses, String era)
{
    public static ProbablePitcherInfo tbd()
    {
        return new ProbablePitcherInfo(null,"TBD", null, null, null);
    }

    public String lastNameOnly()
    {
        if(fullName == null || fullName.isBlank() || fullName.equals("TBD"))
            return "TBD";
        int idx = fullName.indexOf(' ');
        return idx >= 0 ? fullName.substring(idx + 1) : fullName;
    }

    public String formatLine()
    {
        if(wins == null || losses == null || era == null || era.isBlank())
            return lastNameOnly();

        boolean noRealStats = (wins == null || losses == null || era == null) || ((wins == 0 && losses == 0) && (era.equals("-.--") || era.isBlank()));
        if(noRealStats) return lastNameOnly();
        return "%s (%d-%d, %s ERA)".formatted(lastNameOnly(), wins, losses, era);
    }
}
