package me.stephenminer.redvblue;

import org.bukkit.inventory.ItemStack;

public class ItemTranslator {
    public ItemStack fromId(String id){
        Items items = new Items();
        switch (id){
            case "throwingjuice" -> {
                return items.throwingJuice(3);
            }
            case "longrifle" -> {
                return items.longRifle();
            }
            case "manapowder" -> {
                return items.manaPowder();
            }
        }
        return null;
    }
}
