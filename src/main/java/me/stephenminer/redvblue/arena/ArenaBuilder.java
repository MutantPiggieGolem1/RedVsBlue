package me.stephenminer.redvblue.arena;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.chests.GameChest;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;

public class ArenaBuilder {
    private final String id;
    private final RedBlue plugin;

    private Location loc1;
    private Location loc2;
    private Location lobby;
    private Location blueLoc;
    private Location redLoc;
    private String name;

    public ArenaBuilder(String id, RedBlue plugin){
        this.plugin = plugin;
        this.id = id;
    }


    public void loadData(){
        String base = "arenas." + id;
        loc1 = plugin.fromString(plugin.arenas.getConfig().getString(base + ".loc1"));
        loc2 = plugin.fromString(plugin.arenas.getConfig().getString(base + ".loc2"));
        blueLoc = plugin.fromString(plugin.arenas.getConfig().getString(base + ".blue-spawn"));
        redLoc = plugin.fromString(plugin.arenas.getConfig().getString(base + ".red-spawn"));
        lobby = plugin.fromString(plugin.arenas.getConfig().getString(base + ".lobby"));
        name = plugin.arenas.getConfig().getString( base + ".name");
    }

    public Set<GameChest> loadChests(){
        String path = "arenas." + id + ".chests";
        Set<GameChest> chests = new HashSet<>();
        if (plugin.arenas.getConfig().contains(path)) {
            Set<String> stringLocs = plugin.arenas.getConfig().getConfigurationSection(path).getKeys(false);
            for (String sLoc : stringLocs) {
                Location loc = plugin.fromString(sLoc);
                String chestId = plugin.arenas.getConfig().getString(path + "." + sLoc);
                Material mat = Material.matchMaterial(plugin.tables.getConfig().getString("tables." + chestId + ".type"));
                boolean postWall = plugin.tables.getConfig().getBoolean("tables." + chestId + ".post-wall");
                GameChest chest = new GameChest(loc, mat, chestId);
                chest.setPostWall(postWall);
                chests.add(chest);
            }
        }
        return chests;
    }
    public Wall loadWall(){
        String base = "arenas." + id + ".wall";
        Location loc1 = plugin.fromString(plugin.arenas.getConfig().getString(base + ".loc1"));
        Location loc2 = plugin.fromString(plugin.arenas.getConfig().getString(base  + ".loc2"));
        Material mat = Material.matchMaterial(plugin.arenas.getConfig().getString(base + ".type"));
        return new Wall(mat, loc1, loc2);
    }

    public int getFallTime(){
        String path = "settings.wall-time";
        return plugin.settings.getConfig().getInt(path);
    }


    public Arena build(){
        Wall wall = loadWall();
        Arena arena = new Arena(id, name, loc1, loc2, redLoc, blueLoc, lobby);
        arena.setWall(wall);
        wall.buildWall();
        arena.setFallTime(getFallTime());
        arena.addChests(loadChests());
        Arena.arenas.add(arena);
        return arena;
    }


}
