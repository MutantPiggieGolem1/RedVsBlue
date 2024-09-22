package me.stephenminer.redvsblue.events.items;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.loot.Lootable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import me.stephenminer.redvsblue.CustomItems;
import me.stephenminer.redvsblue.RedVsBlue;
import me.stephenminer.redvsblue.util.ArenaConfigUtil;

public class LootWandUse implements Listener {
    private final Map<UUID, Block> selectedTargets;

    private final RedVsBlue plugin;

    public LootWandUse(RedVsBlue plugin) {
        this.plugin = plugin;
        selectedTargets = new HashMap<>();
    }

    @EventHandler
    public void selectTarget(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (!event.hasItem() ||
                action == Action.PHYSICAL || action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_AIR)
            return;
        if (!CustomItems.LOOTWAND.is(event.getItem()))
            return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        var target = event.getClickedBlock();

        if (!(target.getState() instanceof Lootable)) {
            player.sendMessage(ChatColor.RED + "That block cannot accept loot tables!");
            return;
        }
        
        var arena = ArenaConfigUtil.findOnFileDeep(target);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "That block isn't in an arena!");
            return;
        }

        selectedTargets.put(player.getUniqueId(), target);
        player.sendMessage(ChatColor.GREEN
                + "Please type a loot table key to assign to this block, 'delete' to unlink it, or 'cancel'!");
    }

    @EventHandler
    public void chatFollowup(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        var target = selectedTargets.get(player.getUniqueId());
        if (target == null) return;
        selectedTargets.remove(player.getUniqueId());
        event.setCancelled(true);

        var arena = ArenaConfigUtil.findOnFileDeep(target);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "That block isn't in an arena!");
            return;
        }

        String arg = ChatColor.stripColor(event.getMessage()).toLowerCase();
        switch (arg) {
            case "cancel":
                break;
            case "delete":
                if (arena.deleteLootCache(new BlockVector(target.getLocation().toVector()))) {
                    player.sendMessage(ChatColor.GREEN + "Success! Unlinked a " + target.getType().toString() + ".");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "There was no loot cache there!");
                    return;
                }
                break;
            default:
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!(target.getState() instanceof Lootable)) {
                            player.sendMessage(ChatColor.RED + "That block cannot accept loot tables!");
                            return;
                        }
                        var key = NamespacedKey.fromString(arg, plugin);
                        if (key == null) {
                            player.sendMessage(ChatColor.RED + "Invalid loot table key '" + key + "'.");
                            return;
                        }
                        var table = plugin.getServer().getLootTable(key);
                        if (table == null) {
                            player.sendMessage(ChatColor.RED + "Missing loot table for key '" + key + "'.");
                            return;
                        }
                        if (arena.createLootCache(new BlockVector(target.getLocation().toVector()), key.toString())) {
                            player.sendMessage(ChatColor.GREEN + "Success! Linked a " + target.getType().toString() + " to "+key.toString()+".");
                        } else {
                            player.sendMessage(ChatColor.YELLOW + "There was already a loot cache there!");
                            return;
                        }
                    }
                }.runTask(plugin);
                break;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selectedTargets.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        selectedTargets.remove(event.getPlayer().getUniqueId());
    }
}
