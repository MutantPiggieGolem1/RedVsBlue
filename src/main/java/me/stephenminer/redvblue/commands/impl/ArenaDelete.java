package me.stephenminer.redvblue.commands.impl;

import java.util.Collection;

import org.bukkit.command.CommandSender;

import me.stephenminer.redvblue.arena.ArenaConfig;
import me.stephenminer.redvblue.commands.ArenaCommandTreeHandler.ArenaHandledCommand;
import me.stephenminer.redvblue.util.ArenaConfigUtil;

public class ArenaDelete implements ArenaHandledCommand {

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, ArenaConfig arena, String[] args) {
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
