package me.stephenminer.redvblue.commands;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.stephenminer.redvblue.CustomItems;
import me.stephenminer.redvblue.RedBlue;

public class GiveCustom implements CommandExecutor, TabCompleter {
    private final RedBlue plugin;
    public GiveCustom(RedBlue plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player) {
            if (!player.hasPermission("rvb.commands.give")){
                player.sendMessage(ChatColor.RED + "No permission to use this command!");
                return false;
            }
            int size = args.length;
            if (size < 1) return false;

            CustomItems oi;
            try {
                oi = CustomItems.valueOf(args[0]);
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "RVB item with id " + args[0] + ", doesn't exist!");
                return false;
            }
            int amount = 1;
            if (size >= 2) amount = Integer.parseInt(args[1]);
            if (oi == CustomItems.ARENAWAND || oi == CustomItems.WALLWAND || oi == CustomItems.WALLREMOVER) {
                if (!player.hasPermission("rvb.commands.wand")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that!");
                    return false;
                }
                amount = 1;
            }
            ItemStack item = oi.mcitem.clone();
            Player reciever = player;
            if (size >= 3){
                reciever = Bukkit.getPlayerExact(args[1]);
                if (reciever == null){
                    player.sendMessage(ChatColor.RED + "Player " + args[2] + " doesn't exist!");
                    return false;
                }else{
                    player.sendMessage(ChatColor.GREEN + "You gave " + reciever.getName() + " an item!");
                }
            }
            item.setAmount(amount);
            for (var overflow : reciever.getInventory().addItem(item).values()) {
                reciever.getWorld().dropItemNaturally(reciever.getLocation(), overflow);
            }
            reciever.sendMessage(ChatColor.GREEN + "You have been given an item!");
            return true;
        } else sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        switch (args.length) {
            case 1:
                var start = args[0].toLowerCase();
                return Arrays.stream(CustomItems.values()).map((item) -> item.name()).filter((name) -> name.toLowerCase().startsWith(start)).toList();
            case 2:
                return List.of("[integer]");
            case 3:
                return plugin.filter(Bukkit.getOnlinePlayers().stream().map((player) -> player.getName()).toList(), args[2]);
            default:
                return null;
        }
    }
}
