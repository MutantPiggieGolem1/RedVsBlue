package me.stephenminer.redvblue.events;

import me.stephenminer.redvblue.RedBlue;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LongRifleUse implements Listener {
    private final RedBlue plugin;
    private final Set<UUID> cooldown;

    public LongRifleUse(RedBlue plugin){
        this.plugin = plugin;
        cooldown = new HashSet<>();
    }


    @EventHandler
    public void shootRifle(PlayerInteractEvent event){
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        if (cooldown.contains(player.getUniqueId())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(net.md_5.bungee.api.ChatColor.AQUA + "Ability on Cooldown!"));
            return;
        }
        if (plugin.checkLore(item,"longrifle")){
            boolean shoot = checkAndTakeAmmo(player);
            if (shoot) {
                fire(player);

            }else {
                player.sendMessage(ChatColor.RED + "You are missing mana-powder or arrows!");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        cooldown.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event){
        cooldown.remove(event.getPlayer().getUniqueId());
    }





    private boolean checkAndTakeAmmo(Player player){
        ItemStack powder = null;
        ItemStack arrow = null;
        ItemStack[] items = player.getInventory().getContents();
        for (ItemStack item : items){
            if (plugin.checkLore(item,"manapowder")) {
                powder = item;
                continue;
            }
            Material mat = item.getType();
            if (mat == Material.ARROW || mat == Material.TIPPED_ARROW || mat == Material.SPECTRAL_ARROW){
                arrow = item;
                continue;
            }
        }
        if (powder == null || arrow == null) return false;
        powder.setAmount(powder.getAmount()-1);
        arrow.setAmount(arrow.getAmount()-1);
        return true;
    }

    private void fire(Player shooter){
        Vector dir = shooter.getLocation().getDirection();
        Arrow arrow = shooter.launchProjectile(Arrow.class,dir.clone().multiply(4));
        arrow.setKnockbackStrength(3);
        World world = shooter.getWorld();
        world.spawnParticle(Particle.ASH,shooter.getLocation().clone().add(dir),10);
        world.playSound(shooter.getLocation(), Sound.ENTITY_GENERIC_EXPLODE,2,1);
    }

    /**
     *
     * @param player
     * @param duration - time in ticks
     */
    private void runCooldown(Player player, int duration){
        cooldown.add(player.getUniqueId());
        float exp = player.getExp();
        new BukkitRunnable(){
            int count;
            @Override
            public void run(){
                if (!player.isOnline() || player.isDead()){
                    this.cancel();
                    return;
                }
                if (count >= duration){
                    player.playSound(player,Sound.BLOCK_IRON_TRAPDOOR_CLOSE,1,1);
                    player.setExp(exp);
                    cooldown.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                float ratio = ((float) count) / duration;
                player.setExp(ratio);
                count++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
