package me.stephenminer.redvsblue.commands.impl;

import java.util.Collection;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.stephenminer.redvsblue.ArenaSelector;
import me.stephenminer.redvsblue.RedVsBlue;
import me.stephenminer.redvsblue.arena.Arena;
import me.stephenminer.redvsblue.commands.HandledCommand;
import me.stephenminer.redvsblue.util.ArenaConfigUtil;

public class JoinArena implements HandledCommand {
    private final RedVsBlue plugin;
    public JoinArena(RedVsBlue plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        boolean isSame = false; // whether the command is targeting someone else
        Player target;
        if (sender instanceof Player p) {
            if (args.length == 0) {
                ArenaSelector gui = new ArenaSelector(plugin);
                gui.display(p);
                return true;
            } else if (args.length > 1 && p.hasPermission("rvb.commands.join.others")) {
                target = Bukkit.getPlayer(args[1]);
            } else target = p;
            if (target.getUniqueId().equals(p.getUniqueId())) isSame = true;
        } else {
            if (args.length < 2) {
                sender.sendMessage("If not a player, you must provide both a player target and an arena id.");
                return false;
            }
            target = Bukkit.getPlayer(args[1]);
        }
        if (target == null) {
            sender.sendMessage("Player with name '" + args[1] + "' doesn't exist!");
            return false;
        }
        if (Arena.arenaOf(target).isPresent()) {
            sender.sendMessage((isSame ? "You are" : "'" + target.getName() + "' is") + " already in an arena!");
            return false;
        }

        var aid = args[0].toLowerCase();
        Arena arena = findOrCreateArena(aid);
        if (arena == null) {
            sender.sendMessage(ChatColor.RED + "Arena '" + aid + "' does not exist!");
            return false;
        }
        arena.addPlayer(target);
        sender.sendMessage(ChatColor.GREEN + "Added " + (isSame ? "you" : "'" + target.getName() + "'") + " to arena '" + aid + "'!");
        return true;
    }

    @Override
    public Collection<String> getOptions(int argPos) {
        if (argPos == 0) return Stream.concat(
                Arena.joinableArenas().stream().map(Arena::getId),
                ArenaConfigUtil.readyIDsOnFileDeep().stream()
            ).toList();
        if (argPos == 1) return Bukkit.getOnlinePlayers()
            .stream().map((p) -> p.getName()).toList();
        return null;
    }

    private @Nullable Arena findOrCreateArena(String id) {
        return Arena.arenaOf(id).orElseGet(() -> {
            var arenaConfig = ArenaConfigUtil.findOnFileShallow(id);
            if (arenaConfig == null) return null;
            return arenaConfig.build();
        });
    }
}
