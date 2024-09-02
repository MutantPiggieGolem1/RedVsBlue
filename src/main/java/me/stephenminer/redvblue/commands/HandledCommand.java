package me.stephenminer.redvblue.commands;

import java.util.Collection;

import org.bukkit.command.CommandSender;

public interface HandledCommand {
    public boolean playerOnly();
    public default String permission() {return null;}
    public boolean execute(CommandSender sender, String[] args);
    public default Collection<String> getOptions(int argPos) {return null;}
}