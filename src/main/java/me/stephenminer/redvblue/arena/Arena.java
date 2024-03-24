package me.stephenminer.redvblue.arena;

import me.stephenminer.redvblue.Items;
import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.chests.NewLootChest;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Arena {
    public static List<Arena> arenas = new ArrayList<>();
    private final RedBlue plugin;
    private Location loc1;
    private Location loc2;

    /**
    Game Can Function without a wall, this just removed a graceperiod.
     If a wall is set then the "grace period" where walls are up blocking teams will be active.
     **/
    private List<Wall> walls;
    private final Location redSpawn;
    private final Location blueSpawn;
    private final Location lobby;
    private String id;
    private String name;
    private int fallTime;
    private int time;
    private boolean started;
    private boolean starting;
    private boolean ending;
    private boolean saved;



    private List<UUID> players;
    private Set<NewLootChest> chests;
    private HashMap<UUID, Team> offline;

    private ArenaSaver saver;

    public HashMap<Location, DataPair> blockMap;

    private Scoreboard board;

    /**
     * Constructor for arena, make sure to add the wall!!
     * @param id
     * @param name
     * @param loc1
     * @param loc2
     * @param redSpawn
     * @param blueSpawn
     */
    public Arena(String id, String name, Location loc1, Location loc2, Location redSpawn, Location blueSpawn, Location lobby){
        this.plugin = RedBlue.getPlugin(RedBlue.class);
        this.id = id;
        this.name = name;
        this.loc1 = loc1;
        this.loc2 = loc2;
        this.lobby = lobby;
        this.redSpawn = redSpawn;
        this.blueSpawn = blueSpawn;
        this.walls = new ArrayList<>();
        createBoard();
        offline = new HashMap<>();
        players = new ArrayList<>();
        blockMap = new HashMap<>();
        chests = new HashSet<>();
        saver = new ArenaSaver(this);
        saver.saveMap();
        updateBoard();
    }


    public void addPlayer(Player player){
        if (!saved){
            saver.saveMap();
            saved = true;
        }
        if (ending) {
            player.sendMessage(ChatColor.RED + "Game is ending!");
            return;
        }
        if (!started){
            player.getActivePotionEffects().clear();
            players.add(player.getUniqueId());
            broadcast(ChatColor.GREEN + player.getName() + " has joined the game! (" + players.size() + "/" + loadMinPlayers() + " to start)");
            player.teleport(lobby);
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            if (!starting) checkStart();
        }else if (!wallsFallen()){
            player.getActivePotionEffects().clear();
            players.add(player.getUniqueId());
            Team team = findOpenTeam();
            team.addPlayer(player);

            Items items = new Items();
            player.getInventory().clear();
            if (board.getTeam("red").equals(team)) {
                player.teleport(redSpawn);
                items.outfitPlayer(player,0);
            } else {
                player.teleport(blueSpawn);
                items.outfitPlayer(player,1);
            }
            player.sendMessage(ChatColor.GOLD + "You are on the " + team.getName() + " team!");
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(5);
            return;
        }else{
            player.sendMessage(ChatColor.RED + "Sorry! This game has already started and the walls have dropped, you can not join this game at this time!");
            return;
        }

    }


    public boolean wallsFallen(){
       for (Wall wall : walls){
           if (!wall.isFallen()) return false;
       }
       return true;
    }



    public void removePlayer(Player player){
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        Team red = board.getTeam("red");
        Team blue = board.getTeam("blue");

        players.remove(player.getUniqueId());

        if (red.hasPlayer(player)){
            red.removePlayer(player);
        }else if (blue.hasPlayer(player)) blue.removePlayer(player);
        broadcast(ChatColor.RED + player.getDisplayName() + " has quit the game! (" + players.size() + "/" + loadMinPlayers() +" to start)");
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        player.getActivePotionEffects().clear();
        removeEffects(player);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(5);
        if (plugin.rerouteLoc != null) player.teleport(plugin.rerouteLoc);
        else player.sendMessage(ChatColor.RED + "Tell your admins to set the reroute location!");
        if (!ending) {
            checkDeletion();
            checkEnding();
        }
    }

    public void disconnectPlayer(Player player){
        if (!started) {
            removePlayer(player);
            return;
        }
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        Team red = board.getTeam("red");
        Team blue = board.getTeam("blue");
        broadcast(ChatColor.RED + player.getDisplayName() + " has disconnected & can rejoin!");
        players.remove(player.getUniqueId());
        if (plugin.rerouteLoc != null) player.teleport(plugin.rerouteLoc);
        if (red.hasPlayer(player)){
            offline.put(player.getUniqueId(), red);
            red.removePlayer(player);
        }
        if (blue.hasPlayer(player)){
            offline.put(player.getUniqueId(), blue);
            blue.removePlayer(player);
        }
        timeout(player.getUniqueId());
        if (!ending){
            checkEnding();
            checkDeletion();
        }
    }

    public void checkStart(){
        if (players.size() >= loadMinPlayers()){
            starting = true;
            new BukkitRunnable(){
                final int max = 15*20;
                int count = 0;
                @Override
                public void run(){
                    if (saver.isSaving()) {
                        return;
                    }
                    if (count % 20 == 0){
                        broadcast(Sound.ENTITY_CAT_AMBIENT,2,1);
                        broadcast(ChatColor.GOLD + "" + ((max - count) / 20) + " seconds until game start!");
                    }
                    if (players.size() < loadMinPlayers()){
                        broadcast(ChatColor.RED + "Not enough players! (" + players.size() + "/" + loadMinPlayers() + ")");
                        broadcast(Sound.ENTITY_CAT_PURREOW, 50, 1);
                        starting = false;
                        this.cancel();
                        return;
                    }
                    if (count >= max){
                        if (saver.isSaving())broadcast(ChatColor.YELLOW + "Map is saving" );
                        count-=20;
                        start();
                        this.cancel();
                        return;
                    }
                    count++;
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    public void checkDeletion(){
        if (players.size() == 0){
            Arena arena = this;
            new BukkitRunnable(){
                final int max = 2*60*20;
                int count;
                @Override
                public void run(){
                    if (!Arena.arenas.contains(arena)){
                        started = false;
                        this.cancel();
                        return;
                    }
                    if (ending || players.size() != 0){
                        this.cancel();
                        return;
                    }
                    if (count >= max){
                        end();
                        this.cancel();
                        return;
                    }
                    count++;
                }
            }.runTaskTimer(plugin, 1, 1);
        }
    }

    public void checkEnding(){
        Team red = board.getTeam("red");
        Team blue = board.getTeam("blue");
        if (!ending && started && !players.isEmpty() && (getAlive((byte) 0) == 0 || getAlive((byte) 1) == 0)) end();
    }

    public void end(){
        started = false;
        starting = false;
        ending = true;
        saved = false;
        Arena arena = this;
        Team red = board.getTeam("red");
        Team blue = board.getTeam("blue");
        if (getAlive((byte) 0) == 0) {
            broadcast(ChatColor.RED + "GAME OVER");
            broadcast("--------------------------");
            broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "Blue Team Wins!!!");
            broadcast(ChatColor.GOLD + "Members: ");
            for (OfflinePlayer p : blue.getPlayers()){
                broadcast("-" + p.getName());
            }
            broadcast("--------------------------");
        } else if (getAlive((byte) 1) == 0) {
            broadcast(ChatColor.RED + "GAME OVER");
            broadcast("--------------------------");
            broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "Red Team Wins!!!");
            broadcast(ChatColor.GOLD + "Members: ");
            for (OfflinePlayer p : red.getPlayers()){
                broadcast("-" + p.getName());
            }
            broadcast("--------------------------");
        }
        new BukkitRunnable(){
            @Override
            public void run(){
                board.clearSlot(DisplaySlot.SIDEBAR);
                offline.clear();

                walls.forEach(Wall::destroyWall);
                for (int i = players.size()-1; i >= 0; i--){
                    UUID uuid = players.get(i);
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;
                    removePlayer(p);
                }
                players.clear();
                reset();
               // saver.loadMap();
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        if (!saver.isLoading()){
                            Arena.arenas.remove(arena);
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin,1,1);

            }
        }.runTaskLater(plugin, 100);
    }

    private void removeEffects(Player player){
        for (PotionEffect effect : player.getActivePotionEffects()){
            player.removePotionEffect(effect.getType());
        }
    }

    public void forceEnd(){
        forceEnd(false);
    }
    public void forceEnd(boolean reset){
        started = false;
        starting = false;
        ending = true;
        saved = false;
        Arena.arenas.remove(this);
        for (int i = players.size()-1; i >= 0; i--){
            UUID uuid = players.get(i);
            Player p = Bukkit.getPlayer(uuid);
            removePlayer(p);
        }
        offline.clear();
        players.clear();
        walls.forEach(Wall::buildWall);
        if (reset) reset();
    }

    public void start(){
        start(false);
    }
    public void start(boolean setTeam){
        started = true;
        walls.forEach(Wall::buildWall);
        for (NewLootChest lootChest : chests){
            lootChest.loadChest();

        }
        wallTimer();
        Team red = board.getTeam("red");
        Team blue = board.getTeam("blue");
        List<UUID> copy = new ArrayList<>(players);

        Items items = new Items();
        if (!setTeam) {
            while (!copy.isEmpty()) {
                int index = ThreadLocalRandom.current().nextInt(copy.size());
                UUID uuid = copy.get(index);
                Player p = Bukkit.getPlayer(uuid);
                p.setScoreboard(board);
                p.setHealth(20);
                p.setFoodLevel(20);
                Team team = findOpenTeam();
                int t = team.equals(red) ? 0 : 1;
                if (team.equals(red)) p.teleport(redSpawn);
                else if (team.equals(blue)) p.teleport(blueSpawn);
                team.addPlayer(p);
                items.outfitPlayer(p, t);
                copy.remove(index);
            }
        }else{
            String lowellCheck = "[Lowell]";
            String otherCheck = "[place-holder]";
            boolean rand = ThreadLocalRandom.current().nextBoolean();;
            if (rand){
                assignTeam(red,lowellCheck, true);
                assignTeam(blue, otherCheck, false);
            }else {
                assignTeam(red, otherCheck, true);
                assignTeam(blue, lowellCheck, false);
            }
        }
        broadcastTitle(ChatColor.GOLD + "Red vs Blue");
    }

    private void assignTeam(Team team, String checkFor, boolean red){
        TeamChecker checker = new TeamChecker(plugin);
        Items items = new Items();
        for (UUID uuid : players){
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            team.addPlayer(player);
            int teamId;
            if (red) {
                player.teleport(redSpawn);
                teamId = 0;
            } else {
                player.teleport(blueSpawn);
                teamId = 1;
            }

            items.outfitPlayer(player,teamId);
        }
    }


    private void wallTimer(){
        new BukkitRunnable(){
            @Override
            public void run(){
                if (!started){
                    this.cancel();
                    return;
                }
                if (time % 20 == 0 && (fallTime-time) / 20 == 60) {
                    broadcast(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2, 2);
                    broadcast(ChatColor.YELLOW + "1 Minute Until the Wall Drops");
                }
                if (time % 20 == 0 & (fallTime-time) / 20 == 30){
                    broadcast(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2, 2);
                    broadcast(ChatColor.YELLOW + "30 Seconds Until the Wall Drops");
                }
                if (time % 20 == 0 && (fallTime-time) / 20 < 15){
                    broadcast(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2);
                    broadcast(ChatColor.YELLOW + "" + ((fallTime-time) / 20) + " Seconds Until the Wall drops!");
                }
                if (time == fallTime) {
                    walls.forEach(Wall::destroyWall);
                    broadcastTitle(ChatColor.DARK_RED + "The Wall Has Fallen");
                    revealTimer();
                    broadcast(Sound.EVENT_RAID_HORN, 244, 1);
                    this.cancel();
                    return;
                }
                time++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void revealTimer(){
        final int max = 10*60*10;
        fallTime = max;
        new BukkitRunnable(){
            int count = 0;

            @Override
            public void run(){
                if (!started){
                    this.cancel();
                    return;
                }
                if (count >= max){
                    broadcastTitle(ChatColor.GOLD + "All Players Revealed!");
                    for (UUID uuid : players){
                        try{
                            Player player = Bukkit.getPlayer(uuid);
                            if (player.getGameMode() == GameMode.SPECTATOR) continue;
                            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 99999, 9));
                        }catch (Exception ignored){}
                    }
                    this.cancel();
                    return;
                }
                count++;
                time = count;

            }
        }.runTaskTimer(plugin, 1, 1);
    }





    private void timeout(UUID uuid){
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        new BukkitRunnable(){
            final int max = 5*60*20;
            int count = 0;
            @Override
            public void run() {
                if (!started || ending){
                    this.cancel();
                    offline.remove(uuid);
                    return;
                }
                if (count >= max){
                    broadcast(ChatColor.DARK_RED + op.getName() + " can no longer rejoin...");
                    offline.remove(uuid);
                    this.cancel();
                    return;
                }
                if (op.isOnline()){
                    Player player = op.getPlayer();
                    Team team = offline.get(uuid);
                    if (team.equals(board.getTeam("red"))) player.teleport(redSpawn);
                    else player.teleport(blueSpawn);
                    players.add(uuid);
                    broadcast(ChatColor.GREEN + player.getDisplayName() + " has rejoined the game!");
                    offline.remove(uuid);
                    this.cancel();
                    return;
                }
                count++;
            }
        }.runTaskTimer(plugin, 20, 1);
    }


    private Team findOpenTeam(){
        Team red = board.getTeam("red");
        Team blue = board.getTeam("blue");
        if (red.getSize() < blue.getSize()) return red;
        else if (blue.getSize() < red.getSize()) return blue;
        else {
            return ThreadLocalRandom.current().nextBoolean() ? red : blue;
        }
    }









    public void broadcast(String msg){
        for (int i = players.size()-1; i >= 0; i--){
            UUID uuid = players.get(i);
            Player p = Bukkit.getPlayer(uuid);
            p.sendMessage(msg);
        }
    }
    public void broadcast(Sound sound, float vol, float pitch){
        for (int i = players.size()-1; i >= 0; i--){
            UUID uuid = players.get(i);
            Player p = Bukkit.getPlayer(uuid);
            p.playSound(p, sound, vol, pitch);
        }
    }

    public void broadcastTitle(String msg){
        for (int i = players.size()-1; i >= 0; i--){
            UUID uuid = players.get(i);
            Player p = Bukkit.getPlayer(uuid);
            p.sendTitle(msg, "",5, 40, 5);
        }
    }

    /**
     * @param player
     * @param block
     * @return true if the player is breaking a block in an arena (which is okay).
     * False if player breaks a block outside of the arena and doesnt have permission to do so (not okay)
     */
    public boolean tryEdit(Player player, Block block){
        if (!players.contains(player.getUniqueId())) return false;
        for (Wall wall : walls) {
            if (wall != null && !wall.tryEdit(player, block)) return false;
        }
        BoundingBox bounds = BoundingBox.of(loc1.clone().add(0.5,0.5,0.5), loc2.clone().add(0.5,0.5,0.5));
        Vector corner1 = block.getLocation().toVector();
        Vector corner2 = corner1.clone().add(new Vector(1,1,1));
        if (!bounds.overlaps(corner1, corner2)){
            if (!player.hasPermission("rvb.edit.live")){
                player.sendMessage(ChatColor.RED + "You can not break blocks outside of arenas while you are apart of one!",
                        "Leave the game (/rvbLeave) if you want to leave");
                return false;
            }
            return true;
        }else return started;
    }

    public boolean isInArena(Location loc){
        BoundingBox bounds = BoundingBox.of(loc1.clone().add(0.5,0.5,0.5),loc2.clone().add(0.5,0.5,0.5));
        Vector corner1 = loc.clone().getBlock().getLocation().toVector();
        Vector corner2 = corner1.clone().add(new Vector(1,1,1));
        return bounds.overlaps(corner1, corner2);
    }

    public void reset(){
        saver.loadMap();
        /*
        Set<Location> locSet = blockMap.keySet();
        for (Location loc : locSet){
            DataPair pair = blockMap.get(loc);
            Block block = loc.getBlock();
            block.setType(pair.mat());
            block.setBlockData(pair.data());
        }

         */
    }

    public int getAlive(byte team){
        int alive = 0;
        if (team == 0){
            Team red = board.getTeam("red");
            for (OfflinePlayer offlinePlayer : red.getPlayers()){
                if (offlinePlayer.isOnline()){
                    Player player = offlinePlayer.getPlayer();
                    if (player.getGameMode().equals(GameMode.SURVIVAL)) alive++;
                }
            }
        }else if (team == 1){
            Team blue = board.getTeam("blue");
            for (OfflinePlayer offlinePlayer : blue.getPlayers()){
                if (offlinePlayer.isOnline()){
                    Player player = offlinePlayer.getPlayer();
                    if (player.getGameMode().equals(GameMode.SURVIVAL)) alive++;
                }
            }
        }
        return alive;
    }
    public int loadMinPlayers(){return plugin.settings.getConfig().getInt("settings.min-players");}
    public int fallTime(){ return fallTime; }
    public int time(){ return time; }
    public String getId(){ return id; }
    public String name(){ return name; }

    public Location getLobby(){ return lobby; }
    public Location getRedSpawn(){ return redSpawn; }
    public Location getBlueSpawn(){ return blueSpawn;}
    public Scoreboard getBoard(){ return board; }
    public void setFallTime(int fallTime){ this.fallTime = fallTime; }
    public void setTime(int time){ this.time = time; }
    public List<UUID> getPlayers(){ return players; }
    public boolean isStarted(){ return started; }
    public boolean hasPlayer(Player player){ return players.contains(player.getUniqueId());}

    public List<Wall> getWalls(){
        return walls;
    }

    public void addWall(Wall wall){
        walls.add(wall);
    }

    public void setWalls(List<Wall> walls){ this.walls = walls; }

    public void removeWall(Wall wall){ walls.remove(wall); }
    public void addChests(Collection<NewLootChest> chests){
        this.chests.addAll(chests);
    }
    public Set<NewLootChest> getGameChests(){ return chests; }

    public Location getLoc1(){ return loc1; }
    public Location getLoc2(){ return loc2; }

    /**
     *
     * @return the players in the arena;
     */
    public int size(){
        return players.size();
    }


    public void save(){
        String base = "arenas." + id;
        plugin.arenas.getConfig().set(base + ".name", name);
        plugin.arenas.getConfig().set(base + ".loc1", plugin.fromBLoc(loc1));
        plugin.arenas.getConfig().set(base + ".loc2", plugin.fromBLoc(loc2));
        plugin.arenas.getConfig().set(base + ".red-spawn",plugin.fromLoc(redSpawn));
        plugin.arenas.getConfig().set(base + ".blue-spawn", plugin.fromLoc(blueSpawn));
        plugin.arenas.getConfig().set(base + ".lobby", plugin.fromLoc(lobby));
        plugin.arenas.saveConfig();
    }


    /*

    SCOREBOARD STUFF BELOW

     */

    public void createBoard(){
        board = Bukkit.getScoreboardManager().getNewScoreboard();
        Team red = board.registerNewTeam("red");
        red.setAllowFriendlyFire(false);
        red.setColor(ChatColor.RED);
        red.setPrefix("[Red] ");
        red.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.FOR_OTHER_TEAMS);

        Team blue = board.registerNewTeam("blue");
        blue.setAllowFriendlyFire(false);
        blue.setColor(ChatColor.BLUE);
        blue.setPrefix("[Blue] ");
        blue.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.FOR_OTHER_TEAMS);

        Objective objective = board.registerNewObjective("teams", "dummy", "Red vs. Blue (not halo!)");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }



    private void updateBoard(){
        Team blue = board.registerNewTeam("blue-count");
        blue.addEntry(ChatColor.RED + "" + ChatColor.BLUE);

        Team red = board.registerNewTeam("red-count");
        red.addEntry(ChatColor.RED + "" + ChatColor.WHITE);

        Team time = board.registerNewTeam("time");
        time.addEntry(ChatColor.RED + "" + ChatColor.AQUA);

        Objective obj = board.getObjective("teams");
        obj.getScore(ChatColor.RED + "" + ChatColor.BLUE).setScore(3);
        obj.getScore(ChatColor.RED + "" + ChatColor.WHITE).setScore(2);
        obj.getScore(ChatColor.RED + "" + ChatColor.AQUA).setScore(1);
        Score line = obj.getScore("--------------------");
        line.setScore(4);
        new BukkitRunnable(){
            @Override
            public void run(){
                blue.setPrefix(ChatColor.BLUE + "Blue-Team: " + ChatColor.WHITE + getAlive((byte) 1));
                red.setPrefix(ChatColor.RED + "Red-Team: " + ChatColor.WHITE + getAlive((byte) 0));
                String str = wallsFallen() ? "Players Revealed In: " : "Walls Fall In: ";
                time.setPrefix(ChatColor.YELLOW + str + ChatColor.WHITE + timeString());
                for (int i = players.size()-1; i >= 0; i--){
                    UUID uuid = players.get(i);
                    Player p = Bukkit.getPlayer(uuid);
                    p.setScoreboard(board);
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    private String timeString(){
        //values should be the same because decimals are floored.
        int seconds = (fallTime-time)/20;
        int minutes = (fallTime-time) / 1200;
        seconds -= (minutes*60);
        String str = seconds >= 10 ? minutes + ":" + seconds : minutes + ":0" + seconds;

        return str;
    }

    public Team red(){
        return board.getTeam("red");
    }
    public Team blue(){
        return board.getTeam("blue");
    }



}
