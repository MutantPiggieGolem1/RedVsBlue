package me.stephenminer.redvblue.chests;

import org.bukkit.inventory.ItemStack;

public class LootItem {
    private final ItemStack item;
    private int chance;
    public LootItem(ItemStack item, int chance){
        this.item = item;
        this.chance = chance;
    }


    public ItemStack item(){ return item; }
    public int chance(){ return chance; }

    public void setChance(int chance){ this.chance = chance; }





}
