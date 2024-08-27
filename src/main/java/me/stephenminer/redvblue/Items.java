package me.stephenminer.redvblue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Banner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class Items {
    public static NamespacedKey USES = new NamespacedKey(JavaPlugin.getPlugin(RedBlue.class),"rbuses");

    public ItemStack arenaWand(){
        ItemStack item = new ItemStack(Material.WOODEN_SHOVEL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Arena Wand");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.ITALIC + "Sets corners for the arena");
        lore.add(ChatColor.YELLOW + "Right-Click set pos2");
        lore.add(ChatColor.YELLOW + "Left-Click set pos1");
        lore.add(ChatColor.BLACK + "arena-wand");
        meta.setLore(lore);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack wallWand(String id){
        ItemStack item = new ItemStack(Material.STONE_SHOVEL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Wall Wand");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.ITALIC + "For adding a wall to your arenas!");
        lore.add(ChatColor.YELLOW + "Left-Click set pos1");
        lore.add(ChatColor.YELLOW + "Right-Click set pos2");
        lore.add(ChatColor.AQUA + "Arena: " + id);
        lore.add(ChatColor.BLACK + "wall-wand");
        meta.setLore(lore);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }
    public ItemStack wallRemover(String id){
        ItemStack item = new ItemStack(Material.STONE_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Wall-Remover Wand");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.ITALIC + "For adding a wall to your arenas!");
        lore.add(ChatColor.YELLOW + "Left-Click set pos1");
        lore.add(ChatColor.YELLOW + "Right-Click set pos2");
        lore.add(ChatColor.AQUA + "Arena: " + id);
        lore.add(ChatColor.BLACK + "wall-remover");
        meta.setLore(lore);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack teamPants(int team){
        ItemStack item = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(team == 0 ? Color.RED : Color.BLUE);
        item.setItemMeta(meta);
        return item;
    }
    public ItemStack teamBoots(int team){
        ItemStack item = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(team == 0 ? Color.RED : Color.BLUE);
        meta.addEnchant(Enchantment.FEATHER_FALLING,1,true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack shield(int team){
        ItemStack item = new ItemStack(Material.SHIELD);
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        Banner banner = (Banner) meta.getBlockState();
        banner.setBaseColor(team == 0 ? DyeColor.RED : DyeColor.BLUE);
        meta.setBlockState(banner);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }
    public void outfitPlayer(Player player, int team){
        PlayerInventory inv = player.getInventory();
        inv.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        inv.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        inv.setLeggings(teamPants(team));
        inv.setBoots(teamBoots(team));
        inv.addItem(new ItemStack(Material.STONE_AXE));
        inv.addItem(new ItemStack(Material.IRON_PICKAXE));
        inv.setItemInOffHand(shield(team));

        Material mat = team == 0 ? Material.RED_BANNER : Material.BLUE_BANNER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Team Standard");
        meta.addEnchant(Enchantment.THORNS, 3, true);
        item.setItemMeta(meta);
        inv.addItem(item);
    }

    public ItemStack throwingJuice(int uses){
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Throwing Juice (" + uses + " uses)");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.ITALIC + "A hearty way to help a friend!");
        lore.add(ChatColor.YELLOW + "R-Click: Shoot an AOE healing beam");
        lore.add(ChatColor.YELLOW + "Uses: " + uses);
        lore.add(ChatColor.BLACK + "throwingjuice");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(Items.USES, PersistentDataType.INTEGER,uses);
        meta.addEnchant(Enchantment.THORNS, ThreadLocalRandom.current().nextInt(4) + 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack manaPowder(){
        ItemStack item = new ItemStack(Material.GUNPOWDER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Mana-Powder");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.ITALIC + "Not that knife ear mana!");
        lore.add(ChatColor.ITALIC + "This is good dwarf mana!");
        lore.add(ChatColor.YELLOW + "Ammo for mana-powered weapons");
        lore.add(ChatColor.BLACK + "manapowder");
        meta.setLore(lore);
        meta.addEnchant(Enchantment.KNOCKBACK,2, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack longRifle(){
        ItemStack item = new ItemStack(Material.WOODEN_HOE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Dwarf Long-Rifle");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.ITALIC + "Latest dwarf innovation! Combine arrows with mana!");
        lore.add(ChatColor.ITALIC + "Watch out for the recoil!");
        lore.add(ChatColor.YELLOW + "Requires mana powder & arrows to use");
        lore.add(ChatColor.YELLOW + "R-Click while shifting: Shoot");
        lore.add(ChatColor.BLACK + "longrifle");
        meta.setLore(lore);
        meta.addEnchant(Enchantment.PROTECTION,4,true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack windScroll(){
        ItemStack item = new ItemStack(Material.CREEPER_BANNER_PATTERN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Wind Scroll");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.ITALIC + "Wind crashes into your face as" );
        lore.add(ChatColor.ITALIC + "you open the scroll" );
        lore.add(ChatColor.YELLOW + "Right-Click to Use");
        lore.add(ChatColor.YELLOW + "8 second cooldown");
        lore.add(ChatColor.BLACK + "windscroll");
        meta.setLore(lore);
        meta.addEnchant(Enchantment.MENDING,1,true);
        item.setItemMeta(meta);
        return item;
    }
}
