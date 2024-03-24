package me.stephenminer.redvblue.arena;

import me.stephenminer.redvblue.RedBlue;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArenaSaver {
    private final RedBlue plugin;
    private final Arena arena;
    private List<BlockState> states;

    private final Location loc1,loc2;
    private boolean saving;
    private boolean loading;

    public ArenaSaver(Arena arena){
        this.plugin = RedBlue.getPlugin(RedBlue.class);
        this.arena = arena;
        this.loc1 = arena.getLoc1();
        this.loc2 = arena.getLoc2();
        states = new ArrayList<>();
        loading = false;
    }




    public void saveMap(){
        saving = true;
        int maxX = maxX();
        int maxY = maxY();
        int maxZ = maxZ();
        int minX = minX();
        int minY = minY();
        int minZ = minZ();
        World world = loc1.getWorld();
        for (int x = minX; x<= maxX; x++){
            for (int y = minY; y<= maxY; y++){
                for (int z = minZ; z <= maxZ; z++){
                    Block block = world.getBlockAt(x,y,z);
                    states.add(block.getState());
                }
            }
        }
        saving = false;
    }

    public void loadMap(){
        loading = true;
        new BukkitRunnable(){
            private int index = 0;
            private int countTo = getCountTo(index, plugin.loadRate());
            @Override
            public void run(){
                for (int i = index; i <= countTo; i++){
                    BlockState state = states.get(i);
                    state.update(true);
                }
                if (index >= states.size()-1){
                    plugin.getLogger().info(ChatColor.GOLD + "" + ChatColor.BOLD + "Arena Finished Loading");
                    loading = false;
                    this.cancel();
                    return;
                }
                index = countTo;
                countTo = Math.min(countTo+ plugin.loadRate(), states.size()-1);
            }
        }.runTaskTimer(plugin, 1,1);
    }

    private int getCountTo(int index, int rate){
        return Math.min(index + rate, states.size()-1);
    }

    public boolean isLoading(){ return loading; }
    public boolean isSaving(){ return saving; }


    public int maxX(){ return Math.max(loc1.getBlockX(), loc2.getBlockX()); }
    public int maxY(){ return Math.max(loc1.getBlockY(),loc2.getBlockY()); }
    public int maxZ(){ return Math.max(loc1.getBlockZ(), loc2.getBlockZ()); }

    public int minX(){ return Math.min(loc1.getBlockX(), loc2.getBlockX()); }
    public int minY(){ return Math.min(loc1.getBlockY(),loc2.getBlockY()); }
    public int minZ(){ return Math.min(loc1.getBlockZ(), loc2.getBlockZ()); }
}
