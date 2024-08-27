package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.Items;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ArenaWand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            if (!player.hasPermission("rvb.commands.wand")){
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command");
                return false;
            }
            Items items = new Items();
            World world = player.getWorld();
            HashMap<Integer, ItemStack> drops = player.getInventory().addItem(items.arenaWand());
            for (int num : drops.keySet()){
                world.dropItemNaturally(player.getLocation(), drops.get(num));
            }
            player.sendMessage(ChatColor.GREEN + "You have gotten your item;");
            return true;
        }else sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
        return false;
    }
}
