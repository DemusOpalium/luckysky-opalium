package de.opalium.luckysky.gui;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.TrapsConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import java.util.List;

public class AdminGui implements Listener {
    private static final String T = ChatColor.DARK_AQUA + "LuckySky Admin", W = "LuckySky";
    private static final int SZ = 27, C1 = 8, C2 = 9, L = 26, P = 0, X = 48, Y = 14;
    private final LuckySkyPlugin p;

    public AdminGui(LuckySkyPlugin p) { this.p = p; }

    public void open(Player pl) { Inventory i = Bukkit.createInventory(pl, SZ, T); fill(i); pl.openInventory(i); }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!T.equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player pl)) return;
        int s = e.getRawSlot(); if (s < 0 || s >= SZ) return;

        switch (s) {
            case C1 -> clearY101(pl);
            case C2 -> fullClear(pl);
            case L -> load(pl);
            case P -> paste(pl);
            case 10 -> { p.game().start(); Msg.to(pl, "Countdown gestartet."); }
            case 11 -> { p.game().stop(); Msg.to(pl, "Stop & Lobby."); }
            case 12 -> { p.game().setDurationMinutes(5); Msg.to(pl, "5 Min."); }
            case 13 -> { p.game().setDurationMinutes(20); Msg.to(pl, "20 Min."); }
            case 14 -> { p.game().setDurationMinutes(60); Msg.to(pl, "60 Min."); }
            case 15 -> toggleTaunts(pl);
            case 16 -> toggleWither(pl);
            case 17 -> p.scoreboard().setEnabled(!p.scoreboard().isEnabled());
            case 18 -> p.scoreboard().setTimerVisible(!p.scoreboard().isTimerVisible());
            case 19 -> cycleVariant(pl);
            case 20 -> Msg.to(pl, "Soft: " + p.game().softClear());
            case 21 -> Msg.to(pl, "Hard: " + p.game().hardClear());
            case 22 -> { p.game().bindAll(); Msg.to(pl, "Gebunden."); }
            case 23 -> { p.game().placePlatform(); Msg.to(pl, "Plattform."); }
            case 24 -> tp(pl);
            case 25 -> { p.configs().saveAll(); p.reloadSettings(); Msg.to(pl, "Gespeichert."); }
        }
        Bukkit.getScheduler().runTask(p, () -> open(pl));
    }

    private void fill(Inventory i) {
        var g = p.configs().game(); var t = p.configs().traps();
        boolean r = p.game().state() == de.opalium.luckysky.game.GameState.RUNNING;
        boolean ta = t.withers().taunts().enabled(), wi = t.withers().enabled();
        boolean sb = p.scoreboard().isEnabled(), tm = p.scoreboard().isTimerVisible();

        i.setItem(C1, GuiItems.tntClearPlaneY101());
        i.setItem(C2, GuiItems.fullClear0to319());
        i.setItem(L, GuiItems.loadSchematic());
        i.setItem(P, GuiItems.pasteSchematic());

        i.setItem(10, GuiItems.b(Material.LIME_DYE, "Start", List.of("Startet Spiel"), r));
        i.setItem(11, GuiItems.b(Material.BARRIER, "Stop", List.of("Stoppt & Lobby")));
        i.setItem(12, GuiItems.b(Material.CLOCK, "5 Min", List.of("Dauer")));
        i.setItem(13, GuiItems.b(Material.CLOCK, "20 Min", List.of("Dauer")));
        i.setItem(14, GuiItems.b(Material.CLOCK, "60 Min", List.of("Dauer")));
        i.setItem(15, GuiItems.t(Material.LIME_WOOL, Material.RED_WOOL, ta, "Taunts AN", "Taunts AUS", List.of("Wither-Taunts")));
        i.setItem(16, GuiItems.t(Material.LIME_WOOL, Material.RED_WOOL, wi, "Wither AN", "Wither AUS", List.of("Wither-Spawns")));
        i.setItem(17, GuiItems.t(Material.OAK_SIGN, Material.SPRUCE_SIGN, sb, "Scoreboard AN", "Scoreboard AUS", List.of("Scoreboard")));
        i.setItem(18, GuiItems.t(Material.COMPASS, Material.CLOCK, tm, "Timer AN", "Timer AUS", List.of("Timer")));
        i.setItem(19, GuiItems.b(Material.SPONGE, "Variante", List.of("Aktuell: " + g.lucky().variant())));
        i.setItem(20, GuiItems.b(Material.FEATHER, "Soft-Wipe", List.of("Effekte")));
        i.setItem(21, GuiItems.b(Material.NETHERITE_SWORD, "Hard-Wipe", List.of("Entities"), true));
        i.setItem(22, GuiItems.b(Material.RESPAWN_ANCHOR, "Bind", List.of("Spawn")));
        i.setItem(23, GuiItems.b(Material.PRISMARINE_BRICKS, "Plattform", List.of("Safe")));
        i.setItem(24, GuiItems.b(Material.ENDER_PEARL, "TP", List.of("Zum Spawn")));
        i.setItem(25, GuiItems.b(Material.NAME_TAG, "Save", List.of("Config")));
    }

    // ====== CLEAR Y=101 (±300) ======
    private void clearY101(Player pl) {
        World w = Bukkit.getWorld(W); if (w == null) { Msg.to(pl, "Welt nicht gefunden!"); return; }
        loadChunks(w, -300, -300, 300, 300); forceLoad(w, -300, -300, 300, 300);
        Msg.to(pl, "Y=101 wird gelöscht..."); in(w, "tellraw @a {\"text\":\"Y=101 wird gereinigt...\",\"color\":\"yellow\"}"); off(w);
        async(() -> {
            for (int x = -300; x <= 300; x += X) for (int z = -300; z <= 300; z += X) {
                int x2 = Math.min(x + X - 1, 300), z2 = Math.min(z + X - 1, 300);
                sync(() -> in(w, "fill %d 101 %d %d 101 %d air".formatted(x, z, x2, z2)));
                sleep(25);
            }
            sync(() -> { p.game().placePlatform(); in(w, "tellraw @a {\"text\":\"Y=101 fertig!\",\"color\":\"green\"}"); on(w); unforce(w); Msg.to(pl, "Fertig."); });
        });
    }

    // ====== FULL CLEAR 0–319 (±300) ======
    private void fullClear(Player pl) {
        World w = Bukkit.getWorld(W); if (w == null) { Msg.to(pl, "Welt nicht gefunden!"); return; }
        loadChunks(w, -300, -300, 300, 300);  // X & Z: ±300 → Chunks von -300 bis +300
        forceLoad(w, -300, -300, 300, 300);   // forceload über gleiche Fläche
        Msg.to(pl, "Vollwipe 0–319..."); in(w, "tellraw @a {\"text\":\"Vollwipe läuft...\",\"color\":\"red\"}"); off(w);
        async(() -> {
            int total = ((600/X)+1)*((600/X)+1)*((319/Y)+1), done = 0;
            for (int x = -300; x <= 300; x += X)
                for (int z = -300; z <= 300; z += X)
                    for (int y = 0; y <= 319; y += Y) {
                        int x2 = Math.min(x + X - 1, 300), z2 = Math.min(z + X - 1, 300), y2 = Math.min(y + Y - 1, 319);
                        sync(() -> in(w, "fill %d %d %d %d %d %d air".formatted(x, y, z, x2, y2, z2)));
                        if (++done % 20 == 0) sync(() -> in(w, "tellraw @a [\"Wipe: \",{\"text\":\"%d%%\",\"color\":\"gold\"}]".formatted(done*100/total)));
                        sleep(50);
                    }
            sync(() -> { p.game().placePlatform(); in(w, "tellraw @a {\"text\":\"Vollwipe fertig!\",\"color\":\"dark_green\"}"); on(w); unforce(w); Msg.to(pl, "Abgeschlossen!"); });
        });
    }

    private void load(Player pl) { if (inWorld(pl) && we()) pl.performCommand("//schem load platform"); Msg.to(pl, we() ? "Geladen." : "WE/FAWE fehlt!"); }
    private void paste(Player pl) { if (inWorld(pl) && we()) pl.performCommand("//paste"); Msg.to(pl, we() ? "Gepastet." : "WE/FAWE fehlt!"); }

    private boolean inWorld(Player pl) { return pl.getWorld().getName().equalsIgnoreCase(W); }
    private boolean we() { return Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null || Bukkit.getPluginManager().getPlugin("WorldEdit") != null; }

    // Chunk-Loading: ±300 in X & Z → Chunks von -300 bis +300
    private void loadChunks(World w, int minX, int minZ, int maxX, int maxZ) {
        int c1 = floorDiv(minX, 16), c2 = floorDiv(maxX, 16);
        int c3 = floorDiv(minZ, 16), c4 = floorDiv(maxZ, 16);
        for (int x = c1; x <= c2; x++) for (int z = c3; z <= c4; z++) w.loadChunk(x, z, true);
    }

    private void forceLoad(World w, int minX, int minZ, int maxX, int maxZ) {
        int c1 = floorDiv(minX, 16), c2 = floorDiv(maxX, 16);
        int c3 = floorDiv(minZ, 16), c4 = floorDiv(maxZ, 16);
        in(w, "forceload add %d %d %d %d".formatted(c1, c3, c2, c4));
    }

    private void unforce(World w) { in(w, "forceload remove all"); }
    private int floorDiv(int a, int b) { int r = a / b; if ((a ^ b) < 0 && (r * b != a)) r--; return r; }

    private void off(World w) { in(w, "gamerule logAdminCommands false"); }
    private void on(World w) { in(w, "gamerule logAdminCommands true"); }
    private void in(World w, String cmd) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute in " + w.getKey() + " run " + cmd); }

    private void async(Runnable r) { Bukkit.getScheduler().runTaskAsynchronously(p, r); }
    private void sync(Runnable r) { Bukkit.getScheduler().runTask(p, r); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    private void toggleTaunts(Player pl) { var t = p.configs().traps(); boolean v = !t.withers().taunts().enabled(); p.configs().updateTraps(t.withTauntsEnabled(v)); p.reloadSettings(); p.game().setTauntsEnabled(v); Msg.to(pl, v ? "Taunts AN" : "Taunts AUS"); }
    private void toggleWither(Player pl) { var t = p.configs().traps(); boolean v = !t.withers().enabled(); p.configs().updateTraps(t.withWithersEnabled(v)); p.reloadSettings(); p.game().setWitherEnabled(v); Msg.to(pl, v ? "Wither AN" : "Wither AUS"); }
    private void cycleVariant(Player pl) { var g = p.configs().game(); var v = g.lucky().variantsAvailable(); String n = v.get((v.indexOf(g.lucky().variant()) + 1) % v.size()); p.configs().updateGame(g.withLuckyVariant(n)); p.reloadSettings(); Msg.to(pl, "Variante: " + n); }
    private void tp(Player pl) { var cfg = p.configs().worlds().luckySky(); var s = cfg.spawn(); pl.teleport(new Location(Worlds.require(cfg.worldName()), s.x(), s.y(), s.z(), s.yaw(), s.pitch())); Msg.to(pl, "TP."); }
}
