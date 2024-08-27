package me.stephenminer.redvblue.arena;

import me.stephenminer.redvblue.BlockRange;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Wall {
    private BlockRange range;
    private Material type;
    private boolean fallen;

    public Wall(Material mat, BlockRange range){
        this.type = mat;
        this.range = range;
        buildWall();
    }

    /**
     * @param player
     * @param target
     * @return true if the player can edit the area, false if not.
     */
    public boolean canEdit(Player player, Block target) {
        Vector corner1 = target.getLocation().toVector();
        Vector corner2 = corner1.clone().add(new Vector(1,1,1));
        return !range.toBoundingBox().overlaps(corner1, corner2) || player.hasPermission("rvb.edit");
    }

    public boolean isOnWall(Location loc) {
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

    public void setFallen(boolean fallen) { this.fallen = fallen; }
    public boolean isFallen() { return fallen; }

    public static Wall fromString(Server currentServer, String serialized) {
        String[] segments = serialized.split(", ", 2);
        return new Wall(Material.getMaterial(segments[0]), BlockRange.fromString(currentServer, segments[1]));
    }

    /**
     * @return String formatted as material/ world: (p1)-(p2)
     */
    @Override
    public String toString() {
        return type.name() + "/ " + range.toString();
    }
}
