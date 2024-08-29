package me.stephenminer.redvblue;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
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

import me.stephenminer.redvblue.events.items.ThrowingJuiceUse;

public enum CustomItems {
    ARENAWAND(Material.WOODEN_SHOVEL, ChatColor.GOLD + "Arena Wand", List.of(
        ChatColor.ITALIC + "Sets corners for the arena",
        ChatColor.YELLOW + "Left-Click set pos1",
        ChatColor.YELLOW + "Right-Click set pos2",
        ChatColor.BLACK + "arena-wand"
    )),
    WALLWAND(Material.STONE_SHOVEL, ChatColor.GOLD + "Wall Wand", List.of(
        ChatColor.ITALIC + "For adding a wall to your arenas!",
        ChatColor.YELLOW + "Left-Click set pos1",
        ChatColor.YELLOW + "Right-Click set pos2",
        ChatColor.BLACK + "wall-wand")),
    WALLREMOVER(Material.STONE_PICKAXE, ChatColor.GOLD + "Wall-Remover Wand", List.of(
        ChatColor.ITALIC + "For removing walls from your arenas!",
        ChatColor.YELLOW + "Left-Click set pos1",
        ChatColor.YELLOW + "Right-Click set pos2",
        ChatColor.BLACK + "wall-remover"
    )),
    THROWINGJUICE(Material.NETHER_STAR, ChatColor.YELLOW + "Throwing Juice (3 uses)", List.of(
        ChatColor.ITALIC + "A hearty way to help a friend!",
        ChatColor.ITALIC + "Shoot an AOE healing beam",
        ChatColor.YELLOW + "Right-Click to Use",
        ChatColor.BLACK + "throwingjuice"
    ), (meta) -> {
        meta.getPersistentDataContainer().set(ThrowingJuiceUse.USES, PersistentDataType.INTEGER, 3);
        meta.addEnchant(Enchantment.THORNS, ThreadLocalRandom.current().nextInt(4) + 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }),
    LONGRIFLE(Material.WOODEN_HOE, ChatColor.LIGHT_PURPLE + "Dwarf Long-Rifle", List.of(
        ChatColor.ITALIC + "Latest dwarf innovation! Combine arrows with mana!",
        ChatColor.ITALIC + "Watch out for the recoil!",
        ChatColor.YELLOW + "Requires mana powder & arrows to use",
        ChatColor.YELLOW + "R-Click while shifting: Shoot",
        ChatColor.BLACK + "longrifle"
    ), (meta) -> {
        meta.addEnchant(Enchantment.PROTECTION,4, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }),
    MANAPOWDER(Material.GUNPOWDER, ChatColor.AQUA + "Mana-Powder", List.of(
        ChatColor.ITALIC + "Not that knife ear mana!",
        ChatColor.ITALIC + "This is good dwarf mana!",
        ChatColor.YELLOW + "Ammo for mana-powered weapons",
        ChatColor.BLACK + "manapowder"
    ), (meta) -> {
        meta.addEnchant(Enchantment.KNOCKBACK,2, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }),
    WINDSCROLL(Material.CREEPER_BANNER_PATTERN, ChatColor.AQUA + "" + ChatColor.BOLD + "Wind Scroll", List.of(
        ChatColor.ITALIC + "Wind crashes into your face as",
        ChatColor.ITALIC + "you open the scroll",
        ChatColor.YELLOW + "Right-Click to Use",
        ChatColor.BLACK + "windscroll"
    ), (meta) -> {
        meta.addEnchant(Enchantment.MENDING,1,true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    });

    public final ItemStack mcitem;
    CustomItems(Material material, String displayName, List<String> lore) {
        this(material, displayName, lore, null);
    }
    CustomItems(Material material, String displayName, List<String> lore, @Nullable MetadataCallback callback) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        meta.setUnbreakable(true);
        if (callback != null) callback.run(meta);
        item.setItemMeta(meta);
        this.mcitem = item;
    }


    private static ItemStack teamPants(int team){
        ItemStack item = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(team == 0 ? Color.RED : Color.BLUE);
        item.setItemMeta(meta);
        return item;
    }
    private static ItemStack teamBoots(int team){
        ItemStack item = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(team == 0 ? Color.RED : Color.BLUE);
        meta.addEnchant(Enchantment.FEATHER_FALLING,1,true);
        item.setItemMeta(meta);
        return item;
    }
    private static ItemStack teamShield(int team){
        ItemStack item = new ItemStack(Material.SHIELD);
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        Banner banner = (Banner) meta.getBlockState();
        banner.setBaseColor(team == 0 ? DyeColor.RED : DyeColor.BLUE);
        meta.setBlockState(banner);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }
    public static void outfitPlayer(Player player, int team){
        PlayerInventory inv = player.getInventory();
        inv.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        inv.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        inv.setLeggings(teamPants(team));
        inv.setBoots(teamBoots(team));
        inv.addItem(new ItemStack(Material.STONE_AXE));
        inv.addItem(new ItemStack(Material.IRON_PICKAXE));
        inv.setItemInOffHand(teamShield(team));

        Material mat = team == 0 ? Material.RED_BANNER : Material.BLUE_BANNER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Team Standard");
        meta.addEnchant(Enchantment.THORNS, 3, true);
        item.setItemMeta(meta);
        inv.addItem(item);
    }
}
