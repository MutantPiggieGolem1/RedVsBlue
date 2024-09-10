package me.stephenminer.redvsblue.events.items;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import org.bukkit.util.BlockVector;

import me.stephenminer.redvsblue.CustomItems;
import me.stephenminer.redvsblue.arena.ArenaConfig;
import me.stephenminer.redvsblue.util.ArenaConfigUtil;
import me.stephenminer.redvsblue.util.BlockRange;

public class SetupWandsUse implements Listener {
    private final Map<UUID, RangeBuilder> aToCreate;
    private final Map<UUID, WallRangeBuilder> wToCreate;
    private final Map<UUID, WallIdentifier> wToDelete;

    public SetupWandsUse() {
        aToCreate = new HashMap<>();
        wToCreate = new HashMap<>();
        wToDelete = new HashMap<>();
    }
    
    // Arena Creation
    @EventHandler
    public void arenaWandLocs(PlayerInteractEvent event){
        if (!event.hasItem() || event.getAction() == Action.PHYSICAL) return;
        Player player = event.getPlayer();
        if (!CustomItems.ARENAWAND.is(event.getItem())) return;
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
        } else if (msg.equals("cancel")) {
            player.sendMessage(ChatColor.RED + "Cancelling creation");
        } else {
            String id = msg.trim().replaceAll("[\\s:]+", "_");
            if (ArenaConfigUtil.existsOnFileDeep(id)) {
                player.sendMessage(ChatColor.RED + "That arena already exists, try again.");
                return;
            }

            ArenaConfig overlap = ArenaConfigUtil.findOnFileDeep(inprog.aLoc(), inprog.bLoc());
            if (overlap != null) {
                player.sendMessage(ChatColor.RED + "Area intersects with other arena '" + overlap.id() + "', try again.");
                return;
            }
            
            ArenaConfig created = ArenaConfig.builder(id, inprog.toRange());
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
        if (!CustomItems.WALLWAND.is(event.getItem())) return;
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
    
                arena = ArenaConfigUtil.findOnFileDeep(loc);
                if (arena == null) {
                    player.sendMessage(ChatColor.RED + "There is no arena here!");
                    return;
                } else if (inprog.arena.isPresent() && arena != inprog.arena.get()) {
                    player.sendMessage(ChatColor.RED + "This is a different arena!");
                    return;
                }
                
                inprog.arena = Optional.of(arena);
                inprog.b = new BlockVector(loc.toVector());
                player.sendMessage(ChatColor.GREEN + "Position 2 set!");
                break;
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                loc = action == Action.LEFT_CLICK_AIR ? 
                    player.getLocation().getBlock().getLocation() :
                    event.getClickedBlock().getLocation();

                arena = ArenaConfigUtil.findOnFileDeep(loc);
                if (arena == null) {
                    player.sendMessage(ChatColor.RED + "There is no arena here!");
                    return;
                } else if (inprog.arena.isPresent() && arena != inprog.arena.get()) {
                    player.sendMessage(ChatColor.RED + "This is a different arena!");
                    return;
                }

                inprog.arena = Optional.of(arena);
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
        event.setCancelled(true);
        String msg = ChatColor.stripColor(event.getMessage()).toUpperCase();
        if (msg.equalsIgnoreCase("cancel")){
            player.sendMessage(ChatColor.RED + "Cancelling creation");
        } else {
            Material mat = Material.matchMaterial(msg);
            if (mat == null) {
                player.sendMessage(ChatColor.YELLOW + "Invalid Material, try again.");
                return;
            }
            try {
                if (inprog.arena.get().createWall(mat, inprog.toRange())) { // FIXME doesnt work async
                    ArenaConfigUtil.saveToFileShallow(inprog.arena.get());
                    player.sendMessage(ChatColor.GREEN + "Wall created and added to arena '" + inprog.arena.get().id() + "'!");
                } else {
                    player.sendMessage(ChatColor.RED + "Wall intersects with another in arena '" + inprog.arena.get().id() + "'!");
                    return;
                }
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Wall must be inside arena '" + inprog.arena.get().id() + "'!");
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
        if (
            !CustomItems.WALLREMOVER.is(player.getInventory().getItemInMainHand()) &&
            !CustomItems.WALLREMOVER.is(player.getInventory().getItemInOffHand())
        ) return;
        event.setCancelled(true);

        var arena = ArenaConfigUtil.findOnFileDeep(event.getBlock().getLocation());
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "There is no arena here!");
            return;
        }
    
        var wall = arena.findWall(event.getBlock().getLocation());
        if (!wall.isPresent()){
            player.sendMessage(ChatColor.RED + "There is no wall here!");
            return;
        }
        
        wToDelete.put(player.getUniqueId(), new WallIdentifier(arena, wall.get()));
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
                ArenaConfigUtil.saveToFileShallow(pair.arena);
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

        public @Nullable Location aLoc() {
            return a == null ? null : a.toLocation(world);
        }

        public @Nullable Location bLoc() {
            return b == null ? null : b.toLocation(world);
        }
    }
    private record WallIdentifier(ArenaConfig arena, BlockRange wall) {}
    private class WallRangeBuilder extends RangeBuilder {
        public @Nonnull Optional<ArenaConfig> arena;

        public WallRangeBuilder(World w) {
            super(w);
            arena = Optional.empty();
        }

        public boolean isDone() {
            return arena.isPresent() && super.isDone();
        }
    }
}
