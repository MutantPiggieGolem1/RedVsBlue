package me.stephenminer.redvblue;

import java.util.Map;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.stephenminer.redvblue.arena.Arena;
import me.stephenminer.redvblue.arena.chests.ChestSetupEvents;
import me.stephenminer.redvblue.commands.*;
import me.stephenminer.redvblue.events.*;
import me.stephenminer.redvblue.events.items.*;
import me.stephenminer.redvblue.util.ArenaConfigUtil;
import me.stephenminer.redvblue.util.ConfigFile;

public final class RedBlue extends JavaPlugin {
    public ConfigFile arenas;
    public ConfigFile tables;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.arenas = new ConfigFile(this, "arenas");
        this.tables = new ConfigFile(this, "loot-tables");
        registerCommands();
        registerEvents();
        ArenaConfigUtil.reloadArenaConfigs();
    }

    @Override
    public void onDisable() {
        for (Arena arena : Arena.arenas) {
            arena.reset();
            arena.forceEnd();
        }
        this.saveConfig();
        this.arenas.saveConfig();
        this.tables.saveConfig();
    }

    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerHandling(this), this);
        pm.registerEvents(new SetupWandsUse(), this);
        pm.registerEvents(new LongRifleUse(), this);
        pm.registerEvents(new ThrowingJuiceUse(), this);
        pm.registerEvents(new WindScrollUse(), this);
        pm.registerEvents(new ChestSetupEvents(), this);
        pm.registerEvents(new ArenaSelector.EventListener(), this);
    }

    private void registerCommands() {
        register("rvb", new CommandTreeHandler(Map.of(
            "join", new JoinArena(),
            "leave", new LeaveArena(),
            "forcestart", new ForceStart(),
            "forceend", new ForceEnd()
        )));
        register("rvbconfig", new CommandTreeHandler(Map.of(
            "reload", new Reload(),
            "minplayers", new MinPlayers(this)
        )));
        register("rvbgive", new GiveCustom());

        // WIP - not yet updated to new command framework
        register("rvbarena", new ArenaCmd(this));
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
            World world = Bukkit.getWorld(wName);
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

    public int loadRate() {
        return Math.max(7000, this.getConfig().getInt("settings.map-regen-rate"));
    }

    public int loadMinPlayers() {
        return this.getConfig().getInt("settings.min-players");
    }
}
