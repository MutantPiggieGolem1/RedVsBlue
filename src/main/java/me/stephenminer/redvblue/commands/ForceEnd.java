package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Arena;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ForceEnd implements CommandExecutor {
    private final RedBlue plugin;

    public ForceEnd(){
        this.plugin = JavaPlugin.getPlugin(RedBlue.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            if (!player.hasPermission("rvb.commands.forceend")){
                player.sendMessage(ChatColor.RED + "No permission!");
                return false;
            }
            Arena arena = arenaIn(player);
            if (arena == null){
                player.sendMessage(ChatColor.RED + "You need to be in an arena that has started to use this command");
                return false;
            }else{
                arena.end();
            }
        }else sender.sendMessage(ChatColor.RED + "Must be a player to use this command!");
        return false;
    }

    private Arena arenaIn(Player player){
        for (Arena arena : Arena.arenas){
            if (arena.hasPlayer(player) && arena.isStarted()) return arena;
        }
        return null;
    }
}
