package de.opalium.luckysky.npc;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.NpcConfig;
import de.opalium.luckysky.config.NpcConfig.ClickBehavior;
import de.opalium.luckysky.config.NpcConfig.Definition;
import de.opalium.luckysky.config.NpcConfig.Position;
import de.opalium.luckysky.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;

public final class NpcDepot {
    private static final String NPC_KEY = "luckysky-npc-id";
    private static final Set<String> SUPPORTED_COMMANDS = Set.of("create", "summon", "recall", "bind", "delete");

    private final LuckySkyPlugin plugin;
    private final Map<String, ManagedNpc> managed = new HashMap<>();
    private final Set<ChunkCoordinate> forcedChunks = new HashSet<>();
    private final Set<UUID> voteParticipants = new HashSet<>();

    private NPCRegistry registry;
    private boolean available;

    public NpcDepot(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        try {
            if (!CitizensAPI.hasImplementation()) {
                plugin.getLogger().warning("Citizens ist nicht geladen – NPC-Depot deaktiviert.");
                available = false;
                return;
            }
        } catch (NoClassDefFoundError error) {
            plugin.getLogger().warning("Citizens-API nicht verfügbar – NPC-Depot deaktiviert.");
            available = false;
            return;
        }
        registry = CitizensAPI.getNPCRegistry();
        available = true;
        reload();
    }

    public void reload() {
        if (!available) {
            return;
        }
        clearAll();
        ensureChunksLoaded();
        voteParticipants.clear();
        NpcConfig config = plugin.configs().npc();
        for (Definition definition : config.definitions().values()) {
            ManagedNpc npc = createManaged(definition);
            managed.put(definition.id(), npc);
            if (definition.autoSummon()) {
                npc.spawnAtStage();
            } else {
                npc.sendToParking();
            }
        }
    }

    public void shutdown() {
        if (!available) {
            return;
        }
        clearAll();
        unloadChunks();
        managed.clear();
        voteParticipants.clear();
    }

    public boolean isAvailable() {
        return available;
    }

    public void handleRightClick(NPCRightClickEvent event) {
        if (!available) {
            return;
        }
        NPC npc = event.getNPC();
        ManagedNpc managedNpc = resolve(npc);
        if (managedNpc == null) {
            return;
        }
        Player player = event.getClicker();
        Definition definition = managedNpc.definition();
        if (player.hasPermission("opalium.luckysky.admin")) {
            handleBehavior(player, definition.actions().admin(), managedNpc, null);
            return;
        }
        boolean running = plugin.game() != null && plugin.game().state() == de.opalium.luckysky.game.GameState.RUNNING;
        ClickBehavior behavior = running ? definition.actions().running() : definition.actions().idle();
        handleBehavior(player, behavior, managedNpc, definition);
    }

