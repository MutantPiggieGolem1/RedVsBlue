package me.stephenminer.redvblue.events.items;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.stephenminer.redvblue.CustomItems;
import me.stephenminer.redvblue.arena.Arena;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ThrowingJuiceUse implements Listener {
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @EventHandler
    public void throwJuice(PlayerInteractEvent event){
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
        var res = world.rayTraceEntities(eyeLoc.clone().add(eyeLoc.getDirection()), eyeLoc.getDirection(), 50, 0.5);
        if (res == null) {
            world.spawnParticle(Particle.ASH, eyeLoc, 15);
            return;
        } else {
            while (eyeLoc.toVector().distanceSquared(res.getHitPosition()) > 1.5) {
                eyeLoc = eyeLoc.add(eyeLoc.getDirection().normalize());
                world.spawnParticle(Particle.HEART, eyeLoc, 2);
            }
        }
        var oa = Arena.arenaOf(player);
        if (onHit(player, oa.orElse(null), res.getHitPosition().toLocation(world)))
            item.setAmount(item.getAmount() - 1);
        
        cooldowns.put(player.getUniqueId(), now + 1000 * 5);
    }

    private boolean onHit(Player shooter, @Nullable Arena arena, Location hitLoc){
        double radius = 1.5;
        boolean anyHit = false;
        Collection<Entity> near = hitLoc.getWorld().getNearbyEntities(hitLoc,radius,radius,radius, e->e instanceof Player);
        for (Entity e : near){
            Player player = (Player) e;
            if (arena == null || teamsMatch(arena,shooter,player)){
                anyHit = true;
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,1,0));
            }
        }
        return anyHit;
    }

    private boolean teamsMatch(Arena arena, Player shooter, Player target){
        if (arena != null && arena.hasPlayer(shooter) && arena.hasPlayer(target)){
            var sb = shooter.getScoreboard();
            assert sb == target.getScoreboard();
            return sb.getEntryTeam(shooter.getName()) == sb.getEntryTeam(target.getName());
        }
        return false;
    }

}
