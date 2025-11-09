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
    private static final ItemFlag[] BASE_FLAGS = {
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_UNBREAKABLE,
            ItemFlag.HIDE_DESTROYS,
            ItemFlag.HIDE_PLACED_ON
    };

    // Core
    public static ItemStack button(Material mat, String name, List<String> lore, boolean glow) {
        return button(mat, c(name), to(lore), glow);
    }
    public static ItemStack button(Material mat, String name, List<String> lore) {
        return button(mat, name, lore, false);
    }
    public static ItemStack button(Material mat, String name, String... lore) {
        return button(mat, name, Arrays.asList(lore), false);
    }
    public static ItemStack button(Material mat, Component name, List<Component> lore, boolean glow) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m == null) return it;

        if (name != null) m.displayName(name);
        if (lore != null && !lore.isEmpty()) m.lore(lore.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        m.addItemFlags(BASE_FLAGS);
        if (glow) m.addEnchant(Enchantment.INFINITY, 1, true); else m.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        it.setItemMeta(m);
        return it;
    }

    // Filler
    public static ItemStack filler() {
        return button(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), false);
    }

    // Presets: Schematics
    public static ItemStack loadSchematic() {
        return button(Material.PAPER, "&aLoad Schematic",
                Arrays.asList("&7Lädt &fplatform", "&7aus &eplugins/WorldEdit/schematics/", "", "&8//schem load platform"), false);
    }
    public static ItemStack pasteSchematic() {
        return button(Material.STRUCTURE_BLOCK, "&aPaste Schematic",
                Arrays.asList("&7Pastet das geladene Schematic", "&7an deiner aktuellen Position.", "", "&8//paste"), true);
    }

    // Presets: Spawns (neu)
    public static ItemStack spawnLuckyBlock() {
        return button(Material.GOLD_BLOCK, "&6&lSpawn Lucky-Block",
                Arrays.asList("&7Setzt einen Lucky-Block", "&7an deiner Position (Test)."), true);
    }
    public static ItemStack spawnWither() {
        return button(Material.WITHER_SKELETON_SKULL, "&5&lSpawn Wither",
                Arrays.asList("&7Spawnt einen Wither", "&7direkt bei dir."), true);
    }

    // Toggle factory
    public static ItemStack toggle(Material on, Material off, boolean enabled, String onLabel, String offLabel, List<String> lore) {
        Material m = enabled ? on : off;
        String name = enabled ? "&a" + onLabel : "&c" + offLabel;
        return button(m, name, lore, enabled);
    }

    // Helpers
    private static Component c(String t) { return t == null ? Component.empty() : S.deserialize(t); }
    private static List<Component> to(List<String> ls) {
        if (ls == null) return null;
        return ls.stream().filter(Objects::nonNull).map(GuiItems::c).collect(Collectors.toList());
    }
}
