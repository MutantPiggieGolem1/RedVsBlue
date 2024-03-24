package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.RedBlue;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MapRegenCmd implements CommandExecutor {
    private final RedBlue plugin;

    public MapRegenCmd(){
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            if (!player.hasPermission("rvb.commands.regen")){
                player.sendMessage(ChatColor.RED + "No permission!");
                return false;
            }
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "You need to input a number for a block regen rate!");
            return false;
        }
        int rate = Integer.parseInt(args[0]);
        plugin.settings.getConfig().set("settings.map-regen-rate",rate);
        plugin.settings.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Set regen rate to " + rate);
        return true;
    }
}
