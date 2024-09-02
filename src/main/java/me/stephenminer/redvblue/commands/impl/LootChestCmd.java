package me.stephenminer.redvblue.commands.impl;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.chests.NewLootChest;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LootChestCmd implements CommandExecutor, TabCompleter {
    private final RedBlue plugin;

    public LootChestCmd(){
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            if (!player.hasPermission("rvb.commands.lootchest")){
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return false;
            }
            int size = args.length;
            if (size < 2){
                player.sendMessage(ChatColor.RED + "Not enough args! /rvbchest [material] [loot-table] [loot-table] [etc.]");
                return false;
            }
            Material mat = Material.matchMaterial(args[0]);
            List<String> tables = new ArrayList<>();
            for (int i = 1; i < size; i++){
                String id = args[i];
                if (validTable(id)){
                    tables.add(id);
                }else player.sendMessage(ChatColor.RED + id + " isn't a valid loot-table id");
            }
            NewLootChest lootChest = new NewLootChest(mat, null, tables);
            player.getInventory().addItem(lootChest.item());
            player.sendMessage(ChatColor.GREEN + "Gave you loot-chest item! Place in an inactive arena to add, destroy to remove");
            return true;
        }else sender.sendMessage(ChatColor.RED + "You need to be a player to use this command");
        return false;
    }

    private boolean validTable(String id){
        String path = "tables." + id;
        return plugin.tables.getConfig().contains(path);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size == 1) return Arrays.stream(Material.values()).map(m -> m.name())
            .filter(n -> n.startsWith(args[0])).toList();
        else return plugin.tables.getConfig().getConfigurationSection("tables").getKeys(false).stream()
            .filter(t -> t.startsWith(args[args.length-1])).toList();
    }
}
