package com.andrew.scoreboardbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import javax.swing.plaf.nimbus.State;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

public class LiveGameMonitorService
{
    private final JDA jda;
    private final MlbApiClient mlbApiClient;
    private final ScheduleMessageRepository scheduleRepository;
    private final LiveGameRepository liveGameRepository;
    private final ThreadTitleFormatter threadTitleFormatter;
    private final PlayByPlayFormatter playByPlayFormatter;

    public LiveGameMonitorService(JDA jda, MlbApiClient mlbApiClient, ScheduleMessageRepository scheduleRepository, LiveGameRepository liveGameRepository, ThreadTitleFormatter threadTitleFormatter, PlayByPlayFormatter playByPlayFormatter)
    {
        this.jda = jda;
        this.mlbApiClient = mlbApiClient;
        this.scheduleRepository = scheduleRepository;
        this.liveGameRepository = liveGameRepository;
        this.threadTitleFormatter = threadTitleFormatter;
        this.playByPlayFormatter = playByPlayFormatter;
    }

    public void tick(String scheduleUrl, LocalDate date) throws Exception
    {
        System.out.println("LIVE TICK START " + java.time.LocalDateTime.now());
        List<ScheduleGame> games;

        try
        {
            games = mlbApiClient.getScheduleJsonFromUrl(scheduleUrl);
        }
        catch(Exception e)
        {
            System.err.println("Could not fetch schedule for live monitor; skipping this tick.");
            e.printStackTrace();
            return;
        }

        System.out.println("LIVE TICK fetched games: " + games.size());

        boolean withinWindow = isWithinLiveWindow(games);
        System.out.println("LIVE TICK withinWindow=" + withinWindow);

        if(!withinWindow) return;

        for(ScheduleGame game : games)
        {
            System.out.println("GAME CHECK " + game.gamePk()
                    + " " + game.away().teamInfo().name()
                    + " @ " + game.home().teamInfo().name()
                    + " state=[" + game.detailedState() + "]"
                    + " start=" + game.gameDate()
                    + " create=" + shouldCreateThreadNow(game)
                    + " process=" + shouldProcessLiveFeedNow(game));
            try
            {
                if(shouldCreateThreadNow(game)) maybeCreateThread(game, date);
                if(shouldProcessLiveFeedNow(game)) maybeProcessLiveFeed(game);
            }
            catch(Exception e)
            {
                System.err.println("Live monitor failed for gamePk " + game.gamePk()
                        + " (" + game.away().teamInfo().name()
                        + " @ " + game.home().teamInfo().name() + ")");
                e.printStackTrace();
            }
        }
    }

    private boolean shouldCreateThreadNow(ScheduleGame game)
    {
        if(liveGameRepository.findByGamePk(game.gamePk()) != null) return false;

        String state = safeLower(game.detailedState());
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime start = game.gameDate();

        if(state.contains("warmup") || state.contains("in progress")) return true;
        return state.contains("final") || state.contains("cancelled") || state.contains("canceled") || state.contains("postponed");
    }

    private boolean shouldProcessLiveFeedNow(ScheduleGame game)
    {
        String state = safeLower(game.detailedState());
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime start = game.gameDate();

        if(state.contains("warmup") || state.contains("pre-game") || state.contains("pregame") || state.contains("in progress") || state.contains("manager challenge") || state.contains("delayed") || state.contains("suspended")) return true;
        if(state.contains("final") || state.contains("cancelled") || state.contains("canceled") || state.contains("postponed")) return false;

        return !now.isBefore(start.minusMinutes(20)) && now.isBefore(start.plusHours(6));
    }

    private boolean isLiveRelevantNow(ScheduleGame game)
    {
        String state = game.detailedState() == null ? "" : game.detailedState().toLowerCase();

        if(state.contains("warmup")
        || state.contains("pre-game")
        || state.contains("in progress")
        || state.contains("manager challenge")
        || state.contains("delayed")
        || state.contains("suspended")) return true;

        if(state.contains("final")
        || state.contains("cancelled")
        || state.contains("canceled")
        || state.contains("postponed")) return false;

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime gameStart = game.gameDate();
        return !now.isBefore(gameStart.minusMinutes(20)) && now.isBefore(gameStart.plusHours(5));
    }

