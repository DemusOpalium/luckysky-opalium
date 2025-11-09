package de.opalium.luckysky.gui;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.TrapsConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class AdminGui implements Listener {
    private static final String TITLE = ChatColor.DARK_AQUA + "LuckySky Admin";
    private static final int SIZE = 27;

    private static final int SLOT_CLEAR_PLANE_Y101 = 8;     // TNT
    private static final int SLOT_CLEAR_FIELD_FULL = 9;     // GUNPOWDER
    private static final int SLOT_LOAD_SCHEM = 26;          // Load
    private static final int SLOT_PASTE_SCHEM = 0;          // Paste

    private static final String LUCKY_WORLD_NAME = "luckysky";
    private static final int STEP_XZ = 48;
    private static final int STEP_Y = 14;

    private final LuckySkyPlugin plugin;

    public AdminGui(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, SIZE, TITLE);
        populate(inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        switch (slot) {
            case SLOT_CLEAR_PLANE_Y101 -> handleClearPlaneY101(p);
            case SLOT_CLEAR_FIELD_FULL -> handleClearField300(p);
            case SLOT_LOAD_SCHEM -> loadSchematic(p);
            case SLOT_PASTE_SCHEM -> pasteSchematic(p);
            case 10 -> { plugin.game().start(); Msg.to(p, "Countdown gestartet."); }
            case 11 -> { plugin.game().stop(); Msg.to(p, "Game gestoppt & Lobby."); }
            case 12 -> { plugin.game().setDurationMinutes(5); Msg.to(p, "Dauer: 5 Min."); }
            case 13 -> { plugin.game().setDurationMinutes(20); Msg.to(p, "Dauer: 20 Min."); }
            case 14 -> { plugin.game().setDurationMinutes(60); Msg.to(p, "Dauer: 60 Min."); }
            case 15 -> toggleTaunts(p);
            case 16 -> toggleWither(p);
            case 17 -> { plugin.scoreboard().setEnabled(!plugin.scoreboard().isEnabled()); }
            case 18 -> { plugin.scoreboard().setTimerVisible(!plugin.scoreboard().isTimerVisible()); }
            case 19 -> cycleLuckyVariant(p);
            case 20 -> { Msg.to(p, "Soft-Wipe: " + plugin.game().softClear()); }
            case 21 -> { Msg.to(p, "Hard-Wipe: " + plugin.game().hardClear()); }
            case 22 -> { plugin.game().bindAll(); Msg.to(p, "Alle gebunden."); }
            case 23 -> { plugin.game().placePlatform(); Msg.to(p, "Plattform gesetzt."); }
            case 24 -> teleportToSpawn(p);
            case 25 -> { plugin.configs().saveAll(); plugin.reloadSettings(); Msg.to(p, "Config gespeichert."); }
        }

        Bukkit.getScheduler().runTask(plugin, () -> open(p));
    }

    private void populate(Inventory inv) {
        GameConfig game = plugin.configs().game();
        TrapsConfig traps = plugin.configs().traps();
        boolean running = plugin.game().state() == de.opalium.luckysky.game.GameState.RUNNING;

        inv.setItem(SLOT_CLEAR_PLANE_Y101, GuiItems.tntClearPlaneY101());
        inv.setItem(SLOT_CLEAR_FIELD_FULL, GuiItems.fullClear0to319());
        inv.setItem(SLOT_LOAD_SCHEM, GuiItems.loadSchematic());
        inv.setItem(SLOT_PASTE_SCHEM, GuiItems.pasteSchematic());

        inv.setItem(10, GuiItems.button(Material.LIME_DYE, "Start Countdown",
                List.of("Startet das Spiel und teleportiert zur Plattform."), running));
        inv.setItem(11, GuiItems.button(Material.BARRIER, "Stop & Lobby",
                List.of("Stoppt das Spiel und sendet alle zur Lobby."), false));
        inv.setItem(12, GuiItems.button(Material.CLOCK, "Mode 5", List.of("Setzt Dauer auf 5 Minuten."), false));
        inv.setItem(13, GuiItems.button(Material.CLOCK, "Mode 20", List.of("Setzt Dauer auf 20 Minuten."), false));
        inv.setItem(14, GuiItems.button(Material.CLOCK, "Mode 60", List.of("Setzt Dauer auf 60 Minuten."), false));

        boolean taunts = traps.withers().taunts().enabled();
        boolean wither = traps.withers().enabled();
        inv.setItem(15, GuiItems.button(Material.GOAT_HORN, taunts ? "Taunts AN" : "Taunts AUS",
                List.of("Schaltet Wither-Taunts um."), taunts));
        inv.setItem(16, GuiItems.button(Material.WITHER_SKELETON_SKULL, wither ? "Wither AN" : "Wither AUS",
                List.of("Aktiviert/Deaktiviert Wither-Spawns."), wither));

        boolean sb = plugin.scoreboard().isEnabled();
        boolean timer = plugin.scoreboard().isTimerVisible();
        inv.setItem(17, GuiItems.button(Material.OAK_SIGN, sb ? "Scoreboard AN" : "Scoreboard AUS",
                List.of("Schaltet das LuckySky-Scoreboard."), sb));
        inv.setItem(18, GuiItems.button(Material.COMPASS, timer ? "Timer sichtbar" : "Timer versteckt",
                List.of("Blendt den Timer im Scoreboard ein/aus."), timer));

        inv.setItem(19, GuiItems.button(Material.SPONGE, "Lucky-Variante",
                List.of("Aktuell: " + game.lucky().variant()), false));
        inv.setItem(20, GuiItems.button(Material.FEATHER, "Soft-Wipe", List.of("Entfernt Effekte um Lucky."), false));
        inv.setItem(21, GuiItems.button(Material.NETHERITE_SWORD, "Hard-Wipe", List.of("Entfernt Entities großflächig."), false));
        inv.setItem(22, GuiItems.button(Material.RESPAWN_ANCHOR, "Bind", List.of("Setzt Spawn für alle."), false));
        inv.setItem(23, GuiItems.button(Material.PRISMARINE_BRICKS, "Plattform", List.of("Baut Safe-Plattform."), false));
        inv.setItem(24, GuiItems.button(Material.ENDER_PEARL, "Teleport", List.of("Teleportiert dich zum Spawn."), false));
        inv.setItem(25, GuiItems.button(Material.NAME_TAG, "Save Config", List.of("Speichert & lädt Config neu."), false));
    }

    // ====== CLEAR Y=101 (±300) ======
    private void handleClearPlaneY101(Player p) {
        World world = Worlds.require(LUCKY_WORLD_NAME);
        loadChunks(world, -300, -300, 300, 300);
        Msg.to(p, "Bereinige Ebene y=101...");
        dispatch("tellraw @a {\"text\":\"LuckySky: Ebene y=101 wird gereinigt...\",\"color\":\"yellow\"}");
        disableAdminLogs();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (int x = -300; x <= 300; x += STEP_XZ) {
                for (int z = -300; z <= 300; z += STEP_XZ) {
                    int x2 = Math.min(x + STEP_XZ - 1, 300);
                    int z2 = Math.min(z + STEP_XZ - 1, 300);
                    String cmd = String.format("fill %d 101 %d %d 101 %d air", x, z, x2, z2);
                    Bukkit.getScheduler().runTask(plugin, () -> dispatch(cmd));
                    sleep(25);
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.game().placePlatform();
                dispatch("tellraw @a {\"text\":\"Ebene y=101 gereinigt!\",\"color\":\"green\"}");
                Msg.to(p, "Fertig.");
                enableAdminLogs();
            });
        });
    }

    // ====== CLEAR 0–319 (±300) ======
    private void handleClearField300(Player p) {
        World world = Worlds.require(LUCKY_WORLD_NAME);
        loadChunks(world, -300, 0, 300, 319);
        Msg.to(p, "Vollbereinigung 0–319 gestartet...");
        dispatch("tellraw @a {\"text\":\"LuckySky: Vollbereinigung läuft...\",\"color\":\"red\"}");
        disableAdminLogs();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int total = ((600 / STEP_XZ) + 1) * ((600 / STEP_XZ) + 1) * ((319 / STEP_Y) + 1);
            int done = 0;
            for (int x = -300; x <= 300; x += STEP_XZ) {
                for (int z = -300; z <= 300; z += STEP_XZ) {
                    for (int y = 0; y <= 319; y += STEP_Y) {
                        int x2 = Math.min(x + STEP_XZ - 1, 300);
                        int z2 = Math.min(z + STEP_XZ - 1, 300);
                        int y2 = Math.min(y + STEP_Y - 1, 319);
                        String cmd = String.format("fill %d %d %d %d %d %d air", x, y, z, x2, y2, z2);
                        Bukkit.getScheduler().runTask(plugin, () -> dispatch(cmd));
                        done++;
                        if (done % 20 == 0) {
                            int pct = Math.min(100, done * 100 / total);
                            Bukkit.getScheduler().runTask(plugin, () ->
                                dispatch("tellraw @a [\"Wipe: \",{\"text\":\"" + pct + "%\",\"color\":\"gold\"}]"));
                        }
                        sleep(50);
                    }
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.game().placePlatform();
                dispatch("tellraw @a {\"text\":\"Spielfeld vollständig gereinigt!\",\"color\":\"dark_green\"}");
                Msg.to(p, "Vollbereinigung abgeschlossen!");
                enableAdminLogs();
            });
        });
    }

    // ====== SCHEMATIC: Load ======
    private void loadSchematic(Player p) {
        if (!p.getWorld().getName().equalsIgnoreCase(LUCKY_WORLD_NAME)) {
            Msg.to(p, "Bitte in LuckySky sein!");
            return;
        }
        p.performCommand("//schem load platform");
        Msg.to(p, "Schematic 'platform' geladen.");
    }

    // ====== SCHEMATIC: Paste ======
    private void pasteSchematic(Player p) {
        if (!p.getWorld().getName().equalsIgnoreCase(LUCKY_WORLD_NAME)) {
            Msg.to(p, "Bitte in LuckySky sein!");
            return;
        }
        p.performCommand("//paste");
        Msg.to(p, "Schematic gepastet.");
    }

    // ====== HELPER: Chunk Loading ======
    private void loadChunks(World world, int minX, int minZ, int maxX, int maxZ) {
        int minCX = minX >> 4, maxCX = maxX >> 4;
        int minCZ = minZ >> 4, maxCZ = maxZ >> 4;
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                world.loadChunk(cx, cz, false);
            }
        }
    }

    private void disableAdminLogs() { dispatch("gamerule logAdminCommands false"); }
    private void enableAdminLogs()  { dispatch("gamerule logAdminCommands true"); }
    private void dispatch(String cmd) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    // ====== BESTEHENDE FUNKTIONEN ======
    private void toggleTaunts(Player p) {
        TrapsConfig t = plugin.configs().traps();
        boolean v = !t.withers().taunts().enabled();
        plugin.configs().updateTraps(t.withTauntsEnabled(v));
        plugin.reloadSettings();
        plugin.game().setTauntsEnabled(v);
        Msg.to(p, v ? "Taunts AN" : "Taunts AUS");
    }

    private void toggleWither(Player p) {
        TrapsConfig t = plugin.configs().traps();
        boolean v = !t.withers().enabled();
        plugin.configs().updateTraps(t.withWithersEnabled(v));
        plugin.reloadSettings();
        plugin.game().setWitherEnabled(v);
        Msg.to(p, v ? "Wither AN" : "Wither AUS");
    }

    private void cycleLuckyVariant(Player p) {
        GameConfig g = plugin.configs().game();
        List<String> vars = g.lucky().variantsAvailable();
        String cur = g.lucky().variant();
        int i = vars.indexOf(cur);
        String next = vars.get((i + 1) % vars.size());
        plugin.configs().updateGame(g.withLuckyVariant(next));
        plugin.reloadSettings();
        Msg.to(p, "Variante: " + next);
    }

    private void teleportToSpawn(Player p) {
        WorldsConfig.LuckyWorld cfg = plugin.configs().worlds().luckySky();
        World w = Worlds.require(cfg.worldName());
        WorldsConfig.Spawn spawn = cfg.spawn();
        p.teleport(new Location(w, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch()));
        Msg.to(p, "Teleportiert.");
    }
}
