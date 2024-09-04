package me.stephenminer.redvsblue.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import me.stephenminer.redvsblue.util.BlockRange;

public class Wall {
    private BlockRange range;
    private Material type;
    private boolean fallen;

    public Wall(Material mat, BlockRange range){
        this.type = mat;
        this.range = range;
        buildWall();
    }

    public boolean contains(Location loc) {
        Vector corner1 = loc.getBlock().getLocation().toVector();
        Vector corner2 = corner1.clone().add(new Vector(1,1,1));
        return range.toBoundingBox().overlaps(corner1, corner2);
    }

    public void buildWall() {
        range.fill(type);
        fallen = false;
    }
    public void destroyWall() {
        fallen = true;
        range.fill(Material.AIR);
    }

    public boolean isFallen() { return fallen; }

    @Override
    public String toString() {
        return type.name() + "#" + range.toString();
    }
}
