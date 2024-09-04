package me.stephenminer.redvsblue.util;

@FunctionalInterface
public interface MetadataCallback {
    public void run(org.bukkit.inventory.meta.ItemMeta m);
}
