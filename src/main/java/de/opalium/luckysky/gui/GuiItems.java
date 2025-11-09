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

    // Komfort-Overload: ohne glow
    public static ItemStack button(Material material, String name, List<String> lore) {
        return button(material, name, lore, false);
    }

    // ====== UNSERE BEIDEN NEUEN ADMIN-BUTTONS (optimiert) ======

    /** TNT-Reset: nur Ebene y=101 im ±300-Umkreis auf AIR */
    public static ItemStack tntClearPlaneY101() {
        return button(
            Material.TNT,
            "&c&lCLEAR Y=101 &8(±300)",
            Arrays.asList(
                "&7Löscht &nalle Blöcke&7 auf &ey=101",
                "&7im Umkreis von &b±300 Blöcken&7.",
                "",
                "&e⏱ Dauer: &f10–30 Sekunden",
                "&aPodest wird danach neu gebaut."
            ),
            true // Glow = auffälliger!
        );
    }

    /** Vollwipe: 0..319 im ±300-Umkreis auf AIR */
    public static ItemStack fullClear0to319() {
        return button(
            Material.GUNPOWDER,
            "&4&lVOLLWIPE &8(0–319, ±300)",
            Arrays.asList(
                "&7Löscht &c&lALLES&7 von &ey=0 bis y=319",
                "&7im Radius &b±300 Blöcke&7.",
                "",
                "&6⚠ &cSehr langsam! &6(1–3 Minuten)",
                "&cKann Server kurz laggen!",
                "",
                "&aPodest wird danach neu gebaut."
            ),
            true // Glow + roter Name = Warnung!
        );
    }

    // Hilfsmethode: § → Adventure Component
    private static Component component(String text) {
        return SERIALIZER.deserialize(text);
    }
}
