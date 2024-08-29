package me.stephenminer.redvblue.events.items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.stephenminer.redvblue.RedBlue;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class WindScrollUse implements Listener {
    private final RedBlue plugin;
    private List<UUID> cooldowns;

    public WindScrollUse(){
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
        cooldowns = new ArrayList<>();
    }

    @EventHandler
    public void useScroll(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        if (plugin.checkLore(item,"windscroll")) return;

        if (onCooldown(player)) return;
        var eyeLoc = player.getEyeLocation();
        var res = player.getWorld().rayTraceEntities(eyeLoc, eyeLoc.getDirection(), 20, 0.3);
        if (res != null)
            res.getHitEntity().setVelocity(eyeLoc.getDirection().multiply(-5).add(new Vector(0, 2, 0)));
        runCooldown(player, 20);
    }

    private boolean onCooldown(Player player){
        if (cooldowns.contains(player.getUniqueId())){
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(ChatColor.RED + "Wind Scroll on cooldown!"));
            return true;
        } else return false;
    }

    private void runCooldown(Player player, int duration){
        cooldowns.add(player.getUniqueId());
        new BukkitRunnable(){
            int count = 0;
            @Override
            public void run(){
                if (count >= duration){
                    this.cancel();
                    cooldowns.remove(player.getUniqueId());
                    player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1,2);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(ChatColor.AQUA + "Wind Scroll off Cooldown!"));
                    return;
                }
            }
        }.runTaskTimer(plugin,1,1);
    }
}
