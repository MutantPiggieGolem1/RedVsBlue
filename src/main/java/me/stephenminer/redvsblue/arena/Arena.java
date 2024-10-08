package me.stephenminer.redvsblue.arena;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BlockVector;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;

import me.stephenminer.redvsblue.CustomItems;
import me.stephenminer.redvsblue.RedVsBlue;
import me.stephenminer.redvsblue.util.BlockRange;
import me.stephenminer.redvsblue.util.StringCaser;
import me.stephenminer.redvsblue.util.WorldEditInterface;

public class Arena {
    public static Set<Arena> arenas = new HashSet<>();
    public static Optional<Arena> arenaOf(Player p) {
        return arenas.stream().filter((a) -> a.players.contains(p.getUniqueId())).findFirst();
    }
    public static Optional<Arena> arenaOf(String id) {
        return arenas.stream().filter((a) -> a.getId().equalsIgnoreCase(id)).findFirst();
    }
    public static Optional<Arena> disconnectedFrom(Player p) {
        return arenas.stream().filter((a) -> a.board.getEntryTeam(p.getName()) != null).findFirst();
    }
    public static Set<Arena> joinableArenas() {
        return arenas.stream().filter(Arena::isJoinable).collect(Collectors.toSet());
    }

    private final RedVsBlue plugin;

    // Loaded from ArenaConfig
    private final String id;
    private final BlockRange bounds;
    private final Location lobby;
    private final Set<Wall> walls;
    private final Map<Team, BlockVector> spawns;
    private final Map<Location, LootTable> lootCaches;
    private final int wallFallTime;
    // ===
    
    // Operational Vars
    private final Set<UUID> players;
    private final Scoreboard board;
    private final CompletableFuture<BlockArrayClipboard> save;
    private ArenaPeriod period;
    // ===


    public Arena(String id, BlockRange bounds, Location lobby, Map<BlockRange, Material> walls, Map<String, BlockVector> spawns, Map<BlockVector, String> lootCaches, int wallFallTime) {
        if (Arena.arenaOf(id).isPresent()) throw new IllegalStateException("Only one arena may exist with a given id.");
        
        this.plugin = RedVsBlue.getPlugin(RedVsBlue.class);
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
        this.lootCaches = new HashMap<>();
        for (var cache : lootCaches.entrySet()) {
            var nk = NamespacedKey.fromString(cache.getValue());
            if (nk == null) {
                plugin.getLogger().warning("[Arena '"+ id +"'] Invalid loot table key '"+cache.getValue()+"'.");
                continue;
            }
            var lt = plugin.getServer().getLootTable(nk);
            if (lt == null) {
                plugin.getLogger().warning("[Arena '"+ id +"'] Missing loot table for key '"+nk+"'.");
                continue;
            }
            this.lootCaches.put(cache.getKey().toLocation(bounds.world()), lt);
        }
        this.wallFallTime = wallFallTime;
        // ===

        players = new HashSet<>();
        save = WorldEditInterface.copy(WorldEditInterface.toCuboidRegion(bounds));

        // Begin game loop
        initializeBoard();
        this.period = ArenaPeriod.QUEUEING;
        var outerThis = this;
        new BukkitRunnable() {
            public void run() {
                if (!Arena.arenas.contains(outerThis)) {
                    this.cancel();
                    return;
                }
                update();
            }
        }.runTaskTimer(plugin, 0, 20 * 2); // Period should be any factor of 10 seconds
        // ===
    }

    // Public Interface
    public void addPlayer(Player player) {
        if (players.contains(player.getUniqueId())) return;
        
        switch (period) {
            case QUEUEING:
                players.add(player.getUniqueId());
                broadcast(ChatColor.GREEN + player.getName() + " has joined the game! ");
                player.setGameMode(GameMode.ADVENTURE); // send them to the lobby
                player.setScoreboard(board);
                player.teleport(lobby);
                player.getInventory().clear();
            break;
            case START:
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
                players.add(player.getUniqueId());
                player.setScoreboard(board);
                firstSpawn(player);
                pt = board.getEntryTeam(player.getName());
                broadcast(ChatColor.GREEN + player.getName() + " has joined the game on the '"+ pt.getDisplayName() +"' team!");
            break;
            case END:
            case ENDED:
                player.sendMessage(ChatColor.YELLOW + "The game has already ended!");
            return;
        }
    }

