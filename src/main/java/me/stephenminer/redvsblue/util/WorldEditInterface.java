package me.stephenminer.redvsblue.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.bukkit.Material;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

public class WorldEditInterface {
    public static BlockType AIR = BlockTypes.AIR;

    public static CuboidRegion toCuboidRegion(BlockRange range) {
        return new CuboidRegion(
                BukkitAdapter.adapt(range.world()),
                BlockVector3.at(range.p1().getX(), range.p1().getY(), range.p1().getZ()),
                BlockVector3.at(range.p2().getX(), range.p2().getY(), range.p2().getZ()));
    }

    public static BlockType toBlockType(Material m) {
        return BukkitAdapter.asBlockType(m);
    }

    public static CompletableFuture<BlockArrayClipboard> copy(CuboidRegion region) {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        return CompletableFuture.supplyAsync(() -> {
            try (EditSession session = WorldEdit.getInstance().newEditSession(region.getWorld())) {
                ForwardExtentCopy op = new ForwardExtentCopy(session, region, clipboard,
                        region.getMinimumPoint());
                op.setCopyingEntities(false);

                Operations.complete(op);
                clipboard.close();

                return clipboard;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Operation> paste(CuboidRegion target, BlockArrayClipboard clipboard) {
        return CompletableFuture.supplyAsync(() -> {
            try (ClipboardHolder holder = new ClipboardHolder(clipboard)) {
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(target.getWorld())) {
                    Operation op = holder.createPaste(editSession)
                            .to(target.getMinimumPoint())
                            .ignoreAirBlocks(false).build();
                    Operations.complete(op);
                    return op;
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Void> fill(BlockRange range, Material mat) {
        return fill(WorldEditInterface.toCuboidRegion(range), WorldEditInterface.toBlockType(mat));
    }

    public static CompletableFuture<Void> fill(CuboidRegion target, BlockType m) {
        return CompletableFuture.runAsync(() -> {
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(target.getWorld())) {
                editSession.setBlocks((Region) target, m);
            }
        });
    }
}
