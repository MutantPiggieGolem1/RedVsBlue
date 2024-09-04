package me.stephenminer.redvsblue;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;
import java.util.HashMap;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import me.stephenminer.redvsblue.util.MetadataCallback;

public enum CustomItems {
    ARENAWAND(Material.WOODEN_SHOVEL, ChatColor.GOLD + "Arena Wand", List.of(
        ChatColor.ITALIC + "Sets corners for an arena",
        ChatColor.YELLOW + "Left-Click set pos1",
        ChatColor.YELLOW + "Right-Click set pos2",
        ChatColor.BLACK + "arena-wand"
    )),
    WALLWAND(Material.STONE_SHOVEL, ChatColor.GOLD + "Wall-Maker Wand", List.of(
        ChatColor.ITALIC + "Sets corners for an arena wall",
        ChatColor.YELLOW + "Left-Click set pos1",
        ChatColor.YELLOW + "Right-Click set pos2",
        ChatColor.BLACK + "wall-wand"
    )),
    WALLREMOVER(Material.STONE_PICKAXE, ChatColor.GOLD + "Wall-Remover Wand", List.of(
        ChatColor.ITALIC + "Selects arena wall to delete",
        ChatColor.YELLOW + "Left-Click select",
        ChatColor.BLACK + "wall-remover"
    )),
    THROWINGJUICE(Material.NETHER_STAR, ChatColor.DARK_AQUA + "Throwing Juice", List.of(
        ChatColor.ITALIC + "A hearty way to help a friend!",
        ChatColor.ITALIC + "Shoot an AOE healing beam",
        ChatColor.YELLOW + "Right-Click to Use",
        ChatColor.BLACK + "throwingjuice"
    ), (meta) -> {
        meta.addEnchant(Enchantment.THORNS, ThreadLocalRandom.current().nextInt(4) + 1, true);
    }),
    LONGRIFLE(Material.WOODEN_HOE, ChatColor.DARK_GREEN + "" + ChatColor.ITALIC + "Dwarf Long-Rifle", List.of(
        ChatColor.ITALIC + "Latest dwarf innovation! Combine arrows with mana!",
        ChatColor.YELLOW + "Requires mana powder & arrows",
        ChatColor.YELLOW + "Right-Click & Sneak to Use",
        ChatColor.BLACK + "longrifle"
    ), (meta) -> {
        meta.addEnchant(Enchantment.PROTECTION,4, true);
    }),
    MANAPOWDER(Material.GUNPOWDER, ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + "Mana-Powder", List.of(
        ChatColor.ITALIC + "Not that knife ear mana!",
        ChatColor.ITALIC + "This is good dwarf mana!",
        ChatColor.YELLOW + "Ammo for mana-powered weapons",
        ChatColor.BLACK + "manapowder"
    ), (meta) -> {
        meta.addEnchant(Enchantment.KNOCKBACK,2, true);
    }),
    WINDSCROLL(Material.CREEPER_BANNER_PATTERN, ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "Wind Scroll", List.of(
        ChatColor.ITALIC + "Wind crashes into your face as",
        ChatColor.ITALIC + "you open the scroll",
        ChatColor.YELLOW + "Right-Click to Use",
        ChatColor.BLACK + "windscroll"
    ), (meta) -> {
        meta.addEnchant(Enchantment.MENDING,1,true);
    }),
    TEAMPANTS(Material.LEATHER_LEGGINGS, null, List.of(
        ChatColor.ITALIC + "Standard issue.",
        ChatColor.BLACK + "teampants"
    )),
    TEAMBOOTS(Material.LEATHER_BOOTS, null, List.of(
        ChatColor.ITALIC + "Standard issue.",
        ChatColor.BLACK + "teamboots"
    ), (meta) -> {
        meta.addEnchant(Enchantment.FEATHER_FALLING,1,true);
    }),
    TEAMBANNER(Material.BROWN_BANNER, ChatColor.GOLD + "Team Standard", List.of(
        ChatColor.ITALIC + "Standard issue.",
        ChatColor.BLACK + "teambanner"
    ), (meta) -> {
        meta.addEnchant(Enchantment.THORNS, 3, true);
    });

    public final ItemStack mcitem;
    CustomItems(Material material, String displayName, List<String> lore) {
        this(material, displayName, lore, null);
    }
    CustomItems(Material material, String displayName, List<String> lore, @Nullable MetadataCallback callback) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (displayName != null) meta.setDisplayName(displayName);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        meta.setUnbreakable(true);
        if (callback != null) callback.run(meta);
        item.setItemMeta(meta);
        this.mcitem = item;
    }

    public boolean is(ItemStack toCheck) {
        if (toCheck == null || !toCheck.hasItemMeta() || !toCheck.getItemMeta().hasLore()) return false;
        if (toCheck.getType() != mcitem.getType()) return false;
        var other = toCheck.getItemMeta();
        var mine = mcitem.getItemMeta();
        return other.getLore().equals(mine.getLore());
    }

    public static void addKit(PlayerInventory inv, ChatColor color) {
        assert color.isColor();
        var c0 = color.asBungee().getColor();
        var c1 = Color.fromRGB(c0 == null ? 0x964B00 : c0.getRGB());
        var c2 = colorMap.getOrDefault(color, DyeColor.BROWN);

        inv.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        inv.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));

        var leggings = TEAMPANTS.mcitem.clone();
        ((LeatherArmorMeta) leggings.getItemMeta()).setColor(c1);
        inv.setLeggings(leggings);

        var boots = TEAMBOOTS.mcitem.clone();
        ((LeatherArmorMeta) leggings.getItemMeta()).setColor(c1);
        inv.setBoots(boots);

        ItemStack shield = new ItemStack(Material.SHIELD);
        var shieldMeta = (BlockStateMeta) shield.getItemMeta();
        var shieldBlockState = (Banner) shieldMeta.getBlockState();
        shieldBlockState.setBaseColor(c2);
        shieldMeta.setBlockState(shieldBlockState);
        shieldMeta.setUnbreakable(true);
        shield.setItemMeta(shieldMeta);
        inv.setItemInOffHand(shield);

        inv.addItem(new ItemStack(Material.STONE_AXE));
        inv.addItem(new ItemStack(Material.IRON_PICKAXE));

        var banner = TEAMBANNER.mcitem.clone();
        banner.setType(Material.valueOf(c2.name()+"_BANNER"));
        inv.addItem(banner);
    }

    private static final Map<ChatColor, DyeColor> colorMap = new HashMap<>(13, 1) {{
        put(ChatColor.WHITE, DyeColor.WHITE);
        put(ChatColor.GOLD, DyeColor.ORANGE);
        put(ChatColor.BLUE, DyeColor.LIGHT_BLUE);
        put(ChatColor.YELLOW, DyeColor.YELLOW);
        put(ChatColor.GREEN, DyeColor.LIME);
        put(ChatColor.LIGHT_PURPLE, DyeColor.PINK);
        put(ChatColor.GRAY, DyeColor.LIGHT_GRAY);
        put(ChatColor.AQUA, DyeColor.CYAN);
        put(ChatColor.DARK_PURPLE, DyeColor.PURPLE);
        put(ChatColor.DARK_BLUE, DyeColor.BLUE);
        put(ChatColor.DARK_GREEN, DyeColor.GREEN);
        put(ChatColor.RED, DyeColor.RED);
        put(ChatColor.BLACK, DyeColor.BLACK);
    }};
}
