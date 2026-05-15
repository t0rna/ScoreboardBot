package com.andrew.scoreboardbot;

import java.util.List;

public record FormattedSchedule(
        String headerMessage,
        List<FormattedGameMessage> gameMessages
)
{

}
