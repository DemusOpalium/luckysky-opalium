package de.opalium.luckysky.npc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

final class CitizensBridge {
    private static final boolean AVAILABLE;
    private static final Method GET_REGISTRY;
    private static final Method REGISTRY_GET_BY_ID;
    private static final Method NPC_IS_SPAWNED;
    private static final Method NPC_GET_ENTITY;
    private static final Method NPC_GET_STORED_LOCATION;
    private static final Method NPC_SPAWN;
    private static final Method NPC_DESPAWN;
    private static final Method NPC_TELEPORT;
    private static final Object DESPAWN_PLUGIN_REASON;
    private static final Object SPAWN_PLUGIN_REASON;

    static {
        Method getRegistry = null;
        Method registryGetById = null;
        Method npcIsSpawned = null;
        Method npcGetEntity = null;
        Method npcGetStoredLocation = null;
        Method npcSpawn = null;
        Method npcDespawn = null;
        Method npcTeleport = null;
        Object despawnPlugin = null;
        Object spawnPlugin = null;
        boolean available = false;
        try {
            Class<?> citizensApi = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Class<?> registryClass = Class.forName("net.citizensnpcs.api.npc.NPCRegistry");
            Class<?> npcClass = Class.forName("net.citizensnpcs.api.npc.NPC");
            Class<?> despawnReasonClass = Class.forName("net.citizensnpcs.api.event.DespawnReason");
            Class<?> spawnReasonClass = Class.forName("net.citizensnpcs.api.npc.NPCSpawnReason");

            getRegistry = citizensApi.getMethod("getNPCRegistry");
            registryGetById = registryClass.getMethod("getById", int.class);
            npcIsSpawned = npcClass.getMethod("isSpawned");
            npcGetEntity = npcClass.getMethod("getEntity");
            npcGetStoredLocation = npcClass.getMethod("getStoredLocation");
            npcSpawn = npcClass.getMethod("spawn", Location.class, spawnReasonClass);
            npcDespawn = npcClass.getMethod("despawn", despawnReasonClass);
            npcTeleport = npcClass.getMethod("teleport", Location.class, TeleportCause.class);

            @SuppressWarnings("unchecked")
            Enum<?> despawn = Enum.valueOf((Class<Enum>) despawnReasonClass.asSubclass(Enum.class), "PLUGIN");
            @SuppressWarnings("unchecked")
            Enum<?> spawn = Enum.valueOf((Class<Enum>) spawnReasonClass.asSubclass(Enum.class), "PLUGIN");
            despawnPlugin = despawn;
            spawnPlugin = spawn;
            available = true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalArgumentException ignored) {
            available = false;
        }
        GET_REGISTRY = getRegistry;
        REGISTRY_GET_BY_ID = registryGetById;
        NPC_IS_SPAWNED = npcIsSpawned;
        NPC_GET_ENTITY = npcGetEntity;
        NPC_GET_STORED_LOCATION = npcGetStoredLocation;
        NPC_SPAWN = npcSpawn;
        NPC_DESPAWN = npcDespawn;
        NPC_TELEPORT = npcTeleport;
        DESPAWN_PLUGIN_REASON = despawnPlugin;
        SPAWN_PLUGIN_REASON = spawnPlugin;
        AVAILABLE = available && getRegistry != null && registryGetById != null && npcIsSpawned != null
                && npcGetEntity != null && npcGetStoredLocation != null && npcSpawn != null && npcDespawn != null
                && npcTeleport != null && despawnPlugin != null && spawnPlugin != null;
    }

    private CitizensBridge() {
    }

    static boolean isAvailable() {
        return AVAILABLE;
    }

    static Optional<NpcHandle> getNpc(int id) {
        if (!AVAILABLE) {
            return Optional.empty();
        }
        try {
            Object registry = GET_REGISTRY.invoke(null);
            if (registry == null) {
                return Optional.empty();
            }
            Object npc = REGISTRY_GET_BY_ID.invoke(registry, id);
            if (npc == null) {
                return Optional.empty();
            }
            return Optional.of(new NpcHandle(npc));
        } catch (IllegalAccessException | InvocationTargetException ex) {
            return Optional.empty();
        }
    }

    static final class NpcHandle {
        private final Object handle;

        private NpcHandle(Object handle) {
            this.handle = handle;
        }

        boolean isSpawned() {
            return invokeBoolean(NPC_IS_SPAWNED);
        }

        Entity entity() {
            try {
                return (Entity) NPC_GET_ENTITY.invoke(handle);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                return null;
            }
        }

        Location storedLocation() {
            try {
                return (Location) NPC_GET_STORED_LOCATION.invoke(handle);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                return null;
            }
        }

        void spawn(Location location) {
            if (location == null) {
                return;
            }
            try {
                NPC_SPAWN.invoke(handle, location, SPAWN_PLUGIN_REASON);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }

        void despawn() {
            try {
                NPC_DESPAWN.invoke(handle, DESPAWN_PLUGIN_REASON);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }

        void teleport(Location location, TeleportCause cause) {
            if (location == null || cause == null) {
                return;
            }
            try {
                NPC_TELEPORT.invoke(handle, location, cause);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }

        private boolean invokeBoolean(Method method) {
            if (method == null) {
                return false;
            }
            try {
                Object value = method.invoke(handle);
                if (value instanceof Boolean result) {
                    return result;
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
            return false;
        }
    }
}
