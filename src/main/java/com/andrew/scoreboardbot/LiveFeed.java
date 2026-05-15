package com.andrew.scoreboardbot;

import java.util.List;

public record LiveFeed(
        String detailedState,
        String inningStateText,
        List<LivePlay> plays,
        List<LiveActionEvent> actionEvents,
        LinescoreSnapshot linescore,
        DecisionsSnapshot decisions
)
{

}