package me.stephenminer.redvblue.events.items;

import me.stephenminer.redvblue.RedBlue;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WindScrollUse {
    private final RedBlue plugin;
    private List<UUID> cooldowns;

    public WindScrollUse(){
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
        cooldowns = new ArrayList<>();
    }




    @EventHandler
    public void useScroll(PlayerInteractEvent event){

    }

    private boolean onCooldown(Player player){
        if (cooldowns.contains(player.getUniqueId())){
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(ChatColor.RED + "Wind Scroll on cooldown!"));
            return true;
        }else return false;
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
