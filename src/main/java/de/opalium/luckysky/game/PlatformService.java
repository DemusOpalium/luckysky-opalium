package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.model.Settings;
import de.opalium.luckysky.model.Settings.PBlock;
import de.opalium.luckysky.util.Worlds;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

public class PlatformService {
    private final LuckySkyPlugin plugin;

    public PlatformService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void placeBase() {
        Settings settings = plugin.settings();
        World world = Worlds.require(settings.world);
        for (PBlock block : settings.getPlatformBlocks()) {
            BlockData data = block.data() == null || block.data().isBlank()
                    ? Material.valueOf(block.type()).createBlockData()
                    : Material.valueOf(block.type()).createBlockData(block.data());
            world.getBlockAt(block.x(), block.y(), block.z()).setBlockData(data, false);
        }
        if (settings.platformBig) {
            fill3x3(world, settings);
        }
    }

    public void placeExtended() {
        Settings settings = plugin.settings();
        World world = Worlds.require(settings.world);
        fill3x3(world, settings);
    }

    private void fill3x3(World world, Settings settings) {
        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z <= 2; z++) {
                world.getBlockAt(x, settings.platformY(), z).setType(Material.PRISMARINE_BRICKS, false);
            }
        }
    }
}
