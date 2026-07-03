package com.andrew.scoreboardbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        String token = System.getenv("DISCORD_TOKEN");

        JDA jda = JDABuilder.createDefault(token).build().awaitReady();

        Guild guild = jda.getGuildById(297013639129464832L);
        if(guild == null)
             throw new IllegalStateException("Guild not found.");

        TextChannel gameday = guild.getTextChannelById(811348878951055383L);

        if(gameday == null)
            throw new IllegalStateException("Channel not found.");

        TeamRegistry teamRegistry = new TeamRegistry(jda);
        MlbApiClient mlbApiClient = new MlbApiClient(teamRegistry);
        ScheduleFormatter formatter = new ScheduleFormatter(jda);

        ScheduleMessageRepository scheduleRepository = new FileScheduleMessageRepository(Path.of("/home/t0rna/scoreboardbot-state/schedule-messages.json"));
        LiveGameRepository liveGameRepository = new FileLiveGameRepository(Path.of("/home/t0rna/scoreboardbot-state/live-games.json"));

        ScheduleService scheduleService = new ScheduleService(mlbApiClient, formatter, scheduleRepository);
        DailyScheduler scheduler = new DailyScheduler();
        ScheduleMonitoringService monitoringService = new ScheduleMonitoringService(jda, mlbApiClient, formatter, scheduleRepository);


        ThreadTitleFormatter threadTitleFormatter = new ThreadTitleFormatter();
        PlayByPlayFormatter playByPlayFormatter = new PlayByPlayFormatter();
        LiveGameMonitorService liveGameMonitorService = new LiveGameMonitorService(
                jda,
                mlbApiClient,
                scheduleRepository,
                liveGameRepository,
                threadTitleFormatter,
                playByPlayFormatter
        );

        scheduler.scheduleDaily(() ->
        {
            try
            {
                liveGameRepository.deleteAll();
                scheduleRepository.deleteAll();
                LocalDate date = LocalDate.now(ZoneId.of("America/Chicago"));
                String url = "https://statsapi.mlb.com/api/v1/schedule?sportId=1&date=" + date + "&hydrate=probablePitcher,stats,team,broadcasts,game(content(media(epg)))";
                // scheduleService.postDailySchedule(drafting, url, date);
                scheduleService.postDailySchedule(gameday, url, date);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }, LocalTime.of(8, 0));

        ScheduledExecutorService updateExecutor = Executors.newSingleThreadScheduledExecutor();

        updateExecutor.scheduleAtFixedRate(() ->
        {
            try
            {
                LocalDate date = LocalDate.now(ZoneId.of("America/Chicago"));
                String url = "https://statsapi.mlb.com/api/v1/schedule?sportId=1&date=" + date + "&hydrate=probablePitcher,stats,team,broadcasts,game(content(media(epg)))";
                monitoringService.checkForUpdates(url, date);
            }
            catch(Exception e)
            {
                System.err.println("Schedule update check failed");
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.MINUTES);

        LocalDate date = LocalDate.now();

        ScheduledExecutorService liveExecutor = Executors.newSingleThreadScheduledExecutor();

        liveExecutor.scheduleAtFixedRate(() ->
        {
            try
            {
                boolean baseballHours = isWithinBaseballHours();
                System.out.println("LIVE SCHEDULER baseballHours=" + baseballHours
                        + " now=" + java.time.LocalDateTime.now());
                if(!baseballHours) return;
                LocalDate date1 = mlbScheduleDateNow();
                String url = "https://statsapi.mlb.com/api/v1/schedule?sportId=1&date=" + date1 + "&hydrate=probablePitcher,stats,team,broadcasts,game(content(media(epg)))";
                liveGameMonitorService.tick(url, date);
            }
            catch(Exception e)
            {
                System.err.println("Live game monitor tick failed.");
                e.printStackTrace();
            }
        }, 20, 20, TimeUnit.SECONDS);

        jda.addEventListener(new ThreadSystemMessageCleaner());

    }

    private static LocalDate mlbScheduleDateNow()
    {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Chicago"));
        if(now.toLocalTime().isBefore(LocalTime.of(6, 0))) return now.toLocalDate().minusDays(1);
        return now.toLocalDate();
    }

    private static boolean isWithinBaseballHours()
    {
        ZoneId zone = ZoneId.of("America/Chicago");
        LocalTime now = LocalTime.now(zone);

        LocalTime start = LocalTime.of(11, 0);
        LocalTime end = LocalTime.of(3, 0);

        return !now.isBefore(start) || now.isBefore(end);
    }
}