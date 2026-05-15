package com.andrew.scoreboardbot;

import net.dv8tion.jda.api.entities.emoji.CustomEmoji;


public record TeamInfo(
    int teamId,
    String name,
    String shortName,
    League league,
    League springLeague,
    CustomEmoji emoji,
    Roles role,
    boolean external,
    String abbreviation
)
{

    public String emojiMention()
    {
        return emoji != null ? emoji.getAsMention() : "";
    }

    public boolean isMlbTeam() { return !external; }

}
