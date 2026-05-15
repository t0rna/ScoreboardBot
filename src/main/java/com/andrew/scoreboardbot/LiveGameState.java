package com.andrew.scoreboardbot;

import java.util.Map;
import java.util.Set;

public record LiveGameState(
    int gamePk,
    long parentChannelId,
    long scheduleMessageId,
    long threadId,
    int lastPostedPlayIndex,
    Map<Integer, String> postedPlayDescriptions,
    String lastKnownDetailedState,
    String lastKnownInningState,
    boolean finalPosted,
    Set<String> postedActionEventKeys
)
{

}
