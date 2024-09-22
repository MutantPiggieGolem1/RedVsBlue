package me.stephenminer.redvsblue.commands.impl;

import java.util.Collection;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import me.stephenminer.redvsblue.arena.ArenaConfig;
import me.stephenminer.redvsblue.commands.ArenaCommandTreeHandler.ArenaHandledCommand;
import me.stephenminer.redvsblue.util.StringCaser;

public class ArenaRemoveTeam implements ArenaHandledCommand {

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, ArenaConfig arena, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Invalid arguments!");
            return false;
        }
        var arg = args[0].toLowerCase();
        if (!arena.delSpawn(arg)) {
            sender.sendMessage(ChatColor.RED + "Arena '" + arena.id() + "' doesn't have a team '" + arg + "'.");
            return false;
        }
        sender.sendMessage(
                ChatColor.GREEN + StringCaser.toTitleCase(arg) + " Spawn: -> " + ChatColor.LIGHT_PURPLE + "null");
        return true;
    }

    @Override
    public Collection<String> getOptions(ArenaConfig arena, int argPos) {
        if (argPos == 0)
            return arena.getTeams();
        return null;
    }
}