    public void removePlayer(Player player, boolean intentional) {
        if (!players.contains(player.getUniqueId())) return;
        players.remove(player.getUniqueId()); // remove them from the game

        switch (period) {
            case QUEUEING:
                broadcast(ChatColor.RED + player.getDisplayName() + " has quit the game! "+getPlayerNumStr(true));
            break;
            case RUNNING:
            case START:
                if (intentional) {
                    broadcast(ChatColor.RED + player.getDisplayName() + " has quit the game!");
                } else {
                    broadcast(ChatColor.RED + player.getDisplayName() + " has disconnected & can rejoin!");
                    update();
                    return;
                }
            break;
            case END:
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
            case END:
            case ENDED:
                player.damage(Float.MAX_VALUE, cause);
            break;
            case START:
                if (!player.getScoreboard().equals(board)) {
                    player.setScoreboard(board);
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
        if (period != ArenaPeriod.QUEUEING || !save.isDone()) return false;
        period = ArenaPeriod.START;
        update();
        return true;
    }

    public boolean forceEnd() {
        if (period == ArenaPeriod.END || period == ArenaPeriod.ENDED) return false;
        period = ArenaPeriod.END;
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
        loadMapThen(() -> {});
    }
    // ===

    // Player Management
    private void spawn(Player player) {
        var team = board.getEntryTeam(player.getName());
        player.teleport(spawns.get(team).toLocation(getWorld()));
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(5);
        if (revealBy != null && revealBy <= System.currentTimeMillis()) {
            reveal(player);
        }
    }

    private void firstSpawn(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
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
        var ot = board.getEntryTeam(player.getName());
        if (ot != null) ot.removeEntry(player.getName()); // remove them from their team
        removeEffects(player);
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(bounds.center());
        player.sendMessage(ChatColor.YELLOW + "You are a spectator! To leave, run /rvb leave.");
    }

    private void clean(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); // Reset their scoreboard
        var ot = board.getEntryTeam(player.getName()); // not everybody is on a team yet- like when queueing
        if (ot != null) ot.removeEntry(player.getName()); // remove them from their team
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
    private Long startAttempt = null; // when to next try to start the game (unix timestamp)
    private long fallBy = 0; // when the walls should fall (unix timestamp)
    private Long revealBy = null; // when players should be revealed (unix timestamp)
    private boolean initialRevealCompleted = false;
    private void update() { // MUST BE SCHEDULED ON A FACTOR OF 10 SECONDS        
        switch (period) {
            case QUEUEING: // Repeats
                long now = System.currentTimeMillis();
                boolean isFull = players.size() >= plugin.loadMinPlayers();
                if (startAttempt != null) {
                    if (now >= startAttempt) {
                        if (isFull) {
                            broadcast(Sound.ENTITY_CAT_PURREOW, 50, 1);
                            broadcastTitle(ChatColor.GREEN + "The Game Has Begun!");
                            period = ArenaPeriod.START;
                        } else if (save.isDone() && players.isEmpty()) { // gracefully close if nobody's playing
                            loadMapThen(() -> Arena.arenas.remove(this));
                            period = ArenaPeriod.ENDED;
                        } else {
                            startAttempt += 500 * plugin.loadArenaStartDelay();
                            broadcast(ChatColor.RED + "Failed to start! " + getPlayerNumStr(true));
                        }
                    } else {
                        long secondsUntil = (startAttempt - now) / 1000;
                        if (secondsUntil != 0 && (secondsUntil % 10 == 0 || secondsUntil < 10)) {
                            broadcast(Sound.ENTITY_CAT_AMBIENT, 2, 1);
                            broadcast(ChatColor.GOLD + "" + secondsUntil + " seconds until game start!");
                        }
                    }
                } else if (isFull && save.isDone()) { // there are enough players, AND the arena is saved
                    startAttempt = now + 1000 * plugin.loadArenaStartDelay();
                    broadcast(Sound.ENTITY_CAT_STRAY_AMBIENT, 2, 1);
                }
                if (period != ArenaPeriod.START) break;
            case START: // Runs Once
                assert save.isDone(); // ensure world is saved
                
                bounds.getTransitiveEntities().forEach(e -> e.teleport(lobby.getWorld().getSpawnLocation())); // remove all entities

                walls.forEach(Wall::buildWall); // raise the walls

                for (var cache : lootCaches.entrySet()) { // load the loot
                    var block = cache.getKey().getBlock().getState();
                    if (!(block instanceof Lootable)) {
                        plugin.getLogger().warning("[Arena '"+ id +"'] Unlinked loot table at "+block.getLocation());
                        continue;
                    }
                    block.getBlock().setType(block.getType(), false); // replace the block (attempt to reset first-open)

                    block = block.getBlock().getState();

                    if (block instanceof Container c)
                        c.getInventory().clear(); // empty whatever was in there before

                    block = block.getBlock().getState(); // re-fetch the state because inventory operations mess with it

                    if (block instanceof Nameable n)
                        n.setCustomName("RvB Loot Cache");
                        
                    ((Lootable) block).setLootTable(cache.getValue());
                
                    block.update(true); // write the newly loot-tabled block to the world
                }
        
                var copy = new ArrayList<UUID>(players);
                Collections.shuffle(copy);
                for (var uuid : copy) firstSpawn(Bukkit.getPlayer(uuid));

                startAttempt = System.currentTimeMillis();
                fallBy = startAttempt + 1000 * wallFallTime;
                int rd = plugin.loadArenaRevealDelay();
                revealBy = rd < 0 ? null : startAttempt + 1000 * rd;

                period = ArenaPeriod.RUNNING;
            case RUNNING: // Repeats
                long liveTeams = spawns.keySet().stream().filter((t) -> t.getEntries().stream().anyMatch(this::isOnline)).count();
                if (liveTeams < 2) {
                    period = ArenaPeriod.END;
                    update();
                    return;
                }

                long now2 = System.currentTimeMillis();

                if (fallBy <= now2 && !hasWallFallen()) {
                    walls.forEach(Wall::destroyWall);
                    broadcast(Sound.EVENT_RAID_HORN, 244, 1);
                    broadcastTitle(ChatColor.YELLOW + "The Walls Have Fallen!");
                }

                if (revealBy != null && revealBy <= now2 && !initialRevealCompleted) {
                    for (var team : spawns.keySet())
                    for (String name : team.getEntries()) {
                        var op = plugin.getServer().getPlayerExact(name);
                        if (op == null) {
                            plugin.getLogger().warning("[Arena '"+ id +"'] Tried to reveal player '"+ name +"' that doesn't exist!");
                            continue;
                        }
                        reveal(op);
                    }
                    broadcast(Sound.BLOCK_ANVIL_LAND, 40, 1);
                    broadcastTitle(ChatColor.RED + "Players Have Been Revealed!");
                    initialRevealCompleted = true;
                }

                break;
            case END: // Runs Once
                for (Team t : spawns.keySet()) {
                    if (!t.getEntries().stream().anyMatch(this::isOnline)) continue;
                    broadcast(
                        "--------------------------",
                        t.getColor() + "" + ChatColor.BOLD + StringCaser.toTitleCase(t.getDisplayName()) + " Team Wins!",
                        ChatColor.DARK_AQUA + "Members: ",
                        " > " + String.join(", ", t.getEntries()),
                        "--------------------------"
                    );
                    break;
                }
                for (UUID uuid : players) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) {
                        plugin.getLogger().warning("[Arena '"+ id +"'] Tried to remove nonexistant player '"+uuid.toString()+"'.");
                        continue;
                    }
                    clean(p);
                }
                players.clear();
                loadMapThen(() -> Arena.arenas.remove(this));
                period = ArenaPeriod.ENDED;
            case ENDED: // Repeats
                break;
        }
    }
    // ===

