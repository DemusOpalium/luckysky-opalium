package de.opalium.luckysky.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class GuiItems {
    private GuiItems() {}

    private static final LegacyComponentSerializer S = LegacyComponentSerializer.legacyAmpersand();

    // Nur konservative Flags – maximal kompatibel
    private static final ItemFlag[] BASE_FLAGS = {
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_ENCHANTS,   // wir zeigen Enchants nie sichtbar
            ItemFlag.HIDE_UNBREAKABLE
    };

    // ---------------- Core-Builder (String)
    public static ItemStack button(Material mat, String name, List<String> lore) {
        return button(mat, name, lore, false);
    }
    public static ItemStack button(Material mat, String name, String... loreLines) {
        return button(mat, name, Arrays.asList(loreLines), false);
    }
    public static ItemStack button(Material mat, String name, List<String> lore, boolean /*unusedGlow*/ glow) {
        return button(mat, c(name), to(lore), false);
    }

    // ---------------- Core-Builder (Component)
    public static ItemStack button(Material mat, Component name, List<Component> lore, boolean /*unusedGlow*/ glow) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m == null) return it;

        if (name != null) m.displayName(name);
        if (lore != null && !lore.isEmpty()) {
            m.lore(lore.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        }

        m.addItemFlags(BASE_FLAGS);
        // Wir verzichten bewusst auf sichtbares Glow (Enchant wäre unsichtbar):
        // m.addEnchant(Enchantment.INFINITY, 1, true);  // <- nicht nötig
        it.setItemMeta(m);
        return it;
    }

    // ---------------- Filler
    public static ItemStack filler() {
        return button(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
    }

    // ---------------- Presets: Admin
    public static ItemStack adminResetWorld() {
        return button(Material.TNT, "&c&lRESET WELT",
                Arrays.asList("&7Löscht &cLuckySky&7 komplett", "&7und erstellt sie neu (gleicher Seed).",
                        "", "&eDauer: &f~10 Sekunden", "&aPodest wird neu gebaut."));
    }

    public static ItemStack adminCreateArenas() {
        return button(Material.NETHER_STAR, "&d&lCREATE 3 ARENAS",
                Arrays.asList("&7Erstellt:", "&f→ LuckySky_Arena1", "&f→ LuckySky_Arena2", "&f→ LuckySky_Arena3",
                        "", "&7±175 Blöcke, Border 350", "&7Spawn: 0, 101, 0"));
    }

    public static ItemStack adminToggleWither(boolean enabled) {
        return button(enabled ? Material.WITHER_SKELETON_SKULL : Material.SKELETON_SKULL,
                enabled ? "&aWither AN" : "&cWither AUS",
                "&7Aktiviert/Deaktiviert Wither-Spawns.");
    }

    public static ItemStack adminLoadSchem() {
        return button(Material.PAPER, "&aLoad Schematic",
                Arrays.asList("&7Lädt Schematic &fplatform", "&7aus &eplugins/WorldEdit/schematics/", "", "&8//schem load platform"));
    }

    public static ItemStack adminPasteSchem() {
        return button(Material.STRUCTURE_BLOCK, "&aPaste Schematic",
                Arrays.asList("&7Pastet das geladene Schematic", "&7an deiner aktuellen Position.", "", "&8//paste"));
    }

    public static ItemStack adminStart(boolean running) {
        return button(Material.LIME_DYE, running ? "&aLäuft…" : "&aStart",
                List.of("&7Startet das Spiel und teleportiert zur Plattform."));
    }

    public static ItemStack adminStop() {
        return button(Material.BARRIER, "&cStop & Lobby", List.of("&7Stoppt das Spiel und sendet alle zur Lobby."));
    }

    public static ItemStack adminDuration(int minutes) {
        return button(Material.CLOCK, "&eDauer: &f" + minutes + " Min",
                List.of("&7Klicke zum Wechseln: 5 → 20 → 60"));
    }

    public static ItemStack adminVariant(String current) {
        return button(Material.SPONGE, "&bLucky-Variante", List.of("&7Aktuell: &f" + current));
    }

    public static ItemStack adminTaunts(boolean enabled) {
        return button(Material.GOAT_HORN, enabled ? "&aTaunts AN" : "&cTaunts AUS", List.of("&7Wither-Taunts umschalten."));
    }

    public static ItemStack adminScoreboard(boolean enabled) {
        return button(Material.OAK_SIGN, enabled ? "&aScoreboard AN" : "&cScoreboard AUS", List.of("&7LuckySky-Scoreboard umschalten."));
    }

    public static ItemStack adminTimer(boolean visible) {
        return button(Material.COMPASS, visible ? "&aTimer sichtbar" : "&cTimer versteckt", List.of("&7Timer im Scoreboard ein/aus."));
    }

    public static ItemStack adminBind() {
        return button(Material.RESPAWN_ANCHOR, "&bBind", List.of("&7Setzt Spawn für alle."));
    }

    public static ItemStack adminPlatform() {
        return button(Material.PRISMARINE_BRICKS, "&bPlattform", List.of("&7Baut Safe-Plattform."));
    }

    public static ItemStack adminTeleport() {
        return button(Material.ENDER_PEARL, "&dTeleport", List.of("&7Zum Spawn."));
    }

    public static ItemStack adminSaveConfig() {
        return button(Material.NAME_TAG, "&aSave Config", List.of("&7Speichert & lädt Config neu."));
    }

    // ---------------- Presets: Spawns (neu)
    public static ItemStack adminSpawnLuckyBlock() {
        return button(Material.GOLD_BLOCK, "&6&lSpawn Lucky-Block", List.of("&7Setzt einen Lucky-Block an deine Position."));
    }

    public static ItemStack adminSpawnWither() {
        return button(Material.WITHER_SKELETON_SKULL, "&5&lSpawn Wither", List.of("&7Spawnt einen Wither bei dir."));
    }

    // ---------------- Helpers
    private static Component c(String t) { return t == null ? Component.empty() : S.deserialize(t); }
    private static List<Component> to(List<String> ls) {
        if (ls == null) return null;
        return ls.stream().filter(Objects::nonNull).map(GuiItems::c).collect(Collectors.toList());
    }
}
