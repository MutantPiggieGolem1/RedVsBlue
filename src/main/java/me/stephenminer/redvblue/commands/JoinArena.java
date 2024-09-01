package me.stephenminer.redvblue.commands;

import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Arena;
import me.stephenminer.redvblue.arena.ArenaConfig;
import me.stephenminer.redvblue.commands.CommandTreeHandler.HandledCommand;

public class JoinArena implements HandledCommand {
    private final RedBlue plugin;
    public JoinArena(RedBlue plugin){
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
            if (args.length > 1 && p.hasPermission("rvb.commands.join.others")) {
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
            sender.sendMessage(ChatColor.RED + "Arena '" + aid + "'' does not exist!");
            return false;
        }
        arena.addPlayer(target);
        sender.sendMessage(ChatColor.GREEN + "Added " + (isSame ? "you" : "'" + target.getName() + "'") + " to arena '" + aid + "'!");
        return true;
    }

    @Override
    public List<String> getOptions(int argPos) {
        if (argPos == 0) return List.copyOf(plugin.arenas.getConfig()
            .getConfigurationSection("arenas").getKeys(false));
        if (argPos == 1) return Bukkit.getOnlinePlayers()
            .stream().map((p) -> p.getName()).toList();
        return null;
    }

    private @Nullable Arena findOrCreateArena(String id) {
        for (Arena arena : Arena.arenas) {
            if (arena.getId().equals(id)) return arena;
        }
        var arenaConfig = (ArenaConfig) plugin.arenas.getConfig().get("arenas." + id);
        if (arenaConfig == null) return null;
        return arenaConfig.build();
    }
}
