package me.stephenminer.redvsblue.commands.impl;

import org.bukkit.ChatColor;

import me.stephenminer.redvsblue.RedVsBlue;
import me.stephenminer.redvsblue.commands.HandledCommand;

public class PlayerLimit implements HandledCommand {
    private final RedVsBlue plugin;
    private final String settingNode, prettyStr;
    public PlayerLimit(RedVsBlue plugin, boolean isMinimum){
        this.plugin = plugin;
        this.settingNode = "playerlimit."+(isMinimum ? "min" : "max");
        this.prettyStr = (isMinimum ? "Min" : "Max")+"imum Players: ";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public String permission() {
        return "rvb.commands.config."+settingNode;
    }

    @Override
    public boolean execute(org.bukkit.command.CommandSender sender, String[] args) {
        int oldVal = plugin.getConfig().getInt("settings."+settingNode);
        if (args.length == 0){
            sender.sendMessage(ChatColor.GREEN + prettyStr + ChatColor.LIGHT_PURPLE + oldVal);
            return true;
        }
        int newVal;
        try {
            newVal = Integer.parseInt(args[0]);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "'" + args[0] + "' is not a number!");
            return false;
        }
        plugin.getConfig().set("settings."+settingNode, newVal);
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + prettyStr +  ChatColor.LIGHT_PURPLE + oldVal + ChatColor.GREEN + " -> " + ChatColor.LIGHT_PURPLE + newVal);
        return true;
    }
}
