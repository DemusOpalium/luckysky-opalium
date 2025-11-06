package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.model.Settings;
import de.opalium.luckysky.model.Settings.PlatformSpec;
import de.opalium.luckysky.model.Settings.Vec3;
import de.opalium.luckysky.util.Worlds;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.BlockFace;

public class PlatformService {
    private final LuckySkyPlugin plugin;

    public PlatformService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void placeBase() {
        PlatformBuilder builder = newBuilder();
        Settings settings = plugin.settings();
        builder.placeBase(settings.platformHalfSize());
    }

    public void placeExtended() {
        PlatformBuilder builder = newBuilder();
        Settings settings = plugin.settings();
        int baseHalfSize = settings.platformHalfSize();
        builder.placeBase(baseHalfSize);
        builder.extend(baseHalfSize + 1);
    }

    private PlatformBuilder newBuilder() {
        Settings settings = plugin.settings();
        World world = Worlds.require(settings.world);
        PlatformSpec spec = settings.platform();
        Vec3 center = spec.center();
        return new PlatformBuilder(world, center.x(), center.y(), center.z());
    }

    private static final class PlatformBuilder {
        private final World world;
        private final int centerX;
        private final int centerY;
        private final int centerZ;

        private PlatformBuilder(World world, int centerX, int centerY, int centerZ) {
            this.world = world;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
        }

        private void placeBase(int halfSize) {
            BlockData base = Material.SMOOTH_STONE.createBlockData();
            fill(centerX - halfSize, centerY, centerZ - halfSize,
                    centerX + halfSize, centerY, centerZ + halfSize, base);

            BlockData rim = Material.STONE_BRICKS.createBlockData();
            line(centerX - halfSize, centerY + 1, centerZ - halfSize,
                    centerX + halfSize, centerY + 1, centerZ - halfSize, rim);
            line(centerX - halfSize, centerY + 1, centerZ + halfSize,
                    centerX + halfSize, centerY + 1, centerZ + halfSize, rim);
            line(centerX - halfSize, centerY + 1, centerZ - halfSize,
                    centerX - halfSize, centerY + 1, centerZ + halfSize, rim);
            line(centerX + halfSize, centerY + 1, centerZ - halfSize,
                    centerX + halfSize, centerY + 1, centerZ + halfSize, rim);

            placeStairs(halfSize);
        }

        private void extend(int halfSize) {
            BlockData floor = Material.PRISMARINE_BRICKS.createBlockData();
            squareEdge(halfSize, centerY, floor);

            BlockData rim = Material.STONE_BRICKS.createBlockData();
            squareEdge(halfSize, centerY + 1, rim);

            placeStairs(halfSize);
        }

        private void placeStairs(int halfSize) {
            Stairs north = (Stairs) Material.PRISMARINE_STAIRS.createBlockData();
            north.setFacing(BlockFace.SOUTH);
            north.setHalf(Stairs.Half.BOTTOM);
            north.setShape(Stairs.Shape.STRAIGHT);
            line(centerX - halfSize, centerY + 1, centerZ - halfSize - 1,
                    centerX + halfSize, centerY + 1, centerZ - halfSize - 1, north);

            Stairs south = (Stairs) north.clone();
            south.setFacing(BlockFace.NORTH);
            line(centerX - halfSize, centerY + 1, centerZ + halfSize + 1,
                    centerX + halfSize, centerY + 1, centerZ + halfSize + 1, south);

            Stairs west = (Stairs) north.clone();
            west.setFacing(BlockFace.EAST);
            line(centerX - halfSize - 1, centerY + 1, centerZ - halfSize,
                    centerX - halfSize - 1, centerY + 1, centerZ + halfSize, west);

            Stairs east = (Stairs) north.clone();
            east.setFacing(BlockFace.WEST);
            line(centerX + halfSize + 1, centerY + 1, centerZ - halfSize,
                    centerX + halfSize + 1, centerY + 1, centerZ + halfSize, east);
        }

        private void set(int x, int y, int z, BlockData data) {
            Block block = world.getBlockAt(x, y, z);
            block.setBlockData(data, false);
        }

        private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockData data) {
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        set(x, y, z, data);
                    }
                }
            }
        }

        private void line(int x1, int y1, int z1, int x2, int y2, int z2, BlockData data) {
            if (x1 == x2 && y1 == y2) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x1, y1, z, data);
                }
            } else if (z1 == z2 && y1 == y2) {
                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                    set(x, y1, z1, data);
                }
            } else if (x1 == x2 && z1 == z2) {
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                    set(x1, y, z1, data);
                }
            }
        }

        private void squareEdge(int halfSize, int y, BlockData data) {
            line(centerX - halfSize, y, centerZ - halfSize,
                    centerX + halfSize, y, centerZ - halfSize, data);
            line(centerX - halfSize, y, centerZ + halfSize,
                    centerX + halfSize, y, centerZ + halfSize, data);
            line(centerX - halfSize, y, centerZ - halfSize,
                    centerX - halfSize, y, centerZ + halfSize, data);
            line(centerX + halfSize, y, centerZ - halfSize,
                    centerX + halfSize, y, centerZ + halfSize, data);
        }
    }
}
