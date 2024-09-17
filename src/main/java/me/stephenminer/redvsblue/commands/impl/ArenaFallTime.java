package me.stephenminer.redvsblue.commands.impl;

import java.util.Collection;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import me.stephenminer.redvsblue.arena.ArenaConfig;
import me.stephenminer.redvsblue.commands.ArenaCommandTreeHandler.ArenaHandledCommand;

public class ArenaFallTime implements ArenaHandledCommand {

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, ArenaConfig arena, String[] args) {
        int oldVal = arena.getWallFallTime();
        if (args.length == 0){
            sender.sendMessage(ChatColor.GREEN + "Arena '" + arena.id() + "' fall time: " + ChatColor.LIGHT_PURPLE + oldVal);
            return true;
        }
        int newVal;
        try {
            newVal = Integer.parseInt(args[0]);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "'" + args[0] + "' is not a number!");
            return false;
        }
        arena.setWallFallTime(newVal);
        sender.sendMessage(ChatColor.GREEN + "Arena '" + arena.id() + "' fall time: " +  ChatColor.LIGHT_PURPLE + oldVal + ChatColor.GREEN + " -> " + ChatColor.LIGHT_PURPLE + newVal);
        return true;
    }

    @Override
    public Collection<String> getOptions(ArenaConfig arena, int argPos) {
        return null;
    }
    
}
