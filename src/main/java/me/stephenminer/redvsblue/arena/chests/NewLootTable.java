package me.stephenminer.redvsblue.arena.chests;

import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import me.stephenminer.redvsblue.RedVsBlue;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Deprecated
public class NewLootTable {
    private final RedVsBlue plugin;
    private final String id;

    private int minEntries, maxRolls, minAmount;
    /**
     * String keys should only be item names associated with the LootItem entry;
     */
    private HashMap<String, LootItem> itemTable;

    public NewLootTable(String id){
        this.plugin = JavaPlugin.getPlugin(RedVsBlue.class);
        this.id = id;
        itemTable = new HashMap<>();
        load();
    }

    public void populate(Inventory inventory){
        inventory.clear();
        byChance = byChance();
        int entries = 0;
        int max = Math.min(inventory.getSize(),maxRolls);
        int min = Math.min(minEntries, inventory.getSize());
        if (itemTable.size() < 1) return;
        for (int i = 0; i < max; i++){
            if (fillItem(inventory)) entries++;
        }
        while(entries <= min){
            if (fillItem(inventory)) entries++;
        }

    }

    private boolean fillItem(Inventory inventory){
        Random random = new Random();
        int base = 100;
        int scalar = 10;
        int roll = random.nextInt(base*scalar);
        int slot = rollSlot(inventory);
        List<ItemStack> validItems = new ArrayList<>();
        for (LootItem lootItem : sortedByChance()){
            if (roll <= lootItem.chance()) {
                validItems = byChance.get(lootItem.chance());
                break;
            }
        }
        if (validItems.size() > 0) {
            ItemStack add = new ItemStack(validItems.get(random.nextInt(validItems.size())));
            int max = Math.min(64,Math.max(minAmount+1, add.getAmount()+1));
            add.setAmount(random.nextInt(minAmount, max));
            inventory.setItem(slot, add);
            return true;
        }
        return false;
    }

    private List<LootItem> sortedByChance(){
        return itemTable.values().stream().sorted(Comparator.comparingInt(LootItem::chance)).toList();
    }

    private HashMap<Integer, List<ItemStack>> byChance;

    private HashMap<Integer, List<ItemStack>> byChance(){
        HashMap<Integer, List<ItemStack>> out = new HashMap<>();
        Set<String> names = itemTable.keySet();
        for (String name : names){
            LootItem lootItem = itemTable.get(name);
            if (out.containsKey(lootItem.chance())){
                out.get(lootItem.chance()).add(lootItem.item());
            }else {
                List<ItemStack> itemList = new ArrayList<>();
                itemList.add(lootItem.item());
                out.put(lootItem.chance(), itemList);
            }
        }
        return out;
    }
    private int rollSlot(Inventory inv){
        int roll = ThreadLocalRandom.current().nextInt(inv.getSize());
        ItemStack current = inv.getItem(roll);
        while (current != null && !current.getType().isAir()){
            roll = ThreadLocalRandom.current().nextInt(inv.getSize());
            current = inv.getItem(roll);
        }
        return roll;
    }

    public void addItem(ItemStack item){
        this.addItem(item, 50);
    }

    /**
     *
     * @param item item to add to the table, items are distinguished by their names (enchanted items will need special names)
     * @param chance the odds for this item to be rolled (rolls are random picks from 0-99)
     */
    public void addItem(ItemStack item, int chance){
        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? ChatColor.stripColor(item.getItemMeta().getDisplayName()) : item.getType().name();

        itemTable.put(name, new LootItem(item, chance));
    }

    public void removeItem(ItemStack item){
        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? ChatColor.stripColor(item.getItemMeta().getDisplayName()) : item.getType().name();
        this.removeItem(name);
    }

    public void removeItem(String name){
        itemTable.remove(name);
    }

    /**
     * Saves loot table to loot-tables.yml file
     */
    public void save(){
        String path = "tables." + id;
        plugin.tables.getConfig().set(path + ".min-entries", minEntries);
        plugin.tables.getConfig().set(path + ".max-rolls",maxRolls);
        path += ".items";
        Set<String> names = itemTable.keySet();
        plugin.tables.getConfig().set(path, null);
        plugin.tables.saveConfig();
        for (String name : names){
            ItemStack item = itemTable.get(name).item();
            int chance = itemTable.get(name).chance();
            plugin.tables.getConfig().set(path + "." + name + ".item",item);
            plugin.tables.getConfig().set(path + "." + name + ".chance",chance);
            plugin.tables.getConfig().set(path + "." + name + ".min-amount",minAmount);
        }
        plugin.tables.saveConfig();
    }

    /**
     * Loads loot table into memory from loot-tables.yml file
     */
    public void load(){
        String path = "tables." + id;
        if (!plugin.tables.getConfig().contains(path)) return;
        minEntries = plugin.tables.getConfig().getInt(path + ".min-entries");
        maxRolls = plugin.tables.getConfig().getInt(path + ".max-rolls");

        if (maxRolls <= 0) maxRolls = 100;

        path+=".items";
        if (!plugin.tables.getConfig().contains(path)) return;
        Set<String> names = plugin.tables.getConfig().getConfigurationSection(path).getKeys(false);
        for (String name : names){
            ItemStack item = plugin.tables.getConfig().getItemStack(path + "." + name + ".item");
            int chance = plugin.tables.getConfig().getInt(path + "." + name + ".chance");
            minAmount = plugin.tables.getConfig().getInt(path + "." + name + ".min-amount");
            if (minAmount < 1) minAmount = 1;
            LootItem lootItem = new LootItem(item, chance);
            itemTable.put(name, lootItem);
        }
    }

    public HashMap<String, LootItem> getItemMap(){ return itemTable; }

}

