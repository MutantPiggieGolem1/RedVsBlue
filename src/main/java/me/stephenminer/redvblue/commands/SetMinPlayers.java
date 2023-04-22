package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.RedBlue;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetMinPlayers implements CommandExecutor {

    private final RedBlue plugin;
    public SetMinPlayers(RedBlue plugin){
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            if (!player.hasPermission("rvb.commands.minplayers")){
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return false;
            }
        }
        if (args.length < 1){
            sender.sendMessage(ChatColor.RED + "Not enough arguments!");
            return false;
        }
        int min = Integer.parseInt(args[0]);
        plugin.settings.getConfig().set("settings.min-players", min);
        plugin.settings.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Set min players needed to start a round!");
        return true;
    }
}
