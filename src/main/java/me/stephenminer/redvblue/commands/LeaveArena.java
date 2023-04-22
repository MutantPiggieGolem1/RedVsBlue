package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Arena;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveArena implements CommandExecutor {
    private final RedBlue plugin;

    public LeaveArena(RedBlue plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            Arena arena = arenaIn(player);
            if (arena == null){
                player.sendMessage(ChatColor.RED + "You are not participating in a RedVBlue game right now!");
                return false;
            }
            arena.removePlayer(player);
            player.sendMessage(ChatColor.GREEN + "Removing you from the game!");
            return true;
        }else sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
        return false;
    }

    private Arena arenaIn(Player player){
        for (int i = Arena.arenas.size()-1; i>=0; i--){
            Arena arena = Arena.arenas.get(i);
            if (arena.hasPlayer(player)) return arena;
        }
        return null;
    }
}
