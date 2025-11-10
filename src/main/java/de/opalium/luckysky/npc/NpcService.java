package de.opalium.luckysky.npc;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.NpcConfig;
import de.opalium.luckysky.config.NpcConfig.Area;
import de.opalium.luckysky.config.NpcConfig.BlockVector;
import de.opalium.luckysky.config.NpcConfig.NpcEntry;
import de.opalium.luckysky.util.Msg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public final class NpcService {
    private final LuckySkyPlugin plugin;
    private Map<String, NpcEntry> definitions = Collections.emptyMap();
    private final Map<String, NpcCollapseTask> activeTasks = new ConcurrentHashMap<>();
    private final boolean citizensPresent;

    public NpcService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.citizensPresent = Bukkit.getPluginManager().getPlugin("Citizens") != null
                && CitizensBridge.isAvailable();
        reload();
    }

    public void reload() {
        cancelAll();
        NpcConfig config = plugin.configs().npcs();
        this.definitions = config != null ? config.npcs() : Collections.emptyMap();
    }

    public void shutdown() {
        cancelAll();
    }

    public List<String> npcIds() {
        return new ArrayList<>(definitions.keySet());
    }

    public boolean collapse(String id, CommandSender feedback) {
        if (!citizensPresent) {
            Msg.to(feedback, "&cCitizens ist nicht geladen.");
            return false;
        }
        NpcEntry entry = definitions.get(id);
        if (entry == null) {
            Msg.to(feedback, "&cNPC '" + id + "' ist nicht konfiguriert.");
            return false;
        }
        if (activeTasks.containsKey(id)) {
            Msg.to(feedback, "&eFür NPC '" + id + "' läuft bereits ein Collapse.");
            return false;
        }
        World world = Bukkit.getWorld(entry.world());
        if (world == null) {
            Msg.to(feedback, "&cWelt '" + entry.world() + "' nicht gefunden.");
            return false;
        }
        CitizensBridge.NpcHandle npc = resolveNpc(entry.npcId());
        if (npc == null) {
            Msg.to(feedback, "&cCitizens-NPC mit ID " + entry.npcId() + " nicht gefunden.");
            return false;
        }
        Location originalLocation = resolveOriginalLocation(npc, world, entry.house());
        if (originalLocation == null) {
            Msg.to(feedback, "&cKeine Ausgangsposition für NPC vorhanden.");
            return false;
        }
        NpcConfig.CollapseScript script = entry.collapseScript();
        if (script.type() == NpcConfig.CollapseScriptType.FAWE && script.fawe().isPresent()) {
            Msg.to(feedback, "&eFAWE-Skripte sind noch nicht implementiert. Verwende SEQUENTIAL.");
        }
        NpcCollapseTask task = new NpcCollapseTask(plugin, id, entry, npc, world, originalLocation, () -> {
            activeTasks.remove(id);
        });
        activeTasks.put(id, task);
        task.start();
        Msg.to(feedback, "&aCollapse für NPC '" + id + "' gestartet.");
        return true;
    }

    private CitizensBridge.NpcHandle resolveNpc(int npcId) {
        return CitizensBridge.getNpc(npcId).orElse(null);
    }

    private Location resolveOriginalLocation(CitizensBridge.NpcHandle npc, World fallbackWorld, Area area) {
        Location location = null;
        if (npc != null && npc.isSpawned()) {
            Entity entity = npc.entity();
            if (entity != null) {
                location = entity.getLocation().clone();
            }
        }
        if (location != null) {
            return location.clone();
        }
        if (npc != null) {
            location = npc.storedLocation();
            if (location != null) {
                return location.clone();
            }
        }
        BlockVector min = area.orderedMin();
        BlockVector max = area.orderedMax();
        double centerX = (min.x() + max.x()) / 2.0D + 0.5D;
        double centerY = max.y() + 0.5D;
        double centerZ = (min.z() + max.z()) / 2.0D + 0.5D;
        return new Location(fallbackWorld, centerX, centerY, centerZ);
    }

    private void cancelAll() {
        for (NpcCollapseTask task : new ArrayList<>(activeTasks.values())) {
            task.cancelTask();
        }
        activeTasks.clear();
    }
}
