package com.andrew.scoreboardbot;

import java.util.List;

public interface LiveGameRepository
{
    void save(LiveGameState state);
    LiveGameState findByGamePk(int gamePk);
    List<LiveGameState> findAll();
    void delete(int gamePk);
    void deleteAll();
}
