package me.stephenminer.redvblue.arena;

import me.stephenminer.redvblue.RedBlue;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Wall {
    private final RedBlue plugin;
    private Location loc1;
    private Location loc2;
    private Material type;
    private boolean fallen;

    public Wall(Material mat, Location loc1, Location loc2){
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
        this.type = mat;
        this.loc1 = loc1;
        this.loc2 = loc2;
        fallen = false;
    }

    /**
     *
     * @param str formatted as material/loc1/loc2
     */
    public Wall(String str){
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
        fallen = false;
        loadFromString(str);
    }


    /**
     *
     * @param player
     * @param edit
     * @return true if the player can edit the area, false if not.
     */
    public boolean tryEdit(Player player, Block edit){

        BoundingBox bounds = BoundingBox.of(loc1.clone().add(0.5,0.5,0.5), loc2.clone().add(0.5,0.5,0.5));
        Vector corner1 = edit.getLocation().toVector();
        Vector corner2 = corner1.clone().add(new Vector(1,1,1));
        if (bounds.overlaps(corner1, corner2)){
            return player.hasPermission("rvb.edit");
        }
        return true;
    }

    public boolean isOnWall(Location loc){
        BoundingBox bounds = BoundingBox.of(loc1.clone().add(0.5,0.5,0.5),loc2.clone().add(0.5,0.5,0.5));
        Vector corner1 = loc.getBlock().getLocation().toVector();
        Vector corner2 = corner1.clone().add(new Vector(1,1,1));
        return bounds.overlaps(corner1, corner2);
    }

    public void buildWall(){
        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());

        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        World world = loc1.getWorld();
        for (int x = minX; x <= maxX; x++){
            for (int y = minY; y <= maxY; y++){
                for (int z = minZ; z <= maxZ; z++){
                    Location temp = new Location(world, x, y, z);
                    temp.getBlock().setType(type);
                }
            }
        }
        fallen = false;
    }
    public void destroyWall(){
        fallen = true;
        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());

        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        World world = loc1.getWorld();
        for (int x = minX; x <= maxX; x++){
            for (int y = minY; y <= maxY; y++){
                for (int z = minZ; z <= maxZ; z++){
                    Location temp = new Location(world, x, y, z);
                    temp.getBlock().setType(Material.AIR);
                }
            }
        }
    }

    private void loadFromString(String str){
        String[] split = str.split("/");
        Material mat = Material.matchMaterial(split[0]);
        Location loc1 = plugin.fromString(split[1]);
        Location loc2 = plugin.fromString(split[2]);
        this.type = mat;
        this.loc1 = loc1;
        this.loc2 = loc2;
    }


    /**
     *
     * @return String formatted as material/loc1/loc2
     */
    @Override
    public String toString(){
        return type.name() + "/" +sLocPair();
    }

    public String sLocPair(){
        return plugin.fromBLoc(loc1) + "/" + plugin.fromBLoc(loc2);
    }

    public void setFallen(boolean fallen){ this.fallen = fallen; }
    public boolean isFallen(){ return fallen; }
}
