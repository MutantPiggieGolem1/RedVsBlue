package me.stephenminer.redvblue.chests;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ChestSetupEvents implements Listener {
    private final RedBlue plugin;

    public ChestSetupEvents() {
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
    }

    /*
     * CHEST ADDING AND REMOVING BELOW
     *
     */

    @EventHandler
    public void addChest(BlockPlaceEvent event){
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        if (!isChestItem(item)) return;
        NewLootChest lootChest = parseData(event.getBlock().getLocation(),item);
        if (lootChest == null) {
            player.sendMessage(ChatColor.RED + "Something went wrong! You probably aren't placing this chest within an actual region");
        }else player.sendMessage(ChatColor.GREEN + "Successfully added your lootchest");
    }

    @EventHandler
    public void removeChest(BlockBreakEvent event){
        Player player = event.getPlayer();
        if (!player.hasPermission("rvb.lootchests.remove")) return;
        String regionId = regionIn(event.getBlock());
        for (Arena arena : Arena.arenas){
            if (arena.getId().equals(regionId)) return;
        }
        boolean removed = removeFromArena(regionId, event.getBlock().getLocation());
        if (removed){
            player.sendMessage(ChatColor.GREEN + "Removed lootchest from " + regionId);
        }
    }



    private boolean isChestItem(ItemStack item){
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        List<String> lore = item.getItemMeta().getLore();
        return lore.contains("RvB LootChest");
    }

    private NewLootChest parseData(Location loc, ItemStack item){
        Material mat = item.getType();
        if (!isChestItem(item)) return null;
        String arenaId = regionIn(loc.getBlock());
        if (arenaId == null) return null;
        List<String> lore = item.getItemMeta().getLore();
        //dataStr = "table1,table2,etc";
        String dataStr = null;
        for (String str : lore){
            if (str.contains("tables:")){
                dataStr = str.replace("tables:","");
                break;
            }
        }
        if (dataStr != null){
            List<String> tables = new ArrayList<>(List.of(dataStr.split(",")));
            NewLootChest lootChest = new NewLootChest(mat, loc, tables);
            saveToArena(arenaId, lootChest);
            return lootChest;
        }
        return null;
    }


    private String regionIn(Block block){
        Set<String> ids = plugin.arenas.getConfig().getConfigurationSection("arenas").getKeys(false);
        Vector v1 = block.getLocation().toVector();
        Vector v2 = v1.clone().add(new Vector(1,1,1));
        for (String id : ids){
            String path = "arenas." + id;
            Location loc1 = plugin.fromString(plugin.arenas.getConfig().getString(path + ".loc1"));
            Location loc2 = plugin.fromString(plugin.arenas.getConfig().getString(path + ".loc2"));
            BoundingBox box = BoundingBox.of(loc1, loc2);
            if (box.overlaps(v1, v2)){
                return id;
            }
        }
        return null;
    }

    private void saveToArena(String arenaId, NewLootChest chest){
        List<String> chests = plugin.arenas.getConfig().getStringList("arenas." + arenaId + ".loot-chests");
        chests.add(chest.toString());
        plugin.arenas.getConfig().set("arenas." + arenaId + ".loot-chests", chests);
        plugin.arenas.saveConfig();
    }

    private boolean removeFromArena(String arenaId, Location loc){
        List<String> chests = plugin.arenas.getConfig().getStringList("arenas." + arenaId + ".loot-chests");
        for (int i = chests.size()-1; i>=0; i++){
            String entry = chests.get(i);
            if (entry.contains(plugin.fromBLoc(loc))){
                chests.remove(i);
                return true;
            }
        }
        plugin.arenas.getConfig().set("arenas." + arenaId + ".loot-chests", chests);
        plugin.arenas.saveConfig();
        return false;
    }


    /*
     * CHEST ADDING AND REMOVING ABOVE
     *
     */

    /*
     *CHEST GUI THINGS BELOW
     */

    @EventHandler
    public void promptChances(InventoryClickEvent event){
        if (!event.isRightClick() || !event.getView().getTopInventory().equals(event.getClickedInventory())) return;
        Player player = (Player) event.getWhoClicked();
        if (!LootTableEditor.sessions.containsKey(player.getUniqueId())) return;
        Inventory inv = event.getClickedInventory();
        LootTableEditor editor = LootTableEditor.sessions.get(player.getUniqueId());
        if (!editor.gui().equals(inv)) return;
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        editor.setDefiningChance(true);
        editor.setChanceItem(editor.itemName(item));
        player.closeInventory();
        player.sendMessage(ChatColor.GOLD + "Type out a number 1-100 in chat to define the chance to receive this item");
    }

    @EventHandler
    public void handleClosing(InventoryCloseEvent event){
        Player player = (Player) event.getPlayer();
        if (!LootTableEditor.sessions.containsKey(player.getUniqueId())) return;
        LootTableEditor editor = LootTableEditor.sessions.get(player.getUniqueId());
        Inventory inventory = event.getInventory();
        if (!editor.gui().equals(inventory)) return;
        editor.writeContents();
        player.sendMessage(ChatColor.YELLOW + "Writing contents to loot-table");
        if (!editor.definingChange()) {
            LootTableEditor.sessions.remove(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Ended your loot-table editing session");
        }
    }

    @EventHandler
    public void defineChance(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        if (!LootTableEditor.sessions.containsKey(player.getUniqueId())) return;
        LootTableEditor editor = LootTableEditor.sessions.get(player.getUniqueId());
        if (!editor.definingChange()) return;
        try {
            int chance = Integer.parseInt(ChatColor.stripColor(event.getMessage()));
            if (chance < 1) chance = 1;
            if (chance > 100) chance = 100;
            if (editor.chanceItem() != null) {
                editor.writeChance(editor.chanceItem(), chance);
                player.sendMessage(ChatColor.GREEN + "Set chance for " + editor.chanceItem() + " to " + chance);
            }else player.sendMessage(ChatColor.RED + "Something went wrong and the editor cannot remember what item you are setting the chance for");
            editor.setChanceItem(null);
            editor.setDefiningChance(false);
            return;
        }catch (Exception e){
            e.printStackTrace();
        }
        player.sendMessage(ChatColor.RED + "Try again. The number you input must be in integer form, from 1-100");
    }
}
