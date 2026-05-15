package com.andrew.scoreboardbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class PlayByPlayFormatter
{
    public MessageEmbed formatGameAdvisory(String stateText) { return new EmbedBuilder().setTitle("Game Advisory").setDescription("Status Change - " + stateText).build(); }

    public MessageEmbed formatInningStateUpdate(String inningStateText) { return new EmbedBuilder().setTitle("Inning State Updated").setDescription(inningStateText).build(); }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    public MessageEmbed formatPlay(LivePlay play, ScheduleGame game)
    {
        if(play == null) return null;
        boolean noDescription = isBlank(play.description()) || play.description().isBlank();
        boolean noHitInfo = play.hitSpeed() == null && play.hitAngle() == null && play.hitDistance() == null;

        if(noDescription && noHitInfo) return null;

        if(play.scoringPlay()) return formatScoringPlay(play, game);
        if(isOutEvent(play)) return formatOutPlay(play, game);
        return formatGenericPlay(play);
    }

    private MessageEmbed formatGenericPlay(LivePlay play)
    {
        EmbedBuilder eb = new EmbedBuilder();

        if(play.description() != null && !play.description().isBlank())
            eb.setDescription(play.description());

        appendHitInfo(eb, play);

        if(eb.isEmpty()) return null;

        return eb.build();
    }

    private MessageEmbed formatOutPlay(LivePlay play, ScheduleGame game)
    {
        EmbedBuilder eb = new EmbedBuilder();
        if(play.description() != null && !play.description().isBlank())
            eb.setDescription(play.description());

        eb.addField("Outs", formatOuts(play), true);
        appendScoreIfPresent(eb, play, game);
        appendHitInfo(eb, play);

        if(eb.isEmpty()) return null;
        return eb.build();
    }

    private MessageEmbed formatScoringPlay(LivePlay play, ScheduleGame game)
    {
        EmbedBuilder eb = new EmbedBuilder().setTitle("Run Scored!");

        if(play.description() != null && !play.description().isBlank())
            eb.setDescription(play.description());

        appendHitInfo(eb, play);
        appendScoreIfPresent(eb, play, game);

        if(eb.isEmpty()) return null;
        return eb.build();
    }

    public MessageEmbed formatGameFinal(ScheduleGame game, LiveFeed feed, String summaryLine, String decisionsLine, String linescoreBlock)
    {
        EmbedBuilder eb = new EmbedBuilder().setTitle("Game Over!").addField("Summary", summaryLine, false);
        if(decisionsLine != null && !decisionsLine.isBlank()) eb.addField("Decisions", decisionsLine, false);
        if(linescoreBlock != null && !linescoreBlock.isBlank()) eb.addField("Final Scorecard", "```" + linescoreBlock + "```", false);
        return eb.build();
    }

    private boolean isOutEvent(LivePlay play)
    {
        return play.outsAdded() > 0 || play.eventType().equalsIgnoreCase("strikeout")
                || play.eventType().equalsIgnoreCase("field_out") || play.eventType().equalsIgnoreCase("force_out")
                || play.eventType().equalsIgnoreCase("double_play") || play.eventType().equalsIgnoreCase("triple_play");
    }

    private String formatOuts(LivePlay play) { return play.outsAfterPlay() + " (+" + play.outsAdded() +")"; }

    private void appendScoreIfPresent(EmbedBuilder eb, LivePlay play, ScheduleGame game)
    {
        if(play.awayScore() != null && play.homeScore() != null)
        {
            eb.addField("Score", formatScore(game, play.awayScore(), play.homeScore()), true);
        }
    }

    private String formatScore(ScheduleGame game, int awayScore, int homeScore)
    {
        return game.away().teamInfo().emoji().getAsMention() + " " + awayScore + " - " + homeScore + " " + game.home().teamInfo().emoji().getAsMention();
    }

    private void appendHitInfo(EmbedBuilder eb, LivePlay play)
    {
        if(play.hitSpeed() != null || play.hitAngle() != null || play.hitDistance() != null)
        {
            StringBuilder sb = new StringBuilder();

            if(play.hitSpeed() != null)
            {
                sb.append("Ball left the bat at a speed of ")
                        .append(play.hitSpeed()).append(" mph");
            }
            if(play.hitAngle() != null)
            {
                if(sb.length() > 0) sb.append(" at a ");
                sb.append(play.hitAngle()).append("° angle");
            }
            if(play.hitDistance() != null)
            {
                if(sb.length() > 0) sb.append(", and travelled ");
                sb.append(play.hitDistance()).append(" feet");
            }
            if(sb.length() > 0) sb.append(".");
            eb.addField("Hit Info", sb.toString(), false);
        }
    }

    public MessageEmbed formatPlayCorrection(LivePlay play)
    {
        return new EmbedBuilder().setTitle("Play Updated").setDescription(play.description()).build();
    }

    public MessageEmbed formatActionEvent(LiveActionEvent event)
    {
        return new EmbedBuilder().setDescription(event.description()).build();
    }
}
