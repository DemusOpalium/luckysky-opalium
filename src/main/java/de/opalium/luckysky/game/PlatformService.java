package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.model.WorldsCfg;
import de.opalium.luckysky.config.model.WorldsCfg.Block;
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
        WorldsCfg.WorldCfg worldCfg = plugin.configs().worlds().primary();
        WorldsCfg.Platform platform = worldCfg.platform();
        World world = Worlds.require(worldCfg.worldName());
        for (Block block : platform.baseBlocks()) {
            BlockData data = block.data() == null || block.data().isBlank()
                    ? Material.valueOf(block.type()).createBlockData()
                    : Material.valueOf(block.type()).createBlockData(block.data());
            world.getBlockAt(block.x(), block.y(), block.z()).setBlockData(data, false);
        }
        if (platform.big3x3()) {
            fill3x3(world, platform.baseBlocks());
        }
    }

    public void placeExtended() {
        WorldsCfg.WorldCfg worldCfg = plugin.configs().worlds().primary();
        WorldsCfg.Platform platform = worldCfg.platform();
        World world = Worlds.require(worldCfg.worldName());
        fill3x3(world, platform.baseBlocks());
    }

    private void fill3x3(World world, java.util.List<Block> blocks) {
        int y = blocks.isEmpty() ? 100 : blocks.get(0).y();
        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z <= 2; z++) {
                world.getBlockAt(x, y, z).setType(Material.PRISMARINE_BRICKS, false);
            }
        }
    }
}
