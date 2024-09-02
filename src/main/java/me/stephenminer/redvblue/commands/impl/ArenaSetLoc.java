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
        var arg = args[0].toLowerCase();
        var l = ((Player) sender).getLocation();
        String old = "";
        String wasSet;
        if (arg == "lobby") {
            arena.setLobby(l);
            wasSet = "Lobby";
        } else if (arena.hasTeam(arg)) {
            var r = arena.setSpawn(arg, new BlockVector(l.toVector()));
            if (r != null) old = r.toString();
            wasSet = arg.substring(0, 1).toUpperCase() + arg.substring(1) + " Spawn";
        } else {
            sender.sendMessage(ChatColor.RED + "Arena '" + arena.id() + "' doesn't have a team '"+arg+"'!");
            return false;
        }
        if (!old.isEmpty()) old += " ";
        sender.sendMessage(ChatColor.GREEN + wasSet + ": " + ChatColor.LIGHT_PURPLE + old + ChatColor.GREEN + "-> " + ChatColor.LIGHT_PURPLE + l.toString());
        return true;
    }

    @Override
    public Collection<String> getOptions(ArenaConfig arena, int argPos) {
        if (argPos == 0) {
            var out = arena.getTeams();
            assert out.add("lobby");
            return out;
        }
        return null;
    }
}
