package me.stephenminer.redvblue.arena;

import java.util.HashSet;
import java.util.Set;

import me.stephenminer.redvblue.arena.chests.NewLootChest;
import me.stephenminer.redvblue.util.ArenaConfigUtil;

@Deprecated
public class LegacyArenaConfig {
    public static Set<NewLootChest> loadLootChests(String id) {
        String path = "arenas." + id + ".loot-chests";
        Set<NewLootChest> chests = new HashSet<>();
        if (!ArenaConfigUtil.accessorThatShouldBeRemovedButIHaventYet().getConfig().contains(path)) return chests;
        for (String entry : ArenaConfigUtil.accessorThatShouldBeRemovedButIHaventYet().getConfig().getStringList(path)){
            NewLootChest lootChest = new NewLootChest(entry);
            chests.add(lootChest);
        }
        return chests;
    }
}