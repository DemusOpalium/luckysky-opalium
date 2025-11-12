package de.opalium.luckysky.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public record GateConfig(String id, String executorPermission, Target target,
                         List<String> openCommands, List<String> closeCommands) {
    private static final GateConfig DEFAULT = new GateConfig(
            "backspawn",
            "opalium.luckysky.admin",
            new Target("LuckySky", 0.0D, 101.0D, 2.0D, 0f, 0f),
            List.of(
                    "mvp create {id}",
                    "mvp select {id}",
                    "mvp modify dest e:{target.world}:{target.x},{target.y},{target.z}"
            ),
            List.of("mvp remove {id}")
    );

    public static GateConfig from(ConfigurationSection section) {
        if (section == null) {
            return DEFAULT;
        }
        String id = section.getString("id", DEFAULT.id);
        String permission = section.getString("executor-permission",
                section.getString("executor_permission", DEFAULT.executorPermission));
        Target target = Target.from(section.getConfigurationSection("target"));
        List<String> open = readCommands(section.getConfigurationSection("open"), "commands", DEFAULT.openCommands);
        List<String> close = readCommands(section.getConfigurationSection("close"), "commands", DEFAULT.closeCommands);
        return new GateConfig(id, permission, target, open, close);
    }

    private static List<String> readCommands(ConfigurationSection section, String key, List<String> fallback) {
        if (section == null) {
            return fallback;
        }
        List<String> commands = section.getStringList(key);
        if (commands.isEmpty()) {
            commands = section.getStringList(key.replace('-', '_'));
        }
        if (commands.isEmpty()) {
            return fallback;
        }
        List<String> cleaned = new ArrayList<>();
        for (String command : commands) {
            String trimmed = command == null ? "" : command.trim();
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        if (cleaned.isEmpty()) {
            return fallback;
        }
        return Collections.unmodifiableList(cleaned);
    }

    public Map<String, String> placeholders() {
        Map<String, String> map = new HashMap<>();
        map.put("id", id);
        map.put("world", target.world());
        map.put("x", format(target.x()));
        map.put("y", format(target.y()));
        map.put("z", format(target.z()));
        map.put("yaw", format(target.yaw()));
        map.put("pitch", format(target.pitch()));
        map.put("target.world", target.world());
        map.put("target.x", format(target.x()));
        map.put("target.y", format(target.y()));
        map.put("target.z", format(target.z()));
        map.put("target.yaw", format(target.yaw()));
        map.put("target.pitch", format(target.pitch()));
        return Collections.unmodifiableMap(map);
    }

    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private String format(float value) {
        if (Math.abs(value - Math.round(value)) < 1e-6) {
            return Integer.toString(Math.round(value));
        }
        return Float.toString(value);
    }

    public void writeTo(ConfigurationSection section) {
        section.set("id", id);
        section.set("executor-permission", executorPermission);
        ConfigurationSection targetSection = section.createSection("target");
        target.writeTo(targetSection);
        ConfigurationSection openSection = section.createSection("open");
        openSection.set("commands", openCommands);
        ConfigurationSection closeSection = section.createSection("close");
        closeSection.set("commands", closeCommands);
    }

    public record Target(String world, double x, double y, double z, float yaw, float pitch) {
        public static Target from(ConfigurationSection section) {
            if (section == null) {
                return DEFAULT.target;
            }
            String world = section.getString("world", DEFAULT.target.world());
            double x = section.getDouble("x", DEFAULT.target.x());
            double y = section.getDouble("y", DEFAULT.target.y());
            double z = section.getDouble("z", DEFAULT.target.z());
            float yaw = (float) section.getDouble("yaw", DEFAULT.target.yaw());
            float pitch = (float) section.getDouble("pitch", DEFAULT.target.pitch());
            return new Target(world, x, y, z, yaw, pitch);
        }

        private void writeTo(ConfigurationSection section) {
            section.set("world", world);
            section.set("x", x);
            section.set("y", y);
            section.set("z", z);
            section.set("yaw", yaw);
            section.set("pitch", pitch);
        }
    }
}
