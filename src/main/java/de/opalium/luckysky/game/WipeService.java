package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.model.GameConfig;
import de.opalium.luckysky.config.model.WorldsCfg;
import de.opalium.luckysky.util.Worlds;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

public class WipeService {
    private final LuckySkyPlugin plugin;

    public WipeService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public int fieldClearSoft() {
        WorldsCfg.WorldCfg worldCfg = plugin.configs().worlds().primary();
        GameConfig.WipeConfig wipes = plugin.configs().game().wipes();
        Vector luckyCenter = new Vector(worldCfg.lucky().x(), worldCfg.lucky().y(), worldCfg.lucky().z());
        World world = Worlds.require(worldCfg.worldName());
        int removed = 0;
        for (Entity entity : world.getEntities()) {
            if (isSoftTarget(entity.getType())
                    && entity.getLocation().toVector().distanceSquared(luckyCenter) <= square(wipes.radius())) {
                entity.remove();
                removed++;
            }
        }
        return removed;
    }

    public int hardWipe() {
        WorldsCfg.WorldCfg worldCfg = plugin.configs().worlds().primary();
        GameConfig.WipeConfig wipes = plugin.configs().game().wipes();
        GameConfig.HardWipeConfig hard = wipes.hardwipe();
        Vector luckyCenter = new Vector(worldCfg.lucky().x(), worldCfg.lucky().y(), worldCfg.lucky().z());
        World world = Worlds.require(worldCfg.worldName());
        int removed = 0;
        for (Entity entity : world.getEntities()) {
            if (!isHardTarget(entity.getType())) {
                continue;
            }
            double distance = entity.getLocation().toVector().distanceSquared(luckyCenter);
            if (entity.getType() == EntityType.ARMOR_STAND
                    && distance > square(hard.armorstandRadius())) {
                continue;
            }
            if (distance <= square(hard.radius())) {
                entity.remove();
                removed++;
            }
        }
        return removed;
    }

    private boolean isSoftTarget(EntityType type) {
        return type == EntityType.AREA_EFFECT_CLOUD
                || type == EntityType.INTERACTION
                || type == EntityType.MARKER
                || type == EntityType.FALLING_BLOCK
                || type.name().endsWith("_DISPLAY");
    }

    private boolean isHardTarget(EntityType type) {
        return isSoftTarget(type) || type == EntityType.ARMOR_STAND;
    }

    private int square(int value) {
        return value * value;
    }
}
