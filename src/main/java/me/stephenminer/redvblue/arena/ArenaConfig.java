package me.stephenminer.redvblue.arena;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.util.BlockVector;

import me.stephenminer.redvblue.util.ArenaConfigUtil;
import me.stephenminer.redvblue.util.BlockRange;

/**
 * A representation of an Arena on file (in the config)
 */
public class ArenaConfig implements ConfigurationSerializable {
    private @Nonnull String id;

    private @Nonnull  BlockRange bounds;
    private @Nullable Location lobby;
    private final @Nonnull Map<BlockRange, Material> walls; // Empty if none created
    private final @Nonnull Map<String, BlockVector> spawns;
    private int wallFallTime;

    public static ArenaConfig builder(String id, BlockRange bounds) {
        if (id == null || bounds == null) return null;
        var arena = new ArenaConfig(id, bounds, 0);
        ArenaConfigUtil.saveToFileShallow(id, arena);
        return arena;
    }

    private ArenaConfig(String id, BlockRange bounds, int wallFallTime) {
        assert id != null && bounds != null;
        this.id = id;
        this.bounds = bounds;
        this.walls = new HashMap<>();
        this.spawns = new HashMap<>();
        this.wallFallTime = wallFallTime;
    }

    public @Nullable Arena build() {
        if (lobby == null || spawns.size() < 2) return null;
        var world = bounds.world();
        var a = new Arena(id, id, bounds, spawns.get("red").toLocation(world), spawns.get("blue").toLocation(world), lobby);
        for (var wall : walls.entrySet()) a.addWall(new Wall(wall.getValue(), wall.getKey()));
        a.setFallTime(wallFallTime);
        Arena.arenas.add(a);
        return a;
    }

    public boolean contains(Location l) {
        return bounds.contains(l);
    }

    public boolean hasTeam(String team) {
        return spawns.containsKey(team);
    }

    public Set<String> getTeams() {
        return new HashSet<>(spawns.keySet());
    }

    public Optional<BlockRange> findWall(Location l) {
        if (!l.getWorld().equals(bounds.world())) return Optional.empty();
        return findWall(new BlockVector(l.toVector()));
    }

    public Optional<BlockRange> findWall(BlockVector loc) {
        return walls.keySet().stream().filter((r) -> r.contains(loc)).findFirst();
    }

    public boolean createWall(Material mat, BlockRange range) {
        for (var wallRange : walls.keySet()) {
            if (range.toBoundingBox().overlaps(wallRange.toBoundingBox()))
                return false;
        }
        walls.put(range, mat);
        range.fill(mat);
        return true;
    }

    public boolean destroyWall(BlockRange range) {
        if (!walls.containsKey(range)) return false;
        walls.remove(range);
        range.fill(Material.AIR);
        return true;
    }

    public void setLobby(Location l) {
        lobby = l;
    }

    public BlockVector setSpawn(String team, BlockVector l) {
        var old = spawns.containsKey(team) ? spawns.get(team).clone() : null;
        spawns.put(team, l);
        return old;
    }

    public void setWallFallTime(@Nonnegative int newTime) {
        wallFallTime = newTime;
    }

    public String id() {
        return id;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> dat = new HashMap<>();
        dat.put("id", id);
        dat.put("bounds", bounds.serialize());
        if (lobby != null) dat.put("lobby", lobby.serialize());

        Map<Map<String, Object>, String> wallsSerialized = new HashMap<>();
        for (var e : walls.entrySet()) {
            wallsSerialized.put(e.getKey().serialize(), e.getValue().name());
        }
        dat.put("walls", wallsSerialized);
        
        Map<String, Map<String, Object>> spawnsSerialized = new HashMap<>();
        for (var e : spawns.entrySet()) {
            spawnsSerialized.put(e.getKey(), e.getValue().serialize());
        }
        dat.put("spawns", spawnsSerialized);
        
        dat.put("wallfalltime", wallFallTime);
        return dat;
    }
    
    public static ArenaConfig deserialize(Map<String, Object> dat) {return new ArenaConfig(dat);}
    public static ArenaConfig valueOf(Map<String, Object> dat) {return new ArenaConfig(dat);}
    @SuppressWarnings("unchecked")
    public ArenaConfig(Map<String, Object> dat) {
        assert dat.containsKey("id");
        id = (String) dat.get("id");
        
        assert dat.containsKey("bounds");
        bounds = BlockRange.deserialize((Map<String, Object>) dat.get("bounds"));

        if (dat.containsKey("lobby")) {
            lobby = Location.deserialize((Map<String, Object>) dat.get("lobby"));
        }

        if (dat.containsKey("walls")) {
            Map<BlockRange, Material> wallsDeserialized = new HashMap<>();
            for (var e : ((Map<Map<String, Object>, String>) dat.get("walls")).entrySet()) {
                wallsDeserialized.put(BlockRange.deserialize(e.getKey()), Material.getMaterial(e.getValue()));
            }
            walls = wallsDeserialized;
        } else {
            walls = new HashMap<>();
        }
        
        if (dat.containsKey("spawns")) {
            Map<String, BlockVector> spawnsDeserialized = new HashMap<>();
            for (var e : ((Map<String, Map<String, Object>>) dat.get("spawns")).entrySet()) {
                spawnsDeserialized.put(e.getKey(), BlockVector.deserialize(e.getValue()));
            }
            spawns = spawnsDeserialized;
        } else {
            spawns = new HashMap<>();
        }

        assert dat.containsKey("wallfalltime");
        wallFallTime = (Integer) dat.get("wallfalltime");
    }

    static {
        ConfigurationSerialization.registerClass(ArenaConfig.class);
    }
}

/* TODO implement
public Set<NewLootChest> loadLootChests(){
    String path = "arenas." + id + ".loot-chests";
    Set<NewLootChest> chests = new HashSet<>();
    if (!plugin.arenas.getConfig().contains(path)) return chests;
    List<String> stringChests  = plugin.arenas.getConfig().getStringList(path);
    for (String entry : stringChests){
        NewLootChest lootChest = new NewLootChest(entry);
        chests.add(lootChest);
    }
    return chests;
}
*/