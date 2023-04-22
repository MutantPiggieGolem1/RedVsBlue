package me.stephenminer.redvblue.arena;

import me.stephenminer.redvblue.RedBlue;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

//NEEDS LUCKPERMS
public class TeamChecker {
    private final RedBlue plugin;
    public TeamChecker(RedBlue plugin){
        this.plugin = plugin;
    }


    public boolean hasPrefix(Player player, String prefix){
        User user = plugin.luckPerms.getUserManager().getUser(player.getUniqueId());
        String check = user.getCachedData().getMetaData().getPrefix();
        return prefix.equals(check);
    }
}