package me.stephenminer.redvsblue.commands;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.stephenminer.redvsblue.arena.ArenaConfig;
import me.stephenminer.redvsblue.util.ArenaConfigUtil;

public class ArenaCommandTreeHandler extends CommandTreeHandler {
    @SuppressWarnings("unchecked")
    public ArenaCommandTreeHandler(Map<String, ArenaHandledCommand> subActions) {
        super(Map.ofEntries((Map.Entry<String,HandledCommand>[]) subActions.entrySet().stream().map((e) -> Map.entry(e.getKey(), (HandledCommand) e.getValue())).toArray((n) -> new Map.Entry[n])));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length <= 1) return List.copyOf(ArenaConfigUtil.idsOnFileShallow());
        if (args.length == 2) return List.copyOf(subCommands.keySet());
        var oa = ArenaConfigUtil.findOnFileShallow(args[0]);
        if (oa == null) return null;
        var sub = (ArenaHandledCommand) subCommands.get(args[1]);
        if (sub == null) return null;
        if (sub instanceof CommandTreeHandler t)
            return t.onTabComplete(sender, command, args[0], Arrays.copyOfRange(args, 1, args.length));
        if (sender instanceof Player && sub.permission() != null && !sender.hasPermission(sub.permission()))
            return null; // They don't have the permission to run the command
        var inprog = args[args.length - 1].toLowerCase();
        var opts = sub.getOptions(oa, args.length - 2); // subtract 2, because the first arg is this command's name
        return opts == null ? null : opts 
            .stream().filter((o) -> ChatColor.stripColor(o).toLowerCase().startsWith(inprog)).toList();
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Invalid Arguments!");
            return false;
        }
        var oa = ArenaConfigUtil.findOnFileShallow(args[0]);
        if (oa == null) {
            sender.sendMessage(ChatColor.RED + "Arena '" + args[0] + "' does not exist!");
            return false;
        }
        var sub = (ArenaHandledCommand) subCommands.get(args[1]);
        assert sub != null; // getOptions validation should prevent this
        if (sender instanceof Player && sub.permission() != null && !sender.hasPermission(sub.permission())) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
            return false;
        }
        if (!(sender instanceof Player) && sub.playerOnly()) {
            sender.sendMessage("This command must be run by a player!");
            return false;
        }
        var subArgs = Arrays.copyOfRange(args, 2, args.length);
        if (!(sub instanceof CommandTreeHandler))
            for (int i = 0; i < subArgs.length; i++) {
                var opts = sub.getOptions(oa, i);
                if (opts != null && !opts.contains(subArgs[i])) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments!");
                    return false;
                }
            }
        if (!sub.execute(sender, oa, subArgs)) {
            return false;
        }
        ArenaConfigUtil.saveToFileShallow(oa);
        return true;
    }

    public interface ArenaHandledCommand extends HandledCommand {
        @Override
        default boolean execute(CommandSender sender, String[] args) {
            throw new UnsupportedOperationException("This command must be run from an ArenaCommandTreeHandler");
        }

        boolean execute(CommandSender sender, ArenaConfig arena, String[] args);

        @Override
        default Collection<String> getOptions(int argPos) {
            throw new UnsupportedOperationException("This command must be run from an ArenaCommandTreeHandler");
        }

        Collection<String> getOptions(ArenaConfig arena, int argPos);
    }
}