package me.stephenminer.redvblue.chests;

import me.stephenminer.redvblue.RedBlue;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;

public class TableGui {
    private final RedBlue plugin;
    private final String id;

    private Inventory current;
    private int currentNum;
    private String currentTitle;

    public TableGui(RedBlue plugin, String id){
        this.plugin = plugin;
        this.id = id;
    }


    public Inventory loadTable(int num){
        currentTitle = id + " table #" + num;
        currentNum = num;
        Inventory inv = Bukkit.createInventory(null, 36, currentTitle);
        current = inv;
        for (int i = 27; i < 36; i++){
            inv.setItem(i, filler());
        }
        inv.setItem(31, close());
        if (plugin.tables.getConfig().contains("tables." + id + ".loot." + num)){
            loadLoot(num);
        }
        return current;
    }

    private void loadLoot(int num){
        String path = "tables." + id + ".loot." + num;
        Set<String> section = plugin.tables.getConfig().getConfigurationSection(path).getKeys(false);
        for (String key : section){
            int index = Integer.parseInt(key);
            ItemStack item = plugin.tables.getConfig().getItemStack(path + "." + key);
            current.setItem(index, item);
        }
    }

    public void save(){
        String path = "tables." + id + ".loot." + currentNum;
        for (int i = 0; i < 27; i++){
            ItemStack item = current.getItem(i);
            plugin.tables.getConfig().set(path + "." + i, item);
        }
        plugin.tables.saveConfig();
    }




    public ItemStack filler(){
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
    public ItemStack close(){
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Close");
        item.setItemMeta(meta);
        return item;
    }

    public String currentTitle(){ return currentTitle; }
    public int currentNum(){ return currentNum; }
    public Inventory currentInv(){ return current; }
}
