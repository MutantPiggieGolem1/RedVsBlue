package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Wall;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ArenaCmd implements CommandExecutor, TabCompleter {
    private final RedBlue plugin;
    public ArenaCmd(RedBlue plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size < 2){
            sender.sendMessage(ChatColor.RED + "You need to input at least an arena-id and subcommand to use this command!");
            return false;
        }
        if (!validId(args[0])){
            sender.sendMessage(ChatColor.RED + args[0] + " is not a real arena!");
            return false;
        }
        if (sender instanceof Player player){
            if (!player.hasPermission("rvb.commands.arena")){
                player.sendMessage(ChatColor.RED + "You do not have permisison to use this command!");
                return false;
            }
            String id = args[0];
            String subCmd = args[1];

            if (subCmd.equalsIgnoreCase("setRedSpawn")){
                setTeamSpawn(id, (byte)0, player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Set red-team spawn!");
                return true;
            }
            if (subCmd.equalsIgnoreCase("setBlueSpawn")){
                setTeamSpawn(id, (byte)1, player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Set blue-team spawn!");
                return true;
            }
            if (subCmd.equalsIgnoreCase("setLobby")){
                setLobbySpawn(id, player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Set lobby spawn!");
                return true;
            }
            if (subCmd.equalsIgnoreCase("removeWall")){
                removeWall(id);
                player.sendMessage(ChatColor.GREEN + "Deleted the wall");
                return true;
            }
            if (subCmd.equalsIgnoreCase("delete")){
                delete(id);
                player.sendMessage(ChatColor.DARK_RED + "DELETED " + id + "! This cannot be undone!!!");
                return true;
            }
            if (size >= 3){
                if (args[1].equalsIgnoreCase("setWallType")){
                    Material mat = null;
                    try{
                        mat = Material.matchMaterial(ChatColor.stripColor(args[2]).toUpperCase());
                    }catch (Exception ignored){}
                    if (mat == null){
                        player.sendMessage(ChatColor.RED + "Invalid Material, defaulting to glass!");
                        mat = Material.GLASS;
                    }
                    setWallType(id, mat);
                }
            }
        }else  sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
        return false;
    }

    private boolean validId(String arenaId){
        return plugin.arenas.getConfig().contains("arenas." + arenaId);
    }

    private void setTeamSpawn(String id, byte team, Location loc){
        String path = "arenas." + id;
        path += (team == 0 ? ".red-spawn" : ".blue-spawn");
        plugin.arenas.getConfig().set(path, plugin.fromLoc(loc));
        plugin.arenas.saveConfig();
    }
    private void setLobbySpawn(String id, Location loc){
        String path = "arenas." + id + ".lobby";
        plugin.arenas.getConfig().set(path, plugin.fromLoc(loc));
        plugin.arenas.saveConfig();
    }

    private void setWallType(String id, Material type){
        String path = "arenas." + id + ".wall.type";
        plugin.arenas.getConfig().set(path, type.name());
        plugin.saveConfig();
    }

    private void removeWall(String id){
        String path = "arenas." + id + ".wall";
        plugin.arenas.getConfig().set(path, null);
        plugin.arenas.saveConfig();
    }
    private void delete(String id){
        plugin.arenas.getConfig().set("arenas." + id, null);
        plugin.arenas.saveConfig();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size == 1) return arenaIds(args[0]);
        if (size == 2) return subCmds(args[1]);
        if (size == 3){
            if (args[1].equalsIgnoreCase("setWallType")) return materials(args[2]);
        }
        return null;
    }

    private List<String> arenaIds(String match){
        Set<String> entries = plugin.arenas.getConfig().getConfigurationSection("arenas").getKeys(false);
        return plugin.filter(entries, match);
    }
    private List<String> subCmds(String match){
        List<String> subs = new ArrayList<>();
        subs.add("setRedSpawn");
        subs.add("setBlueSpawn");
        subs.add("setLobby");
        subs.add("removeWall");
        subs.add("setWallType");
        subs.add("delete");
        return plugin.filter(subs, match);
    }
    private List<String> materials(String match){
        List<String> mats = new ArrayList<>();
        Material[] values = Material.values();
        for (Material mat : values){
            mats.add(mat.name());
        }
        return plugin.filter(mats, match);
    }
}
