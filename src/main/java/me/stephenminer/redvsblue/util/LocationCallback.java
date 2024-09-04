package me.stephenminer.redvsblue.util;

@FunctionalInterface
public interface LocationCallback {
    public void run(org.bukkit.Location l);
}
