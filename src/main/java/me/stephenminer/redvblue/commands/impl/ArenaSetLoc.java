package me.stephenminer.redvblue.commands.impl;

import java.util.Collection;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import me.stephenminer.redvblue.arena.ArenaConfig;
import me.stephenminer.redvblue.commands.ArenaCommandTreeHandler.ArenaHandledCommand;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;

public class ArenaSetLoc implements ArenaHandledCommand {

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, ArenaConfig arena, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Invalid Arguments!");
            return false;
        }
        var arg = args[0].toLowerCase();
        var l = ((Player) sender).getLocation();
        String old = "";
        String wasSet;
        if (arg.equals("lobby")) {
            arena.setLobby(l);
            wasSet = "Lobby";
        } else {
            BlockVector r;
            try {
                r = arena.setSpawn(arg, new BlockVector(l.toVector()));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + e.getMessage());
                return false;
            }
            if (r != null) old = r.toString();
            wasSet = arg.substring(0, 1).toUpperCase() + arg.substring(1) + " Spawn";
        }
        if (!old.isEmpty()) old += " ";
        sender.sendMessage(ChatColor.GREEN + wasSet + ": " + ChatColor.LIGHT_PURPLE + old + ChatColor.GREEN + "-> " + ChatColor.LIGHT_PURPLE + l.toString());
        return true;
    }

    @Override
    public Collection<String> getOptions(ArenaConfig arena, int argPos) {
        return null;
    }
}
