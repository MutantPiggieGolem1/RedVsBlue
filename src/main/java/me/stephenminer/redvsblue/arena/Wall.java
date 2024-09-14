package me.stephenminer.redvsblue.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import me.stephenminer.redvsblue.util.BlockRange;
import me.stephenminer.redvsblue.util.WorldEditInterface;

public class Wall {
    private BlockRange range;
    private Material type;
    private boolean fallen;

    public Wall(Material mat, BlockRange range){
        this.type = mat;
        this.range = range;
    }

    public boolean contains(Location loc) {
        Vector corner1 = loc.getBlock().getLocation().toVector();
        Vector corner2 = corner1.clone().add(new Vector(1,1,1));
        return range.toBoundingBox().overlaps(corner1, corner2);
    }

    public void buildWall() {
        WorldEditInterface.fill(range, type).thenAccept((x) -> fallen = false).join();
    }
    public void destroyWall() {
        WorldEditInterface.fill(range, Material.AIR).thenAccept((x) -> fallen = true).join();
    }

    public boolean isFallen() { return fallen; }

    @Override
    public String toString() {
        return type.name() + "#" + range.toString();
    }
}
