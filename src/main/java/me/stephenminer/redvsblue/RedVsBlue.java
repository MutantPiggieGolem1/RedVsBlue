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

public final class RedVsBlue extends JavaPlugin {
    @Override
    public void onLoad() {
        ConfigurationSerialization.registerClass(me.stephenminer.redvsblue.util.BlockRange.class);
        ConfigurationSerialization.registerClass(me.stephenminer.redvsblue.arena.ArenaConfig.class);
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        ArenaConfigUtil.initialize(this);

        registerCommands();
        registerEvents();
    }

    @Override
    public void onDisable() {
        Arena.arenas.forEach(Arena::absoluteForceEnd);
        this.saveConfig();
        ArenaConfigUtil.save();
    }

    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerHandling(), this);
        pm.registerEvents(new SetupWandsUse(), this);
        pm.registerEvents(new LootWandUse(this), this);
        pm.registerEvents(new LongRifleUse(this), this);
        pm.registerEvents(new ThrowingJuiceUse(), this);
        pm.registerEvents(new WindScrollUse(), this);
        pm.registerEvents(new ArenaSelector.EventListener(), this);
    }

    private void registerCommands() {
        register("rvb", new CommandTreeHandler(Map.of(
            "help", new MessageResponse("""
Usage:
/rvb
    join/leave [id] [player]
    forcestart/forceend [id]"""),
            "join", new JoinArena(this),
            "leave", new LeaveArena(),
            "forcestart", new ForceStart(),
            "forceend", new ForceEnd()
        )));
        register("rvbconfig", new CommandTreeHandler(Map.of(
            "help", new MessageResponse("""
Usage:
/rvbconfig
    reload
    minplayers [#]
    maxplayers [#]
    startdelay [#]
    revealdelay [#]
    arena <id> 
        spawn lobby
        spawn <teamname>
        rmteam <teamname>
        wall
            list
            material <#> [mat]
            delete <#>
        loot
                delete <#>]
        falltime [#]
        delete"""),
            "reload", new Reload(this),
            "minplayers", new IntConfigChange(this, "playerlimit.min", "Minimum Players"),
            "maxplayers", new IntConfigChange(this, "playerlimit.max", "Maximum Players"),
            "startdelay", new IntConfigChange(this, "timings.waitbeforestart", "Start Delay"),
            "revealdelay",new IntConfigChange(this, "timings.waitbeforereveal", "Player Reveal Delay"),
            "arena", new ArenaCommandTreeHandler(Map.of(
                "spawn", new ArenaSetLoc(),
                "rmteam", new ArenaRemoveTeam(),
                // "wall", new ArenaWalls(), SUBOPTIMAL implement
                "loot", new ArenaLoot(),
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
