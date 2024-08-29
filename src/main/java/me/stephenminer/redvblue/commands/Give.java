package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.ItemTranslator;
import me.stephenminer.redvblue.RedBlue;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Give implements CommandExecutor, TabCompleter {
    private final RedBlue plugin;
    public Give(RedBlue plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            if (!player.hasPermission("rvb.commands.give")){
                player.sendMessage(ChatColor.RED + "No permission to use this command!");
                return false;
            }
            int size = args.length;
            if (size >= 1){
                ItemTranslator translator = new ItemTranslator();
                ItemStack item = translator.fromId(args[0]);
                Player reciever = player;
                int amount = 1;
                if (item == null){
                    player.sendMessage(ChatColor.RED + "RVB item with id " + args[0] + ", doesn't exist!");
                    return false;
                }
                if (size >= 2){
                    amount = Integer.parseInt(args[1]);
                    if (size >= 3){
                        reciever = Bukkit.getPlayerExact(args[1]);
                        if (reciever == null){
                            player.sendMessage(ChatColor.RED + "Player " + args[2] + " doesn't exist!");
                            return false;
                        }else{
                            player.sendMessage(ChatColor.GREEN + "You gave " + reciever.getName() + " an item!");
                        }
                    }
                }
                item.setAmount(amount);
                reciever.getInventory().addItem(item);
                reciever.sendMessage(ChatColor.GREEN + "You have been given an item!");
                return true;
            }
        }else sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
        return false;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size == 1) return itemIds(args[0]);
        if (size == 2) return List.of("[integer]");
        if (size == 3) return playerNames(args[2]);
        return null;
    }

    private List<String> itemIds(String match){
        return plugin.filter(List.of("longrifle","throwingjuice","manapowder","windscroll"), match);
    }
    private List<String> playerNames(String match){
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()){
            names.add(player.getName());
        }
        return plugin.filter(names, match);
    }
}
