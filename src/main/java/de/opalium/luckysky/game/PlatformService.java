package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.util.Worlds;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

public class PlatformService {
    private final LuckySkyPlugin plugin;

    public PlatformService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void placeBase() {
        GameConfig game = plugin.configs().game();
        World world = Worlds.require(plugin.configs().worlds().luckySky().worldName());
        placeConfiguredBlocks(world, game.platform().baseBlocks());
        if (game.platform().bigPlatform()) {
            placeBigPlatform(world, platformY(game));
        }
    }

    public void placeExtended() {
        placeBase();
    }

    public boolean isBaseIntact() {
        GameConfig game = plugin.configs().game();
        World world = Worlds.require(plugin.configs().worlds().luckySky().worldName());
        for (GameConfig.Block block : game.platform().baseBlocks()) {
            Material expected = Material.valueOf(block.material().toUpperCase());
            if (world.getBlockAt(block.x(), block.y(), block.z()).getType() != expected) {
                return false;
            }
        }
        return true;
    }

    private void placeConfiguredBlocks(World world, List<GameConfig.Block> blocks) {
        for (GameConfig.Block block : blocks) {
            Material material = Material.valueOf(block.material().toUpperCase());
            BlockData data = block.data() == null || block.data().isBlank()
                    ? material.createBlockData()
                    : material.createBlockData(block.data());
            world.getBlockAt(block.x(), block.y(), block.z()).setBlockData(data, false);
        }
    }

    private void placeBigPlatform(World world, int y) {
        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z <= 2; z++) {
                world.getBlockAt(x, y, z).setType(Material.PRISMARINE_BRICKS, false);
            }
        }
    }

    private int platformY(GameConfig game) {
        if (!game.platform().baseBlocks().isEmpty()) {
            return game.platform().baseBlocks().get(0).y();
        }
        return game.lucky().position().y();
    }
}
