package me.stephenminer.redvblue.chests;

import me.stephenminer.redvblue.RedBlue;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class LootTableEditor {
    public static HashMap<UUID, LootTableEditor> sessions = new HashMap<>();
    private final RedBlue plugin;
    private Inventory gui;
    private NewLootTable table;
    private String id;
    private boolean definingChance;
    /**
     * The identifier for the item currently having its chance defined. Null if this isnt happening
     */
    private String chanceItem;

    public LootTableEditor(String id){
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
        this.id = id;
        this.table = new NewLootTable(id);
        gui = Bukkit.createInventory(null, 54,id + " loot-table");
    }

    /**
     * Fills inventory with entries from table
     */
    public void fillInv(){
        gui.clear();
        HashMap<String, LootItem> itemMap = table.getItemMap();
        Set<String> names = table.getItemMap().keySet();
        for (String name : names){
            gui.addItem(modifyItem(new ItemStack(itemMap.get(name).item())));
        }
    }

    private ItemStack modifyItem(ItemStack item){
        ItemMeta meta = item.getItemMeta();
        List<String> lore = item.hasItemMeta() && meta.hasLore() ? meta.getLore() : new ArrayList<>();
        LootItem lootItem = table.getItemMap().getOrDefault(itemName(item),null);
        int chance = lootItem != null ? lootItem.chance() : 50;
        lore.add(0,ChatColor.YELLOW + "" + ChatColor.BOLD + "Chance: " + chance );
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack revertItem(ItemStack item){
        if (item.hasItemMeta() && item.getItemMeta().hasLore()){
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.getLore();
            for (int i = lore.size()-1; i>=0; i--){
                String entry = lore.get(i);
                if (entry.contains("Chance: ")){
                    System.out.println(1);
                    lore.remove(i);
                }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Writes inventory contents from gui to table. Removes entries from table not in gui, and adds entries to table if they aren't already there from gui
     */
    public void writeContents(){
        List<String> names = new ArrayList<>();
        for (ItemStack item : gui.getContents()){
            if (item == null) continue;
            String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? ChatColor.stripColor(item.getItemMeta().getDisplayName()) : item.getType().name();
            names.add(name);
        }
        HashMap<String, LootItem> loot = table.getItemMap();
        Set<String> realNames = new HashSet<>(loot.keySet());
        for (String name : realNames){
            if (!names.contains(name)) table.getItemMap().remove(name);
        }
        for (int i = 0; i < gui.getSize(); i++){
            ItemStack item = gui.getItem(i);
            if (item == null) continue;
            ItemStack reverted = revertItem(item);
            String name = itemName(reverted);
            if (!realNames.contains(name)) {
                table.addItem(reverted);
            }
        }
        table.save();
    }

    /**
     *
     * @param name item name
     * @param chance some integer between 0-100
     */
    public void writeChance(String name, int chance){
        LootItem oldItem = table.getItemMap().get(name);
        oldItem.setChance(chance);
        table.save();
    }

    public String itemName(ItemStack item){
        return item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? ChatColor.stripColor(item.getItemMeta().getDisplayName()) : item.getType().name();
    }

    public String chanceItem(){ return chanceItem; }

    /**
     *
     * @param chanceItem
     */
    public void setChanceItem(String chanceItem){
        this.chanceItem = chanceItem;
    }
    public boolean definingChange(){ return definingChance; }

    public void setDefiningChance(boolean definingChance) { this.definingChance = definingChance; }
    public Inventory gui(){ return gui; }

}
