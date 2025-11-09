package de.opalium.luckysky.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class GuiItems {
    private GuiItems() {}

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public static ItemStack button(Material material, String name, List<String> lore, boolean glow) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(component(name));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream().map(GuiItems::component).collect(Collectors.toList()));
        }
        if (glow) {
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack button(Material material, String name, List<String> lore) {
        return button(material, name, lore, false);
    }

    // ====== CLEAR BUTTONS ======
    public static ItemStack tntClearPlaneY101() {
        return button(
            Material.TNT,
            "CLEAR Y=101 (300)",
            Arrays.asList(
                "Löscht alle Blöcke auf y=101",
                "im Umkreis von 300 Blöcken.",
                "",
                "Dauer: 10–30 Sekunden",
                "Podest wird danach neu gebaut."
            ),
            true
        );
    }

    public static ItemStack fullClear0to319() {
        return button(
            Material.GUNPOWDER,
            "VOLLWIPE (0–319, 300)",
            Arrays.asList(
                "Löscht ALLES von y=0 bis y=319",
                "im Radius 300 Blöcke.",
                "",
                "Sehr langsam! (1–3 Minuten)",
                "Kann Server kurz laggen!",
                "",
                "Podest wird danach neu gebaut."
            ),
            true
        );
    }

    // ====== NEU: SCHEMATIC BUTTONS ======
    public static ItemStack loadSchematic() {
        return button(
            Material.PAPER,
            "Load Schematic",
            Arrays.asList(
                "Lädt Schematic 'platform' aus",
                "plugins/WorldEdit/schematics/",
                "",
                "Nutze: //schem load platform"
            ),
            false
        );
    }

    public static ItemStack pasteSchematic() {
        return button(
            Material.STRUCTURE_BLOCK,
            "Paste Schematic",
            Arrays.asList(
                "Pastet das geladene Schematic",
                "an deiner aktuellen Position.",
                "",
                "Nutze: //paste"
            ),
            true
        );
    }

    private static Component component(String text) {
        return SERIALIZER.deserialize(text);
    }
}
