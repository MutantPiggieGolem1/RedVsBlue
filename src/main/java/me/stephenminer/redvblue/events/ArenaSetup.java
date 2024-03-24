package me.stephenminer.redvblue.events;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Arena;
import me.stephenminer.redvblue.arena.Wall;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;

public class ArenaSetup implements Listener {
    private final HashMap<UUID, Location> loc1s;
    private final HashMap<UUID, Location> loc2s;

    private final HashMap<UUID, Location> wLoc1s;
    private final HashMap<UUID, Location> wLoc2s;

    private final HashMap<UUID, String> canCreate;
    private final List<UUID> canName;

    private final HashMap<UUID, SPair> canDelete;
    private final RedBlue plugin;
    public ArenaSetup(RedBlue plugin){
        loc1s = new HashMap<>();
        loc2s = new HashMap<>();
        wLoc1s = new HashMap<>();
        wLoc2s = new HashMap<>();
        canName = new ArrayList<>();
        canCreate = new HashMap<>();
        canDelete = new HashMap<>();
        this.plugin = plugin;
    }
    @EventHandler
    public void arenaWandLocs(PlayerInteractEvent event){
        if (!event.hasItem()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (plugin.checkLore(item, "arena-wand")){
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
            }
            if (loc1s.containsKey(uuid) && loc2s.containsKey(uuid)){
                canName.add(uuid);
                player.sendMessage(ChatColor.GOLD + "Please type the name of your arena in chat!");
            }
        }
    }
    @EventHandler
    public void wallWandLocs(PlayerInteractEvent event){
        if (!event.hasItem()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (plugin.checkLore(item, "wall-wand")){
            Action action = event.getAction();
            UUID uuid = player.getUniqueId();
            event.setCancelled(true);
            switch (action){
                case RIGHT_CLICK_AIR -> {
                    wLoc2s.put(uuid, player.getLocation().getBlock().getLocation());
                    player.sendMessage(ChatColor.GREEN + "Position 2 set!");
                }
                case RIGHT_CLICK_BLOCK -> {
                    wLoc2s.put(uuid, event.getClickedBlock().getLocation());
                    player.sendMessage(ChatColor.GREEN + "Position 2 set!");
                }
                case LEFT_CLICK_AIR -> {
                    wLoc1s.put(uuid, player.getLocation().getBlock().getLocation());
                    player.sendMessage(ChatColor.GREEN + "Position 1 set!");
                }
                case LEFT_CLICK_BLOCK -> {
                    wLoc1s.put(uuid, event.getClickedBlock().getLocation());
                    player.sendMessage(ChatColor.GREEN + "Position 1 set!");
                }
            }
            if (wLoc1s.containsKey(uuid) && wLoc2s.containsKey(uuid)){
                canCreate.put(uuid, parseId(item));
                player.sendMessage(ChatColor.GOLD + "Please type confirm if you are ready to create the wall!");
            }
        }
    }

    @EventHandler
    public void removeWall(BlockBreakEvent event){
        Player player = event.getPlayer();
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (plugin.checkLore(item, "wall-remover")){
            event.setCancelled(true);
            String id = parseId(item);
            for (Arena arena : Arena.arenas){
                if (arena.getId().equals(id)) {
                    player.sendMessage(ChatColor.RED + "You cannot remove the wall of an active arena");
                    return;
                }
            }
            String sWall = findWall(id, event.getBlock());
            SPair pair = new SPair(id, sWall);
            canDelete.put(player.getUniqueId(),pair);
            player.sendMessage(ChatColor.GREEN + "Type confirm in chat to confirm deletion");
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
            if (idExists(msg)){
                loc1s.remove(uuid);
                loc2s.remove(uuid);
                canName.remove(uuid);
                player.sendMessage(ChatColor.RED + "An arena with this id (" + msg + ") already exists!");
            }
            else {
                createArena(msg,loc1s.get(uuid), loc2s.get(uuid));
                loc1s.remove(uuid);
                loc2s.remove(uuid);
                canName.remove(uuid);
                player.sendMessage(ChatColor.GREEN + "Created a new arena " + msg + "!");
            }

        }
    }

    @EventHandler
    public void createWall(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (canCreate.containsKey(uuid)){
            String msg = ChatColor.stripColor(event.getMessage()).toUpperCase();
            if (msg.equalsIgnoreCase("confirm")){
                createWall(canCreate.get(uuid), msg, wLoc1s.get(uuid), wLoc2s.get(uuid));
                player.sendMessage(ChatColor.GREEN + "Creating your wall and adding it to the arena!");
                canCreate.remove(uuid);
                wLoc1s.remove(uuid);
                wLoc2s.remove(uuid);
            }else if (msg.equalsIgnoreCase("cancel")){
                canCreate.remove(uuid);
                wLoc1s.remove(uuid);
                wLoc2s.remove(uuid);
                player.sendMessage(ChatColor.RED + "Cancelling creation");
            }
        }
    }
    @EventHandler
    public void deleteWall(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (canDelete.containsKey(uuid)){
            String msg = ChatColor.stripColor(event.getMessage()).toLowerCase();
            if (msg.equalsIgnoreCase("confirm")){
                SPair pair = canDelete.get(uuid);
                deleteWall(pair.s1(),pair.s2());
                player.sendMessage(ChatColor.GREEN + "Deleting your wall and remove it from the arena!");
                canCreate.remove(uuid);
                wLoc1s.remove(uuid);
                wLoc2s.remove(uuid);
            }else if (msg.equalsIgnoreCase("cancel")){
                canDelete.remove(uuid);
                player.sendMessage(ChatColor.RED + "Cancelling deletion");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        UUID uuid = event.getPlayer().getUniqueId();
        loc1s.remove(uuid);
        loc2s.remove(uuid);
        wLoc1s.remove(uuid);
        wLoc2s.remove(uuid);
        canName.remove(uuid);
        canCreate.remove(uuid);
    }
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event){
        UUID uuid = event.getPlayer().getUniqueId();
        loc1s.remove(uuid);
        loc2s.remove(uuid);
        wLoc1s.remove(uuid);
        wLoc2s.remove(uuid);
        canName.remove(uuid);
        canCreate.remove(uuid);
    }

    private void createArena(String id, Location loc1, Location loc2){
        String path = "arenas." + id;
        plugin.arenas.getConfig().set(path + ".loc1", plugin.fromBLoc(loc1));
        plugin.arenas.getConfig().set(path + ".loc2", plugin.fromBLoc(loc2));
        plugin.arenas.saveConfig();
    }

    private String findWall(String arenaId, Block block){
        List<String> walls = plugin.arenas.getConfig().getStringList(arenaId);
        for (String sWall : walls){
            Wall wall = new Wall(sWall);
            if (wall.isOnWall(block.getLocation())) return sWall;
        }
        return null;
    }

    private void createWall(String id, String material, Location loc1, Location loc2){
        String path = "arenas." + id + ".walls";
        Material mat = null;
        try{
            mat = Material.matchMaterial(material);
        }catch (Exception ignored){}
        if (mat == null) mat = Material.GLASS;
        Wall wall = new Wall(mat, loc1, loc2);
        List<String> walls = plugin.arenas.getConfig().getStringList(path);
        walls.add(wall.toString());
        plugin.arenas.getConfig().set(path, walls);
        plugin.arenas.saveConfig();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, wall::buildWall, 1);
    }
    private void deleteWall(String id, String sWall){
        String path = "arenas." + id + ".walls";
        List<String> walls = plugin.arenas.getConfig().getStringList(path);
        walls.remove(sWall);
        plugin.arenas.getConfig().set(path, walls);
        plugin.arenas.saveConfig();
        Wall wall = new Wall(sWall);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, wall::destroyWall, 1);
    }


    private boolean idExists(String id){
        return plugin.arenas.getConfig().contains("arenas." + id);
    }

    private String parseId(ItemStack item){
        if (item.hasItemMeta() && item.getItemMeta().hasLore()){
            List<String> lore = item.getItemMeta().getLore();
            for (String entry : lore){
                String temp = ChatColor.stripColor(entry);
                if (temp.contains("Arena: ")){
                    return temp.replace("Arena: ","");
                }


            }
        }
        return null;
    }

    public record SPair(String s1, String s2){}
}
