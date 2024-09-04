package me.stephenminer.redvsblue.arena.chests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import me.stephenminer.redvsblue.RedVsBlue;

@Deprecated
public class NewLootChest {
    private final RedVsBlue plugin;
    private Location loc;
    private Material mat;
    private List<String> lootTables;


    public NewLootChest(Material mat, Location loc){
       this(mat, loc, new ArrayList<>());
    }

    public NewLootChest(Material mat, Location loc, List<String> lootTables){
        this.plugin = JavaPlugin.getPlugin(RedVsBlue.class);
        this.loc = loc;
        this.mat = mat;
        this.lootTables = lootTables;
    }

    /**
     *
     * @param str formatted as loc/mat/table1,table2,table3,tablex,etc
     */
    public NewLootChest(String str){
        this.plugin = JavaPlugin.getPlugin(RedVsBlue.class);
        fromString(str);
    }




    public void loadChest(){
        Block block = loc.getBlock();
        block.setType(mat);
        Container container = (Container) block.getState();
        Inventory inv = container.getSnapshotInventory();
        NewLootTable lootTable = loadTable();
        lootTable.populate(inv);
        container.update(true);
    }


    private NewLootTable loadTable(){
        String id = lootTables.get(ThreadLocalRandom.current().nextInt(lootTables.size()));
        return new NewLootTable(id);
    }


    public ItemStack item(){
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        StringBuilder tables = new StringBuilder();
        for (String entry : lootTables) {
            tables.append(entry).append('/');
        }
        tables.deleteCharAt(tables.length()-1);
        lore.add("RvB LootChest");
        lore.add("tables:" + tables);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }


    public String toString(){
        StringBuilder base = new StringBuilder(fromBLoc(loc) + "/" + mat.name() + "/");
        for (String entry : lootTables) {
            base.append(entry).append('/');
        }
        base.deleteCharAt(base.length()-1);
        return base.toString();
    }

    private void fromString(String str){
        String[] split = str.split("/");
        Location loc = locFromString(split[0]);
        Material mat = Material.matchMaterial(split[1]);
        List<String> tables = new ArrayList<>(List.of(split[2].split(",")));
        this.loc = loc;
        this.mat = mat;
        this.lootTables = tables;
    }


    public String fromBLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public String fromLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + ',' + loc.getYaw()
                + "," + loc.getPitch();
    }

    public Location locFromString(@Nonnull String str) {
        String[] content = str.split(",");
        String wName = content[0];
        try {
            var world = plugin.getServer().getWorld(wName);
            double x = Double.parseDouble(content[1]);
            double y = Double.parseDouble(content[2]);
            double z = Double.parseDouble(content[3]);
            if (content.length == 6) {
                float yaw = Float.parseFloat(content[4]);
                float pitch = Float.parseFloat(content[5]);
                return new Location(world, x, y, z, yaw, pitch);
            } else
                return new Location(world, x, y, z);
        } catch (Exception e) {
            plugin.getLogger().warning(wName + " is not a loaded/existing world!");
            e.printStackTrace();
        }
        return null;
    }
}
