package me.stephenminer.redvblue.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class CommandTreeHandler implements TabExecutor {
    private final Map<String, HandledCommand> subCommands;
    public CommandTreeHandler(Map<String, HandledCommand> subCommands) {
        this.subCommands = subCommands;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length <= 1) return List.copyOf(subCommands.keySet());
        var sub = subCommands.get(args[0]);
        if (sub == null) return null;
        if (sender instanceof Player && sub.permission() != null && !sender.hasPermission(sub.permission()))
            return null; // They don't have the permission to run the command
        var inprog = args[args.length - 1].toLowerCase();
        var opts = sub.getOptions(args.length - 2);
        return opts == null ? null : opts // subtract 2, because the first arg is this command's name
            .stream().filter((o) -> ChatColor.stripColor(o).toLowerCase().startsWith(inprog)).toList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var sub = args.length == 0 ? subCommands.get("help") : subCommands.get(args[0]);
        if (sub == null) {
            sender.sendMessage(ChatColor.RED + "Invalid arguments!");
            return false;
        }
        if (sender instanceof Player && sub.permission() != null && !sender.hasPermission(sub.permission())) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
            return false;
        }
        if (!(sender instanceof Player) && sub.playerOnly()) {
            sender.sendMessage("This command must be run by a player!");
            return false;
        }
        var subArgs = Arrays.copyOfRange(args, 1, args.length);
        for (int i = 0; i < subArgs.length; i++) {
            var opts = sub.getOptions(i);
            if (opts != null && !opts.contains(subArgs[i])) {
                sender.sendMessage(ChatColor.RED + "Invalid arguments!");
                return false;
            }
        }
        return sub.execute(sender, subArgs);
    }

    public interface HandledCommand {
        public boolean playerOnly();
        public default String permission() {return null;}
        public boolean execute(CommandSender sender, String[] args);
        public List<String> getOptions(int argPos);
    }
}
