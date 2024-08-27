package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.arena.Arena;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveArena implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            Arena arena = Arena.arenaOf(player).orElse(null);
            if (arena == null){
                player.sendMessage(ChatColor.RED + "You are not participating in a RedVBlue game right now!");
                return false;
            }
            arena.removePlayer(player);
            player.sendMessage(ChatColor.GREEN + "Removing you from the game!");
            return true;
        } else sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
        return false;
    }
}
