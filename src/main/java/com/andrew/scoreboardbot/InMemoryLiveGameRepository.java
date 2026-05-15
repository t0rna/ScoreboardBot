package com.andrew.scoreboardbot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLiveGameRepository implements LiveGameRepository
{
    private final Map<Integer, LiveGameState> byGamePk = new ConcurrentHashMap<>();

    @Override
    public void save(LiveGameState state)
    {
        byGamePk.put(state.gamePk(), state);
    }

    @Override
    public LiveGameState findByGamePk(int gamePk)
    {
        return byGamePk.get(gamePk);
    }

    @Override
    public List<LiveGameState> findAll()
    {
        return new ArrayList<>(byGamePk.values());
    }

    @Override
    public void delete(int gamePk)
    {
        byGamePk.remove(gamePk);
    }

    @Override
    public void deleteAll()
    {
        byGamePk.clear();
    }
}
