package me.stephenminer.redvsblue.events;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.stephenminer.redvsblue.arena.Arena;

public class PlayerHandling implements Listener {

    @EventHandler
    public void stopLethal(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getDamageSource().getCausingEntity() != null)
            return;
        var oa = Arena.arenaOf(player);
        if (!oa.isPresent()) return;
        Arena arena = oa.get();
        if (arena.isEnded()) return;
        if (player.getHealth() > event.getFinalDamage()) return;
        event.setCancelled(true);
        arena.killPlayer(player, null);
    }

    @EventHandler
    public void stopPLethal(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        var oa = Arena.arenaOf(player);
        if (!oa.isPresent()) return;
        Arena arena = oa.get();
        if (arena.isEnded()) return;
        if (player.getHealth() > event.getFinalDamage()) return;
        event.setCancelled(true);
        arena.killPlayer(player, event.getDamager());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        var oa = Arena.arenaOf(player);
        if (!oa.isPresent()) return;
        Arena arena = oa.get();
        if (!arena.getWorld().equals(event.getPlayer().getWorld()))
            arena.removePlayer(player, true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        var oa = Arena.arenaOf(player);
        if (!oa.isPresent()) return;
        Arena arena = oa.get();
        arena.removePlayer(player, false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        var oa = Arena.disconnectedFrom(player);
        if (!oa.isPresent()) return;
        Arena arena = oa.get();
        arena.addPlayer(player);
    }

    // SUBOPTIMAL replace protections with worldguard
    @EventHandler
    public void noBeds(PlayerInteractEvent event) {
        if (!event.hasBlock())
            return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (!(block.getBlockData() instanceof Bed))
            return;
        if (!Arena.arenaOf(player).isPresent())
            return;
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "No Time for Sleep!");
    }

    @EventHandler
    public void handlePlacement(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        var oa = Arena.arenaOf(player);
        if (!oa.isPresent()) return;
        Arena arena = oa.get();
        if (!arena.canBreak(player, event.getBlock())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can't place blocks here!");
            return;
        }
    }

    @EventHandler
    public void handleBreaking(BlockBreakEvent event) {
        Player player = event.getPlayer();
        var oa = Arena.arenaOf(player);
        if (!oa.isPresent()) return;
        Arena arena = oa.get();
        Block block = event.getBlock();
        if (!arena.canBreak(player, block)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "you can't break blocks here");
            return;
        }
    }
}
