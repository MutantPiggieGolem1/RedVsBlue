package me.stephenminer.redvblue.util;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.ArenaConfig;

/**
 * A helper to manage scanning the arenas on file
 * This is not in {@link ArenaConfig} because that class should know nothing of global configuration.
 */
public class ArenaConfigUtil {
    private static ConfigFile cfgFile;
    private static ConfigurationSection arenaConfigs;

    public static void initialize(RedBlue plugin) {
        cfgFile = new ConfigFile(plugin, "arenas");
        reload();
    }

    public static void reload() {
        cfgFile.reloadConfig();
        var cfg = cfgFile.getConfig();
        if (!cfg.isConfigurationSection("arenas")) cfg.createSection("arenas");
        arenaConfigs = cfgFile.getConfig().getConfigurationSection("arenas");
    }

    private static String deepToShallow(ArenaConfig arena) {
        if (arenaConfigs == null) throw new IllegalStateException("arenaConfigs has not been initialized.");
        for (String key : arenaConfigs.getKeys(false)) {
            var a = arenaConfigs.getObject(key, ArenaConfig.class);
            if (a == null) continue;
            if (a.id().equals(arena.id())) return key;
        }
        return null; // should NEVER occur
    }

    public static Set<String> idsOnFileShallow() {
        if (arenaConfigs == null) throw new IllegalStateException("arenaConfigs has not been initialized.");
        return arenaConfigs.getKeys(false);
    }
    
    public static Set<String> readyIDsOnFileDeep() {
        return valuesOnFile().stream().filter(ArenaConfig::isBuildable).map(ArenaConfig::id).collect(Collectors.toSet());
    }

    private static Set<ArenaConfig> valuesOnFile() {
        if (arenaConfigs == null) throw new IllegalStateException("arenaConfigs has not been initialized.");
        HashSet<ArenaConfig> builder = new HashSet<>();
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
        return valuesOnFile().stream().anyMatch((a) -> a.id().equals(id));
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
        if (arenaConfigs == null) throw new IllegalStateException("arenaConfigs has not been initialized.");
        arenaConfigs.set(id, arena);
        cfgFile.saveConfig();
    }

    public static void removeFromFileDeep(ArenaConfig arena) {
        if (arenaConfigs == null) throw new IllegalStateException("arenaConfigs has not been initialized.");
        arenaConfigs.set(deepToShallow(arena), null);
        cfgFile.saveConfig();
    }

    public static void save() {
        if (arenaConfigs == null) throw new IllegalStateException("arenaConfigs has not been initialized.");
        cfgFile.saveConfig();
    }
}
