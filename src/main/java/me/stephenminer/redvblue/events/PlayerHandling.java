package me.stephenminer.redvblue.events;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.arena.Arena;
import me.stephenminer.redvblue.arena.DataPair;
import me.stephenminer.redvblue.arena.Wall;

public class PlayerHandling implements Listener {
    private final RedBlue plugin;
    private BlockFace[] faces;

    public PlayerHandling(RedBlue plugin) {
        this.plugin = plugin;
        faces = new BlockFace[6];
        faces[0] = BlockFace.EAST;
        faces[1] = BlockFace.WEST;
        faces[2] = BlockFace.NORTH;
        faces[3] = BlockFace.SOUTH;
        faces[4] = BlockFace.UP;
        faces[5] = BlockFace.DOWN;
    }

    @EventHandler
    public void stopLethal(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)
                return;
            Arena arena = Arena.arenaOf(player).orElseThrow();
            double health = player.getHealth() - event.getFinalDamage();
            if (health <= 0) {
                event.setCancelled(true);
                if (!arena.isStarted()) {
                    player.teleport(arena.getLobby());
                    return;
                } else if (!arena.hasWallFallen()) {
                    player.teleport(arena.getSpawnFor(player));
                    return;
                }
                player.setGameMode(GameMode.SPECTATOR);
                arena.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "NOTICE: " + ChatColor.WHITE + player.getName()
                        + " has been eliminated!");
                player.sendMessage(ChatColor.RED + "You have been eliminated");
                player.sendMessage(ChatColor.GOLD + "You may spectate or do /leaveRvB if you wish to leave!");
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, arena::checkEnding, 5);
                return;
            }
        }
    }

    @EventHandler
    public void noBeds(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.hasBlock())
            return;
        Block block = event.getClickedBlock();
        if (!(block.getBlockData() instanceof Bed))
            return;
        if (!Arena.arenaOf(player).isPresent())
            return;
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "No Time for Sleep!");
    }

    @EventHandler
    public void stopPLethal(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Arena arena = Arena.arenaOf(player).orElseThrow();
        double health = player.getHealth() - event.getFinalDamage();
        if (health >= 0) return;
        event.setCancelled(true);
        if (!arena.isStarted()) {
            player.teleport(arena.getLobby());
        } else if (!arena.hasWallFallen()) {
            player.teleport(arena.getSpawnFor(player));
        } else {
            player.sendMessage(ChatColor.RED + "You have been eliminated!");
            String msg = ChatColor.GOLD + "" + ChatColor.BOLD + "NOTICE: " + ChatColor.WHITE + player.getName()
                    + " has been eliminated by ";
            if (event.getDamager() instanceof LivingEntity living) {
                msg += living.getName();
            } else if (event.getDamager() instanceof Projectile proj
                    && proj.getShooter() instanceof LivingEntity living)
                msg += living.getName() + "!";
            arena.broadcast(msg);
            player.setGameMode(GameMode.SPECTATOR);
            arena.checkEnding();

        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        var oa = Arena.arenaOf(player);
        if (!oa.isPresent())
            return;
        Arena a = oa.orElseThrow();
        if (!a.getLobby().getWorld().equals(event.getPlayer().getWorld()))
            a.removePlayer(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Arena a = Arena.arenaOf(player).orElseThrow();
        a.disconnectPlayer(player);
    }

    @EventHandler
    public void handlePlacement(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Arena a = Arena.arenaOf(player).orElseThrow();
        Block block = event.getBlock();
        boolean pass = a.isStarted() && a.tryEdit(player, block);
        if (!pass) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can't place blocks here!");
            return;
        }
        popPlace(block, a);
        if (!a.blockMap.containsKey(block.getLocation())) {
            BlockState old = event.getBlockReplacedState();
            Material oldMat = old.getType();
            BlockData oldData = old.getBlockData();
            DataPair pair = new DataPair(oldMat, oldData);
            a.blockMap.put(block.getLocation(), pair);
        }
    }

    @EventHandler
    public void handleBreaking(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Arena a = Arena.arenaOf(player).orElseThrow();
        Block block = event.getBlock();
        boolean pass = a.isStarted() && a.tryEdit(player, block);
        if (!pass) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "you can't break blocks here");
            return;
        }
        chainBlocks(block, a);
        if (!a.blockMap.containsKey(block.getLocation())) {
            Material oldMat = block.getType();
            BlockData oldData = block.getBlockData();
            DataPair pair = new DataPair(oldMat, oldData);
            a.blockMap.put(block.getLocation(), pair);
        }
    }

    @EventHandler
    public void handleDecay(BlockFadeEvent event) {
        Block block = event.getBlock();
        Material m = block.getType();
        Location loc = block.getLocation();
        if (m == Material.FIRE || m == Material.SOUL_FIRE)
            return;
        var ao = Arena.arenaOf(loc);
        if (!ao.isPresent()) return;
        Arena arena = ao.orElseThrow();
        if (!arena.blockMap.containsKey(loc)) {
            DataPair pair = new DataPair(m, block.getBlockData());
            arena.blockMap.put(loc, pair);
        }
    }

    @EventHandler
    public void handleFallingBlocks(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof FallingBlock fallingBlock) {
            Location bLoc = fallingBlock.getLocation().getBlock().getLocation();
            var ao = Arena.arenaOf(bLoc);
            if (!ao.isPresent()) return;
            Arena arena = ao.orElseThrow();
            chainBlocks(bLoc.getBlock(), arena);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (fallingBlock.isDead()) {
                        this.cancel();
                        return;
                    }
                    if (fallingBlock.isOnGround()) {
                        Block current = fallingBlock.getLocation().getBlock();
                        if (arena.hasLocation(current.getLocation())
                                && !arena.blockMap.containsKey(current.getLocation())) {
                            DataPair pair = new DataPair(current.getType(), current.getBlockData());
                            arena.blockMap.put(current.getLocation(), pair);
                        }
                        this.cancel();
                        return;
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
            if (!arena.blockMap.containsKey(bLoc)) {
                DataPair pair = new DataPair(fallingBlock.getBlockData().getMaterial(),
                        fallingBlock.getBlockData());
                arena.blockMap.put(bLoc, pair);
            }
        
        }
    }

    @EventHandler
    public void handleBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        for (Arena arena : Arena.arenas) {
            chainBlocks(block, arena);
            if (arena.hasLocation(block.getLocation()) && !arena.blockMap.containsKey(block.getLocation())) {
                DataPair pair = new DataPair(block.getType(), block.getBlockData());
                arena.blockMap.put(block.getLocation(), pair);
                return;
            }
        }
    }

    @EventHandler
    public void handleExplosians(EntityExplodeEvent event) {
        List<Block> affected = event.blockList();
        for (int a = affected.size() - 1; a >= 0; a--) {
            Block block = affected.get(a);
            var ao = Arena.arenaOf(block.getLocation());
            if (!ao.isPresent()) {
                ao = Arena.arenaOf(event.getLocation());
                if (ao.isPresent()) {
                    affected.remove(a);
                    break;
                }
                return;
            }
            Arena arena = ao.orElseThrow();
            if (!arena.isStarted())
                affected.remove(a);
            else {
                for (Wall wall : arena.getWalls()) {
                    if (wall.isOnWall(block.getLocation()) && !wall.isFallen())
                        affected.remove(a);
                    chainBlocks(block, arena);
                    if (!arena.blockMap.containsKey(block.getLocation())) {
                        DataPair pair = new DataPair(block.getType(), block.getBlockData());
                        arena.blockMap.put(block.getLocation(), pair);
                    }
                }
                break;
            }
        }
    }

    /**
     * MIGHT BE VOLATILE !!!!!!
     * Fail
     */
    // @EventHandler
    // public void handleBlockPhysics(BlockPhysicsEvent event) {
    // }

    // @EventHandler
    // public void recordBreakage(BlockBreakEvent event) {
    //     Block block = event.getBlock();
    // }

    public void chainBlocksUp(Block block, Arena arena) {
        Material mat = block.getType();
        Set<Material> stack = popStack();
        Set<Material> dirUp = popDirUp();
        if (stack.contains(mat)) {
            Location loc = block.getLocation().clone();
            int y = block.getY();
            Material m = loc.getBlock().getType();
            while (y < 256 && stack.contains(mat)) {
                if (m == mat) {
                    if (!arena.blockMap.containsKey(loc)) {
                        DataPair pair = new DataPair(m, loc.getBlock().getBlockData());
                        arena.blockMap.put(loc, pair);
                    }
                }
                loc = loc.clone().add(0, 1, 0);
                m = loc.getBlock().getType();
                y++;
            }
        } else if (dirUp.contains(mat)) {
            Location loc = block.getLocation();
            if (!arena.blockMap.containsKey(loc)) {
                DataPair pair = new DataPair(mat, loc.getBlock().getBlockData());
                arena.blockMap.put(loc, pair);
            }
        }
    }

    public void checkSides(Block block, Arena arena) {
        Set<Material> sideMats = popSides();
        BlockFace[] sides = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
        for (BlockFace side : sides) {
            Block b = block.getRelative(side);
            if (sideMats.contains(b.getType()) && arena.hasLocation(b.getLocation())
                    && !arena.blockMap.containsKey(b.getLocation())) {
                DataPair pair = new DataPair(b.getType(), b.getBlockData());
                arena.blockMap.put(b.getLocation(), pair);
            }
        }
    }

    public void chainBlocksDown(Block block, Arena arena) {
        Material mat = block.getType();
        Set<Material> stack = popDown();
        if (stack.contains(mat)) {
            Location loc = block.getLocation().clone();
            int y = block.getY();
            Material m = loc.getBlock().getType();
            while (y > 0 && stack.contains(mat)) {
                if (m == mat) {
                    if (!arena.blockMap.containsKey(loc)) {
                        DataPair pair = new DataPair(m, loc.getBlock().getBlockData());
                        arena.blockMap.put(loc, pair);
                    }
                }
                loc = loc.clone().add(0, -1, 0);
                m = loc.getBlock().getType();
                y--;
            }
        }
    }

    public void popPlace(Block block, Arena arena) {
        BlockFace[] sides = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
        for (BlockFace face : sides) {
            Block b = block.getRelative(face);
            if (!block.getType().isAir() && b.getType() == Material.CACTUS) {
                if (arena.hasLocation(b.getLocation()) && !arena.blockMap.containsKey(b.getLocation())) {
                    DataPair pair = new DataPair(Material.CACTUS, b.getBlockData());
                    arena.blockMap.put(b.getLocation(), pair);
                }
            }
        }
    }

    public void chainBlocks(Block block, Arena arena) {
        chainBlocksDown(block.getRelative(BlockFace.DOWN), arena);
        chainBlocksUp(block.getRelative(BlockFace.UP), arena);
        checkSides(block, arena);
    }

    private Set<Material> popStack() {
        Set<Material> mats = new HashSet<>();
        mats.add(Material.FLOWER_POT);
        mats.add(Material.MOSS_CARPET);
        mats.add(Material.SUGAR_CANE);
        mats.add(Material.CACTUS);
        mats.add(Material.CYAN_CARPET);
        mats.add(Material.BLACK_CARPET);
        mats.add(Material.MAGENTA_CARPET);
        mats.add(Material.RED_CARPET);
        mats.add(Material.GREEN_CARPET);
        mats.add(Material.BLUE_CARPET);
        mats.add(Material.LIME_CARPET);
        mats.add(Material.PURPLE_CARPET);
        mats.add(Material.LIGHT_BLUE_CARPET);
        mats.add(Material.ORANGE_CARPET);
        mats.add(Material.PINK_CARPET);
        mats.add(Material.GRAY_CARPET);
        mats.add(Material.LIGHT_GRAY_CARPET);
        mats.add(Material.WHITE_CARPET);
        mats.add(Material.BROWN_CARPET);
        mats.add(Material.BIG_DRIPLEAF);
        mats.add(Material.BIG_DRIPLEAF_STEM);
        mats.add(Material.POINTED_DRIPSTONE);
        mats.add(Material.SEAGRASS);
        mats.add(Material.KELP);
        mats.add(Material.KELP_PLANT);
        return mats;
    }

    private Set<Material> popDown() {
        Set<Material> mats = new HashSet<>();
        mats.add(Material.VINE);
        mats.add(Material.CAVE_VINES);
        mats.add(Material.CAVE_VINES_PLANT);
        mats.add(Material.WEEPING_VINES_PLANT);
        mats.add(Material.WEEPING_VINES);
        mats.add(Material.TWISTING_VINES);
        mats.add(Material.TWISTING_VINES_PLANT);
        mats.add(Material.BIG_DRIPLEAF);
        mats.add(Material.BIG_DRIPLEAF_STEM);
        mats.add(Material.POINTED_DRIPSTONE);
        mats.add(Material.SPORE_BLOSSOM);
        return mats;
    }

    public Set<Material> popDirUp() {
        Set<Material> mats = new HashSet<>(popStack());
        mats.add(Material.TORCH);
        mats.add(Material.SOUL_TORCH);
        mats.add(Material.LANTERN);
        mats.add(Material.SOUL_LANTERN);
        mats.add(Material.TALL_GRASS);
        mats.add(Material.SUNFLOWER);
        mats.add(Material.LARGE_FERN);
        mats.add(Material.LILAC);
        mats.add(Material.ROSE_BUSH);
        mats.add(Material.PEONY);
        mats.add(Material.SEA_PICKLE);
        mats.add(Material.CANDLE);
        mats.add(Material.RED_CANDLE);
        mats.add(Material.BLUE_CANDLE);
        mats.add(Material.GREEN_CANDLE);
        mats.add(Material.YELLOW_CANDLE);
        mats.add(Material.CYAN_CANDLE);
        mats.add(Material.MAGENTA_CANDLE);
        mats.add(Material.LIME_CANDLE);
        mats.add(Material.WHITE_CANDLE);
        mats.add(Material.ORANGE_CANDLE);
        mats.add(Material.GRAY_CANDLE);
        mats.add(Material.LIGHT_GRAY_CANDLE);
        mats.add(Material.LIGHT_BLUE_CANDLE);
        mats.add(Material.PURPLE_CANDLE);
        mats.add(Material.BLACK_CANDLE);
        mats.add(Material.WARPED_BUTTON);
        mats.add(Material.BIRCH_BUTTON);
        mats.add(Material.JUNGLE_BUTTON);
        mats.add(Material.ACACIA_BUTTON);
        mats.add(Material.DARK_OAK_BUTTON);
        mats.add(Material.OAK_BUTTON);
        mats.add(Material.CRIMSON_BUTTON);
        mats.add(Material.SPRUCE_BUTTON);
        mats.add(Material.MANGROVE_BUTTON);
        mats.add(Material.STONE_BUTTON);
        mats.add(Material.POLISHED_BLACKSTONE_BUTTON);
        mats.add(Material.WARPED_PRESSURE_PLATE);
        mats.add(Material.BIRCH_PRESSURE_PLATE);
        mats.add(Material.JUNGLE_PRESSURE_PLATE);
        mats.add(Material.ACACIA_PRESSURE_PLATE);
        mats.add(Material.DARK_OAK_PRESSURE_PLATE);
        mats.add(Material.OAK_PRESSURE_PLATE);
        mats.add(Material.CRIMSON_PRESSURE_PLATE);
        mats.add(Material.SPRUCE_PRESSURE_PLATE);
        mats.add(Material.MANGROVE_PRESSURE_PLATE);
        mats.add(Material.STONE_PRESSURE_PLATE);
        mats.add(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE);
        mats.add(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        mats.add(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
        mats.add(Material.POPPY);
        mats.add(Material.DANDELION);
        mats.add(Material.BLUE_ORCHID);
        mats.add(Material.ALLIUM);
        mats.add(Material.AZURE_BLUET);
        mats.add(Material.RED_TULIP);
        mats.add(Material.ORANGE_TULIP);
        mats.add(Material.PINK_TULIP);
        mats.add(Material.WHITE_TULIP);
        mats.add(Material.OXEYE_DAISY);
        mats.add(Material.CORNFLOWER);
        mats.add(Material.LILY_OF_THE_VALLEY);
        mats.add(Material.BROWN_MUSHROOM);
        mats.add(Material.RED_MUSHROOM);
        mats.add(Material.WARPED_FUNGUS);
        mats.add(Material.CRIMSON_FUNGUS);
        mats.add(Material.WITHER_ROSE);
        mats.add(Material.LEVER);
        mats.add(Material.RAIL);
        mats.add(Material.ACTIVATOR_RAIL);
        mats.add(Material.DETECTOR_RAIL);
        mats.add(Material.POWERED_RAIL);
        mats.add(Material.REDSTONE);
        mats.add(Material.REPEATER);
        mats.add(Material.COMPARATOR);
        mats.add(Material.REDSTONE_TORCH);
        return mats;
    }

    public Set<Material> popSides() {
        Set<Material> mats = new HashSet<>();
        mats.add(Material.LEVER);
        mats.add(Material.WARPED_BUTTON);
        mats.add(Material.BIRCH_BUTTON);
        mats.add(Material.JUNGLE_BUTTON);
        mats.add(Material.ACACIA_BUTTON);
        mats.add(Material.DARK_OAK_BUTTON);
        mats.add(Material.OAK_BUTTON);
        mats.add(Material.CRIMSON_BUTTON);
        mats.add(Material.SPRUCE_BUTTON);
        mats.add(Material.MANGROVE_BUTTON);
        mats.add(Material.STONE_BUTTON);
        mats.add(Material.ITEM_FRAME);
        mats.add(Material.GLOW_ITEM_FRAME);
        mats.add(Material.WALL_TORCH);
        mats.add(Material.SOUL_WALL_TORCH);
        mats.add(Material.REDSTONE_WALL_TORCH);
        mats.add(Material.TRIPWIRE_HOOK);
        mats.add(Material.COCOA);
        return mats;
    }

    @EventHandler
    public void handleWaterDamage(BlockFromToEvent event) {
        Block to = event.getToBlock();
        Material tMat = to.getType();
        for (Arena arena : Arena.arenas) {
            if (arena.hasLocation(to.getLocation())) {
                chainBlocks(to, arena);
                if (!arena.blockMap.containsKey(to.getLocation())) {
                    DataPair pair = new DataPair(tMat, to.getBlockData());
                    arena.blockMap.put(to.getLocation(), pair);
                    return;
                }
            }
        }
    }
}
