package me.stephenminer.redvblue.util;

@FunctionalInterface
public interface LocationCallback {
    public void run(org.bukkit.Location l);
}
