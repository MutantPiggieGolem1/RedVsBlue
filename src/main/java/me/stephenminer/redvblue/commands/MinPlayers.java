package me.stephenminer.redvblue.commands;

import org.bukkit.ChatColor;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.commands.CommandTreeHandler.HandledCommand;

public class MinPlayers implements HandledCommand {

    private final RedBlue plugin;
    public MinPlayers(RedBlue plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public String permission() {
        return "rvb.commands.config.minplayers";
    }

    @Override
    public boolean execute(org.bukkit.command.CommandSender sender, String[] args) {
        int oldMin = plugin.getConfig().getInt("settings.min-players");
        if (args.length == 0){
            sender.sendMessage(ChatColor.GREEN + "Minimum Players: " + ChatColor.LIGHT_PURPLE + oldMin);
            return true;
        }
        int newMin;
        try {
            newMin = Integer.parseInt(args[0]);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "'" + args[0] + "' is not a number!");
            return false;
        }
        plugin.getConfig().set("settings.min-players", newMin);
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Minimum Players: " + ChatColor.LIGHT_PURPLE + oldMin + ChatColor.GREEN + " -> " + ChatColor.LIGHT_PURPLE + newMin);
        return true;
    }

    @Override
    public java.util.List<String> getOptions(int argPos) {
        return null;
    }
}
