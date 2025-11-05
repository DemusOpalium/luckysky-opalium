package de.opalium.luckysky.core;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.util.ConfigKeys;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class WipeService {
    private final LuckySkyPlugin plugin;

    public WipeService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public int performSoftWipe() {
        return performWipe(false);
    }

    public int performHardWipe() {
        int removed = performWipe(true);
        plugin.getWitherService().despawnWither();
        return removed;
    }

    private int performWipe(boolean hard) {
        Optional<World> optionalWorld = plugin.getGameWorld();
        if (optionalWorld.isEmpty()) {
            return 0;
        }
        World world = optionalWorld.get();
        Location center = plugin.getPlatformBuilder().getPlatformCenter(world).clone();
        double range = plugin.getConfigData().getDouble(hard ? ConfigKeys.WIPES_HARD_DISTANCE : ConfigKeys.WIPES_SOFT_DISTANCE, hard ? 5000D : 300D);
        List<String> typeNames = plugin.getConfigData().getStringList(hard ? ConfigKeys.WIPES_HARD_KILL : ConfigKeys.WIPES_SOFT_KILL);
        Set<String> normalized = new HashSet<>();
        for (String name : typeNames) {
            normalized.add(name.toLowerCase());
        }

        Collection<Entity> nearby = world.getNearbyEntities(center, range, range, range);
        int removed = 0;
        for (Entity entity : nearby) {
            if (entity instanceof Player) {
                continue;
            }
            EntityType type = entity.getType();
            String key = type.getKey().toString().toLowerCase();
            if (normalized.contains(key)) {
                entity.remove();
                removed++;
            }
        }
        return removed;
    }
}
