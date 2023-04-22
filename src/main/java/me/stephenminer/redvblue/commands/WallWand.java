package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.Items;
import me.stephenminer.redvblue.RedBlue;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class WallWand implements CommandExecutor, TabCompleter {
    private final RedBlue plugin;
    public WallWand(RedBlue plugin){
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (args.length < 1){
            sender.sendMessage(ChatColor.RED + "You need to include an arena id for the wall to be apart of!");
            return false;
        }else if (!validId(args[0])){
            sender.sendMessage(ChatColor.RED + args[0] + " is not a valid arena id!");
            return false;
        }
        if (sender instanceof Player player){
            if (!player.hasPermission("rvb.commands.wand")){
                player.sendMessage(ChatColor.RED + "ydhptutc");
                return false;
            }
            Items items = new Items();
            World world = player.getWorld();
            HashMap<Integer, ItemStack> toDrop = player.getInventory().addItem(items.wallWand(args[0]));
            for (int num : toDrop.keySet()){
                world.dropItemNaturally(player.getLocation(),toDrop.get(num));
            }
            player.sendMessage(ChatColor.GREEN + "You have recieved your item!");
            return true;
        }else sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
        return false;
    }

    private boolean validId(String id){
        return plugin.arenas.getConfig().contains("arenas." + id);
    }



    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        if (args.length == 1) return arenaIds(args[0]);
        return null;
    }


    private List<String> arenaIds(String match){
        Set<String> fileEntries = plugin.arenas.getConfig().getConfigurationSection("arenas").getKeys(false);
        return plugin.filter(fileEntries, match);
    }
}