    private void maybeCreateThread(ScheduleGame game, LocalDate date)
    {
        if(!shouldCreateThread(game)) return;
        if(liveGameRepository.findByGamePk(game.gamePk()) != null) return;

        System.out.println("Trying to create thread for gamePk " + game.gamePk());

        System.out.println("Looking for posted states with date=" + date);
        List<PostedGameState> postedStates = scheduleRepository.findAll();
        System.out.println("Total posted states in repo: " + postedStates.size());
        for(PostedGameState state : postedStates)
            System.out.println("Repo state: gamePk=" + state.gamePk()
                    + " channelId=" + state.channelId()
                    + " scheduleDate=" + state.scheduleDate()
                    + " messageId=" + state.messageId());
        System.out.println("Posted states today: " + postedStates.size());
        for(PostedGameState posted : postedStates)
        {
            if(posted.gamePk() != game.gamePk()) continue;
            TextChannel channel = jda.getTextChannelById(posted.channelId());
            if(channel == null) continue;

            System.out.println("Found posted state: gamePk=" + posted.gamePk()
                    + " channel=" + posted.channelId());

            channel.retrieveMessageById(posted.messageId()).queue(message ->
            {
                message.createThreadChannel(threadTitleFormatter.format(game)).queue(thread ->
                {
                    liveGameRepository.save(new LiveGameState(
                            game.gamePk(),
                            posted.channelId(),
                            posted.messageId(),
                            thread.getIdLong(),
                            -1,
                            new HashMap<>(),
                            "",
                            "",
                            false,
                            new HashSet<>()
                    ));
                    System.out.println("Created thread for gamePk " + game.gamePk());
                });
            });
            break;
        }
    }

    private void maybeProcessLiveFeed(ScheduleGame game) throws Exception
    {
        LiveGameState state = liveGameRepository.findByGamePk(game.gamePk());
        if(state == null) return;

        LiveFeed feed = mlbApiClient.getLiveFeed(game.gamePk());
        if(feed == null) return;

        ThreadChannel thread = jda.getThreadChannelById(state.threadId());
        if(thread == null) return;


        processStatusAdvisories(state, feed, thread);

        state = liveGameRepository.findByGamePk(game.gamePk());
        if(state == null) return;

        processInningState(state, feed, thread);
        state = liveGameRepository.findByGamePk(game.gamePk());
        if(state == null) return;

        processActionEvents(state, feed, thread);

        processNewPlays(state, feed, thread, game);

        state = liveGameRepository.findByGamePk(game.gamePk());
        if(state == null) return;

        processPlayCorrections(state, feed, thread);
        processGameFinal(state, feed, thread, game);
    }

    private void processStatusAdvisories(LiveGameState state, LiveFeed feed, ThreadChannel thread)
    {
        String oldState = state.lastKnownDetailedState() == null ? "" : state.lastKnownDetailedState();
        String newState = normalizeAdvisoryState(feed.detailedState());

        if(newState.isBlank()) return;
        if(oldState.equals(newState)) return;

        liveGameRepository.save(new LiveGameState(
                state.gamePk(),
                state.parentChannelId(),
                state.scheduleMessageId(),
                state.threadId(),
                state.lastPostedPlayIndex(),
                new HashMap<>(state.postedPlayDescriptions()),
                newState,
                state.lastKnownInningState(),
                state.finalPosted(),
                new HashSet<>(state.postedActionEventKeys())
        ));

        thread.sendMessageEmbeds(playByPlayFormatter.formatGameAdvisory(newState)).queue();
    }

    private String normalizeAdvisoryState(String rawState)
    {
        String state = safeLower(rawState);

        if (state.contains("challenge")) return "";
        if (state.equals("game over") || state.equals("final")) return "";

        if (state.contains("warmup")) return "Warmup";
        if (state.contains("in progress")) return "In Progress";
        if (state.contains("delayed")) return "Delayed";
        if (state.contains("suspended")) return "Suspended";

        return "";
    }

    private void processInningState(LiveGameState state, LiveFeed feed, ThreadChannel thread)
    {
        String oldInningState = state.lastKnownInningState() == null ? "" : state.lastKnownInningState();
        String newInningState = feed.inningStateText() == null ? "" : feed.inningStateText();

        if(!newInningState.isBlank() && !Objects.equals(oldInningState, newInningState))
        {

            thread.sendMessageEmbeds(playByPlayFormatter.formatInningStateUpdate(newInningState)).queue();

            liveGameRepository.save(new LiveGameState(
                    state.gamePk(),
                    state.parentChannelId(),
                    state.scheduleMessageId(),
                    state.threadId(),
                    state.lastPostedPlayIndex(),
                    new HashMap<>(state.postedPlayDescriptions()),
                    state.lastKnownDetailedState(),
                    newInningState,
                    state.finalPosted(),
                    new HashSet<>(state.postedActionEventKeys())
            ));
        }
    }

    private void processNewPlays(LiveGameState state, LiveFeed feed, ThreadChannel thread, ScheduleGame game)
    {
        System.out.println("processNewPlays gamePk=" + state.gamePk()
                + " lastPosted=" + state.lastPostedPlayIndex()
                + " feedPlays=" + feed.plays().size());
        sendNextCompletedPlay(state.gamePk(), feed, thread, game);
    }

