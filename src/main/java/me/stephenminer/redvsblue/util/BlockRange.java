package me.stephenminer.redvsblue.util;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;

public record BlockRange(World world, BlockVector p1, BlockVector p2) implements ConfigurationSerializable {
    /**
     * @return A {@link BoundingBox} fully containing both corners of the range.
     */
    private BoundingBox toBoundingBox() {
        return BoundingBox.of(p1.toLocation(world).getBlock(), p2.toLocation(world).getBlock());
    }

    public boolean overlaps(BlockRange other) {
        return other.world().equals(world) && toBoundingBox().overlaps(other.toBoundingBox());
    }

    public boolean contains(BlockVector other) {
        return toBoundingBox().contains(other);
    }

    public boolean contains(Location other) {
        return other.getWorld().equals(world) && toBoundingBox().contains(other.toVector());
    }
    
    public boolean contains(Block block) {
        return block.getWorld().equals(world) && toBoundingBox().contains(block.getLocation().add(0.5, 0.5, 0.5).toVector());
    }

    public Location center() {
        return p1.getMidpoint(p2).toLocation(world);
    }

    public int volume() {
        var t = p1.subtract(p2);
        return Math.abs(t.getBlockX() * t.getBlockY() * t.getBlockZ());
    }

    public Collection<Entity> getTransitiveEntities() {
        return world.getNearbyEntities(toBoundingBox(), e -> !(e instanceof Player || e instanceof Hanging || e instanceof ArmorStand));
    }

    @Override
    public final String toString() {
        return world.getName() + "/(" + p1.toString() + ")~(" + p2.toString() + ")";
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

    @Override
    public final int hashCode() {
        return Objects.hash(world, p1, p2);
    }

    @Override
    public final boolean equals(Object arg0) {
        if (!(arg0 instanceof BlockRange other)) return false;
        return other.world.equals(world) && other.p1.equals(p1) && other.p2.equals(p2);
    }
}
