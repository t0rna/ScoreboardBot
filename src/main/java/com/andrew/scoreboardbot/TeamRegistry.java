package com.andrew.scoreboardbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;

import java.util.HashMap;
import java.util.Map;

public class TeamRegistry
{
    private final Map<String, TeamInfo> byName = new HashMap<>();
    private final Map<Integer, TeamInfo> byId = new HashMap<>();

    public TeamRegistry(JDA jda)
    {
        register(jda, 120, "Washington Nationals", "Nationals", League.NATIONAL, League.GRAPEFRUIT, 344303505240227860L, Roles.NATIONALS, false, "WAS");
        register(jda, 138, "St. Louis Cardinals", "Cardinals", League.NATIONAL, League.GRAPEFRUIT, 821071534738767892L, Roles.CARDINALS, false, "STL");
        register(jda, 134, "Pittsburgh Pirates", "Pirates", League.NATIONAL, League.GRAPEFRUIT, 344303369705357312L, Roles.PIRATES, false, "PIT");
        register(jda, 139, "Tampa Bay Rays", "Rays", League.AMERICAN, League.GRAPEFRUIT, 821077079231299636L, Roles.RAYS, false, "TB");
        register(jda, 142, "Minnesota Twins", "Twins", League.AMERICAN, League.GRAPEFRUIT, 821076927069683714L, Roles.TWINS, false, "MIN");
        register(jda, 111, "Boston Red Sox", "Red Sox", League.AMERICAN, League.GRAPEFRUIT, 344305725369221125L, Roles.RED_SOX, false, "BOS");
        register(jda, 144, "Atlanta Braves", "Braves", League.NATIONAL, League.GRAPEFRUIT, 344303732869300224L, Roles.BRAVES, false, "ATL");
        register(jda, 143, "Philadelphia Phillies", "Phillies", League.NATIONAL, League.GRAPEFRUIT, 344303952101376000L, Roles.PHILLIES, false, "PHI");
        register(jda, 146, "Miami Marlins", "Marlins", League.NATIONAL, League.GRAPEFRUIT, 763205433435095050L, Roles.MARLINS, false, "MIA");
        register(jda, 117, "Houston Astros", "Astros", League.AMERICAN, League.GRAPEFRUIT, 346460958165565440L, Roles.ASTROS, false, "HOU");
        register(jda, 116, "Detroit Tigers", "Tigers", League.AMERICAN, League.GRAPEFRUIT, 1346284342023950458L, Roles.TIGERS, false, "DET");
        register(jda, 147, "New York Yankees", "Yankees", League.AMERICAN, League.GRAPEFRUIT, 1346284361867329567L, Roles.YANKEES, false, "NYY");
        register(jda, 141, "Toronto Blue Jays", "Blue Jays", League.AMERICAN, League.GRAPEFRUIT, 821077358857551892L, Roles.BLUE_JAYS, false, "TOR");
        register(jda, 121, "New York Mets", "Mets", League.NATIONAL, League.GRAPEFRUIT, 344303849869410304L, Roles.METS, false, "NYM");
        register(jda, 119, "Los Angeles Dodgers", "Dodgers", League.NATIONAL, League.CACTUS, 344304090475659266L, Roles.DODGERS, false, "LAD");
        register(jda, 112, "Chicago Cubs", "Cubs", League.NATIONAL, League.CACTUS, 344302345854779404L, Roles.CUBS, false, "CHC");
        register(jda, 133, "Athletics", "Athletics", League.AMERICAN, League.CACTUS, 1379268008190410855L, Roles.ATHLETICS, false, "ATH");
        register(jda, 114, "Cleveland Guardians", "Guardians", League.AMERICAN, League.CACTUS, 952059137343238164L, Roles.GUARDIANS, false, "CLE");
        register(jda, 140, "Texas Rangers", "Rangers", League.AMERICAN, League.CACTUS, 344305726048567296L, Roles.RANGERS, false, "TEX");
        register(jda, 145, "Chicago White Sox", "White Sox", League.AMERICAN, League.CACTUS, 344302739968491520L, Roles.WHITE_SOX, false, "CHW");
        register(jda, 118, "Kansas City Royals", "Royals", League.AMERICAN, League.CACTUS, 821077495024582737L, Roles.ROYALS, false, "KC");
        register(jda, 158, "Milwaukee Brewers", "Brewers", League.NATIONAL, League.CACTUS, 344303041773568021L, Roles.BREWERS, false, "MIL");
        register(jda, 137, "San Francisco Giants", "Giants", League.NATIONAL, League.CACTUS, 344304615036289025L, Roles.GIANTS, false, "SF");
        register(jda, 115, "Colorado Rockies", "Rockies", League.NATIONAL, League.CACTUS, 344304203629330443L, Roles.ROCKIES, false, "COL");
        register(jda, 108, "Los Angeles Angels", "Angels", League.AMERICAN, League.CACTUS, 344305737431908352L, Roles.ANGELS, false, "LAA");
        register(jda, 135, "San Diego Padres", "Padres", League.NATIONAL, League.CACTUS, 768180992510394418L, Roles.PADRES, false, "SD");
        register(jda, 109, "Arizona Diamondbacks", "Diamondbacks", League.NATIONAL, League.CACTUS, 344304325243437067L, Roles.DIAMONDBACKS, false, "ARI");
        register(jda, 113, "Cincinnati Reds", "Reds", League.NATIONAL, League.CACTUS, 344303187504922624L, Roles.REDS, false, "CIN");
        register(jda, 136, "Seattle Mariners", "Mariners", League.AMERICAN, League.CACTUS, 344305726069669890L, Roles.MARINERS, false, "SEA");
        register(jda, 110, "Baltimore Orioles", "Orioles", League.AMERICAN, League.GRAPEFRUIT, 344305725922738186L, Roles.ORIOLES, false, "BAL");
        register(jda, 159, "American League All-Stars", "AL", League.AMERICAN, null, 344302519167483904L, Roles.AL, false, "AL");
        register(jda, 160, "National League All-Stars", "NL", League.NATIONAL, null, 344302564315234304L, Roles.NL, false, "NL");
    }

    private void register(JDA jda, int id, String name, String shortName, League league, League springLeague, long emojiId, Roles role, boolean external, String abbreviation)
    {
        CustomEmoji emoji = jda.getEmojiById(emojiId);
        TeamInfo info = new TeamInfo(id, name, shortName, league, springLeague, emoji, role, external, abbreviation);
        byName.put(name, info);
        byId.put(id, info);
    }

    public TeamInfo getbyName(String name)
    {
        TeamInfo info = byName.get(name);
        if(info == null)
            throw new IllegalArgumentException("Unknown team: " + name);
        return info;
    }

    public TeamInfo getByIdOrExternal(int id, String name)
    {
        TeamInfo info = byId.get(id);
        if(info != null)
            return info;
        return new TeamInfo(id, name, name, null, null, null, null, true, name.substring(0, 2).toUpperCase());
    }
}
