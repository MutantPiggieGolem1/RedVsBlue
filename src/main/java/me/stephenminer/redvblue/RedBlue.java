package me.stephenminer.redvblue;

import me.stephenminer.redvblue.arena.Arena;
import me.stephenminer.redvblue.chests.GuiEvents;
import me.stephenminer.redvblue.events.ArenaSetup;
import me.stephenminer.redvblue.events.PlayerHandling;
import me.stephenminer.redvblue.commands.*;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RedBlue extends JavaPlugin {
    public LuckPerms luckPerms;
    public ConfigFile arenas;
    public ConfigFile settings;
    public ConfigFile tables;
    public Location rerouteLoc;
    @Override
    public void onEnable() {
        this.arenas = new ConfigFile(this, "arenas");
        this.settings = new ConfigFile(this, "settings");
        this.tables = new ConfigFile(this, "tables");
        if (this.settings.getConfig().contains("settings.reroute-loc"))
            rerouteLoc = fromString(this.settings.getConfig().getString("settings.reroute-loc"));
        registerCommands();
        registerEvents();
        RegisteredServiceProvider<LuckPerms> luckPermProvider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (luckPermProvider != null){
            luckPerms = luckPermProvider.getProvider();
        }

    }

    @Override
    public void onDisable() {
        for (int i = Arena.arenas.size()-1; i >=0; i--){
            Arena arena = Arena.arenas.get(i);
            arena.reset();
            arena.forceEnd();
        }
        this.settings.saveConfig();
        this.arenas.saveConfig();
    }

    private void registerEvents(){
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerHandling(this), this);
        pm.registerEvents(new GuiEvents(this), this);
        pm.registerEvents(new ArenaSetup(this), this);
    }
    private void registerCommands(){
        getCommand("setRerouteLoc").setExecutor(new RerouteLoc(this));
        getCommand("arenaWand").setExecutor(new ArenaWand());
        getCommand("leaveRvB").setExecutor(new LeaveArena(this));
        getCommand("reloadRvB").setExecutor(new Reload(this));
        getCommand("setMinPlayers").setExecutor(new SetMinPlayers(this));
        getCommand("setWallTime").setExecutor(new SetWallTime(this));
        getCommand("forceRvb").setExecutor(new ForceRvB());

        WallWand wallWand = new WallWand(this);
        getCommand("wallWand").setExecutor(wallWand);
        getCommand("wallWand").setTabCompleter(wallWand);

        ArenaCmd arenaCmd = new ArenaCmd(this);
        getCommand("arena").setExecutor(arenaCmd);
        getCommand("arena").setTabCompleter(arenaCmd);

        JoinArena joinArena = new JoinArena(this);
        getCommand("joinRvB").setExecutor(joinArena);
        getCommand("joinRvB").setTabCompleter(joinArena);

        LootChest lootChest = new LootChest(this);
        getCommand("lootChest").setExecutor(lootChest);
        getCommand("lootChest").setTabCompleter(lootChest);

        Give give = new Give(this);
        getCommand("rvbGive").setExecutor(give);
        getCommand("rvbGive").setTabCompleter(give);




    }


    public String fromBLoc(Location loc){
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
    public String fromLoc(Location loc){
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + ',' + loc.getYaw() + "," + loc.getPitch();
    }


    public Location fromString(String str){
        String[] content = str.split(",");
        String wName = content[0];
        try {
            World world = Bukkit.getWorld(wName);
            double x = Double.parseDouble(content[1]);
            double y = Double.parseDouble(content[2]);
            double z = Double.parseDouble(content[3]);
            if (content.length == 6){
                float yaw = Float.parseFloat(content[4]);
                float pitch = Float.parseFloat(content[5]);
                return new Location(world,x,y,z,yaw,pitch);
            }else return new Location(world, x, y, z);
        }catch (Exception e){
            getLogger().warning(wName + " is not a loaded/existing world!");
            e.printStackTrace();
        }
        return null;
    }

    public boolean checkLore(ItemStack item, String check){
        if (item.hasItemMeta() && item.getItemMeta().hasLore()){
            List<String> lore = item.getItemMeta().getLore();
            check = check.toLowerCase();
            for (String entry : lore){
                String temp = ChatColor.stripColor(entry).toLowerCase();
                if (temp.equals(check)) return true;
            }
        }
        return false;
    }

    public List<String> filter(Collection<String> base, String match){
        List<String> filtered = new ArrayList<>();
        match = match.toLowerCase();
        for (String entry : base){
            String temp = ChatColor.stripColor(entry).toLowerCase();
            if (temp.contains(match)) filtered.add(entry);
        }
        return filtered;
    }
}
