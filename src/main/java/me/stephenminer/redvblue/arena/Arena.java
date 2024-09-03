package me.stephenminer.redvblue.arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import me.stephenminer.redvblue.CustomItems;
import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.chests.NewLootChest;
import me.stephenminer.redvblue.util.BlockRange;

public class Arena {
    public static Set<Arena> arenas = new HashSet<>();
    public static Optional<Arena> arenaOf(Player p) {
        return arenas.stream().filter((a) -> a.players.contains(p.getUniqueId())).findFirst();
    }
    public static Optional<Arena> arenaOf(Location l) {
        return arenas.stream().filter((a) -> a.contains(l)).findFirst();
    }
    public static Optional<Arena> arenaOf(String id) {
        return arenas.stream().filter((a) -> a.getId().equalsIgnoreCase(id)).findFirst();
    }
    public static Optional<Arena> disconnectedFrom(Player p) {
        return arenas.stream().filter((a) -> a.board.getEntryTeam(p.getName()) != null).findFirst();
    }

    private final RedBlue plugin;
    private final StateSaver saver = new StateSaver();

    // Loaded from ArenaConfig
    private final String id;
    private final BlockRange bounds;
    private final Location lobby;
    private final Set<Wall> walls;
    private final Map<Team, BlockVector> spawns;
    private final Set<NewLootChest> chests;
    private final int wallFallTime;
    // ===
    
    // Operational Vars
    private final Set<UUID> players;
    private Scoreboard board;
    private ArenaPeriod period;
    // ===

    public Arena(String id, BlockRange bounds, Location lobby, Map<BlockRange, Material> walls, Map<String, BlockVector> spawns, int wallFallTime) {
        this.plugin = RedBlue.getPlugin(RedBlue.class);
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();

        // Load from ArenaConfig
        this.id = id;
        this.bounds = bounds;
        this.lobby = lobby;
        this.walls = walls.entrySet().stream().map((e) -> new Wall(e.getValue(), e.getKey())).collect(Collectors.toSet());
        this.spawns = new HashMap<>();
        for (var spawn : spawns.entrySet()) {
            this.spawns.put(createTeam(spawn.getKey()), spawn.getValue());
        }
        this.chests = new HashSet<>();
        this.wallFallTime = wallFallTime;
        // ===

        players = new HashSet<>();

        // Begin game loop
        initializeBoard();
        this.period = ArenaPeriod.QUEUEING;
        saver.loadMap();
        new BukkitRunnable() {
            public void run() {update();}
        }.runTaskTimer(plugin, 0, 20 * 2); // Period should be any factor of 10 seconds
        // ===
    }

    // Public Interface
    public void addPlayer(Player player) {
        if (players.contains(player.getUniqueId())) return;
        
        switch (period) {
            case QUEUEING:
                broadcast(ChatColor.GREEN + player.getName() + " has joined the game! "+getPlayerNumStr());
                player.setGameMode(GameMode.ADVENTURE); // send them to the lobby
                player.getInventory().clear();
                player.teleport(lobby);
            break;
            case STARTING:
            case RUNNING:
                Team pt = board.getEntryTeam(player.getName());
                if (pt != null) { // player rejoined
                    players.add(player.getUniqueId());
                    broadcast(ChatColor.GREEN + player.getDisplayName() + " has rejoined the game!");
                    spawn(player);
                    return;
                }
                if (hasWallFallen()) {
                    player.sendMessage(ChatColor.YELLOW + "The game has already started!");
                    spectate(player);
                    return;
                }
                firstSpawn(player);
                pt = board.getEntryTeam(player.getName());
                broadcast(ChatColor.GREEN + player.getName() + " has joined the game on the '"+ pt.getDisplayName() +"' team!");
            break;
            case ENDING:
            case ENDED:
                player.sendMessage(ChatColor.YELLOW + "The game has already ended!");
            return;
        }
        players.add(player.getUniqueId());
    }

    public void removePlayer(Player player, boolean intentional) {
        if (!players.contains(player.getUniqueId())) return;
        players.remove(player.getUniqueId()); // remove them from the game

        switch (period) {
            case QUEUEING:
                broadcast(ChatColor.RED + player.getDisplayName() + " has quit the game! "+getPlayerNumStr());
            break;
            case RUNNING:
            case STARTING:
                if (intentional) {
                    broadcast(ChatColor.RED + player.getDisplayName() + " has quit the game!");
                } else {
                    broadcast(ChatColor.RED + player.getDisplayName() + " has disconnected & can rejoin!");
                    update();
                    return;
                }
            break;
            case ENDING:
            case ENDED:
            return;
        }

        clean(player); // Return the player to normal
        update();
    }

