package me.stephenminer.redvblue.events.items;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Arena;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ThrowingJuiceUse implements Listener {
    public static NamespacedKey USES = new NamespacedKey(JavaPlugin.getPlugin(RedBlue.class),"rbuses");
    private final RedBlue plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ThrowingJuiceUse() {
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
    }

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
        if (!plugin.checkLore(item,"throwingjuice")) return;
        
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
        if (!oa.isPresent() || onHit(player, oa.orElseThrow(), res.getHitPosition().toLocation(world)))
            updateUses(item);
        
        cooldowns.put(player.getUniqueId(), now + 1000 * 5);
    }

    private void updateUses(ItemStack item){
        if (!item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(USES, PersistentDataType.INTEGER)) return;
        int current = item.getItemMeta().getPersistentDataContainer().get(USES,PersistentDataType.INTEGER);
        current-=1;
        if (current < 1){
            item.setAmount(0);
            return;
        }
        item.getItemMeta().getPersistentDataContainer().set(USES,PersistentDataType.INTEGER,current);
        List<String> lore = item.getItemMeta().getLore();
        for (int i = 0; i < lore.size(); i++){
            String entry = lore.get(i);
            String temp = ChatColor.stripColor(entry);
            if (temp.contains("Uses: ")) {
                lore.set(i,ChatColor.YELLOW + "Uses: "+ current);
                return;
            }
        }
    }

    private boolean onHit(Player shooter, Arena arena, Location hitLoc){
        double radius = 1.5;
        boolean anyHit = false;
        Collection<Entity> near = hitLoc.getWorld().getNearbyEntities(hitLoc,radius,radius,radius, e->e instanceof Player);
        for (Entity e : near){
            Player player = (Player) e;
            if (teamsMatch(arena,shooter,player)){
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
