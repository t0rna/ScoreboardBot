package com.andrew.scoreboardbot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DailyScheduler
{
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void scheduleDaily(Runnable task, LocalTime time)
    {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.with(time);

        if(now.compareTo(nextRun) > 0)
        {
            nextRun = nextRun.plusDays(1);
        }

        long initialDelay = Duration.between(now, nextRun).toMillis();
        long period = TimeUnit.DAYS.toMillis(1);

        scheduler.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.MILLISECONDS);
    }
}
