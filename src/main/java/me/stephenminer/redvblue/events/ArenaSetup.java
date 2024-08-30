package me.stephenminer.redvblue.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import me.stephenminer.redvblue.BlockRange;
import me.stephenminer.redvblue.CustomItems;
import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Wall;

public class ArenaSetup implements Listener {
    private final HashMap<UUID, Location> loc1s;
    private final HashMap<UUID, Location> loc2s;
    private final List<UUID> canName;

    private final HashMap<UUID, InProgRange> wToCreate;
    private final HashMap<UUID, SPair> wToDelete;

    private final RedBlue plugin;
    public ArenaSetup(RedBlue plugin){
        loc1s = new HashMap<>();
        loc2s = new HashMap<>();
        canName = new ArrayList<>();
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
        if (CustomItems.ARENAWAND.is(item)){
            Action action = event.getAction();
            UUID uuid = player.getUniqueId();
            event.setCancelled(true);
            switch (action){
                case RIGHT_CLICK_AIR -> {
                    loc2s.put(uuid, player.getLocation().getBlock().getLocation());
                    player.sendMessage(ChatColor.GREEN + "Position 2 set!");
                }
                case RIGHT_CLICK_BLOCK -> {
                    loc2s.put(uuid, event.getClickedBlock().getLocation());
                    player.sendMessage(ChatColor.GREEN + "Position 2 set!");
                }
                case LEFT_CLICK_AIR -> {
                    loc1s.put(uuid, player.getLocation().getBlock().getLocation());
                    player.sendMessage(ChatColor.GREEN + "Position 1 set!");
                }
                case LEFT_CLICK_BLOCK -> {
                    loc1s.put(uuid, event.getClickedBlock().getLocation());
                    player.sendMessage(ChatColor.GREEN + "Position 1 set!");
                }
                default -> {return;}
            }
            if (loc1s.containsKey(uuid) && loc2s.containsKey(uuid)){
                canName.add(uuid);
                player.sendMessage(ChatColor.GOLD + "Please type the name of your arena in chat!");
            }
        }
    }
    @EventHandler
    public void nameArena(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (canName.contains(uuid)){
            event.setCancelled(true);

            String msg = ChatColor.stripColor(event.getMessage()).replace(' ', '_');
            if (msg.contains("cancel")){
                canName.remove(uuid);
                loc1s.remove(uuid);
                loc2s.remove(uuid);
                player.sendMessage(ChatColor.RED + "Cancelling creation...");
                return;
            }
            if (arenaExists(msg)){
                loc1s.remove(uuid);
                loc2s.remove(uuid);
                canName.remove(uuid);
                player.sendMessage(ChatColor.RED + "An arena with this id (" + msg + ") already exists!");
            }
            else {
                createArena(msg, BlockRange.fromLocations(loc1s.get(uuid), loc2s.get(uuid)));
                loc1s.remove(uuid);
                loc2s.remove(uuid);
                canName.remove(uuid);
                player.sendMessage(ChatColor.GREEN + "Created a new arena " + msg + "!");
            }
        }
    }
    private void createArena(String id, BlockRange range){
        String path = "arenas." + id;
        plugin.arenas.getConfig().set(path + ".range", range); // FIXME centralize
        plugin.arenas.saveConfig();
    }
    // ===

