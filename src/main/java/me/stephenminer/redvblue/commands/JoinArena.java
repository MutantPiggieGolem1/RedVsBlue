package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.ArenaGui;
import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Arena;
import me.stephenminer.redvblue.arena.ArenaBuilder;
import me.stephenminer.redvblue.events.ArenaGuiEvents;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class JoinArena implements CommandExecutor, TabCompleter {
    private final RedBlue plugin;
    public JoinArena(RedBlue plugin){
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            if (inArena(player)){
                player.sendMessage(ChatColor.RED + "You cannot use this command while inside an arena!",
                        ChatColor.RED + "Please use /leaveRvB first!");
                return false;
            }
            int size = args.length;
            if (size < 1){
                ArenaGui gui = new ArenaGui(plugin);
                gui.display(player);
                ArenaGuiEvents.IN_GUI.put(player.getUniqueId(),gui);
            }else{
                String id = ChatColor.stripColor(args[0]);
                Arena arena = findTarget(id);
                if (arena == null) {
                    player.sendMessage(ChatColor.RED + "No arena with id " + id + " exists!");
                    return false;
                }
                arena.addPlayer(player);

            }
        }else sender.sendMessage(ChatColor.RED + "You need to be a player to use this command!");
        return false;
    }

    private boolean inArena(Player player){
        for (Arena arena : Arena.arenas){
            if (arena.hasPlayer( player)) return true;
        }
        return false;
    }

    private Arena findRandom(){
        if (Arena.arenas.isEmpty()){
            Set<String> fileEntries = plugin.arenas.getConfig().getConfigurationSection("arenas").getKeys(false);
            int target = ThreadLocalRandom.current().nextInt(fileEntries.size());
            int i = 0;
            String element = null;
            for (String entry : fileEntries){
                if (i == target)  element = entry;
            }
            ArenaBuilder builder = new ArenaBuilder(element, plugin);
            builder.loadData();
            builder.loadWalls();
            return builder.build();
        }else{
            Arena max = Arena.arenas.get(0);
            for (int i = Arena.arenas.size()-1; i>=0; i--){
                Arena arena = Arena.arenas.get(i);
                if (max.size() < arena.size()) max = arena;
            }
            return max;
        }
    }

    private Arena findTarget(String id){
        for (Arena arena : Arena.arenas){
            if (arena.getId().equals(id)) return arena;
        }
        if (plugin.arenas.getConfig().contains("arenas." + id)){
            ArenaBuilder builder = new ArenaBuilder(id, plugin);
            builder.loadWalls();
            builder.loadData();
            return builder.build();
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size == 1) return arenaIds(args[0]);
        return null;
    }

    private List<String> arenaIds(String match){
        Set<String> onFile = plugin.arenas.getConfig().getConfigurationSection("arenas").getKeys(false);
        return plugin.filter(onFile, match);
    }
}
