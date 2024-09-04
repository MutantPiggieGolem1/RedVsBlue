package me.stephenminer.redvsblue.util;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public record BlockRange(World world, BlockVector p1, BlockVector p2) implements ConfigurationSerializable {
    public void forEach(LocationCallback callback) {
        var min = BlockVector.getMinimum(p1, p2);
        var max = BlockVector.getMaximum(p1, p2);
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++)
        for (int y = min.getBlockY(); y <= max.getBlockY(); y++)
        for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++)
            callback.run(new Location(world, x, y, z));
    }

    public void fill(Material m) {
        forEach((Location l) -> l.getBlock().setType(m));
    }

    public BoundingBox toBoundingBox() {
        Vector offset = new Vector(0.5, 0.5, 0.5);
        return BoundingBox.of(p1.clone().add(offset), p2.clone().add(offset));
    }

    public Location center() {
        return p1.getMidpoint(p2).toLocation(world);
    }

    public boolean contains(BlockVector other) {
        return toBoundingBox().contains(other);
    }

    public boolean contains(Location other) {
        return other.getWorld().equals(world) && toBoundingBox().contains(other.toVector());
    }

    @Override
    public final String toString() {
        return world.getName() + "/(" + p1.toString() + ")~(" + p2.toString() + ")";
    }

    public static final BlockRange fromLocations(Location loc1, Location loc2) {
        assert loc1.getWorld().equals(loc2.getWorld());
        return new BlockRange(loc1.getWorld(), new BlockVector(loc1.toVector()), new BlockVector(loc2.toVector()));
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
            "world", world.getUID().toString(),
            "p1", p1.serialize(),
            "p2", p2.serialize()
        );
    }

    public static BlockRange deserialize(Map<String, Object> dat) {return new BlockRange(dat);}
    public static BlockRange valueOf(Map<String, Object> dat) {return new BlockRange(dat);}
    @SuppressWarnings("unchecked")
    public BlockRange(Map<String, Object> dat) {
        this(
            Bukkit.getWorld(UUID.fromString((String) dat.get("world"))),
            BlockVector.deserialize((Map<String,Object>) dat.get("p1")),
            BlockVector.deserialize((Map<String,Object>) dat.get("p2"))
        );
    }
}
