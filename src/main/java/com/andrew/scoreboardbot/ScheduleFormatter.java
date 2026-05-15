package com.andrew.scoreboardbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ScheduleFormatter
{
    private static final DateTimeFormatter HEADER_FORMAT = DateTimeFormatter.ofPattern("MMMM d");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");
    private final JDA jda;

    public ScheduleFormatter(JDA jda)
    {
        this.jda = jda;
    }

    public String formatDateHeader(LocalDate date, boolean springTraining)
    {
        String leftLeagueEmoji = springTraining ? jda.getEmojiById(1337167447005855826L).getAsMention() : jda.getEmojiById(344302564315234304L).getAsMention();
        String rightLeagueEmoji = springTraining ? jda.getEmojiById(1337167445940633640L).getAsMention() : jda.getEmojiById(344302519167483904L).getAsMention();
        String mlbEmoji = jda.getEmojiById(360520397235552256L).getAsMention();

        return "⚾ " + mlbEmoji + " " + leftLeagueEmoji + " " + date.format(DateTimeFormatter.ofPattern("MMMM d")) + " " + rightLeagueEmoji + " \uD83D\uDCC5 ⚾";
    }

    public FormattedGameMessage formatGame(ScheduleGame game, String probableLineOverride)
    {
        String sectionHeader = formatSectionHeader(game);

        if(game.isResumedSuspendedGame())
        {
            return new FormattedGameMessage(game.gamePk(),
                    sectionHeader + "\n" +
                            formatSuspendedGameMatchupLine(game) + "\n"
                            + "```" + formatSuspendedGameDetailLine(game) + "```");
        }

        if(game.isDisrupted())
        {
            String matchupLine = formatMatchupLine(game);
            String disruptionLine = formatDisruptionLine(game);
            return new FormattedGameMessage(
                    game.gamePk(),
                    sectionHeader + "\n~~" + matchupLine + "~~\n```" + disruptionLine + "```"
            );
        }

        String matchupLine = formatMatchupLine(game);
        String probableLine = probableLineOverride != null ? probableLineOverride : formatProbableLine(game);

        return new FormattedGameMessage(
                game.gamePk(),
                sectionHeader + "\n" + matchupLine + probableLine
        );
    }

    public String formatProbableLine(ScheduleGame game)
    {
        return "```" + game.awayProbable().formatLine() + " vs " + game.homeProbable().formatLine() + "```";
    }

    public FormattedGameMessage formatGame(ScheduleGame game)
    {
        return formatGame(game, null);
    }

    private String formatSuspendedGameMatchupLine(ScheduleGame game)
    {
        String awayRole = roleMention(game.away().teamInfo());
        String homeRole = roleMention(game.home().teamInfo());

        String awayEmoji = emoji(game.away().teamInfo());
        String homeEmoji = emoji(game.home().teamInfo());

        String awayScore = game.away().score() != null ? String.valueOf(game.away().score()) : "0";
        String homeScore = game.home().score() != null ? String.valueOf(game.home().score()) : "0";

        return awayRole + " " + awayEmoji + " " + awayScore + "\n" + homeRole + " " +  homeEmoji + " " + homeScore;
    }

    private String formatSuspendedGameDetailLine(ScheduleGame game)
    {
        if(game.resumedFromDate() != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Completion of suspended game from ")
                    .append(formatShortDate(game.resumedFromDate()));

            String resumeTime = game.gameDate()
                    .withZoneSameInstant(ZoneId.of("America/New_York"))
                    .format(DateTimeFormatter.ofPattern("h:mm a"))
                    .toUpperCase();

            sb.append("; to resume at ").append(resumeTime).append(" ET.");
            return sb.toString();
        }
        return "Suspended game.";
    }

    public String formatDisruptionLine(ScheduleGame game)
    {
        String state = game.detailedState();
        if(state.equalsIgnoreCase("Cancelled") || state.equalsIgnoreCase("Canceled"))
            return game.disruptionReason() != null ? "Canceled due to " + game.disruptionReason() + "." : "Canceled.";
        if(state.equalsIgnoreCase("Postponed"))
        {
            String reason = game.disruptionReason();
            LocalDate makeupDate = game.makeupDate();
            if(reason == null)
            {
                return makeupDate != null ? "Postponed; makeup on " + formatShortDate(makeupDate) + "." : "Postponed; makeup TBD.";
            }
            return makeupDate != null ? "Postponed due to " + reason.toLowerCase() + "; makeup on " + formatShortDate(makeupDate) + "." : "Postponed due to " + reason.toLowerCase() + "; makeup TBD.";
        }
        if(state.equalsIgnoreCase("Suspended"))
            return "Suspended.";
        return state + ".";
    }

    private String formatSectionHeader(ScheduleGame game)
    {
        if(!game.home().teamInfo().isMlbTeam() || !game.away().teamInfo().isMlbTeam())
            return jda.getEmojiById(360520397235552256L).getAsMention() + " " + Text.EXHIBITION.getText();
        if(game.isSpringTraining())
        {
            if(game.away().teamInfo().springLeague().equals(game.home().teamInfo().springLeague()) && game.away().teamInfo().springLeague().equals(League.GRAPEFRUIT))
                return jda.getEmojiById(1337167447005855826L).getAsMention() + " " + Text.GRAPEFRUIT.getText();
            if(game.away().teamInfo().springLeague().equals(game.home().teamInfo().springLeague()) && game.away().teamInfo().springLeague().equals(League.CACTUS))
                return jda.getEmojiById(1337167445940633640L).getAsMention() + " " + Text.CACTUS.getText();
            if(game.away().teamInfo().springLeague().equals(League.GRAPEFRUIT))
                return jda.getEmojiById(1337167447005855826L).getAsMention() + " " + jda.getEmojiById(1337167445940633640L).getAsMention() + " " + Text.INTERLEAGUE.getText();
            return jda.getEmojiById(1337167445940633640L).getAsMention() + " " + jda.getEmojiById(1337167447005855826L).getAsMention() + " " + Text.INTERLEAGUE.getText();
        }
        if(game.gameType().equalsIgnoreCase("A"))
        {
            return game.away().teamInfo().emoji() + " " + jda.getEmojiById(360520397235552256L).getAsMention() + " " + game.home().teamInfo().emoji() + " ᴍʟʙ ᴀʟʟ-ꜱᴛᴀʀ ɢᴀᴍᴇ";
        }
        if (game.gameType().equalsIgnoreCase("F"))
        {
            // wild card
            String result = "";
            if(game.away().teamInfo().league().equals(League.AMERICAN))
            {
                result += jda.getEmojiById(344302519167483904L).getAsMention() + " " + Text.AL_WC.getText() + " ";
            }
            else
            {
                result += jda.getEmojiById(344302564315234304L).getAsMention() + " " + Text.NL_WC.getText() + " ";
            }
            result += Text.GAME.getText() + " ";
            switch(game.getSeriesGameNumber())
            {
                case 1:
                    result += Text.ONE.getText();
                    break;
                case 2:
                    result += Text.TWO.getText();
                    break;
                case 3:
                    result += Text.THREE.getText();
                    break;
            }
            return result;
        }
        if(game.gameType().equalsIgnoreCase("D"))
        {
            // LDS
            String result = "";
            if(game.away().teamInfo().league().equals(League.AMERICAN))
            {
                result += jda.getEmojiById(344302519167483904L).getAsMention() + " " + Text.ALDS.getText() + " ";
            }
            else
            {
                result += jda.getEmojiById(344302564315234304L).getAsMention() + " " + Text.NLDS.getText() + " ";
            }
            result += Text.GAME.getText() + " ";
            switch(game.getSeriesGameNumber())
            {
                case 1:
                    result += Text.ONE.getText();
                    break;
                case 2:
                    result += Text.TWO.getText();
                    break;
                case 3:
                    result += Text.THREE.getText();
                    break;
                case 4:
                    result += Text.FOUR.getText();
                    break;
                case 5:
                    result += Text.FIVE.getText();
                    break;
            }
            return result;
        }
        if(game.gameType().equalsIgnoreCase("L"))
        {
            // LCS
            String result = "";
            if(game.away().teamInfo().league().equals(League.AMERICAN))
            {
                result += jda.getEmojiById(344302519167483904L).getAsMention() + " " + Text.ALCS.getText() + " ";
            }
            else
            {
                result += jda.getEmojiById(344302564315234304L).getAsMention() + " " + Text.ALCS.getText() + " ";
            }
            result += Text.GAME.getText() + " ";
            switch(game.getSeriesGameNumber())
            {
                case 1:
                    result += Text.ONE.getText();
                    break;
                case 2:
                    result += Text.TWO.getText();
                    break;
                case 3:
                    result += Text.THREE.getText();
                    break;
                case 4:
                    result += Text.FOUR.getText();
                    break;
                case 5:
                    result += Text.FIVE.getText();
                    break;
                case 6:
                    result += Text.SIX.getText();
                    break;
                case 7:
                    result += Text.SEVEN.getText();
                    break;
            }
            return result;
        }
        if(game.gameType().equalsIgnoreCase("W"))
        {
            // world series
            String result = jda.getEmojiById(360997365601271808L).getAsMention() + " " + Text.WS.getText() + " " + Text.GAME.getText() + " ";
            switch(game.getSeriesGameNumber())
            {
                case 1:
                    result += Text.ONE.getText();
                    break;
                case 2:
                    result += Text.TWO.getText();
                    break;
                case 3:
                    result += Text.THREE.getText();
                    break;
                case 4:
                    result += Text.FOUR.getText();
                    break;
                case 5:
                    result += Text.FIVE.getText();
                    break;
                case 6:
                    result += Text.SIX.getText();
                    break;
                case 7:
                    result += Text.SEVEN.getText();
                    break;
            }
            return result;
        }
        if(game.isInterleague())
        {
            if (game.away().teamInfo().league().equals(League.AMERICAN))
            {
                return jda.getEmojiById(344302519167483904L).getAsMention() + " " + jda.getEmojiById(344302564315234304L).getAsMention() + " " + Text.INTERLEAGUE.getText();
            }
            else
            {
                return jda.getEmojiById(344302564315234304L).getAsMention() + " " + jda.getEmojiById(344302519167483904L).getAsMention() + " " + Text.INTERLEAGUE.getText();
            }
        }
        return switch(game.away().teamInfo().league())
        {
            case AMERICAN -> jda.getEmojiById(344302519167483904L).getAsMention() + " " + Text.AMERICAN.getText();
            case NATIONAL -> jda.getEmojiById(344302564315234304L).getAsMention() + " " + Text.NATIONAL.getText();
            // below not needed but compiler complained without it
            case GRAPEFRUIT -> null;
            case CACTUS -> null;
        };
    }

    private String discordRelativeTime(ScheduleGame game)
    {
        long epochSeconds = game.gameDate().toEpochSecond();
        return "<t:" + epochSeconds + ":R>";
    }

    private String formatMatchupLine(ScheduleGame game)
    {
        ScheduleGame.TeamSnapshot away = game.away();
        ScheduleGame.TeamSnapshot home = game.home();

        String awayEmoji = emoji(away.teamInfo());
        String homeEmoji = emoji(home.teamInfo());

        String awayRole = roleMention(away.teamInfo());
        String homeRole = roleMention(home.teamInfo());

        String awayRecord = "(" + away.wins() + "-" + away.losses() + ")";
        String homeRecord = "(" + home.wins() + "-" + home.losses() + ")";

        String time = game.gameDate().withZoneSameInstant(ZoneId.of("America/New_York")).format(TIME_FORMAT);

        StringBuilder sb = new StringBuilder();
        sb.append(awayRole).append(" ")
                .append(awayEmoji).append(" ");

        if(!game.isPostseason() || !game.isExhibition())
        {
            sb.append(awayRecord);
        }

        sb.append(splitSquadSuffix(away))
                .append(" @ ")
                .append(homeRole).append(" ")
                .append(homeEmoji).append(" ");

        if(!game.isPostseason() || !game.isExhibition())
        {
            sb.append(homeRecord);
        }

        sb.append(splitSquadSuffix(home))
                .append(" ");

        if(shouldShowStartTime(game))
        {
            sb.append(time)
                    .append(" ET")
                    .append(" (")
                    .append(discordRelativeTime(game)).append(") ");
        }

        sb.append(formatDoubleheaderSuffix(game));

        if(game.isSpringTraining())
            sb.append(formatBroadcasts(game));

        if(game.isRegularOrPostseason())
            sb.append(formatNationalBroadcastNote(game));

        if(game.isPostseason() && game.getSeriesGameNumber() > 1)
        {
            if(game.home().wins() == game.away().wins())
            {
                sb.append(" (Series tied ")
                        .append(game.home().wins())
                        .append("-")
                        .append(game.away().wins())
                        .append(")");
            }
            else if(game.home().wins() > game.away().wins())
            {
                sb.append(" (")
                        .append(game.home().teamInfo().emoji().getAsMention())
                        .append(" leads series ")
                        .append(game.home().wins())
                        .append("-")
                        .append(game.away().wins())
                        .append(")");
            }
            else
            {
                sb.append(" (")
                        .append(game.away().teamInfo().emoji().getAsMention())
                        .append(" leads series ")
                        .append(game.away().wins()).append("-")
                        .append(game.home().wins()).append(")");
            }
        }

        CustomEmoji mlbEmoji = jda.getEmojiById(360520397235552256L);
        if(game.freeGame())
            sb.append(mlbEmoji != null ? " (" + mlbEmoji.getAsMention() + ".tv Free Game)" : "(MLB:.tv Free Game)");

        return sb.toString();
    }

    private String formatShortDate(LocalDate date)
    {
        return date.getMonthValue() + "/" + date.getDayOfMonth();
    }

    private String formatDoubleheaderSuffix(ScheduleGame game)
    {
        if(game.originalDate() != null && !game.isDoubleheader()) return "(Makeup of " + formatShortDate(game.originalDate()) + ")";
        if(!game.isDoubleheader()) return "";
        if(game.isSplitDoubleheader())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("(Game ")
                    .append(game.gameNumber())
                    .append(" of DH");
            if(game.isMakeupGame())
            {
                sb.append("; makeup of ")
                        .append(formatShortDate(game.makeupFromDate()));
            }
            sb.append(") ");
            return sb.toString();
        }
        if(game.isStraightDoubleheader())
        {
            if(game.gameNumber() == 1) return "(Game 1 of DH)";
            StringBuilder sb = new StringBuilder();
            sb.append("(Game 2 of DH");
            if(game.isMakeupGame())
            {
                sb.append("; makeup of ").append(formatShortDate(game.makeupFromDate()));
            }
            sb.append(")");
            return sb.toString();
        }
        return "";
    }

    private boolean shouldShowStartTime(ScheduleGame game)
    {
        return !(game.isStraightDoubleheader() && game.gameNumber() == 2);
    }

    private String formatNationalBroadcastNote(ScheduleGame game)
    {
        List<String> partners = game.broadcasts().nationalTv();
        if(partners.isEmpty()) return "";
        if(isFoxRegionalBroadcast(partners))
            return "(Regional broadcast on " + formatPartnerList(partners) + "; see map for game in your area.)";

        if(isSundayNightBaseball(partners))
            return sundayNightBaseball(partners);

        if(isStreamingBroadcast(partners))
            return "(Available exclusively on " + formatPartnerList(partners) + ")";

        if(isMlbNetwork(partners) || (game.isRegularSeason() && isTbs(partners)) || (nonExclusiveFs1(partners) && (game.broadcasts().homeTv() || game.broadcasts().awayTv())))
            return "(Out-of-market broadcast on " + formatPartnerList(partners) + ")";

        return "(National broadcast on " + formatPartnerList(partners) + ")";
    }

    private boolean nonExclusiveFs1(List<String> partners)
    {
        for(String s : partners)
        {
            if(s.toLowerCase().contains("fs1"))
            {
                return true;
            }
        }
        return false;
    }

    private String sundayNightBaseball(List<String> partners)
    {
        boolean nbcsn = false;
        for(String s : partners)
        {
            String partner = s.toLowerCase();
            if (partner.contains("nbcsn"))
            {
                nbcsn = true;
                break;
            }
        }
        if(nbcsn) return "(National broadcast on " + mapNationalPartnerEmoji("nbcsn") + "; also available on " + mapNationalPartnerEmoji("peacock") + ")";
        return "(National broadcast on " + mapNationalPartnerEmoji("nbc") + "; also available on " + mapNationalPartnerEmoji("peacock") + ")";
    }

    private boolean isSundayNightBaseball(List<String> partners)
    {
        for(String s : partners) return s.toLowerCase().contains("nbc") || s.toLowerCase().contains("peacock");
        return false;
    }

    private boolean isStreamingBroadcast(List<String> partners)
    {
        for(String partner : partners)
        {
            String p = partner.toLowerCase();
            if(p.contains("apple") ||
            p.contains("netflix") ||
            p.contains("peacock") ||
            p.contains("roku"))
                return true;
        }
        return false;
    }

    private boolean isMlbNetwork(List<String> partners)
    {
        for(String partner : partners)
        {
            String p = partner.toLowerCase();
            if(p.equals("mlb network") || p.startsWith("mlb")) return true;
        }
        return false;
    }

    private boolean isTbs(List<String> partners)
    {
        for(String partner : partners)
        {
            String p = partner.toLowerCase();
            if(p.equals("tbs")) return true;
        }
        return false;
    }

    private boolean isFoxRegionalBroadcast(List<String> partners)
    {
        if(partners.size() != 1) return false;

        String p = partners.get(0).toLowerCase();
        return (p.equals("fox") || p.startsWith("fox ")) && MlbApiClient.regionalFox;
    }

    private String formatPartnerList(List<String> partners)
    {
        List<String> formatted = new ArrayList<>();

        for(String partner : partners)
        {
            String mapped = mapNationalPartnerEmoji(partner);
            if(mapped != null)
                formatted.add(mapped);
        }
        return String.join(", ", formatted);
    }

    private String mapNationalPartnerEmoji(String name)
    {
        if(name == null) return "";

        String normalized = name.trim().toLowerCase();
        if(normalized.contains("espn"))
            return jda.getEmojiById(1259969905185525910L).getAsMention();
        if(normalized.equals("fox") || normalized.startsWith("fox "))
            return jda.getEmojiById(1259970903299719258L).getAsMention();
        if(normalized.equals("fs1"))
            return jda.getEmojiById(1259969908872187924L).getAsMention();
        if(normalized.equals("tbs"))
            return jda.getEmojiById(1259969930372055071L).getAsMention();
        if(normalized.equals("mlb network") || normalized.startsWith("mlbn"))
            return jda.getEmojiById(1259969910092861603L).getAsMention();
        if(normalized.contains("apple"))
            return jda.getEmojiById(1259969903889219605L).getAsMention();
        if(normalized.contains("netflix"))
            return jda.getEmojiById(1460862224565407794L).getAsMention();
        if(normalized.contains("nbcsn"))
            return jda.getEmojiById(1460862140788248677L).getAsMention();
        if(normalized.contains("peacock") && normalized.contains("nbc"))
            return jda.getEmojiById(1460862093430624436L).getAsMention();
        if(normalized.contains("peacock"))
            return jda.getEmojiById(1460862186850357376L).getAsMention();
        if(normalized.contains("nbc"))
            return jda.getEmojiById(1460862093430624436L).getAsMention();
        return name;
    }

    private String mapNationalTvEmoji(String name)
    {
        if(name == null) return null;

        String normalized = name.trim().toLowerCase();

        if(normalized.equals("espn"))
            return jda.getEmojiById(1259969905185525910L).getAsMention();
        if(normalized.equals("mlb network") || normalized.startsWith("mlbn"))
            return jda.getEmojiById(1259969910092861603L).getAsMention();

        return name;
    }

    private String formatBroadcasts(ScheduleGame game)
    {
        BroadcastInfo b = game.broadcasts();

        String tvSection = formatTvSection(game, b);
        String radioSection = formatRadioSection(game, b);

        boolean hasTv = tvSection != null && !tvSection.isBlank();
        boolean hasRadio = radioSection != null && !radioSection.isBlank();

        if(!hasTv && !hasRadio) return "";
        if(hasTv && hasRadio) return " " + tvSection + " " + radioSection;
        if(hasTv) return " " + tvSection;
        return " " + radioSection;
    }

    private String formatTvSection(ScheduleGame game, BroadcastInfo b)
    {
        Set<String> items = new LinkedHashSet<>();
        if(b.awayTv())
            items.add(game.away().teamInfo().emojiMention());
        if(b.homeTv())
            items.add(game.home().teamInfo().emojiMention());

        for(String national : b.nationalTv())
        {
            String emoji = mapNationalTvEmoji(national);
            if(emoji != null) items.add(emoji);
        }

        if(items.isEmpty()) return null;
        return "(\uD83D\uDCFA " + String.join(" ", items) + ")";
    }



    private String formatRadioSection(ScheduleGame game, BroadcastInfo b) {
        List<String> items = new ArrayList<>();

        if (b.awayRadio()) {
            items.add(game.away().teamInfo().emojiMention());
        }

        if (b.homeRadio()) {
            items.add(game.home().teamInfo().emojiMention());
        }

        if (items.isEmpty()) {
            return null;
        }

        return "(\uD83D\uDCFB " + String.join(" ", items) + ")";
    }

    private String splitSquadSuffix(ScheduleGame.TeamSnapshot team)
    {
        return team.splitSquad() ? " (ss)" : "";
    }

    private String emoji(TeamInfo teamInfo)
    {
        return teamInfo != null && teamInfo.emoji() != null ? teamInfo.emoji().getAsMention() : "";
    }

    private String roleMention(TeamInfo teamInfo)
    {
        if(teamInfo == null || teamInfo.role() == null)
            return teamInfo != null ? teamInfo.name() : "";
        return "<@&" + teamInfo.role().getId() + ">";
    }

    public String renderGameContent(ScheduleGame game) { return formatGame(game).content(); }

    public String renderGameContent(ScheduleGame game, String probableLineOverride) { return formatGame(game, probableLineOverride).content(); }
}
