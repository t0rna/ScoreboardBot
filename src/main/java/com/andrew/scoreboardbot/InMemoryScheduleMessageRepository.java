package com.andrew.scoreboardbot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class InMemoryScheduleMessageRepository implements ScheduleMessageRepository
{

    private final Map<String, PostedGameState> byKey = new ConcurrentHashMap<>();

    @Override
    public void save(PostedGameState state) { byKey.put(key(state.gamePk(), state.channelId()), state); }

    private String key(int gamePk, long channelId) { return gamePk + "-" + channelId; }

    @Override
    public PostedGameState findByGamePk(int gamePk, long channelId)
    {
        return byKey.get(key(gamePk, channelId));
    }

    @Override
    public List<PostedGameState> findByDate(LocalDate date)
    {
        return byKey.values().stream()
                .filter(s -> s.scheduleDate().equals(date))
                .toList();
    }

    @Override
    public void deleteAll() { byKey.clear(); }

    @Override
    public List<PostedGameState> findAll() { return new ArrayList<>(byKey.values()); }
}
