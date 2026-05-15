package com.andrew.scoreboardbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import com.andrew.scoreboardbot.InMemoryScheduleMessageRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class ScheduleMonitoringService
{
    private final JDA jda;
    private final MlbApiClient mlbApiClient;
    private final ScheduleFormatter formatter;
    private final ScheduleMessageRepository repository;

    public ScheduleMonitoringService(JDA jda, MlbApiClient mlbApiClient, ScheduleFormatter formatter, ScheduleMessageRepository repository)
    {
        this.jda = jda;
        this.mlbApiClient = mlbApiClient;
        this.formatter = formatter;
        this.repository = repository;
    }

    public void checkForUpdates(String url, LocalDate date) throws Exception
    {
        List<ScheduleGame> latestGames = mlbApiClient.getScheduleJsonFromUrl(url);
        List<PostedGameState> postedStates = repository.findByDate(date);

        for(ScheduleGame game : latestGames)
        {
            for(PostedGameState oldState : postedStates)
            {
                if(oldState.gamePk() != game.gamePk()) continue;

                String probableLineToUse = game.isPregame() ? formatter.formatProbableLine(game) : oldState.postedProbableLine();
                String newContent = formatter.renderGameContent(game, probableLineToUse);
                boolean contentChanged = !Objects.equals(oldState.gameContentHash(), newContent);
                if(!contentChanged) continue;

                TextChannel channel = jda.getTextChannelById(oldState.channelId());
                if(channel == null)
                {
                    System.err.println("Could not find channel " + oldState.channelId() + " for gamePk" + game.gamePk());
                    continue;
                }

                channel.retrieveMessageById(oldState.messageId()).queue(message ->
                {
                    message.editMessage(newContent).queue(success ->
                    {
                        String updatedStoredProbableLine = game.isPregame() ? formatter.formatProbableLine(game) : oldState.postedProbableLine();
                        repository.save(
                                new PostedGameState(
                                        game.gamePk(),
                                        oldState.messageId(),
                                        oldState.channelId(),
                                        date,
                                        game.awayProbableName(),
                                        game.homeProbableName(),
                                        game.detailedState(),
                                        newContent,
                                        updatedStoredProbableLine
                                )
                        );

                        System.out.println("Updated gamePk " + game.gamePk() + " in channel " + oldState.channelId());
                    }, failure ->
                    {
                        System.err.println("Failed to edit message for gamePk " + game.gamePk() + " in channel " + oldState.channelId());
                        failure.printStackTrace();
                    });
                }, failure ->
                {
                    System.err.println("Failed to retrieve message " + oldState.messageId() + " for gamePk " + game.gamePk() + " in channel " + oldState.channelId());
                    failure.printStackTrace();
                });
            }
        }
    }
}
