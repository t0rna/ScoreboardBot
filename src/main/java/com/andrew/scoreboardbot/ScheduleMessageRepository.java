package com.andrew.scoreboardbot;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleMessageRepository
{
    void save(PostedGameState state);
    PostedGameState findByGamePk(int gamePk, long channelId);
    List<PostedGameState> findByDate(LocalDate date);
    void deleteAll();
    List<PostedGameState> findAll();
}