package de.opalium.luckysky.core;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.util.ConfigKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class CorridorCleaner {
    private final LuckySkyPlugin plugin;

    public CorridorCleaner(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void clearCorridor() {
        plugin.getGameWorld().ifPresent(world -> {
            Location center = plugin.getPlatformBuilder().getPlatformBlockLocation(world);
            int radius = plugin.getConfigData().getInt(ConfigKeys.CORRIDOR_RADIUS, 6);
            int height = plugin.getConfigData().getInt(ConfigKeys.CORRIDOR_HEIGHT, 8);
            int baseY = center.getBlockY();

            for (int x = center.getBlockX() - radius; x <= center.getBlockX() + radius; x++) {
                for (int z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; z++) {
                    for (int y = baseY; y <= baseY + height; y++) {
                        world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    }
                }
            }
        });
    }

}
