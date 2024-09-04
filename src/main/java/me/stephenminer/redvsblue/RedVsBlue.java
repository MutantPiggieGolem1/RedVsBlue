package me.stephenminer.redvsblue;

import java.util.Map;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.stephenminer.redvsblue.arena.Arena;
import me.stephenminer.redvsblue.commands.*;
import me.stephenminer.redvsblue.commands.impl.*;
import me.stephenminer.redvsblue.events.*;
import me.stephenminer.redvsblue.events.items.*;
import me.stephenminer.redvsblue.util.ArenaConfigUtil;
import me.stephenminer.redvsblue.util.ConfigFile;

public final class RedVsBlue extends JavaPlugin {
    public ConfigFile tables;

    @Override
    public void onLoad() {
        ConfigurationSerialization.registerClass(me.stephenminer.redvsblue.util.BlockRange.class);
        ConfigurationSerialization.registerClass(me.stephenminer.redvsblue.arena.ArenaConfig.class);
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        ArenaConfigUtil.initialize(this);
        this.tables = new ConfigFile(this, "loot-tables");

        registerCommands();
        registerEvents();
    }

    @Override
    public void onDisable() {
        Arena.arenas.forEach(Arena::absoluteForceEnd);
        this.saveConfig();
        ArenaConfigUtil.save();
        this.tables.saveConfig();
    }

    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerHandling(), this);
        pm.registerEvents(new SetupWandsUse(), this);
        pm.registerEvents(new LongRifleUse(this), this);
        pm.registerEvents(new ThrowingJuiceUse(), this);
        pm.registerEvents(new WindScrollUse(), this);
        // pm.registerEvents(new ChestSetupEvents(this), this);
        pm.registerEvents(new ArenaSelector.EventListener(), this);
    }

    
    /**
     * /rvb
     *      join
     *      
     * /rvbconfig
     *      reload
     *      minplayers <#>
     *      maxplayers <#>
     *      arena <id> 
     *          spawn lobby/red/blue
     *          wall
     *              list
     *              material <#> <mat>
     *              delete <#>
     *         delete
     */
    private void registerCommands() {
        register("rvb", new CommandTreeHandler(Map.of(
            "join", new JoinArena(this),
            "leave", new LeaveArena(),
            "forcestart", new ForceStart(),
            "forceend", new ForceEnd()
        )));
        register("rvbconfig", new CommandTreeHandler(Map.of(
            "reload", new Reload(this),
            "minplayers", new PlayerLimit(this, true),
            "maxplayers", new PlayerLimit(this, false),
            "arena", new ArenaCommandTreeHandler(Map.of(
                "spawn", new ArenaSetLoc(),
                // "wall", new ArenaWalls(), SUBOPTIMAL implement
                "falltime", new ArenaFallTime(),
                "delete", new ArenaDelete()
            )) {
                @Override
                public String permission() {
                    return "rvb.commands.config.arena";
                }
            }
        )));
        register("rvbgive", new GiveCustom());

        // SUBOPTIMAL not yet updated to new command framework
        register("rvbchest", new LootChestCmd());
        register("rvbloot", new LootTableCmd());
    }

    private void register(String name, CommandExecutor executor) {
        var cmd = getCommand(name);
        cmd.setExecutor(executor);
        if (executor instanceof TabCompleter t) cmd.setTabCompleter(t);
    }

    // CONFIGURATION
    @Override
    public void reloadConfig() {
        ArenaConfigUtil.reload();
        super.reloadConfig();
    }

    /**
     * @return How many blockstates per tick to update
     */
    public int loadRate() {
        return Math.max(7000, this.getConfig().getInt("settings.map-regen-rate"));
    }

    public int loadMinPlayers() {
        return this.getConfig().getInt("settings.playerlimit.min");
    }

    public int loadArenaStartDelay() {
        return this.getConfig().getInt("settings.timings.waitbeforestart");
    }

    public int loadArenaRevealDelay() {
        return this.getConfig().getInt("settings.timings.waitbeforereveal");
    }
    // ===
}