    private void sendNextCompletedPlay(int gamePk, LiveFeed feed, ThreadChannel thread, ScheduleGame game)
    {
        LiveGameState state = liveGameRepository.findByGamePk(gamePk);
        if(state == null) return;

        int nextIndex = state.lastPostedPlayIndex() + 1;

        LivePlay nextPlay = feed.plays().stream()
                .filter(p -> p.atBatIndex() == nextIndex)
                .findFirst()
                .orElse(null);

        if(nextPlay == null)
        {
            System.out.println("Next play idx=" + nextIndex + " not complete yet; waiting.");
            return;
        }

        thread.sendMessageEmbeds(playByPlayFormatter.formatPlay(nextPlay, game)).queue(
                success ->
                {
                    LiveGameState latest = liveGameRepository.findByGamePk(gamePk);
                    if(latest == null) return;

                    Map<Integer, String> descriptions = new HashMap<>(latest.postedPlayDescriptions());
                    descriptions.put(nextIndex, nextPlay.description());

                    liveGameRepository.save(new LiveGameState(
                            latest.gamePk(),
                            latest.parentChannelId(),
                            latest.scheduleMessageId(),
                            latest.threadId(),
                            nextIndex,
                            descriptions,
                            latest.lastKnownDetailedState(),
                            latest.lastKnownInningState(),
                            latest.finalPosted(),
                            new HashSet<>(state.postedActionEventKeys())
                    ));

                    sendNextCompletedPlay(gamePk, feed, thread, game);
                },
                failure ->
                {
                    System.err.println("FAILED to sent play idx=" + nextIndex + "; will retry next tick.");
                    failure.printStackTrace();
                }
        );
    }

    private void processPlayCorrections(LiveGameState state, LiveFeed feed, ThreadChannel thread)
    {
        LiveGameState latest = liveGameRepository.findByGamePk(state.gamePk());
        if(latest == null) return;

        Map<Integer, String> descriptions = new HashMap<>(latest.postedPlayDescriptions());
        boolean changed = false;

        for(LivePlay play : feed.plays())
        {
            if(play.atBatIndex() > latest.lastPostedPlayIndex()) continue;

            String oldDescription = descriptions.get(play.atBatIndex());
            String newDescription = play.description();

            if(oldDescription == null || newDescription == null || newDescription.isBlank()) continue;

            if(!oldDescription.equals(newDescription))
            {
                thread.sendMessageEmbeds(playByPlayFormatter.formatPlayCorrection(play)).queue();

                descriptions.put(play.atBatIndex(), newDescription);
                changed = true;
            }
        }

        if(changed)
        {
            liveGameRepository.save(new LiveGameState(
                    latest.gamePk(),
                    latest.parentChannelId(),
                    latest.scheduleMessageId(),
                    latest.threadId(),
                    latest.lastPostedPlayIndex(),
                    descriptions,
                    latest.lastKnownDetailedState(),
                    latest.lastKnownInningState(),
                    latest.finalPosted(),
                    new HashSet<>(latest.postedActionEventKeys())
            ));
        }
    }

    private boolean isFinalState(String state)
    {
        String s = safeLower(state);
        return s.equals("final") || s.equals("game over");
    }

    private void processGameFinal(LiveGameState state, LiveFeed feed, ThreadChannel thread, ScheduleGame game)
    {
        if(state.finalPosted()) return;
        if(!isFinalState(feed.detailedState())) return;
        if(!isFinalDataReady(game, feed))
        {
            System.out.println("Final state seen, but final data not ready yet for gamePk " + state.gamePk());
            return;
        }

        String summary = buildSummary(feed, game);
        String decisions = buildDecisions(feed);
        String linescore = buildLinescore(feed);

        thread.sendMessageEmbeds(playByPlayFormatter.formatGameFinal(game, feed, summary, decisions, linescore)).queue();

        liveGameRepository.save(new LiveGameState(
                state.gamePk(),
                state.parentChannelId(),
                state.scheduleMessageId(),
                state.threadId(),
                state.lastPostedPlayIndex(),
                new HashMap<>(state.postedPlayDescriptions()),
                feed.detailedState(),
                state.lastKnownInningState(),
                true,
                new HashSet<>(state.postedActionEventKeys())
        ));
    }

    private boolean isFinalDataReady(ScheduleGame game, LiveFeed feed)
    {
        if (!isFinalState(feed.detailedState())) return false;
        if (feed.linescore() == null) return false;

        Integer away = feed.linescore().awayRuns();
        Integer home = feed.linescore().homeRuns();

        if (away == null || home == null) return false;

        // Only block tied finals for games where ties should not happen
        return game.isSpringTraining() || !away.equals(home);
    }

