package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.util.Worlds;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class PlatformService {
    private final LuckySkyPlugin plugin;

    public PlatformService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void placeBase() {
        GameConfig game = plugin.configs().game();
        World world = Worlds.require(plugin.configs().worlds().luckySky().worldName());
        for (GameConfig.Block block : game.platform().baseBlocks()) {
            BlockData data = block.data() == null || block.data().isBlank()
                    ? Material.valueOf(block.material().toUpperCase()).createBlockData()
                    : Material.valueOf(block.material().toUpperCase()).createBlockData(block.data());
            world.getBlockAt(block.x(), block.y(), block.z()).setBlockData(data, false);
        }
        if (game.platform().bigPlatform()) {
            fill3x3(world, game);
        }
    }

    public void placeExtended() {
        GameConfig game = plugin.configs().game();
        World world = Worlds.require(plugin.configs().worlds().luckySky().worldName());
        fill3x3(world, game);
    }

    private List<Block> fill3x3(World world, GameConfig game) {
        List<Block> blocks = new ArrayList<>();
        int y = platformY(game);
        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z <= 2; z++) {
                Block block = world.getBlockAt(x, y, z);
                block.setType(Material.PRISMARINE_BRICKS, false);
                blocks.add(block);
            }
        }
        return blocks;
    }

    private int platformY(GameConfig game) {
        if (!game.platform().baseBlocks().isEmpty()) {
            return game.platform().baseBlocks().get(0).y();
        }
        return game.lucky().position().y();
    }
}
