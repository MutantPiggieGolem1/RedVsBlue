package me.stephenminer.redvblue.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.stephenminer.redvblue.arena.Arena;
import me.stephenminer.redvblue.commands.CommandTreeHandler.HandledCommand;

public class ForceStart implements HandledCommand {

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public String permission() {
        return "rvb.commands.forcestart";
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
                a = oa.orElseThrow();
            } else {
                sender.sendMessage("If not a player, you must provide an arena id.");
                return false;
            }
        } else {
            String id = args[0].toLowerCase();
            a = Arena.arenas.stream().filter((ar) -> ar.getId() == id).findFirst().orElseThrow();
        }
        if (a.isStarted()) {
            sender.sendMessage(ChatColor.RED + "You cannot start an arena that's already started!");
            return false;
        }
        a.start();
        return true;
    }

    @Override
    public List<String> getOptions(int argPos) {
        if (argPos == 0) return Arena.arenas.stream().map((a) -> a.getId()).toList();
        return null;
    }
}
