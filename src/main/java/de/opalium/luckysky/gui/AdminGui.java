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

import java.util.List;

public class AdminGui implements Listener {

    private static final String TITLE = ChatColor.DARK_AQUA + "LuckySky Admin";
    private static final int SIZE = 27;

    // === SLOTS ===
    private static final int SLOT_CLEAR_PLANE_Y101 = 8;
    private static final int SLOT_CLEAR_FIELD_FULL = 9;
    private static final int SLOT_LOAD_SCHEM = 26;
    private static final int SLOT_PASTE_SCHEM = 0;
    private static final int SLOT_RESET_WORLD = 7;
    private static final int SLOT_CREATE_ARENAS = 16;
    private static final int SLOT_TOGGLE_WITHER = 6;

    private static final String LUCKY_WORLD_NAME = "LuckySky";
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
            case SLOT_RESET_WORLD -> resetWorld(p);
            case SLOT_CREATE_ARENAS -> saveAndCreateThreeWorlds(p);
            case SLOT_TOGGLE_WITHER -> toggleWither(p);

            case 10 -> { plugin.game().start(); Msg.to(p, "&aCountdown gestartet."); }
            case 11 -> { plugin.game().stop(); Msg.to(p, "&eGame gestoppt & Lobby."); }
            case 12 -> { plugin.game().setDurationMinutes(5); Msg.to(p, "&aDauer: 5 Min."); }
            case 13 -> { plugin.game().setDurationMinutes(20); Msg.to(p, "&aDauer: 20 Min."); }
            case 14 -> { plugin.game().setDurationMinutes(60); Msg.to(p, "&aDauer: 60 Min."); }
            case 15 -> toggleTaunts(p);
            case 17 -> { plugin.scoreboard().setEnabled(!plugin.scoreboard().isEnabled()); }
            case 18 -> { plugin.scoreboard().setTimerVisible(!plugin.scoreboard().isTimerVisible()); }
            case 19 -> cycleLuckyVariant(p);
            case 20 -> { Msg.to(p, "&7Soft-Wipe: &f" + plugin.game().softClear()); }
            case 21 -> { Msg.to(p, "&7Hard-Wipe: &f" + plugin.game().hardClear()); }
            case 22 -> { plugin.game().bindAll(); Msg.to(p, "&bAlle gebunden."); }
            case 23 -> { plugin.game().placePlatform(); Msg.to(p, "&bPlattform gesetzt."); }
            case 24 -> teleportToSpawn(p);
            case 25 -> { plugin.configs().saveAll(); plugin.reloadSettings(); Msg.to(p, "&aConfig gespeichert."); }
        }

        Bukkit.getScheduler().runTask(plugin, () -> open(p));
    }

    private void populate(Inventory inv) {
        GameConfig game = plugin.configs().game();
        TrapsConfig traps = plugin.configs().traps();
        boolean running = plugin.game().state() == de.opalium.luckysky.game.GameState.RUNNING;

        inv.setItem(SLOT_CLEAR_PLANE_Y101, GuiItems.tntClearPlaneY101());
        inv.setItem(SLOT_CLEAR_FIELD_FULL, GuiItems.fullClear0to319());
        inv.setItem(SLOT_LOAD_SCHEM, GuiItems.button(Material.PAPER, "&aLoad Schematic", List.of("&7Lädt Schematic aus Folder."), false));
        inv.setItem(SLOT_PASTE_SCHEM, GuiItems.button(Material.STRUCTURE_BLOCK, "&aPaste Schematic", List.of("&7Pastet geladenes Schematic."), false));

        inv.setItem(SLOT_RESET_WORLD, GuiItems.button(
            Material.TNT, "&c&lRESET WELT",
            List.of("&7Löscht &cLuckySky&7 komplett", "&7und erstellt sie neu.", "", "&eDauer: &f~10s", "&aPodest neu gebaut."), true
        ));

        inv.setItem(SLOT_CREATE_ARENAS, GuiItems.button(
            Material.NETHER_STAR, "&d&lCREATE 3 ARENAS",
            List.of("&7±175 Blöcke, Border 350", "&7Spawn: 0,101,0", "&7Flat-Welt, gleicher Seed"), true
        ));

        boolean witherEnabled = traps.withers().enabled();
        inv.setItem(SLOT_TOGGLE_WITHER, GuiItems.button(
            Material.WITHER_SKELETON_SKULL,
            witherEnabled ? "&aWither AN" : "&cWither AUS",
            List.of("&7Aktiviert/Deaktiviert Wither-Spawns."), witherEnabled
        ));

        // Rest unverändert...
        inv.setItem(10, GuiItems.button(Material.LIME_DYE, "&aStart Countdown", List.of("&7Startet das Spiel."), running));
        inv.setItem(11, GuiItems.button(Material.BARRIER, "&cStop & Lobby", List.of("&7Stoppt das Spiel."), false));
        inv.setItem(12, GuiItems.button(Material.CLOCK, "&eMode 5", List.of("&7Dauer: 5 Min."), false));
        inv.setItem(13, GuiItems.button(Material.CLOCK, "&eMode 20", List.of("&7Dauer: 20 Min."), false));
        inv.setItem(14, GuiItems.button(Material.CLOCK, "&eMode 60", List.of("&7Dauer: 60 Min."), false));

        boolean tauntsEnabled = traps.withers().taunts().enabled();
        inv.setItem(15, GuiItems.button(Material.GOAT_HORN, tauntsEnabled ? "&aTaunts AN" : "&cTaunts AUS", List.of("&7Schaltet Taunts."), tauntsEnabled));

        boolean sbEnabled = plugin.scoreboard().isEnabled();
        boolean timerVis = plugin.scoreboard().isTimerVisible();
        inv.setItem(17, GuiItems.button(Material.OAK_SIGN, sbEnabled ? "&aScoreboard AN" : "&cScoreboard AUS", List.of("&7Schaltet Scoreboard."), sbEnabled));
        inv.setItem(18, GuiItems.button(Material.COMPASS, timerVis ? "&aTimer sichtbar" : "&cTimer versteckt", List.of("&7Blendet Timer ein/aus."), timerVis));
        inv.setItem(19, GuiItems.button(Material.SPONGE, "&bLucky-Variante", List.of("&7Aktuell: &f" + game.lucky().variant()), false));
        inv.setItem(20, GuiItems.button(Material.FEATHER, "&bSoft-Wipe", List.of("&7Entfernt Effekte."), false));
        inv.setItem(21, GuiItems.button(Material.NETHERITE_SWORD, "&cHard-Wipe", List.of("&7Entfernt Entities."), false));
        inv.setItem(22, GuiItems.button(Material.RESPAWN_ANCHOR, "&bBind", List.of("&7Setzt Spawn."), false));
        inv.setItem(23, GuiItems.button(Material.PRISMARINE_BRICKS, "&bPlattform", List.of("&7Baut Safe-Plattform."), false));
        inv.setItem(24, GuiItems.button(Material.ENDER_PEARL, "&dTeleport", List.of("&7Zum Spawn."), false));
        inv.setItem(25, GuiItems.button(Material.NAME_TAG, "&aSave Config", List.of("&7Speichert Config."), false));
    }

    // ===================================================================
    // === MULTIVERSE: WELT RESET (SICHER + UNLOAD-WAIT) =================
    // ===================================================================
    private void resetWorld(Player p) {
        World world = Bukkit.getWorld(LUCKY_WORLD_NAME);
        if (world == null) {
            Msg.to(p, "&cWelt 'LuckySky' nicht gefunden!");
            return;
        }

        long seed = world.getSeed(); // ← Seed vorher sichern!
        Msg.to(p, "&eWelt wird resettet... (Seed: " + seed + ")");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // 1. Spieler raus
            sync(() -> Bukkit.getOnlinePlayers().forEach(pl -> {
                World lobby = Bukkit.getWorld("world");
                if (lobby != null) pl.teleport(lobby.getSpawnLocation());
            }));

            // 2. Welt löschen
            sync(() -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv delete " + LUCKY_WORLD_NAME);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv confirm");
            });

            waitForWorldUnload(LUCKY_WORLD_NAME); // ← SICHERHEIT!

            // 3. Welt neu erstellen
            sync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "mv create " + LUCKY_WORLD_NAME + " NORMAL -t FLAT -s " + seed));

            waitForWorldLoad(LUCKY_WORLD_NAME);

            // 4. Plattform neu
            sync(() -> {
                plugin.game().placePlatform();
                dispatch("tellraw @a {\"text\":\"Welt resettet & Plattform gebaut!\",\"color\":\"green\"}");
                Msg.to(p, "&aWelt erfolgreich resettet!");
            });
        });
    }

    // ===================================================================
    // === MULTIVERSE: 3 ARENEN (FORTSCHRITT + UNLOAD-WAIT) ==============
    // ===================================================================
    private void saveAndCreateThreeWorlds(Player p) {
        World current = Bukkit.getWorld(LUCKY_WORLD_NAME);
        if (current == null) {
            Msg.to(p, "&cAktuelle Welt nicht gefunden!");
            return;
        }

        long seed = current.getSeed();
        String baseName = "LuckySky_Arena";
        int size = 175;
        int border = size * 2;

        Msg.to(p, "&eErstelle 3 Arenen... (Seed: " + seed + ")");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (int i = 1; i <= 3; i++) {
                String worldName = baseName + i;

                // 1. Löschen
                sync(() -> {
                    if (Bukkit.getWorld(worldName) != null) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv delete " + worldName);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv confirm");
                    }
                });
                waitForWorldUnload(worldName);

                // 2. Erstellen
                sync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "mv create " + worldName + " NORMAL -t FLAT -s " + seed));
                waitForWorldLoad(worldName);

                // 3. Border + Spawn
                sync(() -> {
                    World w = Bukkit.getWorld(worldName);
                    if (w != null) {
                        w.getWorldBorder().setCenter(0, 0);
                        w.getWorldBorder().setSize(border);
                        w.setSpawnLocation(0, 101, 0);
                    }
                });

                // 4. Plattform
                sync(() -> plugin.game().placePlatform());

                // 5. Fortschritt
                final int finalI = i;
                sync(() -> dispatch("tellraw @a {\"text\":\"Arena " + finalI + "/3 erstellt!\",\"color\":\"aqua\"}"));
            }

            sync(() -> {
                dispatch("tellraw @a {\"text\":\"3 Arenen fertig: LuckySky_Arena1–3!\",\"color\":\"green\"}");
                Msg.to(p, "&aAlle Arenen bereit!");
            });
        });
    }

    // ===================================================================
    // === SICHERHEITS-WAITS ============================================
    // ===================================================================
    private void waitForWorldUnload(String worldName) {
        for (int i = 0; i < 20; i++) {
            if (Bukkit.getWorld(worldName) == null) return;
            sleep(500);
        }
        Bukkit.getLogger().warning("[LuckySky] Welt '" + worldName + "' nicht vollständig entladen!");
    }

    private void waitForWorldLoad(String worldName) {
        for (int i = 0; i < 20; i++) {
            if (Bukkit.getWorld(worldName) != null) return;
            sleep(500);
        }
        Bukkit.getLogger().warning("[LuckySky] Welt '" + worldName + "' nicht geladen!");
    }

    // ===================================================================
    // === CLEAR FUNKTIONEN (execute in + sync) =========================
    // ===================================================================
    private void handleClearPlaneY101(Player p) {
        World world = Bukkit.getWorld(LUCKY_WORLD_NAME);
        if (world == null) { Msg.to(p, "&cWelt nicht gefunden!"); return; }
        loadChunksForArea(world, -300, -300, 300, 300);
        Msg.to(p, "&aBereinige y=101...");
        dispatch("tellraw @a {\"text\":\"Ebene y=101 wird gereinigt...\",\"color\":\"yellow\"}");
        disableAdminLogs();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (int x = -300; x <= 300; x += STEP_XZ) {
                for (int z = -300; z <= 300; z += STEP_XZ) {
                    int x1 = x, x2 = Math.min(x + STEP_XZ - 1, 300);
                    int z1 = z, z2 = Math.min(z + STEP_XZ - 1, 300);
                    String cmd = String.format("execute in %s run fill %d 101 %d %d 101 %d air", LUCKY_WORLD_NAME, x1, z1, x2, z2);
                    sync(() -> dispatch(cmd));
                    sleep(25);
                }
            }
            sync(() -> {
                restorePlatform(world);
                dispatch("tellraw @a {\"text\":\"Ebene y=101 gereinigt!\",\"color\":\"green\"}");
                Msg.to(p, "&aFertig.");
                enableAdminLogs();
            });
        });
    }

    // (handleClearField300 analog – gekürzt für Übersicht)

    // ===================================================================
    // === HELPER ========================================================
    // ===================================================================
    private void loadChunksForArea(World world, int minX, int minZ, int maxX, int maxZ) {
        int minCX = minX >> 4, maxCX = maxX >> 4;
        int minCZ = minZ >> 4, maxCZ = maxZ >> 4;
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                world.loadChunk(cx, cz, false);
            }
        }
    }

    private void restorePlatform(World world) {
        String[] cmds = {
            "execute in " + LUCKY_WORLD_NAME + " run setblock 0 100 -1 prismarine_stairs[facing=south,half=bottom,shape=straight]",
            "execute in " + LUCKY_WORLD_NAME + " run setblock 0 100 0 prismarine_stairs[facing=south,half=bottom,shape=straight]",
            "execute in " + LUCKY_WORLD_NAME + " run setblock 0 100 1 prismarine_bricks",
            "execute in " + LUCKY_WORLD_NAME + " run setblock 0 100 2 prismarine_bricks"
        };
        for (String cmd : cmds) dispatch(cmd);
    }

    private void dispatch(String cmd) {
        sync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }

    private void sync(Runnable r) { Bukkit.getScheduler().runTask(plugin, r); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    // === BESTEHENDE FUNKTIONEN (unverändert) ===
    private void loadSchematic(Player p) { if (!isInLuckyWorld(p)) { Msg.to(p, "&cIn LuckySky sein!"); return; } p.performCommand("//schem load platform"); Msg.to(p, "&aGeladen."); }
    private void pasteSchematic(Player p) { if (!isInLuckyWorld(p)) { Msg.to(p, "&cIn LuckySky sein!"); return; } p.performCommand("//paste"); Msg.to(p, "&aGepastet."); }
    private boolean isInLuckyWorld(Player p) { return p.getWorld().getName().equalsIgnoreCase(LUCKY_WORLD_NAME); }
    private void disableAdminLogs() { dispatch("gamerule logAdminCommands false"); }
    private void enableAdminLogs() { dispatch("gamerule logAdminCommands true"); }

    private void toggleTaunts(Player p) { /* ... */ }
    private void toggleWither(Player p) { /* ... */ }
    private void cycleLuckyVariant(Player p) { /* ... */ }
    private void teleportToSpawn(Player p) { /* ... */ }
}
