package me.stephenminer.redvsblue.commands.impl;

import java.util.Collection;

import org.bukkit.command.CommandSender;

import me.stephenminer.redvsblue.arena.ArenaConfig;
import me.stephenminer.redvsblue.commands.ArenaCommandTreeHandler.ArenaHandledCommand;
import me.stephenminer.redvsblue.util.ArenaConfigUtil;

public class ArenaDelete implements ArenaHandledCommand {

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, ArenaConfig arena, String[] args) { // SUBOPTIMAL add confirmation
        ArenaConfigUtil.removeFromFileDeep(arena);
        return true;
    }

    @Override
    public Collection<String> getOptions(int argPos) {
        return null;
    }

    @Override
    public Collection<String> getOptions(ArenaConfig arena, int argPos) {
        return null;
    }
}
