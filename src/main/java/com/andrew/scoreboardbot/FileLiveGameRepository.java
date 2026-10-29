package com.andrew.scoreboardbot;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileLiveGameRepository implements LiveGameRepository
{
    private final Path path;
    private final Gson gson;
    private final Map<Integer, LiveGameState> byGamePk = new HashMap<>();

    public FileLiveGameRepository(Path path)
    {
        this.path = path;
        this.gson = buildGson();

        load();
    }

    @Override
    public synchronized LiveGameState findByGamePk(int gamePk) { return byGamePk.get(gamePk); }

    @Override
    public List<LiveGameState> findAll()
    {
        return byGamePk.values().stream().toList();
    }

    @Override
    public void delete(int gamePk)
    {
        byGamePk.remove(gamePk);
    }

    @Override
    public synchronized void save(LiveGameState state) { byGamePk.put(state.gamePk(), state); persist(); }

    @Override
    public synchronized void deleteAll() { byGamePk.clear(); persist(); }

    private void load()
    {
        try
        {
            if(!Files.exists(path)) return;

            String json = Files.readString(path);
            Type type = new TypeToken<Map<Integer, LiveGameState>>() {}.getType();
            Map<Integer, LiveGameState> loaded = gson.fromJson(json, type);
            if(loaded != null)
            {
                byGamePk.clear();
                byGamePk.putAll(loaded);
            }

            System.out.println("Loaded live game states: " + byGamePk.size());
        }
        catch(Exception e)
        {
            System.err.println("Failed to load live games states.");
            e.printStackTrace();
        }
    }

    private void persist()
    {
        try
        {
            Files.createDirectories(path.getParent());

            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temp, gson.toJson(byGamePk), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch(Exception e)
        {
            System.err.println("Failed to persist live game states.");
            e.printStackTrace();
        }
    }

    private Gson buildGson()
    {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                                new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                                LocalDate.parse(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }

}
