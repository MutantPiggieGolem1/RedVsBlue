package me.stephenminer.redvblue.events.items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import me.stephenminer.redvblue.RedBlue;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class WindScrollUse implements Listener {
    private final RedBlue plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public WindScrollUse() {
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
    }

    @EventHandler
    public void useScroll(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(player.getUniqueId()) && cooldowns.get(player.getUniqueId()) > now) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(ChatColor.AQUA + "Ability on Cooldown!"));
            return;
        }
        if (!plugin.checkLore(item,"windscroll")) return;

        var eyeLoc = player.getEyeLocation();
        var res = player.getWorld().rayTraceEntities(eyeLoc.clone().add(eyeLoc.getDirection()), eyeLoc.getDirection(), 20, 0.3);
        if (res == null) {
            player.getWorld().spawnParticle(Particle.ASH, eyeLoc, 15);
            return;
        } else
            res.getHitEntity().setVelocity(eyeLoc.getDirection().multiply(5).add(new Vector(0, 2, 0)));
        
        if (player.isSneaking() && consumeMana(player)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.AQUA + "" + ChatColor.BOLD + "Skipped Cooldown!"));
            return;
        }
        cooldowns.put(player.getUniqueId(), now + 1000 * 20);
    }
    
    private boolean consumeMana(Player player) {
        for (ItemStack item : player.getInventory().getContents()){
            if (item == null) continue;
            if (plugin.checkLore(item,"manapowder")) {
                item.setAmount(item.getAmount()-1);
                return true;
            }
        }
        return false;
    }
}