    private boolean shouldCreateThread(ScheduleGame game)
    {
        String state = game.detailedState() == null ? "" : game.detailedState().toLowerCase();
        return state.contains("warmup") || state.contains("pre-game") || state.contains("pregame") || state.contains("in progress");
    }

    private String buildSummary(LiveFeed feed, ScheduleGame game)
    {
        if(feed.linescore() == null || feed.linescore().awayRuns() == null || feed.linescore().homeRuns() == null)
        {
            return "Final.";
        }
        return "Final score: " + game.away().teamInfo().emoji().getAsMention() + " " + feed.linescore().awayRuns() + " - " + feed.linescore().homeRuns() + " " + game.home().teamInfo().emoji().getAsMention();
    }

    private String buildDecisions(LiveFeed feed)
    {
        if(feed.decisions() == null) return "";

        List<String> lines = new ArrayList<>();

        String winner = formatDecision("Winner", feed.decisions().winner());
        String loser = formatDecision("Loser", feed.decisions().loser());
        String save = formatDecision("Save", feed.decisions().save());

        if(!winner.isBlank()) lines.add(winner);
        if(!loser.isBlank()) lines.add(loser);
        if(!save.isBlank()) lines.add(save);

        return String.join("\n", lines);
    }

    private String formatDecision(String label, DecisionsSnapshot.PitcherDecision decision)
    {
        if(decision == null || decision.fullName() == null || decision.fullName().isBlank()) return "";
        if(decision.summary() == null || decision.summary().isBlank()) return label + ": " + decision.fullName();
        return label + ": " + decision.fullName() + "(" + decision.summary() + ")";
    }

    private String buildLinescore(LiveFeed feed)
    {
        if(feed.linescore() == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("R  H  E\n");
        sb.append(feed.linescore().awayRuns()).append("  ")
                .append(feed.linescore().awayHits()).append("  ")
                .append(feed.linescore().awayErrors()).append("\n")
                .append(feed.linescore().homeRuns()).append("  ")
                .append(feed.linescore().homeHits()).append("  ")
                .append(feed.linescore().homeErrors());

        return sb.toString();
    }

    private boolean isWithinLiveWindow(List<ScheduleGame> games)
    {
        if(games.isEmpty()) return false;

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        System.out.println("now = " + now);

        for(ScheduleGame g : games)
            System.out.println("window check gamePk= " + g.gamePk() + " state=[" + g.detailedState() + "] gameDate=" + g.gameDate() + " finishedOrDead=" + isFinishedOrDead(g));

        ZonedDateTime earliest = games.stream()
                .map(ScheduleGame::gameDate)
                .min(ZonedDateTime::compareTo)
                .orElse(null);

        ZonedDateTime latest = games.stream()
                .map(ScheduleGame::gameDate)
                .max(ZonedDateTime::compareTo)
                .orElse(null);

        ZonedDateTime windowStart = earliest.minusMinutes(30);
        ZonedDateTime windowEnd = latest.plusHours(5);

        System.out.println("earliest=" + earliest);
        System.out.println("latest=" + latest);
        System.out.println("windowStart=" + windowStart);
        System.out.println("windowEnd=" + windowEnd);

        return !now.isBefore(windowStart) && !now.isAfter(windowEnd);
    }

    private boolean isFinishedOrDead(ScheduleGame game)
    {
        String state = game.detailedState() == null ? "" : game.detailedState().toLowerCase();
        return state.contains("final") || state.contains("cancelled") || state.contains("canceled") || state.contains("postponed") || state.contains("game over");
    }

    private String safeLower(String value) { return value == null ? "" : value.trim().toLowerCase(); }

    private void processActionEvents(LiveGameState state, LiveFeed feed, ThreadChannel thread)
    {
        for(LiveActionEvent event : feed.actionEvents())
        {
            LiveGameState latest = liveGameRepository.findByGamePk(state.gamePk());
            if(latest == null) return;

            Set<String> posted = new HashSet<>(latest.postedActionEventKeys());
            if(posted.contains(event.key())) continue;

            posted.add(event.key());

            liveGameRepository.save(new LiveGameState(
                    latest.gamePk(),
                    latest.parentChannelId(),
                    latest.scheduleMessageId(),
                    latest.threadId(),
                    latest.lastPostedPlayIndex(),
                    new HashMap<>(latest.postedPlayDescriptions()),
                    latest.lastKnownDetailedState(),
                    latest.lastKnownInningState(),
                    latest.finalPosted(),
                    posted
            ));

            thread.sendMessageEmbeds(playByPlayFormatter.formatActionEvent(event)).queue(
                    success ->
                    {
                        System.out.println("Sent action event " + event.key());
                    },
                    failure ->
                    {
                        System.err.println("FAILED action event " + event.key());
                        failure.printStackTrace();
                    }
            );
        }
    }
}
