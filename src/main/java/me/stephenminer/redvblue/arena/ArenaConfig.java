package me.stephenminer.redvblue.arena;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.BlockVector;

import me.stephenminer.redvblue.BlockRange;

public class ArenaConfig implements ConfigurationSerializable {
    private @Nonnull String name;

    private @Nonnull  BlockRange bounds;
    private @Nullable Location lobby;
    private @Nonnull  Map<Material, BlockRange> walls; // Empty if none created
    private @Nullable Map<String, BlockVector> spawns;

    public @Nullable Arena build(int fallTime) {
        if (lobby == null || spawns == null || spawns.size() < 2) return null;
        var world = bounds.world();
        var a = new Arena("id", name, bounds, spawns.get("red").toLocation(world), spawns.get("blue").toLocation(world), lobby);
        for (var wall : walls.entrySet()) a.addWall(new Wall(wall.getKey(), wall.getValue()));
        a.getWalls().forEach(Wall::buildWall);
        a.setFallTime(fallTime);
        Arena.arenas.add(a);
        return a;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> dat = new HashMap<>();
        dat.put("name", name);
        dat.put("bounds", bounds.serialize());
        if (lobby != null) dat.put("lobby", lobby.serialize());

        Map<String, Map<String, Object>> wallsSerialized = new HashMap<>();
        for (var e : walls.entrySet()) {
            wallsSerialized.put(e.getKey().name(), e.getValue().serialize());
        }
        dat.put("walls", wallsSerialized);
        
        if (spawns != null) {
            Map<String, Map<String, Object>> spawnsSerialized = new HashMap<>();
            for (var e : spawns.entrySet()) {
                spawnsSerialized.put(e.getKey(), e.getValue().serialize());
            }
            dat.put("spawns", spawnsSerialized);
        }
        return dat;
    }
    
    public static ArenaConfig deserialize(Map<String, Object> dat) {
        return new ArenaConfig(dat);
    }

    public static ArenaConfig valueOf(Map<String, Object> dat) {
        return new ArenaConfig(dat);
    }

    @SuppressWarnings("unchecked")
    public ArenaConfig(Map<String, Object> dat) {
        assert dat.containsKey("name");
        name = (String) dat.get("name");
        
        assert dat.containsKey("bounds");
        bounds = BlockRange.deserialize((Map<String, Object>) dat.get("bounds"));

        if (dat.containsKey("lobby")) {
            lobby = Location.deserialize((Map<String, Object>) dat.get("lobby"));
        }

        if (dat.containsKey("walls")) {
            Map<Material, BlockRange> wallsDeserialized = new HashMap<>();
            for (var e : ((Map<String, Map<String, Object>>) dat.get("walls")).entrySet()) {
                wallsDeserialized.put(Material.getMaterial(e.getKey()), BlockRange.deserialize(e.getValue()));
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
        }
    }
}
