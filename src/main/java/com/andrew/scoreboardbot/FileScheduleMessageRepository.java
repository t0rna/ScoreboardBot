package com.andrew.scoreboardbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FileScheduleMessageRepository implements ScheduleMessageRepository
{

    private final Path path;
    private final Gson gson;
    private final List<PostedGameState> states = new ArrayList<>();

    public FileScheduleMessageRepository(Path path)
    {
        this.path = path;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    @Override
    public synchronized void save(PostedGameState state)
    {
        states.add(state);
        persist();
    }

    @Override
    public PostedGameState findByGamePk(int gamePk, long channelId)
    {
        for(PostedGameState state : states)
        {
            if(state.gamePk() == gamePk && state.channelId() == channelId)
                return state;
        }
        return null;
    }

    @Override
    public List<PostedGameState> findByDate(LocalDate date)
    {
        return states.stream().filter(s -> s.scheduleDate().equals(date)).toList();
    }

    @Override
    public synchronized void deleteAll()
    {
        states.clear();
        persist();
    }

    @Override
    public synchronized List<PostedGameState> findAll()
    {
        return new ArrayList<>(states);
    }

    private void load()
    {
        try
        {
            if(!Files.exists(path)) return;

            String json = Files.readString(path);
            Type type = new TypeToken<List<PostedGameState>>() {}.getType();
            List<PostedGameState> loaded = gson.fromJson(json, type);
            if(loaded != null)
            {
                states.clear();
                states.addAll(loaded);
            }

            System.out.println("Loaded schedule message states: " + states.size());
        }
        catch(Exception e)
        {
            System.err.println("Failed to load schedule message states");
            e.printStackTrace();
        }
    }

    private void persist()
    {
        try
        {
            Files.createDirectories(path.getParent());

            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temp, gson.toJson(states), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch(Exception e)
        {
            System.err.println("Failed to persist schedule message states");
            e.printStackTrace();
        }
    }
}