    // Wall Creation
    @EventHandler
    public void createWallLocs(PlayerInteractEvent event){
        if (!event.hasItem() || event.getAction() == Action.PHYSICAL) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (CustomItems.WALLWAND.is(item)) {
            Action action = event.getAction();
            UUID uuid = player.getUniqueId();
            event.setCancelled(true);
            
            if (!wToCreate.containsKey(uuid)) wToCreate.put(uuid, new InProgRange(player.getWorld()));
            InProgRange inprog = wToCreate.get(uuid);
            Location loc;
            String sArena;
            switch (action){
                case RIGHT_CLICK_AIR:
                case RIGHT_CLICK_BLOCK:
                    loc = action == Action.RIGHT_CLICK_AIR ? 
                        player.getLocation().getBlock().getLocation() :
                        event.getClickedBlock().getLocation();
        
                    sArena = findArena(loc);
                    if (sArena == null) {
                        player.sendMessage(ChatColor.RED + "There is no arena here!");
                        return;
                    } else if (inprog.sArena != null && sArena != inprog.sArena) {
                        player.sendMessage(ChatColor.RED + "This is a different arena!");
                        return;
                    }
                    
                    inprog.sArena = sArena;
                    inprog.b = new BlockVector(loc.toVector());
                    player.sendMessage(ChatColor.GREEN + "Position 2 set!");
                    break;
                case LEFT_CLICK_AIR:
                case LEFT_CLICK_BLOCK:
                    loc = action == Action.LEFT_CLICK_AIR ? 
                        player.getLocation().getBlock().getLocation() :
                        event.getClickedBlock().getLocation();

                    sArena = findArena(loc);
                    if (sArena == null) {
                        player.sendMessage(ChatColor.RED + "There is no arena here!");
                        return;
                    } else if (inprog.sArena != null && sArena != inprog.sArena) {
                        player.sendMessage(ChatColor.RED + "This is a different arena!");
                        return;
                    }

                    inprog.sArena = sArena;
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
    }
    @EventHandler
    public void createWallMsg(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!wToCreate.containsKey(uuid)) return;
        InProgRange inprog = wToCreate.get(uuid);
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
            createWall(inprog.sArena, mat, inprog.toRange());
            player.sendMessage(ChatColor.GREEN + "Creating your wall and adding it to the arena!");
        }
        wToCreate.remove(uuid);
    }
    private void createWall(String sArena, Material material, BlockRange range){
        String path = "arenas." + sArena + ".walls";
        Wall wall = new Wall(material, range);
        List<String> walls = plugin.arenas.getConfig().getStringList(path);
        walls.add(wall.toString());
        plugin.arenas.getConfig().set(path, walls);
        plugin.arenas.saveConfig();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, wall::buildWall, 1);
    }
    // ===

    // Wall Deletion
    @EventHandler
    public void deleteWallLocs(BlockBreakEvent event){
        Player player = event.getPlayer();
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (CustomItems.WALLREMOVER.is(item)){
            event.setCancelled(true);
            var sArena = findArena(event.getBlock().getLocation());
            if (sArena == null) {
                player.sendMessage(ChatColor.RED + "There is no arena here!");
                return;
            }
        
            String sWall = findWall(sArena, event.getBlock());
            if (sWall == null){
                player.sendMessage(ChatColor.RED + "There is no wall here!");
                return;
            }
            wToDelete.put(player.getUniqueId(), new SPair(sArena, sWall));
            player.sendMessage(ChatColor.GREEN + "Type confirm in chat to confirm deletion");
        }
    }
    @EventHandler
    public void deleteWallMsg(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!wToDelete.containsKey(uuid)) return;
        String msg = ChatColor.stripColor(event.getMessage()).toLowerCase();
        if (msg.equalsIgnoreCase("confirm")){
            SPair pair = wToDelete.get(uuid);
            deleteWall(pair.s1(),pair.s2());
            player.sendMessage(ChatColor.GREEN + "Removing your wall from the arena!");
        } else {
            player.sendMessage(ChatColor.RED + "Cancelling deletion");
        }
        wToDelete.remove(uuid);
    }
    private void deleteWall(String id, String sWall){
        String path = "arenas." + id + ".walls";
        List<String> walls = plugin.arenas.getConfig().getStringList(path);
        walls.remove(sWall);
        plugin.arenas.getConfig().set(path, walls);
        plugin.arenas.saveConfig();
        Wall wall = Wall.fromString(plugin.getServer(), sWall);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, wall::destroyWall, 1);
    }
    // ===

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {clear(event.getPlayer().getUniqueId());}
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {clear(event.getPlayer().getUniqueId());}
    private void clear(UUID uuid) {
        loc1s.remove(uuid);
        loc2s.remove(uuid);
        wToCreate.remove(uuid);
        wToDelete.remove(uuid);
        canName.remove(uuid);
    }

    private boolean arenaExists(String id) {
        return plugin.arenas.getConfig().contains("arenas." + id);
    }

    private @Nullable String findArena(Location loc) {
        var allArenas = plugin.arenas.getConfig().getConfigurationSection("arenas").getKeys(false);
        for (String arenaKey : allArenas) {
            String sBounds = plugin.arenas.getConfig().getString("arenas." + arenaKey + ".bounds");
            var bounds = BlockRange.fromString(plugin.getServer(), sBounds);
            if (bounds.contains(loc)) return arenaKey;
        }
        return null;
    }

    private @Nullable String findWall(String arenaKey, Block block) {
        String path = "arenas." + arenaKey + ".walls";
        List<String> walls = plugin.arenas.getConfig().getStringList(path);
        for (String sWall : walls){
            Wall wall = Wall.fromString(plugin.getServer(), sWall);
            if (wall.isOnWall(block.getLocation())) return sWall;
        }
        return null;
    }

    private record SPair(String s1, String s2){}
    private class InProgRange {
        public World world;
        public @Nullable String sArena;
        public @Nullable BlockVector a, b;

        public InProgRange(World world) {
            this.world = world;
        }

        public boolean isDone() {
            return sArena != null && a != null && b != null && world != null;
        }

        public BlockRange toRange() {
            return new BlockRange(world, a, b);
        }
    }
}
