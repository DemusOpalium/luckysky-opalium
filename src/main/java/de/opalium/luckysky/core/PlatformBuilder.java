package de.opalium.luckysky.core;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.util.ConfigKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;

import java.util.List;
import java.util.Optional;

public class PlatformBuilder {
    private final LuckySkyPlugin plugin;

    public PlatformBuilder(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void buildBase() {
        plugin.getGameWorld().ifPresent(world -> {
            BlockData blockData = parseMaterial();
            Location center = getPlatformBlockLocation(world);
            setBlock(world, center, blockData);

            int radius = Math.max(1, plugin.getConfigData().getInt(ConfigKeys.PLATFORM_SIZE_BASE, 1));
            for (int i = 1; i <= radius; i++) {
                setBlock(world, center.clone().add(i, 0, 0), blockData);
                setBlock(world, center.clone().add(-i, 0, 0), blockData);
                setBlock(world, center.clone().add(0, 0, i), blockData);
                setBlock(world, center.clone().add(0, 0, -i), blockData);
            }
        });
    }

    public void buildExpanded() {
        plugin.getGameWorld().ifPresent(world -> {
            buildBase();
            BlockData blockData = parseMaterial();
            Location center = getPlatformBlockLocation(world);
            int size = Math.max(1, plugin.getConfigData().getInt(ConfigKeys.PLATFORM_SIZE_PLUS, 3));
            int radius = Math.max(1, size / 2);
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    setBlock(world, center.clone().add(x, 0, z), blockData);
                }
            }
        });
    }

    public void placeInfoSign() {
        if (!plugin.getConfigData().getBoolean(ConfigKeys.SIGN_ENABLED, true)) {
            return;
        }
        plugin.getGameWorld().ifPresent(world -> {
            Location center = getPlatformBlockLocation(world);
            List<Integer> offset = plugin.getConfigData().getIntegerList(ConfigKeys.SIGN_OFFSET);
            int offX = offset.size() > 0 ? offset.get(0) : 0;
            int offY = offset.size() > 1 ? offset.get(1) : 0;
            int offZ = offset.size() > 2 ? offset.get(2) : -2;
            Location signLocation = center.clone().add(offX, offY, offZ);

            Block block = world.getBlockAt(signLocation);
            block.setType(Material.OAK_SIGN, false);
            BlockData data = block.getBlockData();
            if (data instanceof org.bukkit.block.data.type.Sign signData) {
                signData.setRotation(BlockFace.SOUTH);
                block.setBlockData(signData, false);
            }

            if (block.getState() instanceof Sign sign) {
                List<String> lines = plugin.getConfigData().getStringList(ConfigKeys.SIGN_LINES);
                for (int i = 0; i < Math.min(4, lines.size()); i++) {
                    sign.setLine(i, org.bukkit.ChatColor.translateAlternateColorCodes('&', lines.get(i)));
                }
                sign.update(true, false);
            }
        });
    }

    public Location getPlatformCenter(World world) {
        Location location = getPlatformBlockLocation(world);
        return location.clone().add(0.5, 1, 0.5);
    }

    public Location getPlatformBlockLocation(World world) {
        List<Integer> center = plugin.getConfigData().getIntegerList(ConfigKeys.PLATFORM_CENTER);
        int x = center.size() > 0 ? center.get(0) : 0;
        int z = center.size() > 1 ? center.get(1) : 0;
        int y = plugin.getConfigData().getInt(ConfigKeys.PLATFORM_Y, 201);
        return new Location(world, x, y, z);
    }

    private BlockData parseMaterial() {
        String materialName = plugin.getConfigData().getString(ConfigKeys.PLATFORM_MATERIAL, "BARRIER");
        Material material = Optional.ofNullable(Material.matchMaterial(materialName))
                .orElse(Material.BARRIER);
        return material.createBlockData();
    }

    private void setBlock(World world, Location location, BlockData data) {
        Block block = world.getBlockAt(location);
        block.setBlockData(data, false);
    }
}
