package me.stephenminer.redvblue.chests;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.commands.LootChest;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GuiEvents implements Listener {

    private final RedBlue plugin;
    public GuiEvents(RedBlue plugin){
        this.plugin = plugin;
    }


    @EventHandler
    public void onClose(InventoryCloseEvent event){
        Player player = (Player) event.getPlayer();
        if (LootChest.editing.containsKey(player.getUniqueId())){
            TableGui gui = LootChest.editing.get(player.getUniqueId());
            String title = event.getView().getTitle();
            if (ChatColor.stripColor(title).equals(gui.currentTitle())){
                gui.save();
            }
        }
        LootChest.editing.remove(player.getUniqueId());
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        if (LootChest.editing.containsKey(player.getUniqueId())) LootChest.editing.remove(player.getUniqueId());
    }

    @EventHandler
    public void guiClick(InventoryClickEvent event){
        if (event.getView().getTopInventory().equals(event.getClickedInventory())){
            Player player = (Player) event.getWhoClicked();
            if (LootChest.editing.containsKey(player.getUniqueId())){
                TableGui gui = LootChest.editing.get(player.getUniqueId());
                String title = ChatColor.stripColor(event.getView().getTitle());
                if (title.equals(gui.currentTitle())){
                    int slot = event.getSlot();
                    if (slot == 31){
                        event.setCancelled(true);
                        LootChest.editing.remove(player.getUniqueId());
                        gui.save();
                        player.closeInventory();
                    }else if (slot >= 27){
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
