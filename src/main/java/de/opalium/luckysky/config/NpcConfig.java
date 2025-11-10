package de.opalium.luckysky.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

public final class NpcConfig {
    private final Storage storage;
    private final Position parking;
    private final Map<String, Definition> definitions;

    private NpcConfig(Storage storage, Position parking, Map<String, Definition> definitions) {
        this.storage = storage;
        this.parking = parking;
        this.definitions = Collections.unmodifiableMap(definitions);
    }

    public static NpcConfig from(FileConfiguration config) {
        Storage storage = readStorage(config.getConfigurationSection("storage"));
        Position parking = readPosition(config.getConfigurationSection("parking"),
                new Position("LuckySkyStorage", 0.5, -58, 0.5, 0f, 0f));
        Map<String, Definition> definitions = new LinkedHashMap<>();
        ConfigurationSection npcsSection = config.getConfigurationSection("npcs");
        if (npcsSection != null) {
            for (String id : npcsSection.getKeys(false)) {
                ConfigurationSection section = npcsSection.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                Definition definition = readDefinition(id, section, parking);
                definitions.put(id, definition);
            }
        }
        return new NpcConfig(storage, parking, definitions);
    }

    public void writeTo(FileConfiguration config) {
        config.set("storage.world", storage.world());
        List<Map<String, Object>> chunkList = new ArrayList<>();
        for (Chunk chunk : storage.chunks()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("x", chunk.x());
            entry.put("z", chunk.z());
            chunkList.add(entry);
        }
        config.set("storage.chunks", chunkList);

        writePosition(config.createSection("parking"), parking);

        ConfigurationSection npcsSection = config.createSection("npcs");
        for (Definition definition : definitions.values()) {
            ConfigurationSection section = npcsSection.createSection(definition.id());
            section.set("display-name", definition.displayName());
            section.set("entity-type", definition.entityType().name());
            section.set("auto-summon", definition.autoSummon());
            writePosition(section.createSection("spawn"), definition.spawn());
            if (definition.skin().isPresent()) {
                Skin skin = definition.skin().get();
                ConfigurationSection skinSection = section.createSection("skin");
                skinSection.set("texture", skin.texture());
                skinSection.set("signature", skin.signature());
            }
            ConfigurationSection actionsSection = section.createSection("actions");
            writeAction(actionsSection, "admin", definition.actions().admin());
            writeAction(actionsSection, "player-idle", definition.actions().idle());
            writeAction(actionsSection, "player-running", definition.actions().running());
            section.set("start-commands", definition.startCommands());
            section.set("vote-commands", definition.voteCommands());
            section.set("collapse-commands", definition.collapseCommands());
            section.set("collapse-delay-ticks", definition.collapseDelayTicks());
            section.set("return-delay-ticks", definition.returnDelayTicks());
        }
    }

    public Storage storage() {
        return storage;
    }

    public Position parking() {
        return parking;
    }

    public Map<String, Definition> definitions() {
        return definitions;
    }

    public Optional<Definition> definition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public NpcConfig withUpdatedSpawn(String id, Position spawn) {
        Definition definition = definitions.get(id);
        if (definition == null) {
            return this;
        }
        Map<String, Definition> updated = new LinkedHashMap<>(definitions);
        updated.put(id, definition.withSpawn(spawn));
        return new NpcConfig(storage, parking, updated);
    }

    private static Storage readStorage(ConfigurationSection section) {
        if (section == null) {
            return new Storage("LuckySkyStorage", List.of());
        }
        String world = section.getString("world", "LuckySkyStorage");
        List<Chunk> chunks = new ArrayList<>();
        for (Object entry : section.getMapList("chunks")) {
            if (entry instanceof Map<?, ?> map) {
                int x = parseInt(map.get("x"), 0);
                int z = parseInt(map.get("z"), 0);
                chunks.add(new Chunk(x, z));
            }
        }
        return new Storage(world, Collections.unmodifiableList(chunks));
    }

    private static Definition readDefinition(String id, ConfigurationSection section, Position defaultParking) {
        String displayName = section.getString("display-name", id);
        String entityTypeRaw = section.getString("entity-type", "PLAYER");
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(entityTypeRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            entityType = EntityType.PLAYER;
        }
        boolean autoSummon = section.getBoolean("auto-summon", true);
        Position spawn = readPosition(section.getConfigurationSection("spawn"), defaultParking);
        Skin skin = readSkin(section.getConfigurationSection("skin"));
        Actions actions = readActions(section.getConfigurationSection("actions"));
        List<String> startCommands = copyList(section.getStringList("start-commands"));
        List<String> voteCommands = copyList(section.getStringList("vote-commands"));
        List<String> collapseCommands = copyList(section.getStringList("collapse-commands"));
        int collapseDelayTicks = section.getInt("collapse-delay-ticks", 120);
        int returnDelayTicks = section.getInt("return-delay-ticks", 200);
        return new Definition(id, displayName, entityType, autoSummon, spawn, Optional.ofNullable(skin),
                actions, startCommands, voteCommands, collapseCommands, collapseDelayTicks, returnDelayTicks);
    }

