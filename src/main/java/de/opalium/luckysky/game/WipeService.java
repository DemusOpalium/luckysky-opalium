package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.util.Worlds;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class WipeService {
    private final LuckySkyPlugin plugin;

    public WipeService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public int fieldClearSoft() {
        GameConfig game = plugin.configs().game();
        World world = Worlds.require(plugin.configs().worlds().luckySky().worldName());
        int removed = 0;
        GameConfig.Position center = game.lucky().position();
        org.bukkit.util.Vector centerVector = center.toVector();
        for (Entity entity : world.getEntities()) {
            if (isSoftTarget(entity.getType())
                    && entity.getLocation().toVector().distanceSquared(centerVector) <= square(game.wipes().softRadius())) {
                entity.remove();
                removed++;
            }
        }
        return removed;
    }

    public int hardWipe() {
        GameConfig game = plugin.configs().game();
        World world = Worlds.require(plugin.configs().worlds().luckySky().worldName());
        int removed = 0;
        GameConfig.Position center = game.lucky().position();
        org.bukkit.util.Vector centerVector = center.toVector();
        for (Entity entity : world.getEntities()) {
            if (!isHardTarget(entity.getType())) {
                continue;
            }
            double distance = entity.getLocation().toVector().distanceSquared(centerVector);
            if (entity.getType() == EntityType.ARMOR_STAND
                    && distance > square(game.wipes().armorstandRadius())) {
                continue;
            }
            if (distance <= square(game.wipes().hardRadius())) {
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
