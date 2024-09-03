package me.stephenminer.redvblue;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import me.stephenminer.redvblue.arena.Arena;
import me.stephenminer.redvblue.util.ArenaConfigUtil;

public class ArenaSelector implements InventoryHolder {
    private final RedBlue plugin;
    private final Inventory inv;

    public ArenaSelector(RedBlue plugin) {
        this.plugin = plugin;
        this.inv = Bukkit.createInventory(null, 18, ChatColor.AQUA + "Arena Selector");
    }

    public void display(Player player) {
        player.openInventory(this.getInventory());
        scheduleRefreshes();
    }

    private void scheduleRefreshes() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (inv.getViewers().isEmpty()) {
                    this.cancel();
                    return;
                } else {
                    inv.clear();
                    for (String id : ArenaConfigUtil.readyIDsOnFileDeep()) {
                        inv.addItem(arenaIcon(id));
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 100);
    }

    private ItemStack arenaIcon(String id) {
        Arena arena = Arena.arenaOf(id).orElse(null);
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + id);
        meta.setLore(List.of(
                ChatColor.BLUE + (arena == null ? "0" : "" + arena.getPlayerCount()) + " players",
                arena == null || arena.isJoinable() ?
                    ChatColor.GREEN + "Click to join!" :
                    ChatColor.YELLOW + "Click to spectate!"
        ));
        meta.setCustomModelData(hashCode());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public static class EventListener implements Listener {
        @EventHandler
        public void onClick(InventoryClickEvent event) {
            ItemStack item = event.getCurrentItem();
            if (event.getInventory() == null || item == null) return;
            if (!(event.getView().getTitle().equals(ChatColor.AQUA + "Arena Selector")))
                return; // not ideal but idc
            event.setCancelled(true);
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return; // all buttons are on top silly
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
                return;
            String arenaId = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            Bukkit.dispatchCommand(event.getWhoClicked(), "rvb join " + arenaId);
        }
    }
}