    private static Position readPosition(ConfigurationSection section, Position fallback) {
        if (section == null) {
            return fallback;
        }
        String world = section.getString("world", fallback.world());
        double x = section.getDouble("x", fallback.x());
        double y = section.getDouble("y", fallback.y());
        double z = section.getDouble("z", fallback.z());
        float yaw = (float) section.getDouble("yaw", fallback.yaw());
        float pitch = (float) section.getDouble("pitch", fallback.pitch());
        return new Position(world, x, y, z, yaw, pitch);
    }

    private static Skin readSkin(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String texture = section.getString("texture");
        String signature = section.getString("signature");
        if (texture == null || texture.isBlank()) {
            return null;
        }
        return new Skin(texture, signature != null ? signature : "");
    }

    private static Actions readActions(ConfigurationSection section) {
        if (section == null) {
            return new Actions(ClickBehavior.adminDefault(), ClickBehavior.playerStartDefault(),
                    ClickBehavior.playerVoteDefault());
        }
        ClickBehavior admin = readAction(section, "admin", ClickBehavior.adminDefault());
        ClickBehavior idle = readAction(section, "player-idle", ClickBehavior.playerStartDefault());
        ClickBehavior running = readAction(section, "player-running", ClickBehavior.playerVoteDefault());
        return new Actions(admin, idle, running);
    }

    private static ClickBehavior readAction(ConfigurationSection section, String key, ClickBehavior fallback) {
        if (section == null) {
            return fallback;
        }
        Object value = section.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof String stringValue) {
            return ClickBehavior.fromString(stringValue).orElse(fallback);
        }
        if (value instanceof ConfigurationSection child) {
            String typeRaw = child.getString("type", "NONE");
            ClickBehavior.ActionType type = ClickBehavior.parseType(typeRaw).orElse(fallback.type());
            List<String> commands = copyList(child.getStringList("commands"));
            return new ClickBehavior(type, commands);
        }
        if (value instanceof Map<?, ?> map) {
            Object typeObj = map.get("type");
            String typeRaw = typeObj != null ? Objects.toString(typeObj) : "NONE";
            ClickBehavior.ActionType type = ClickBehavior.parseType(typeRaw).orElse(fallback.type());
            List<String> commands = copyListFromMap(map.get("commands"));
            return new ClickBehavior(type, commands);
        }
        return fallback;
    }

    private static void writeAction(ConfigurationSection actionsSection, String key, ClickBehavior behavior) {
        if (behavior.commands().isEmpty()) {
            actionsSection.set(key, behavior.type().name());
        } else {
            ConfigurationSection section = actionsSection.createSection(key);
            section.set("type", behavior.type().name());
            section.set("commands", behavior.commands());
        }
    }

    private static void writePosition(ConfigurationSection section, Position position) {
        section.set("world", position.world());
        section.set("x", position.x());
        section.set("y", position.y());
        section.set("z", position.z());
        section.set("yaw", position.yaw());
        section.set("pitch", position.pitch());
    }

    private static List<String> copyList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    @SuppressWarnings("unchecked")
    private static List<String> copyListFromMap(Object source) {
        if (source instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object element : list) {
                out.add(Objects.toString(element));
            }
            return Collections.unmodifiableList(out);
        }
        return List.of();
    }

    private static int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    public record Storage(String world, List<Chunk> chunks) {
    }

    public record Chunk(int x, int z) {
    }

    public record Position(String world, double x, double y, double z, float yaw, float pitch) {
    }

    public record Skin(String texture, String signature) {
        public boolean isComplete() {
            return texture != null && !texture.isBlank();
        }
    }

    public record Actions(ClickBehavior admin, ClickBehavior idle, ClickBehavior running) {
    }

    public record Definition(
            String id,
            String displayName,
            EntityType entityType,
            boolean autoSummon,
            Position spawn,
            Optional<Skin> skin,
            Actions actions,
            List<String> startCommands,
            List<String> voteCommands,
            List<String> collapseCommands,
            int collapseDelayTicks,
            int returnDelayTicks) {

        public Definition withSpawn(Position spawn) {
            return new Definition(id, displayName, entityType, autoSummon, spawn, skin, actions,
                    startCommands, voteCommands, collapseCommands, collapseDelayTicks, returnDelayTicks);
        }
    }

    public record ClickBehavior(ActionType type, List<String> commands) {
        public static Optional<ClickBehavior> fromString(String raw) {
            return parseType(raw).map(type -> new ClickBehavior(type, List.of()));
        }

        public static Optional<ActionType> parseType(String raw) {
            if (raw == null) {
                return Optional.empty();
            }
            try {
                return Optional.of(ActionType.valueOf(raw.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }

        public static ClickBehavior adminDefault() {
            return new ClickBehavior(ActionType.GUI, List.of());
        }

        public static ClickBehavior playerStartDefault() {
            return new ClickBehavior(ActionType.START, List.of());
        }

        public static ClickBehavior playerVoteDefault() {
            return new ClickBehavior(ActionType.VOTE, List.of());
        }

        public enum ActionType {
            NONE,
            GUI,
            START,
            VOTE,
            COMMAND
        }
    }
}
