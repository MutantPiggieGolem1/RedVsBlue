package me.stephenminer.redvblue.events.items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import me.stephenminer.redvblue.CustomItems;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class WindScrollUse implements Listener {
    private final Map<UUID, Long> cooldowns = new HashMap<>();

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
        if (!CustomItems.WINDSCROLL.is(item)) return;

        var eyeLoc = player.getEyeLocation();
        var res = player.getWorld().rayTraceEntities(eyeLoc, eyeLoc.getDirection(), 20, 0.3, (e) -> !(e instanceof Player p && p.getUniqueId().equals(player.getUniqueId())));
        if (res == null) {
            player.getWorld().spawnParticle(Particle.DUST, eyeLoc, 15, new Particle.DustOptions(Color.GRAY, 2));
            return;
        } else
            res.getHitEntity().setVelocity(eyeLoc.getDirection().multiply(2).add(new Vector(0, 1, 0)));
        
        if (player.isSneaking() && consumeMana(player)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.AQUA + "" + ChatColor.BOLD + "Skipped Cooldown!"));
            return;
        }
        cooldowns.put(player.getUniqueId(), now + 1000 * 20);
    }
    
    private boolean consumeMana(Player player) {
        for (ItemStack item : player.getInventory().getContents()){
            if (item == null) continue;
            if (CustomItems.MANAPOWDER.is(item)) {
                item.setAmount(item.getAmount()-1);
                return true;
            }
        }
        return false;
    }
}
