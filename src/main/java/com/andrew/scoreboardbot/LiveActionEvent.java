package com.andrew.scoreboardbot;

public record LiveActionEvent(
        String key,
        int atBatIndex,
        int eventIndex,
        String eventType,
        String description,
        Integer awayScore,
        Integer homeScore
)
{

}