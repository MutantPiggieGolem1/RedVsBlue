package me.stephenminer.redvblue.events.items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;

import me.stephenminer.redvblue.CustomItems;
import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.ArenaConfig;
import me.stephenminer.redvblue.util.BlockRange;

public class SetupWandsUse implements Listener {
    private final Map<UUID, RangeBuilder> aToCreate;
    private final Map<UUID, WallRangeBuilder> wToCreate;
    private final Map<UUID, WallIdentifier> wToDelete;

    private final RedBlue plugin;
    public SetupWandsUse(RedBlue plugin){
        aToCreate = new HashMap<>();
        wToCreate = new HashMap<>();
        wToDelete = new HashMap<>();
        this.plugin = plugin;
    }
    
    // Arena Creation
    @EventHandler
    public void arenaWandLocs(PlayerInteractEvent event){
        if (!event.hasItem() || event.getAction() == Action.PHYSICAL) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!CustomItems.ARENAWAND.is(item)) return;
        Action action = event.getAction();
        UUID uuid = player.getUniqueId();
        event.setCancelled(true);

        if (!aToCreate.containsKey(uuid)) aToCreate.put(uuid, new RangeBuilder(player.getWorld()));
        RangeBuilder inprog = aToCreate.get(uuid);
        Location loc = action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_AIR ? 
            player.getLocation().getBlock().getLocation() :
            event.getClickedBlock().getLocation();
        switch (action){
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                inprog.a = new BlockVector(loc.toVector());
                player.sendMessage(ChatColor.GREEN + "Position 1 set!");
                break;
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                inprog.b = new BlockVector(loc.toVector());
                player.sendMessage(ChatColor.GREEN + "Position 2 set!");
                break;
            default:
                return;
        }
        if (inprog.isDone()) {
            player.sendMessage(ChatColor.GOLD + "Please type your new arena's id, or 'cancel'!");
        }
    }
    @EventHandler
    public void createArena(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!aToCreate.containsKey(uuid)) return;
        RangeBuilder inprog = aToCreate.get(uuid);
        if (!inprog.isDone()) return;
        event.setCancelled(true);
        String msg = ChatColor.stripColor(event.getMessage()).toLowerCase();
        if (msg.isBlank()) {
            player.sendMessage(ChatColor.RED + "Invalid response, try again.");
            return;
        } else if (msg == "cancel") {
            player.sendMessage(ChatColor.RED + "Cancelling creation");
        } else {
            String id = msg.trim().replaceAll("[\\s:]+", "_");
            if (arenaExistsDeep(id)) {
                player.sendMessage(ChatColor.RED + "That arena already exists, try again.");
                return;
            }

            ArenaConfig overlap = findArena(inprog.aLoc(), inprog.bLoc());
            if (overlap != null) {
                player.sendMessage(ChatColor.RED + "Area intersects with other arena '" + overlap.id() + "', try again.");
                return;
            }
            
            ArenaConfig created = ArenaConfig.builder(id, inprog.toRange(), plugin.arenas);
            if (created == null) {
                player.sendMessage(ChatColor.RED + "if you're seeing this, something has gone terribly terribly wrong. tell your admins to contact the devs. try again.");
                return;
            }

            player.sendMessage(ChatColor.GREEN + "Arena '" + created.id() + "' created and saved!");
        }
        aToCreate.remove(uuid);
    }
    // ===

    // Wall Creation
    @EventHandler
    public void createWallLocs(PlayerInteractEvent event){
        if (!event.hasItem() || event.getAction() == Action.PHYSICAL) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!CustomItems.WALLWAND.is(item)) return;
        Action action = event.getAction();
        UUID uuid = player.getUniqueId();
        event.setCancelled(true);
        
        if (!wToCreate.containsKey(uuid)) wToCreate.put(uuid, new WallRangeBuilder(player.getWorld()));
        WallRangeBuilder inprog = wToCreate.get(uuid);
        Location loc;
        ArenaConfig arena;
        switch (action){
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                loc = action == Action.RIGHT_CLICK_AIR ? 
                    player.getLocation().getBlock().getLocation() :
                    event.getClickedBlock().getLocation();
    
                arena = findArena(loc);
                if (arena == null) {
                    player.sendMessage(ChatColor.RED + "There is no arena here!");
                    return;
                } else if (inprog.arena != null && arena != inprog.arena) {
                    player.sendMessage(ChatColor.RED + "This is a different arena!");
                    return;
                }
                
                inprog.arena = arena;
                inprog.b = new BlockVector(loc.toVector());
                player.sendMessage(ChatColor.GREEN + "Position 2 set!");
                break;
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                loc = action == Action.LEFT_CLICK_AIR ? 
                    player.getLocation().getBlock().getLocation() :
                    event.getClickedBlock().getLocation();

                arena = findArena(loc);
                if (arena == null) {
                    player.sendMessage(ChatColor.RED + "There is no arena here!");
                    return;
                } else if (inprog.arena != null && arena != inprog.arena) {
                    player.sendMessage(ChatColor.RED + "This is a different arena!");
                    return;
                }

                inprog.arena = arena;
                inprog.a = new BlockVector(loc.toVector());
                player.sendMessage(ChatColor.GREEN + "Position 1 set!");
                break;
            default:
                return;
        }
        if (inprog.isDone()) {
            player.sendMessage(ChatColor.GOLD + "Please type your wall's material, or 'cancel'!");
        }
    }
    @EventHandler
    public void createWall(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!wToCreate.containsKey(uuid)) return;
        WallRangeBuilder inprog = wToCreate.get(uuid);
        if (!inprog.isDone()) return;
        String msg = ChatColor.stripColor(event.getMessage()).toUpperCase();
        if (msg.equalsIgnoreCase("cancel")){
            player.sendMessage(ChatColor.RED + "Cancelling creation");
        } else {
            Material mat = Material.matchMaterial(msg);
            if (mat == null) {
                player.sendMessage(ChatColor.YELLOW + "Invalid Material, try again.");
                return;
            }
            if (inprog.arena.createWall(mat, inprog.toRange())) {
                player.sendMessage(ChatColor.GREEN + "Wall created and added to arena '" + inprog.arena.id() + "'!");
            } else {
                player.sendMessage(ChatColor.RED + "Wall intersects with another in arena '" + inprog.arena.id() + "'!");
                return;
            }
        }
        wToCreate.remove(uuid);
    }
    // ===

    // Wall Deletion
    @EventHandler
    public void deleteWallLocs(BlockBreakEvent event){
        Player player = event.getPlayer();
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!CustomItems.WALLREMOVER.is(item)) return;
        event.setCancelled(true);

        var arena = findArena(event.getBlock().getLocation());
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "There is no arena here!");
            return;
        }
    
        var wall = arena.findWall(event.getBlock().getLocation());
        if (!wall.isPresent()){
            player.sendMessage(ChatColor.RED + "There is no wall here!");
            return;
        }
        
        wToDelete.put(player.getUniqueId(), new WallIdentifier(arena, wall.orElseThrow()));
        player.sendMessage(ChatColor.GREEN + "Type confirm in chat to confirm deletion");
    }
    @EventHandler
    public void deleteWall(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!wToDelete.containsKey(uuid)) return;
        String msg = ChatColor.stripColor(event.getMessage()).toLowerCase();
        if (msg.equalsIgnoreCase("confirm")){
            var pair = wToDelete.get(uuid);
            if (pair.arena().destroyWall(pair.wall())) { 
                player.sendMessage(ChatColor.GREEN + "Wall deleted and removed from arena '" + pair.arena().id() + "'!");
            } else {
                player.sendMessage(ChatColor.RED + "Wall did not exist in arena '" + pair.arena().id() + "'!");
                return;
            }
        } else {
            player.sendMessage(ChatColor.RED + "Cancelling deletion");
        }
        wToDelete.remove(uuid);
    }
    // ===

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {clear(event.getPlayer().getUniqueId());}
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {clear(event.getPlayer().getUniqueId());}
    private void clear(UUID uuid) {
        aToCreate.remove(uuid);
        wToCreate.remove(uuid);
        wToDelete.remove(uuid);
    }

    private boolean arenaExistsDeep(String id) { // Non-naive, does not expect arena key to correspond to arena id
        return plugin.arenas.getConfig().getConfigurationSection("arenas")
            .getValues(false).values().stream().anyMatch((a) -> ((ArenaConfig) a).id() == id);
    }

    private @Nullable ArenaConfig findArena(Location... loc) {
        var configSection = plugin.arenas.getConfig().getConfigurationSection("arenas");
        for (var arenaEntry : configSection.getValues(false).entrySet()) {
            var arenaConfig = (ArenaConfig) arenaEntry.getValue();
            for (Location l : loc)
                if (arenaConfig.contains(l)) return arenaConfig;
        }
        return null;
    }

    private class RangeBuilder {
        public final @Nonnull World world;
        public @Nullable BlockVector a, b;

        public RangeBuilder(World w) {
            this.world = w;
        }

        public boolean isDone() {
            return a != null && b != null;
        }

        public BlockRange toRange() {
            return new BlockRange(world, a, b);
        }

        public Location aLoc() {
            return a.toLocation(world);
        }

        public Location bLoc() {
            return b.toLocation(world);
        }
    }
    private record WallIdentifier(ArenaConfig arena, BlockRange wall) {}
    private class WallRangeBuilder extends RangeBuilder {
        public @Nullable ArenaConfig arena;

        public WallRangeBuilder(World w) {
            super(w);
        }

        public boolean isDone() {
            return arena != null && super.isDone();
        }
    }
}