    public void executeCommand(CommandSender sender, String[] args) {
        if (!available) {
            Msg.to(sender, "&cCitizens ist nicht aktiv.");
            return;
        }
        if (args.length < 2) {
            Msg.to(sender, "&7Verwendung: /ls npc <create|summon|recall|bind|delete> <id>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (!SUPPORTED_COMMANDS.contains(action)) {
            Msg.to(sender, "&cUnbekannte Aktion: " + action);
            return;
        }
        if (args.length < 3) {
            Msg.to(sender, "&7Gib eine NPC-ID an.");
            return;
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        Definition definition = plugin.configs().npc().definition(id).orElse(null);
        if (definition == null && !action.equals("create")) {
            Msg.to(sender, "&cNPC nicht in Konfiguration gefunden: " + id);
            return;
        }
        switch (action) {
            case "create" -> {
                if (definition == null) {
                    Msg.to(sender, "&cNPC nicht in Konfiguration gefunden: " + id);
                    return;
                }
                ManagedNpc previous = managed.remove(id);
                if (previous != null) {
                    previous.destroy();
                }
                ManagedNpc npc = createManaged(definition);
                managed.put(id, npc);
                if (definition.autoSummon()) {
                    npc.spawnAtStage();
                } else {
                    npc.sendToParking();
                }
                Msg.to(sender, "&aNPC '" + id + "' neu erstellt.");
            }
            case "summon" -> {
                ManagedNpc npc = managed.computeIfAbsent(id, key -> createManaged(definition));
                npc.spawnAtStage();
                Msg.to(sender, "&aNPC '" + id + "' wurde zur Bühne teleportiert.");
            }
            case "recall" -> {
                ManagedNpc npc = managed.computeIfAbsent(id, key -> createManaged(definition));
                npc.sendToParking();
                Msg.to(sender, "&eNPC '" + id + "' parkt jetzt im Depot.");
            }
            case "bind" -> {
                if (!(sender instanceof Player player)) {
                    Msg.to(sender, "&cNur Spieler können Bind verwenden.");
                    return;
                }
                Position spawn = new Position(player.getWorld().getName(), player.getLocation().getX(),
                        player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(),
                        player.getLocation().getPitch());
                NpcConfig updated = plugin.configs().npc().withUpdatedSpawn(id, spawn);
                plugin.configs().updateNpc(updated);
                ManagedNpc npc = managed.computeIfAbsent(id, key -> createManaged(updated.definition(id).orElseThrow()));
                npc.updateDefinition(updated.definition(id).orElseThrow());
                npc.spawnAtStage();
                Msg.to(sender, "&aSpawn für NPC '" + id + "' gebunden.");
            }
            case "delete" -> {
                ManagedNpc npc = managed.remove(id);
                if (npc == null) {
                    Msg.to(sender, "&cNPC '" + id + "' ist nicht aktiv.");
                    return;
                }
                npc.destroy();
                Msg.to(sender, "&cNPC '" + id + "' gelöscht.");
            }
        }
    }

    public List<String> knownIds() {
        return plugin.configs().npc().definitions().keySet().stream().sorted().collect(Collectors.toList());
    }

    private void handleBehavior(Player player, ClickBehavior behavior, ManagedNpc npc, Definition definition) {
        switch (behavior.type()) {
            case GUI -> plugin.adminGui().open(player);
            case START -> handleStart(player, npc, definition);
            case VOTE -> handleVote(player, definition);
            case COMMAND -> runCommands(behavior.commands(), player, npc.definition().id());
            case NONE -> {
            }
        }
    }

    private void handleStart(Player player, ManagedNpc npc, Definition definition) {
        if (plugin.game() == null) {
            Msg.to(player, "&cLuckySky ist noch nicht bereit.");
            return;
        }
        if (plugin.game().state() == de.opalium.luckysky.game.GameState.RUNNING) {
            Msg.to(player, "&cLuckySky läuft bereits.");
            return;
        }
        runCommands(definition != null ? definition.startCommands() : List.of(), player, npc.definition().id());
        plugin.game().start();
        voteParticipants.clear();
        scheduleCollapse(definition, player);
        scheduleReturn(definition, npc);
    }

    private void handleVote(Player player, Definition definition) {
        if (definition != null) {
            runCommands(definition.voteCommands(), player, definition.id());
        }
        if (!voteParticipants.add(player.getUniqueId())) {
            Msg.to(player, "&eDu hast bereits abgestimmt.");
            return;
        }
        Msg.to(player, "&aDeine Stimme wurde gezählt.");
    }

    private void scheduleCollapse(Definition definition, Player player) {
        if (definition == null || definition.collapseCommands().isEmpty()) {
            return;
        }
        int delay = Math.max(1, definition.collapseDelayTicks());
        new BukkitRunnable() {
            @Override
            public void run() {
                runCommands(definition.collapseCommands(), player, definition.id());
            }
        }.runTaskLater(plugin, delay);
    }

    private void scheduleReturn(Definition definition, ManagedNpc npc) {
        if (definition == null) {
            return;
        }
        int delay = definition.returnDelayTicks();
        if (delay <= 0) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                npc.sendToParking();
            }
        }.runTaskLater(plugin, delay);
    }

    private ManagedNpc resolve(NPC npc) {
        String id = npc.data().get(NPC_KEY, String.class);
        if (id == null) {
            return null;
        }
        return managed.computeIfAbsent(id, key -> {
            Definition definition = plugin.configs().npc().definition(key).orElse(null);
            if (definition == null) {
                return null;
            }
            ManagedNpc managedNpc = new ManagedNpc(definition, npc);
            managed.put(key, managedNpc);
            return managedNpc;
        });
    }

