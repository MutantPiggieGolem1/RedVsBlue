package me.stephenminer.redvsblue.util;

import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import me.stephenminer.redvsblue.RedVsBlue;
import me.stephenminer.redvsblue.arena.ArenaConfig;

/**
 * A helper to manage scanning the arenas on file
 * This is not in {@link ArenaConfig} because that class should know nothing of global configuration.
 */
public class ArenaConfigUtil {
    private static ConfigFile cfgFile;
    private static ConfigurationSection arenaConfigs;

    public static void initialize(RedVsBlue plugin) {
        cfgFile = new ConfigFile(plugin, "arenas");
        reload();
    }

    public static void reload() {
        cfgFile.reloadConfig();
        arenaConfigs = cfgFile.getConfig().getConfigurationSection("arenas");
    }

    // private static String deepToShallow(ArenaConfig arena) {
    //     if (arenaConfigs == null) throw new IllegalStateException("arenaConfigs has not been initialized.");
    //     for (String key : arenaConfigs.getKeys(false)) {
    //         var a = arenaConfigs.getObject(key, ArenaConfig.class);
    //         if (a == null) continue;
    //         if (a.id().equals(arena.id())) return key;
    //     }
    //     return null; // should NEVER occur
    // }

    public static Set<String> idsOnFileShallow() {
        if (arenaConfigs == null) throw new IllegalStateException("arenaConfigs has not been initialized.");
        return arenaConfigs.getKeys(false);
    }
    
    public static Set<String> readyIDsOnFileDeep() {
        return valuesOnFile().stream().filter(ArenaConfig::isBuildable).map(ArenaConfig::id).collect(Collectors.toSet());
    }

    private static Set<ArenaConfig> valuesOnFile() {
        if (arenaConfigs == null) throw new IllegalStateException("arenaConfigs has not been initialized.");
        return idsOnFileShallow().stream().map(ArenaConfigUtil::findOnFileShallow).collect(Collectors.toSet());
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
        var a = arenaConfigs.getObject(id, ArenaConfig.class);
        if (a == null) return null;
        assert a.id() == id;
        return a;
    }

    public static @Nullable ArenaConfig findOnFileDeep(Block block) {
        return valuesOnFile().stream().filter(a -> a.getBounds().contains(block)).findAny().orElse(null);
    }

    public static @Nullable ArenaConfig findOnFileDeep(BlockRange range) {
        return valuesOnFile().stream().filter(a -> a.getBounds().overlaps(range)).findAny().orElse(null);
    }

    public static void saveToFileDeep(ArenaConfig arena) {
        if (arenaConfigs == null) throw new IllegalStateException("arenaConfigs has not been initialized.");
        arenaConfigs.set(arena.id(), arena);
        cfgFile.saveConfig();
    }

    public static void removeFromFileDeep(ArenaConfig arena) {
        if (arenaConfigs == null) throw new IllegalStateException("arenaConfigs has not been initialized.");
        arenaConfigs.set(arena.id(), null);
        cfgFile.saveConfig();
    }

    public static void save() {
        if (arenaConfigs == null) throw new IllegalStateException("arenaConfigs has not been initialized.");
        cfgFile.saveConfig();
    }
}
