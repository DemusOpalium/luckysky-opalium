package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.model.Settings;
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
        Settings settings = plugin.settings();
        World world = Worlds.require(settings.world);
        int removed = 0;
        for (Entity entity : world.getEntities()) {
            if (isSoftTarget(entity.getType())
                    && entity.getLocation().toVector().distanceSquared(settings.luckyCenter()) <= square(settings.wipeRadius)) {
                entity.remove();
                removed++;
            }
        }
        return removed;
    }

    public int hardWipe() {
        Settings settings = plugin.settings();
        World world = Worlds.require(settings.world);
        int removed = 0;
        for (Entity entity : world.getEntities()) {
            if (!isHardTarget(entity.getType())) {
                continue;
            }
            double distance = entity.getLocation().toVector().distanceSquared(settings.luckyCenter());
            if (entity.getType() == EntityType.ARMOR_STAND
                    && distance > square(settings.armorstandRadius)) {
                continue;
            }
            if (distance <= square(settings.hardRadius)) {
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
