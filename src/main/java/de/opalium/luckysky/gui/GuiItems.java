package de.opalium.luckysky.gui;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays; // ⬅️ NEU
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

    // ⬇️ Komfort-Overload: ohne glow
    public static ItemStack button(Material material, String name, List<String> lore) {
        return button(material, name, lore, false);
    }

    // ⬇️ UNSERE BEIDEN NEUEN ADMIN-BUTTONS (Factory-Methoden)

    /** TNT-Reset: nur Ebene y=101 im ±300-Umkreis auf AIR, danach Podest-Wiederherstellung. */
    public static ItemStack tntClearPlaneY101() {
        return button(
            Material.TNT,
            "§cFELD CLEAR §7(±300 @ y=101)",
            Arrays.asList(
                "§7Ebene y=101 im Radius ±300 → AIR.",
                "§7Podest wird danach neu gesetzt."
            )
        );
    }

    /** Vollwipe: 0..319 im ±300-Umkreis auf AIR, danach Podest-Wiederherstellung. */
    public static ItemStack fullClear0to319() {
        return button(
            Material.GUNPOWDER,
            "§cFELD CLEAR §7(0..319, ±300)",
            Arrays.asList(
                "§70..319 im ±300-Umkreis → AIR.",
                "§7Podest wird danach neu gesetzt."
            )
        );
    }

    private static Component component(String text) {
        return SERIALIZER.deserialize(text);
    }
}
