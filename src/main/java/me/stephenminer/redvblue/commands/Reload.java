package me.stephenminer.redvblue.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.commands.CommandTreeHandler.HandledCommand;

public class Reload implements HandledCommand {

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
        var plugin = JavaPlugin.getPlugin(RedBlue.class);
        plugin.reloadConfig();
        plugin.arenas.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Reloaded Files");
        return true;
    }

    @Override
    public List<String> getOptions(int argPos) {
        return null;
    }
}
