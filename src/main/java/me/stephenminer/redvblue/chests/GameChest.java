package me.stephenminer.redvblue.chests;

import me.stephenminer.redvblue.RedBlue;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class GameChest {
    private RedBlue plugin;
    private final Location loc;
    private final Material mat;
    private final String lootId;
    private List<ItemStack[]> loottables;
    private boolean postWall;
    public GameChest(Location loc, Material mat, String lootId){
        this.loc = loc;
        this.mat = mat;
        this.lootId = lootId;
        this.plugin = RedBlue.getPlugin(RedBlue.class);
        loottables = new ArrayList<>();
        loadLoot();
    }

    private void loadLoot(){
        String path = "tables." + lootId + ".loot";
        Set<String> nums = plugin.tables.getConfig().getConfigurationSection(path).getKeys(false);
        int length = nums.size();
        int invSize = 27;
        for (String str : nums){
            Set<String> indexes = plugin.tables.getConfig().getConfigurationSection(path + "." + str).getKeys(false);

            ItemStack[] table = new ItemStack[invSize];
            for (String index : indexes){
                int num = Integer.parseInt(index);
                ItemStack item = plugin.tables.getConfig().getItemStack(path + "." + str + "." + index);
                table[num] = item;
            }
            loottables.add(table);
        }
    }

    public boolean loadContainer(){
        Block block = loc.getBlock();
        block.setType(mat);
        if (block.getState() instanceof Container container){
            Inventory inv = container.getInventory();
            inv.clear();
            ItemStack[] table = loottables.get(ThreadLocalRandom.current().nextInt(loottables.size()));
            inv.setContents(table);
            return true;
        }
        return false;
    }



    public Location getLocation(){ return loc; }
    public Material getMat(){ return mat; }
    public boolean isPostWall(){ return postWall; }

    /**
     * @return the id for the loottables associated with this chest
     */
    public String getId(){ return lootId; }

    public void setPostWall(boolean postWall){
        this.postWall = postWall;
    }

}
