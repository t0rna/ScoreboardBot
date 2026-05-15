package com.andrew.scoreboardbot;

import java.time.LocalDate;

public record PostedGameState(
        int gamePk,
        long messageId,
        long channelId,
        LocalDate scheduleDate,
        String awayProbableName,
        String homeProbableName,
        String detailedState,
        String gameContentHash,
        String postedProbableLine
)
{

}