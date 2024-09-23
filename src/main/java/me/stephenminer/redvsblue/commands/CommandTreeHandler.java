package me.stephenminer.redvsblue.commands;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class CommandTreeHandler implements TabExecutor, HandledCommand {
    protected final Map<String, HandledCommand> subCommands;
    public CommandTreeHandler(Map<String, HandledCommand> subCommands) {
        this.subCommands = subCommands;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length <= 1) return List.copyOf(subCommands.keySet());
        var sub = subCommands.get(args[0]);
        if (sub == null) return null;
        if (sub instanceof CommandTreeHandler t)
            return t.onTabComplete(sender, command, args[0], Arrays.copyOfRange(args, 1, args.length));
        if (sender instanceof Player && sub.permission() != null && !sender.hasPermission(sub.permission()))
            return null; // They don't have the permission to run the command
        var inprog = args[args.length - 1].toLowerCase();
        var opts = sub.getOptions(args.length - 2); // subtract 2, because the first arg is this command's name
        return opts == null ? null : opts
            .stream().filter((o) -> ChatColor.stripColor(o).toLowerCase().startsWith(inprog)).toList();
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            if (!subCommands.containsKey("help")) {
                sender.sendMessage(ChatColor.RED + "Invalid arguments!");
                return false;
            }
            return subCommands.get("help").execute(sender, args);
        }
        var sub = subCommands.get(args[0]);
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
        if (!(sub instanceof CommandTreeHandler))
            for (int i = 0; i < subArgs.length; i++) {
                var opts = sub.getOptions(i);
                if (opts != null && !opts.contains(subArgs[i])) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments!");
                    return false;
                }
            }
        return sub.execute(sender, subArgs);
    }

    @Override
    public Collection<String> getOptions(int argPos) {
        throw new UnsupportedOperationException("CommandTreeHandlers cannot operate with getOptions.");
    }
}
