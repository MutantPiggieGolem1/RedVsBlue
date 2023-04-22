package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.RedBlue;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RerouteLoc implements CommandExecutor {
    private final RedBlue plugin;

    public RerouteLoc(RedBlue plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            if (!player.hasPermission("rvb.commands.reroute")){
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return false;
            }
            plugin.rerouteLoc = player.getLocation();
            plugin.settings.getConfig().set("settings.reroute-loc", plugin.fromLoc(player.getLocation()));
            plugin.settings.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Set reroute loc");
            return true;
        }else {
            sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
            return false;
        }
    }
}
