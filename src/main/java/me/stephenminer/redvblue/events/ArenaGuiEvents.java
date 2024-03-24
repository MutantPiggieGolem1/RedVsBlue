package me.stephenminer.redvblue.events;

import me.stephenminer.redvblue.ArenaGui;
import me.stephenminer.redvblue.RedBlue;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class ArenaGuiEvents implements Listener {
    public static HashMap<UUID, ArenaGui> IN_GUI = new HashMap<>();
    private final RedBlue plugin;
    public ArenaGuiEvents(){
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
    }



    @EventHandler
    public void onClick(InventoryClickEvent event){
        Player player = (Player)  event.getWhoClicked();
        if (IN_GUI.containsKey(player.getUniqueId())){
            ArenaGui gui = IN_GUI.get(player.getUniqueId());
            gui.handleInteract(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event){
        IN_GUI.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        IN_GUI.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void worldChange(PlayerChangedWorldEvent event){
        IN_GUI.remove(event.getPlayer().getUniqueId());
    }
}
