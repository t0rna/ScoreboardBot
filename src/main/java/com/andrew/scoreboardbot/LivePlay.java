package com.andrew.scoreboardbot;

public record LivePlay(
        int atBatIndex,
        String eventType,
        String description,
        boolean scoringPlay,
        int outsAfterPlay,
        int outsAdded,
        Integer awayScore,
        Integer homeScore,
        String hitSpeed,
        String hitAngle,
        String hitDistance,
        String inningHalf,
        int inning
)
{

}
