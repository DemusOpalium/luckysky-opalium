package de.opalium.luckysky.listeners;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.npc.NpcDepot;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class NpcRightClickListener implements Listener {
    private final LuckySkyPlugin plugin;

    public NpcRightClickListener(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        NpcDepot depot = plugin.npcDepot();
        if (depot != null && depot.isAvailable()) {
            depot.handleRightClick(event);
        }
    }
}
