package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.arena.Arena;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForceRvB implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            if (!player.hasPermission("rvb.commands.forcestart")){
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return false;
            }
            Arena arena = Arena.arenaOf(player).orElse(null);
            boolean setTeams = false;
            if (args.length > 0) setTeams = ChatColor.stripColor(args[0]).equalsIgnoreCase(("setTeams"));
            if (arena != null){
                if (!arena.isStarted()) {
                    arena.start();
                    player.sendMessage(ChatColor.GREEN + "Force-Starting arena");
                    return true;
                } else player.sendMessage(ChatColor.RED + "You cannot start an arena that has already started!");
            } else player.sendMessage(ChatColor.RED + "You are not in an arena!");
        } else sender.sendMessage(ChatColor.RED + "You must be a player to use this command!");
        return false;
    }
}
