package com.andrew.scoreboardbot;

import java.util.List;

public record LinescoreSnapshot(
        Integer awayRuns,
        Integer homeRuns,
        Integer awayHits,
        Integer homeHits,
        Integer awayErrors,
        Integer homeErrors,
        List<InningLine> innings
)
{
    public record InningLine(
            int inning,
            Integer awayRuns,
            Integer homeRuns
    )
    {

    }
}