    // Scoreboard
    private Team createTeam(String name) {
        name = name.toLowerCase();
        Team t = board.registerNewTeam(name);
        try {
            t.setColor(ChatColor.valueOf(name.toUpperCase()));
        } catch (Exception ignored) {
            plugin.getLogger().info("[Arena '"+ id +"'] Team '" + name + "' registered without a color analogue!");
        }
        t.setAllowFriendlyFire(false);
        t.setPrefix(
            ChatColor.DARK_AQUA + "[" +
            t.getColor() + "" + ChatColor.BOLD + StringCaser.toTitleCase(name) + 
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
                        team.getColor() + StringCaser.toTitleCase(team.getDisplayName()) + " Team: " +
                        ChatColor.RESET + team.getEntries().stream().filter(outerThis::isOnline).count() + " Alive"
                    );
                }
        
                Team time = board.getTeam("time");
                time.setPrefix(switch (period) {
                    case QUEUEING -> "Queueing: " + getPlayerNumStr(false);
                    case START -> "Loading";
                    case RUNNING -> {
                        long now = System.currentTimeMillis();
                        Long s = null;
                        String prefix = null;
                        if (now < fallBy) {
                            s = ( fallBy - now ) / 1000;
                            prefix = "Walls Fall";
                        }
                        if (revealBy != null && now < revealBy) {
                            long ns = ( revealBy - now ) / 1000;
                            if (s == null || ns < s) {
                                s = ns;
                                prefix = "Players Reveal";
                            }
                        }
                        if (prefix == null || s == null) yield "Deathmatch";
                        yield prefix + " in " + String.format("%02d:%02d", s / 60, s % 60);
                    }
                    case END -> "Game Over";
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

    public boolean isJoinable() {
        return period == ArenaPeriod.QUEUEING || period == ArenaPeriod.START || (period == ArenaPeriod.RUNNING && !hasWallFallen());
    }

    public boolean isEnded() {
        return period == ArenaPeriod.END || period == ArenaPeriod.ENDED;
    }

    public boolean canBreak(Player player, Block block) {
        if (player.hasPermission("rvb.edit.live")) return true;

        if (!players.contains(player.getUniqueId())) return false; // outsiders cant edit arena
        for (Wall wall : walls) 
            if (!wall.isFallen() && wall.contains(block)) return false; // nobody can edit erect walls
        
        if (!bounds.contains(block)) { // insiders cant edit outside world
            player.sendMessage(ChatColor.RED + "You can not break blocks outside of arenas while you are a part of one!");
            return false;
        }

        return period == ArenaPeriod.RUNNING;
    }
    // ===

    // Utils
    private Team getSmallestTeam() {
        var smallest = spawns.keySet().stream().mapToInt(Team::getSize).min();
        if (!smallest.isPresent()) throw new IllegalStateException("getSmallestTeam() called with no teams!");
        var onlySmallest = spawns.keySet().stream().filter((t) -> t.getSize() == smallest.getAsInt()).toList();
        if (onlySmallest.isEmpty()) return null;
        return onlySmallest.size() > 1 ? onlySmallest.get(ThreadLocalRandom.current().nextInt(onlySmallest.size())) : onlySmallest.get(0);
    }

    private boolean isOnline(String playerName) {
        var op = plugin.getServer().getPlayerExact(playerName);
        return op != null && op.isOnline();
    }

    private String getPlayerNumStr(boolean parenthesize) {
        var s = players.size() + "/" + plugin.loadMinPlayers() + " players";
        return parenthesize ? "("+s+")" : s;
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
        activePlayers().forEach(p -> p.sendMessage(msg));
    }

    private void broadcast(Sound sound, float vol, float pitch) {
        activePlayers().forEach(p -> p.playSound(p, sound, vol, pitch));
    }

    private void broadcastTitle(String msg) {
        activePlayers().forEach(p -> p.sendTitle(msg, "", 5, 40, 5));
    }

    private Set<Player> activePlayers() {
        Set<Player> out = new HashSet<>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) {
                plugin.getLogger().warning("[Arena '"+ id +"'] Nonexistant or offline player @"+uuid+" wasn't removed!");
                continue;
            }
            out.add(p);
        }
        return out;
    }
    
    private void loadMapThen(Runnable callback) {
        if (!save.isDone()) {
            plugin.getLogger().warning("Map '" + id + "' loaded before save completed. Load cancelled.");
            callback.run();
            save.cancel(true);
            return;
        }
        WorldEditInterface.paste(WorldEditInterface.toCuboidRegion(bounds), save.join())
            .orTimeout(1, MINUTES)
            .whenComplete((__, ex) -> {
                if (ex != null) {
                    plugin.getLogger().severe("Map '" + id + "' encountered an exception loading");
                    ex.printStackTrace();
                }
                callback.run();
            });
    }
    // ===

    private enum ArenaPeriod {
        QUEUEING, START, RUNNING, END, ENDED
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Arena a)) return false;
        return a.getId().equals(id);
    }
}