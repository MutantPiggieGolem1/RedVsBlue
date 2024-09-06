package me.stephenminer.redvsblue.commands.impl;

import java.util.Collection;
import java.util.stream.IntStream;

import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import me.stephenminer.redvsblue.arena.ArenaConfig;
import me.stephenminer.redvsblue.commands.ArenaCommandTreeHandler.ArenaHandledCommand;

public class ArenaLoot implements ArenaHandledCommand {

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, ArenaConfig arena, String[] args) {
        if (args.length == 0) {
            String out = "";
            int i = 0;
            for (var lc : arena.getLootCaches().entrySet()) {
                out += " "+ ++i +". ("+lc.getKey().toString()+") "+lc.getValue();
            }
            sender.sendMessage(out);
            return true;
        } else if (args[0].equals("delete") && args.length >= 2) {
            int i;
            try {
                i = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "'"+args[1]+"' is not a number!");
                return false;
            }
            var key = arena.getLootCaches().keySet().stream().skip(i).findFirst();
            if (!key.isPresent()) {
                sender.sendMessage(ChatColor.RED + "Loot Cache #"+i+" was not found.");
                return false;
            }
            if (!arena.deleteLootCache(key.get())) {
                sender.sendMessage(ChatColor.YELLOW + "There was no loot cache #"+i+"!"); // CATASTROPHIC ERROR HOW DID THIS HAPPEN
                return false;
            }
            sender.sendMessage(ChatColor.GREEN + "Success! Unlinked a " + key.get().getBlock().getType().toString() + ".");
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid Arguments!");
            return false;
        }
    }

    @Override
    public Collection<String> getOptions(ArenaConfig arena, int argPos) {
        if (argPos == 0) return Set.of("delete");
        if (argPos == 1) return IntStream.range(0, arena.getLootCaches().size()-1).mapToObj((i) -> ""+i).toList();
        return null;
    }
    
}
