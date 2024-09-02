package me.stephenminer.redvblue.arena;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;

import me.stephenminer.redvblue.RedBlue;

public class ArenaStateSaver { // TODO revise this to do its own internal state checking
    private final RedBlue plugin;
    private final Arena arena;
    private final List<BlockState> states;

    private boolean saving, loading;

    public ArenaStateSaver(RedBlue plugin, Arena arena){
        this.plugin = plugin;
        this.arena = arena;
        states = new ArrayList<>();
        loading = false;
    }

    public void saveMap() {
        saving = true;
        new BukkitRunnable(){
            @Override
            public void run() {
                arena.getBounds().forEach((Location l) -> states.add(l.getBlock().getState()));
                saving = false;
            }
        }.runTaskAsynchronously(plugin);
    }

    public void loadMap() {
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
                countTo = Math.min(countTo + plugin.loadRate(), states.size()-1);
            }
        }.runTaskTimer(plugin, 1,1);
    }

    private int getCountTo(int index, int rate){
        return Math.min(index + rate, states.size()-1);
    }

    public boolean isLoading(){ return loading; }
    public boolean isSaving(){ return saving; }
}