    private ManagedNpc createManaged(Definition definition) {
        NPC npc = findOrCreateNpc(definition);
        return new ManagedNpc(definition, npc);
    }

    private NPC findOrCreateNpc(Definition definition) {
        NPC existing = findExisting(definition.id());
        if (existing != null) {
            applyDefinition(existing, definition);
            return existing;
        }
        NPC created = registry.createNPC(resolveEntityType(definition.entityType()), definition.displayName());
        applyDefinition(created, definition);
        return created;
    }

    private void applyDefinition(NPC npc, Definition definition) {
        npc.setName(Msg.color(definition.displayName()));
        npc.data().setPersistent(NPC_KEY, definition.id());
        npc.setProtected(true);
        npc.data().setPersistent("collidable", false);
        npc.getOrAddTrait(SkinTrait.class);
        definition.skin().ifPresent(skin -> {
            if (skin.isComplete()) {
                SkinTrait trait = npc.getOrAddTrait(SkinTrait.class);
                trait.setSkinPersistent(definition.id(), skin.texture(), skin.signature());
            }
        });
    }

    private EntityType resolveEntityType(EntityType type) {
        return type != null ? type : EntityType.PLAYER;
    }

    private NPC findExisting(String id) {
        for (NPC npc : registry) {
            if (Objects.equals(npc.data().get(NPC_KEY, String.class), id)) {
                return npc;
            }
        }
        return null;
    }

    private void runCommands(List<String> commands, Player source, String npcId) {
        if (commands.isEmpty()) {
            return;
        }
        Server server = plugin.getServer();
        String playerName = source != null ? source.getName() : "";
        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }
            String processed = command
                    .replace("%player%", playerName)
                    .replace("%npc%", npcId);
            server.dispatchCommand(server.getConsoleSender(), processed);
        }
    }

    private void ensureChunksLoaded() {
        unloadChunks();
        NpcConfig config = plugin.configs().npc();
        String worldName = config.storage().world();
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Storage-Welt nicht geladen: " + worldName);
            return;
        }
        for (NpcConfig.Chunk chunk : config.storage().chunks()) {
            world.setChunkForceLoaded(chunk.x(), chunk.z(), true);
            forcedChunks.add(new ChunkCoordinate(worldName, chunk.x(), chunk.z()));
        }
    }

    private void unloadChunks() {
        if (forcedChunks.isEmpty()) {
            return;
        }
        for (ChunkCoordinate coordinate : forcedChunks) {
            org.bukkit.World world = Bukkit.getWorld(coordinate.world());
            if (world != null) {
                world.setChunkForceLoaded(coordinate.x(), coordinate.z(), false);
            }
        }
        forcedChunks.clear();
    }

    private void clearAll() {
        for (ManagedNpc npc : new ArrayList<>(managed.values())) {
            npc.destroy();
        }
        managed.clear();
    }

    private final class ManagedNpc {
        private Definition definition;
        private final NPC npc;

        ManagedNpc(Definition definition, NPC npc) {
            this.definition = definition;
            this.npc = npc;
        }

        Definition definition() {
            return definition;
        }

        void updateDefinition(Definition definition) {
            this.definition = definition;
            applyDefinition(npc, definition);
        }

        void spawnAtStage() {
            Location location = toLocation(definition.spawn());
            if (location == null) {
                return;
            }
            if (npc.isSpawned()) {
                npc.teleport(location, TeleportCause.PLUGIN);
            } else {
                npc.spawn(location);
            }
        }

        void sendToParking() {
            Location location = toLocation(plugin.configs().npc().parking());
            if (location == null) {
                return;
            }
            if (npc.isSpawned()) {
                npc.teleport(location, TeleportCause.PLUGIN);
            } else {
                npc.spawn(location);
            }
        }

        void destroy() {
            if (npc.isSpawned()) {
                npc.despawn();
            }
            registry.deregister(npc);
        }

        private Location toLocation(Position position) {
            org.bukkit.World world = Bukkit.getWorld(position.world());
            if (world == null) {
                plugin.getLogger().warning("NPC-Welt nicht geladen: " + position.world());
                return null;
            }
            return new Location(world, position.x(), position.y(), position.z(), position.yaw(), position.pitch());
        }
    }

    private record ChunkCoordinate(String world, int x, int z) {
    }
}
