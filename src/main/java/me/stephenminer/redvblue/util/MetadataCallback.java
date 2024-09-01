package me.stephenminer.redvblue.util;

@FunctionalInterface
public interface MetadataCallback {
    public void run(org.bukkit.inventory.meta.ItemMeta m);
}
