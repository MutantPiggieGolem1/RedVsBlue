package me.stephenminer.redvblue.events.items;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import me.stephenminer.redvblue.RedBlue;

public class WindScrollUse implements Listener {
    private final RedBlue plugin;

    public WindScrollUse(){
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
    }

    @EventHandler
    public void useScroll(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getAction() != Action.RIGHT_CLICK_AIR) return;
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        if (!plugin.checkLore(item,"windscroll")) return;

        if (player.hasCooldown(item.getType())) return;
        var eyeLoc = player.getEyeLocation();
        var res = player.getWorld().rayTraceEntities(eyeLoc.clone().add(eyeLoc.getDirection()), eyeLoc.getDirection(), 20, 0.3);
        if (res != null)
            res.getHitEntity().setVelocity(eyeLoc.getDirection().multiply(5).add(new Vector(0, 2, 0)));
        player.setCooldown(item.getType(), 20 * 20);
    }
}
