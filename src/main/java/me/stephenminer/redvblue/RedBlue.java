package me.stephenminer.redvblue;

import java.util.Map;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.stephenminer.redvblue.arena.Arena;
import me.stephenminer.redvblue.arena.chests.ChestSetupEvents;
import me.stephenminer.redvblue.commands.*;
import me.stephenminer.redvblue.commands.impl.*;
import me.stephenminer.redvblue.events.*;
import me.stephenminer.redvblue.events.items.*;
import me.stephenminer.redvblue.util.ArenaConfigUtil;
import me.stephenminer.redvblue.util.ConfigFile;

public final class RedBlue extends JavaPlugin {
    public ConfigFile tables;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        ArenaConfigUtil.initialize(this);
        this.tables = new ConfigFile(this, "loot-tables");
        registerCommands();
        registerEvents();
        ArenaConfigUtil.reload();
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
        pm.registerEvents(new ChestSetupEvents(this), this);
        pm.registerEvents(new ArenaSelector.EventListener(), this);
    }

    
    /**
     * /rvbconfig arena <id> 
     *      spawn lobby/red/blue
     *      wall
     *          list
     *          material <#> <mat>
     *          delete <#>
     *      delete
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
                // "wall", new ArenaWalls(), TODO implement
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

    @Deprecated
    public String fromBLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    @Deprecated
    public String fromLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + ',' + loc.getYaw()
                + "," + loc.getPitch();
    }

    @Deprecated
    public Location fromString(@Nonnull String str) {
        String[] content = str.split(",");
        String wName = content[0];
        try {
            var world = this.getServer().getWorld(wName);
            double x = Double.parseDouble(content[1]);
            double y = Double.parseDouble(content[2]);
            double z = Double.parseDouble(content[3]);
            if (content.length == 6) {
                float yaw = Float.parseFloat(content[4]);
                float pitch = Float.parseFloat(content[5]);
                return new Location(world, x, y, z, yaw, pitch);
            } else
                return new Location(world, x, y, z);
        } catch (Exception e) {
            getLogger().warning(wName + " is not a loaded/existing world!");
            e.printStackTrace();
        }
        return null;
    }

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
}
