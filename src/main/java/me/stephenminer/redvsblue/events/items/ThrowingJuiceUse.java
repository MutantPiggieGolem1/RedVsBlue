package me.stephenminer.redvsblue.events.items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.stephenminer.redvsblue.CustomItems;
import me.stephenminer.redvsblue.arena.Arena;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ThrowingJuiceUse implements Listener {
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final double SPLASHRADIUS = 1.5;

    @EventHandler
    public void throwJuice(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(player.getUniqueId()) && cooldowns.get(player.getUniqueId()) > now) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(net.md_5.bungee.api.ChatColor.AQUA + "Ability on Cooldown!"));
            return;
        }
        if (!CustomItems.THROWINGJUICE.is(item)) return;
        
        var world = player.getWorld();
        var eyeLoc = player.getEyeLocation();
        var res = world.rayTraceEntities(eyeLoc, eyeLoc.getDirection(), 50, 0.5, (e) -> !(e instanceof Player p && p.getUniqueId().equals(player.getUniqueId())) && !(e instanceof Item));
        if (res == null) {
            world.spawnParticle(Particle.DUST, eyeLoc, 15, new Particle.DustOptions(Color.GRAY, 2));
            return;
        } else {
            while (eyeLoc.toVector().distanceSquared(res.getHitPosition()) > 1) {
                eyeLoc = eyeLoc.add(eyeLoc.getDirection().normalize());
                world.spawnParticle(Particle.HEART, eyeLoc, 2);
            }
        }
        var oa = Arena.arenaOf(player);
        for (Entity e : world.getNearbyEntities(
            res.getHitPosition().toLocation(world),
            SPLASHRADIUS, SPLASHRADIUS, SPLASHRADIUS,
            e->e instanceof Player
        )) {
            Player p = (Player) e;
            var oa2 = Arena.arenaOf(p);
            if (oa.isPresent() != oa2.isPresent()) return; // In-arena can't affect out-arena, and vice versa
            if (oa.isPresent() && oa.get().equals(oa2.get()) && teamsMatchNaive(player, p)) return;
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,50,3));
        }
        
        item.setAmount(item.getAmount() - 1);
        cooldowns.put(player.getUniqueId(), now + 1000 * 5);
    }

    private boolean teamsMatchNaive(Player shooter, Player target) { // Does not check if they're in the same arena.
        var sb = shooter.getScoreboard();
        assert sb == target.getScoreboard();
        return sb.getEntryTeam(shooter.getName()) == sb.getEntryTeam(target.getName());
    }
}
