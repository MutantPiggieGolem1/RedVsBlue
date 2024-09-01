package me.stephenminer.redvblue.events.items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import me.stephenminer.redvblue.CustomItems;
import me.stephenminer.redvblue.RedBlue;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class LongRifleUse implements Listener {
    private final RedBlue plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long cooldownDurationMS = 20 * 1000;

    public LongRifleUse() {
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
    }

    @EventHandler
    public void shootRifle(PlayerInteractEvent event){
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();

        if (!CustomItems.LONGRIFLE.is(item)) return;
        if (!player.isSneaking()){
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(net.md_5.bungee.api.ChatColor.AQUA + "You must be sneaking to shoot!"));
            return;
        }
        if (cooldowns.containsKey(player.getUniqueId()) && cooldowns.get(player.getUniqueId()) > now) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(net.md_5.bungee.api.ChatColor.AQUA + "Ability on Cooldown!"));
            return;
        }
        if (!checkAndTakeAmmo(player)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(net.md_5.bungee.api.ChatColor.AQUA + "Missing mana-powder or arrows!"));
            return;
        }
        
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setVelocity(arrow.getVelocity().multiply(3));
        arrow.setKnockbackStrength(3);
        new BukkitRunnable(){
            @Override
            public void run(){
                if (arrow.isDead() || arrow.isInBlock()) {
                    this.cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.DUST,arrow.getLocation(),1,new Particle.DustOptions(Color.AQUA,1));
            }
        }.runTaskTimer(plugin,0,1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE,2,1);

        cooldowns.put(player.getUniqueId(), now + cooldownDurationMS);
        updateCooldownExpBar(player);
    }


    @EventHandler
    public void onCrouch(PlayerToggleSneakEvent event) {onScopeChange(event.getPlayer());}
    @EventHandler
    public void onItemSwap(PlayerItemHeldEvent event) {onScopeChange(event.getPlayer());}

    private void onScopeChange(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (player.isSneaking() && CustomItems.LONGRIFLE.is(item)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, 5, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    private boolean checkAndTakeAmmo(Player player){
        ItemStack powder = null;
        ItemStack arrow = null;
        for (ItemStack item : player.getInventory().getContents()){
            if (item == null) continue;
            if (CustomItems.MANAPOWDER.is(item)) {
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

    private void updateCooldownExpBar(Player player) {
        long donetime = cooldowns.get(player.getUniqueId());
        float exp = player.getExp();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()){
                    this.cancel();
                    return;
                }
                long now = System.currentTimeMillis();
                if (now >= donetime) { //done, reset.
                    player.playSound(player,Sound.BLOCK_IRON_TRAPDOOR_OPEN,2.5f,1);
                    player.playSound(player,Sound.BLOCK_IRON_TRAPDOOR_CLOSE,2.5f,1);
                    player.setExp(exp);
                    this.cancel();
                    return;
                }
                player.setExp(1 - ( (donetime - now) / cooldownDurationMS ));
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
