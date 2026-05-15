package com.andrew.scoreboardbot;

public record DecisionsSnapshot(
        PitcherDecision winner,
        PitcherDecision loser,
        PitcherDecision save
)
{
    public record PitcherDecision(
            String fullName,
            String summary
    )
    {

    }
}
