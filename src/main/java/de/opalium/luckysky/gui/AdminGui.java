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
    private static final int SLOT_CLEAR_PLANE_Y101 = 8;     // TNT
    private static final int SLOT_CLEAR_FIELD_FULL = 9;     // GUNPOWDER
    private static final int SLOT_LOAD_SCHEM = 26;          // PAPER
    private static final int SLOT_PASTE_SCHEM = 0;          // STRUCTURE_BLOCK
    private static final int SLOT_RESET_WORLD = 7;          // TNT (NEU)
    private static final int SLOT_CREATE_ARENAS = 16;       // NETHER_STAR (NEU)
    private static final int SLOT_TOGGLE_WITHER = 6;        // WITHER_SKULL (neu verschoben!)

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

        // RESET WELT
        inv.setItem(SLOT_RESET_WORLD, GuiItems.button(
                Material.TNT, "&c&lRESET WELT",
                List.of("&7Löscht &cLuckySky&7 komplett",
                        "&7und erstellt sie neu (gleicher Seed).",
                        "", "&eDauer: &f~10 Sekunden",
                        "&aPodest wird neu gebaut."), true));

        // CREATE 3 ARENAS
        inv.setItem(SLOT_CREATE_ARENAS, GuiItems.button(
                Material.NETHER_STAR, "&d&lCREATE 3 ARENAS",
                List.of("&7Speichert Seed & erstellt:",
                        "&f→ LuckySky_Arena1",
                        "&f→ LuckySky_Arena2",
                        "&f→ LuckySky_Arena3",
                        "", "&7±175 Blöcke, Border 175",
                        "&7Spawn: 0, 101, 0"), true));

        // WITHER TOGGLE
        boolean witherEnabled = traps.withers().enabled();
        inv.setItem(SLOT_TOGGLE_WITHER, GuiItems.button(
                Material.WITHER_SKELETON_SKULL,
                witherEnabled ? "&aWither AN" : "&cWither AUS",
                List.of("&7Aktiviert/Deaktiviert Wither-Spawns."), witherEnabled
        ));

        inv.setItem(10, GuiItems.button(Material.LIME_DYE, "&aStart Countdown",
                List.of("&7Startet das Spiel und teleportiert zur Plattform."), running));
        inv.setItem(11, GuiItems.button(Material.BARRIER, "&cStop & Lobby",
                List.of("&7Stoppt das Spiel und sendet alle zur Lobby."), false));
        inv.setItem(12, GuiItems.button(Material.CLOCK, "&eMode 5", List.of("&7Setzt Dauer auf 5 Minuten."), false));
        inv.setItem(13, GuiItems.button(Material.CLOCK, "&eMode 20", List.of("&7Setzt Dauer auf 20 Minuten."), false));
        inv.setItem(14, GuiItems.button(Material.CLOCK, "&eMode 60", List.of("&7Setzt Dauer auf 60 Minuten."), false));

        boolean tauntsEnabled = traps.withers().taunts().enabled();
        inv.setItem(15, GuiItems.button(Material.GOAT_HORN, tauntsEnabled ? "&aTaunts AN" : "&cTaunts AUS", List.of("&7Schaltet Wither-Taunts um."), tauntsEnabled));

        boolean scoreboardEnabled = plugin.scoreboard().isEnabled();
        boolean timerVisible = plugin.scoreboard().isTimerVisible();
        inv.setItem(17, GuiItems.button(Material.OAK_SIGN, scoreboardEnabled ? "&aScoreboard AN" : "&cScoreboard AUS", List.of("&7Schaltet das LuckySky-Scoreboard."), scoreboardEnabled));
        inv.setItem(18, GuiItems.button(Material.COMPASS, timerVisible ? "&aTimer sichtbar" : "&cTimer versteckt", List.of("&7Blendt den Timer im Scoreboard ein/aus."), timerVisible));
        inv.setItem(19, GuiItems.button(Material.SPONGE, "&bLucky-Variante", List.of("&7Aktuell: &f" + game.lucky().variant()), false));
        inv.setItem(20, GuiItems.button(Material.FEATHER, "&bSoft-Wipe", List.of("&7Entfernt Effekte um Lucky."), false));
        inv.setItem(21, GuiItems.button(Material.NETHERITE_SWORD, "&cHard-Wipe", List.of("&7Entfernt Entities großflächig."), false));
        inv.setItem(22, GuiItems.button(Material.RESPAWN_ANCHOR, "&bBind", List.of("&7Setzt Spawn für alle."), false));
        inv.setItem(23, GuiItems.button(Material.PRISMARINE_BRICKS, "&bPlattform", List.of("&7Baut Safe-Plattform."), false));
        inv.setItem(24, GuiItems.button(Material.ENDER_PEARL, "&dTeleport", List.of("&7Teleportiert dich zum Spawn."), false));
        inv.setItem(25, GuiItems.button(Material.NAME_TAG, "&aSave Config", List.of("&7Speichert & lädt Config neu."), false));
    }

    // ===================================================================
    // === MULTIVERSE: WELT RESET =======================================
    // ===================================================================
    private void resetWorld(Player p) {
        if (!hasMV()) { Msg.to(p, "&cMultiverse-Core fehlt!"); return; }

        World world = Bukkit.getWorld(LUCKY_WORLD_NAME);
        if (world == null) { Msg.to(p, "&cWelt 'LuckySky' nicht gefunden!"); return; }

        long seed = world.getSeed();
        Msg.to(p, "&eWelt wird resettet... Spieler werden teleportiert.");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // 1) Spieler raus
            sync(() -> Bukkit.getOnlinePlayers().forEach(pl -> {
                World lobby = Bukkit.getWorlds().get(0);
                pl.teleport(lobby.getSpawnLocation());
            }));

            // 2) Delete & confirm
            sync(() -> {
                dispatch("mv delete " + LUCKY_WORLD_NAME);
                dispatch("mv confirm");
            });

            waitForWorldUnload(LUCKY_WORLD_NAME);

            // 3) Neu erstellen (flat, gleicher Seed)
            sync(() -> dispatch("mv create " + LUCKY_WORLD_NAME + " NORMAL -t FLAT -s " + seed));

            // 4) kleine safety-wait + Plattform bauen
            sleep(1500);
            sync(() -> {
                World nw = Bukkit.getWorld(LUCKY_WORLD_NAME);
                if (nw != null) {
                    nw.setSpawnLocation(0, 101, 0);
                    plugin.game().placePlatform();
                    dispatchIn(nw, "tellraw @a {\"text\":\"Welt wurde resettet & Plattform neu gebaut!\",\"color\":\"green\"}");
                    Msg.to(p, "&aWelt erfolgreich resettet!");
                } else {
                    Msg.to(p, "&cFehler: Welt nach Create nicht geladen.");
                }
            });
        });
    }

    // ===================================================================
    // === MULTIVERSE: 3 KLEINE ARENEN ==================================
    // ===================================================================
    private void saveAndCreateThreeWorlds(Player p) {
        if (!hasMV()) { Msg.to(p, "&cMultiverse-Core fehlt!"); return; }
        World current = Bukkit.getWorld(LUCKY_WORLD_NAME);
        if (current == null) { Msg.to(p, "&cAktuelle Welt nicht gefunden!"); return; }

        long seed = current.getSeed();
        String baseName = "LuckySky_Arena";
        int size = 175;
        int border = size * 2;

        Msg.to(p, "&eErstelle 3 kleine Welten... (Seed: " + seed + ")");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (int i = 1; i <= 3; i++) {
                String worldName = baseName + i;

                // ggf. löschen
                sync(() -> {
                    if (Bukkit.getWorld(worldName) != null) {
                        dispatch("mv delete " + worldName);
                        dispatch("mv confirm");
                    }
                });
                waitForWorldUnload(worldName);

                // neu erstellen
                sync(() -> dispatch("mv create " + worldName + " NORMAL -t FLAT -s " + seed));
                sleep(800);

                // Border + Spawn + Plattform
                sync(() -> {
                    World w = Bukkit.getWorld(worldName);
                    if (w != null) {
                        w.getWorldBorder().setCenter(0, 0);
                        w.getWorldBorder().setSize(border);
                        w.setSpawnLocation(0, 101, 0);
                        plugin.game().placePlatform();
                    }
                });
                sleep(400);

                int progress = (int) Math.round(i / 3.0 * 100.0);
                sync(() -> dispatch("tellraw @a [\"\",{\"text\":\"Arenen: \",\"color\":\"aqua\"},{\"text\":\"" + progress + "%\",\"color\":\"gold\"}]"));
            }

            sync(() -> {
                dispatch("tellraw @a {\"text\":\"3 neue Arenen erstellt: LuckySky_Arena1–3!\",\"color\":\"aqua\"}");
                Msg.to(p, "&a3 kleine Welten erstellt & bereit!");
            });
        });
    }

    // ===================================================================
    // === CLEAR FUNKTIONEN (mit execute in + forceload) =================
    // ===================================================================
    private void handleClearPlaneY101(Player p) {
        World w = Bukkit.getWorld(LUCKY_WORLD_NAME);
        if (w == null) { Msg.to(p, "&cWelt nicht gefunden!"); return; }

        // Chunks laden + forceload
        loadChunksForArea(w, -300, -300, 300, 300);
        forceload(w, -300, -300, 300, 300, true);

        Msg.to(p, "&aBereinige y=101...");
        dispatchIn(w, "tellraw @a {\"text\":\"Ebene y=101 wird gereinigt...\",\"color\":\"yellow\"}");
        dispatchIn(w, "gamerule logAdminCommands false");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (int x = -300; x <= 300; x += STEP_XZ) {
                for (int z = -300; z <= 300; z += STEP_XZ) {
                    int x2 = Math.min(x + STEP_XZ - 1, 300);
                    int z2 = Math.min(z + STEP_XZ - 1, 300);
                    String cmd = String.format("fill %d 101 %d %d 101 %d air", x, z, x2, z2);
                    sync(() -> dispatchIn(w, cmd));
                    sleep(25);
                }
            }
            sync(() -> {
                restorePlatform(w);
                dispatchIn(w, "tellraw @a {\"text\":\"Ebene y=101 gereinigt!\",\"color\":\"green\"}");
                dispatchIn(w, "gamerule logAdminCommands true");
                forceload(w, -300, -300, 300, 300, false);
                Msg.to(p, "&aFertig.");
            });
        });
    }

    private void handleClearField300(Player p) {
        World w = Bukkit.getWorld(LUCKY_WORLD_NAME);
        if (w == null) { Msg.to(p, "&cWelt nicht gefunden!"); return; }

        loadChunksForArea(w, -300, -300, 300, 300);
        forceload(w, -300, -300, 300, 300, true);

        Msg.to(p, "&aVollbereinigung läuft...");
        dispatchIn(w, "tellraw @a {\"text\":\"Vollbereinigung läuft...\",\"color\":\"red\"}");
        dispatchIn(w, "gamerule logAdminCommands false");

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
                        sync(() -> dispatchIn(w, cmd));
                        if (++done % 20 == 0) {
                            int pct = Math.min(100, done * 100 / total);
                            int finalPct = pct;
                            sync(() -> dispatchIn(w, "tellraw @a [\"\",{\"text\":\"Wipe: \",\"color\":\"white\"},{\"text\":\"" + finalPct + "%\",\"color\":\"gold\"}]"));
                        }
                        sleep(50);
                    }
                }
            }
            sync(() -> {
                restorePlatform(w);
                dispatchIn(w, "tellraw @a {\"text\":\"Spielfeld gereinigt!\",\"color\":\"dark_green\"}");
                dispatchIn(w, "gamerule logAdminCommands true");
                forceload(w, -300, -300, 300, 300, false);
                Msg.to(p, "&aVollbereinigung abgeschlossen!");
            });
        });
    }

    // ===================================================================
    // === HELPER ========================================================
    // ===================================================================
    private void loadChunksForArea(World world, int minX, int minZ, int maxX, int maxZ) {
        int minCX = floorDiv(minX, 16), maxCX = floorDiv(maxX, 16);
        int minCZ = floorDiv(minZ, 16), maxCZ = floorDiv(maxZ, 16);
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                world.loadChunk(cx, cz, true); // true = laden/erzeugen
            }
        }
    }

    private void forceload(World w, int minX, int minZ, int maxX, int maxZ, boolean add) {
        int minCX = floorDiv(minX, 16), maxCX = floorDiv(maxX, 16);
        int minCZ = floorDiv(minZ, 16), maxCZ = floorDiv(maxZ, 16);
        String cmd = (add ? "forceload add " : "forceload remove ") +
                minCX + " " + minCZ + " " + maxCX + " " + maxCZ;
        dispatchIn(w, cmd);
    }

    private void restorePlatform(World world) {
        String[] cmds = {
                "setblock 0 100 -1 prismarine_stairs[facing=south,half=bottom,shape=straight]",
                "setblock 0 100 0 prismarine_stairs[facing=south,half=bottom,shape=straight]",
                "setblock 0 100 1 prismarine_bricks",
                "setblock 0 100 2 prismarine_bricks"
        };
        for (String c : cmds) dispatchIn(world, c);
    }

    private void dispatch(String cmd) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd); }
    private void dispatchIn(World world, String cmd) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute in " + world.getKey() + " run " + cmd);
    }

    private void sync(Runnable r) { Bukkit.getScheduler().runTask(plugin, r); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    private void waitForWorldUnload(String worldName) {
        for (int i = 0; i < 20; i++) {
            if (Bukkit.getWorld(worldName) == null) return;
            sleep(500);
        }
    }

    private int floorDiv(int a, int b) { int r = a / b; if ((a ^ b) < 0 && (r * b != a)) r--; return r; }
    private boolean hasMV() { return Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null; }

    // ===================================================================
    // === FUNKTIONEN ====================================================
    // ===================================================================
    private void loadSchematic(Player p) {
        if (!isInLuckyWorld(p)) { Msg.to(p, "&cIn LuckySky sein!"); return; }
        p.performCommand("//schem load platform");
        Msg.to(p, "&aSchematic geladen.");
    }

    private void pasteSchematic(Player p) {
        if (!isInLuckyWorld(p)) { Msg.to(p, "&cIn LuckySky sein!"); return; }
        p.performCommand("//paste");
        Msg.to(p, "&aSchematic gepastet.");
    }

    private boolean isInLuckyWorld(Player p) {
        return p.getWorld().getName().equalsIgnoreCase(LUCKY_WORLD_NAME);
    }

    private void toggleTaunts(Player p) {
        TrapsConfig t = plugin.configs().traps();
        boolean v = !t.withers().taunts().enabled();
        plugin.configs().updateTraps(t.withTauntsEnabled(v));
        plugin.reloadSettings();
        plugin.game().setTauntsEnabled(v);
        Msg.to(p, v ? "&aTaunts aktiviert." : "&cTaunts deaktiviert.");
    }

    private void toggleWither(Player p) {
        TrapsConfig t = plugin.configs().traps();
        boolean v = !t.withers().enabled();
        plugin.configs().updateTraps(t.withWithersEnabled(v));
        plugin.reloadSettings();
        plugin.game().setWitherEnabled(v);
        Msg.to(p, v ? "&aWither aktiviert." : "&cWither deaktiviert.");
    }

    private void cycleLuckyVariant(Player p) {
        GameConfig g = plugin.configs().game();
        List<String> vars = g.lucky().variantsAvailable();
        String cur = g.lucky().variant();
        int i = vars.indexOf(cur);
        String next = vars.get((i + 1) % vars.size());
        plugin.configs().updateGame(g.withLuckyVariant(next));
        plugin.reloadSettings();
        Msg.to(p, "&bLucky-Variante: &f" + next);
    }

    private void teleportToSpawn(Player p) {
        WorldsConfig.LuckyWorld cfg = plugin.configs().worlds().luckySky();
        World w = Worlds.require(cfg.worldName());
        WorldsConfig.Spawn spawn = cfg.spawn();
        p.teleport(new Location(w, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch()));
        Msg.to(p, "&dTeleportiert.");
    }
}
