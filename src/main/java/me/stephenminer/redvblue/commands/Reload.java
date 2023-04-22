package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.RedBlue;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Reload implements CommandExecutor {
    private final RedBlue plugin;
    public Reload(RedBlue plugin){
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            if (!player.hasPermission("rvb.commands.reload")){
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return false;
            }
        }
        plugin.settings.reloadConfig();
        plugin.arenas.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Reloaded Files");
        return true;
    }
}
