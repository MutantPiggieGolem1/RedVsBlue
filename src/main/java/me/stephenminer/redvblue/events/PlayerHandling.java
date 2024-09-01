package me.stephenminer.redvblue.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Arena;

public class PlayerHandling implements Listener { // TODO replace with worldguard
    private final RedBlue plugin;

    public PlayerHandling(RedBlue plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void stopLethal(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)
                return;
            Arena arena = Arena.arenaOf(player).orElseThrow();
            double health = player.getHealth() - event.getFinalDamage();
            if (health <= 0) {
                event.setCancelled(true);
                if (!arena.isStarted()) {
                    player.teleport(arena.getLobby());
                    return;
                } else if (!arena.hasWallFallen()) {
                    player.teleport(arena.getSpawnFor(player));
                    return;
                }
                player.setGameMode(GameMode.SPECTATOR);
                arena.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "NOTICE: " + ChatColor.WHITE + player.getName()
                        + " has been eliminated!");
                player.sendMessage(ChatColor.RED + "You have been eliminated");
                player.sendMessage(ChatColor.GOLD + "You may spectate or do /leaveRvB if you wish to leave!");
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, arena::checkEnding, 5);
                return;
            }
        }
    }

    @EventHandler
    public void noBeds(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.hasBlock())
            return;
        Block block = event.getClickedBlock();
        if (!(block.getBlockData() instanceof Bed))
            return;
        if (!Arena.arenaOf(player).isPresent())
            return;
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "No Time for Sleep!");
    }

    @EventHandler
    public void stopPLethal(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Arena arena = Arena.arenaOf(player).orElseThrow();
        double health = player.getHealth() - event.getFinalDamage();
        if (health >= 0) return;
        event.setCancelled(true);
        if (!arena.isStarted()) {
            player.teleport(arena.getLobby());
        } else if (!arena.hasWallFallen()) {
            player.teleport(arena.getSpawnFor(player));
        } else {
            player.sendMessage(ChatColor.RED + "You have been eliminated!");
            String msg = ChatColor.GOLD + "" + ChatColor.BOLD + "NOTICE: " + ChatColor.WHITE + player.getName()
                    + " has been eliminated by ";
            if (event.getDamager() instanceof LivingEntity living) {
                msg += living.getName();
            } else if (event.getDamager() instanceof Projectile proj
                    && proj.getShooter() instanceof LivingEntity living)
                msg += living.getName() + "!";
            arena.broadcast(msg);
            player.setGameMode(GameMode.SPECTATOR);
            arena.checkEnding();

        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        var oa = Arena.arenaOf(player);
        if (!oa.isPresent())
            return;
        Arena a = oa.orElseThrow();
        if (!a.getLobby().getWorld().equals(event.getPlayer().getWorld()))
            a.removePlayer(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Arena a = Arena.arenaOf(player).orElseThrow();
        a.disconnectPlayer(player);
    }

    @EventHandler
    public void handlePlacement(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Arena a = Arena.arenaOf(player).orElseThrow();
        Block block = event.getBlock();
        boolean pass = a.isStarted() && a.tryEdit(player, block);
        if (!pass) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can't place blocks here!");
            return;
        }
    }

    @EventHandler
    public void handleBreaking(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Arena a = Arena.arenaOf(player).orElseThrow();
        Block block = event.getBlock();
        boolean pass = a.isStarted() && a.tryEdit(player, block);
        if (!pass) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "you can't break blocks here");
            return;
        }
    }
}
