package me.stephenminer.redvsblue.commands.impl;

import java.util.Collection;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.stephenminer.redvsblue.arena.Arena;
import me.stephenminer.redvsblue.commands.HandledCommand;

public class ForceEnd implements HandledCommand {

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public String permission() {
        return "rvb.commands.forceend";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Arena a = null;
        if (args.length == 0) {
            if (sender instanceof Player player) {
                var oa = Arena.arenaOf(player);
                if (!oa.isPresent()) {
                    sender.sendMessage(ChatColor.RED + "You must be in an arena or provide an arena id.");
                    return false;
                }
                a = oa.get();
            } else {
                sender.sendMessage("If not a player, you must provide an arena id.");
                return false;
            }
        } else {
            String id = args[0].toLowerCase();
            a = Arena.arenaOf(id).get();
        }
        if (!a.forceEnd()) {
            sender.sendMessage(ChatColor.RED + "Arena is already ending!");
            return false;
        }
        return true;
    }

    @Override
    public Collection<String> getOptions(int argPos) {
        if (argPos == 0) return Arena.arenas.stream().map((a) -> a.getId()).toList();
        return null;
    }
}
