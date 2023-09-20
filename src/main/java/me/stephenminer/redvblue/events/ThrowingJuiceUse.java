package me.stephenminer.redvblue.events;

import me.stephenminer.redvblue.Items;
import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Arena;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ThrowingJuiceUse implements Listener {
    private final RedBlue plugin;
    public ThrowingJuiceUse(RedBlue plugin){
        this.plugin = plugin;
    }



    @EventHandler
    public void throwJuice(PlayerInteractEvent event){
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
      //  if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (player.hasCooldown(Material.NETHER_STAR)) return;
        if (plugin.checkLore(item,"throwingjuice")){
            shootBeam(player,item);
            player.setCooldown(Material.NETHER_STAR,10);
        }
    }

    private void updateUses(ItemStack item){
        if (!item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(Items.USES, PersistentDataType.INTEGER)) return;
        int current = item.getItemMeta().getPersistentDataContainer().get(Items.USES,PersistentDataType.INTEGER);
        current-=1;
        if (current < 1){
            item.setAmount(0);
            return;
        }
        item.getItemMeta().getPersistentDataContainer().set(Items.USES,PersistentDataType.INTEGER,current);
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
        int beamLength = 100;
        Arena arenaIn = Arena.arenas.stream().filter(arena->arena.hasPlayer(shooter)).findAny().orElse(null);
        boolean hitAnything = false;
        Location loc = shooter.getEyeLocation();
        Vector dir = shooter.getLocation().getDirection();
        World world = loc.getWorld();
        for (int i = 0; i < beamLength; i++){

            world.spawnParticle(Particle.HEART,loc,1);
            if (checkCollision(shooter,arenaIn,loc)){
                hitAnything=true;
                updateUses(item);
                return;
            }
            loc.add(dir);
        }
        if (hitAnything) updateUses(item);

    }

    private boolean checkCollision(Player shooter, Arena arena, Location loc){
        Vector position = loc.toVector();
        Vector min = position.clone().subtract(new Vector(0.25,0.25,0.25));
        Vector max = position.clone().add(new Vector(0.25,0.25,0.25));
        BoundingBox bounds = BoundingBox.of(min, max);

        Block block = loc.getBlock();
        if (block.getType().isSolid() && block.getBoundingBox().overlaps(bounds)){
            return onHit(shooter, arena, loc);
        }
        World world = loc.getWorld();
        Collection<Entity> near = world.getNearbyEntities(loc,1,1,1);
        for (Entity e : near){
            if (e.equals(shooter)) continue;
            if (e.getBoundingBox().overlaps(bounds)){
                return onHit(shooter, arena,e.getLocation());
            }
        }
        return false;
    }

    private boolean onHit(Player shooter, Arena arena, Location hitLoc){
        double radius = 1.5;
        boolean anyHit = false;
        Collection<Entity> near = hitLoc.getWorld().getNearbyEntities(hitLoc,radius,radius,radius).stream().filter(e->e instanceof Player).toList();
        for (Entity e : near){
            Player player = (Player) e;
            if (teamsMatch(arena,shooter,player)){
                anyHit = true;
                player.addPotionEffect(new PotionEffect(PotionEffectType.HEAL,1,0));
            }
        }
        return anyHit;
    }

    private boolean teamsMatch(Arena arena, Player shooter, Player hit){
        if (arena != null && arena.hasPlayer(shooter) && arena.hasPlayer(hit)){
            Team red = arena.red();
            Team blue = arena.blue();
            if (red.hasPlayer(shooter) && red.hasPlayer(hit)) return true;
            if (blue.hasPlayer(shooter) && blue.hasPlayer( hit)) return true;
        }
        return false;
    }

}
