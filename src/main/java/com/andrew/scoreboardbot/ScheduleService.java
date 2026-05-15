package com.andrew.scoreboardbot;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ScheduleService
{
    private final MlbApiClient mlbApiClient;
    private final ScheduleFormatter formatter;
    private final ScheduleMessageRepository messageRepository;

    public ScheduleService(MlbApiClient mlbApiClient, ScheduleFormatter formatter, ScheduleMessageRepository messageRepository)
    {
        this.mlbApiClient = mlbApiClient;
        this.formatter = formatter;
        this.messageRepository = messageRepository;
    }

    private List<ScheduleGame> orderGamesForDisplay(List<ScheduleGame> games)
    {
        List<ScheduleGame> ordered = new ArrayList<>(games);

        ordered.sort((a, b) ->
        {
            int timeCompare = a.gameDate().compareTo(b.gameDate());
            if(timeCompare != 0) return timeCompare;
            boolean sameDoubleheaderPair = a.isDoubleheader() && b.isDoubleheader()
                    && a.away().teamInfo().teamId() == b.away().teamInfo().teamId()
                    && a.home().teamInfo().teamId() == b.home().teamInfo().teamId()
                    && a.gameDate().toLocalDate().equals(b.gameDate().toLocalDate());

            if(sameDoubleheaderPair) return Integer.compare(a.gameNumber(), b.gameNumber());
            return 0;
        });
        return ordered;
    }

    private String doubleheaderGroupKey(ScheduleGame game)
    {
        return game.away().teamInfo().teamId()
                + "-"
                + game.home().teamInfo().teamId()
                + "-"
                + game.gameDate().toLocalDate()
                + "-"
                + game.doubleHeader();
    }

    public void postDailySchedule(TextChannel channel, String url, LocalDate date) throws Exception
    {
        List<ScheduleGame> games = mlbApiClient.getScheduleJsonFromUrl(url);
        List<ScheduleGame> finalGames = orderGamesForDisplay(games);

        if(finalGames.isEmpty())
            return;

        boolean springTraining = finalGames.stream().allMatch(g -> g.gameType().equalsIgnoreCase("S") || g.gameType().equalsIgnoreCase("E"));

        channel.sendMessage(formatter.formatDateHeader(date, springTraining)).queue(success -> sendGamesSequentially(channel, finalGames, date, 0));
    }

    private void sendGamesSequentially(TextChannel channel, List<ScheduleGame> games, LocalDate date, int index)
    {
        if(index >= games.size())
            return;

        ScheduleGame game = games.get(index);
        FormattedGameMessage formattedGame = formatter.formatGame(game);

        channel.sendMessage(formattedGame.content()).queue(sentMessage ->
        {
            messageRepository.save(new PostedGameState(
                    game.gamePk(),
                    sentMessage.getIdLong(),
                    channel.getIdLong(),
                    date,
                    game.awayProbableName(),
                    game.homeProbableName(),
                    game.detailedState(),
                    formatter.renderGameContent(game),
                    formatter.formatProbableLine(game)
            ));
            System.out.println("SAVED POSTED STATE gamePk=" + game.gamePk()
                    + " channelId=" + channel.getIdLong()
                    + " date=" + date
                    + " totalForDate=" + messageRepository.findByDate(date).size());
            sendGamesSequentially(channel, games, date, index + 1);
        });
    }
}
