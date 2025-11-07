package de.opalium.luckysky.gui.editor;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.arena.ArenaService;
import de.opalium.luckysky.arena.ArenaService.ArenaDefinition;
import de.opalium.luckysky.arena.ArenaService.ArenaVariant;
import de.opalium.luckysky.arena.ArenaService.OperationType;
import de.opalium.luckysky.gui.GuiItems;
import de.opalium.luckysky.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class ArenaEditorGui implements Listener {
    private static final String MAIN_TITLE = ChatColor.DARK_BLUE + "Arena-Editor";
    private static final String VARIANT_TITLE_PREFIX = ChatColor.DARK_AQUA + "Arena: ";
    private static final int MAIN_SIZE = 27;
    private static final int VARIANT_SIZE = 36;

    private final LuckySkyPlugin plugin;
    private final ArenaService arenaService;
    private final Map<UUID, ArenaEditorSession> sessions = new HashMap<>();

    public ArenaEditorGui(LuckySkyPlugin plugin, ArenaService arenaService) {
        this.plugin = plugin;
        this.arenaService = arenaService;
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(player, MAIN_SIZE, MAIN_TITLE);
        populateMain(inventory);
        player.openInventory(inventory);
    }

    private void populateMain(Inventory inventory) {
        inventory.clear();
        List<String> arenas = new ArrayList<>(arenaService.arenaIds());
        arenas.sort(String::compareToIgnoreCase);
        for (int i = 0; i < arenas.size() && i < MAIN_SIZE; i++) {
            String arenaId = arenas.get(i);
            ArenaDefinition definition = arenaService.arena(arenaId).orElse(null);
            String name = definition != null ? definition.name() : arenaId;
            List<String> lore;
            if (definition == null) {
                lore = List.of();
            } else {
                List<String> variantNames = new ArrayList<>(definition.variants().keySet());
                variantNames.sort(String::compareToIgnoreCase);
                lore = List.of("&7Varianten: &f" + String.join(", ", variantNames));
            }
            inventory.setItem(i, GuiItems.button(Material.BOOK, "&b" + name, lore, false));
        }
    }

    private void openVariant(Player player, String arenaId) {
        ArenaDefinition definition = arenaService.arena(arenaId).orElse(null);
        if (definition == null) {
            Msg.to(player, "&cArena nicht gefunden: " + arenaId);
            return;
        }
        List<String> variantIds = new ArrayList<>(definition.variants().keySet());
        variantIds.sort(String::compareToIgnoreCase);
        String variantId = variantIds.stream().findFirst().orElse(null);
        if (variantId == null) {
            Msg.to(player, "&cKeine Varianten verfügbar.");
            return;
        }
        ArenaEditorSession session = sessions.computeIfAbsent(player.getUniqueId(), id -> new ArenaEditorSession(arenaId, variantId));
        session.setArena(arenaId, variantId);
        Inventory inventory = Bukkit.createInventory(player, VARIANT_SIZE, VARIANT_TITLE_PREFIX + arenaId);
        populateVariant(inventory, definition, session);
        player.openInventory(inventory);
    }

    private void populateVariant(Inventory inventory, ArenaDefinition definition, ArenaEditorSession session) {
        inventory.clear();
        List<String> variants = new ArrayList<>(definition.variants().keySet());
        variants.sort(String::compareToIgnoreCase);
        int index = 0;
        for (String variantId : variants) {
            ArenaVariant variant = definition.variants().get(variantId);
            boolean selected = variantId.equals(session.variantId());
            List<String> lore = List.of(
                    "&7Welt: &f" + variant.worldName(),
                    "&7Origin: &f" + variant.originX() + "," + variant.originY() + "," + variant.originZ());
            inventory.setItem(index, GuiItems.button(Material.MAP, "&e" + variantId, lore, selected));
            index++;
        }
        inventory.setItem(27, GuiItems.button(Material.GRASS_BLOCK, "&aBuild", List.of("&7Arena bauen"), false));
        inventory.setItem(28, GuiItems.button(Material.BARRIER, "&cClear", List.of("&7Arena entfernen"), false));
        inventory.setItem(29, GuiItems.button(Material.LANTERN, "&eLight", List.of("&7Licht setzen"), false));
        inventory.setItem(30, GuiItems.button(Material.GOLD_BLOCK, "&6Crown", List.of("&7Krone setzen"), false));
        inventory.setItem(31, GuiItems.button(Material.SMOOTH_STONE, "&fFloor", List.of("&7Boden aktualisieren"), false));
        inventory.setItem(32, GuiItems.button(Material.GLASS, "&fCeiling", List.of("&7Decke aktualisieren"), false));
        inventory.setItem(33, GuiItems.button(Material.COMPASS, "&bOrigin = Standort", List.of("&7Setzt den Ursprung auf deine Position"), false));
        inventory.setItem(34, GuiItems.button(Material.WRITABLE_BOOK, "&aSpeichern", List.of("&7Schreibt arenas.yml"), false));
        inventory.setItem(35, GuiItems.button(Material.ARROW, "&7Zurück", List.of("&7Zur Arena-Auswahl"), false));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (title.equals(MAIN_TITLE)) {
            handleMainClick(event, player);
        } else if (title.startsWith(VARIANT_TITLE_PREFIX)) {
            handleVariantClick(event, player, title.substring(VARIANT_TITLE_PREFIX.length()));
        }
    }

    private void handleMainClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= MAIN_SIZE) {
            return;
        }
        String arenaId = new ArrayList<>(arenaService.arenaIds()).stream()
                .sorted(String::compareToIgnoreCase)
                .skip(event.getRawSlot())
                .findFirst()
                .orElse(null);
        if (arenaId == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> openVariant(player, arenaId));
    }

    private void handleVariantClick(InventoryClickEvent event, Player player, String arenaId) {
        event.setCancelled(true);
        ArenaDefinition definition = arenaService.arena(arenaId).orElse(null);
        if (definition == null) {
            player.closeInventory();
            return;
        }
        ArenaEditorSession session = sessions.computeIfAbsent(player.getUniqueId(), id -> new ArenaEditorSession(arenaId, plugin.settings().arenaDefaultVariant()));
        List<String> variants = new ArrayList<>(definition.variants().keySet());
        variants.sort(String::compareToIgnoreCase);
        if (event.getRawSlot() < variants.size()) {
            int index = event.getRawSlot();
            String variantId = variants.get(index);
            session.setArena(arenaId, variantId);
            Bukkit.getScheduler().runTask(plugin, () -> reopenVariant(player, definition, session));
            return;
        }
        switch (event.getRawSlot()) {
            case 27 -> runOperation(player, session, OperationType.BUILD);
            case 28 -> runOperation(player, session, OperationType.CLEAR);
            case 29 -> runOperation(player, session, OperationType.LIGHT);
            case 30 -> runOperation(player, session, OperationType.CROWN);
            case 31 -> runOperation(player, session, OperationType.FLOOR);
            case 32 -> runOperation(player, session, OperationType.CEILING);
            case 33 -> updateOrigin(player, session);
            case 34 -> save(player);
            case 35 -> Bukkit.getScheduler().runTask(plugin, () -> openMain(player));
            default -> {
            }
        }
    }

    private void reopenVariant(Player player, ArenaDefinition definition, ArenaEditorSession session) {
        Inventory inventory = Bukkit.createInventory(player, VARIANT_SIZE, VARIANT_TITLE_PREFIX + session.arenaId());
        populateVariant(inventory, definition, session);
        player.openInventory(inventory);
    }

    private void runOperation(Player player, ArenaEditorSession session, OperationType type) {
        arenaService.applyWithFeedback(session.arenaId(), session.variantId(), type);
        Msg.to(player, "&7Operation " + type.key() + " gestartet.");
    }

    private void updateOrigin(Player player, ArenaEditorSession session) {
        ArenaDefinition definition = arenaService.arena(session.arenaId()).orElse(null);
        if (definition == null) {
            Msg.to(player, "&cArena nicht gefunden.");
            return;
        }
        arenaService.updateVariantOrigin(session.arenaId(), session.variantId(), player.getLocation());
        Msg.to(player, "&aOrigin aktualisiert auf deine Position.");
        if (plugin.settings().arenaEditorAutosave()) {
            arenaService.save();
            Msg.to(player, "&7Änderungen gespeichert.");
        }
        Bukkit.getScheduler().runTask(plugin, () -> reopenVariant(player, definition, session));
    }

    private void save(Player player) {
        arenaService.save();
        Msg.to(player, "&aArenen gespeichert.");
    }
}
