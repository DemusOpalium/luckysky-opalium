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
    private static final ItemFlag[] F = {ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON};

    public static ItemStack b(Material m, String n, List<String> l, boolean g) {
        ItemStack s = new ItemStack(m);
        ItemMeta t = s.getItemMeta();
        if (t == null) return s;
        t.displayName(c(n));
        if (l != null) t.lore(l.stream().map(GuiItems::c).collect(Collectors.toList()));
        t.addItemFlags(F);
        if (g) t.addEnchant(Enchantment.INFINITY, 1, true);
        else t.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        s.setItemMeta(t);
        return s;
    }

    public static ItemStack b(Material m, String n, List<String> l) { return b(m, n, l, false); }
    public static ItemStack b(Material m, String n, String... l) { return b(m, n, Arrays.asList(l), false); }

    public static ItemStack t(Material on, Material off, boolean e, String a, String b, List<String> l) {
        return b(e ? on : off, e ? "&a" + a : "&c" + b, l, e);
    }

    public static ItemStack clearY101() { return b(Material.TNT, "&c&lCLEAR Y=101 &8(±300)",
        Arrays.asList("&7Löscht &nalle Blöcke&7 auf &ey=101", "&7im Umkreis von &b±300 Blöcken&7.", "", "&eDauer: &f10–30s", "&aPodest neu gebaut."), true); }

    public static ItemStack fullClear() { return b(Material.GUNPOWDER, "&4&lVOLLWIPE &8(0–319, ±300)",
        Arrays.asList("&7Löscht &c&lALLES&7 von &ey=0 bis y=319", "&7im Radius &b±300 Blöcke&7.", "", "&6Langsam! &6(1–3 Min)", "&cLag möglich!", "", "&aPodest neu gebaut."), true); }

    public static ItemStack loadSchem() { return b(Material.PAPER, "&aLoad Schematic",
        Arrays.asList("&7Lädt &fplatform", "&7aus &eplugins/WorldEdit/schematics/", "", "&7//schem load platform"), false); }

    public static ItemStack pasteSchem() { return b(Material.STRUCTURE_BLOCK, "&aPaste Schematic",
        Arrays.asList("&7Pastet geladenes Schematic", "&7an deiner Position.", "", "&7//paste"), true); }

    private static Component c(String t) { return t == null ? Component.empty() : S.deserialize(t); }
}