    public void killPlayer(Player player, Entity cause) {
        switch (period) {
            case QUEUEING:
                player.teleport(lobby);
            break;
            case ENDING:
            case ENDED:
                player.damage(Float.MAX_VALUE, cause);
            break;
            case STARTING:
                if (!player.getScoreboard().equals(board)) {
                    firstSpawn(player);
                    break;
                }
            case RUNNING:
                if (hasWallFallen()) {
                    broadcast(ChatColor.RED + player.getName() + " has been eliminated!");
                    spectate(player);
                    update();
                    return;
                }
                respawn(player, true);
            break;
        }
    }

    public boolean forceStart() {
        if (period != ArenaPeriod.QUEUEING) return false;
        period = ArenaPeriod.STARTING;
        update();
        return true;
    }

    public boolean forceEnd() {
        if (period == ArenaPeriod.ENDING || period == ArenaPeriod.ENDED) return false;
        period = ArenaPeriod.ENDING;
        update();
        return true;
    }
    
    /**
     * To be used only in server shutdowns. does the bare minimum to clean up players and the world
     */
    public void absoluteForceEnd() {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) clean(p);
        }
        saver.loadMap();
        period = ArenaPeriod.ENDED;
    }
    // ===

    // Player Management
    private void spawn(Player player) {
        var team = board.getEntryTeam(player.getName());
        player.teleport(spawns.get(team).toLocation(bounds.world()));
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(5);
        if (revealBy != null && revealBy >= System.currentTimeMillis()) {
            reveal(player);
        }
    }

    private void firstSpawn(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setScoreboard(board);
        Team team = getSmallestTeam();
        team.addEntry(player.getName());
        spawn(player);
        player.getInventory().clear();
        CustomItems.addKit(player.getInventory(), team.getColor());
    }

    private void respawn(Player player, boolean dropInv) {
        player.setGameMode(GameMode.SURVIVAL);
        if (dropInv) {
            for (var item : player.getInventory().getContents()) {
                if (item == null) continue;
                player.getWorld().dropItemNaturally(player.getLocation(), item.clone()).setPickupDelay(40);
            }
        }
        player.getInventory().clear();
        spawn(player);
    }

    private void reveal(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 9));
    }

    private void spectate(Player player) {
        var team = board.getEntryTeam(player.getName());
        if (team != null) team.removeEntry(player.getName()); // remove them from their team
        removeEffects(player);
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(bounds.center());
        player.sendMessage(ChatColor.YELLOW + "You are a spectator! To leave, run /rvb leave.");
    }

    private void clean(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); // Reset their scoreboard
        board.getEntryTeam(player.getName()).removeEntry(player.getName()); // remove them from their team
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        removeEffects(player);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(5);
        player.teleport(lobby.getWorld().getSpawnLocation());
    }
    // ===

    // State Management
    private Long startAttempt = null;
    private long fallBy = 0;
    private Long revealBy = null;
    private void update() { // MUST BE SCHEDULED ON A FACTOR OF 10 SECONDS
        if (!Arena.arenas.contains(this)) period = ArenaPeriod.ENDED;
        
        switch (period) {
            case QUEUEING:
                long now = System.currentTimeMillis();
                boolean isFull = players.size() < plugin.loadMinPlayers();
                if (startAttempt != null) {
                    if (now >= startAttempt) {
                        if (isFull) {
                            period = ArenaPeriod.RUNNING;
                            broadcast(Sound.ENTITY_CAT_PURREOW, 50, 1);
                            broadcastTitle(ChatColor.GREEN + "The Game Has Begun!");
                            period = ArenaPeriod.STARTING;
                            break;
                        } else {
                            startAttempt += 500 * plugin.loadArenaStartDelay();
                            broadcast(ChatColor.RED + "Failed to start! " + getPlayerNumStr());
                        }
                    } else {
                        long secondsUntil = (startAttempt - now) / 1000;
                        if (secondsUntil % 10 == 0) {
                            broadcast(Sound.ENTITY_CAT_AMBIENT, 2, 1);
                            broadcast(ChatColor.GOLD + "" + secondsUntil + " seconds until game start!");
                        }
                    }
                } else if (isFull) {
                    startAttempt = now + 1000 * plugin.loadArenaStartDelay();
                }
                break;
            case STARTING:
                walls.forEach(Wall::buildWall);
                chests.forEach(NewLootChest::loadChest);
        
                var copy = new ArrayList<UUID>(players);
                Collections.shuffle(copy);
                for (var uuid : copy) firstSpawn(Bukkit.getPlayer(uuid));

                startAttempt = System.currentTimeMillis();
                fallBy = startAttempt + 1000 * wallFallTime;
                int rd = plugin.loadArenaRevealDelay();
                revealBy = rd < 0 ? null : startAttempt + 1000 * rd;
                break;
            case RUNNING:
                long liveTeams = spawns.keySet().stream().filter((t) -> t.getEntries().stream().anyMatch(this::isOnline)).count();
                if (liveTeams < 2) {
                    period = ArenaPeriod.ENDING;
                    update();
                    return;
                }

                long now2 = System.currentTimeMillis();

                if (fallBy >= now2) {
                    walls.forEach(Wall::destroyWall);
                    broadcast(Sound.EVENT_RAID_HORN, 244, 1);
                    broadcastTitle(ChatColor.YELLOW + "The Walls Have Fallen!");
                }

                if (revealBy != null && revealBy >= now2) {
                    for (var team : spawns.keySet())
                    for (String name : team.getEntries()) {
                        var op = plugin.getServer().getPlayerExact(name);
                        if (op == null) {
                            plugin.getLogger().warning("Tried to reveal player '"+ name +"' that doesn't exist!");
                            continue;
                        }
                        reveal(op);
                    }
                    broadcast(Sound.BLOCK_ANVIL_LAND, 40, 1);
                    broadcastTitle(ChatColor.RED + "Players Heave Been Revealed!");
                }

                break;
            case ENDING:
                for (Team t : spawns.keySet()) {
                    if (!t.getEntries().stream().anyMatch(this::isOnline)) continue;
                    broadcast(
                        ChatColor.RED + "GAME OVER",
                        "--------------------------",
                        ChatColor.GOLD + "" + ChatColor.BOLD + t.getDisplayName() + " Team Wins!!!",
                        ChatColor.GOLD + "Members: ",
                        " > " + String.join(",", t.getEntries()),
                        "--------------------------"
                    );
                    break;
                }
                period = ArenaPeriod.ENDED;
                final Arena thisOuter = this;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (UUID uuid : players) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) clean(p);
                        }
                        players.clear();
                        Arena.arenas.remove(thisOuter);
                        assert saver.loadMap();
                    }
                }.runTaskLater(plugin, 100);
                break;
            case ENDED: // Waiting for the runnable to finish
                break;
        }
    }

    // Scoreboard
    private Team createTeam(String name) {
        name = name.toLowerCase();
        Team t = board.registerNewTeam(name);
        try {
            t.setColor(ChatColor.valueOf(name.toUpperCase()));
        } catch (Exception ignored) {
            plugin.getLogger().info("Arena '" + id + "' registered team '" + name + "' without a color analogue!");
        }
        t.setAllowFriendlyFire(false);
        t.setPrefix(
            ChatColor.DARK_AQUA + "[" +
            t.getColor() + "" + ChatColor.BOLD + name.substring(0, 1).toUpperCase() + name.substring(1) + 
            ChatColor.RESET + ChatColor.DARK_AQUA + "] " + ChatColor.RESET
        );
        t.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.FOR_OTHER_TEAMS);
        return t;
    }

    private void initializeBoard() {
        Objective obj = board.registerNewObjective("teams", Criteria.DUMMY, "RvB");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int i = 1;
        for (var team : spawns.keySet()) {
            Team count = board.registerNewTeam(team.getName()+"-count");
            var entryName = ChatColor.RESET + "" + team.getColor();
            count.addEntry(entryName);
            obj.getScore(entryName).setScore(i++);
        }
        
        Team time = board.registerNewTeam("time");
        var entryName = ChatColor.RESET + "" + ChatColor.DARK_AQUA;
        time.addEntry(entryName);
        obj.getScore(entryName).setScore(i++);

        obj.getScore("--------------------").setScore(i++);

        final Arena outerThis = this;
        new BukkitRunnable() { // Update Loop
            public void run() {
                if (period == ArenaPeriod.ENDED) {
                    this.cancel();
                    return;
                }

                for (var team : spawns.keySet()) {
                    Team count = board.getTeam(team.getName()+"-count");
                    count.setPrefix(
                        team.getColor() + team.getName().substring(0, 1).toUpperCase() + team.getName().substring(1) + " Team: " +
                        ChatColor.RESET + team.getEntries().stream().filter(outerThis::isOnline).count() + " Alive"
                    );
                }
        
                Team time = board.getTeam("time");
                time.setPrefix(switch (period) {
                    case QUEUEING -> "Waiting";
                    case STARTING -> "Loading";
                    case RUNNING -> {
                        long now = System.currentTimeMillis();
                        Long s = null;
                        String prefix = null;
                        if (now < fallBy) {
                            s = ( fallBy - now ) / 1000;
                            prefix = "Walls Fall";
                        }
                        if (revealBy != null && now < revealBy) {
                            s = ( revealBy - now ) / 1000;
                            prefix = "Players Reveal";
                        }
                        if (prefix == null || s == null) yield "Deathmatch";
                        yield prefix + " in " + String.format("%02d:%02d", s / 60, s % 60);
                    }
                    case ENDING -> "Game Over";
                    case ENDED -> "---";
                });
                
                long now = System.currentTimeMillis();
                if (now < fallBy) {
                    int secondsUntil = (int) (fallBy - now) / 1000; // rounds down, should catch missed ticks
                    if (secondsUntil == 60 || secondsUntil == 30 || secondsUntil <= 10) {
                        broadcast(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, secondsUntil / 30 + 1, 2);
                        broadcast(ChatColor.YELLOW + "" + secondsUntil + " Seconds Until the Wall Drops");
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }
    // ===

    // Getters
    public String getId() {
        return id;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public World getWorld() {
        return bounds.world();
    }

    public boolean contains(Location loc) {
        var corner1 = loc.clone().getBlock().getLocation().toVector();
        var corner2 = corner1.clone().add(new Vector(1, 1, 1));
        return bounds.toBoundingBox().overlaps(corner1, corner2);
    }

    public boolean isJoinable() {
        return period == ArenaPeriod.QUEUEING || period == ArenaPeriod.STARTING || (period == ArenaPeriod.RUNNING && !hasWallFallen());
    }

    public boolean isEnded() {
        return period == ArenaPeriod.ENDING || period == ArenaPeriod.ENDED;
    }

    public boolean canBreak(Player player, Block block) {
        if (player.hasPermission("rvb.edit.live")) return true;

        if (!players.contains(player.getUniqueId())) return false; // outsiders cant edit arena
        for (Wall wall : walls) 
            if (!wall.isFallen() && wall.contains(block.getLocation())) return false; // nobody can edit erect walls
        
        if (!contains(block.getLocation())) { // insiders cant edit world
            player.sendMessage(ChatColor.RED + "You can not break blocks outside of arenas while you are a part of one!");
            return false;
        }

        return period == ArenaPeriod.RUNNING;
    }
    // ===

    // Utils
    private Team getSmallestTeam() {
        var smallest = spawns.keySet().stream().mapToInt(Team::getSize).min();
        if (!smallest.isPresent()) return null;
        var onlySmallest = spawns.keySet().stream().filter((t) -> t.getSize() == smallest.getAsInt()).toList();
        if (onlySmallest.isEmpty()) return null;
        return onlySmallest.size() > 1 ? onlySmallest.get(ThreadLocalRandom.current().nextInt(onlySmallest.size())) : onlySmallest.get(0);
    }

    private boolean isOnline(String playerName) {
        var op = plugin.getServer().getPlayerExact(playerName);
        return op != null && op.isOnline();
    }

    private String getPlayerNumStr() {
        return "( "+players.size() + "/" + plugin.loadMinPlayers() + " players)";
    }

    public boolean hasWallFallen() {
        for (Wall wall : walls) if (!wall.isFallen()) return false;
        return true;
    }

    private void removeEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }
    
    private void broadcast(String... msg) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;
            p.sendMessage(msg);
        }
    }

    private void broadcast(Sound sound, float vol, float pitch) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            p.playSound(p, sound, vol, pitch);
        }
    }

    private void broadcastTitle(String msg) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            p.sendTitle(msg, "", 5, 40, 5);
        }
    }
    // ===

    private enum ArenaPeriod {
        QUEUEING, STARTING, RUNNING, ENDING, ENDED
    }

    private class StateSaver { // FIXME integrate this into the state machine
        private final List<BlockState> states;
        private boolean saving, loading;

        public StateSaver() {
            states = new ArrayList<>();
            loading = saving = false;
        }

        @SuppressWarnings("unused")
        public boolean saveMap() {
            if (loading || saving) return false;
            saving = true;
            states.clear();
            new BukkitRunnable(){
                @Override
                public void run() {
                    bounds.forEach((Location l) -> states.add(l.getBlock().getState()));
                    saving = false;
                }
            }.runTaskAsynchronously(plugin);
            return true;
        }

        public boolean loadMap() {
            if (loading || saving) return false;
            loading = true;
            new BukkitRunnable() {
                private final Iterator<BlockState> statesIter = states.iterator();
                @Override
                public void run() {
                    int c = 0;
                    while (statesIter.hasNext()) {
                        if (c++ >= plugin.loadRate()) return; // couldnt care less if this is off by 1
                        statesIter.next().update(true);
                    }
                    plugin.getLogger().info(ChatColor.GOLD + "" + ChatColor.BOLD + "Arena Finished Loading");
                    loading = false;
                    this.cancel();
                }
            }.runTaskTimer(plugin, 1, 1);
            return true;
        }
    }

    @Deprecated
    public void setLootChestsREPLACEME(Set<NewLootChest> t) {
        this.chests.addAll(t);
    }
}
