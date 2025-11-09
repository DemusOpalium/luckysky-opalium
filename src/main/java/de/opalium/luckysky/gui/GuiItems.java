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

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    // Flags, die immer versteckt werden sollen (NICHT HIDE_ENCHANTS – sonst verschwindet der Glint)
    private static final ItemFlag[] BASE_FLAGS = {
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_UNBREAKABLE,
            ItemFlag.HIDE_DESTROYS,
            ItemFlag.HIDE_PLACED_ON,
            ItemFlag.HIDE_POTION_EFFECTS,
            ItemFlag.HIDE_DYE
    };

    /* ─────────────────────────────────────────────────────────────
       Core-Builder (String-API)
       ───────────────────────────────────────────────────────────── */
    public static ItemStack button(Material material, String name, List<String> lore, boolean glow) {
        return button(material, component(name), toComponents(lore), glow);
    }

    public static ItemStack button(Material material, String name, List<String> lore) {
        return button(material, name, lore, false);
    }

    public static ItemStack button(Material material, String name, String... loreLines) {
        return button(material, name, Arrays.asList(loreLines), false);
    }

    /* ─────────────────────────────────────────────────────────────
       Core-Builder (Component-API)
       ───────────────────────────────────────────────────────────── */
    public static ItemStack button(Material material, Component name, List<Component> lore, boolean glow) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack; // Failsafe

        if (name != null) meta.displayName(name);
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        }

        // Immer: Basis-Flags
        meta.addItemFlags(BASE_FLAGS);

        if (glow) {
            // Glint aktiv lassen -> KEIN HIDE_ENCHANTS
            meta.addEnchant(Enchantment.INFINITY, 1, true);
        } else {
            // Falls Items bereits Enchants hätten, nicht anzeigen
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        stack.setItemMeta(meta);
        return stack;
    }

    /* ─────────────────────────────────────────────────────────────
       Toggle-Factory (für AN/AUS-Buttons mit einheitlichem Look)
       ───────────────────────────────────────────────────────────── */
    public static ItemStack toggle(Material matOn, Material matOff, boolean enabled,
                                   String onLabel, String offLabel, List<String> commonLore) {
        Material m = enabled ? matOn : matOff;
        String name = enabled ? "&a" + onLabel : "&c" + offLabel;
        // Glow nur wenn aktiv – wirkt „klickbar“
        return button(m, name, commonLore, enabled);
    }

    /* ─────────────────────────────────────────────────────────────
       PRESETS
       ───────────────────────────────────────────────────────────── */
    // CLEAR BUTTONS
    public static ItemStack tntClearPlaneY101() {
        return button(
                Material.TNT,
                "&c&lCLEAR Y=101 &8(±300)",
                Arrays.asList(
                        "&7Löscht &nalle Blöcke&7 auf &ey=101",
                        "&7im Umkreis von &b±300 Blöcken&7.",
                        "",
                        "&eDauer: &f10–30 Sekunden",
                        "&aPodest wird danach neu gebaut."
                ),
                true
        );
    }

    public static ItemStack fullClear0to319() {
        return button(
                Material.GUNPOWDER,
                "&4&lVOLLWIPE &8(0–319, ±300)",
                Arrays.asList(
                        "&7Löscht &c&lALLES&7 von &ey=0 bis y=319",
                        "&7im Radius &b±300 Blöcke&7.",
                        "",
                        "&6Sehr langsam! &6(1–3 Minuten)",
                        "&cKann Server kurz laggen!",
                        "",
                        "&aPodest wird danach neu gebaut."
                ),
                true
        );
    }

    // SCHEMATIC BUTTONS
    public static ItemStack loadSchematic() {
        return button(
                Material.PAPER,
                "&aLoad Schematic",
                Arrays.asList(
                        "&7Lädt Schematic &fplatform",
                        "&7aus &eplugins/WorldEdit/schematics/",
                        "",
                        "&7Befehl: &f//schem load platform"
                ),
                false
        );
    }

    public static ItemStack pasteSchematic() {
        return button(
                Material.STRUCTURE_BLOCK,
                "&aPaste Schematic",
                Arrays.asList(
                        "&7Pastet das geladene Schematic",
                        "&7an deiner aktuellen Position.",
                        "",
                        "&7Befehl: &f//paste"
                ),
                true
        );
    }

    /* ─────────────────────────────────────────────────────────────
       Helpers
       ───────────────────────────────────────────────────────────── */
    private static Component component(String text) {
        return text == null ? Component.empty() : SERIALIZER.deserialize(text);
    }

    private static List<Component> toComponents(List<String> lines) {
        if (lines == null) return null;
        return lines.stream().filter(Objects::nonNull).map(GuiItems::component).collect(Collectors.toList());
    }
}
