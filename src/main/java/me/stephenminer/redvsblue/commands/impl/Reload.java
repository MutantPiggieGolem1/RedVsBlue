package me.stephenminer.redvsblue.commands.impl;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import me.stephenminer.redvsblue.RedVsBlue;
import me.stephenminer.redvsblue.commands.HandledCommand;

public class Reload implements HandledCommand {
    private final RedVsBlue plugin;
    public Reload(RedVsBlue plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public String permission() {
        return "rvb.commands.config.reload";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Reloaded Files");
        return true;
    }
}
