package me.stephenminer.redvblue.events.items;

import me.stephenminer.redvblue.RedBlue;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LongRifleUse implements Listener {
    private final RedBlue plugin;
    private final Set<UUID> cooldown;

    public LongRifleUse(RedBlue plugin){
        this.plugin = plugin;
        cooldown = new HashSet<>();
    }

    @EventHandler
    public void shootRifle(PlayerInteractEvent event){
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (plugin.checkLore(item,"longrifle")){
            if (!player.isSneaking()){
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(net.md_5.bungee.api.ChatColor.AQUA + "You must be sneaking to shoot!"));
                return;
            }
            if (cooldown.contains(player.getUniqueId())) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(net.md_5.bungee.api.ChatColor.AQUA + "Ability on Cooldown!"));
                return;
            }
            boolean shoot = checkAndTakeAmmo(player);
            if (shoot) {
                fire(player);
                cooldown.add(player.getUniqueId());
                runCooldown(player,20);
            }else {
                player.sendMessage(ChatColor.RED + "You are missing mana-powder or arrows!");
            }
        }
    }


    @EventHandler
    public void onCrouch(PlayerToggleSneakEvent event){
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        new BukkitRunnable(){
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.isSneaking()){
                    this.cancel();
                    if (player.isOnline()) player.removePotionEffect(PotionEffectType.SLOWNESS);
                }
                if (plugin.checkLore(main,"longrifle")){
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,5,5));
                }else{
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    this.cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin,0,1);
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        cooldown.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event){
        cooldown.remove(event.getPlayer().getUniqueId());
    }

    private boolean checkAndTakeAmmo(Player player){
        ItemStack powder = null;
        ItemStack arrow = null;
        ItemStack[] items = player.getInventory().getContents();
        for (ItemStack item : items){
            if (item == null) continue;
            if (plugin.checkLore(item,"manapowder")) {
                powder = item;
                continue;
            }
            Material mat = item.getType();
            if (mat == Material.ARROW || mat == Material.TIPPED_ARROW || mat == Material.SPECTRAL_ARROW){
                arrow = item;
                continue;
            }
        }
        if (powder == null || arrow == null) return false;
        powder.setAmount(powder.getAmount()-1);
        arrow.setAmount(arrow.getAmount()-1);
        return true;
    }

    private void fire(Player shooter){
        Vector dir = shooter.getLocation().getDirection();
        Arrow arrow = shooter.launchProjectile(Arrow.class);
        arrow.setVelocity(arrow.getVelocity().multiply(3));
        arrow.setKnockbackStrength(3);
        new BukkitRunnable(){
            World world = arrow.getWorld();
            @Override
            public void run(){
                if (arrow.isDead() || arrow.isInBlock()) {
                    this.cancel();
                    return;
                }
                world.spawnParticle(Particle.DUST,arrow.getLocation(),1,new Particle.DustOptions(Color.AQUA,1));
            }
        }.runTaskTimer(plugin,0,1);
        World world = shooter.getWorld();
        world.spawnParticle(Particle.ASH,shooter.getEyeLocation().clone().add(dir),10);
        world.playSound(shooter.getLocation(), Sound.ENTITY_GENERIC_EXPLODE,2,1);
    }

    /**
     *
     * @param player
     * @param duration - time in ticks
     */
    private void runCooldown(Player player, int duration){
        cooldown.add(player.getUniqueId());
        float exp = player.getExp();
        new BukkitRunnable(){
            int count;
            @Override
            public void run(){
                if (!player.isOnline() || player.isDead()){
                    this.cancel();
                    return;
                }
                if (count >= duration){
                    player.playSound(player,Sound.BLOCK_IRON_TRAPDOOR_OPEN,2.5f,1);
                    player.playSound(player,Sound.BLOCK_IRON_TRAPDOOR_CLOSE,2.5f,1);
                    player.setExp(exp);
                    cooldown.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                float ratio = ((float) count) / duration;
                player.setExp(ratio);
                count++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
