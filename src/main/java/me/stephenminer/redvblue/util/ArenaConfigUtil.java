package me.stephenminer.redvblue.util;

import java.util.HashSet;
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
    private static ConfigurationSection arenaConfigs = cfgFile.getConfig().getConfigurationSection("arenas");

    public static void reloadArenaConfigs() {
        var cfg = cfgFile.getConfig();
        if (!cfg.isConfigurationSection("arenas")) cfg.createSection("arenas");
        arenaConfigs = cfgFile.getConfig().getConfigurationSection("arenas");
    }

    private static String deepToShallow(ArenaConfig arena) {
        for (String key : arenaConfigs.getKeys(false)) {
            var a = arenaConfigs.getObject(key, ArenaConfig.class);
            if (a.id().equals(arena.id())) return key;
        }
        return null; // should NEVER occur
    }

    public static Set<String> idsOnFileShallow() {
        return arenaConfigs == null ? Set.of() : arenaConfigs.getKeys(false);
    }

    private static Set<ArenaConfig> valuesOnFile() {
        HashSet<ArenaConfig> builder = new HashSet<>();
        if (arenaConfigs == null) return builder;
        for (String key : arenaConfigs.getKeys(false)) {
            var a = arenaConfigs.getObject(key, ArenaConfig.class);
            if (a != null) builder.add(a); // a will be null for malformed config
        }
        return builder;
    }

    /**
     * Expects arena key to correspond with arena id
     */
    public static boolean existsOnFileShallow(String id) {
        return idsOnFileShallow().contains(id);
    }

    /**
     * Does not expect arena key to correspond with arena id
     */
    public static boolean existsOnFileDeep(String id) {
        return valuesOnFile().stream().anyMatch((a) -> a.id() == id);
    }

    public static @Nullable ArenaConfig findOnFileShallow(String id) {
        return arenaConfigs.getObject(id, ArenaConfig.class);
    }

    public static @Nullable ArenaConfig findOnFileDeep(Location... loc) {
        for (var arena : valuesOnFile()) {
            for (Location l : loc)
                if (arena.contains(l)) return arena;
        }
        return null;
    }

    public static void saveToFileShallow(ArenaConfig arena) {
        saveToFileShallow(arena.id(), arena);
    }

    public static void saveToFileShallow(String id, ArenaConfig arena) {
        arenaConfigs.set(id, arena);
        cfgFile.saveConfig();
    }

    public static void removeFromFileDeep(ArenaConfig arena) {
        arenaConfigs.set(deepToShallow(arena), null);
        cfgFile.saveConfig();
    }
}
