package me.stephenminer.redvblue.util;

import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.ArenaConfig;

/**
 * A helper to manage scanning the arenas on file
 * This is not in {@link ArenaConfig} because that class should know nothing of global configuration.
 */
public class ArenaConfigUtil {
    private static final ConfigFile cfgFile = JavaPlugin.getPlugin(RedBlue.class).arenas;
    private static final ConfigurationSection arenaConfigs = cfgFile.getConfig().getConfigurationSection("arenas");

    /**
     * Expects arena key to correspond with arena id
     */
    public static Set<String> idsOnFileShallow() {
        return arenaConfigs.getKeys(false);
    }

    public static boolean existsOnFileShallow(String id) {
        return idsOnFileShallow().contains(id);
    }

    /**
     * Does not expect arena key to correspond with arena id
     */
    public static boolean existsOnFileDeep(String id) {
        return arenaConfigs.getValues(false).values().stream().anyMatch((a) -> ((ArenaConfig) a).id() == id);
    }

    public static @Nullable ArenaConfig findOnFileShallow(String id) {
        return arenaConfigs.getObject(id, ArenaConfig.class);
    }

    public static @Nullable ArenaConfig findOnFileDeep(Location... loc) {
        for (var arenaEntry : arenaConfigs.getValues(false).entrySet()) {
            var arenaConfig = (ArenaConfig) arenaEntry.getValue();
            for (Location l : loc)
                if (arenaConfig.contains(l)) return arenaConfig;
        }
        return null;
    }

    public static void saveToFile(String id, ArenaConfig arena) {
        arenaConfigs.set(id, arena);
        cfgFile.saveConfig();
    }
}
