package com.andrew.scoreboardbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ScheduleFormatter
{
    private static final DateTimeFormatter HEADER_FORMAT = DateTimeFormatter.ofPattern("MMMM d");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");
    private final JDA jda;
    private List<String> broadcastEmoji;

    public ScheduleFormatter(JDA jda)
    {
        this.jda = jda;
        broadcastEmoji = new ArrayList<>();
        broadcastEmoji.add(jda.getEmojiById(1259969902501040129L).getAsMention());
        broadcastEmoji.add(jda.getEmojiById(1259969905185525910L).getAsMention());
        broadcastEmoji.add(jda.getEmojiById(1259969908872187924L).getAsMention());
        broadcastEmoji.add(jda.getEmojiById(1259970903299719258L).getAsMention());
        broadcastEmoji.add(jda.getEmojiById(1259969930372055071L).getAsMention());
        broadcastEmoji.add(jda.getEmojiById(1259969910092861603L).getAsMention());
        broadcastEmoji.add(jda.getEmojiById(1259969903889219605L).getAsMention());
        broadcastEmoji.add(jda.getEmojiById(1460862224565407794L).getAsMention());
        broadcastEmoji.add(jda.getEmojiById(1460862140788248677L).getAsMention());
        broadcastEmoji.add(jda.getEmojiById(1460862093430624436L).getAsMention());
        broadcastEmoji.add(jda.getEmojiById(1460862186850357376L).getAsMention());
        broadcastEmoji.add(jda.getEmojiById(1460862093430624436L).getAsMention());
    }

    public String formatDateHeader(LocalDate date, boolean springTraining)
    {
        String leftLeagueEmoji = springTraining ? jda.getEmojiById(1337167447005855826L).getAsMention() : jda.getEmojiById(344302564315234304L).getAsMention();
        String rightLeagueEmoji = springTraining ? jda.getEmojiById(1337167445940633640L).getAsMention() : jda.getEmojiById(344302519167483904L).getAsMention();
        String mlbEmoji = jda.getEmojiById(360520397235552256L).getAsMention();

        return "⚾ " + mlbEmoji + " " + leftLeagueEmoji + " " + date.format(DateTimeFormatter.ofPattern("MMMM d")) + " " + rightLeagueEmoji + " \uD83D\uDCC5 ⚾";
    }

    public FormattedGameMessage formatGame(ScheduleGame game, String probableLineOverride) throws Exception
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

    public FormattedGameMessage formatGame(ScheduleGame game) throws Exception
    {
        return formatGame(game, null);
    }

    private String formatSuspendedGameMatchupLine(ScheduleGame game) throws Exception
    {
        String awayRole = roleMention(game.away().teamInfo());
        String homeRole = roleMention(game.home().teamInfo());

        String awayEmoji = emoji(game.away().teamInfo());
        String homeEmoji = emoji(game.home().teamInfo());

        String json = MlbApiClient.readUrl("https://statsapi.mlb.com/api/v1.1/game/" + game.gamePk() + "/feed/live");
        JSONObject root = new JSONObject(json);
        JSONObject linescore = root.getJSONObject("liveData").getJSONObject("linescore");
        JSONObject teams = linescore.getJSONObject("teams");

        String awayScore = String.valueOf(teams.getJSONObject("away").optInt("runs", 0));
        String homeScore = String.valueOf(teams.getJSONObject("home").optInt("runs", 0));

        String outs = String.valueOf(linescore.getInt("outs"));
        String inning = linescore.getString("inningHalf") + " " + linescore.getInt("currentInning");

        return awayRole + " " + awayEmoji + " " + awayScore + "\n" + homeRole + " " +  homeEmoji + " " + homeScore + "\n" + outs + " out - " + inning;
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
        {
            ZonedDateTime completionDate = game.resumeDateTime();
            LocalDate date = completionDate.toLocalDate();
            return "Suspended; to be completed on " + formatShortDate(date) + ".";
        }
        return state + ".";
    }

    private String formatSectionHeader(ScheduleGame game)
    {
        if(!game.home().teamInfo().isMlbTeam() || !game.away().teamInfo().isMlbTeam())
            return jda.getEmojiById(360520397235552256L).getAsMention() + " " + Text.EXHIBITION.getText();
        if(game.venueId() == 5445)
        {
            // field of dreams
            return jda.getEmojiById(1537316721986113576L).getAsMention() + " \uD835\uDC0C\uD835\uDC0B\uD835\uDC01 \uD835\uDC00\uD835\uDC13 \uD835\uDC05\uD835\uDC08\uD835\uDC04\uD835\uDC0B\uD835\uDC03 \uD835\uDC0E\uD835\uDC05 \uD835\uDC03\uD835\uDC11\uD835\uDC04\uD835\uDC00\uD835\uDC0C\uD835\uDC12";
        }
        if(game.venueId() == 2735)
        {
            // little league classic
            return jda.getEmojiById(1538937408794534038L).getAsMention() + "\uD835\uDC0C\uD835\uDC0B\uD835\uDC01 \uD835\uDC0B\uD835\uDC08\uD835\uDC13\uD835\uDC13\uD835\uDC0B\uD835\uDC04 \uD835\uDC0B\uD835\uDC04\uD835\uDC00\uD835\uDC06\uD835\uDC14\uD835\uDC04 \uD835\uDC02\uD835\uDC0B\uD835\uDC00\uD835\uDC12\uD835\uDC12\uD835\uDC08\uD835\uDC02";
        }
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
            return game.away().teamInfo().emoji().getAsMention() + " " + jda.getEmojiById(360520397235552256L).getAsMention() + " " + game.home().teamInfo().emoji().getAsMention() + " \uD835\uDC0C\uD835\uDC0B\uD835\uDC01 \uD835\uDC00\uD835\uDC0B\uD835\uDC0B-\uD835\uDC12\uD835\uDC13\uD835\uDC00\uD835\uDC11 \uD835\uDC06\uD835\uDC00\uD835\uDC0C\uD835\uDC04";
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
                result += jda.getEmojiById(344302564315234304L).getAsMention() + Text.NL_WC.getText() + " ";
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
                result += jda.getEmojiById(344302564315234304L).getAsMention() + " " + Text.NLCS.getText() + " ";
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
                .append(awayEmoji);

        if(!game.isPostseason() && !game.isExhibition())
        {
            sb.append(" ").append(awayRecord);
        }

        sb.append(splitSquadSuffix(away))
                .append(" @ ")
                .append(homeRole).append(" ")
                .append(homeEmoji);

        if(!game.isPostseason() && !game.isExhibition())
        {
            sb.append(" ").append(homeRecord);
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

        String location = game.location();
        if(location != null) sb.append(location).append(" ");

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
                if(game.gameType().equalsIgnoreCase("F") && game.home().wins() == 2)
                {
                    sb.append(" (").append(game.home().teamInfo().emoji().getAsMention()).append(" wins series 2-").append(game.away().wins()).append(")");
                }
                else if(game.gameType().equalsIgnoreCase("D") && game.home().wins() == 3)
                {
                    sb.append(" (").append(game.home().teamInfo().emoji().getAsMention()).append(" wins series 3-").append(game.away().wins()).append(")");
                }
                else if((game.gameType().equalsIgnoreCase("L") || game.gameType().equalsIgnoreCase("W")) && game.home().wins() == 4)
                {
                    sb.append(" (").append(game.home().teamInfo().emoji().getAsMention()).append(" wins series 4-").append(game.away().wins()).append(")");
                }
                else
                {
                    sb.append(" (")
                            .append(game.home().teamInfo().emoji().getAsMention())
                            .append(" leads series ")
                            .append(game.home().wins())
                            .append("-")
                            .append(game.away().wins())
                            .append(")");
                }
            }
            else
            {
                if(game.gameType().equalsIgnoreCase("F") && game.away().wins() == 2)
                {
                    sb.append(" (").append(game.away().teamInfo().emoji().getAsMention()).append(" wins series 2-").append(game.home().wins()).append(")");
                }
                else if(game.gameType().equalsIgnoreCase("D") && game.away().wins() == 3)
                {
                    sb.append(" (").append(game.away().teamInfo().emoji().getAsMention()).append(" wins series 3-").append(game.home().wins()).append(")");
                }
                else if((game.gameType().equalsIgnoreCase("L") || game.gameType().equalsIgnoreCase("W")) && game.away().wins() == 4)
                {
                    sb.append(" (").append(game.away().teamInfo().emoji().getAsMention()).append(" wins series 4-").append(game.home().wins()).append(")");
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
        if(isFoxRegionalBroadcast(partners, game))
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
        if(nbcsn) return "(National broadcast on " + jda.getEmojiById(1460862140788248677L).getAsMention() + "; also available on " + jda.getEmojiById(1460862093430624436L).getAsMention() + ")";
        return "(National broadcast on " + jda.getEmojiById(1460862093430624436L).getAsMention() + "; also available on " + jda.getEmojiById(1460862093430624436L).getAsMention() + ")";
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

    private boolean isFoxRegionalBroadcast(List<String> partners, ScheduleGame game)
    {
        if(partners.size() != 1 || game.isPostseason()) return false;

        String p = partners.get(0).toLowerCase();
        return (p.equals("fox") || p.startsWith("fox ")) && MlbApiClient.regionalFox;
    }

    private String formatPartnerList(List<String> partners)
    {
        List<String> formatted = new ArrayList<>();

        for(String partner : partners)
        {
            String mapped = mapNationalPartnerEmoji(partner, formatted);
            if(mapped != null)
                formatted.add(mapped);
        }
        return String.join(", ", formatted);
    }

    private String mapNationalPartnerEmoji(String name, List<String> previous)
    {
        if(name == null) return "";

        String result = "";

        String normalized = name.trim().toLowerCase();
        if(normalized.contains("abc"))
            result = jda.getEmojiById(1259969902501040129L).getAsMention();
        else if(normalized.contains("espn"))
            result = jda.getEmojiById(1259969905185525910L).getAsMention();
        else if(normalized.contains("fs1"))
            result = jda.getEmojiById(1259969908872187924L).getAsMention();
        else if(normalized.contains("fox / fox one"))
            result = jda.getEmojiById(1259970903299719258L).getAsMention();
        else if(normalized.contains("fox one"))
            result = "";
        else if(normalized.startsWith("fox ") || normalized.contains("fox"))
            result = jda.getEmojiById(1259970903299719258L).getAsMention();
        else if(normalized.contains("tbs"))
            result = jda.getEmojiById(1259969930372055071L).getAsMention();
        else if(normalized.equals("mlb network") || normalized.startsWith("mlbn"))
            result = jda.getEmojiById(1259969910092861603L).getAsMention();
        else if(normalized.contains("apple"))
            result = jda.getEmojiById(1259969903889219605L).getAsMention();
        else if(normalized.contains("netflix"))
            result = jda.getEmojiById(1460862224565407794L).getAsMention();
        else if(normalized.contains("nbcsn"))
            result = jda.getEmojiById(1460862140788248677L).getAsMention();
        else if(normalized.contains("peacock") && normalized.contains("nbc"))
            result = jda.getEmojiById(1460862093430624436L).getAsMention();
        else if(normalized.contains("peacock"))
            result = jda.getEmojiById(1460862186850357376L).getAsMention();
        else if(normalized.contains("nbc"))
            result = jda.getEmojiById(1460862093430624436L).getAsMention();

        if(previous.contains(result) || result.equalsIgnoreCase(""))
            return null;
        else
            return result;
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

    public String renderGameContent(ScheduleGame game) throws Exception { return formatGame(game).content(); }

    public String renderGameContent(ScheduleGame game, String probableLineOverride) throws Exception { return formatGame(game, probableLineOverride).content(); }
}
