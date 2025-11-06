package de.opalium.luckysky.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

public final class Worlds {
    private Worlds() {
    }

    public static World require(String name) {
        World world = Bukkit.getWorld(name);
        if (world == null) {
            throw new IllegalStateException("World not loaded: " + name);
        }
        return world;
    }
}
