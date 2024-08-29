package me.stephenminer.redvblue;

import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public record BlockRange(World world, BlockVector p1, BlockVector p2) {
    public void forEach(LocationCallback callback) {
        var min = BlockVector.getMinimum(p1, p2);
        var max = BlockVector.getMaximum(p1, p2);
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++)
        for (int y = min.getBlockY(); y <= max.getBlockY(); y++)
        for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++)
            callback.run(new Location(world, x, y, z));
    }

    public void fill(Material m) {
        forEach((Location l) -> l.getBlock().setType(m));
    }

    public BoundingBox toBoundingBox() {
        Vector offset = new Vector(0.5, 0.5, 0.5);
        return BoundingBox.of(p1.clone().add(offset), p2.clone().add(offset));
    }

    public static final BlockRange fromLocations(Location loc1, Location loc2) {
        assert loc1.getWorld() == loc2.getWorld();
        return new BlockRange(loc1.getWorld(), new BlockVector(loc1.toVector()), new BlockVector(loc2.toVector()));
    }

    private static final Pattern serializedPattern = Pattern.compile("(.*)/\\((\\d+),(\\d+),(\\d+)\\)~\\((\\d+),(\\d+),(\\d+)\\)");
    public static final BlockRange fromString(Server currentServer, String serialized) {
        var m = serializedPattern.matcher(serialized);
        assert m.matches();
        return new BlockRange(currentServer.getWorld(m.group(1)),
            new BlockVector(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4))),
            new BlockVector(Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)), Integer.parseInt(m.group(7)))
        );
    }

    @Override
    public final String toString() {
        return world.getName() + "/(" + p1.toString() + ")~(" + p2.toString() + ")";
    }
}
