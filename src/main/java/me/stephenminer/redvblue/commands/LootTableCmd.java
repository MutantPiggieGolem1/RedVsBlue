package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.chests.LootTableEditor;
import me.stephenminer.redvblue.arena.chests.NewLootTable;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class LootTableCmd implements CommandExecutor, TabCompleter {
    private final RedBlue plugin;

    public LootTableCmd(){
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
            if (size < 1) {
                player.sendMessage(ChatColor.RED + "You need to input a subcommand!");
                return false;
            }
            String sub = args[0].toLowerCase();
            //create
            if (sub.equals("create")){
                if (size < 2) {
                    player.sendMessage(ChatColor.RED + "Not enough args! /rvbloot create [id]");
                    return false;
                }
                String id = args[1];
                if (existingTables().contains(id)){
                    player.sendMessage(ChatColor.RED + "This table-id already exists! Use /rvbloot edit if you wish to edit the table");
                    return false;
                }
                createLootTable(id);
                player.sendMessage(ChatColor.GREEN + "Created new loot table " + id);
                player.sendMessage(ChatColor.YELLOW + "Now use /rvbloot edit " + id + " in order to edit the loot table!");
                return true;
            }
            //min-entries
            if (sub.equals("min-entries")){
                if (size < 3){
                    player.sendMessage(ChatColor.RED + "Not enough args! /rvbloot min-entries [id] [min # of entries when populating an inventory]");
                    return false;
                }
                String id = args[1];
                if (!existingTables().contains(id)){
                    player.sendMessage(ChatColor.RED + "Input id " + id + " doesn't exist!");
                    return false;
                }
                int minEntries = Integer.parseInt(args[2]);
                setMinEntries(id, minEntries);
                player.sendMessage(ChatColor.GREEN + "Set min-entries to  "+ minEntries + " for " + id);
                return true;
            }
            //max-rolls
            if (sub.equals("max-rolls")){
                if (size < 3){
                    player.sendMessage(ChatColor.RED + "Not enough args! /rvbloot max-rolls [id] [max # of rolls when populating an inventory (overridden by min-entries)]");
                    return false;
                }
                String id = args[1];
                if (!existingTables().contains(id)){
                    player.sendMessage(ChatColor.RED + "Input id " + id + " doesn't exist!");
                    return false;
                }
                int maxRolls = Integer.parseInt(args[2]);
                setMaxRolls(id, maxRolls);
                player.sendMessage(ChatColor.GREEN + "Set max-rolls to  "+ maxRolls + " for " + id);
                return true;
            }
            //edit
            if (sub.equals("edit")){
                if (size < 2){
                    player.sendMessage(ChatColor.RED + "Not enough args! /rvbloot edit [id]");
                    return false;
                }
                String id = args[1];
                if (!existingTables().contains(id)){
                    player.sendMessage(ChatColor.RED + "Input id " + id + " doesn't exist!");
                    return false;
                }

                LootTableEditor editor = new LootTableEditor(id);
                editor.fillInv();
                LootTableEditor.sessions.put(player.getUniqueId(),editor);
                player.openInventory(editor.gui());
                player.sendMessage(ChatColor.GREEN + "Opening session to for table " + id);
                return true;
            }
            //delete
            if (sub.equals("delete")){
                if (size < 3){
                    player.sendMessage(ChatColor.RED + "Not enough args! /rvbloot delete [id] confirm");
                    return false;
                }
                String confirm = args[2];
                if (!confirm.equalsIgnoreCase("confirm")){
                    player.sendMessage(ChatColor.RED + "You need to confirm deletion! /rvbloot delete [id] confirm");
                    return false;
                }
                String id = args[1];
                if (!existingTables().contains(id)){
                    player.sendMessage(ChatColor.RED + "Input id " + id + " doesn't exist!");
                    return false;
                }
                delete(id);
                player.sendMessage(ChatColor.GREEN + "Deleted loot-table " + id);
                return true;
            }
        }else sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
        return false;
    }
    private Set<String> existingTables(){
        if (!plugin.tables.getConfig().contains("tables")) return new HashSet<>();
        else return plugin.tables.getConfig().getConfigurationSection("tables").getKeys(false);
    }

    private void createLootTable(String id){
        NewLootTable lootTable = new NewLootTable(id);
        lootTable.save();
    }
    private void setMinEntries(String id, int min){
        plugin.tables.getConfig().set("tables." + id + ".min-entries",min);
        plugin.tables.saveConfig();
    }
    private void setMaxRolls(String id, int max){
        plugin.tables.getConfig().set("tables." + id + ".max-rolls",max);
        plugin.tables.saveConfig();
    }
    private void delete(String id){
        plugin.tables.getConfig().set("tables." + id, null);
        plugin.tables.saveConfig();
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size == 1) return subCmds(args[0]);
        if (size == 2) return arenaIds(args[1]);
        if (size == 3){
            String sub = args[1].toLowerCase();
            if (sub.equals("max-rolls") || sub.equals("min-entries")) return integer();
        }
        return null;
    }


    private List<String> subCmds(String match){
        return List.of("create","delete","edit","max-rolls","min-entries").stream().filter(n -> n.startsWith(match)).toList();
    }
    private List<String> arenaIds(String match){
        return existingTables().stream().filter(n -> n.startsWith(match)).toList();
    }
    private List<String> integer(){
        List<String> out = new ArrayList<>();
        out.add("[integer]");
        return out;
    }
}
