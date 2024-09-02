package me.stephenminer.redvblue.commands.impl;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.stephenminer.redvblue.arena.Arena;
import me.stephenminer.redvblue.commands.HandledCommand;

public class LeaveArena implements HandledCommand {

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        boolean isSame = false; // whether the command is targeting someone else
        Player target;
        if (sender instanceof Player p) {
            if (args.length > 0 && p.hasPermission("rvb.commands.leave.others")) {
                target = Bukkit.getPlayer(args[0]);
            } else target = p;
            if (target.getUniqueId().equals(p.getUniqueId())) isSame = true;
        } else {
            if (args.length < 1) {
                sender.sendMessage("If not a player, you must provide a player target.");
                return false;
            }
            target = Bukkit.getPlayer(args[0]);
        }
        if (target == null) {
            sender.sendMessage("Player with name '" + args[0] + "' doesn't exist!");
            return false;
        }

        var oa = Arena.arenaOf(target);
        if (!oa.isPresent()) {
            sender.sendMessage((isSame ? "You are" : "'" + target.getName() + "' is") + " not in an arena!");
            return false;
        }
        Arena a = oa.get();

        a.removePlayer(target);
        sender.sendMessage(ChatColor.GREEN + "Removed " + (isSame ? "you" : "'" + target.getName() + "'") + " from arena '" + a.getId() + "'!");
        return true;
    }

    @Override
    public Collection<String> getOptions(int argPos) {
        if (argPos == 0) return Bukkit.getOnlinePlayers()
            .stream().map((p) -> p.getName()).toList();
        return null;
    }
}
