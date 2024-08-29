package me.stephenminer.redvblue.events.items;

import java.util.Collection;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Arena;

public class ThrowingJuiceUse implements Listener {
    public static NamespacedKey USES = new NamespacedKey(JavaPlugin.getPlugin(RedBlue.class),"rbuses");

    private final RedBlue plugin;
    public ThrowingJuiceUse(RedBlue plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void throwJuice(PlayerInteractEvent event){
        if (!event.hasItem()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getAction() != Action.RIGHT_CLICK_AIR) return;
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        if (player.hasCooldown(Material.NETHER_STAR)) return;
        if (!plugin.checkLore(item,"throwingjuice")) return;
        shootBeam(player,item);
        player.setCooldown(Material.NETHER_STAR,10);
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

    private void shootBeam(Player shooter, ItemStack item){
        World world = shooter.getWorld();
        var eyeLoc = shooter.getEyeLocation();
        var res = world.rayTraceEntities(eyeLoc, eyeLoc.getDirection(), 100, 0.5);
        if (res == null) {
            world.spawnParticle(Particle.ASH, eyeLoc, 15);
            return;
        } else {
            while (eyeLoc.toVector().distanceSquared(res.getHitPosition()) > 2) {
                eyeLoc = eyeLoc.add(eyeLoc.getDirection().normalize());
                world.spawnParticle(Particle.HEART, eyeLoc, 5);
            }
        }
        if (onHit(shooter, Arena.arenaOf(shooter).orElseThrow(), res.getHitPosition().toLocation(world)))
            updateUses(item);
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
