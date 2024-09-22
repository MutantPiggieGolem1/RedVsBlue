package me.stephenminer.redvsblue.commands.impl;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import me.stephenminer.redvsblue.commands.HandledCommand;

public record MessageResponse(String message) implements HandledCommand {

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GRAY + message);
        return true;
    }
}
