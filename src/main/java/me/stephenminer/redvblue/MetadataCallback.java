package me.stephenminer.redvblue;

@FunctionalInterface
public interface MetadataCallback {
    public void run(org.bukkit.inventory.meta.ItemMeta m);
}
