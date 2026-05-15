package com.andrew.scoreboardbot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class MlbApiClient
{
    private final TeamRegistry teamRegistry;
    public static boolean regionalFox;

    public MlbApiClient(TeamRegistry teamRegistry) { this.teamRegistry = teamRegistry; }

    public List<ScheduleGame> getScheduleJsonFromUrl(String urlString) throws Exception
    {
        String json = readUrl(urlString);
        return parseSchedule(json);
    }

    public List<ScheduleGame> parseSchedule(String json) throws JSONException
    {
        JSONObject root = new JSONObject(json);
        JSONArray dates = root.getJSONArray("dates");
        List<ScheduleGame> games = new ArrayList<>();

        if(dates.length() == 0)
            return games;

        JSONArray gameArray = dates.getJSONObject(0).getJSONArray("games");
        for(int i = 0; i < gameArray.length(); i++)
        {
            JSONObject game = gameArray.getJSONObject(i);

            int gamePk = game.getInt("gamePk");
            String gameType = game.optString("gameType", "");
            ZonedDateTime gameDate = ZonedDateTime.parse(game.getString("gameDate"));
            String detailedState = game.getJSONObject("status").getString("detailedState");
            String seriesDescription = game.optString("seriesDescription", "");
            String dayNight = game.optString("dayNight", "");

            JSONObject away = game.getJSONObject("teams").getJSONObject("away");
            JSONObject home = game.getJSONObject("teams").getJSONObject("home");

            int awayId = away.getJSONObject("team").getInt("id");
            String awayName = away.getJSONObject("team").getString("name");

            int homeId = home.getJSONObject("team").getInt("id");
            String homeName = home.getJSONObject("team").getString("name");

            ScheduleGame.TeamSnapshot awaySnapshot = new ScheduleGame.TeamSnapshot(
                    teamRegistry.getByIdOrExternal(awayId, awayName),
                    parseWins(away),
                    parseLosses(away),
                    away.optBoolean("splitSquad", false),
                    away.has("score") ? away.getInt("score") : null
            );

            ScheduleGame.TeamSnapshot homeSnapshot = new ScheduleGame.TeamSnapshot(
                    teamRegistry.getByIdOrExternal(homeId, homeName),
                    parseWins(home),
                    parseLosses(home),
                    home.optBoolean("splitSquad", false),
                    home.has("score") ? home.getInt("score") : null
            );

            ProbablePitcherInfo awayProbable = parseProbablePitcher(away);
            ProbablePitcherInfo homeProbable = parseProbablePitcher(home);

            boolean freeGame = parseFreeGame(game);

            String doubleHeader = game.optString("doubleHeader", "N");
            int gameNumber = game.optInt("gameNumber", 0);
            LocalDate makeupFromDate = parseMakeupFromDate(game);

            LocalDate resumedFromDate = parseResumedFromDate(game);
            ZonedDateTime resumeDateTime = parseResumeDateTime(game);
            int numberInSeries = game.optInt("seriesGameNumber", 0);

            LocalDate makeupDate = parseMakeupDate(game);
            String disruptionReason = parseDisruptionReason(game);
            LocalDate originalDate = parseOriginalDate(game);

            games.add(new ScheduleGame(
                    gamePk,
                    gameDate,
                    detailedState,
                    seriesDescription,
                    dayNight,
                    gameType,
                    awaySnapshot,
                    homeSnapshot,
                    awayProbable,
                    homeProbable,
                    freeGame,
                    parseBroadcasts(game),
                    doubleHeader,
                    gameNumber,
                    makeupFromDate,
                    resumedFromDate,
                    resumeDateTime,
                    numberInSeries,
                    makeupDate,
                    disruptionReason,
                    originalDate
            ));
        }
        regionalOrNationalFox(games);
        return games;
    }

    private void regionalOrNationalFox(List<ScheduleGame> games) throws JSONException
    {
        int count = 0;
        for(ScheduleGame game : games)
        {
            List<String> partners = game.broadcasts().nationalTv();
            if(partners.isEmpty()) continue;
            String p = partners.get(0).toLowerCase();
            if(p.equals("fox") || p.startsWith("fox ")) count++;
        }
        regionalFox = count > 1;
    }

    private LocalDate parseOriginalDate(JSONObject game)
    {
        String value = game.optString("rescheduledFromDate", "").trim();
        if(!value.isBlank())
        {
            try
            {
                return LocalDate.parse(value);
            }
            catch (Exception ignored) {}
        }
        return null;
    }

    private String parseDisruptionReason(JSONObject obj) throws JSONException
    {
        String reason = obj.getJSONObject("status").optString("reason", "");
        if(!reason.isBlank()) return reason;
        return null;
    }

    private LocalDate parseMakeupDate(JSONObject obj)
    {
        String value = obj.optString("rescheduleGameDate", "").trim();
        if(!value.isBlank())
        {
            try
            {
                return LocalDate.parse(value);
            }
            catch (Exception ignored) {}
        }
        return null;
    }

    private int parseWins(JSONObject teamObj) throws JSONException
    {
        if(teamObj.has("leagueRecord"))
            return teamObj.getJSONObject("leagueRecord").optInt("wins", 0);
        return 0;
    }

    private int parseLosses(JSONObject teamObj) throws JSONException
    {
        if(teamObj.has("leagueRecord"))
            return teamObj.getJSONObject("leagueRecord").optInt("losses", 0);
        return 0;
    }

    private LocalDate parseResumedFromDate(JSONObject game)
    {
        String value = game.optString("resumedFromDate", "").trim();
        if(!value.isBlank())
        {
            try
            {
                return LocalDate.parse(value);
            }
            catch(Exception ignored) {}
        }
        return null;
    }

    private ZonedDateTime parseResumeDateTime(JSONObject game)
    {
        String value = game.optString("resumeDate", "").trim();
        if(!value.isBlank())
        {
            try
            {
                return ZonedDateTime.parse(value);
            }
            catch(Exception ignored) {}
        }
        return null;
    }

    private LocalDate parseMakeupFromDate(JSONObject game)
    {
        String value = game.optString("rescheduledFrom", "").trim();
        if(!value.isBlank())
        {
            try
            {
                return OffsetDateTime.parse(value).toLocalDate();
            }
            catch (Exception ignored) {}
        }
        return null;
    }

    private BroadcastInfo parseBroadcasts(JSONObject game) throws JSONException
    {
        boolean awayTv = false;
        boolean homeTv = false;
        boolean awayRadio = false;
        boolean homeRadio = false;
        List<String> nationalTv = new ArrayList<>();

        if(!game.has("broadcasts"))
            return new BroadcastInfo(awayTv, homeTv, awayRadio, homeRadio, nationalTv);

        JSONArray broadcasts = game.getJSONArray("broadcasts");

        for(int i = 0; i < broadcasts.length(); i++)
        {
            JSONObject b = broadcasts.getJSONObject(i);

            String type = b.optString("type", "");
            String name = b.optString("name", "");
            String side = b.optString("homeAway", "");
            boolean isNational = b.optBoolean("isNational", false);
            if(name.isBlank()) continue;

            if(type.equalsIgnoreCase("TV"))
            {
                if(isNational)
                    addIfMissing(nationalTv, name);
                else if(side.equalsIgnoreCase("away"))
                    awayTv = true;
                else if(side.equalsIgnoreCase("home"))
                    homeTv = true;
            }
            else if(type.equalsIgnoreCase("RADIO") || type.equalsIgnoreCase("AUDIO") || type.equalsIgnoreCase("AM") || type.equalsIgnoreCase("FM"))
            {
                if(side.equalsIgnoreCase("away"))
                    awayRadio = true;
                else if(side.equalsIgnoreCase("home"))
                    homeRadio = true;
            }
        }

        return new BroadcastInfo(awayTv, homeTv, awayRadio, homeRadio, nationalTv);
    }

    private void addIfMissing(List<String> list, String value)
    {
        if(!list.contains(value)) list.add(value);
    }

    private boolean parseFreeGame(JSONObject game) throws JSONException
    {
        if(game.has("broadcasts"))
        {
            JSONArray broadcasts = game.getJSONArray("broadcasts");
            for(int i = 0; i < broadcasts.length(); i++)
            {
                JSONObject broadcast = broadcasts.getJSONObject(i);
                if(broadcast.optBoolean("freeGame", false) || broadcast.optBoolean("freeGameStatus", false))
                    return true;
            }
        }

        if(game.has("content"))
        {
            JSONObject content = game.getJSONObject("content");
            if(content.has("media"))
            {
                JSONObject media = content.getJSONObject("media");
                return media.optBoolean("freeGame", false);
            }
        }
        return false;
    }

    private ProbablePitcherInfo parseProbablePitcher(JSONObject teamObj) throws JSONException {
        if(!teamObj.has("probablePitcher"))
            return ProbablePitcherInfo.tbd();

        JSONObject probable = teamObj.getJSONObject("probablePitcher");

        int id = probable.has("id") ? probable.getInt("id") : null;
        String fullName = probable.optString("fullName", "TBD");

        Integer wins = null;
        Integer losses = null;
        String era = null;

        if(probable.has("stats"))
        {
            JSONArray stats = probable.getJSONArray("stats");
            for(int i = 0; i < stats.length(); i++)
            {
                JSONObject statBlock = stats.getJSONObject(i);

                String type = statBlock.getJSONObject("type").optString("displayName", "");
                String group = statBlock.getJSONObject("group").optString("displayName", "");

                if(type.equals("statsSingleSeason") && group.equalsIgnoreCase("pitching"))
                {
                    JSONObject statValues = statBlock.getJSONObject("stats");
                    if(statValues.has("wins"))
                        wins = statValues.getInt("wins");
                    if(statValues.has("losses"))
                        losses = statValues.getInt("losses");
                    era = statValues.optString("era", null);
                    break;
                }
            }
        }
        return new ProbablePitcherInfo(id, fullName, wins, losses, era);
    }

    private String readUrl(String urlString) throws Exception
    {
        int attempts = 3;
        for(int attempt = 1; attempt <= attempts; attempt++)
        {
            try
            {
                URL url = URI.create(urlString).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "ScoreboardBot/1.0");

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)))
                {
                    StringBuilder sb = new StringBuilder();
                    String line;

                    while((line = reader.readLine()) != null)
                    {
                        sb.append(line);
                    }
                    return sb.toString();
                }
            }
            catch(Exception e)
            {
                if(attempt == attempts) throw e;
                System.err.println("MLB API request failed, retrying attempt " + (attempt + 1) + "/" +attempts + ": " + e.getMessage());
                Thread.sleep(1000L * attempt);
            }
        }
        throw new IllegalStateException("Unreachable readUrl failure");
    }

    public LiveFeed getLiveFeed(int gamePk) throws Exception
    {
        String url = "https://statsapi.mlb.com/api/v1.1/game/" + gamePk + "/feed/live";
        String json = readUrl(url);
        return parseLiveFeed(json);
    }

    private LiveFeed parseLiveFeed(String json) throws JSONException
    {
        JSONObject root = new JSONObject(json);

        JSONObject gameData = root.getJSONObject("gameData");
        JSONObject liveData = root.getJSONObject("liveData");

        String detailedState = gameData.getJSONObject("status").optString("detailedState", "");

        JSONObject linescoreObj = liveData.optJSONObject("linescore");
        String inningStateText = parseInningStateText(linescoreObj);

        List<LivePlay> plays = parseLivePlays(liveData.optJSONObject("plays"));
        LinescoreSnapshot linescore = parseLinescore(linescoreObj);
        DecisionsSnapshot decisions = parseDecisions(liveData.optJSONObject("decisions"));
        List<LiveActionEvent> events = parseActionEvents(liveData.optJSONObject("plays"));

        return new LiveFeed(detailedState, inningStateText, plays, events, linescore, decisions);
    }

    private String parseInningStateText(JSONObject linescoreObj)
    {
        if(linescoreObj == null) return "";
        String inningHalf = linescoreObj.optString("inningHalf", "");
        String currentInningOrdinal = linescoreObj.optString("currentInningOrdinal", "");

        if(inningHalf.isBlank() || currentInningOrdinal.isBlank()) return "";

        return inningHalf + " of the " + currentInningOrdinal;
    }

    private List<LivePlay> parseLivePlays(JSONObject playsObj) throws JSONException
    {
        List<LivePlay> result = new ArrayList<>();
        if(playsObj == null) return result;

        JSONArray allPlays = playsObj.optJSONArray("allPlays");
        if(allPlays == null) return result;

        int previousOuts = 0;
        int previousInning = -1;
        String previousHalf = "";

        for(int i = 0; i < allPlays.length(); i++)
        {
            JSONObject play = allPlays.getJSONObject(i);
            if(i == 0)
            {
                System.out.println("RAW PLAY 0:");
                System.out.println(play.toString(2));
            }

            JSONObject aboutObj = play.optJSONObject("about");
            JSONObject resultObj = play.optJSONObject("result");
            JSONObject countObj = play.optJSONObject("count");

            boolean isComplete = aboutObj != null && aboutObj.optBoolean("isComplete", false);

            int atBatIndex = aboutObj != null ? aboutObj.optInt("atBatIndex", i) : i;

            String eventType = resultObj != null ? resultObj.optString("eventType", "") : "";
            String description = resultObj != null ? resultObj.optString("description", "") : "";

            if(!isComplete || description.isBlank()) continue;

            boolean scoringPlay = aboutObj != null && aboutObj.optBoolean("isScoringPlay", false);

            String inningHalf = aboutObj != null ? aboutObj.optString("halfInning", "") : "";
            int inning = aboutObj != null ? aboutObj.optInt("inning", 0) : 0;

            if(inning != previousInning || !inningHalf.equals(previousHalf))
            {
                previousOuts = 0;
                previousInning = inning;
                previousHalf = inningHalf;
            }

            int outsAfterPlay = countObj != null ? countObj.optInt("outs", previousOuts) : previousOuts;
            int outsAdded = outsAfterPlay - previousOuts;
            if(outsAdded < 0) outsAdded = outsAfterPlay;

            previousOuts = outsAfterPlay;

            Integer awayScore = null;
            Integer homeScore = null;
            if (resultObj != null) {
                if (resultObj.has("awayScore")) awayScore = resultObj.getInt("awayScore");
                if (resultObj.has("homeScore")) homeScore = resultObj.getInt("homeScore");
            }

            String hitSpeed = null;
            String hitAngle = null;
            String hitDistance = null;

            JSONObject hitData = play.optJSONObject("hitData");
            if (hitData != null) {
                if (hitData.has("launchSpeed")) hitSpeed = String.valueOf(hitData.getDouble("launchSpeed"));
                if (hitData.has("launchAngle")) hitAngle = String.valueOf(hitData.getDouble("launchAngle"));
                if (hitData.has("totalDistance")) hitDistance = String.valueOf(hitData.getDouble("totalDistance"));
            }

            LivePlay livePlay = new LivePlay(atBatIndex, eventType, description, scoringPlay, outsAfterPlay, outsAdded, awayScore, homeScore, hitSpeed, hitAngle, hitDistance, inningHalf, inning);
            System.out.println("play idx=" + livePlay.atBatIndex()
                    + " type=" + livePlay.eventType()
                    + " desc=[" + livePlay.description() + "]"
                    + " scoring=" + livePlay.scoringPlay()
                    + " outsAfter=" + livePlay.outsAfterPlay()
                    + " outsAdded=" + livePlay.outsAdded());
            result.add(livePlay);
        }
        return result;
    }

    private LinescoreSnapshot parseLinescore(JSONObject linescoreObj) throws JSONException
    {
        if(linescoreObj == null) return null;

        Integer awayRuns = null;
        Integer homeRuns = null;
        Integer awayHits = null;
        Integer homeHits = null;
        Integer awayErrors = null;
        Integer homeErrors = null;

        JSONObject teams = linescoreObj.optJSONObject("teams");
        if(teams != null)
        {
            JSONObject away = teams.optJSONObject("away");
            JSONObject home = teams.optJSONObject("home");

            if(away != null)
            {
                awayRuns = away.has("runs") ? away.getInt("runs") : null;
                awayHits = away.has("hits") ? away.getInt("hits") : null;
                awayErrors = away.has("errors") ? away.getInt("errors") : null;
            }

            if(home != null)
            {
                homeRuns = home.has("runs") ? home.getInt("runs") : null;
                homeHits = home.has("hits") ? home.getInt("hits") : null;
                homeErrors = home.has("errors") ? home.getInt("errors") : null;
            }
        }

        List<LinescoreSnapshot.InningLine> innings = new ArrayList<>();
        JSONArray inningArray = linescoreObj.optJSONArray("innings");
        if(inningArray != null)
        {
            for(int i = 0; i < inningArray.length(); i++)
            {
                JSONObject inning = inningArray.getJSONObject(i);
                innings.add(new LinescoreSnapshot.InningLine(
                        inning.optInt("num", i + 1),
                        inning.has("away") ? inning.getJSONObject("away").optInt("runs") : null,
                        inning.has("home") ? inning.getJSONObject("home").optInt("runs") : null
                ));
            }
        }
        return new LinescoreSnapshot(awayRuns, homeRuns, awayHits, homeHits, awayErrors, homeErrors, innings);
    }

    private DecisionsSnapshot parseDecisions(JSONObject decisionsObj)
    {
        if(decisionsObj == null) return null;
        return new DecisionsSnapshot(parseDecisionPitcher(decisionsObj.optJSONObject("winner")), parseDecisionPitcher(decisionsObj.optJSONObject("loser")), parseDecisionPitcher(decisionsObj.optJSONObject("save")));
    }

    private DecisionsSnapshot.PitcherDecision parseDecisionPitcher(JSONObject obj)
    {
        if(obj == null) return null;
        return new DecisionsSnapshot.PitcherDecision(obj.optString("fullName", ""), obj.optString("summary", ""));
    }

    private List<LiveActionEvent> parseActionEvents(JSONObject playsObj) throws JSONException
    {
        List<LiveActionEvent> result = new ArrayList<>();
        if(playsObj == null) return result;

        JSONArray allPlays = playsObj.optJSONArray("allPlays");
        if(allPlays == null) return result;

        for(int i = 0; i < allPlays.length(); i++)
        {
            JSONObject play = allPlays.getJSONObject(i);
            JSONObject aboutObj = play.optJSONObject("about");
            int atBatIndex = aboutObj != null ? aboutObj.optInt("atBatIndex", i) : i;

            JSONArray playEvents = play.optJSONArray("playEvents");
            if(playEvents == null) continue;

            for(int j = 0; j < playEvents.length(); j++)
            {
                JSONObject event = playEvents.getJSONObject(j);
                JSONObject details = event.optJSONObject("details");
                if(details == null) continue;

                String eventType = details.optString("eventType", "");
                String description = details.optString("description", "");

                if(!isMeaningfulActionEvent(eventType, description)) continue;

                int eventIndex = event.optInt("index", j);
                String key = atBatIndex + ":" + eventIndex + ":" + eventType;

                Integer awayScore = details.has("awayScore") ? details.getInt("awayScore") : null;
                Integer homeScore = details.has("homeScore") ? details.getInt("homeScore") : null;

                result.add(new LiveActionEvent(
                        key,
                        atBatIndex,
                        eventIndex,
                        eventType,
                        description,
                        awayScore,
                        homeScore
                ));
            }
        }
        return result;
    }

    private boolean isMeaningfulActionEvent(String eventType, String description) {
        String type = eventType == null ? "" : eventType.toLowerCase();
        String desc = description == null ? "" : description.toLowerCase();

        return type.equals("wild_pitch")
                || type.equals("passed_ball")
                || type.equals("stolen_base")
                || type.equals("caught_stealing")
                || type.equals("pickoff")
                || type.equals("pickoff_caught_stealing")
                || type.equals("balk")
                || type.equals("defensive_indiff")
                || desc.contains("wild pitch")
                || desc.contains("passed ball")
                || desc.contains("steals")
                || desc.contains("caught stealing")
                || desc.contains("pickoff")
                || desc.contains("balk")
                || desc.contains("defensive indifference");
    }
}
